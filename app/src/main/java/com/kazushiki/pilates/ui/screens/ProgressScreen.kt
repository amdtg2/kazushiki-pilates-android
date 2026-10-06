package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.model.AchievementLibrary
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.model.WorkoutSession
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.components.AchievementBadge
import com.kazushiki.pilates.ui.components.KPBottomSheet
import com.kazushiki.pilates.ui.components.KPEyebrow
import com.kazushiki.pilates.ui.components.StatTile
import com.kazushiki.pilates.ui.components.WeeklyGoalCard
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.kpCard
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Composable
fun ProgressScreen() {
    val activityStore = LocalActivityStore.current
    val profileStore = LocalProfileStore.current
    val colors = KP.colors
    var showLog by remember { mutableStateOf(false) }
    var showAllAchievements by remember { mutableStateOf(false) }

    val target = profileStore.profile.sessionsPerWeek
    val recent = activityStore.sessions.sortedByDescending { it.date }.take(10)
    val weekly = activityStore.weeklyStreak(target)
    val earned = activityStore.earnedAchievements(target)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        IOSLargeTitle(title = "Progress") {
            IconButton(
                onClick = { showLog = true },
                modifier = Modifier.semantics { contentDescription = "Log a workout" },
            ) {
                Icon(sfIcon("plus"), contentDescription = null, tint = colors.accent)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { StatTile(value = "${activityStore.streak()}", label = "day streak") }
                    Box(Modifier.weight(1f)) {
                        StatTile(value = "${weekly.weeks}", label = if (weekly.weeks == 1) "week streak" else "weeks in a row")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { StatTile(value = "${activityStore.sessions.size}", label = "workouts") }
                    Box(Modifier.weight(1f)) { StatTile(value = "${activityStore.totalMinutes}", label = "minutes") }
                }
            }

            WeeklyGoalCard(weekly = weekly)

            AchievementsSection(earned = earned, onSeeAll = { showAllAchievements = true })

            MonthCalendar(activeDays = activityStore.activeDays())

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KPEyebrow(text = "Recent workouts")
                if (recent.isEmpty()) {
                    Text(
                        "Your finished workouts will show up here, including ones you log yourself.",
                        fontSize = 17.sp,
                        color = colors.mutedText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .kpCard(),
                    )
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .kpCard(padding = 14.dp),
                    ) {
                        recent.forEachIndexed { index, session ->
                            RecentSessionRow(session = session, onDelete = { activityStore.delete(session) })
                            if (index != recent.lastIndex) {
                                HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllAchievements) {
        AchievementsSheet(earned = earned, onDismiss = { showAllAchievements = false })
    }
    if (showLog) {
        LogWorkoutSheet(onDismiss = { showLog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentSessionRow(session: WorkoutSession, onDelete: () -> Unit) {
    val colors = KP.colors
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val areas = session.bodyAreas
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (session.isManual == true) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        }
                    },
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(session.workoutTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                Text(formatSessionDate(session.date), fontSize = 12.sp, color = colors.mutedText)
                if (!areas.isNullOrEmpty()) {
                    Text(areas.joinToString(" · ") { it.title }, fontSize = 12.sp, color = colors.accent)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${session.minutes} min",
                    style = TextStyle(fontSize = 15.sp, color = colors.text, fontFeatureSettings = "tnum"),
                )
                val rating = session.rating
                if (rating != null) {
                    Text(rating.title, fontSize = 12.sp, color = colors.accent)
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Delete this log", color = DestructiveRed) },
                leadingIcon = { Icon(screenIcon("trash"), contentDescription = null, tint = DestructiveRed) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/** "Oct 6, 2026 at 3:45 PM" (date abbreviated, time shortened). */
private fun formatSessionDate(date: Instant): String {
    val zoned = date.atZone(KPCalendar.current.zone)
    val day = zoned.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val time = zoned.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return "$day at $time"
}

/** Earned badges first, then the next few to aim for. */
@Composable
private fun AchievementsSection(earned: Set<String>, onSeeAll: () -> Unit) {
    val colors = KP.colors
    val unlocked = AchievementLibrary.all.filter { it.id in earned }
    val locked = AchievementLibrary.all.filter { it.id !in earned }
    val shown = (unlocked.takeLast(4) + locked.take(4)).take(8)

    Column(
        Modifier
            .fillMaxWidth()
            .kpCard(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KPEyebrow(text = "Achievements")
            Spacer(Modifier.width(8.dp))
            Text("${unlocked.size} of ${AchievementLibrary.all.size}", fontSize = 12.sp, color = colors.mutedText)
            Spacer(Modifier.weight(1f))
            Text(
                "See all",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            shown.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { achievement ->
                        val isEarned = achievement.id in earned
                        Column(
                            Modifier
                                .weight(1f)
                                .clearAndSetSemantics {
                                    contentDescription = "${achievement.title}, ${if (isEarned) "earned" else "locked"}. ${achievement.detail}"
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AchievementBadge(achievement = achievement, isEarned = isEarned, size = 54.dp)
                            Text(
                                achievement.title,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEarned) colors.text else colors.mutedText,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                minLines = 2,
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Every achievement, earned and still to come. */
@Composable
fun AchievementsSheet(earned: Set<String>, onDismiss: () -> Unit) {
    val colors = KP.colors
    KPBottomSheet(onDismiss = onDismiss) {
        // Inline navigation bar: title in the middle, Done on the right.
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                "Achievements",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                modifier = Modifier.align(Alignment.Center),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text("Done", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KP.compactCornerRadius))
                    .background(colors.surface),
            ) {
                AchievementLibrary.all.forEachIndexed { index, achievement ->
                    val isEarned = achievement.id in earned
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "${achievement.title}. ${achievement.detail}. ${if (isEarned) "Earned" else "Locked"}" }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AchievementBadge(achievement = achievement, isEarned = isEarned, size = 46.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                achievement.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEarned) colors.text else colors.mutedText,
                            )
                            Text(achievement.detail, fontSize = 15.sp, color = colors.mutedText)
                        }
                        if (isEarned) {
                            Icon(sfIcon("checkmark.circle.fill"), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                        }
                    }
                    if (index != AchievementLibrary.all.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = 76.dp), thickness = 0.5.dp, color = colors.divider)
                    }
                }
            }
        }
    }
}

/** The current month with workout days filled in. */
@Composable
fun MonthCalendar(activeDays: Set<Instant>, month: Instant = Instant.now()) {
    val colors = KP.colors
    val calendar = remember { KPCalendar.current }
    val today = calendar.startOfDay(Instant.now())
    val zoned = month.atZone(calendar.zone)
    val title = zoned.format(DateTimeFormatter.ofPattern("LLLL yyyy", calendar.locale))
    val symbols = UserProfile.orderedWeekdays(calendar).map { calendar.veryShortWeekdaySymbol(it) }

    // Days of the month, padded with null so the first day lands in its weekday column.
    val firstDay = zoned.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant()
    val count = YearMonth.from(zoned).lengthOfMonth()
    val leading = (calendar.weekday(firstDay) - calendar.firstWeekday + 7) % 7
    val days: List<Instant?> = List(leading) { null } + (0 until count).map { calendar.addDays(firstDay, it.toLong()) }
    val dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

    Column(
        Modifier
            .fillMaxWidth()
            .kpCard(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                symbols.forEach { symbol ->
                    Text(
                        symbol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 34.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day != null) {
                                val isActive = day in activeDays
                                val isToday = day == today
                                val label = day.atZone(calendar.zone).format(dayFormatter) + if (isActive) ", workout done" else ""
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(if (isActive) colors.accent else Color.Transparent)
                                        .border(1.5.dp, if (isToday && !isActive) colors.accent else Color.Transparent, CircleShape)
                                        .clearAndSetSemantics { contentDescription = label },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${calendar.dayOfMonth(day)}",
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            color = if (isActive) colors.onAccent else colors.text,
                                            fontFeatureSettings = "tnum",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
