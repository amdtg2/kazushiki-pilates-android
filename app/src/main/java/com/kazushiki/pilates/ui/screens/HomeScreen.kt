package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.Plan
import com.kazushiki.pilates.model.PlanDay
import com.kazushiki.pilates.model.Workout
import com.kazushiki.pilates.model.WorkoutLibrary
import com.kazushiki.pilates.model.WorkoutTimeline
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.LocalSubscriptionStore
import com.kazushiki.pilates.ui.Route
import com.kazushiki.pilates.ui.components.FullScreenDialog
import com.kazushiki.pilates.ui.components.KPActionTile
import com.kazushiki.pilates.ui.components.KPDestructive
import com.kazushiki.pilates.ui.components.KPFilledButton
import com.kazushiki.pilates.ui.components.KPLabel
import com.kazushiki.pilates.ui.components.KPPageHeader
import com.kazushiki.pilates.ui.components.KPSecondaryButton
import com.kazushiki.pilates.ui.components.KPSectionLabel
import com.kazushiki.pilates.ui.components.KPTag
import com.kazushiki.pilates.ui.components.ProgramRowContent
import com.kazushiki.pilates.ui.components.WeekStreakLabel
import com.kazushiki.pilates.ui.components.WorkoutTile
import com.kazushiki.pilates.ui.components.kpFont
import com.kazushiki.pilates.ui.components.kpOutlined
import com.kazushiki.pilates.ui.components.kpStatusBarPadding
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import java.time.Instant
import kotlin.math.max

/** A workout to open in the full-screen player. */
private data class PlayRequest(
    val workout: Workout,
    val timeline: WorkoutTimeline,
    val planID: String?,
    val dayNumber: Int?,
)

/**
 * Home: a bold header, today's workout from her program (or a nudge to pick one),
 * shortcuts to build a workout or browse programs, this week, then quick sessions.
 */
@Composable
fun HomeScreen() {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val activityStore = LocalActivityStore.current
    val subscriptions = LocalSubscriptionStore.current
    val router = LocalRouter.current

    var showPaywall by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<PlayRequest?>(null) }
    var leaving by remember { mutableStateOf<Plan?>(null) }

    val active = activityStore.activePrograms(profileStore.profile)
    val program = active.firstOrNull()
    val next = program?.let { activityStore.nextDay(it) }
    val nextWorkout = next?.workout

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .kpStatusBarPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        KPPageHeader(
            eyebrow = greeting(),
            title = "Build the strength you want to feel.",
            subtitle = PilatesGoal.entries
                .filter { it in profileStore.profile.goals }
                .joinToString(" • ") { it.title },
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
        )

        if (program != null && next != null && nextWorkout != null) {
            TodayCard(
                plan = program,
                day = next,
                workout = nextWorkout,
                onPlay = { playing = it },
                onPaywall = { showPaywall = true },
                onLeave = { leaving = it },
            )
        } else {
            TryProgramCard()
        }

        // Both open the Challenges tab, straight to the matching screen.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KPActionTile(
                systemImage = "bolt.fill",
                title = "Quick workout",
                subtitle = "Build a session",
                modifier = Modifier.weight(1f),
                onClick = { router.openChallenges(Route.QuickWorkout) },
            )
            KPActionTile(
                systemImage = "flame.fill",
                title = "Challenges",
                subtitle = "Join a challenge",
                modifier = Modifier.weight(1f),
                onClick = { router.openChallenges(Route.ChallengeBrowse) },
            )
        }

        WeekCard(onLog = { showLog = true })

        if (active.size > 1) {
            OtherPrograms(programs = active.drop(1), onLeave = { leaving = it })
        }

        if (!subscriptions.isPremium) {
            PremiumRow(onClick = { showPaywall = true })
        }

        QuickSessions(excluding = next?.workoutID)
    }

    if (showPaywall) {
        FullScreenDialog(onDismiss = { showPaywall = false }) {
            PaywallScreen(onClose = { showPaywall = false })
        }
    }

    if (showLog) {
        LogWorkoutSheet(onDismiss = { showLog = false })
    }

    val leavingPlan = leaving
    if (leavingPlan != null) {
        AlertDialog(
            onDismissRequest = { leaving = null },
            title = { Text("Leave ${leavingPlan.title}?") },
            text = {
                Text("It comes off your home screen. Workouts you've done stay in your history, and you can rejoin any time.")
            },
            confirmButton = {
                TextButton(onClick = {
                    activityStore.leave(leavingPlan.id)
                    leaving = null
                }) {
                    Text("Leave", color = KPDestructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { leaving = null }) {
                    Text("Cancel", color = c.accent)
                }
            },
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.mutedText,
        )
    }

    val request = playing
    if (request != null) {
        FullScreenDialog(onDismiss = { playing = null }) {
            WorkoutPlayerScreen(
                workout = request.workout,
                timeline = request.timeline,
                planID = request.planID,
                dayNumber = request.dayNumber,
                onClose = { playing = null },
            )
        }
    }
}

