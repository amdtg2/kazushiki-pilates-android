package com.kazushiki.pilates.figure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.player.ExercisePlayback
import kotlin.math.min

/**
 * An exercise playing on repeat, driven by the clock. Used for decorative demos
 * (welcome screen, paywall).
 */
@Composable
fun LoopingFigureView(
    exercise: Exercise = ExerciseLibrary.gluteBridge,
    variationIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    val variation = exercise.variations[min(variationIndex, exercise.variations.size - 1)]
    var time by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(exercise) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val seconds = (now - start) / 1_000_000_000.0
                val loop = exercise.loopDuration
                time = if (loop > 0) seconds % loop else 0.0
            }
        }
    }

    FigureView(
        pose = ExercisePlayback.frame(exercise, variation, time).pose,
        props = exercise.props,
        modifier = modifier,
    )
}

/** The most recognizable pose of an exercise, used as its thumbnail (ExerciseRow.previewPose on iPhone). */
fun previewPose(exercise: Exercise): FigurePose {
    val variation = exercise.variations[0]
    val name = exercise.sequence.firstOrNull { it.phase == "Hold" || it.isPump }?.pose
        ?: exercise.sequence[exercise.sequence.size / 2].pose
    return variation.pose(name)
}
