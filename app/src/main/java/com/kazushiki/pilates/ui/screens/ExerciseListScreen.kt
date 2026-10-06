package com.kazushiki.pilates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.model.WorkoutLibrary
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.Route
import com.kazushiki.pilates.ui.components.ExerciseRow
import com.kazushiki.pilates.ui.components.KPCollapsible
import com.kazushiki.pilates.ui.components.KPTopBar
import com.kazushiki.pilates.ui.components.WorkoutRow
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP

/** The Library tab: exercise demos and workouts in dropdown groups,
 *  filtered to the user's equipment unless they choose Everything. */
@Composable
fun ExerciseListScreen() {
    val colors = KP.colors
    var showWorkouts by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var filterMenuOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        IOSLargeTitle(title = "Library") {
            Box {
                IconButton(
                    onClick = { filterMenuOpen = true },
                    modifier = Modifier.semantics { contentDescription = "Filter" },
                ) {
                    Icon(
                        screenIcon(if (showAll) "line.3.horizontal.decrease.circle" else "line.3.horizontal.decrease.circle.fill"),
                        contentDescription = null,
                        tint = colors.accent,
                    )
                }
                DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                    listOf(false to "My equipment", true to "Everything").forEach { (value, title) ->
                        DropdownMenuItem(
                            text = { Text(title, color = colors.text) },
                            onClick = {
                                showAll = value
                                filterMenuOpen = false
                            },
                            leadingIcon = {
                                if (showAll == value) {
                                    Icon(sfIcon("checkmark"), contentDescription = null, tint = colors.accent)
                                } else {
                                    Spacer(Modifier.size(24.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IOSSegmentedPicker(
                options = listOf("Exercises", "Workouts"),
                selectedIndex = if (showWorkouts) 1 else 0,
                onSelect = { showWorkouts = it == 1 },
            )
            Text(
                if (showAll) "Showing everything" else "Showing what fits your equipment",
                fontSize = 12.sp,
                color = colors.mutedText,
            )
            if (showWorkouts) WorkoutGroups(showAll = showAll) else ExerciseGroups(showAll = showAll)
        }
    }
}

/** Exercises grouped by equipment, one dropdown per group. */
@Composable
fun ExerciseGroups(showAll: Boolean) {
    val profileStore = LocalProfileStore.current
    val router = LocalRouter.current
    val owned = profileStore.profile.equipment + Equipment.MAT
    val list = if (showAll) ExerciseLibrary.all else ExerciseLibrary.all.filter { owned.containsAll(it.equipment) }
    Column(Modifier.fillMaxWidth()) {
        Equipment.entries.forEach { item ->
            val group = list.filter { it.equipment.firstOrNull() == item }
            if (group.isNotEmpty()) {
                KPCollapsible(
                    title = if (item == Equipment.MAT) "Mat" else item.title,
                    count = group.size,
                ) {
                    group.forEach { exercise ->
                        ExerciseRow(exercise = exercise, onClick = { router.push(Route.ExerciseDetail(exercise)) })
                    }
                }
            }
        }
    }
}

/** Workouts grouped by level, one dropdown per level. The user's level opens first. */
@Composable
fun WorkoutGroups(showAll: Boolean) {
    val profileStore = LocalProfileStore.current
    val router = LocalRouter.current
    val owned = profileStore.profile.equipment + Equipment.MAT
    val list = if (showAll) WorkoutLibrary.all else WorkoutLibrary.all.filter { it.isAvailable(owned) }
    Column(Modifier.fillMaxWidth()) {
        ExerciseLevel.entries.forEach { level ->
            val group = list.filter { it.level == level }
            if (group.isNotEmpty()) {
                val isMine = level == profileStore.profile.level
                KPCollapsible(
                    title = level.title,
                    subtitle = if (isMine) "Your level" else null,
                    count = group.size,
                    initiallyExpanded = isMine,
                ) {
                    group.forEach { workout ->
                        WorkoutRow(workout = workout, onClick = { router.push(Route.WorkoutPreview(workout)) })
                    }
                }
            }
        }
    }
}

/** Every workout, pushed from "See all" on the home screen. */
@Composable
fun AllWorkoutsScreen() {
    val colors = KP.colors
    val router = LocalRouter.current
    var showAll by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        KPTopBar(title = "Workouts", onBack = { router.pop() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IOSSegmentedPicker(
                options = listOf("My equipment", "Everything"),
                selectedIndex = if (showAll) 1 else 0,
                onSelect = { showAll = it == 1 },
            )
            WorkoutGroups(showAll = showAll)
        }
    }
}
