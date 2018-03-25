package fr.piotr.dismoitoutsms.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewAnimationUtils;

/**
 * Created by piotr on 21/12/2017.
 *
 */

public class AnimUtils {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void reveal(View myView){
        reveal(myView, () -> {
            //DO NOTHING
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void reveal(View myView, Runnable onEnd){

        // get the center for the clipping circle
        int cx = myView.getWidth();
        int cy = myView.getHeight();

        // get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        // create the animator for this view (the start radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onEnd.run();
            }
        });

        // make the view visible and start the animation
        myView.setVisibility(View.VISIBLE);
        if(myView.isAttachedToWindow()) {
            anim.start();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void unreveal(View myView) {
        unreveal(myView, () -> {
            //DO NOTHING
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void unreveal(View myView, Runnable onEnd) {

        if(myView==null){
            return;
        }

        // get the center for the clipping circle
        int cx = myView.getWidth();
        int cy = myView.getHeight();

        // get the initial radius for the clipping circle
        float initialRadius = (float) Math.hypot(cx, cy);

        // create the animation (the final radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                myView.setVisibility(View.GONE);
                onEnd.run();
            }
        });

        if(myView.isAttachedToWindow())
        // start the animation
        {
            anim.start();
        }
    }

}
