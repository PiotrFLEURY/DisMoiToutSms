package fr.piotr.dismoitoutsms.reception;

import android.content.Context;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static java.lang.System.currentTimeMillis;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by piotr_000 on 12/03/2016.
 */
public class StepListenerTest {

    @Mock
    Context context;

    @Spy
    StepListener stepListener = new StepListener(context);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        //doNothing().when(stepListener).notifyStep();
    }

    @Test
    public void clean() {
        //GIVEN
        Long aMinuteAgo = System.currentTimeMillis() - 60000;
        Long now = System.currentTimeMillis();

        //WHEN
        stepListener.stepDetected(aMinuteAgo);
        stepListener.clean(now);

        //THEN
        Assert.assertEquals(0, stepListener.steps.size());
    }

    @Test
    public void nidsDePoule(){
        //GIVEN
        int eventCount=15;

        //WHEN
        while(eventCount>0) {
            stepListener.stepDetected(currentTimeMillis());
            eventCount--;
        }

        //THEN
        verify(stepListener, never()).stopService();
    }

    @Test
    public void marche_lente() throws Exception{
        //GIVEN
        int eventCount=15;

        //WHEN
        while(eventCount>0) {
            stepListener.stepDetected(currentTimeMillis());
            Thread.sleep(1000);
            eventCount--;
        }

        //THEN
        verify(stepListener).stopService();
    }

    @Test
    public void marche_normale() throws Exception{
        //GIVEN
        int eventCount=15;

        //WHEN
        while(eventCount>0) {
            stepListener.stepDetected(currentTimeMillis());
            Thread.sleep(500);
            eventCount--;
        }

        //THEN
        verify(stepListener).stopService();
    }

}