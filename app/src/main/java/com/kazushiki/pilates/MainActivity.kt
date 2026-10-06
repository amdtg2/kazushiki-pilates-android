package com.kazushiki.pilates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kazushiki.pilates.ui.AppRouter
import com.kazushiki.pilates.ui.LocalActivityStore
import com.kazushiki.pilates.ui.LocalProfileStore
import com.kazushiki.pilates.ui.LocalRouter
import com.kazushiki.pilates.ui.LocalSubscriptionStore
import com.kazushiki.pilates.ui.MainTabs
import com.kazushiki.pilates.ui.components.FullScreenDialog
import com.kazushiki.pilates.ui.screens.OnboardingScreen
import com.kazushiki.pilates.ui.screens.PaywallScreen
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.KazushikiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KazushikiApp
        setContent {
            KazushikiTheme {
                val router = remember { AppRouter() }
                CompositionLocalProvider(
                    LocalProfileStore provides app.profileStore,
                    LocalActivityStore provides app.activityStore,
                    LocalSubscriptionStore provides app.subscriptionStore,
                    LocalRouter provides router,
                ) {
                    LaunchedEffect(Unit) { app.subscriptionStore.start(this@MainActivity) }
                    AppRoot()
                }
            }
        }
    }
}

/** Root gate: onboarding on first launch, then the main tabs. */
@Composable
private fun AppRoot() {
    val profileStore = LocalProfileStore.current
    val subscriptions = LocalSubscriptionStore.current
    Box(Modifier.fillMaxSize().background(KP.colors.background)) {
        AnimatedContent(
            targetState = profileStore.hasCompletedOnboarding,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
            label = "onboarding",
        ) { done ->
            if (done) MainTabs() else OnboardingScreen()
        }
        // The paywall appears once, right after onboarding, for anyone not subscribed.
        if (profileStore.hasCompletedOnboarding && !subscriptions.isPremium && !subscriptions.hasSeenPaywall) {
            FullScreenDialog(onDismiss = { subscriptions.markPaywallSeen() }) {
                PaywallScreen(onClose = { subscriptions.markPaywallSeen() })
            }
        }
    }
}
