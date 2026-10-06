package com.kazushiki.pilates.services

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.kazushiki.pilates.BuildConfig
import com.kazushiki.pilates.data.KeyValueStore
import com.kazushiki.pilates.model.Workout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Product IDs and links for the Kazushiki Pilates subscription.
 *
 * The IDs must match the subscriptions created in Google Play Console:
 *  - `kazushiki.pilates.monthly`: $12.99/month base plan (auto-renewing, P1M)
 *  - `kazushiki.pilates.annual`:  $79.99/year base plan (auto-renewing, P1Y)
 * Each base plan needs a "7-day free trial" offer (one free pricing phase of P7D or P1W,
 * eligibility: new customers only). Play only returns that offer to users who can still get it.
 */
object SubscriptionConfig {
    const val monthlyID = "kazushiki.pilates.monthly"
    const val annualID = "kazushiki.pilates.annual"
    val productIDs = listOf(annualID, monthlyID)

    /** Workouts anyone can do without subscribing. */
    val freeWorkoutIDs: Set<String> = setOf("core-wake-up")

    /** Google Play's terms of service. */
    const val termsURL = "https://play.google.com/about/play-terms/"
    /** TODO: Replace with your own privacy policy page before publishing on Google Play. */
    const val privacyURL = "https://example.com/kazushiki-pilates/privacy"
}

/** A subscription as shown on the paywall (kept free of Play Billing objects so it can be tested). */
data class SubscriptionProduct(
    val id: String,
    val title: String,
    /** "$79.99" */
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    /** ISO-8601 renewal period, e.g. "P1M" or "P1Y". */
    val billingPeriod: String,
    /** ISO-8601 length of the free trial ("P7D", "P1W"), or null when no trial is offered. */
    val freeTrialPeriod: String?,
    /** The offer to buy (the free-trial offer when there is one, otherwise the base plan). */
    val offerToken: String,
) {
    /** "7-day free trial" when this product has a free-trial offer. */
    val freeTrialLabel: String?
        get() = freeTrialPeriod?.let { "${PricingText.trialLabel(it)} free trial" }

    val periodWord: String get() = PricingText.periodWord(billingPeriod)
}

enum class PurchaseOutcome { PURCHASED, CANCELLED, PENDING }

/** A purchase or restore that didn't finish; [message] is shown to the user. */
class PurchaseException(message: String) : Exception(message)

/** Loads subscription products, buys and restores them, and tracks whether the user is premium. */
class SubscriptionStore(context: Context, private val store: KeyValueStore) {
    private val contextRef: Context = context

    var products: List<SubscriptionProduct> by mutableStateOf(emptyList())
        private set
    var isPremium: Boolean by mutableStateOf(false)
        private set
    /** Whether the user can still get the free trial (first-time subscribers only). */
    var isEligibleForTrial: Boolean by mutableStateOf(true)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var loadFailed: Boolean by mutableStateOf(false)
        private set

    /** Set once the paywall has been shown after onboarding, so it doesn't reappear on every launch. */
    var hasSeenPaywall: Boolean by mutableStateOf(store.getBoolean(SEEN_KEY) ?: false)
        private set

    private var testUnlockedState: Boolean by mutableStateOf(testingToolsEnabled && (store.getBoolean(TEST_UNLOCK_KEY) ?: false))

    /** Premium unlocked for testing. Remembered between launches; ignored outside debug builds. */
    var testUnlocked: Boolean
        get() = testUnlockedState
        set(value) {
            testUnlockedState = value
            store.putBoolean(TEST_UNLOCK_KEY, value)
            updatePremium()
        }

    /** Whether Google Play reports an active subscription. */
    private var playPremium = false

    private val productDetails = mutableMapOf<String, ProductDetails>()
    private var pendingPurchase: CompletableDeferred<PurchaseOutcome>? = null
    private val connectionMutex = Mutex()

