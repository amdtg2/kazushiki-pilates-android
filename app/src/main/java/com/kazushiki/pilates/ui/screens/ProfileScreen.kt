package com.kazushiki.pilates.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.services.ReminderScheduler
import com.kazushiki.pilates.services.SubscriptionStore
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalSubscriptionStore
import com.kazushiki.pilates.ui.components.FullScreenDialog
import com.kazushiki.pilates.ui.components.WeekdayPicker
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import kotlinx.coroutines.launch

/** Edit onboarding answers, reminders, and start over. (ProfileView on iPhone.) */
@Composable
fun ProfileScreen() {
    val profileStore = LocalProfileStore.current
    val activityStore = LocalActivityStore.current
    val subscriptions = LocalSubscriptionStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = KP.colors
    val profile = profileStore.profile

    var confirmingReset by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var goalsExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }
    var levelMenuOpen by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        ReminderScheduler.apply(context, profileStore.profile)
    }

    // Changes to any of these reschedule the reminder.
    val reminderSettings: List<Int> = listOf(
        if (profile.remindersOn) 1 else 0, profile.reminderHour, profile.reminderMinute, profile.minutesPerDay,
    ) + profile.effectiveTrainingDays.sorted()
    var lastReminderSettings by remember { mutableStateOf(reminderSettings) }
    LaunchedEffect(reminderSettings) {
        if (reminderSettings != lastReminderSettings) {
            lastReminderSettings = reminderSettings
            ReminderScheduler.apply(context, profileStore.profile)
        }
    }

    val goalSummary: String = run {
        val goals = PilatesGoal.entries.filter { it in profile.goals }
        if (goals.size == 1) goals[0].title else "${goals.size} selected"
    }
    val equipmentSummary: String = run {
        val extra = (profile.equipment - Equipment.MAT).size
        if (extra == 0) "Mat only" else "Mat + $extra"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        IOSLargeTitle(title = "Profile")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Subscription
            GroupedSection(header = "Subscription") {
                if (subscriptions.isPremium) {
                    GroupedRow {
                        Icon(sfIcon("checkmark.seal.fill"), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Premium is active", fontSize = 17.sp, color = colors.accent)
                    }
                    GroupedDivider()
                    GroupedButtonRow("Manage subscription") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                        runCatching { context.startActivity(intent) }
                    }
                } else {
                    GroupedButtonRow(if (subscriptions.isEligibleForTrial) "Start your 7-day free trial" else "Go Premium") {
                        showPaywall = true
                    }
                    GroupedDivider()
                    GroupedButtonRow("Restore purchases") {
                        scope.launch {
                            restoreMessage = try {
                                subscriptions.restore()
                                if (subscriptions.isPremium) "Premium restored." else "No active subscription was found for this Google account."
                            } catch (e: Exception) {
                                "Restore didn't finish. Please try again."
                            }
                        }
                    }
                }
            }

            if (SubscriptionStore.testingToolsEnabled) {
                GroupedSection(header = "Testing", footer = "Only in test builds. Customers never see this.") {
                    GroupedToggleRow(
                        title = "Unlock Premium for testing",
                        checked = subscriptions.testUnlocked,
                        onCheckedChange = { subscriptions.testUnlocked = it },
                    )
                }
            }

            // Your training
            GroupedSection(
                header = "Your training",
                footer = "Workouts swap moves to match your equipment. Mat work is always included.",
            ) {
                DisclosureRow(title = "Goals", value = goalSummary, expanded = goalsExpanded) { goalsExpanded = !goalsExpanded }
                if (goalsExpanded) {
                    PilatesGoal.entries.forEach { goal ->
                        GroupedDivider()
                        GroupedToggleRow(
                            title = goal.title,
                            systemImage = goal.systemImage,
                            checked = goal in profile.goals,
                            onCheckedChange = {
                                // Keep at least one goal selected.
                                if (profileStore.profile.goals != setOf(goal)) profileStore.toggleGoal(goal)
                            },
                        )
                    }
                }
                GroupedDivider()
                Box {
                    GroupedRow(onClick = { levelMenuOpen = true }) {
                        Text("Level", fontSize = 17.sp, color = colors.text)
                        Spacer(Modifier.weight(1f))
                        Text(profile.level.title, fontSize = 17.sp, color = colors.mutedText)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = levelMenuOpen, onDismissRequest = { levelMenuOpen = false }) {
                        ExerciseLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.title, color = colors.text) },
                                onClick = {
                                    levelMenuOpen = false
                                    profileStore.update { it.copy(level = level) }
                                },
                                leadingIcon = {
                                    if (level == profile.level) {
                                        Icon(sfIcon("checkmark"), contentDescription = null, tint = colors.accent)
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }
                                },
                            )
                        }
                    }
                }
                GroupedDivider()
                DisclosureRow(title = "Equipment", value = equipmentSummary, expanded = equipmentExpanded) { equipmentExpanded = !equipmentExpanded }
                if (equipmentExpanded) {
                    Equipment.entries.forEach { item ->
                        GroupedDivider()
                        GroupedToggleRow(
                            title = item.title,
                            checked = item in profile.equipment,
                            enabled = item != Equipment.MAT,
                            onCheckedChange = { profileStore.toggleEquipment(item) },
                        )
                    }
                }
            }

            // Workout days
            val sessions = profile.sessionsPerWeek
            GroupedSection(
                header = "Workout days",
                footer = "$sessions ${if (sessions == 1) "day" else "days"} a week. Your plan and reminders follow these days. ${profile.recommendedDaysPerWeek} is recommended for your level.",
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    WeekdayPicker(
                        selection = profile.effectiveTrainingDays,
                        onSelectionChange = { days -> profileStore.update { it.copy(trainingDays = days) } },
                    )
                }
            }

            // Time per day
            GroupedSection(header = "Time per day") {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    IOSSegmentedPicker(
                        options = UserProfile.minuteOptions.map { "$it min" },
                        selectedIndex = UserProfile.minuteOptions.indexOf(profile.minutesPerDay),
                        onSelect = { index -> profileStore.update { it.copy(minutesPerDay = UserProfile.minuteOptions[index]) } },
                    )
                }
            }

            // Workouts
            GroupedSection(
                header = "Workouts",
                footer = "A voice talks you through each exercise, counts reps and paces the Hundred's breathing.",
            ) {
                GroupedToggleRow(
                    title = "Voice cues",
                    checked = profileStore.voiceCuesEnabled,
                    onCheckedChange = { profileStore.setVoiceCues(it) },
                )
            }

            // Reminder
            GroupedSection(
                header = "Reminder",
                footer = if (profile.remindersOn) "Sent ${profile.trainingDaysPhrase()}. Nothing on rest days." else null,
            ) {
                GroupedToggleRow(
                    title = "Workout-day reminders",
                    checked = profile.remindersOn,
                    onCheckedChange = { on ->
                        profileStore.update { it.copy(remindersOn = on) }
                        if (on && needsNotificationPermission(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                if (profile.remindersOn) {
                    GroupedDivider()
                    GroupedRow {
                        Text("Time", fontSize = 17.sp, color = colors.text)
                        Spacer(Modifier.weight(1f))
                        TimeValueChip(text = profile.reminderTimeText(), onClick = { editingTime = true })
                    }
                }
            }

            // Start over
            GroupedSection(footer = "Clears your answers and workout history, then starts onboarding again.") {
                GroupedButtonRow("Redo setup", color = DestructiveRed) { confirmingReset = true }
            }
        }
    }

    if (editingTime) {
        IOSTimePickerDialog(
            hour = profile.reminderHour,
            minute = profile.reminderMinute,
            onDismiss = { editingTime = false },
            onConfirm = { h, m ->
                editingTime = false
                profileStore.update { it.copy(reminderHour = h, reminderMinute = m) }
            },
        )
    }

    if (showPaywall) {
        FullScreenDialog(onDismiss = { showPaywall = false }) {
            PaywallScreen(onClose = { showPaywall = false })
        }
    }

    restoreMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { restoreMessage = null },
            title = { Text("Subscription") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { restoreMessage = null }) { Text("OK", color = colors.accent) }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.mutedText,
        )
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text("Start over?") },
            text = { Text("Your answers and workout history will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingReset = false
                    activityStore.resetAll()
                    subscriptions.resetPaywallSeen()
                    profileStore.resetOnboarding()
                }) { Text("Clear and redo setup", color = DestructiveRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) { Text("Cancel", color = colors.accent) }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.mutedText,
        )
    }
}

