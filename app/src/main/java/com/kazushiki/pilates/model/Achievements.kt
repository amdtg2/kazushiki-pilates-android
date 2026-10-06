package com.kazushiki.pilates.model

import com.kazushiki.pilates.data.ActivityStore
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max

// Weekly streak

/** Weeks in a row she hit her weekly goal (her number of workout days). */
data class WeeklyStreak(
    /** Consecutive weeks with the goal met, counting this week once it's met. */
    val weeks: Int,
    /** Workouts done this week. */
    val thisWeek: Int,
    /** Her weekly goal. */
    val target: Int,
    /** The longest run of goal weeks ever. */
    val best: Int,
) {
    val goalMetThisWeek: Boolean get() = thisWeek >= target
    val remainingThisWeek: Int get() = max(0, target - thisWeek)

    /** A short line for Home and the finish screen. */
    val status: String
        get() {
            if (goalMetThisWeek) {
                return if (weeks > 1) "Goal reached. That's $weeks weeks in a row!" else "Weekly goal reached. Nice work!"
            }
            val more = if (remainingThisWeek == 1) "1 more workout" else "$remainingThisWeek more workouts"
            return if (weeks > 0) "$more to make it ${weeks + 1} weeks in a row." else "$more to hit your goal this week."
        }

    companion object {
        fun compute(
            days: List<Instant>,
            target: Int,
            now: Instant = Instant.now(),
            calendar: KPCalendar = KPCalendar.current,
        ): WeeklyStreak {
            val goal = max(1, target)
            // Workouts per week, keyed by the start of each week.
            val counts = mutableMapOf<Instant, Int>()
            for (day in days) {
                val start = calendar.startOfWeek(day)
                counts[start] = (counts[start] ?: 0) + 1
            }
            val thisWeekStart = calendar.startOfWeek(now)
            val thisWeek = counts[thisWeekStart] ?: 0

            // Count back from last week; this week only adds once it's met.
            var weeks = if (thisWeek >= goal) 1 else 0
            var cursor = calendar.addWeeks(thisWeekStart, -1)
            while ((counts[cursor] ?: 0) >= goal) {
                weeks += 1
                cursor = calendar.addWeeks(cursor, -1)
            }

            // Longest run of goal weeks.
            var best = 0
            var run = 0
            var previous: Instant? = null
            for (week in counts.keys.sorted()) {
                if ((counts[week] ?: 0) < goal) continue
                run = if (previous != null && calendar.addWeeks(previous, 1) == week) run + 1 else 1
                best = max(best, run)
                previous = week
            }
            return WeeklyStreak(weeks = weeks, thisWeek = thisWeek, target = goal, best = max(best, weeks))
        }
    }
}

// Achievements

data class Achievement(
    val id: String,
    val title: String,
    val detail: String,
    val systemImage: String,
)

/** Everything achievements are judged on, worked out once from her history. */
data class AchievementStats(
    var workouts: Int = 0,
    var minutes: Int = 0,
    var bestDayStreak: Int = 0,
    var bestWeekStreak: Int = 0,
    var goalWeeks: Int = 0,
    var differentWorkouts: Int = 0,
    var earlyWorkouts: Int = 0,
    var lateWorkouts: Int = 0,
    var finishedChallenges: Int = 0,
    var finishedPrograms: Int = 0,
) {
    companion object {
        fun from(
            sessions: List<WorkoutSession>,
            weeklyTarget: Int,
            now: Instant = Instant.now(),
            calendar: KPCalendar = KPCalendar.current,
        ): AchievementStats {
            val s = AchievementStats()
            s.workouts = sessions.size
            s.minutes = sessions.sumOf { it.minutes }
            s.bestDayStreak = longestDayStreak(sessions.map { it.date }, calendar)
            s.bestWeekStreak = WeeklyStreak.compute(sessions.map { it.date }, weeklyTarget, now, calendar).best

            val perWeek = sessions.groupingBy { calendar.startOfWeek(it.date) }.eachCount()
            s.goalWeeks = perWeek.values.count { it >= max(1, weeklyTarget) }

            val guided = sessions.filter { it.isManual != true }
            s.differentWorkouts = guided.map { it.workoutID }.toSet().size
            s.earlyWorkouts = guided.count { calendar.hour(it.date) < 8 }
            s.lateWorkouts = guided.count { calendar.hour(it.date) >= 21 }

            val doneDays = mutableMapOf<String, MutableSet<Int>>()
            for (session in sessions) {
                val plan = session.planID ?: continue
                val day = session.dayNumber ?: continue
                doneDays.getOrPut(plan) { mutableSetOf() } += day
            }
            fun isFinished(plan: Plan) = (doneDays[plan.id] ?: emptySet<Int>()).containsAll(plan.days.map { it.number })
            s.finishedChallenges = PlanLibrary.challenges.count(::isFinished)
            s.finishedPrograms = (ProgramLibrary.all + ProgramLibrary.focusPrograms).count(::isFinished)
            return s
        }

        fun longestDayStreak(dates: List<Instant>, calendar: KPCalendar): Int {
            val days = dates.map { calendar.startOfDay(it) }.toSet().sorted()
            var best = 0
            var run = 0
            var previous: Instant? = null
            for (day in days) {
                run = if (previous != null && calendar.addDays(previous, 1) == day) run + 1 else 1
                best = max(best, run)
                previous = day
            }
            return best
        }
    }
}

