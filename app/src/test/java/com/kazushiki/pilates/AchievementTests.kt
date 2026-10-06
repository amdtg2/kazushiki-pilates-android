package com.kazushiki.pilates

import com.kazushiki.pilates.model.AchievementLibrary
import com.kazushiki.pilates.model.AchievementStats
import com.kazushiki.pilates.model.CelebrationSummary
import com.kazushiki.pilates.model.Encouragement
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.PlanLibrary
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.model.WeeklyStreak
import com.kazushiki.pilates.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class AchievementTests {
    private val calendar = KPCalendar(zone = ZoneId.of("America/Chicago"), firstWeekday = 1, locale = Locale.US)

    /** Wednesday 7 October 2026, 10 AM. */
    private val now: Instant get() = calendar.date(2026, 10, 7, 10)

    private fun daysAgo(days: Int, hour: Int = 10): Instant {
        val day = calendar.addDays(now, -days.toLong())
        return calendar.atTime(day, hour, 0)
    }

    private fun session(date: Instant, id: String = "core-wake-up", minutes: Int = 15, plan: String? = null, day: Int? = null): WorkoutSession =
        WorkoutSession(date = date, workoutID = id, workoutTitle = id, planID = plan, dayNumber = day, seconds = (minutes * 60).toDouble())

    // Weekly streak

    @Test
    fun unfinishedWeekDoesNotBreakTheStreak() {
        // The week of 20 Sep and last week (27 Sep – 3 Oct) both hit 3; this week has 1 so far.
        val days = listOf(13, 12, 11, 6, 5, 4, 1).map { daysAgo(it) }
        val streak = WeeklyStreak.compute(days, 3, now, calendar)
        assertEquals(1, streak.thisWeek)
        assertEquals(2, streak.weeks)
        assertFalse(streak.goalMetThisWeek)
        assertEquals(2, streak.remainingThisWeek)
        assertEquals("2 more workouts to make it 3 weeks in a row.", streak.status)
    }

    @Test
    fun meetingTheGoalAddsThisWeek() {
        // Last week hit 3, and this week (from Sun 4 Oct) just hit 3 too.
        val days = listOf(6, 5, 4, 3, 2, 1).map { daysAgo(it) }
        val streak = WeeklyStreak.compute(days, 3, now, calendar)
        assertEquals(3, streak.thisWeek)
        assertEquals(2, streak.weeks)
        assertTrue(streak.goalMetThisWeek)
        assertEquals("Goal reached. That's 2 weeks in a row!", streak.status)
    }

    @Test
    fun aMissedWeekResetsTheStreakButKeepsTheBest() {
        // Three goal weeks, then a week with nothing, then this week.
        val old = listOf(31, 30, 29, 24, 23, 22, 17, 16, 15).map { daysAgo(it) }
        val streak = WeeklyStreak.compute(old + listOf(daysAgo(0)), 3, now, calendar)
        assertEquals(0, streak.weeks)
        assertEquals(3, streak.best)
        assertEquals("2 more workouts to hit your goal this week.", streak.status)
    }

    // Achievements

    @Test
    fun firstWorkoutEarnsFirstStep() {
        val new = AchievementLibrary.newlyEarned(session(daysAgo(0)), emptyList(), 3, now, calendar)
        assertEquals(listOf("first-workout"), new.map { it.id })
    }

    @Test
    fun achievementsAreEarnedOnlyOnce() {
        val history = (1..4).map { session(daysAgo(it)) }
        val fifth = AchievementLibrary.newlyEarned(session(daysAgo(0)), history, 3, now, calendar)
        assertTrue(fifth.any { it.id == "workouts-5" })
        assertFalse(fifth.any { it.id == "first-workout" })
        // Five days in a row includes the 3-day streak, earned earlier.
        assertFalse(fifth.any { it.id == "day-streak-3" })
    }

    @Test
    fun timeOfDayAndStreakBadges() {
        val sessions = listOf(session(daysAgo(2, hour = 6)), session(daysAgo(1, hour = 22)), session(daysAgo(0)))
        val earned = AchievementLibrary.earned(sessions = sessions, weeklyTarget = 3, now = now, calendar = calendar)
        assertTrue(earned.containsAll(setOf("first-workout", "early-bird", "night-owl", "day-streak-3")))
        assertFalse("workouts-5" in earned)
    }

    @Test
    fun finishingAChallengeIsCelebrated() {
        val challenge = PlanLibrary.sevenDayCore
        val sessions = challenge.days.map { day ->
            session(daysAgo(7 - day.number), id = day.workoutID, plan = challenge.id, day = day.number)
        }
        val earned = AchievementLibrary.earned(sessions = sessions, weeklyTarget = 3, now = now, calendar = calendar)
        assertTrue("challenge-done" in earned)
        assertTrue("day-streak-7" in earned)
    }

    @Test
    fun everyAchievementHasAUniqueID() {
        val ids = AchievementLibrary.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // Every ID can actually be awarded.
        val maxed = AchievementStats()
        maxed.workouts = 1000
        maxed.minutes = 100_000
        maxed.bestDayStreak = 100
        maxed.bestWeekStreak = 100
        maxed.goalWeeks = 100
        maxed.differentWorkouts = 100
        maxed.earlyWorkouts = 1
        maxed.lateWorkouts = 1
        maxed.finishedChallenges = 1
        maxed.finishedPrograms = 1
        assertEquals(ids.toSet(), AchievementLibrary.earned(maxed))
    }

    // Encouragement

    @Test
    fun messagesFitTheMoment() {
        val profile = UserProfile(trainingDays = setOf(2, 4, 6))

        val first = CelebrationSummary.make(session = session(now), history = emptyList(), profile = profile, now = now, calendar = calendar)
        assertEquals("Your first workout!", first.encouragement.title)
        assertEquals("first-workout", first.newAchievements.firstOrNull()?.id)

        // Third workout this week meets the goal of 3.
        val history = listOf(session(daysAgo(2)), session(daysAgo(1)))
        val goal = CelebrationSummary.make(session = session(now), history = history, profile = profile, now = now, calendar = calendar)
        assertEquals("Weekly goal reached!", goal.encouragement.title)
        assertTrue(goal.weekly.goalMetThisWeek)

        // The last day of a challenge.
        val challenge = PlanLibrary.sevenDayCore
        val last = CelebrationSummary.make(
            session = session(now, plan = challenge.id, day = 7), history = listOf(session(daysAgo(30))),
            profile = profile, now = now, calendar = calendar,
        )
        assertEquals("You did it!", last.encouragement.title)
        assertEquals("Day 7 of 7 · 7-Day Core Challenge", last.planProgress)
    }

    @Test
    fun everydayMessagesVary() {
        val titles = (0 until Encouragement.generalPool.size).map { seed ->
            Encouragement.make(
                Encouragement.Context(
                    totalWorkouts = 12, dayStreak = 1, weekly = WeeklyStreak(weeks = 0, thisWeek = 1, target = 3, best = 0),
                    justMetWeeklyGoal = false, planTitle = null, dayNumber = null, planLength = null,
                ),
                seed,
            ).title
        }.toSet()
        assertEquals(Encouragement.generalPool.size, titles.size)
    }
}