// Header

private fun greeting(): String = when (KPCalendar.current.hour(Instant.now())) {
    in 5 until 12 -> "Good morning"
    in 12 until 17 -> "Good afternoon"
    else -> "Good evening"
}

// Today's workout

/** The highlighted card style for today's workout: surface, accent edge and a soft glow. */
@Composable
private fun Modifier.heroCard(): Modifier {
    val c = KP.colors
    val shape = RoundedCornerShape(24.dp)
    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = c.accent.copy(alpha = 0.12f),
            spotColor = c.accent.copy(alpha = 0.12f),
        )
        .background(c.surface, shape)
        .border(1.dp, c.accent.copy(alpha = 0.35f), shape)
}

@Composable
private fun TodayCard(
    plan: Plan,
    day: PlanDay,
    workout: Workout,
    onPlay: (PlayRequest) -> Unit,
    onPaywall: () -> Unit,
    onLeave: (Plan) -> Unit,
) {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val subscriptions = LocalSubscriptionStore.current
    val router = LocalRouter.current
    val timeline = WorkoutTimeline(workout, profileStore.profile.equipment)
    val unlocked = subscriptions.canAccess(workout)
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heroCard()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TODAY'S WORKOUT",
                style = kpFont(12, FontWeight.ExtraBold, tracking = 2.5f),
                color = c.accent,
            )
            Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
            Text(
                text = "~${timeline.estimatedMinutes} MIN",
                style = kpFont(12, FontWeight.ExtraBold, tracking = 1f),
                color = c.mutedText,
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { menuOpen = true }
                        .semantics { contentDescription = "Program options" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = sfIcon("ellipsis"),
                        contentDescription = null,
                        tint = c.mutedText,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(if (plan.isChallenge) "View challenge" else "View program", color = c.text) },
                        onClick = {
                            menuOpen = false
                            router.openChallenges(Route.PlanDetail(plan))
                        },
                        trailingIcon = {
                            Icon(sfIcon("list.bullet"), contentDescription = null, tint = c.text)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (plan.isChallenge) "Leave challenge" else "Leave program", color = KPDestructive) },
                        onClick = {
                            menuOpen = false
                            onLeave(plan)
                        },
                        trailingIcon = {
                            Icon(sfIcon("rectangle.portrait.and.arrow.right"), contentDescription = null, tint = KPDestructive)
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = workout.title,
                style = kpFont(28, FontWeight.ExtraBold),
                color = c.text,
            )
            Text(
                text = workout.focus.joinToString(" • ") { it.title },
                style = kpFont(17),
                color = c.mutedText,
            )
        }

        // ViewThatFits: the tags in a row when they fit, otherwise stacked.
        RowOrColumn(spacing = 8.dp) {
            KPTag(text = "${timeline.exercises.size} exercises")
            KPTag(text = "Day ${day.number} of ${plan.days.size}")
            KPTag(text = plan.title)
        }

        KPFilledButton(
            text = if (unlocked) "START WORKOUT" else "UNLOCK WORKOUT",
            systemImage = if (unlocked) "play.fill" else "lock.fill",
            onClick = {
                if (unlocked) {
                    onPlay(PlayRequest(workout, timeline, plan.id, day.number))
                } else {
                    onPaywall()
                }
            },
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5f,
            extraVerticalPadding = 8.dp,
            modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            text = "Workout details",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { router.push(Route.WorkoutPreview(workout, plan.id, day.number)) }
                .padding(vertical = 4.dp),
            style = kpFont(15, FontWeight.SemiBold),
            color = c.accent,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * SwiftUI `ViewThatFits(in: .horizontal)` over an HStack and a VStack: lays the children out
 * in a row when their natural widths fit, otherwise in a leading-aligned column.
 */
@Composable
private fun RowOrColumn(spacing: Dp, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val idealWidth = measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) } +
            gap * max(measurables.size - 1, 0)
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        if (idealWidth <= constraints.maxWidth) {
            val width = placeables.sumOf { it.width } + gap * max(placeables.size - 1, 0)
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height.coerceAtLeast(constraints.minHeight)) {
                var x = 0
                placeables.forEach {
                    it.placeRelative(x, (height - it.height) / 2)
                    x += it.width + gap
                }
            }
        } else {
            val width = placeables.maxOfOrNull { it.width } ?: 0
            val height = placeables.sumOf { it.height } + gap * max(placeables.size - 1, 0)
            layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height.coerceAtLeast(constraints.minHeight)) {
                var y = 0
                placeables.forEach {
                    it.placeRelative(0, y)
                    y += it.height + gap
                }
            }
        }
    }
}

