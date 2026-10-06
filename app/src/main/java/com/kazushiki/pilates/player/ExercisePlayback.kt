package com.kazushiki.pilates.player

import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.exercises.ExerciseCounter
import com.kazushiki.pilates.exercises.ExerciseVariation
import com.kazushiki.pilates.figure.FigurePose
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Everything the player needs to draw one moment of an exercise. */
data class PlaybackFrame(
    val pose: FigurePose,
    val stepIndex: Int,
    /** Seconds since the current step began. */
    val stepElapsed: Double,
    val phase: String,
    val cue: String,
    val counter: String,
    /** How many full loops of the sequence have finished. */
    val completedLoops: Int,
)

/** Pure timing logic: maps a playback time to a pose, phase, cue and counter. */
object ExercisePlayback {
    /** Seconds per arm pump in the Hundred (100 pumps = 50 seconds). */
    const val pumpPeriod = 0.5
    /** Degrees the arms move up and down during a pump. */
    const val pumpAmplitude = 5.0
    const val pumpsPerBreath = 5

    fun frame(exercise: Exercise, variation: ExerciseVariation, time: Double): PlaybackFrame {
        val steps = exercise.sequence
        val loopDuration = max(exercise.loopDuration, 0.001)
        val clamped = max(0.0, time)
        val loops = (clamped / loopDuration).toInt()
        val local = clamped - loops * loopDuration

        var index = 0
        var stepStart = 0.0
        while (index < steps.size - 1 && local >= stepStart + steps[index].duration) {
            stepStart += steps[index].duration
            index += 1
        }
        val step = steps[index]
        val previous = steps[(index - 1 + steps.size) % steps.size]
        val elapsed = local - stepStart
        val progress = min(1.0, elapsed / max(step.duration, 0.001))

        var pose = FigurePose.interpolate(variation.pose(previous.pose), variation.pose(step.pose), ease(progress))
        var cue = step.cue
        val counter: String

        when (val c = exercise.counter) {
            is ExerciseCounter.Reps -> counter = "Rep ${loops % max(c.count, 1) + 1} of ${c.count}"
            ExerciseCounter.HundredCount -> {
                if (step.isPump) {
                    val count = min(100, (elapsed / pumpPeriod).toInt() + 1)
                    val wobble = pumpAmplitude * sin(2 * PI * elapsed / pumpPeriod)
                    pose = pose.copy(arm = pose.arm.copy(upper = pose.arm.upper + wobble, fore = pose.arm.fore + wobble))
                    val breath = (count - 1) / pumpsPerBreath
                    val beat = (count - 1) % pumpsPerBreath + 1
                    val breathWord = if (breath % 2 == 0) "Inhale" else "Exhale"
                    cue = "$breathWord · $beat of $pumpsPerBreath. ${step.cue}"
                    counter = "Count $count / 100"
                } else {
                    val pumpIndex = steps.indexOfFirst { it.isPump }.let { if (it < 0) steps.size else it }
                    counter = if (index < pumpIndex) "Count 0 / 100" else "Count 100 / 100"
                }
            }
        }

        return PlaybackFrame(
            pose = pose, stepIndex = index, stepElapsed = elapsed, phase = step.phase,
            cue = cue, counter = counter, completedLoops = loops,
        )
    }

    /** Smooth start and stop between poses. */
    fun ease(t: Double): Double = 0.5 - 0.5 * cos(PI * t)
}
