package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PilatesGoal(val title: String, val detail: String, val systemImage: String) {
    @SerialName("coreStrength") CORE_STRENGTH("Core strength", "A stronger, steadier center", "figure.core.training"),
    @SerialName("tone") TONE("Tone and sculpt", "Firm up glutes, legs and arms", "figure.strengthtraining.functional"),
    @SerialName("posture") POSTURE("Better posture", "Stand taller, ease desk-day stiffness", "figure.stand"),
    @SerialName("flexibility") FLEXIBILITY("Flexibility", "Longer, looser muscles", "figure.flexibility"),
    @SerialName("recovery") RECOVERY("Gentle recovery", "Low-impact movement for rest days or after a break", "leaf"),
}

@Serializable
data class UserProfile(
    val goals: Set<PilatesGoal> = emptySet(),
    val level: ExerciseLevel = ExerciseLevel.BEGINNER,
    /** Always contains MAT: every workout has a mat-only version. */
    val equipment: Set<Equipment> = setOf(Equipment.MAT),
    val minutesPerDay: Int = 15,
    val remindersOn: Boolean = true,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    /** Workouts per week the user chose. null means use the level's recommendation. */
    val daysPerWeek: Int? = null,
    /** The weekdays she plans to work out (1 = Sunday … 7 = Saturday). null until she picks. */
    val trainingDays: Set<Int>? = null,
) {
    /** Recommended workouts per week for the user's level. */
    val recommendedDaysPerWeek: Int
        get() = when (level) {
            ExerciseLevel.BEGINNER -> 3
            ExerciseLevel.INTERMEDIATE -> 4
            ExerciseLevel.ADVANCED -> 5
        }

    val sessionsPerWeek: Int get() = effectiveTrainingDays.size

    /** Her chosen workout days, or a sensible spread for her days-per-week until she picks. */
    val effectiveTrainingDays: Set<Int>
        get() = if (!trainingDays.isNullOrEmpty()) trainingDays else defaultTrainingDays(daysPerWeek ?: recommendedDaysPerWeek)

    /** "Mon, Wed and Fri", in the order of the user's week. */
    fun trainingDaysText(calendar: KPCalendar = KPCalendar.current): String {
        val names = orderedWeekdays(calendar).filter { it in effectiveTrainingDays }.map { calendar.shortWeekdaySymbol(it) }
        if (names.size == 7) return "every day"
        if (names.size <= 1) return names.firstOrNull() ?: ""
        return names.dropLast(1).joinToString(", ") + " and " + names.last()
    }

    /** "on Mon, Wed and Fri", or "every day". */
    fun trainingDaysPhrase(calendar: KPCalendar = KPCalendar.current): String =
        if (effectiveTrainingDays.size == 7) "every day" else "on " + trainingDaysText(calendar)

    val isReady: Boolean get() = goals.isNotEmpty()

    /** The first selected goal in display order; it names the plan. */
    val primaryGoal: PilatesGoal? get() = PilatesGoal.entries.firstOrNull { it in goals }

    /** "8:00 AM" */
    fun reminderTimeText(): String {
        val time = java.time.LocalTime.of(reminderHour, reminderMinute)
        return time.format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT))
    }

    companion object {
        val minuteOptions = listOf(10, 15, 20, 30)
        val dayOptions = listOf(2, 3, 4, 5, 6)

        /** Evenly spread workout days, starting Monday. */
        fun defaultTrainingDays(count: Int): Set<Int> = when {
            count <= 1 -> setOf(2)
            count == 2 -> setOf(2, 5)
            count == 3 -> setOf(2, 4, 6)
            count == 4 -> setOf(2, 3, 5, 7)
            count == 5 -> setOf(2, 3, 4, 5, 6)
            count == 6 -> setOf(2, 3, 4, 5, 6, 7)
            else -> (1..7).toSet()
        }

        /** Weekday numbers in the order the user's calendar starts the week. */
        fun orderedWeekdays(calendar: KPCalendar = KPCalendar.current): List<Int> =
            (0 until 7).map { (calendar.firstWeekday - 1 + it) % 7 + 1 }
    }
}

/** The starter plan (kept for the personal plan; no longer shown as a program). */
data class PlanSuggestion(
    val title: String,
    val weeks: Int,
    val sessionsPerWeek: Int,
    val minutesPerSession: Int,
    val focus: List<String>,
) {
    companion object {
        fun make(profile: UserProfile): PlanSuggestion {
            val title = when (profile.primaryGoal) {
                PilatesGoal.CORE_STRENGTH -> "Core Foundations"
                PilatesGoal.TONE -> "Sculpt and Tone"
                PilatesGoal.POSTURE -> "Stand Tall"
                PilatesGoal.FLEXIBILITY -> "Long and Loose"
                PilatesGoal.RECOVERY -> "Gentle Reset"
                null -> "Pilates Essentials"
            }
            return PlanSuggestion(
                title = title, weeks = 4, sessionsPerWeek = profile.sessionsPerWeek,
                minutesPerSession = profile.minutesPerDay,
                focus = PilatesGoal.entries.filter { it in profile.goals }.map { it.title },
            )
        }
    }
}
