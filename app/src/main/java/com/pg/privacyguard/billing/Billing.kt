package com.pg.privacyguard.billing

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.pg.privacyguard.R
import com.pg.privacyguard.data.Blocklist
import com.pg.privacyguard.data.Pro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the user's Play purchases mean for Pro. */
enum class PurchaseStatus { NONE, PENDING, PURCHASED }

/** The parts of a Play purchase that decide Pro, so [proStatus] can be unit tested. */
data class OwnedPurchase(val products: List<String>, val state: Int, val acknowledged: Boolean, val token: String)

/** A completed purchase of Pro wins over a pending one; anything else (refunded, none) is [PurchaseStatus.NONE]. */
internal fun proStatus(purchases: List<OwnedPurchase>): PurchaseStatus {
    val pro = purchases.filter { Pro.PRODUCT_ID in it.products }
    return when {
        pro.any { it.state == Purchase.PurchaseState.PURCHASED } -> PurchaseStatus.PURCHASED
        pro.any { it.state == Purchase.PurchaseState.PENDING } -> PurchaseStatus.PENDING
        else -> PurchaseStatus.NONE
    }
}

/**
 * Google Play Billing for the one-time Pro purchase. No server and no account: the app asks
 * Play on the phone what was bought. A refunded purchase disappears from Play's answer, so
 * [refresh] then removes Pro. Pro is only removed on a clear answer from Play, never because
 * Play couldn't be reached.
 */
object Billing {
    data class State(
        /** Local price from Play, e.g. "BDT 220.00"; null until Play answers. Never hard-coded. */
        val price: String? = null,
        val pending: Boolean = false,
        /** False when Play Billing isn't available, e.g. the app wasn't installed from Play. */
        val available: Boolean = false,
        @StringRes val message: Int? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val refreshMutex = Mutex()
    private var client: BillingClient? = null
    private var product: ProductDetails? = null
    private lateinit var appContext: Context

    /** Connects once and checks purchases. Call when the app opens. */
    fun connect(context: Context) {
        if (client != null) {
            scope.launch { refresh() }
            return
        }
        appContext = context.applicationContext
        val newClient = BillingClient.newBuilder(appContext)
            .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = newClient
        newClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    scope.launch { refresh() }
                } else {
                    _state.update { it.copy(available = false) }
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection reconnects on the next call.
            }
        })
    }

    /** Asks Play for the price and for what the user owns. Restores Pro on a new phone. */
    suspend fun refresh() = refreshMutex.withLock {
        val client = client ?: return@withLock
        val details = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Pro.PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            ).build()
        )
        if (details.billingResult.responseCode == BillingResponseCode.OK) {
            product = details.productDetailsList?.firstOrNull()
            val price = product?.oneTimePurchaseOfferDetails?.formattedPrice
            _state.update { it.copy(price = price, available = product != null) }
        }

        val owned = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        )
        if (owned.billingResult.responseCode == BillingResponseCode.OK) handle(owned.purchasesList)
    }

    /** Opens Play's purchase screen. The result arrives in [onPurchasesUpdated]. */
    fun buy(activity: Activity) {
        val client = client
        val product = product
        if (client == null || product == null) {
            _state.update { it.copy(message = R.string.pro_not_available) }
            return
        }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
        ).build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            _state.update { it.copy(message = R.string.pro_purchase_failed) }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch { handle(purchases.orEmpty()) }
            BillingResponseCode.USER_CANCELED -> {}
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { refresh() }
            else -> _state.update { it.copy(message = R.string.pro_purchase_failed) }
        }
    }

    private suspend fun handle(purchases: List<Purchase>) {
        val owned = purchases.map {
            OwnedPurchase(it.products, it.purchaseState, it.isAcknowledged, it.purchaseToken)
        }
        val status = proStatus(owned)
        // Unacknowledged purchases are refunded by Play after 3 days; if this fails, the
        // next refresh tries again.
        owned.filter { it.state == Purchase.PurchaseState.PURCHASED && !it.acknowledged }.forEach {
            client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.token).build())
        }
        Pro.set(appContext, status == PurchaseStatus.PURCHASED)
        Blocklist.onProChanged()
        _state.update { it.copy(pending = status == PurchaseStatus.PENDING) }
    }
}
