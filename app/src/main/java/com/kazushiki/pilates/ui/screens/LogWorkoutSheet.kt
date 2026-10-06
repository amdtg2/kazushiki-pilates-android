package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.BodyArea
import com.kazushiki.pilates.model.CelebrationSummary
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.ManualActivity
import com.kazushiki.pilates.model.SessionRating
import com.kazushiki.pilates.model.WorkoutSession
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.components.CelebrationContent
import com.kazushiki.pilates.ui.components.KPBottomSheet
import com.kazushiki.pilates.ui.components.KPPrimaryButton
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import java.time.Instant

private val quickMinutes = listOf(15, 20, 30, 45, 60)

/** How far back a workout can be logged. */
private const val DAYS_BACK = 7

/** Log a workout done outside the app so it counts toward the streak and weekly total. */
@Composable
fun LogWorkoutSheet(onDismiss: () -> Unit) {
    val activityStore = LocalActivityStore.current
    val profileStore = LocalProfileStore.current
    val colors = KP.colors

    /** Set after saving, to show a little celebration before closing. */
    var celebration by remember { mutableStateOf<CelebrationSummary?>(null) }
    var activity by remember { mutableStateOf(ManualActivity.PILATES) }
    var minutes by remember { mutableStateOf(30) }
    var areas by remember { mutableStateOf<Set<BodyArea>>(emptySet()) }
    var rating by remember { mutableStateOf<SessionRating?>(null) }
    var date by remember { mutableStateOf(Instant.now()) }

    fun save() {
        if (areas.isEmpty()) return
        val session = loggedSession(activity = activity, minutes = minutes, areas = areas, rating = rating, date = date)
        val summary = CelebrationSummary.make(session = session, history = activityStore.sessions, profile = profileStore.profile)
        activityStore.log(session)
        celebration = summary
    }

    KPBottomSheet(onDismiss = onDismiss) {
        val shown = celebration
        if (shown != null) {
            // Celebration after saving
            Box(Modifier.fillMaxWidth().height(44.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .padding(top = 12.dp),
            ) {
                CelebrationContent(summary = shown)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                HeavyProminentButton(text = "Done", onClick = onDismiss)
            }
        } else {
            // Inline navigation bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 8.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancel", fontSize = 17.sp, color = colors.accent)
                }
                Text(
                    "Log a workout",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    modifier = Modifier.align(Alignment.Center),
                )
                TextButton(
                    onClick = { save() },
                    enabled = areas.isNotEmpty(),
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text(
                        "Save",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (areas.isNotEmpty()) colors.accent else colors.mutedText,
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                GroupedSection(header = "What did you do?") {
                    ChipGrid(ManualActivity.entries, columns = 3) { item ->
                        FormChip(title = item.title, systemImage = item.systemImage, isSelected = activity == item) {
                            activity = item
                        }
                    }
                }

                GroupedSection(header = "How long was it?") {
                    ChipGrid(quickMinutes, columns = 5) { value ->
                        FormChip(title = "$value", isSelected = minutes == value) { minutes = value }
                    }
                    GroupedDivider()
                    GroupedRow {
                        Text(
                            "$minutes minutes",
                            style = TextStyle(fontSize = 17.sp, color = colors.text, fontFeatureSettings = "tnum"),
                            modifier = Modifier.weight(1f),
                        )
                        MinuteStepper(
                            canDecrement = minutes > 5,
                            canIncrement = minutes < 180,
                            onDecrement = { minutes = (minutes - 5).coerceIn(5, 180) },
                            onIncrement = { minutes = (minutes + 5).coerceIn(5, 180) },
                        )
                    }
                }

                GroupedSection(header = "What did you work?", footer = "Pick everything that applies.") {
                    ChipGrid(BodyArea.entries, columns = 3) { area ->
                        FormChip(title = area.title, isSelected = area in areas) {
                            areas = if (area in areas) areas - area else areas + area
                        }
                    }
                }

                GroupedSection(header = "How did it feel?") {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        IOSSegmentedPicker(
                            options = SessionRating.entries.map { it.title },
                            selectedIndex = rating?.let { SessionRating.entries.indexOf(it) } ?: -1,
                            onSelect = { rating = SessionRating.entries[it] },
                        )
                    }
                }

                GroupedSection(footer = "Counts toward your streak and weekly goal. Your plan stays on the same day.") {
                    GroupedRow {
                        Text("When", fontSize = 17.sp, color = colors.text)
                    }
                    DayChips(selected = date, onSelect = { date = it })
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                KPPrimaryButton(
                    text = if (areas.isEmpty()) "Pick what you worked" else "Log $minutes min",
                    onClick = { save() },
                    enabled = areas.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Builds the logged session. Today keeps the current time; earlier days are logged at noon. */
fun loggedSession(
    activity: ManualActivity,
    minutes: Int,
    areas: Set<BodyArea>,
    rating: SessionRating?,
    date: Instant,
    now: Instant = Instant.now(),
    calendar: KPCalendar = KPCalendar.current,
): WorkoutSession {
    val whenLogged = if (calendar.isSameDay(date, now)) now else calendar.atTime(date, 12, 0)
    return WorkoutSession(
        date = whenLogged,
        workoutID = "manual",
        workoutTitle = "${activity.title} on my own",
        seconds = (minutes * 60).toDouble(),
        rating = rating,
        isManual = true,
        activity = activity,
        bodyAreas = BodyArea.entries.filter { it in areas },
    )
}

/** The bold full-width button pinned under the celebration. */
@Composable
internal fun HeavyProminentButton(text: String, onClick: () -> Unit) {
    val colors = KP.colors
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(KP.compactCornerRadius))
            .background(colors.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.onAccent)
    }
}

/** A fixed-column grid of chips (rows of equal-width cells). */
@Composable
private fun <T> ChipGrid(items: List<T>, columns: Int, chip: @Composable (T) -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    Box(Modifier.weight(1f)) { chip(item) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A selectable pill used in forms. */
@Composable
private fun FormChip(title: String, isSelected: Boolean, systemImage: String? = null, onClick: () -> Unit) {
    val colors = KP.colors
    val haptics = LocalHapticFeedback.current
    val content = if (isSelected) colors.onAccent else colors.text
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (systemImage == null) 36.dp else 58.dp)
            .clip(RoundedCornerShape(KP.compactCornerRadius))
            .background(if (isSelected) colors.accent else colors.accentSoft)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        if (systemImage != null) {
            Icon(sfIcon(systemImage), contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** iPhone's Stepper: a rounded − | + control. */
@Composable
private fun MinuteStepper(
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val colors = KP.colors
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(47.dp)
                .fillMaxHeight()
                .clickable(enabled = canDecrement, onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Text("−", fontSize = 22.sp, color = if (canDecrement) colors.text else colors.divider)
        }
        VerticalDivider(Modifier.height(18.dp), thickness = 1.dp, color = colors.divider)
        Box(
            Modifier
                .width(47.dp)
                .fillMaxHeight()
                .clickable(enabled = canIncrement, onClick = onIncrement),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontSize = 22.sp, color = if (canIncrement) colors.text else colors.divider)
        }
    }
}

/** The last eight days as chips: Today, Yesterday, then weekday names. */
@Composable
private fun DayChips(selected: Instant, onSelect: (Instant) -> Unit) {
    val calendar = remember { KPCalendar.current }
    val today = calendar.startOfDay(Instant.now())
    val days = (0..DAYS_BACK).map { calendar.addDays(today, -it.toLong()) }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEachIndexed { index, day ->
            val title = when (index) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> calendar.weekdayName(calendar.weekday(day))
            }
            val isSelected = calendar.isSameDay(selected, day)
            Box(Modifier.width(if (index == 1) 104.dp else 96.dp)) {
                FormChip(title = title, isSelected = isSelected) {
                    // Today keeps "now"; other days are stored as that day and logged at noon.
                    onSelect(if (index == 0) Instant.now() else day)
                }
            }
        }
    }
}
