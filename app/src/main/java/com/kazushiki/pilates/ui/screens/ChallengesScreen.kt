package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.model.Plan
import com.kazushiki.pilates.model.PlanDay
import com.kazushiki.pilates.model.PlanLibrary
import com.kazushiki.pilates.model.ProgramCategory
import com.kazushiki.pilates.model.ProgramFocus
import com.kazushiki.pilates.model.ProgramLibrary
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.Route
import com.kazushiki.pilates.ui.components.ChallengeCard
import com.kazushiki.pilates.ui.components.KPCollapsible
import com.kazushiki.pilates.ui.components.KPDestructive
import com.kazushiki.pilates.ui.components.KPLabel
import com.kazushiki.pilates.ui.components.KPLinearProgress
import com.kazushiki.pilates.ui.components.KPModeCard
import com.kazushiki.pilates.ui.components.KPPageHeader
import com.kazushiki.pilates.ui.components.KPPrimaryButton
import com.kazushiki.pilates.ui.components.KPSectionLabel
import com.kazushiki.pilates.ui.components.KPTopBar
import com.kazushiki.pilates.ui.components.ProgramRow
import com.kazushiki.pilates.ui.components.WorkoutRow
import com.kazushiki.pilates.ui.components.WorkoutRowStatus
import com.kazushiki.pilates.ui.components.kpFont
import com.kazushiki.pilates.ui.components.kpStatusBarPadding
import com.kazushiki.pilates.ui.theme.KP

/** The Challenges tab: build a quick workout or join a challenge, then what she's doing now. */
@Composable
fun ChallengesScreen() {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val activityStore = LocalActivityStore.current
    val router = LocalRouter.current
    val active = activityStore.activePrograms(profileStore.profile)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .kpStatusBarPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KPPageHeader(
            eyebrow = "Challenges",
            title = "Choose how you want to train today.",
            subtitle = "Build a one-off session or join a challenge and see it through.",
            modifier = Modifier.padding(bottom = 10.dp),
        )

        KPSectionLabel(text = "Training modes")

        KPModeCard(
            systemImage = "bolt.fill",
            eyebrow = "Quick workout",
            title = "Build a workout",
            detail = "Choose your equipment, focus and time.",
            action = "Build session",
            onClick = { router.push(Route.QuickWorkout) },
        )

        KPModeCard(
            systemImage = "flame.fill",
            eyebrow = "Challenges",
            title = "Join a challenge",
            detail = "Popular 14 to 60-day challenges, plus multi-week programs by body area or equipment.",
            action = "View challenges",
            onClick = { router.push(Route.ChallengeBrowse) },
        )

        if (active.isNotEmpty()) {
            KPSectionLabel(text = "In progress", modifier = Modifier.padding(top = 14.dp))
            for (plan in active) {
                ProgramRow(plan = plan, onClick = { router.push(Route.PlanDetail(plan)) })
            }
        }
    }
}

