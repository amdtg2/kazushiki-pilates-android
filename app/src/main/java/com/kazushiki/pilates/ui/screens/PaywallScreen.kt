package com.kazushiki.pilates.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.figure.LoopingFigureView
import com.kazushiki.pilates.services.PricingText
import com.kazushiki.pilates.services.PurchaseOutcome
import com.kazushiki.pilates.services.SubscriptionConfig
import com.kazushiki.pilates.services.SubscriptionProduct
import com.kazushiki.pilates.services.SubscriptionStore
import com.kazushiki.pilates.ui.LocalSubscriptionStore
import com.kazushiki.pilates.ui.components.KPEyebrow
import com.kazushiki.pilates.ui.sfIcon
import com.kazushiki.pilates.ui.theme.KP
import com.kazushiki.pilates.ui.theme.kpCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Subscription offer: annual or monthly, with the free trial for first-time subscribers. */
@Composable
fun PaywallScreen(onClose: () -> Unit) {
    val subscriptions = LocalSubscriptionStore.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val c = KP.colors

    var selectedID by rememberSaveable { mutableStateOf(SubscriptionConfig.annualID) }
    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val products = subscriptions.products
    val selected: SubscriptionProduct? = products.firstOrNull { it.id == selectedID } ?: products.firstOrNull()
    val trialLabel: String? = if (subscriptions.isEligibleForTrial) selected?.freeTrialLabel else null

    LaunchedEffect(Unit) {
        if (subscriptions.products.isEmpty()) subscriptions.loadProducts()
    }

    // Close once the user becomes premium (like onChange(of: isPremium) on iPhone).
    var wasPremium by remember { mutableStateOf(subscriptions.isPremium) }
    val isPremium = subscriptions.isPremium
    LaunchedEffect(isPremium) {
        if (isPremium && !wasPremium) onClose()
        wasPremium = isPremium
    }

    fun buy() {
        val product = selected ?: return
        val activity = context.findActivity()
        if (activity == null) {
            message = "Google Play couldn't complete this purchase. Please try again."
            return
        }
        isWorking = true
        scope.launch {
            try {
                when (subscriptions.purchase(activity, product)) {
                    PurchaseOutcome.PURCHASED -> onClose()
                    PurchaseOutcome.PENDING -> {
                        message = "Your purchase is waiting for approval. You'll get access as soon as it's approved."
                    }
                    PurchaseOutcome.CANCELLED -> {}
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Google Play couldn't complete this purchase. Please try again."
            } finally {
                isWorking = false
            }
        }
    }

    fun restore() {
        isWorking = true
        scope.launch {
            try {
                subscriptions.restore()
                if (subscriptions.isPremium) {
                    onClose()
                } else {
                    message = "No active subscription was found for this Google account."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Restore didn't finish. Please try again."
            } finally {
                isWorking = false
            }
        }
    }

    fun open(url: String) {
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            // No browser available.
        }
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                LoopingFigureView(
                    exercise = ExerciseLibrary.rollUp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .background(c.stage, RoundedCornerShape(KP.cornerRadius)),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KPEyebrow(text = "Kazushiki Pilates Premium")
                    Text(
                        text = "Unlock your full plan",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.text,
                    )
                    Text(
                        text = "Every workout, plan and challenge, built around your goals and the equipment you have.",
                        fontSize = 17.sp,
                        color = c.mutedText,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Benefit("Your personal 4-week plan, day by day", sfIcon("calendar"))
                    Benefit("All workouts and challenges", sfIcon("figure.pilates"))
                    Benefit("Voice-guided sessions with rep counts", Icons.AutoMirrored.Filled.VolumeUp)
                    Benefit("Moves that adapt to your equipment", Icons.Filled.SwapHoriz)
                }

                // Plans
                if (products.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().kpCard(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (subscriptions.isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = c.mutedText, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            }
                        } else {
                            Text(
                                text = "Subscription options couldn't load. Check your connection and try again.",
                                fontSize = 17.sp,
                                color = c.mutedText,
                            )
                            Text(
                                text = "Try again",
                                fontSize = 17.sp,
                                color = c.accent,
                                modifier = Modifier.clickable { scope.launch { subscriptions.loadProducts() } },
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (product in products) {
                            PlanOption(
                                product = product,
                                isSelected = product.id == selected?.id,
                                monthly = subscriptions.monthly,
                                onSelect = { selectedID = product.id },
                            )
                        }
                    }
                }
            }

            // Footer
            HorizontalDivider(color = c.divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { buy() },
                    enabled = selected != null && !isWorking,
                    shape = RoundedCornerShape(KP.compactCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = c.onAccent,
                        disabledContainerColor = c.accent.copy(alpha = 0.4f),
                        disabledContentColor = c.onAccent.copy(alpha = 0.8f),
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            color = c.onAccent,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Text(
                            text = trialLabel?.let { "Start $it" } ?: "Subscribe",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (selected != null) {
                    Text(
                        text = finePrint(selected, trialLabel),
                        fontSize = 12.sp,
                        color = c.mutedText,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FooterLink("Restore purchases", enabled = !isWorking) { restore() }
                    FooterLink("Terms of Use", enabled = !isWorking) { open(SubscriptionConfig.termsURL) }
                    FooterLink("Privacy Policy", enabled = !isWorking) { open(SubscriptionConfig.privacyURL) }
                }

                if (SubscriptionStore.testingToolsEnabled) {
                    TextButton(onClick = { subscriptions.testUnlocked = true }) {
                        Text(
                            text = "Skip: unlock Premium for testing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.accent,
                        )
                    }
                }
            }
        }

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(c.surface)
                .clickable { onClose() }
                .semantics { contentDescription = "Close" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = c.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    val shown = message
    if (shown != null) {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Subscription") },
            text = { Text(shown) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("OK", color = c.accent) }
            },
        )
    }
}

@Composable
private fun Benefit(text: String, icon: ImageVector) {
    val c = KP.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
        Text(text = text, fontSize = 17.sp, color = c.text)
    }
}

@Composable
private fun PlanOption(
    product: SubscriptionProduct,
    isSelected: Boolean,
    monthly: SubscriptionProduct?,
    onSelect: () -> Unit,
) {
    val c = KP.colors
    val isAnnual = product.id == SubscriptionConfig.annualID
    val savings: Int? = if (isAnnual && monthly != null) {
        PricingText.annualSavingsPercent(product.priceMicros, monthly.priceMicros)
    } else {
        null
    }
    val shape = RoundedCornerShape(KP.compactCornerRadius)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) c.accentSoft else c.surface, shape)
            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) c.accent else c.divider, shape)
            .clickable { onSelect() }
            .semantics { selected = isSelected }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = sfIcon(if (isSelected) "checkmark.circle.fill" else "circle"),
            contentDescription = null,
            tint = if (isSelected) c.accent else c.divider,
            modifier = Modifier.size(26.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isAnnual) "Annual" else "Monthly",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.text,
                )
                if (savings != null) {
                    Text(
                        text = "Save $savings%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.onAccent,
                        modifier = Modifier
                            .background(c.accent, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(text = priceLine(product, isAnnual), fontSize = 15.sp, color = c.mutedText)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FooterLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = KP.colors
    Text(
        text = text,
        fontSize = 12.sp,
        color = if (enabled) c.accent else c.accent.copy(alpha = 0.4f),
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
    )
}

private fun priceLine(product: SubscriptionProduct, isAnnual: Boolean): String {
    val base = "${product.formattedPrice}/${product.periodWord}"
    if (!isAnnual) return base
    return base + " · " + PricingText.perMonth(product.priceMicros, product.currencyCode) + "/month"
}

private fun finePrint(product: SubscriptionProduct, trialLabel: String?): String {
    val price = "${product.formattedPrice}/${product.periodWord}"
    if (trialLabel != null) {
        val trial = trialLabel.replace(" free trial", "")
        return "Free for the $trial trial, then $price. Cancel anytime in Google Play at least 24 hours before the trial ends and you won't be charged."
    }
    return "$price, renews automatically. Cancel anytime in Google Play at least 24 hours before renewal."
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
