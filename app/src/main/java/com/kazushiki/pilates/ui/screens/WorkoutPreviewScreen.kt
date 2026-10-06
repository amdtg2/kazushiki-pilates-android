package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.ExerciseCounter
import com.kazushiki.pilates.figure.FigureView
import com.kazushiki.pilates.model.Workout
import com.kazushiki.pilates.model.WorkoutSegment
import com.kazushiki.pilates.model.WorkoutTimeline
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.LocalSubscriptionStore
import com.kazushiki.pilates.ui.Route
import com.kazushiki.pilates.ui.components.FullScreenDialog
import com.kazushiki.pilates.ui.components.KPEyebrow
import com.kazushiki.pilates.ui.components.KPPrimaryButton
import com.kazushiki.pilates.ui.components.KPTopBar
import com.kazushiki.pilates.ui.components.Pill
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.kpCard

/** Shows what's in a workout before starting it. */
@Composable
fun WorkoutPreviewScreen(workout: Workout, planID: String?, dayNumber: Int?) {
    val profileStore = LocalProfileStore.current
    val subscriptions = LocalSubscriptionStore.current
    val router = LocalRouter.current
    val colors = KP.colors
    var isPlaying by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }

    val equipment = profileStore.profile.equipment
    val schedule = remember(workout, equipment) { WorkoutTimeline(workout, equipment) }
    val unlocked = subscriptions.canAccess(workout)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        KPTopBar(title = "", onBack = { router.pop() })

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dayNumber != null) {
                    KPEyebrow(text = "Day $dayNumber")
                }
                Text(
                    workout.title,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    lineHeight = 41.sp,
                )
                Text(workout.summary, fontSize = 17.sp, color = colors.mutedText)
                Row(
                    Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Pill(text = "${schedule.estimatedMinutes} min")
                    Pill(text = workout.level.title)
                    workout.focus.forEach { Pill(text = it.title) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KPEyebrow(text = "${schedule.exercises.size} exercises")
                Text("Tap a move to watch how it's done.", fontSize = 15.sp, color = colors.mutedText)
                schedule.exercises.forEach { segment ->
                    ExerciseSegmentRow(
                        segment = segment,
                        originalID = workout.slots[segment.slotIndex].exerciseID,
                        onClick = { router.push(Route.ExerciseDetail(segment.exercise, segment.variation.id)) },
                    )
                }
            }
        }

        // Pinned start button (safeAreaInset on iPhone).
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            KPPrimaryButton(
                text = if (unlocked) "Start workout" else "Unlock with Premium",
                onClick = { if (unlocked) isPlaying = true else showPaywall = true },
                systemImage = if (unlocked) "play.fill" else "lock.fill",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showPaywall) {
        FullScreenDialog(onDismiss = { showPaywall = false }) {
            PaywallScreen(onClose = { showPaywall = false })
        }
    }

    if (isPlaying) {
        FullScreenDialog(onDismiss = { isPlaying = false }) {
            WorkoutPlayerScreen(
                workout = workout,
                timeline = schedule,
                planID = planID,
                dayNumber = dayNumber,
                onClose = { isPlaying = false },
            )
        }
    }
}

@Composable
private fun ExerciseSegmentRow(segment: WorkoutSegment, originalID: String, onClick: () -> Unit) {
    val colors = KP.colors
    val exercise = segment.exercise
    val amount = when (exercise.counter) {
        is ExerciseCounter.Reps -> "${segment.loops} reps · ${segment.variation.title}"
        ExerciseCounter.HundredCount -> "100 pumps · ${segment.variation.title}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KP.cornerRadius))
            .clickable(onClickLabel = "Shows the animated demo", onClick = onClick)
            .kpCard(padding = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FigureView(
            pose = segment.variation.pose(exercise.sequence[exercise.sequence.size / 2].pose),
            props = exercise.props,
            modifier = Modifier
                .width(96.dp)
                .clip(RoundedCornerShape(KP.compactCornerRadius))
                .background(colors.stage),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(exercise.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            Text(amount, fontSize = 15.sp, color = colors.mutedText)
            if (exercise.id != originalID) {
                Text("Swapped to match your equipment", fontSize = 12.sp, color = colors.accent)
            }
        }
        Icon(
            screenIcon("play.circle.fill"),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(28.dp),
        )
    }
}
