package com.glazev.celebrationai.service

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult

// Интерфейс для поддержки мульти-стора (RuStore, Google Play, AppGallery)
interface BillingManager {
    val isSubscribed: StateFlow<Boolean>
    fun initialize()
    fun purchaseSubscription(activity: Activity, productId: String)
    fun restorePurchases()
}

class RuStoreBillingManagerImpl(private val context: Context) : BillingManager {
    private val billingClient: RuStoreBillingClient = RuStoreBillingClientFactory.create(
        context = context,
        consoleApplicationId = "19452535", // ID приложения из RuStore Консоли
        deeplinkScheme = "celebrationai"
    )
    private val appSettings = com.glazev.celebrationai.data.AppSettings(context)

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed

    override fun initialize() {
        Log.d("Billing", "Initializing RuStore Billing...")
        restorePurchases()
    }

    override fun purchaseSubscription(activity: Activity, productId: String) {
        android.widget.Toast.makeText(context, context.getString(com.glazev.celebrationai.R.string.msg_billing_start), android.widget.Toast.LENGTH_SHORT).show()
        billingClient.purchases.purchaseProduct(productId)
            .addOnSuccessListener { paymentResult ->
                Log.d("Billing", "Purchase Success: $paymentResult")
                _isSubscribed.value = true
                if (paymentResult is ru.rustore.sdk.billingclient.model.purchase.PaymentResult.Success) {
                    appSettings.invoiceId = paymentResult.invoiceId
                }
                android.widget.Toast.makeText(context, context.getString(com.glazev.celebrationai.R.string.msg_billing_success), android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Log.e("Billing", "Purchase Error", error)
                android.widget.Toast.makeText(context, context.getString(com.glazev.celebrationai.R.string.msg_billing_error, error.message), android.widget.Toast.LENGTH_LONG).show()
            }
    }

    override fun restorePurchases() {
        billingClient.purchases.getPurchases()
            .addOnSuccessListener { purchases ->
                val activeSub = purchases.find { it.productId == "premium_sub" && it.purchaseState == ru.rustore.sdk.billingclient.model.purchase.PurchaseState.PAID }
                _isSubscribed.value = activeSub != null
                if (activeSub != null) {
                    appSettings.invoiceId = activeSub.invoiceId
                }
                Log.d("Billing", "Restored purchases. Subscribed: ${_isSubscribed.value}")
            }
            .addOnFailureListener { error ->
                Log.e("Billing", "Restore Error", error)
            }
    }
}
