package com.kazushiki.pilates

import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.exercises.ExerciseVariation
import com.kazushiki.pilates.figure.FigurePose
import com.kazushiki.pilates.figure.FigureSolver
import com.kazushiki.pilates.figure.FigureStage
import com.kazushiki.pilates.figure.Pt
import com.kazushiki.pilates.figure.ReformerGeometry
import com.kazushiki.pilates.figure.distance
import com.kazushiki.pilates.player.ExercisePlayback
import com.kazushiki.pilates.player.PlaybackFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FigureEngineTests {
    private data class Sample(val exercise: Exercise, val variation: ExerciseVariation, val frame: PlaybackFrame)

    /** Samples a full loop of every exercise and variation. */
    private fun sampleFrames(step: Double = 0.05): List<Sample> {
        val result = mutableListOf<Sample>()
        for (exercise in ExerciseLibrary.all) {
            for (variation in exercise.variations) {
                var time = 0.0
                while (time < exercise.loopDuration) {
                    result.add(Sample(exercise, variation, ExercisePlayback.frame(exercise, variation, time)))
                    time += step
                }
            }
        }
        return result
    }

    @Test
    fun libraryHasAllExercises() {
        assertEquals(34, ExerciseLibrary.all.size)
        assertEquals(34, ExerciseLibrary.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyStepPoseExistsInEveryVariation() {
        for (exercise in ExerciseLibrary.all) {
            for (variation in exercise.variations) {
                for (step in exercise.sequence) {
                    assertNotNull(
                        "${exercise.name} / ${variation.title} is missing pose ${step.pose}",
                        variation.poses[step.pose],
                    )
                }
            }
        }
    }

    @Test
    fun solvedJointsAreAlwaysFinite() {
        for ((exercise, variation, frame) in sampleFrames()) {
            val joints = FigureSolver.solve(frame.pose)
            for (point in joints.all) {
                assertTrue(
                    "${exercise.name} / ${variation.title} produced an invalid joint",
                    point.x.isFinite() && point.y.isFinite(),
                )
            }
        }
    }

    @Test
    fun figureStaysInsideTheStage() {
        val stage = FigureStage.viewBox
        // CGRect.insetBy(dx: -4, dy: -4).contains(point): min inclusive, max exclusive.
        val minX = stage.minX - 4
        val minY = stage.minY - 4
        val maxX = stage.maxX + 4
        val maxY = stage.maxY + 4
        // Collect every exercise that leaves the stage so one run reports them all.
        val outside = linkedMapOf<String, String>()
        for ((exercise, variation, frame) in sampleFrames()) {
            val joints = FigureSolver.solve(frame.pose)
            for ((index, point) in joints.all.withIndex()) {
                val inside = point.x >= minX && point.x < maxX && point.y >= minY && point.y < maxY
                if (!inside) outside.getOrPut("${exercise.name} / ${variation.id} / joint $index") { "$point" }
            }
        }
        assertTrue("Leaves the stage: $outside", outside.isEmpty())
    }

    @Test
    fun bridgeKeepsFeetAndShouldersPlanted() {
        val bridge = ExerciseLibrary.gluteBridge
        var time = 0.0
        while (time < bridge.loopDuration) {
            val frame = ExercisePlayback.frame(bridge, bridge.variations[0], time)
            val joints = FigureSolver.solve(frame.pose)
            assertTrue(distance(joints.ankle, Pt(350.0, 272.0)) < 0.5)
            assertTrue(distance(joints.shoulder, Pt(150.0, ExerciseLibrary.lyingY)) < 0.01)
            time += 0.05
        }
    }

    @Test
    fun singleLegBridgeKeepsWorkingFootPlanted() {
        val bridge = ExerciseLibrary.gluteBridge
        val single = bridge.variations.first { it.id == "single" }
        var time = 0.0
        while (time < bridge.loopDuration) {
            val joints = FigureSolver.solve(ExercisePlayback.frame(bridge, single, time).pose)
            assertTrue(distance(joints.farAnkle, Pt(350.0, 272.0)) < 0.5)
            time += 0.05
        }
    }

    @Test
    fun bridgeLiftsHipsAtTheTop() {
        val bridge = ExerciseLibrary.gluteBridge
        val down = FigureSolver.solve(bridge.variations[0].pose("down"))
        val up = FigureSolver.solve(bridge.variations[0].pose("up"))
        assertTrue(down.pelvis.y - up.pelvis.y > 35)
    }

    @Test
    fun bridgeCountsReps() {
        val bridge = ExerciseLibrary.gluteBridge
        val first = ExercisePlayback.frame(bridge, bridge.variations[0], 0.1)
        val second = ExercisePlayback.frame(bridge, bridge.variations[0], bridge.loopDuration + 0.1)
        assertEquals("Rep 1 of 10", first.counter)
        assertEquals("Rep 2 of 10", second.counter)
    }

    @Test
    fun hundredCountsToOneHundredWithAlternatingBreaths() {
        val hundred = ExerciseLibrary.hundred
        val variation = hundred.variations[0]
        val pumpStart = hundred.sequence.takeWhile { !it.isPump }.sumOf { it.duration }

        val firstPump = ExercisePlayback.frame(hundred, variation, pumpStart + 0.1)
        assertEquals("Count 1 / 100", firstPump.counter)
        assertTrue(firstPump.cue.startsWith("Inhale"))

        val sixthPump = ExercisePlayback.frame(hundred, variation, pumpStart + 5 * ExercisePlayback.pumpPeriod + 0.1)
        assertTrue(sixthPump.cue.startsWith("Exhale"))

        val lastPump = ExercisePlayback.frame(hundred, variation, pumpStart + 49.9)
        assertEquals("Count 100 / 100", lastPump.counter)
    }

    @Test
    fun interpolationHitsBothEnds() {
        val poses = ExerciseLibrary.gluteBridge.variations[0].poses
        val a = poses.getValue("down")
        val b = poses.getValue("up")
        assertEquals(a, FigurePose.interpolate(a, b, 0.0))
        assertEquals(b, FigurePose.interpolate(a, b, 1.0))
    }

    @Test
    fun reformerFootworkKeepsHeelsOnTheFootbar() {
        val footwork = ExerciseLibrary.reformerFootwork
        var time = 0.0
        while (time < footwork.loopDuration) {
            val joints = FigureSolver.solve(ExercisePlayback.frame(footwork, footwork.variations[0], time).pose)
            assertTrue(distance(joints.ankle, ReformerGeometry.footbarHeel) < 0.5)
            time += 0.05
        }
        // The carriage actually travels.
        val inPose = FigureSolver.solve(footwork.variations[0].pose("in"))
        val outPose = FigureSolver.solve(footwork.variations[0].pose("out"))
        assertTrue(inPose.pelvis.x - outPose.pelvis.x > 50)
    }
}
