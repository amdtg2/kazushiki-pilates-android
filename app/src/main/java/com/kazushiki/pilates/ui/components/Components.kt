package com.kazushiki.pilates.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.figure.FigureView
import com.kazushiki.pilates.figure.previewPose
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.Plan
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.model.Workout
import com.kazushiki.pilates.model.WorkoutTimeline
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalSubscriptionStore
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.kpCard
import kotlin.math.max

/** iPhone's linear `ProgressView` tinted with the accent. */
@Composable
internal fun KPLinearProgress(done: Int, total: Int, modifier: Modifier = Modifier) {
    val c = KP.colors
    val fraction = (done.toFloat() / max(total, 1).toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(c.divider),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.accent),
            )
        }
    }
}

/** A still thumbnail of a workout's first exercise (its middle pose), on the stage color. */
@Composable
internal fun WorkoutThumbnail(timeline: WorkoutTimeline, modifier: Modifier = Modifier) {
    val first = timeline.exercises.firstOrNull() ?: return
    val sequence = first.exercise.sequence
    val pose = first.variation.pose(sequence[sequence.size / 2].pose)
    Box(
        modifier = modifier.background(KP.colors.stage, RoundedCornerShape(KP.compactCornerRadius)),
    ) {
        FigureView(pose = pose, props = first.exercise.props, modifier = Modifier.fillMaxWidth())
    }
}

/** A dropdown section: a tappable header with a count that shows or hides its rows. */
@Composable
fun KPCollapsible(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
    dimmed: Boolean = false,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = KP.colors
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(250),
        label = "chevron",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 14.dp)
                .semantics { stateDescription = if (isExpanded) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = kpFont(17, FontWeight.SemiBold),
                    color = if (dimmed) c.mutedText else c.text,
                )
                if (subtitle != null) {
                    Text(text = subtitle, style = kpFont(12), color = c.mutedText)
                }
            }
            if (count != null) {
                Text(
                    text = "$count",
                    modifier = Modifier
                        .background(c.accentSoft, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = kpFont(12, FontWeight.SemiBold, monospacedDigits = true),
                    color = c.accent,
                )
            }
            // chevron.down (rotated -90° when collapsed): the mapped chevron.right turned 0°/90°.
            Icon(
                imageVector = sfIcon("chevron.right"),
                contentDescription = null,
                tint = c.mutedText,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(250)) + expandVertically(tween(250)),
            exit = fadeOut(tween(250)) + shrinkVertically(tween(250)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.divider),
        )
    }
}

