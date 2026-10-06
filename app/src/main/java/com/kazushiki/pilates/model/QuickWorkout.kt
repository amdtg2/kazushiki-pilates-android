package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.BodyArea
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.exercises.ExerciseCounter
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.exercises.ExerciseVariation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Builds a one-off workout from what she picks: equipment, body areas and time. */
object QuickWorkoutBuilder {
    val minuteOptions = listOf(10, 15, 20, 30, 45)
    const val maxFocuses = 2
    const val restSeconds = 15.0

    /** The body areas each focus works. */
    fun areas(focus: ProgramFocus): List<BodyArea> = when (focus) {
        ProgramFocus.UPPER_BODY -> listOf(BodyArea.ARMS, BodyArea.BACK)
        ProgramFocus.CORE -> listOf(BodyArea.CORE)
        ProgramFocus.LOWER_BODY -> listOf(BodyArea.LEGS, BodyArea.GLUTES)
        ProgramFocus.GLUTES -> listOf(BodyArea.GLUTES)
        ProgramFocus.BACK -> listOf(BodyArea.BACK)
        ProgramFocus.FULL_BODY -> BodyArea.entries
    }

    /** The minute option closest to her usual session length. */
    fun defaultMinutes(profile: UserProfile): Int =
        minuteOptions.minByOrNull { abs(it - profile.minutesPerDay) } ?: 15

    fun build(
        equipment: Set<Equipment>,
        focuses: List<ProgramFocus>,
        minutes: Int,
        level: ExerciseLevel,
        library: List<Exercise> = ExerciseLibrary.all,
    ): Workout {
        val owned = equipment + Equipment.MAT
        val chosen = focuses.ifEmpty { listOf(ProgramFocus.FULL_BODY) }
        val targets = chosen.flatMap { areas(it) }.toSet()
        val usable = library.filter { owned.containsAll(it.equipment) }

        // Best matches first: moves that hit more of her chosen areas, then moves that use
        // the equipment she picked, then library order.
        fun score(exercise: Exercise): Int {
            val hits = exercise.bodyAreas.toSet().intersect(targets).size
            val leadsWithTarget = exercise.bodyAreas.firstOrNull()?.let { it in targets } == true
            val usesEquipment = exercise.equipment.any { it != Equipment.MAT }
            return hits * 10 + (if (leadsWithTarget) 4 else 0) + (if (usesEquipment) 3 else 0)
        }
        fun ranked(list: List<Exercise>): List<Exercise> =
            list.withIndex().sortedWith(compareByDescending<IndexedValue<Exercise>> { score(it.value) }.thenBy { it.index }).map { it.value }

        val pool = ranked(usable.filter { e -> e.bodyAreas.any { it in targets } }).toMutableList()
        // Keep some variety when few moves match: top up with core moves, then anything else.
        val minimumVariety = 5
        if (pool.size < minimumVariety) {
            val rest = usable.filter { e -> pool.none { it.id == e.id } }
            val core = rest.filter { BodyArea.CORE in it.bodyAreas }
            val others = rest.filter { BodyArea.CORE !in it.bodyAreas }
            pool += (core + others).take(minimumVariety - pool.size)
        }
        if (pool.isEmpty()) pool += usable

        val target = (minutes * 60).toDouble()
        val blockSeconds = if (minutes <= 15) 50.0 else 65.0
        val slots = mutableListOf<WorkoutSlot>()
        var total = 0.0
        var usedHundred = false
        var index = 0
        var skipped = 0

        while (total < target - 20 && slots.size < 40 && pool.isNotEmpty() && skipped < pool.size) {
            val exercise = pool[index % pool.size]
            index += 1
            val isHundred = exercise.counter == ExerciseCounter.HundredCount
            if (isHundred && usedHundred) {
                skipped += 1
                continue
            }
            skipped = 0
            val loops = if (isHundred) 1 else max(2, (blockSeconds / max(exercise.loopDuration, 1.0)).roundToInt())
            val duration = exercise.loopDuration * loops
            val rest = if (slots.isEmpty()) 0.0 else restSeconds
            // Don't overshoot by more than half a block.
            if (slots.isNotEmpty() && total + rest + duration > target + blockSeconds / 2) break
            usedHundred = usedHundred || isHundred
            slots += WorkoutSlot(exercise.id, variation(exercise, level).id, loops)
            total += rest + duration
        }

        val focusAreas = if (ProgramFocus.FULL_BODY in chosen) listOf(BodyArea.FULL_BODY) else BodyArea.entries.filter { it in targets }
        val name = chosen.joinToString(" + ") { it.title }
        return Workout(
            id = "quick-" + chosen.joinToString("-") { it.rawValue } + "-$minutes",
            title = "Quick $name",
            summary = "A $minutes-minute session built from what you picked.",
            focus = focusAreas,
            level = level,
            slots = slots,
            restSeconds = restSeconds,
        )
    }

    /** The hardest version at or below her level, or the easiest one. */
    fun variation(exercise: Exercise, level: ExerciseLevel): ExerciseVariation =
        exercise.variations.filter { it.level.rank <= level.rank }.maxByOrNull { it.level.rank }
            ?: exercise.variations.minByOrNull { it.level.rank }
            ?: exercise.variations[0]
}
