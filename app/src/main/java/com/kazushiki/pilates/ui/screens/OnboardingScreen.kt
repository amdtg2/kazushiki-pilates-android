package com.kazushiki.pilates.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.figure.LoopingFigureView
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.ProgramLibrary
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.services.ReminderScheduler
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.components.KPEyebrow
import com.kazushiki.pilates.ui.components.KPIconBadge
import com.kazushiki.pilates.ui.components.KPPrimaryButton
import com.kazushiki.pilates.ui.components.WeekdayPicker
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.kpCard

private enum class OnboardingStep { WELCOME, GOALS, LEVEL, DAYS, EQUIPMENT, TIME, REMINDER, PLAN }

/** First-launch setup: goals, level, days, equipment, time, reminder, then a summary. */
@Composable
fun OnboardingScreen() {
    val store = LocalProfileStore.current
    val context = LocalContext.current
    val colors = KP.colors
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    var isFinishing by remember { mutableStateOf(false) }
    val step = OnboardingStep.entries[stepIndex]
    val stepCount = OnboardingStep.entries.size

    fun finish() {
        ReminderScheduler.apply(context, store.profile)
        store.completeOnboarding()
        isFinishing = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        finish()
    }

    fun go(index: Int) {
        if (index in 0 until stepCount) stepIndex = index
    }

    fun advance() {
        if (step != OnboardingStep.PLAN) {
            go(stepIndex + 1)
            return
        }
        isFinishing = true
        if (store.profile.remindersOn && needsNotificationPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            finish()
        }
    }

    val primaryTitle = when (step) {
        OnboardingStep.WELCOME -> "Get started"
        OnboardingStep.PLAN -> "Let's go"
        else -> "Continue"
    }
    val canContinue = step != OnboardingStep.GOALS || store.profile.isReady

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        if (step != OnboardingStep.WELCOME) {
            // Header: back + progress
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = { go(stepIndex - 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Back" },
                ) {
                    Icon(screenIcon("chevron.left"), contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                }
                LinearProgressIndicator(
                    progress = { stepIndex.toFloat() / (stepCount - 1).toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.accent,
                    trackColor = colors.divider,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
            },
            modifier = Modifier.weight(1f),
            label = "onboardingStep",
        ) { shown ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                when (shown) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.GOALS -> GoalsStep()
                    OnboardingStep.LEVEL -> LevelStep()
                    OnboardingStep.DAYS -> DaysStep()
                    OnboardingStep.EQUIPMENT -> EquipmentStep()
                    OnboardingStep.TIME -> TimeStep()
                    OnboardingStep.REMINDER -> ReminderStep()
                    OnboardingStep.PLAN -> PlanStep()
                }
            }
        }

        // Footer
        KPPrimaryButton(
            text = primaryTitle,
            onClick = { advance() },
            enabled = canContinue && !isFinishing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

// Steps

@Composable
private fun WelcomeStep() {
    val colors = KP.colors
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        LoopingFigureView(
            modifier = Modifier
                .padding(top = 40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(KP.cornerRadius))
                .background(colors.stage),
        )
        KPEyebrow(text = "Kazushiki Pilates")
        Text(
            "Pilates at home, built around what you have.",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            lineHeight = 41.sp,
        )
        Text(
            "Answer a few quick questions and we'll set things up for your goals, your level and your equipment.",
            fontSize = 17.sp,
            color = colors.mutedText,
        )
    }
}

@Composable
private fun GoalsStep() {
    val store = LocalProfileStore.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("What do you want from Pilates?", "Pick as many as you like.")
        PilatesGoal.entries.forEach { goal ->
            OptionCard(
                title = goal.title,
                detail = goal.detail,
                systemImage = goal.systemImage,
                isSelected = goal in store.profile.goals,
                allowsMultiple = true,
                onClick = { store.toggleGoal(goal) },
            )
        }
    }
}

@Composable
private fun LevelStep() {
    val store = LocalProfileStore.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("What's your Pilates level?", "You can change this any time.")
        ExerciseLevel.entries.forEach { level ->
            OptionCard(
                title = level.title,
                detail = level.detail,
                isSelected = store.profile.level == level,
                onClick = { store.update { it.copy(level = level) } },
            )
        }
    }
}

@Composable
private fun DaysStep() {
    val store = LocalProfileStore.current
    val colors = KP.colors
    val profile = store.profile
    val count = profile.sessionsPerWeek
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle("Which days will you work out?", "Pick the days that fit your week. Consistency matters more than volume.")
        Column(
            Modifier
                .fillMaxWidth()
                .kpCard(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WeekdayPicker(
                selection = profile.effectiveTrainingDays,
                onSelectionChange = { days -> store.update { it.copy(trainingDays = days) } },
            )
            Text(
                "$count ${if (count == 1) "day" else "days"} a week · ${profile.trainingDaysText()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
            )
            Text(
                "${profile.recommendedDaysPerWeek} days is recommended for your level.",
                fontSize = 13.sp,
                color = colors.mutedText,
            )
        }
    }
}

