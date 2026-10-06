package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.BodyArea
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.exercises.ExerciseVariation
import kotlin.math.ceil
import kotlin.math.max

/** One exercise inside a workout. */
data class WorkoutSlot(
    val exerciseID: String,
    val variationID: String,
    /** How many times the exercise's sequence plays (reps for the bridge, 1 for the Hundred). */
    val loops: Int,
    /** Exercise IDs to use instead when the user lacks this one's equipment. */
    val alternatives: List<String> = emptyList(),
)

data class Workout(
    val id: String,
    val title: String,
    val summary: String,
    val focus: List<BodyArea>,
    val level: ExerciseLevel,
    val slots: List<WorkoutSlot>,
    val restSeconds: Double = 15.0,
    /** Equipment the whole workout depends on (no mat fallback), e.g. a reformer. */
    val requires: Equipment? = null,
    /** Adaptive workouts swap equipment moves for mat moves, so they suit everyone. */
    val isMixed: Boolean = false,
) {
    /** Equipment the workout is built around (the first choice in each slot), without the mat. */
    val equipmentFocus: Set<Equipment>
        get() = slots.mapNotNull { ExerciseLibrary.exercise(it.exerciseID) }.flatMap { it.equipment }.toSet() - Equipment.MAT

    /** Adaptive workouts always suit; equipment-specific ones need that equipment. */
    fun isAvailable(equipment: Set<Equipment>): Boolean {
        if (requires != null && requires !in equipment) return false
        return isMixed || equipment.containsAll(equipmentFocus)
    }
}

/** Picks the slot's exercise, or the first alternative the user has equipment for. */
fun ExerciseLibrary.resolve(slot: WorkoutSlot, equipment: Set<Equipment>): Pair<Exercise, ExerciseVariation>? {
    val owned = equipment + Equipment.MAT
    val candidates = (listOf(slot.exerciseID) + slot.alternatives).mapNotNull { exercise(it) }
    val original = candidates.firstOrNull() ?: return null
    val chosen = candidates.firstOrNull { owned.containsAll(it.equipment) } ?: original
    val variation = chosen.variations.firstOrNull { it.id == slot.variationID } ?: chosen.variations[0]
    return chosen to variation
}

// Timeline

data class WorkoutSegment(
    val kind: Kind,
    val slotIndex: Int,
    val exercise: Exercise,
    val variation: ExerciseVariation,
    val loops: Int,
    val start: Double,
    val duration: Double,
) {
    enum class Kind { EXERCISE, REST }

    val end: Double get() = start + duration
}

/** A workout laid out on a single clock: exercise, rest, exercise, rest, … exercise. */
class WorkoutTimeline(workout: Workout, equipment: Set<Equipment>) {
    val segments: List<WorkoutSegment>

    init {
        val list = mutableListOf<WorkoutSegment>()
        var clock = 0.0
        var isFirst = true
        workout.slots.forEachIndexed { index, slot ->
            val resolved = ExerciseLibrary.resolve(slot, equipment) ?: return@forEachIndexed
            val (exercise, variation) = resolved
            if (!isFirst && workout.restSeconds > 0) {
                list += WorkoutSegment(WorkoutSegment.Kind.REST, index, exercise, variation, 0, clock, workout.restSeconds)
                clock += workout.restSeconds
            }
            isFirst = false
            val loops = max(1, slot.loops)
            val duration = exercise.loopDuration * loops
            list += WorkoutSegment(WorkoutSegment.Kind.EXERCISE, index, exercise, variation, loops, clock, duration)
            clock += duration
        }
        segments = list
    }

    val totalDuration: Double get() = segments.lastOrNull()?.end ?: 0.0

    val estimatedMinutes: Int get() = max(1, ceil(totalDuration / 60).toInt())

    /** Exercise segments only, in order. */
    val exercises: List<WorkoutSegment> get() = segments.filter { it.kind == WorkoutSegment.Kind.EXERCISE }

    /** The segment playing at [time], or null once the workout is over. */
    fun segmentIndex(time: Double): Int? {
        if (time >= totalDuration) return null
        return segments.indexOfFirst { time < it.end }.takeIf { it >= 0 }
    }
}
