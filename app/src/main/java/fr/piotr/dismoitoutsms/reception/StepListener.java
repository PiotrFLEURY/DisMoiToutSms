package fr.piotr.dismoitoutsms.reception;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import fr.piotr.dismoitoutsms.BuildConfig;
import fr.piotr.dismoitoutsms.R;
import fr.piotr.dismoitoutsms.util.NotificationHelper;

/**
 * Created by piotr_000 on 12/03/2016.
 *
 */
public class StepListener implements SensorEventListener {

    private static final int STEP_LIMIT = 100;

    private Context context;

    Set<Long> steps;

    public StepListener(Context context) {
        this.context=context;
        this.steps = new TreeSet<>();
    }

    void clean(long time){
        Set<Long> newSteps = new HashSet<>();
        for(Long value:steps){
            long delay = time - value;
            if(delay < 60000){
                newSteps.add(value);
            }
        }
        steps=newSteps;
    }

    void stepDetected(long time) {
        clean(time);
        steps.add(time);
        if(steps.size()==STEP_LIMIT){
            stopService();
        } else {
            notifyStep();
        }
    }

    void notifyStep() {
        if(BuildConfig.DEBUG) {
            NotificationHelper.open(context, NotificationHelper.STOPPED_BY_STEP_COUNTER,
                    R.drawable.ic_directions_run_white_24dp,
                    context.getResources().getString(R.string.service_notif_titre),
                    steps.size() + " pas sur " + STEP_LIMIT);
        }
    }

    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_STEP_DETECTOR) {
            stepDetected(System.currentTimeMillis());
        }
    }

    void stopService() {
        Intent intent = new Intent(context, ServiceCommunicator.class);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        context.stopService(intent);
        NotificationHelper.open(context, NotificationHelper.STOPPED_BY_STEP_COUNTER);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }
}