// Shared screen helpers (used by the other screens in this package)

internal val DestructiveRed = Color(0xFFD32F2F)

/** True on Android 13+ when notifications haven't been allowed yet. */
internal fun needsNotificationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** SF Symbols these screens need that aren't in ui/Icons.kt; everything else goes through [sfIcon]. */
internal fun screenIcon(name: String): ImageVector = when (name) {
    "chevron.left" -> Icons.AutoMirrored.Filled.ArrowBackIos
    "checkmark.square.fill" -> Icons.Filled.CheckBox
    "square" -> Icons.Filled.CheckBoxOutlineBlank
    "pause.fill" -> Icons.Filled.Pause
    "arrow.counterclockwise" -> Icons.Filled.Replay
    "forward.end.fill" -> Icons.Filled.SkipNext
    "xmark" -> Icons.Filled.Close
    "speaker.wave.2.fill" -> Icons.AutoMirrored.Filled.VolumeUp
    "speaker.slash.fill" -> Icons.AutoMirrored.Filled.VolumeOff
    "play.circle.fill" -> Icons.Filled.PlayCircle
    "trash" -> Icons.Filled.Delete
    "line.3.horizontal.decrease.circle", "line.3.horizontal.decrease.circle.fill" -> Icons.Filled.FilterList
    "chevron.down" -> Icons.Filled.KeyboardArrowDown
    else -> sfIcon(name)
}