@Composable
private fun EquipmentStep() {
    val store = LocalProfileStore.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("What equipment do you have?", "Every workout works with just a mat. Add anything else you own and we'll use it.")
        Equipment.entries.forEach { item ->
            OptionCard(
                title = item.title,
                detail = if (item == Equipment.MAT) "Included in every plan" else null,
                isSelected = item in store.profile.equipment,
                allowsMultiple = true,
                isLocked = item == Equipment.MAT,
                onClick = { store.toggleEquipment(item) },
            )
        }
    }
}

@Composable
private fun TimeStep() {
    val store = LocalProfileStore.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("How much time per day?", "Short sessions done often beat long ones done rarely.")
        UserProfile.minuteOptions.forEach { minutes ->
            OptionCard(
                title = "$minutes minutes",
                detail = if (minutes == 15) "Most popular" else null,
                isSelected = store.profile.minutesPerDay == minutes,
                onClick = { store.update { it.copy(minutesPerDay = minutes) } },
            )
        }
    }
}

@Composable
private fun ReminderStep() {
    val store = LocalProfileStore.current
    val colors = KP.colors
    val profile = store.profile
    var editingTime by remember { mutableStateOf(false) }

    val summary = if (!profile.remindersOn) {
        "No reminders. You can turn them on any time in Profile."
    } else {
        "We'll remind you ${profile.trainingDaysPhrase()} at ${profile.reminderTimeText()}. We'll ask for permission when you finish setup."
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle("Want a reminder?", "We'll only nudge you on your workout days, at a time that suits you.")
        Column(
            Modifier
                .fillMaxWidth()
                .kpCard(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Workout-day reminders", fontSize = 17.sp, color = colors.text, modifier = Modifier.weight(1f))
                Switch(
                    checked = profile.remindersOn,
                    onCheckedChange = { on -> store.update { it.copy(remindersOn = on) } },
                    colors = kpSwitchColors(),
                )
            }
            if (profile.remindersOn) {
                HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Time", fontSize = 17.sp, color = colors.text, modifier = Modifier.weight(1f))
                    TimeValueChip(text = profile.reminderTimeText(), onClick = { editingTime = true })
                }
            }
        }
        Text(summary, fontSize = 13.sp, color = colors.mutedText)
    }

    if (editingTime) {
        IOSTimePickerDialog(
            hour = profile.reminderHour,
            minute = profile.reminderMinute,
            onDismiss = { editingTime = false },
            onConfirm = { h, m ->
                editingTime = false
                store.update { it.copy(reminderHour = h, reminderMinute = m) }
            },
        )
    }
}

@Composable
private fun PlanStep() {
    val store = LocalProfileStore.current
    val colors = KP.colors
    val profile = store.profile
    val picks = ProgramLibrary.recommended(profile).take(3)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle("You're all set", "Here's your week. Pick a program or challenge whenever you're ready.")
        Column(
            Modifier
                .fillMaxWidth()
                .kpCard(padding = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlanStat("${profile.sessionsPerWeek}", "days a week")
                PlanStat("${profile.minutesPerDay}", "minutes each")
            }
            Text("Workout days: " + profile.trainingDaysText(), fontSize = 17.sp, color = colors.mutedText)
            Text(
                "Equipment: " + Equipment.entries.filter { it in profile.equipment }.joinToString(", ") { it.title },
                fontSize = 17.sp,
                color = colors.mutedText,
            )
        }

        if (picks.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .kpCard(padding = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KPEyebrow(text = "Recommended programs")
                picks.forEach { program ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KPIconBadge(systemImage = program.focus?.systemImage ?: "figure.pilates", size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(program.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                            Text(
                                listOfNotNull(program.level?.title, "${program.weeks} weeks").joinToString(" · "),
                                fontSize = 12.sp,
                                color = colors.mutedText,
                            )
                        }
                    }
                }
                Text("You'll find these under Challenges → Programs.", fontSize = 13.sp, color = colors.mutedText)
            }
        }
    }
}

// Pieces

@Composable
private fun StepTitle(heading: String, subtitle: String) {
    val colors = KP.colors
    Column(
        Modifier.padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(heading, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Text(subtitle, fontSize = 17.sp, color = colors.mutedText)
    }
}

@Composable
private fun PlanStat(value: String, label: String) {
    val colors = KP.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.accent, fontFeatureSettings = "tnum"),
        )
        Text(label, fontSize = 13.sp, color = colors.mutedText)
    }
}

/** A tappable choice row used throughout onboarding. */
@Composable
fun OptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    detail: String? = null,
    systemImage: String? = null,
    allowsMultiple: Boolean = false,
    isLocked: Boolean = false,
) {
    val colors = KP.colors
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(KP.compactCornerRadius)
    val indicator = when {
        isLocked -> "lock.fill"
        allowsMultiple -> if (isSelected) "checkmark.square.fill" else "square"
        else -> if (isSelected) "checkmark.circle.fill" else "circle"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) colors.accentSoft else colors.surface)
            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) colors.accent else colors.divider, shape)
            .clickable(enabled = !isLocked) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (systemImage != null) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                Icon(sfIcon(systemImage), contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            if (detail != null) {
                Text(detail, fontSize = 15.sp, color = colors.mutedText)
            }
        }
        Icon(
            screenIcon(indicator),
            contentDescription = null,
            tint = if (isSelected) colors.accent else colors.divider,
            modifier = Modifier.size(24.dp),
        )
    }
}
