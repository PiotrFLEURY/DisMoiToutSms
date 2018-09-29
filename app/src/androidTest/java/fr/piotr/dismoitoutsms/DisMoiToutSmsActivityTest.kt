package fr.piotr.dismoitoutsms

import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4

import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.COMMANDE_VOCALE
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.PRIVATE_LIFE_MODE
import fr.piotr.dismoitoutsms.util.ConfigurationManager.Configuration.UNIQUEMENT_CONTACTS
import fr.piotr.dismoitoutsms.util.ConfigurationManager.getBoolean
import fr.piotr.dismoitoutsms.util.ConfigurationManager.setBoolean

@LargeTest
@RunWith(AndroidJUnit4::class)
class DisMoiToutSmsActivityTest {

    @Rule
    @JvmField
    var activityActivityTestRule = ActivityTestRule(DisMoiToutSmsActivity::class.java)

    private var startTime = System.currentTimeMillis()

    @Before
    fun init() {
        setBoolean(activityActivityTestRule.activity, UNIQUEMENT_CONTACTS, false)
        setBoolean(activityActivityTestRule.activity, COMMANDE_VOCALE, false)
        setBoolean(activityActivityTestRule.activity, PRIVATE_LIFE_MODE, false)
    }

    @Test
    fun startTime() {
        val startedAt = System.currentTimeMillis()

        Assert.assertThat(java.lang.Long.valueOf(startedAt - startTime), Matchers.lessThan(java.lang.Long.valueOf(2000)))
    }

    @Test
    fun toggleOnlyContacts() {

        onView(withId(R.id.switch_uniquement_mes_contacts))
                .check(matches(isDisplayed()))
                .check(matches(isChecked()))

//        onView(withId(R.id.switch_uniquement_mes_contacts))
//                .check(matches(isNotChecked()))
//                .perform(click())
//                .check(matches(isChecked()))

        Assert.assertEquals(true, getBoolean(activityActivityTestRule.activity, UNIQUEMENT_CONTACTS))

    }

    @Test
    fun toggleVocalAnswer() {

        onView(withId(R.id.switch_reponse_vocale))
                .check(matches(isNotChecked()))
                .perform(click())
                .check(matches(isChecked()))

        Assert.assertEquals(true, getBoolean(activityActivityTestRule.activity, COMMANDE_VOCALE))

    }

    @Test
    fun togglePrivateLife() {

        onView(withId(R.id.switch_private_life_mode))
                .check(matches(isNotChecked()))
                .perform(click())
                .check(matches(isChecked()))

        Assert.assertEquals(true, getBoolean(activityActivityTestRule.activity, PRIVATE_LIFE_MODE))

    }
}
