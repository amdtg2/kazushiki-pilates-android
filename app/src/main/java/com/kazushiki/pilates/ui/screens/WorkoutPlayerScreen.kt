package com.kazushiki.pilates.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.ExerciseCounter
import com.kazushiki.pilates.figure.FigureView
import com.kazushiki.pilates.model.CelebrationSummary
import com.kazushiki.pilates.model.SessionRating
import com.kazushiki.pilates.model.Workout
import com.kazushiki.pilates.model.WorkoutSegment
import com.kazushiki.pilates.model.WorkoutSession
import com.kazushiki.pilates.model.WorkoutTimeline
import com.kazushiki.pilates.player.ExercisePlayback
import com.kazushiki.pilates.services.VoiceCoach
import com.kazushiki.pilates.services.VoiceScript
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.components.CelebrationContent
import com.kazushiki.pilates.ui.components.KPEyebrow
import com.kazushiki.pilates.ui.theme.KP
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Full-screen guided workout: each exercise plays for its reps, with rest screens between. */
@Composable
fun WorkoutPlayerScreen(
    workout: Workout,
    timeline: WorkoutTimeline,
    planID: String?,
    dayNumber: Int?,
    onClose: () -> Unit,
) {
    val activityStore = LocalActivityStore.current
    val profileStore = LocalProfileStore.current
    val context = LocalContext.current
    val view = LocalView.current
    val colors = KP.colors

    val coach = remember { VoiceCoach(context) }
    var baseTime by remember { mutableDoubleStateOf(0.0) }
    /** When the current play run started (System.nanoTime); null while paused. Starts playing. */
    var playStart by remember { mutableStateOf<Long?>(System.nanoTime()) }
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    var confirmingExit by remember { mutableStateOf(false) }
    /** Set when the last exercise ends: the session to save and what to celebrate. */
    var finished by remember { mutableStateOf<Pair<WorkoutSession, CelebrationSummary>?>(null) }
    val isPlaying = playStart != null

    // Keep the screen on while the player is up; stop talking when it closes.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            coach.stop()
            coach.shutdown()
        }
    }

    // The workout clock, running only while playing.
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            now = System.nanoTime()
        }
    }

    fun playbackTime(at: Long): Double {
        val start = playStart ?: return baseTime
        return baseTime + (at - start) / 1_000_000_000.0
    }

    fun pause() {
        baseTime = playbackTime(System.nanoTime())
        playStart = null
        coach.stop()
    }

    fun say(text: String) {
        if (!profileStore.voiceCuesEnabled) return
        if (!(playStart != null || text.startsWith("Workout complete"))) return
        coach.speak(text)
    }

    fun resume() {
        if (playStart != null) return
        val t = System.nanoTime()
        now = t
        playStart = t
    }

    fun skip(to: Double) {
        baseTime = to
        if (playStart != null) {
            val t = System.nanoTime()
            now = t
            playStart = t
        }
    }

    fun prepareFinish(): CelebrationSummary {
        finished?.let { return it.second }
        val session = WorkoutSession(
            date = Instant.now(),
            workoutID = workout.id,
            workoutTitle = workout.title,
            planID = planID,
            dayNumber = dayNumber,
            seconds = timeline.totalDuration,
        )
        val summary = CelebrationSummary.make(
            session = session,
            history = activityStore.sessions,
            profile = profileStore.profile,
            exercises = timeline.exercises.size,
        )
        finished = session to summary
        return summary
    }

    fun save(rating: SessionRating?) {
        prepareFinish()
        val session = finished?.first ?: return
        activityStore.log(session.copy(rating = rating))
        onClose()
    }

    val time = playbackTime(now)
    val index = timeline.segmentIndex(time)

    // Back asks before ending, like the close button (and can't skip saving on the finish screen).
    BackHandler(enabled = true) { if (index != null) confirmingExit = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        if (index != null) {
            val segment = timeline.segments[index]
            val elapsed = time - segment.start
            val exerciseNumber = timeline.exercises.indexOfFirst { it.start == segment.start && it.kind == WorkoutSegment.Kind.EXERCISE }
                .takeIf { it >= 0 }
                ?: timeline.exercises.indexOfFirst { it.slotIndex == segment.slotIndex }.takeIf { it >= 0 }
                ?: 0
            val frame = if (segment.kind == WorkoutSegment.Kind.EXERCISE) {
                ExercisePlayback.frame(segment.exercise, segment.variation, elapsed)
            } else {
                null
            }
            val prompt = VoiceScript.prompt(segment, index, frame, time)

            LaunchedEffect(prompt.key) { say(prompt.text) }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconButton(
                        onClick = { confirmingExit = true },
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "End workout" },
                    ) {
                        Icon(screenIcon("xmark"), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                    }
                    val progress = (time / max(timeline.totalDuration, 1.0)).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.accent,
                        trackColor = colors.divider,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    val voiceOn = profileStore.voiceCuesEnabled
                    IconButton(
                        onClick = {
                            profileStore.setVoiceCues(!profileStore.voiceCuesEnabled)
                            if (!profileStore.voiceCuesEnabled) coach.stop()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .semantics { contentDescription = if (voiceOn) "Mute voice cues" else "Turn on voice cues" },
                    ) {
                        Icon(
                            screenIcon(if (voiceOn) "speaker.wave.2.fill" else "speaker.slash.fill"),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                when (segment.kind) {
                    WorkoutSegment.Kind.EXERCISE -> ExerciseStage(segment, elapsed, exerciseNumber + 1, timeline.exercises.size)
                    WorkoutSegment.Kind.REST -> RestStage(segment, remaining = segment.end - time)
                }

                Spacer(Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IOSLabelButton(
                        text = if (isPlaying) "Pause" else "Resume",
                        systemImage = if (isPlaying) "pause.fill" else "play.fill",
                        prominent = true,
                        onClick = { if (isPlaying) pause() else resume() },
                        modifier = Modifier.weight(1f),
                    )
                    IOSLabelButton(
                        text = if (segment.kind == WorkoutSegment.Kind.REST) "Skip rest" else "Next",
                        systemImage = "forward.end.fill",
                        prominent = false,
                        onClick = { skip(segment.end) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            LaunchedEffect(Unit) {
                pause()
                val summary = prepareFinish()
                say("Workout complete. ${summary.encouragement.title}.")
            }
            WorkoutCompleteContent(summary = finished?.second, onSave = { save(it) })
        }
    }

    if (confirmingExit) {
        AlertDialog(
            onDismissRequest = { confirmingExit = false },
            title = { Text("End this workout?") },
            text = { Text("Your progress in this session won't be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingExit = false
                    onClose()
                }) { Text("End workout", color = DestructiveRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingExit = false }) { Text("Keep going", color = colors.accent) }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.mutedText,
        )
    }
}

@Composable
private fun ExerciseStage(segment: WorkoutSegment, elapsed: Double, number: Int, total: Int) {
    val colors = KP.colors
    val frame = ExercisePlayback.frame(segment.exercise, segment.variation, elapsed)
    val counter = when (segment.exercise.counter) {
        is ExerciseCounter.Reps -> "Rep ${min(frame.completedLoops + 1, segment.loops)} of ${segment.loops}"
        ExerciseCounter.HundredCount -> frame.counter
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KPEyebrow(text = "Exercise $number of $total")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    segment.exercise.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    lineHeight = 34.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    counter,
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.accent, fontFeatureSettings = "tnum"),
                )
            }
        }
        FigureView(
            pose = frame.pose,
            props = segment.exercise.props,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KP.cornerRadius))
                .background(colors.stage),
        )
        Text(
            frame.phase.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = colors.mutedText,
        )
        Text(frame.cue, fontSize = 20.sp, color = colors.text)
    }
}

