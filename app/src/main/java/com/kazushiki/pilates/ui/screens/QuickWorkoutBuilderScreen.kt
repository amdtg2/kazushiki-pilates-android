package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.model.ProgramFocus
import com.kazushiki.pilates.model.QuickWorkoutBuilder
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.Route
import com.kazushiki.pilates.ui.components.KPChip
import com.kazushiki.pilates.ui.components.KPFilledButton
import com.kazushiki.pilates.ui.components.KPPageHeader
import com.kazushiki.pilates.ui.components.KPSettingCard
import com.kazushiki.pilates.ui.components.KPTopBar
import com.kazushiki.pilates.ui.theme.KP

private val EquipmentSaver = Saver<MutableState<Set<Equipment>>, ArrayList<String>>(
    save = { state -> ArrayList(state.value.map { it.name }) },
    restore = { names -> mutableStateOf(names.map { Equipment.valueOf(it) }.toSet()) },
)

private val FocusSaver = Saver<MutableState<List<ProgramFocus>>, ArrayList<String>>(
    save = { state -> ArrayList(state.value.map { it.name }) },
    restore = { names -> mutableStateOf(names.map { ProgramFocus.valueOf(it) }) },
)

/** "Build a workout": pick equipment, what to work and how long, then get a session. */
@Composable
fun QuickWorkoutBuilderScreen() {
    val c = KP.colors
    val profileStore = LocalProfileStore.current
    val router = LocalRouter.current
    val haptics = LocalHapticFeedback.current
    val profile = profileStore.profile

    // Defaults load once from her profile (iPhone's loadDefaults on first appear).
    val equipmentState = rememberSaveable(saver = EquipmentSaver) {
        mutableStateOf(profile.equipment + Equipment.MAT)
    }
    val focusState = rememberSaveable(saver = FocusSaver) {
        val suggested = ProgramFocus.suggested(profile.goals).take(1)
        mutableStateOf(suggested.ifEmpty { listOf(ProgramFocus.FULL_BODY) })
    }
    var equipment by equipmentState
    var focuses by focusState
    var minutes by rememberSaveable { mutableStateOf(QuickWorkoutBuilder.defaultMinutes(profile)) }

    fun toggleEquipment(item: Equipment) {
        if (item == Equipment.MAT) return
        equipment = if (item in equipment) equipment - item else equipment + item
    }

    fun toggleFocus(focus: ProgramFocus) {
        val current = focuses
        val updated: List<ProgramFocus> = if (focus in current) {
            current - focus
        } else if (focus == ProgramFocus.FULL_BODY) {
            // Full body covers everything, so it stands alone.
            listOf(ProgramFocus.FULL_BODY)
        } else {
            var list = current.filter { it != ProgramFocus.FULL_BODY }
            if (list.size >= QuickWorkoutBuilder.maxFocuses) list = list.drop(1)
            list + focus
        }
        if (updated != current) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        focuses = updated
    }

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
                .padding(16.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KPPageHeader(
                eyebrow = "Quick workout",
                title = "Build a session around what you need today.",
                modifier = Modifier.padding(bottom = 6.dp),
            )

            KPSettingCard(
                title = "Equipment",
                footnote = "Every session works with just a mat. Add what you have nearby.",
            ) {
                TwoColumnGrid(items = Equipment.entries) { item, modifier ->
                    KPChip(
                        title = if (item == Equipment.MAT) "Mat" else item.title,
                        isSelected = item in equipment,
                        systemImage = equipmentIcon(item),
                        isLocked = item == Equipment.MAT,
                        modifier = modifier,
                        onClick = { toggleEquipment(item) },
                    )
                }
            }

            KPSettingCard(
                title = "Focus",
                footnote = "Choose up to ${QuickWorkoutBuilder.maxFocuses} areas for this session.",
            ) {
                TwoColumnGrid(items = ProgramFocus.entries) { focus, modifier ->
                    KPChip(
                        title = if (focus == ProgramFocus.BACK) "Back" else focus.title,
                        isSelected = focus in focuses,
                        systemImage = focus.systemImage,
                        modifier = modifier,
                        onClick = { toggleFocus(focus) },
                    )
                }
            }

            KPSettingCard(title = "Duration", footnote = "minutes") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (option in QuickWorkoutBuilder.minuteOptions) {
                        KPChip(
                            title = "$option",
                            isSelected = minutes == option,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "$option minutes" },
                            onClick = { minutes = option },
                        )
                    }
                }
            }
        }

        // Pinned bottom button (iPhone's safeAreaInset on a bar background).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            KPFilledButton(
                text = "Build my workout",
                systemImage = "sparkles",
                enabled = focuses.isNotEmpty(),
                fontWeight = FontWeight.ExtraBold,
                extraVerticalPadding = 6.dp,
                onClick = {
                    val workout = QuickWorkoutBuilder.build(
                        equipment = equipment,
                        focuses = focuses,
                        minutes = minutes,
                        level = profileStore.profile.level,
                    )
                    router.push(Route.WorkoutPreview(workout))
                },
            )
        }
    }
}

/** A two-column grid of equal-width cells with 10dp gaps (iPhone's two flexible GridItems). */
@Composable
private fun <T> TwoColumnGrid(items: List<T>, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (pair in items.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cell(pair[0], Modifier.weight(1f))
                if (pair.size > 1) {
                    cell(pair[1], Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f).height(0.dp))
                }
            }
        }
    }
}

private fun equipmentIcon(item: Equipment): String = when (item) {
    Equipment.MAT -> "rectangle.portrait.fill"
    Equipment.BAND -> "line.diagonal"
    Equipment.RING -> "circle"
    Equipment.BALL -> "circle.fill"
    Equipment.WEIGHTS -> "dumbbell.fill"
    Equipment.WALL -> "square.split.bottomrightquarter"
    Equipment.REFORMER -> "bed.double.fill"
}
