package com.pg.privacyguard.billing

import com.android.billingclient.api.Purchase.PurchaseState
import com.pg.privacyguard.data.Pro
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingTest {
    private fun purchase(state: Int, product: String = Pro.PRODUCT_ID) =
        OwnedPurchase(listOf(product), state, acknowledged = false, token = "t")

    @Test
    fun purchasedUnlocksPro() {
        assertEquals(PurchaseStatus.PURCHASED, proStatus(listOf(purchase(PurchaseState.PURCHASED))))
    }

    @Test
    fun pendingDoesNotUnlockYet() {
        assertEquals(PurchaseStatus.PENDING, proStatus(listOf(purchase(PurchaseState.PENDING))))
    }

    @Test
    fun refundedPurchaseIsGoneSoProIsRemoved() {
        // Play leaves a refunded purchase out of its answer.
        assertEquals(PurchaseStatus.NONE, proStatus(emptyList()))
    }

    @Test
    fun otherProductsDontCount() {
        assertEquals(PurchaseStatus.NONE, proStatus(listOf(purchase(PurchaseState.PURCHASED, "something_else"))))
    }

    @Test
    fun purchasedWinsOverPending() {
        val both = listOf(purchase(PurchaseState.PENDING), purchase(PurchaseState.PURCHASED))
        assertEquals(PurchaseStatus.PURCHASED, proStatus(both))
    }
}
