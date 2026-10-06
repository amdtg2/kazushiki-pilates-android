package com.kazushiki.pilates.services

import com.kazushiki.pilates.exercises.ExerciseCounter
import com.kazushiki.pilates.model.WorkoutSegment
import com.kazushiki.pilates.player.ExercisePlayback
import com.kazushiki.pilates.player.PlaybackFrame

/** Something the voice coach should say. The coach speaks when [key] changes. */
data class VoicePrompt(val key: String, val text: String)

/** Decides what to say at each moment of a workout. Kept separate from audio so it can be tested. */
object VoiceScript {
    fun prompt(segment: WorkoutSegment, segmentIndex: Int, frame: PlaybackFrame?, time: Double): VoicePrompt {
        when (segment.kind) {
            WorkoutSegment.Kind.REST -> {
                val remaining = segment.end - time
                if (remaining <= 3) return VoicePrompt("$segmentIndex-ready", "Get ready.")
                return VoicePrompt("$segmentIndex-rest", "Rest. Up next, ${segment.exercise.name}.")
            }
            WorkoutSegment.Kind.EXERCISE -> {
                if (frame == null) return VoicePrompt("$segmentIndex", segment.exercise.name)
                val exercise = segment.exercise
                val step = exercise.sequence[frame.stepIndex]

                // The Hundred: call the breath every five pumps.
                if (step.isPump) {
                    val breath = (frame.stepElapsed / (ExercisePlayback.pumpPeriod * ExercisePlayback.pumpsPerBreath)).toInt()
                    val word = if (breath % 2 == 0) "Inhale" else "Exhale"
                    val text = if (breath == 0) "${step.cue} $word." else "$word."
                    return VoicePrompt("$segmentIndex-breath-$breath", text)
                }

                // After the first rep, just count.
                if (frame.completedLoops > 0 && exercise.counter is ExerciseCounter.Reps) {
                    val rep = frame.completedLoops + 1
                    val text = if (rep == segment.loops) "Last one." else "$rep."
                    return VoicePrompt("$segmentIndex-rep-${frame.completedLoops}", text)
                }

                // First rep: speak each new phase's cue once.
                var runStart = frame.stepIndex
                while (runStart > 0 && exercise.sequence[runStart - 1].phase == step.phase) runStart -= 1
                if (runStart == 0) {
                    return VoicePrompt("$segmentIndex-phase-0", "${exercise.name}. ${amount(segment)}. ${exercise.sequence[0].cue}")
                }
                return VoicePrompt("$segmentIndex-phase-$runStart", exercise.sequence[runStart].cue)
            }
        }
    }

    fun amount(segment: WorkoutSegment): String = when (segment.exercise.counter) {
        is ExerciseCounter.Reps -> "${segment.loops} reps"
        ExerciseCounter.HundredCount -> "100 pumps"
    }
}
