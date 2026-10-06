package com.kazushiki.pilates.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kazushiki.pilates.model.AchievementLibrary
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.Plan
import com.kazushiki.pilates.model.PlanDay
import com.kazushiki.pilates.model.PlanLibrary
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.model.WeeklyStreak
import com.kazushiki.pilates.model.WorkoutSession
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.time.Instant

/** Completed workouts, saved on device. Plan progress and streaks are derived from these. */
class ActivityStore(private val store: KeyValueStore = MemoryStore()) {
    var sessions: List<WorkoutSession> by mutableStateOf(emptyList())
        private set

    /** Programs she has joined, most recent first. Finished ones stay here until she leaves them. */
    var enrolledPlanIDs: List<String> by mutableStateOf(emptyList())
        private set

    init {
        val loaded = store.getString(KEY)?.let {
            runCatching { AppJson.decodeFromString(ListSerializer(WorkoutSession.serializer()), it) }.getOrNull()
        } ?: emptyList()

        val saved = store.getString(ENROLLED_KEY)?.let {
            runCatching { AppJson.decodeFromString(ListSerializer(String.serializer()), it) }.getOrNull()
        }
        enrolledPlanIDs = if (saved != null) {
            // The old starter plan is no longer something she joins.
            saved.filter { it != PlanLibrary.personalPlanID }
        } else {
            // First launch with enrollment: count programs she already started (newest first).
            loaded.sortedByDescending { it.date }
                .mapNotNull { it.planID }
                .filter { it != PlanLibrary.personalPlanID }
                .distinct()
        }
        sessions = loaded
    }

    fun log(session: WorkoutSession) {
        sessions = sessions + session
        saveSessions()
        // Doing a program's workout joins it (and brings it to the top).
        val planID = session.planID
        if (planID != null && planID != PlanLibrary.personalPlanID) enroll(planID)
    }

    /** Removes a session, e.g. a workout logged by mistake. */
    fun delete(session: WorkoutSession) {
        sessions = sessions.filter { it.id != session.id }
        saveSessions()
    }

    fun resetAll() {
        sessions = emptyList()
        saveSessions()
        enrolledPlanIDs = emptyList()
        saveEnrolled()
    }

    // Enrollment

    fun isEnrolled(planID: String): Boolean = planID in enrolledPlanIDs

    /** Joins a program, or moves it to the top if she's already in it. */
    fun enroll(planID: String) {
        if (planID == PlanLibrary.personalPlanID || enrolledPlanIDs.firstOrNull() == planID) return
        enrolledPlanIDs = listOf(planID) + enrolledPlanIDs.filter { it != planID }
        saveEnrolled()
    }

    /** Leaves a program. Workouts she already did stay in her history. */
    fun leave(planID: String) {
        enrolledPlanIDs = enrolledPlanIDs.filter { it != planID }
        saveEnrolled()
    }

    /** Joined programs that still have workouts left, most recent first. */
    fun activePrograms(profile: UserProfile): List<Plan> =
        enrolledPlanIDs.mapNotNull { PlanLibrary.plan(it, profile) }.filter { nextDay(it) != null }

    /** The most recently joined program she has finished, if any. */
    fun lastFinishedProgram(profile: UserProfile): Plan? =
        enrolledPlanIDs.mapNotNull { PlanLibrary.plan(it, profile) }.firstOrNull { nextDay(it) == null }

    // Plans

    fun completedDays(planID: String): Set<Int> =
        sessions.filter { it.planID == planID }.mapNotNull { it.dayNumber }.toSet()

    /** The first day of the plan not yet completed, or null when the plan is finished. */
    fun nextDay(plan: Plan): PlanDay? {
        val done = completedDays(plan.id)
        return plan.days.firstOrNull { it.number !in done }
    }

    fun isUnlocked(day: PlanDay, plan: Plan): Boolean {
        if (!plan.isChallenge || day.number <= 1) return true
        return (day.number - 1) in completedDays(plan.id)
    }

    // Stats

    val totalMinutes: Int get() = sessions.sumOf { it.minutes }

    fun streak(now: Instant = Instant.now(), calendar: KPCalendar = KPCalendar.current): Int =
        streak(sessions.map { it.date }, now, calendar)

    /** Weeks in a row with her weekly goal met. */
    fun weeklyStreak(target: Int, now: Instant = Instant.now(), calendar: KPCalendar = KPCalendar.current): WeeklyStreak =
        WeeklyStreak.compute(sessions.map { it.date }, target, now, calendar)

    /** IDs of achievements she has earned. */
    fun earnedAchievements(weeklyTarget: Int): Set<String> = AchievementLibrary.earned(sessions, weeklyTarget)

    fun activeDays(calendar: KPCalendar = KPCalendar.current): Set<Instant> =
        sessions.map { calendar.startOfDay(it.date) }.toSet()

    fun sessionsInWeekOf(date: Instant = Instant.now(), calendar: KPCalendar = KPCalendar.current): List<WorkoutSession> {
        val start = calendar.startOfWeek(date)
        val end = calendar.addWeeks(start, 1)
        return sessions.filter { !it.date.isBefore(start) && it.date.isBefore(end) }
    }

    private fun saveSessions() {
        store.putString(KEY, AppJson.encodeToString(ListSerializer(WorkoutSession.serializer()), sessions))
    }

    private fun saveEnrolled() {
        store.putString(ENROLLED_KEY, AppJson.encodeToString(ListSerializer(String.serializer()), enrolledPlanIDs))
    }

    companion object {
        const val KEY = "kp.sessions"
        const val ENROLLED_KEY = "kp.enrolledPlans"

        /** Consecutive days with at least one session, ending today (or yesterday, so a
         *  streak isn't shown as broken before today's workout). */
        fun streak(days: List<Instant>, now: Instant, calendar: KPCalendar): Int {
            val active = days.map { calendar.startOfDay(it) }.toSet()
            var day = calendar.startOfDay(now)
            if (day !in active) day = calendar.addDays(day, -1)
            var count = 0
            while (day in active) {
                count += 1
                day = calendar.addDays(day, -1)
            }
            return count
        }
    }
}
