package com.kazushiki.pilates

import android.content.ContextWrapper
import com.kazushiki.pilates.data.MemoryStore
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.PlanLibrary
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.model.WorkoutLibrary
import com.kazushiki.pilates.services.PricingText
import com.kazushiki.pilates.services.SubscriptionConfig
import com.kazushiki.pilates.services.SubscriptionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The subscriptions themselves live in Google Play Console: `kazushiki.pilates.monthly`
 * ($12.99/month) and `kazushiki.pilates.annual` ($79.99/year), each with a 7-day free-trial
 * offer on its base plan. These tests cover the store's pure logic.
 */
class SubscriptionTests {
    // The store never touches the context until it talks to Google Play.
    private fun makeStore(store: MemoryStore = MemoryStore()) = SubscriptionStore(ContextWrapper(null), store)

    @Test
    fun trialLabels() {
        assertEquals("7-day", PricingText.trialLabel("P1W"))
        assertEquals("7-day", PricingText.trialLabel("P7D"))
        assertEquals("1-month", PricingText.trialLabel("P1M"))
    }

    @Test
    fun periodWords() {
        assertEquals("month", PricingText.periodWord("P1M"))
        assertEquals("year", PricingText.periodWord("P1Y"))
        assertEquals("week", PricingText.periodWord("P1W"))
        assertEquals("day", PricingText.periodWord("P7D"))
    }

    @Test
    fun annualSavings() {
        assertEquals(48, PricingText.annualSavingsPercent(annualMicros = 79_990_000L, monthlyMicros = 12_990_000L))
        // No savings claim when the annual plan isn't cheaper.
        assertNull(PricingText.annualSavingsPercent(annualMicros = 200_000_000L, monthlyMicros = 10_000_000L))
    }

    @Test
    fun annualPerMonth() {
        val text = PricingText.perMonth(annualMicros = 79_990_000L, currencyCode = "USD", locale = Locale.US)
        assertEquals("$6.66", text)
    }

    @Test
    fun onlyTheFreeWorkoutIsOpenWithoutPremium() {
        val store = makeStore()
        assertTrue(store.canAccess(WorkoutLibrary.coreWakeUp))
        assertFalse(store.canAccess(WorkoutLibrary.fullBodyFlow))
    }

    @Test
    fun everyPlanStartsWithAFreeWorkout() {
        for (level in ExerciseLevel.entries) {
            val profile = UserProfile(goals = setOf(PilatesGoal.CORE_STRENGTH), level = level)
            val first = PlanLibrary.personalPlan(profile).days.firstOrNull()?.workoutID
            assertTrue("$level plan starts locked", first != null && first in SubscriptionConfig.freeWorkoutIDs)
        }
    }

    @Test
    fun testUnlockIsRememberedInTestBuilds() {
        val defaults = MemoryStore()
        makeStore(defaults).testUnlocked = true
        val reopened = makeStore(defaults)
        assertEquals(SubscriptionStore.testingToolsEnabled, reopened.isPremium)
        assertEquals(SubscriptionStore.testingToolsEnabled, reopened.canAccess(WorkoutLibrary.fullBodyFlow))
    }
}