    // Created lazily so the store can be built in plain JVM unit tests.
    private val scope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        scope.launch { onPurchasesUpdated(result, purchases) }
    }

    private val billingClient: BillingClient? by lazy {
        try {
            val appContext = contextRef.applicationContext ?: contextRef
            BillingClient.newBuilder(appContext)
                .setListener(purchasesListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build()
        } catch (e: Exception) {
            null
        }
    }

    init {
        isPremium = testUnlockedState
    }

    val monthly: SubscriptionProduct? get() = products.firstOrNull { it.id == SubscriptionConfig.monthlyID }
    val annual: SubscriptionProduct? get() = products.firstOrNull { it.id == SubscriptionConfig.annualID }

    /** Shows the paywall again after "Clear and redo setup". */
    fun resetPaywallSeen() {
        hasSeenPaywall = false
        store.putBoolean(SEEN_KEY, false)
    }

    fun markPaywallSeen() {
        hasSeenPaywall = true
        store.putBoolean(SEEN_KEY, true)
    }

    fun canAccess(workout: Workout): Boolean =
        isPremium || workout.id in SubscriptionConfig.freeWorkoutIDs

    private fun updatePremium() {
        isPremium = playPremium || (testingToolsEnabled && testUnlockedState)
    }

    // Lifecycle

    /** Call once at launch: connects to Google Play, then loads products and status. Never throws. */
    suspend fun start(activity: Activity) {
        try {
            loadProducts()
            refreshEntitlements()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadFailed = true
            isLoading = false
        }
    }

    /** Connects to Google Play if needed. Returns false when Play isn't available (emulators, sideloads). */
    private suspend fun connect(): Boolean {
        val client = billingClient ?: return false
        return connectionMutex.withLock {
            if (client.isReady) return@withLock true
            try {
                suspendCancellableCoroutine<Boolean> { cont ->
                    try {
                        client.startConnection(object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                if (cont.isActive) cont.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                            }

                            override fun onBillingServiceDisconnected() {
                                if (cont.isActive) cont.resume(false)
                            }
                        })
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun loadProducts() {
        isLoading = true
        loadFailed = false
        try {
            val client = billingClient
            if (client == null || !connect()) {
                loadFailed = true
                return
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    SubscriptionConfig.productIDs.map { id ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(id)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    }
                )
                .build()
            val result = client.queryProductDetails(params)
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                loadFailed = true
                return
            }
            val details = result.productDetailsList.orEmpty()
            val loaded = details.mapNotNull { detail ->
                val product = makeProduct(detail)
                if (product != null) productDetails[product.id] = detail
                product
            }
            products = loaded.sortedByDescending { it.priceMicros }
            loadFailed = loaded.isEmpty()
            if (loaded.isNotEmpty()) {
                isEligibleForTrial = loaded.any { it.freeTrialPeriod != null }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadFailed = true
        } finally {
            isLoading = false
        }
    }

    /** Builds the paywall model from Play's details: the free-trial offer if offered, else the base plan. */
    private fun makeProduct(details: ProductDetails): SubscriptionProduct? {
        val offers = details.subscriptionOfferDetails.orEmpty()
        if (offers.isEmpty()) return null
        val trialOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        }
        val baseOffer = offers.firstOrNull { it.offerId == null } ?: offers.first()
        val chosen = trialOffer ?: baseOffer
        val phases = chosen.pricingPhases.pricingPhaseList
        if (phases.isEmpty()) return null
        val recurring = phases.lastOrNull { it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING }
            ?: phases.last()
        val trialPhase = trialOffer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros == 0L }
        return SubscriptionProduct(
            id = details.productId,
            title = details.name,
            formattedPrice = recurring.formattedPrice,
            priceMicros = recurring.priceAmountMicros,
            currencyCode = recurring.priceCurrencyCode,
            billingPeriod = recurring.billingPeriod,
            freeTrialPeriod = trialPhase?.billingPeriod,
            offerToken = chosen.offerToken,
        )
    }

    /** Re-reads the user's subscriptions from Google Play. Returns false when Play couldn't be reached. */
    private suspend fun queryEntitlements(): Boolean {
        try {
            val client = billingClient
            if (client == null || !connect()) return false
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = client.queryPurchasesAsync(params)
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return false
            val ours = result.purchasesList.filter { isOurs(it) }
            acknowledge(ours)
            playPremium = ours.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        } finally {
            updatePremium()
        }
    }

    suspend fun refreshEntitlements() {
        queryEntitlements()
    }

    private fun isOurs(purchase: Purchase): Boolean =
        purchase.products.any { it in SubscriptionConfig.productIDs }

    /** Play refunds purchases that aren't acknowledged within three days. */
    private suspend fun acknowledge(purchases: List<Purchase>) {
        val client = billingClient ?: return
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                try {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    client.acknowledgePurchase(params)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Tried again on the next refresh.
                }
            }
        }
    }

    // Purchasing

    /** Opens Google Play's purchase sheet. Throws [PurchaseException] when the purchase fails. */
    suspend fun purchase(activity: Activity, product: SubscriptionProduct): PurchaseOutcome {
        val details = productDetails[product.id]
            ?: throw PurchaseException("This subscription isn't available right now. Please try again.")
        val client = billingClient
        if (client == null || !connect()) {
            throw PurchaseException("Google Play isn't available right now. Please try again.")
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(product.offerToken)
                        .build()
                )
            )
            .build()

        val deferred = CompletableDeferred<PurchaseOutcome>()
        pendingPurchase?.cancel()
        pendingPurchase = deferred
        try {
            val launch = client.launchBillingFlow(activity, params)
            when (launch.responseCode) {
                BillingClient.BillingResponseCode.OK -> Unit
                BillingClient.BillingResponseCode.USER_CANCELED -> return PurchaseOutcome.CANCELLED
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    queryEntitlements()
                    if (isPremium) return PurchaseOutcome.PURCHASED
                    throw PurchaseException(PURCHASE_FAILED)
                }
                else -> throw PurchaseException(PURCHASE_FAILED)
            }
            return deferred.await()
        } finally {
            if (pendingPurchase === deferred) pendingPurchase = null
        }
    }

    private suspend fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val deferred = pendingPurchase
        try {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val ours = purchases.orEmpty().filter { isOurs(it) }
                    acknowledge(ours)
                    val purchased = ours.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    queryEntitlements()
                    // Play can lag a moment behind the purchase sheet; trust the purchase itself.
                    if (purchased) playPremium = true
                    updatePremium()
                    val outcome = when {
                        purchased -> {
                            isEligibleForTrial = false
                            PurchaseOutcome.PURCHASED
                        }
                        ours.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseOutcome.PENDING
                        else -> PurchaseOutcome.CANCELLED
                    }
                    deferred?.complete(outcome)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> deferred?.complete(PurchaseOutcome.CANCELLED)
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    queryEntitlements()
                    if (isPremium) deferred?.complete(PurchaseOutcome.PURCHASED)
                    else deferred?.completeExceptionally(PurchaseException(PURCHASE_FAILED))
                }
                else -> deferred?.completeExceptionally(PurchaseException(PURCHASE_FAILED))
            }
        } catch (e: CancellationException) {
            deferred?.cancel()
            throw e
        } catch (e: Exception) {
            deferred?.completeExceptionally(PurchaseException(PURCHASE_FAILED))
        }
    }

    /** Re-syncs purchases with Google Play (for a new phone or reinstall). Throws when Play can't be reached. */
    suspend fun restore() {
        if (!queryEntitlements()) throw PurchaseException("Restore didn't finish. Please try again.")
    }

    companion object {
        /**
         * Testing tools (unlocking Premium without paying) exist only in debug builds.
         * Google Play builds are release builds, so customers never see them.
         */
        val testingToolsEnabled: Boolean = BuildConfig.DEBUG

        private const val SEEN_KEY = "kp.hasSeenPaywall"
        private const val TEST_UNLOCK_KEY = "kp.testUnlocked"
        private const val PURCHASE_FAILED = "Google Play couldn't complete this purchase. Please try again."
    }
}

