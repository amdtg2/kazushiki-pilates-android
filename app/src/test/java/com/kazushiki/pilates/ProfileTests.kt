package com.kazushiki.pilates

import com.kazushiki.pilates.data.AppJson
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.PlanLibrary
import com.kazushiki.pilates.model.PlanSuggestion
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.exercises.ExerciseLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class ProfileTests {
    private fun gregorian(firstWeekday: Int = 1): KPCalendar =
        KPCalendar(zone = ZoneId.of("America/Chicago"), firstWeekday = firstWeekday, locale = Locale.US)

    @Test
    fun profileNeedsAGoalBeforeItIsReady() {
        var profile = UserProfile()
        assertFalse(profile.isReady)
        profile = profile.copy(goals = setOf(PilatesGoal.POSTURE))
        assertTrue(profile.isReady)
    }

    @Test
    fun matIsAlwaysIncluded() {
        assertTrue(Equipment.MAT in UserProfile().equipment)
    }

    @Test
    fun planIsNamedAfterTheFirstGoalInDisplayOrder() {
        val profile = UserProfile(goals = setOf(PilatesGoal.FLEXIBILITY, PilatesGoal.CORE_STRENGTH))
        val plan = PlanSuggestion.make(profile)
        assertEquals("Core Foundations", plan.title)
        assertEquals(listOf("Core strength", "Flexibility"), plan.focus)
    }

    @Test
    fun sessionsPerWeekFollowLevel() {
        val profile = UserProfile(goals = setOf(PilatesGoal.TONE), minutesPerDay = 20, level = ExerciseLevel.ADVANCED)
        val plan = PlanSuggestion.make(profile)
        assertEquals(5, plan.sessionsPerWeek)
        assertEquals(20, plan.minutesPerSession)
    }

    @Test
    fun profileSurvivesEncoding() {
        val profile = UserProfile(
            goals = setOf(PilatesGoal.RECOVERY, PilatesGoal.TONE),
            equipment = setOf(Equipment.MAT, Equipment.BAND, Equipment.WALL),
            reminderHour = 19,
        )
        val data = AppJson.encodeToString(UserProfile.serializer(), profile)
        assertEquals(profile, AppJson.decodeFromString(UserProfile.serializer(), data))
    }

    @Test
    fun chosenDaysPerWeekOverrideTheRecommendation() {
        var profile = UserProfile(goals = setOf(PilatesGoal.TONE))
        assertEquals(3, PlanSuggestion.make(profile).sessionsPerWeek)
        profile = profile.copy(daysPerWeek = 5)
        assertEquals(5, PlanSuggestion.make(profile).sessionsPerWeek)
        assertEquals(20, PlanLibrary.personalPlan(profile).days.size)
    }

    @Test
    fun profilesSavedBeforeDaysPerWeekStillLoad() {
        val old = """{"goals":["tone"],"level":"beginner","equipment":["mat"],"minutesPerDay":15,"remindersOn":true,"reminderHour":8,"reminderMinute":0}"""
        val profile = AppJson.decodeFromString(UserProfile.serializer(), old)
        assertNull(profile.daysPerWeek)
        assertEquals(3, profile.sessionsPerWeek)
    }

    @Test
    fun pickedWorkoutDaysSetTheWeeklyPlan() {
        var profile = UserProfile(goals = setOf(PilatesGoal.TONE), trainingDays = setOf(2, 4, 6, 7))
        assertEquals(4, profile.sessionsPerWeek)
        assertEquals(16, PlanLibrary.personalPlan(profile).days.size)

        val calendar = gregorian()
        assertEquals("Mon, Wed, Fri and Sat", profile.trainingDaysText(calendar))
        assertEquals("on Mon, Wed, Fri and Sat", profile.trainingDaysPhrase(calendar))
        profile = profile.copy(trainingDays = (1..7).toSet())
        assertEquals("every day", profile.trainingDaysPhrase(calendar))

        // An empty pick falls back to the default spread.
        profile = profile.copy(trainingDays = emptySet())
        assertEquals(3, profile.sessionsPerWeek)
    }

    @Test
    fun defaultWorkoutDaysMatchTheCount() {
        for (count in 1..7) {
            val days = UserProfile.defaultTrainingDays(count)
            assertEquals(count, days.size)
            assertTrue(days.all { it in 1..7 })
        }
    }

    @Test
    fun profilesSavedBeforeWorkoutDaysStillLoad() {
        val old = """{"goals":["tone"],"level":"beginner","equipment":["mat"],"minutesPerDay":15,"daysPerWeek":4,"remindersOn":true,"reminderHour":8,"reminderMinute":0}"""
        val profile = AppJson.decodeFromString(UserProfile.serializer(), old)
        assertNull(profile.trainingDays)
        assertEquals(setOf(2, 3, 5, 7), profile.effectiveTrainingDays)

        val updated = profile.copy(trainingDays = setOf(1, 3))
        val data = AppJson.encodeToString(UserProfile.serializer(), updated)
        assertEquals(setOf(1, 3), AppJson.decodeFromString(UserProfile.serializer(), data).trainingDays)
    }

    @Test
    fun weekStartsOnTheCalendarsFirstDay() {
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 1), UserProfile.orderedWeekdays(gregorian(firstWeekday = 2)))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), UserProfile.orderedWeekdays(gregorian(firstWeekday = 1)))
    }
}
