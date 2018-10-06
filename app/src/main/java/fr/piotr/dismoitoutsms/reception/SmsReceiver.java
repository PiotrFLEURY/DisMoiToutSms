package fr.piotr.dismoitoutsms.reception;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Calendar;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import fr.piotr.dismoitoutsms.SmsRecuActivity;
import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.messages.Message;
import fr.piotr.dismoitoutsms.util.ConfigurationManager;

import static fr.piotr.dismoitoutsms.util.ContactHelper.getContact;

/**
 * @author Piotr
 * 
 */
public class SmsReceiver extends BroadcastReceiver {

	public static final String	ACTION_RECEIVE_SMS	= "android.provider.Telephony.SMS_RECEIVED";

	private static SmsReceiver	instance;
	private SortedSet<Message> messagesEnAttente;

    private boolean dictating;

    public boolean isDictating() {
        return dictating;
    }

    public void setDictating(boolean dictating) {
        this.dictating = dictating;
    }

	public static SmsReceiver getInstance() {
		if (instance == null) {
			instance = new SmsReceiver();
		}
		return instance;
	}

	public SmsReceiver() {
		instance = this;
		messagesEnAttente = new TreeSet<>();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (ACTION_RECEIVE_SMS.equals(intent.getAction())) {
            onSmsReceived(context, intent);
		}
	}

    private void onSmsReceived(Context context, Intent intent) {
        Log.i("DisMoiToutSms", "SmsReceiver new message");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            if(pdus!=null) {
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format );
                    } else {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                }
                if (messages.length > -1) {
                    StringBuilder smsEntier = new StringBuilder();
                    for (SmsMessage sms : messages) {
                        smsEntier.append(sms.getMessageBody());
                    }
                    SmsMessage message = messages[0];
                    onSmsReceived(context, message.getDisplayOriginatingAddress(), smsEntier.toString());
                }
            }
        }
    }

    public void onSmsReceived(Context context, String phoneNumber, String smsEntier) {
        Contact contact = getContact(context, phoneNumber);
        if (jePeuxDicterLeSmsDe(context, contact)) {
            Log.i("DisMoiToutSms", "SmsReceiver can speak");

            if(contact==null) {
                contact = new Contact(-1, phoneNumber, phoneNumber, 0);//FIXME named parameters
            }

            dicterLeSms(context, smsEntier, contact);
        }
    }

    private void dicterLeSms(final Context context, final String contenuDuMessage, final Contact contact) {
        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager.getCallState()!=TelephonyManager.CALL_STATE_IDLE){
            addToWaiting(contact, contenuDuMessage);
            telephonyManager.listen(new PhoneStateListener(){
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    if(state==TelephonyManager.CALL_STATE_IDLE){
                        Log.i("DisMoiToutSms", "SmsReceiver reading message");
                        nextMessage(context);
                        telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            if (isDictating()) {
                Log.i("DisMoiToutSms", "SmsReceiver waiting to speak");
                addToWaiting(contact, contenuDuMessage);
            } else if (!messagesEnAttente.isEmpty()) {
                Log.i("DisMoiToutSms",
                        "SmsReceiver appending message because of already waiting messages");
                addToWaiting(contact, contenuDuMessage);

                if(!isDictating()) {
                    nextMessage(context);
                }
            } else {
                Log.i("DisMoiToutSms", "SmsReceiver reading message");
                afficherEtLiteLeMessage(context, new Message(Calendar.getInstance().getTime(), contact, contenuDuMessage));
            }
        }
	}

	public void afficherEtLiteLeMessage(Context context, Message message) {

        final String contenuDuMessage = message.getMessage();
        String phoneNumber = message.getContact().getTelephone();
        Contact contact = message.getContact();

		String contactName = contact.getName();

        Intent intent = new Intent(context, SmsRecuActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SmsRecuActivity.Parameters.DATE.name(), message.getDate().getTime());
		intent.putExtra(SmsRecuActivity.Parameters.CONTACT_NAME.toString(), contactName);
		intent.putExtra(SmsRecuActivity.Parameters.CONTACT.toString(), contact);
		intent.putExtra(SmsRecuActivity.Parameters.MESSAGE.toString(), contenuDuMessage);
		intent.putExtra(SmsRecuActivity.Parameters.NUMERO_A_QUI_REPONDRE.toString(), phoneNumber);
		context.startActivity(intent);

	}

	private void addToWaiting(Contact contact, String phrase) {
		for (Message message : messagesEnAttente) {
			if (message.contactIs(contact)) {
				message.append(phrase);
				return;
			}
		}
		messagesEnAttente.add(new Message(Calendar.getInstance().getTime(), contact, phrase));
	}

    public void standBy(Message message) {
        messagesEnAttente.add(message);
    }

	private boolean jePeuxDicterLeSmsDe(Context context, Contact contact) {
		boolean uniquementContacts = ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS);
        return !(uniquementContacts && (contact == null || ConfigurationManager.leContactEstBannis(context, contact.getId())));
    }

	public void nextMessage(final Context context) {
        if (!messagesEnAttente.isEmpty()) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!messagesEnAttente.isEmpty()) {
                        if (isDictating()) {
                            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SmsRecuActivity.EVENT_FINISH));
                        }
                        Message message = messagesEnAttente.first();
                        messagesEnAttente.remove(message);
                        afficherEtLiteLeMessage(context, message);
                    }
                }
            }, 2000);
        }
	}


}
