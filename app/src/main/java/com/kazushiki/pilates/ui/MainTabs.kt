package com.kazushiki.pilates.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.kazushiki.pilates.ui.screens.AllWorkoutsScreen
import com.kazushiki.pilates.ui.screens.ChallengeBrowseScreen
import com.kazushiki.pilates.ui.screens.ChallengesScreen
import com.kazushiki.pilates.ui.screens.ExerciseDetailScreen
import com.kazushiki.pilates.ui.screens.ExerciseListScreen
import com.kazushiki.pilates.ui.screens.HomeScreen
import com.kazushiki.pilates.ui.screens.PlanDetailScreen
import com.kazushiki.pilates.ui.screens.ProfileScreen
import com.kazushiki.pilates.ui.screens.ProgressScreen
import com.kazushiki.pilates.ui.screens.QuickWorkoutBuilderScreen
import com.kazushiki.pilates.ui.screens.WorkoutPreviewScreen
import com.kazushiki.pilates.ui.theme.KP

/** The five tabs, each with its own stack of pushed screens. */
@Composable
fun MainTabs() {
    val router = LocalRouter.current
    val colors = KP.colors
    val stateHolder = rememberSaveableStateHolder()
    val stack = router.stack(router.tab)

    BackHandler(enabled = stack.isNotEmpty()) { router.pop() }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            NavigationBar(containerColor = colors.surface) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = router.tab == tab,
                        onClick = { router.select(tab) },
                        icon = { Icon(sfIcon(tab.systemImage), contentDescription = null) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.accent,
                            selectedTextColor = colors.accent,
                            indicatorColor = colors.accentSoft,
                            unselectedIconColor = colors.mutedText,
                            unselectedTextColor = colors.mutedText,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            val top = stack.lastOrNull()
            // Keep each screen's scroll position while others are pushed on top of it.
            val screenKey = "${router.tab.name}-${stack.size}-${top?.hashCode() ?: 0}"
            key(screenKey) {
                stateHolder.SaveableStateProvider(screenKey) {
                    if (top == null) TabRoot(router.tab) else RouteScreen(top)
                }
            }
        }
    }
}

@Composable
private fun TabRoot(tab: AppTab) {
    when (tab) {
        AppTab.HOME -> HomeScreen()
        AppTab.CHALLENGES -> ChallengesScreen()
        AppTab.LIBRARY -> ExerciseListScreen()
        AppTab.PROGRESS -> ProgressScreen()
        AppTab.PROFILE -> ProfileScreen()
    }
}

@Composable
private fun RouteScreen(route: Route) {
    when (route) {
        is Route.WorkoutPreview -> WorkoutPreviewScreen(route.workout, route.planID, route.dayNumber)
        is Route.PlanDetail -> PlanDetailScreen(route.plan)
        Route.QuickWorkout -> QuickWorkoutBuilderScreen()
        Route.ChallengeBrowse -> ChallengeBrowseScreen()
        Route.AllWorkouts -> AllWorkoutsScreen()
        is Route.ExerciseDetail -> ExerciseDetailScreen(route.exercise, route.variationID)
    }
}