/** The iPhone large navigation title on tab roots, with optional toolbar buttons at the top right. */
@Composable
internal fun IOSLargeTitle(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    val colors = KP.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            content = actions,
        )
        Text(
            title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

/** An inset-grouped Form section: uppercase header, rounded card of rows, footer. */
@Composable
internal fun GroupedSection(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KP.colors
    Column(Modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                header.uppercase(),
                fontSize = 13.sp,
                color = colors.mutedText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 7.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KP.compactCornerRadius))
                .background(colors.surface)
                .animateContentSize(),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                fontSize = 13.sp,
                color = colors.mutedText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
            )
        }
    }
}

/** A thin separator between rows, inset like iPhone. */
@Composable
internal fun GroupedDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = KP.colors.divider)
}

/** One 44dp-high row of a grouped section. */
@Composable
internal fun GroupedRow(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val base = Modifier.fillMaxWidth()
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        clickable
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A Form button row (accent text, or red for destructive). */
@Composable
internal fun GroupedButtonRow(title: String, color: Color? = null, onClick: () -> Unit) {
    GroupedRow(onClick = onClick) {
        Text(title, fontSize = 17.sp, color = color ?: KP.colors.accent)
    }
}

/** Title (and optional icon) with a switch at the trailing edge. */
@Composable
internal fun GroupedToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    systemImage: String? = null,
    enabled: Boolean = true,
) {
    val colors = KP.colors
    GroupedRow {
        if (systemImage != null) {
            Icon(sfIcon(systemImage), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            fontSize = 17.sp,
            color = if (enabled) colors.text else colors.mutedText,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = kpSwitchColors(),
        )
    }
}

@Composable
internal fun kpSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = KP.colors.accent,
    checkedBorderColor = KP.colors.accent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = KP.colors.divider,
    uncheckedBorderColor = KP.colors.divider,
    disabledCheckedThumbColor = Color.White,
    disabledCheckedTrackColor = KP.colors.accent.copy(alpha = 0.45f),
    disabledCheckedBorderColor = KP.colors.accent.copy(alpha = 0.0f),
    disabledUncheckedTrackColor = KP.colors.divider.copy(alpha = 0.5f),
)

/** A DisclosureGroup header row: title, value, rotating chevron. */
@Composable
private fun DisclosureRow(title: String, value: String, expanded: Boolean, onClick: () -> Unit) {
    val colors = KP.colors
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "disclosure")
    GroupedRow(onClick = onClick) {
        Text(title, fontSize = 17.sp, color = colors.text)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 17.sp, color = colors.mutedText)
        Spacer(Modifier.width(8.dp))
        Icon(
            sfIcon("chevron.right"),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
        )
    }
}

/** The compact time pill iPhone shows for an hour-and-minute DatePicker. */
@Composable
internal fun TimeValueChip(text: String, onClick: () -> Unit) {
    val colors = KP.colors
    Text(
        text,
        fontSize = 17.sp,
        color = colors.text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

/** iPhone's segmented Picker. [selectedIndex] of -1 means nothing selected yet. */
@Composable
internal fun IOSSegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KP.colors
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.divider.copy(alpha = 0.7f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) colors.surface else Color.Transparent)
                    .clickable {
                        if (index != selectedIndex) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(index)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A large bordered / bordered-prominent button with an icon and label (iPhone `Label` in a button). */
@Composable
internal fun IOSLabelButton(
    text: String,
    systemImage: String,
    prominent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KP.colors
    val content = if (prominent) colors.onAccent else colors.accent
    Row(
        modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(KP.compactCornerRadius))
            .background(if (prominent) colors.accent else colors.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(screenIcon(systemImage), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = content, maxLines = 1)
    }
}

/** An hour-and-minute picker in a dialog (the iPhone DatePicker's time wheel). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IOSTimePickerDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val colors = KP.colors
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time") },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK", color = colors.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.accent) }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}
