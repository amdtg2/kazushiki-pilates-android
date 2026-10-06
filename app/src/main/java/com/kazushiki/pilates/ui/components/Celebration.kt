package com.kazushiki.pilates.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kazushiki.pilates.model.Achievement
import com.kazushiki.pilates.model.CelebrationSummary
import com.kazushiki.pilates.model.WeeklyStreak
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import kotlin.math.max
import kotlin.math.min

/** The feel-good part of the finish screen: message, stats, weekly goal and new badges. */
@Composable
fun CelebrationContent(summary: CelebrationSummary, modifier: Modifier = Modifier) {
    val c = KP.colors
    val haptics = LocalHapticFeedback.current
    // 0 → 1 with SwiftUI's spring(response: 0.5, dampingFraction: 0.6).
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 158f))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        val s = 0.6f + 0.4f * appear.value
                        scaleX = s
                        scaleY = s
                    }
                    .background(c.accentSoft, CircleShape),
            )
            Icon(
                imageVector = sfIcon(if (summary.newAchievements.isEmpty()) "checkmark" else "star.fill"),
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        val s = 0.3f + 0.7f * appear.value
                        scaleX = s
                        scaleY = s
                    },
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = summary.encouragement.title,
                style = kpFont(30, FontWeight.ExtraBold),
                color = c.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = summary.encouragement.message,
                style = kpFont(17),
                color = c.mutedText,
                textAlign = TextAlign.Center,
            )
            val progress = summary.planProgress
            if (progress != null) {
                KPTag(text = progress, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CelebrationStat("${summary.minutes}", if (summary.minutes == 1) "minute" else "minutes", Modifier.weight(1f))
            val exercises = summary.exercises
            if (exercises != null) {
                CelebrationStat("$exercises", "exercises", Modifier.weight(1f))
            }
            CelebrationStat("${summary.dayStreak}", "day streak", Modifier.weight(1f))
        }

        WeeklyGoalCard(weekly = summary.weekly)

        if (summary.newAchievements.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kpOutlined(stroke = c.prop.copy(alpha = 0.6f), strokeWidth = 1.5.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (summary.newAchievements.size == 1) "NEW ACHIEVEMENT" else "NEW ACHIEVEMENTS",
                    style = kpFont(12, FontWeight.ExtraBold, tracking = 2f),
                    color = c.accent,
                )
                for (achievement in summary.newAchievements) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) { },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AchievementBadge(achievement = achievement, isEarned = true, size = 48.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(text = achievement.title, style = kpFont(17, FontWeight.SemiBold), color = c.text)
                            Text(text = achievement.detail, style = kpFont(15), color = c.mutedText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = KP.colors
    Column(
        modifier = modifier
            .kpOutlined(radius = 16.dp)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = value, style = kpFont(22, FontWeight.ExtraBold, monospacedDigits = true), color = c.text)
        Text(text = label, style = kpFont(12), color = c.mutedText)
    }
}

/** Weekly goal progress: a dot per workout, the week streak and what's left to do. */
@Composable
fun WeeklyGoalCard(weekly: WeeklyStreak, modifier: Modifier = Modifier) {
    val c = KP.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .kpOutlined()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WEEKLY GOAL",
                style = kpFont(12, FontWeight.ExtraBold, tracking = 2f),
                color = c.mutedText,
            )
            Spacer(Modifier.weight(1f))
            WeekStreakLabel(weeks = weekly.weeks)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = "${min(weekly.thisWeek, weekly.target)} of ${weekly.target} workouts this week"
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(max(weekly.target, 1)) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(if (index < weekly.thisWeek) c.accent else c.divider, RoundedCornerShape(percent = 50)),
                )
            }
        }
        Text(
            text = weekly.status,
            style = kpFont(15, FontWeight.SemiBold),
            color = if (weekly.goalMetThisWeek) c.accent else c.text,
        )
    }
}

/** "3-week streak" with a gold medal, or a quiet hint when there's no streak yet. */
@Composable
fun WeekStreakLabel(weeks: Int, modifier: Modifier = Modifier) {
    val c = KP.colors
    val text = if (weeks == 1) "1-week streak" else "$weeks-week streak"
    KPLabel(
        text = text,
        systemImage = "medal.fill",
        style = kpFont(15, FontWeight.ExtraBold),
        color = if (weeks > 0) c.prop else c.mutedText,
        modifier = modifier,
    )
}

/** A round badge for an achievement; locked ones are greyed out. */
@Composable
fun AchievementBadge(achievement: Achievement, isEarned: Boolean, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val c = KP.colors
    val background = if (isEarned) {
        Modifier.background(Brush.linearGradient(listOf(c.prop, c.accent)), CircleShape)
    } else {
        Modifier
            .background(c.background, CircleShape)
            .border(1.dp, c.divider, CircleShape)
    }
    Box(
        modifier = modifier
            .size(size)
            .then(background)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = sfIcon(if (isEarned) achievement.systemImage else "lock.fill"),
            contentDescription = null,
            tint = if (isEarned) c.onAccent else c.mutedText,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}
