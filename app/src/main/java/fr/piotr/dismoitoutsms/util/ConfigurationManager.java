package fr.piotr.dismoitoutsms.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import fr.piotr.dismoitoutsms.contacts.Contact;
import fr.piotr.dismoitoutsms.contacts.Contacts;

/**
 * @author Piotr
 * 
 */
public class ConfigurationManager {

	public enum Configuration {
		EMOTICONES, COMMANDE_VOCALE, UNIQUEMENT_CONTACTS, CONTACTS_BANNIS, LANGUE_DICTION, ARRET_STEP_DETECTOR, HEADSET_MODE
	}

	private static Map<String, String>	configuration	= new HashMap<>();

	private static final String			FICHIER			= "DisMoisToutSms.properties";

	public static void set(Context context, Configuration conf, String valeur) {
		charger(context);
		configuration.put(conf.toString(), valeur);
		sauvegarder(context);
	}

	public static void setBoolean(Context context, Configuration conf, boolean valeur) {
		set(context, conf, String.valueOf(valeur));
	}

	private static void sauvegarder(Context context) {
		FileOutputStream ous = null;
		OutputStreamWriter osw = null;
		try {
			// Crée un flux de sortie vers un fichier local.
			ous = context.openFileOutput(FICHIER, Context.MODE_PRIVATE);
			osw = new OutputStreamWriter(ous);
			for (final Entry<String, String> entry : configuration.entrySet()) {
				osw.write(entry.getKey() + "=" + entry.getValue() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (osw != null) {
					osw.close();
				}
				if (ous != null) {
					ous.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static void charger(Context context) {
		FileInputStream ins = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			// Crée un flux d’entrée depuis un fichier local.
			ins = context.openFileInput(FICHIER);
			isr = new InputStreamReader(ins);
			br = new BufferedReader(isr);
			String line = br.readLine();
			while (line != null) {
				String[] split = line.split("=");
				String cle = split[0];
				String valeur = "";
				if (split.length == 2) {
					valeur = split[1];
				}
				configuration.put(cle, valeur);
				line = br.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (isr != null) {
					isr.close();
				}
				if (ins != null) {
					ins.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String get(Context context, Configuration conf) {
		charger(context);
		String valeur = configuration.get(conf.toString());
		if (valeur == null) {
			valeur = "";
		}
		return valeur;
	}

	public static boolean getBoolean(Context context, Configuration config) {
		return Boolean.TRUE.toString().equals(get(context, config));
	}

	public static boolean leContactEstBannis(Context context, Integer id) {
		String valeur = get(context, Configuration.CONTACTS_BANNIS);
		if (valeur == null) {
			return false;
		}
		List<String> bannis = Arrays.asList(valeur.split(";"));
		for (String cle : bannis) {
			if (cle.equals(id.toString())) {
				return true;
			}
		}
		return false;
	}

	public static void bannirContact(Context context, Integer id) {
		if (!leContactEstBannis(context, id)) {
			String valeur = get(context, Configuration.CONTACTS_BANNIS);
			valeur = valeur + id + ";";
			set(context, Configuration.CONTACTS_BANNIS, valeur);
		}
	}

	public static List<String> getIdsContactsBannis(Context context) {
		String valeur = get(context, Configuration.CONTACTS_BANNIS);
		if (valeur == null) {
			valeur = "";
		}
		return new ArrayList<>(Arrays.asList(valeur.split(";")));
	}

//	public static void gratifierContact(Context context, Integer id) {
//		if (leContactEstBannis(context, id)) {
//			String valeur = get(context, Configuration.CONTACTS_BANNIS);
//			List<String> bannis = new ArrayList<String>(Arrays.asList(valeur.split(";")));
//			bannis.remove(id.toString());
//			StringBuffer sb = new StringBuffer();
//			for (String cle : bannis) {
//				sb.append(cle);
//				sb.append(";");
//			}
//			valeur = sb.toString();
//			set(context, CONTACTS_BANNIS, valeur);
//		}
//	}
//
//	public static boolean aucunContactBanni(Context context) {
//		String valeur = get(context, Configuration.CONTACTS_BANNIS);
//		return valeur == null || valeur.isEmpty();
//	}
//
//	public static void gratifierTousLesContacts(Context context) {
//		set(context, CONTACTS_BANNIS, "");
//	}
//
//	public static void bannirTousLesContacts(Context context) {
//		Collection<Contact> allContacts = getAllContacts(context);
//		for (Contact contact : allContacts) {
//			bannirContact(context, contact.getId());
//		}
//	}

	public static void bannirLesContacts(Context context, Contacts contactsABannir) {
		StringBuilder valeur = new StringBuilder();
		for (Contact contact : contactsABannir) {
			valeur.append(contact.getId());
			valeur.append(";");
		}
		set(context, Configuration.CONTACTS_BANNIS, valeur.toString());
	}

	public static Locale getLangueSelectionnee(Context context) {
		String string = get(context, Configuration.LANGUE_DICTION);
		if (string == null || string.isEmpty()) {
			return Locale.getDefault();
		}
		Locale[] availableLocales = Locale.getAvailableLocales();
		for (Locale locale : availableLocales) {
			if (locale.getDisplayName().equals(string)) {
				return locale;
			}
		}
		return null;
	}

	public static void setLangueSelectionnee(Context context, String string) {
		Locale[] availableLocales = Locale.getAvailableLocales();
		for (Locale locale : availableLocales) {
			if (locale.getDisplayName().equals(string)) {
				set(context, Configuration.LANGUE_DICTION, string);
			}
		}
	}

}