@Composable
private fun TryProgramCard() {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val activityStore = LocalActivityStore.current
    val router = LocalRouter.current
    val finished = activityStore.lastFinishedProgram(profileStore.profile)
    val heading = if (finished != null) "You finished ${finished.title}" else "Find your challenge"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heroCard()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (finished == null) "START HERE" else "COMPLETE",
            style = kpFont(12, FontWeight.ExtraBold, tracking = 2.5f),
            color = c.accent,
        )
        Text(text = heading, style = kpFont(28, FontWeight.ExtraBold), color = c.text)
        Text(
            text = if (finished == null) {
                "Join a challenge and you'll get a workout lined up for each day, so you always know what's next."
            } else {
                "Pick your next challenge to keep the momentum going."
            },
            style = kpFont(17),
            color = c.mutedText,
        )
        KPFilledButton(
            text = "JOIN A CHALLENGE",
            onClick = { router.openChallenges(Route.ChallengeBrowse) },
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5f,
            extraVerticalPadding = 8.dp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OtherPrograms(programs: List<Plan>, onLeave: (Plan) -> Unit) {
    val c = KP.colors
    val router = LocalRouter.current
    var menuFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KPSectionLabel(text = "Also in progress")
        for (program in programs) {
            // A long press opens the context menu, like iPhone's .contextMenu.
            Box {
                ProgramRowContent(
                    plan = program,
                    modifier = Modifier
                        .clip(RoundedCornerShape(KP.cornerRadius))
                        .combinedClickable(
                            onClick = { router.openChallenges(Route.PlanDetail(program)) },
                            onLongClick = { menuFor = program.id },
                        ),
                )
                DropdownMenu(
                    expanded = menuFor == program.id,
                    onDismissRequest = { menuFor = null },
                ) {
                    DropdownMenuItem(
                        text = { Text("Leave program", color = KPDestructive) },
                        onClick = {
                            menuFor = null
                            onLeave(program)
                        },
                        trailingIcon = {
                            Icon(sfIcon("rectangle.portrait.and.arrow.right"), contentDescription = null, tint = KPDestructive)
                        },
                    )
                }
            }
        }
    }
}

// This week

