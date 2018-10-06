package fr.piotr.dismoitoutsms

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.piotr.dismoitoutsms.reception.SmsReceiver
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SmsReception {

    @Rule
    @JvmField
    var activityActivityTestRule = ActivityTestRule(DisMoiToutSmsActivity::class.java)

    @Before
    fun before() = activityActivityTestRule.activity.onDeactivate()

    @After
    fun after() = activityActivityTestRule.activity.onDeactivate()

    @Test
    fun dictionActivated() {
        onView(withId(R.id.switch_activation))
                .check(ViewAssertions.matches(isNotChecked()))
                .perform(ViewActions.click())

        SmsReceiver.getInstance().onSmsReceived(activityActivityTestRule.activity, "0123456789", "hello")

        onView(withId(R.id.message))
                .check(ViewAssertions.matches(isDisplayed()))
                .check(ViewAssertions.matches(withText("hello")))
    }


//    @Test
//    fun dictionNotActivated() {
//        onView(withId(R.id.switch_activation))
//                .check(ViewAssertions.matches(isNotChecked()))
//
//        SmsReceiver.getInstance().onSmsReceived(activityActivityTestRule.activity, "0123456789", "hello")
//
//        onView(withId(R.id.message))
//                .check(ViewAssertions.matches(CoreMatchers.not(isDisplayed())))
//    }

}