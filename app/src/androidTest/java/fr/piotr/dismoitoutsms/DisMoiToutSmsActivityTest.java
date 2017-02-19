package fr.piotr.dismoitoutsms;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by piotr_000 on 30/07/2016.
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DisMoiToutSmsActivityTest {

    @Rule
    public ActivityTestRule<DisMoiToutSmsActivity> mActivityRule = new ActivityTestRule<>(
            DisMoiToutSmsActivity.class);

    @Test
    public void changeText_sameActivity() {
        // Type text and then press the button.
        onView(withId(R.id.btn_activate)).perform(click());

        // Check that the text was changed.
        onView(withId(R.id.status_text)).check(ViewAssertions.matches(withText(R.string.activated)));

        onView(withId(R.id.btn_deactivate)).perform(click());

        // Check that the text was changed.
        onView(withId(R.id.status_text)).check(ViewAssertions.matches(withText(R.string.deactivated)));

        onView(withId(R.id.status_icon)).perform(click());

        // Check that the text was changed.
        onView(withId(R.id.status_text)).check(ViewAssertions.matches(withText(R.string.activated)));

        onView(withId(R.id.status_icon)).perform(click());

        // Check that the text was changed.
        onView(withId(R.id.status_text)).check(ViewAssertions.matches(withText(R.string.deactivated)));


    }

}
