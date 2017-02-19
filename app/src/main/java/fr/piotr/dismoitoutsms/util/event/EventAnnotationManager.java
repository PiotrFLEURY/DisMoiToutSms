package fr.piotr.dismoitoutsms.util.event;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by piotr_000 on 23/10/2016.
 *
 */

public class EventAnnotationManager {

    private static Map<Class<?>, EventReceiver> receivers = new HashMap<>();

    public static void registerReceiver(Context context) {
        Class<? extends Context> clazz = context.getClass();
        EventReceiver receiver = new EventReceiver();
        for (Method method : clazz.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if(EventAware.class.equals(annotation.getClass())){
                    EventAware eventAware = (EventAware) annotation;
                    receiver.put(eventAware.value(), method);
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, receiver.getFilter());
        receivers.put(clazz, receiver);
    }

    public static void unregisterReceiver(Context context){
        Class<? extends Context> clazz = context.getClass();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receivers.get(clazz));
        receivers.remove(clazz);
    }

}