@Composable
private fun RestStage(segment: WorkoutSegment, remaining: Double) {
    val colors = KP.colors
    val first = segment.exercise.sequence[0]
    val firstPose = segment.variation.pose(first.pose)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Rest", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.mutedText)
        Text(
            "${ceil(remaining).toInt()}",
            style = TextStyle(fontSize = 88.sp, fontWeight = FontWeight.Bold, color = colors.accent, fontFeatureSettings = "tnum"),
        )
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Up next: ${segment.exercise.name}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            FigureView(
                pose = firstPose,
                props = segment.exercise.props,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KP.cornerRadius))
                    .background(colors.stage),
            )
            Text(first.cue, fontSize = 15.sp, color = colors.mutedText, textAlign = TextAlign.Center)
        }
    }
}

/** Shown when the last exercise finishes: a celebration, then how it felt. */
@Composable
private fun WorkoutCompleteContent(summary: CelebrationSummary?, onSave: (SessionRating?) -> Unit) {
    val colors = KP.colors
    var rating by remember { mutableStateOf<SessionRating?>(null) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (summary != null) {
                Box(Modifier.padding(top = 24.dp)) { CelebrationContent(summary = summary) }
            } else {
                CircularProgressIndicator(color = colors.accent, modifier = Modifier.padding(top = 80.dp))
            }

            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("How did that feel?", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionRating.entries.forEach { option ->
                        val selected = rating == option
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (selected) colors.accent else colors.accentSoft)
                                .clickable { rating = option }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                option.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) colors.onAccent else colors.accent,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            HeavyProminentButton(text = "Save and finish", onClick = { onSave(rating) })
        }
    }
}