/** A small ring showing how much of a plan is done. */
@Composable
fun ProgressRing(done: Int, total: Int, modifier: Modifier = Modifier) {
    val c = KP.colors
    val progress = if (total > 0) done.toFloat() / total.toFloat() else 0f
    Box(
        modifier = modifier
            .size(34.dp)
            .clearAndSetSemantics { contentDescription = "$done of $total days done" },
        contentAlignment = Alignment.Center,
    ) {
        val divider = c.divider
        val accent = c.accent
        Canvas(Modifier.size(34.dp)) {
            val stroke = 4.dp.toPx()
            drawCircle(color = divider, radius = size.minDimension / 2f, style = Stroke(width = stroke))
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceAtMost(1f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        if (done >= total && total > 0) {
            Icon(
                imageVector = sfIcon("checkmark"),
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** The body of [ProgramRow] without the tap handling, so callers can add their own gestures. */
@Composable
internal fun ProgramRowContent(plan: Plan, modifier: Modifier = Modifier, needs: Equipment? = null) {
    val c = KP.colors
    val activityStore = LocalActivityStore.current
    val done = activityStore.completedDays(plan.id).size
    val perWeek = plan.days.count { it.week == 1 }
    val length: List<String?> =
        if (plan.isChallenge) listOf("${plan.days.size} days") else listOf("${plan.weeks} wk", "$perWeek× a week")
    val parts: List<String?> = listOf(plan.level?.title) + length + listOf(needs?.let { "Needs ${it.title.lowercase()}" })
    val meta = parts.filterNotNull().joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (needs == null) 1f else 0.75f)
            .kpCard(padding = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProgressRing(done = done, total = plan.days.size)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = plan.title, style = kpFont(17, FontWeight.SemiBold), color = c.text)
            Text(text = meta, style = kpFont(12), color = if (needs == null) c.mutedText else c.accent)
        }
        Icon(
            imageVector = sfIcon("chevron.right"),
            contentDescription = null,
            tint = c.mutedText,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A compact one-line program row for lists. */
@Composable
fun ProgramRow(plan: Plan, modifier: Modifier = Modifier, needs: Equipment? = null, onClick: () -> Unit) {
    ProgramRowContent(
        plan = plan,
        needs = needs,
        modifier = modifier
            .clip(RoundedCornerShape(KP.cornerRadius))
            .clickable(onClick = onClick),
    )
}

/** A small workout card for horizontal carousels. */
@Composable
fun WorkoutTile(workout: Workout, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val subscriptions = LocalSubscriptionStore.current
    val timeline = WorkoutTimeline(workout, profileStore.profile.equipment)
    val shape = RoundedCornerShape(KP.cornerRadius)

    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .kpOutlined(radius = KP.cornerRadius)
            .padding(10.dp)
            .width(156.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkoutThumbnail(timeline = timeline, modifier = Modifier.fillMaxWidth())
        Text(
            text = workout.title,
            style = kpFont(15, FontWeight.SemiBold),
            color = c.text,
            maxLines = 2,
            minLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${timeline.estimatedMinutes} min · ${workout.level.title}",
                style = kpFont(12),
                color = c.mutedText,
            )
            if (!subscriptions.canAccess(workout)) {
                Icon(
                    imageVector = sfIcon("lock.fill"),
                    contentDescription = "Premium",
                    tint = c.accent,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/** A program card for horizontal carousels. */
@Composable
fun ProgramTile(plan: Plan, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KP.colors
    val activityStore = LocalActivityStore.current
    val done = activityStore.completedDays(plan.id).size
    val perWeek = plan.days.count { it.week == 1 }
    val shape = RoundedCornerShape(KP.cornerRadius)

    Column(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .kpOutlined(radius = KP.cornerRadius)
            .padding(14.dp)
            .width(200.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KPEyebrow(text = plan.focus?.title ?: plan.category.title)
        Text(
            text = plan.title,
            style = kpFont(17, FontWeight.SemiBold),
            color = c.text,
            maxLines = 2,
            minLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(plan.level?.title, "${plan.weeks} wk", "$perWeek× a week").joinToString(" · "),
            style = kpFont(12),
            color = c.mutedText,
        )
        KPLinearProgress(done = done, total = plan.days.size, modifier = Modifier.padding(top = 4.dp))
    }
}

/** An exercise row with a still thumbnail of its key pose. */
@Composable
fun ExerciseRow(exercise: Exercise, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KP.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KP.cornerRadius))
            .clickable(onClick = onClick)
            .kpCard(padding = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(96.dp)
                .background(c.stage, RoundedCornerShape(KP.compactCornerRadius)),
        ) {
            FigureView(pose = previewPose(exercise), props = exercise.props, modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = exercise.name, style = kpFont(17, FontWeight.SemiBold), color = c.text)
            Text(
                text = exercise.bodyAreas.joinToString(" · ") { it.title },
                style = kpFont(12),
                color = c.mutedText,
            )
        }
        Icon(
            imageVector = sfIcon("chevron.right"),
            contentDescription = null,
            tint = c.mutedText,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A section title with a "See all" link. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onSeeAll: () -> Unit) {
    val c = KP.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = kpFont(20, FontWeight.Bold), color = c.text)
        Spacer(Modifier.weight(1f))
        Text(
            text = "See all",
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSeeAll)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            style = kpFont(15, FontWeight.SemiBold),
            color = c.accent,
        )
    }
}

/** Seven round day chips (S M T W T F S) for picking workout days. Always keeps at least one day. */
@Composable
fun WeekdayPicker(selection: Set<Int>, onSelectionChange: (Set<Int>) -> Unit, modifier: Modifier = Modifier) {
    val c = KP.colors
    val haptics = LocalHapticFeedback.current
    val calendar = KPCalendar.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (weekday in UserProfile.orderedWeekdays(calendar)) {
            val isOn = weekday in selection
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 44.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (isOn) c.accent else c.surface, CircleShape)
                        .border(1.dp, if (isOn) Color.Transparent else c.divider, CircleShape)
                        .clickable {
                            val next = if (isOn) {
                                if (selection.size > 1) selection - weekday else selection
                            } else {
                                selection + weekday
                            }
                            if (next != selection) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectionChange(next)
                            }
                        }
                        .semantics {
                            contentDescription = calendar.weekdayName(weekday)
                            selected = isOn
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = calendar.veryShortWeekdaySymbol(weekday),
                        style = kpFont(15, FontWeight.SemiBold),
                        color = if (isOn) c.onAccent else c.text,
                    )
                }
            }
        }
    }
}

/** A large card for a popular challenge: length, title, summary, level and progress. */
@Composable
fun ChallengeCard(plan: Plan, modifier: Modifier = Modifier, isAvailable: Boolean = true, onClick: () -> Unit) {
    val c = KP.colors
    val activityStore = LocalActivityStore.current
    val done = activityStore.completedDays(plan.id).size
    val joined = activityStore.isEnrolled(plan.id)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isAvailable) 1f else 0.6f)
            .clip(RoundedCornerShape(KP.cardRadius))
            .clickable(onClick = onClick)
            .kpOutlined(stroke = if (plan.isPopular) c.accent.copy(alpha = 0.3f) else c.divider)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${plan.days.size} DAYS",
                style = kpFont(12, FontWeight.ExtraBold, tracking = 2f),
                color = c.accent,
            )
            if (plan.isPopular) {
                KPLabel(
                    text = "POPULAR",
                    systemImage = "flame.fill",
                    style = kpFont(11, FontWeight.ExtraBold),
                    color = c.text,
                    spacing = 4.dp,
                    modifier = Modifier
                        .background(c.accentSoft, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (joined) {
                Icon(
                    imageVector = sfIcon("checkmark.circle.fill"),
                    contentDescription = "Joined",
                    tint = c.accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(text = plan.title, style = kpFont(20, FontWeight.ExtraBold), color = c.text)
        Text(text = plan.summary, style = kpFont(15), color = c.mutedText)

        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val level = plan.level
            if (level != null) {
                KPTag(text = level.title)
            }
            val equipment = plan.category.equipment
            if (equipment != null) {
                KPTag(
                    text = if (isAvailable) "Best with ${equipment.title.lowercase()}" else "Needs ${equipment.title.lowercase()}",
                    modifier = Modifier.weight(1f, fill = false),
                )
            } else {
                KPTag(text = "Mat only")
            }
        }

        if (done > 0) {
            KPLinearProgress(
                done = done,
                total = plan.days.size,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = "$done of ${plan.days.size} days done" },
            )
        }
    }
}

// MainTabView.swift ------------------------------------------------------------------------------

enum class WorkoutRowStatus { OPEN, NEXT, DONE, LOCKED }

/** A workout row with a figure thumbnail, used on Today and in plans. */
@Composable
fun WorkoutRow(
    workout: Workout,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    status: WorkoutRowStatus = WorkoutRowStatus.OPEN,
    onClick: (() -> Unit)? = null,
) {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val subscriptions = LocalSubscriptionStore.current
    val timeline = WorkoutTimeline(workout, profileStore.profile.equipment)
    val needsPremium = !subscriptions.canAccess(workout) && status != WorkoutRowStatus.DONE
    val statusImage = when (status) {
        WorkoutRowStatus.OPEN -> "chevron.right"
        // play.circle.fill isn't mapped; play.fill is the closest.
        WorkoutRowStatus.NEXT -> "play.circle.fill"
        WorkoutRowStatus.DONE -> "checkmark.circle.fill"
        WorkoutRowStatus.LOCKED -> "lock.fill"
    }

    var rowModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        rowModifier = rowModifier
            .clip(RoundedCornerShape(KP.cornerRadius))
            .clickable(onClick = onClick)
    }

    Row(
        modifier = rowModifier.kpCard(padding = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (timeline.exercises.isNotEmpty()) {
            WorkoutThumbnail(
                timeline = timeline,
                modifier = Modifier
                    .width(88.dp)
                    .alpha(if (status == WorkoutRowStatus.LOCKED) 0.4f else 1f),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (eyebrow != null) {
                KPEyebrow(text = eyebrow)
            }
            Text(
                text = workout.title,
                style = kpFont(17, FontWeight.SemiBold),
                color = if (status == WorkoutRowStatus.LOCKED) c.mutedText else c.text,
            )
            Text(
                text = "${timeline.estimatedMinutes} min · ${workout.focus.joinToString(", ") { it.title }}",
                style = kpFont(15),
                color = c.mutedText,
            )
            if (needsPremium) {
                KPLabel(
                    text = "Premium",
                    systemImage = "lock.fill",
                    style = kpFont(12, FontWeight.SemiBold),
                    color = c.accent,
                    spacing = 4.dp,
                )
            }
        }
        Icon(
            imageVector = sfIcon(statusImage),
            contentDescription = null,
            tint = if (status == WorkoutRowStatus.OPEN || status == WorkoutRowStatus.LOCKED) c.mutedText else c.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** A big number with a label. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val c = KP.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .kpCard(padding = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = value, style = kpFont(22, FontWeight.Bold, monospacedDigits = true), color = c.accent)
        Text(text = label, style = kpFont(12), color = c.mutedText)
    }
}

// WorkoutPreviewView.swift -----------------------------------------------------------------------

@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
    val c = KP.colors
    Text(
        text = text,
        modifier = modifier
            .background(c.accentSoft, RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = kpFont(12, FontWeight.Medium),
        color = c.accent,
    )
}
