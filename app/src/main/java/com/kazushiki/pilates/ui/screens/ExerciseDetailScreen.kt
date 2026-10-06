package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.figure.FigureView
import com.kazushiki.pilates.player.ExercisePlayback
import com.kazushiki.pilates.player.PlaybackFrame
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.components.KPEyebrow
import com.kazushiki.pilates.ui.components.KPTopBar
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.kpCard

private val playbackSpeeds = listOf(0.5, 1.0, 2.0)

/**
 * Plays an exercise's animated demo with its phases, cues and counter (ExercisePlayerView on iPhone).
 * [variationID] opens a specific version (e.g. the Advanced Hundred); otherwise the first.
 */
@Composable
fun ExerciseDetailScreen(exercise: Exercise, variationID: String? = null) {
    val colors = KP.colors
    val router = LocalRouter.current

    var selectedVariationID by rememberSaveable(exercise.id) {
        mutableStateOf((exercise.variations.firstOrNull { it.id == variationID } ?: exercise.variations.firstOrNull())?.id ?: "")
    }
    val variation = exercise.variations.firstOrNull { it.id == selectedVariationID } ?: exercise.variations[0]

    var speed by remember { mutableDoubleStateOf(1.0) }
    /** Playback seconds accumulated before the current play run. */
    var baseTime by remember { mutableDoubleStateOf(0.0) }
    /** When the current play run started (System.nanoTime); null while paused. */
    var playStart by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    val isPlaying = playStart != null

    fun playbackTime(at: Long): Double {
        val start = playStart ?: return baseTime
        return baseTime + (at - start) / 1_000_000_000.0 * speed
    }

    fun play() {
        if (playStart != null) return
        val t = System.nanoTime()
        now = t
        playStart = t
    }

    fun pause() {
        baseTime = playbackTime(System.nanoTime())
        playStart = null
    }

    fun restart() {
        baseTime = 0.0
        val t = System.nanoTime()
        now = t
        playStart = t
    }

    /** Changing speed keeps the figure where it is instead of jumping. */
    fun setSpeed(newSpeed: Double) {
        val t = System.nanoTime()
        baseTime = playbackTime(t)
        if (playStart != null) {
            playStart = t
            now = t
        }
        speed = newSpeed
    }

    // Starts playing on first appearance.
    LaunchedEffect(Unit) { play() }

    // The animation clock, running only while playing.
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            now = System.nanoTime()
        }
    }

    val frame = ExercisePlayback.frame(exercise, variation, playbackTime(now))

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        KPTopBar(title = exercise.name, onBack = { router.pop() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DemoStage(exercise, frame)

            // Controls
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IOSLabelButton(
                        text = if (isPlaying) "Pause" else "Play",
                        systemImage = if (isPlaying) "pause.fill" else "play.fill",
                        prominent = true,
                        onClick = { if (isPlaying) pause() else play() },
                        modifier = Modifier.weight(1f),
                    )
                    IOSLabelButton(
                        text = "Restart",
                        systemImage = "arrow.counterclockwise",
                        prominent = false,
                        onClick = { restart() },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (exercise.variations.size > 1) {
                    IOSSegmentedPicker(
                        options = exercise.variations.map { it.title },
                        selectedIndex = exercise.variations.indexOfFirst { it.id == variation.id },
                        onSelect = { selectedVariationID = exercise.variations[it].id },
                    )
                }

                IOSSegmentedPicker(
                    options = playbackSpeeds.map { if (it == 0.5) "0.5×" else "${it.toInt()}×" },
                    selectedIndex = playbackSpeeds.indexOf(speed),
                    onSelect = { setSpeed(playbackSpeeds[it]) },
                )
            }

            // About
            Column(
                Modifier
                    .fillMaxWidth()
                    .kpCard(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KPEyebrow(text = "About this move")
                Text(exercise.summary, fontSize = 17.sp, color = colors.text)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    exercise.equipment.forEach { CapsuleTag(it.title) }
                    exercise.bodyAreas.forEach { CapsuleTag(it.title) }
                }
            }
        }
    }
}

@Composable
private fun DemoStage(exercise: Exercise, frame: PlaybackFrame) {
    val colors = KP.colors
    val shape = RoundedCornerShape(KP.cornerRadius)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.divider, shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                exercise.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                frame.counter,
                style = TextStyle(fontSize = 15.sp, color = colors.mutedText, fontFeatureSettings = "tnum"),
            )
        }

        FigureView(
            pose = frame.pose,
            props = exercise.props,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .background(colors.stage),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            exercise.phases.forEach { phase ->
                val isCurrent = phase == frame.phase
                Text(
                    phase,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrent) colors.onAccent else colors.mutedText,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isCurrent) colors.accent else colors.accentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Text(
            frame.cue,
            fontSize = 17.sp,
            color = colors.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 66.dp),
        )
    }
}

@Composable
private fun CapsuleTag(text: String) {
    val colors = KP.colors
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = colors.accent,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.accentSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
