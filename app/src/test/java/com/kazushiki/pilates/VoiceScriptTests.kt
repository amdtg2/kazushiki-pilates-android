package com.kazushiki.pilates

import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.model.Workout
import com.kazushiki.pilates.model.WorkoutLibrary
import com.kazushiki.pilates.model.WorkoutSegment
import com.kazushiki.pilates.model.WorkoutTimeline
import com.kazushiki.pilates.player.ExercisePlayback
import com.kazushiki.pilates.services.VoicePrompt
import com.kazushiki.pilates.services.VoiceScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceScriptTests {
    private fun prompts(workout: Workout, step: Double = 0.1): List<VoicePrompt> {
        val timeline = WorkoutTimeline(workout, setOf(Equipment.MAT))
        val result = mutableListOf<VoicePrompt>()
        var time = 0.0
        while (true) {
            val index = timeline.segmentIndex(time) ?: break
            val segment = timeline.segments[index]
            val frame = if (segment.kind == WorkoutSegment.Kind.EXERCISE) {
                ExercisePlayback.frame(segment.exercise, segment.variation, time - segment.start)
            } else {
                null
            }
            val prompt = VoiceScript.prompt(segment, index, frame, time)
            if (result.lastOrNull()?.key != prompt.key) result.add(prompt)
            time += step
        }
        return result
    }

    @Test
    fun eachPromptIsSpokenOnce() {
        for (workout in WorkoutLibrary.all) {
            val spoken = prompts(workout)
            assertEquals("${workout.title} repeats a prompt", spoken.size, spoken.map { it.key }.toSet().size)
        }
    }

    @Test
    fun introducesExercisesAndCountsReps() {
        val spoken = prompts(WorkoutLibrary.backCare).map { it.text }
        assertTrue(spoken.firstOrNull()?.startsWith("Swan Prep. 8 reps.") == true)
        assertTrue(spoken.contains("2."))
        assertTrue(spoken.contains("Last one."))
        assertTrue(spoken.contains("Rest. Up next, Bird Dog."))
        assertTrue(spoken.contains("Get ready."))
    }

    @Test
    fun hundredCallsTwentyBreaths() {
        val spoken = prompts(WorkoutLibrary.coreWakeUp).filter { it.key.contains("-breath-") }
        assertEquals(20, spoken.size)
        assertEquals("Exhale.", spoken[1].text)
    }
}