/** Everything she can join in one place: popular challenges, quick starts, then programs. */
@Composable
fun ChallengeBrowseScreen() {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val router = LocalRouter.current
    val profile = profileStore.profile
    var byBodyArea by rememberSaveable { mutableStateOf(true) }

    // Body areas that match the user's goals come first.
    val suggested = ProgramFocus.suggested(profile.goals)
    val orderedFocuses = suggested + ProgramFocus.entries.filter { it !in suggested }

    // Mat and mixed programs first, then equipment the user owns, then the rest.
    val owned = profile.equipment
    val equipmentCategories = ProgramCategory.entries.filter { it.equipment != null }
    val mine = equipmentCategories.filter { cat -> cat.equipment?.let { it in owned } == true }
    val others = equipmentCategories.filter { cat -> cat.equipment?.let { it in owned } != true }
    val orderedCategories = listOf(ProgramCategory.MAT, ProgramCategory.MIXED) + mine + others

    fun canDo(plan: Plan): Boolean {
        if (plan.category != ProgramCategory.REFORMER) return true
        return Equipment.REFORMER in profile.equipment
    }

    // Popular challenges, with ones that need equipment she doesn't have last.
    val allPopular = PlanLibrary.popularChallenges
    val popular = allPopular.filter { canDo(it) } + allPopular.filter { !canDo(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        KPTopBar(onBack = { router.pop() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            KPPageHeader(
                eyebrow = "Challenges",
                title = "Pick your challenge.",
                subtitle = "Join one and it shows up on Home with today's workout. Leave any time.",
                modifier = Modifier.padding(bottom = 22.dp),
            )

            KPSectionLabel(text = "Popular", modifier = Modifier.padding(bottom = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (plan in popular) {
                    ChallengeCard(
                        plan = plan,
                        isAvailable = canDo(plan),
                        onClick = { router.push(Route.PlanDetail(plan)) },
                    )
                }
            }

            KPSectionLabel(text = "Quick starts", modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (plan in PlanLibrary.otherChallenges) {
                    ProgramRow(plan = plan, onClick = { router.push(Route.PlanDetail(plan)) })
                }
            }

            Column(
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                KPSectionLabel(text = "Programs")
                Text(
                    text = "Multi-week plans with 2 to 5 sessions a week, at your own pace.",
                    style = kpFont(15),
                    color = c.mutedText,
                )
            }

            SegmentedControl(
                options = listOf("Body area", "Equipment"),
                selectedIndex = if (byBodyArea) 0 else 1,
                onSelect = { byBodyArea = it == 0 },
                modifier = Modifier.padding(bottom = 4.dp),
            )

            if (byBodyArea) {
                for (focus in orderedFocuses) {
                    key("focus-${focus.name}") {
                        val programs = ProgramLibrary.programs(focus)
                        KPCollapsible(
                            title = focus.title,
                            subtitle = focus.subtitle,
                            count = programs.size,
                        ) {
                            for (plan in programs) {
                                ProgramRow(plan = plan, needs = null, onClick = { router.push(Route.PlanDetail(plan)) })
                            }
                        }
                    }
                }
            } else {
                for (category in orderedCategories) {
                    key("category-${category.name}") {
                        val missing: Equipment? = category.equipment?.let { if (it in profile.equipment) null else it }
                        val programs = ProgramLibrary.programs(category)
                        KPCollapsible(
                            title = category.title,
                            subtitle = categorySubtitle(category, missing != null),
                            count = programs.size,
                            dimmed = missing != null,
                        ) {
                            for (plan in programs) {
                                ProgramRow(plan = plan, needs = missing, onClick = { router.push(Route.PlanDetail(plan)) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun categorySubtitle(category: ProgramCategory, missing: Boolean): String = when (category) {
    ProgramCategory.MAT -> "No equipment needed"
    ProgramCategory.MIXED -> "Uses whatever you have"
    else -> if (missing) "Add it in Profile to use" else "You have this"
}

/** iPhone's segmented `Picker`: a grey track with the selected segment raised in white. */
@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KP.colors
    val outer = RoundedCornerShape(9.dp)
    val inner = RoundedCornerShape(7.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(c.divider.copy(alpha = 0.7f), outer)
            .padding(2.dp),
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val segment = if (isSelected) {
                Modifier
                    .shadow(elevation = 1.dp, shape = inner)
                    .background(c.surface, inner)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(segment)
                    .clip(inner)
                    .clickable { onSelect(index) }
                    .semantics { selected = isSelected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = kpFont(13, if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = c.text,
                )
            }
        }
    }
}

@Composable
fun PlanDetailScreen(plan: Plan) {
    val c = KP.colors
    val activityStore = LocalActivityStore.current
    val router = LocalRouter.current
    var confirmingLeave by remember { mutableStateOf(false) }

    val done = activityStore.completedDays(plan.id)
    val next = activityStore.nextDay(plan)
    val weeks = plan.days.map { it.week }.toSet().sorted()
    val enrolled = activityStore.isEnrolled(plan.id)
    val noun = if (plan.isChallenge) "challenge" else "program"
    val status = if (next == null) "Completed" else "You're in this $noun"
    val joinTitle = if (done.isEmpty()) "Start $noun" else "Rejoin $noun"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        KPTopBar(title = plan.title, onBack = { router.pop() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = plan.summary, style = kpFont(17), color = c.mutedText)
                KPLinearProgress(done = done.size, total = plan.days.size)
            }

            if (enrolled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.accentSoft, RoundedCornerShape(KP.compactCornerRadius))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KPLabel(
                        text = status,
                        systemImage = "checkmark.circle.fill",
                        style = kpFont(15, FontWeight.SemiBold),
                        color = c.accent,
                        modifier = Modifier.weight(1f),
                    )
                    // .bordered, .controlSize(.small), role: .destructive
                    Text(
                        text = "Leave",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(KPDestructive.copy(alpha = 0.15f))
                            .clickable { confirmingLeave = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        style = kpFont(15, FontWeight.Medium),
                        color = KPDestructive,
                    )
                }
            } else {
                KPPrimaryButton(text = joinTitle, onClick = { activityStore.enroll(plan.id) })
            }

            if (weeks.size > 1) {
                // One dropdown per week; the week with the next session starts open.
                Column {
                    for (week in weeks) {
                        key(week) {
                            val days = plan.days.filter { it.week == week }
                            val finished = days.count { it.number in done }
                            KPCollapsible(
                                title = "Week $week",
                                subtitle = if (finished == days.size) "Complete" else "$finished of ${days.size} done",
                                initiallyExpanded = week == (next?.week ?: 1),
                            ) {
                                for (day in days) {
                                    PlanDayRow(plan, day, done = day.number in done, isNext = day.number == next?.number)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (day in plan.days) {
                        PlanDayRow(plan, day, done = day.number in done, isNext = day.number == next?.number)
                    }
                }
            }
        }
    }

    if (confirmingLeave) {
        AlertDialog(
            onDismissRequest = { confirmingLeave = false },
            title = { Text("Leave ${plan.title}?") },
            text = {
                Text("It comes off your home screen. Workouts you've done stay in your history, and you can rejoin any time.")
            },
            confirmButton = {
                TextButton(onClick = {
                    activityStore.leave(plan.id)
                    confirmingLeave = false
                }) {
                    Text("Leave program", color = KPDestructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingLeave = false }) {
                    Text("Cancel", color = c.accent)
                }
            },
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.mutedText,
        )
    }
}

@Composable
private fun PlanDayRow(plan: Plan, day: PlanDay, done: Boolean, isNext: Boolean) {
    val workout = day.workout ?: return
    val activityStore = LocalActivityStore.current
    val router = LocalRouter.current
    val unlocked = activityStore.isUnlocked(day, plan)
    val status = when {
        done -> WorkoutRowStatus.DONE
        !unlocked -> WorkoutRowStatus.LOCKED
        isNext -> WorkoutRowStatus.NEXT
        else -> WorkoutRowStatus.OPEN
    }
    if (unlocked) {
        WorkoutRow(
            workout = workout,
            eyebrow = "Day ${day.number}",
            status = status,
            onClick = { router.push(Route.WorkoutPreview(workout, plan.id, day.number)) },
        )
    } else {
        WorkoutRow(
            workout = workout,
            eyebrow = "Day ${day.number}",
            status = status,
            modifier = Modifier.semantics { stateDescription = "Finish day ${day.number - 1} to unlock" },
        )
    }
}
