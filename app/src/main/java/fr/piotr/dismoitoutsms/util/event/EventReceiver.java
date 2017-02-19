package fr.piotr.dismoitoutsms.util.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by piotr_000 on 23/10/2016.
 *
 */

public class EventReceiver extends BroadcastReceiver {

    Map<String, Method> actions = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(actions.keySet().contains(intent.getAction())){
            Method method = actions.get(intent.getAction());
            try {
                method.invoke(this, null);
            } catch (IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public void put(String value, Method method) {
        actions.put(value, method);
    }

    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        for (String action : actions.keySet()) {
            filter.addAction(action);
        }
        return filter;
    }
}