object AchievementLibrary {
    /** In the order they're shown, roughly easiest first. */
    val all: List<Achievement> = listOf(
        Achievement("first-workout", "First Step", "Finish your first workout", "figure.pilates"),
        Achievement("goal-week", "Goal Getter", "Hit your weekly goal", "target"),
        Achievement("day-streak-3", "On a Roll", "Work out 3 days in a row", "flame"),
        Achievement("workouts-5", "High Five", "Finish 5 workouts", "hand.raised.fill"),
        Achievement("minutes-60", "Hour of Power", "Move for 60 minutes in total", "clock.fill"),
        Achievement("early-bird", "Early Bird", "Work out before 8 AM", "sunrise.fill"),
        Achievement("night-owl", "Night Owl", "Work out after 9 PM", "moon.stars.fill"),
        Achievement("workouts-10", "Double Digits", "Finish 10 workouts", "10.circle.fill"),
        Achievement("explorer", "Explorer", "Try 10 different workouts", "map.fill"),
        Achievement("day-streak-7", "Week Warrior", "Work out 7 days in a row", "flame.fill"),
        Achievement("week-streak-4", "Month of Momentum", "Hit your weekly goal 4 weeks in a row", "calendar.badge.checkmark"),
        Achievement("challenge-done", "Challenge Champion", "Finish a challenge", "trophy.fill"),
        Achievement("program-done", "Program Graduate", "Finish a program", "graduationcap.fill"),
        Achievement("workouts-25", "Dedicated", "Finish 25 workouts", "star.fill"),
        Achievement("minutes-500", "500 Club", "Move for 500 minutes in total", "stopwatch.fill"),
        Achievement("workouts-50", "Half Century", "Finish 50 workouts", "medal.fill"),
        Achievement("week-streak-12", "Habit Formed", "Hit your weekly goal 12 weeks in a row", "checkmark.seal.fill"),
        Achievement("day-streak-30", "Unstoppable", "Work out 30 days in a row", "bolt.heart.fill"),
        Achievement("minutes-1000", "1,000 Minutes", "Move for 1,000 minutes in total", "hourglass"),
        Achievement("workouts-100", "Centurion", "Finish 100 workouts", "crown.fill"),
    )

    fun achievement(id: String): Achievement? = all.firstOrNull { it.id == id }

    /** IDs of every achievement these stats have earned. */
    fun earned(s: AchievementStats): Set<String> {
        val ids = mutableSetOf<String>()
        fun award(id: String, condition: Boolean) { if (condition) ids += id }
        award("first-workout", s.workouts >= 1)
        award("workouts-5", s.workouts >= 5)
        award("workouts-10", s.workouts >= 10)
        award("workouts-25", s.workouts >= 25)
        award("workouts-50", s.workouts >= 50)
        award("workouts-100", s.workouts >= 100)
        award("minutes-60", s.minutes >= 60)
        award("minutes-500", s.minutes >= 500)
        award("minutes-1000", s.minutes >= 1000)
        award("day-streak-3", s.bestDayStreak >= 3)
        award("day-streak-7", s.bestDayStreak >= 7)
        award("day-streak-30", s.bestDayStreak >= 30)
        award("goal-week", s.goalWeeks >= 1)
        award("week-streak-4", s.bestWeekStreak >= 4)
        award("week-streak-12", s.bestWeekStreak >= 12)
        award("explorer", s.differentWorkouts >= 10)
        award("early-bird", s.earlyWorkouts >= 1)
        award("night-owl", s.lateWorkouts >= 1)
        award("challenge-done", s.finishedChallenges >= 1)
        award("program-done", s.finishedPrograms >= 1)
        return ids
    }

    fun earned(
        sessions: List<WorkoutSession>,
        weeklyTarget: Int,
        now: Instant = Instant.now(),
        calendar: KPCalendar = KPCalendar.current,
    ): Set<String> = earned(AchievementStats.from(sessions, weeklyTarget, now, calendar))

    /** Achievements a new session unlocks, in display order. */
    fun newlyEarned(
        session: WorkoutSession,
        history: List<WorkoutSession>,
        weeklyTarget: Int,
        now: Instant = Instant.now(),
        calendar: KPCalendar = KPCalendar.current,
    ): List<Achievement> {
        val before = earned(history, weeklyTarget, now, calendar)
        val after = earned(history + session, weeklyTarget, now, calendar)
        return all.filter { it.id in after && it.id !in before }
    }
}

// Encouragement