@Composable
private fun WeekCard(onLog: () -> Unit) {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val activityStore = LocalActivityStore.current
    val calendar = KPCalendar.current
    val now = Instant.now()
    val start = calendar.startOfWeek(now)
    val days = (0 until 7).map { calendar.addDays(start, it.toLong()) }
    val activeDays = activityStore.activeDays(calendar)
    val today = calendar.startOfDay(now)
    val done = activityStore.sessionsInWeekOf(now, calendar).size
    val target = profileStore.profile.sessionsPerWeek
    val planned = profileStore.profile.effectiveTrainingDays
    val streak = activityStore.streak(now, calendar)
    val weekly = activityStore.weeklyStreak(target, now, calendar)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .kpOutlined()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KPSectionLabel(text = "This week")
            Text(text = "$done of $target done", style = kpFont(15), color = c.mutedText)
            Spacer(Modifier.weight(1f))
            KPLabel(
                text = "$streak",
                systemImage = "flame.fill",
                style = kpFont(15, FontWeight.ExtraBold),
                color = c.prop,
                spacing = 4.dp,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$streak day streak" },
            )
        }

        // Weekly streak: weeks in a row with her goal met, and what keeps it going.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WeekStreakLabel(weeks = weekly.weeks)
                Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
                Row(
                    modifier = Modifier.clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(max(target, 1)) { index ->
                        Box(
                            Modifier
                                .size(width = 14.dp, height = 6.dp)
                                .background(if (index < done) c.accent else c.divider, RoundedCornerShape(percent = 50)),
                        )
                    }
                }
            }
            Text(
                text = weekly.status,
                style = kpFont(15, FontWeight.SemiBold),
                color = if (weekly.goalMetThisWeek) c.accent else c.mutedText,
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            for (day in days) {
                val isActive = day in activeDays
                val isToday = day == today
                val weekday = calendar.weekday(day)
                val isPlanned = weekday in planned
                val label = calendar.weekdayName(weekday) +
                    (if (isActive) ", workout done" else if (isPlanned) ", workout day" else "")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = calendar.veryShortWeekdaySymbol(weekday),
                        style = kpFont(12, if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold),
                        color = if (isToday) c.text else c.mutedText,
                    )
                    val fill = when {
                        isActive -> c.accent
                        isPlanned -> c.accentSoft
                        else -> c.background
                    }
                    var circle = Modifier
                        .size(34.dp)
                        .background(fill, CircleShape)
                    if (isToday && !isActive) {
                        circle = circle.border(2.dp, c.text, CircleShape)
                    }
                    Box(modifier = circle, contentAlignment = Alignment.Center) {
                        if (isActive) {
                            Icon(
                                imageVector = sfIcon("checkmark"),
                                contentDescription = null,
                                tint = c.onAccent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        KPSecondaryButton(
            text = "I did a self workout",
            systemImage = "checkmark.circle.fill",
            onClick = onLog,
            modifier = Modifier.semantics {
                contentDescription = "I did a self workout. Log a workout you did outside the app so your streak keeps going"
            },
        )
    }
}

// Premium

@Composable
private fun PremiumRow(onClick: () -> Unit) {
    val c = KP.colors
    val subscriptions = LocalSubscriptionStore.current
    val shape = RoundedCornerShape(KP.cornerRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .background(c.accentSoft, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = sfIcon("sparkles"),
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = if (subscriptions.isEligibleForTrial) "Try Premium free for 7 days" else "Go Premium",
                style = kpFont(15, FontWeight.SemiBold),
                color = c.text,
            )
            Text(
                text = "Every program, workout and challenge",
                style = kpFont(12),
                color = c.mutedText,
            )
        }
        Icon(
            imageVector = sfIcon("chevron.right"),
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(18.dp),
        )
    }
}

// Quick sessions

/** Lets a child reach past its parent's horizontal padding (iPhone's `.padding(.horizontal, -16)`). */
private fun Modifier.bleedHorizontally(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = (amount * 2).roundToPx()
    val wide = if (constraints.hasBoundedWidth) {
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra,
        )
    } else {
        constraints
    }
    val placeable = measurable.measure(wide)
    val width = if (constraints.hasBoundedWidth) {
        (placeable.width - extra).coerceIn(constraints.minWidth, constraints.maxWidth)
    } else {
        placeable.width
    }
    layout(width, placeable.height) {
        placeable.placeRelative(if (constraints.hasBoundedWidth) -extra / 2 else 0, 0)
    }
}

@Composable
private fun QuickSessions(excluding: String?) {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val router = LocalRouter.current
    val profile = profileStore.profile
    val available = WorkoutLibrary.all.filter {
        it.isAvailable(profile.equipment + Equipment.MAT) && it.id != excluding
    }
    val picks = (available.filter { it.level == profile.level } + available.filter { it.level != profile.level }).take(8)

    Column(
        modifier = Modifier.padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KPSectionLabel(text = "Quick sessions")
            Spacer(Modifier.weight(1f))
            Text(
                text = "See all",
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { router.push(Route.AllWorkouts) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                style = kpFont(15, FontWeight.SemiBold),
                color = c.accent,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bleedHorizontally(16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (workout in picks) {
                WorkoutTile(workout = workout, onClick = { router.push(Route.WorkoutPreview(workout)) })
            }
        }
    }
}