/** Pure helpers for price and trial wording, kept free of Play Billing objects so they can be tested. */
object PricingText {
    private val periodPattern = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?$")

    private class Period(val years: Int, val months: Int, val weeks: Int, val days: Int)

    private fun parse(isoPeriod: String): Period? {
        val match = periodPattern.matchEntire(isoPeriod.trim().uppercase(Locale.US)) ?: return null
        fun part(index: Int): Int = match.groupValues[index].toIntOrNull() ?: 0
        return Period(part(1), part(2), part(3), part(4))
    }

    /** "month" for P1M, "year" for P1Y, "week" for P1W, "day" for P1D. */
    fun periodWord(isoPeriod: String): String {
        val period = parse(isoPeriod) ?: return "period"
        return when {
            period.years > 0 -> "year"
            period.months > 0 -> "month"
            period.weeks > 0 -> "week"
            period.days > 0 -> "day"
            else -> "period"
        }
    }

    /** "7-day" for a one-week or seven-day trial, "1-month" for a month. */
    fun trialLabel(isoPeriod: String): String {
        val period = parse(isoPeriod) ?: return isoPeriod
        return when {
            period.years > 0 -> "${period.years}-year"
            period.months > 0 -> "${period.months}-month"
            else -> "${period.weeks * 7 + period.days}-day"
        }
    }

    /** Percent saved by paying yearly instead of 12 × monthly, rounded down. */
    fun annualSavingsPercent(annualMicros: Long, monthlyMicros: Long): Int? {
        val yearOfMonthly = monthlyMicros * 12
        if (yearOfMonthly <= 0 || annualMicros >= yearOfMonthly) return null
        return ((yearOfMonthly - annualMicros) * 100 / yearOfMonthly).toInt()
    }

    /** The annual price spread over twelve months, rounded down to cents, e.g. "$6.66". */
    fun perMonth(annualMicros: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        var digits = 2
        try {
            val currency = Currency.getInstance(currencyCode)
            format.currency = currency
            if (currency.defaultFractionDigits >= 0) digits = currency.defaultFractionDigits
        } catch (e: IllegalArgumentException) {
            // Unknown code: keep the locale's currency symbol and two decimals.
        }
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        val value = BigDecimal.valueOf(annualMicros).divide(BigDecimal.valueOf(12_000_000L), digits, RoundingMode.DOWN)
        return format.format(value)
    }
}
