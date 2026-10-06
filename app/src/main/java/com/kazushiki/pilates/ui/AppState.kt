package com.kazushiki.pilates.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import com.kazushiki.pilates.data.ActivityStore
import com.kazushiki.pilates.data.ProfileStore
import com.kazushiki.pilates.exercises.Exercise
import com.kazushiki.pilates.model.Plan
import com.kazushiki.pilates.model.Workout
import com.kazushiki.pilates.services.SubscriptionStore

enum class AppTab(val title: String, val systemImage: String) {
    HOME("Home", "house.fill"),
    CHALLENGES("Challenges", "flame.fill"),
    LIBRARY("Library", "figure.pilates"),
    PROGRESS("Progress", "chart.bar.fill"),
    PROFILE("Profile", "person.crop.circle"),
}

/** Screens pushed on top of a tab's root screen (NavigationLink destinations on iPhone). */
sealed interface Route {
    data class WorkoutPreview(val workout: Workout, val planID: String? = null, val dayNumber: Int? = null) : Route
    data class PlanDetail(val plan: Plan) : Route
    data object QuickWorkout : Route
    data object ChallengeBrowse : Route
    data object AllWorkouts : Route
    data class ExerciseDetail(val exercise: Exercise, val variationID: String? = null) : Route
}

/** The selected tab and each tab's stack of pushed screens. */
class AppRouter {
    var tab: AppTab by mutableStateOf(AppTab.HOME)

    private val stacks: Map<AppTab, SnapshotStateList<Route>> = AppTab.entries.associateWith { mutableStateListOf() }

    fun stack(tab: AppTab): SnapshotStateList<Route> = stacks.getValue(tab)

    /** Pushes a screen on the current tab. */
    fun push(route: Route) {
        stack(tab).add(route)
    }

    fun pop() {
        val current = stack(tab)
        if (current.isNotEmpty()) current.removeAt(current.lastIndex)
    }

    /** Tapping the selected tab again pops back to its root, like iPhone. */
    fun select(newTab: AppTab) {
        if (newTab == tab) stack(newTab).clear()
        tab = newTab
    }

    /** Switches to the Challenges tab, optionally straight to one of its screens. */
    fun openChallenges(route: Route? = null) {
        val target = stack(AppTab.CHALLENGES)
        target.clear()
        if (route != null) target.add(route)
        tab = AppTab.CHALLENGES
    }
}

val LocalProfileStore = staticCompositionLocalOf<ProfileStore> { error("No ProfileStore") }
val LocalActivityStore = staticCompositionLocalOf<ActivityStore> { error("No ActivityStore") }
val LocalSubscriptionStore = staticCompositionLocalOf<SubscriptionStore> { error("No SubscriptionStore") }
val LocalRouter = staticCompositionLocalOf<AppRouter> { error("No AppRouter") }