/** The upbeat message on the finish screen, picked to fit the moment. */
data class Encouragement(val title: String, val message: String) {
    data class Context(
        val totalWorkouts: Int,
        val dayStreak: Int,
        val weekly: WeeklyStreak,
        /** True when this workout is the one that met the weekly goal. */
        val justMetWeeklyGoal: Boolean,
        val planTitle: String?,
        val dayNumber: Int?,
        val planLength: Int?,
        val isManual: Boolean = false,
    )

    companion object {
        fun make(c: Context, seed: Int): Encouragement {
            if (c.totalWorkouts == 1) {
                return Encouragement("Your first workout!", "This is where it starts. You showed up for yourself today, and that's the hardest part.")
            }
            if (c.planTitle != null && c.dayNumber != null && c.planLength != null && c.dayNumber == c.planLength) {
                return Encouragement("You did it!", "You finished ${c.planTitle}. ${c.planLength} days of showing up. Be proud of that.")
            }
            if (c.justMetWeeklyGoal) {
                return if (c.weekly.weeks > 1) {
                    Encouragement("${c.weekly.weeks} weeks strong!", "Weekly goal reached again. This is what consistency looks like.")
                } else {
                    Encouragement("Weekly goal reached!", "You did what you set out to do this week. Everything from here is a bonus.")
                }
            }
            if (c.dayStreak in listOf(3, 7, 14, 21, 30, 50, 100)) {
                return Encouragement("${c.dayStreak} days in a row!", "Your streak is growing. Your body notices every one of these days.")
            }
            if (c.planTitle != null && c.dayNumber != null && c.planLength != null &&
                (c.dayNumber * 2 == c.planLength || c.dayNumber * 2 == c.planLength + 1)
            ) {
                return Encouragement("Halfway there!", "Day ${c.dayNumber} of ${c.planLength} in ${c.planTitle}. The second half is where it all comes together.")
            }
            val pool = if (c.isManual) loggedPool else generalPool
            return pool[abs(seed) % pool.size]
        }

        val generalPool = listOf(
            Encouragement("Beautiful work", "You moved with intention today. That's what Pilates is all about."),
            Encouragement("You showed up", "Some days that's the whole win, and today you did more than that."),
            Encouragement("Stronger already", "Every session builds strength you'll feel in the way you stand, walk and breathe."),
            Encouragement("Look at you go", "Another workout done. Your future self says thank you."),
            Encouragement("That's how it's done", "Steady, controlled and consistent. You're building a habit that lasts."),
            Encouragement("Feel that?", "That's your core waking up. Take a breath and enjoy it."),
            Encouragement("Proud of you", "You chose yourself today. Keep that energy for the rest of your day."),
            Encouragement("One more for the books", "Small sessions add up to big changes. You're on your way."),
        )

        val loggedPool = listOf(
            Encouragement("Logged and counted", "Every bit of movement matters. Your streak thanks you."),
            Encouragement("Nice work out there", "Training your own way still counts. Keep it up."),
            Encouragement("Movement is movement", "However you moved today, you showed up. That's what keeps a habit going."),
        )
    }
}

/** Everything the finish screen shows about a just-finished workout. */
data class CelebrationSummary(
    val encouragement: Encouragement,
    val minutes: Int,
    val exercises: Int?,
    val dayStreak: Int,
    val weekly: WeeklyStreak,
    val newAchievements: List<Achievement>,
    /** e.g. "Day 5 of 30 · 30-Day Pilates Challenge" */
    val planProgress: String?,
) {
    companion object {
        fun make(
            session: WorkoutSession,
            history: List<WorkoutSession>,
            profile: UserProfile,
            exercises: Int? = null,
            now: Instant = Instant.now(),
            calendar: KPCalendar = KPCalendar.current,
        ): CelebrationSummary {
            val target = profile.sessionsPerWeek
            val all = history + session
            val before = WeeklyStreak.compute(history.map { it.date }, target, now, calendar)
            val after = WeeklyStreak.compute(all.map { it.date }, target, now, calendar)
            val dayStreak = ActivityStore.streak(all.map { it.date }, now, calendar)
            val plan = session.planID?.let { PlanLibrary.plan(it, profile) }

            val context = Encouragement.Context(
                totalWorkouts = all.size,
                dayStreak = dayStreak,
                weekly = after,
                justMetWeeklyGoal = !before.goalMetThisWeek && after.goalMetThisWeek,
                planTitle = plan?.title,
                dayNumber = session.dayNumber,
                planLength = plan?.days?.size,
                isManual = session.isManual == true,
            )
            val progress = if (plan != null && session.dayNumber != null) "Day ${session.dayNumber} of ${plan.days.size} · ${plan.title}" else null
            return CelebrationSummary(
                encouragement = Encouragement.make(context, all.size),
                minutes = session.minutes,
                exercises = exercises,
                dayStreak = dayStreak,
                weekly = after,
                newAchievements = AchievementLibrary.newlyEarned(session, history, target, now, calendar),
                planProgress = progress,
            )
        }
    }
}
