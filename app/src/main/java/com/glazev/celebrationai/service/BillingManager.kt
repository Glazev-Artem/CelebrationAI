package com.glazev.celebrationai.service

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.ProductPurchase
import ru.rustore.sdk.pay.model.ProductPurchaseStatus
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus

// Интерфейс для поддержки мульти-стора (RuStore, Google Play, AppGallery)
interface BillingManager {
    val isSubscribed: StateFlow<Boolean>
    fun initialize()
    fun purchaseSubscription(activity: Activity, productId: String)
    fun restorePurchases()
}

class RuStoreBillingManagerImpl(private val context: Context) : BillingManager {
    private val appSettings = com.glazev.celebrationai.data.AppSettings(context)

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed

    override fun initialize() {
        Log.d("Billing", "Initializing RuStore Pay SDK...")
        RuStorePayClient.instance.getPurchaseInteractor().getPurchaseAvailability()
            .addOnSuccessListener { result ->
                if (result is ru.rustore.sdk.pay.model.PurchaseAvailabilityResult.Available) {
                    Log.d("Billing", "Purchase is available")
                    restorePurchases()
                } else {
                    Log.w("Billing", "Purchase is unavailable: $result")
                }
            }
            .addOnFailureListener { error ->
                Log.e("Billing", "Availability check error", error)
            }
    }

    override fun purchaseSubscription(activity: Activity, productId: String) {
        android.widget.Toast.makeText(context, context.getString(com.glazev.celebrationai.R.string.msg_billing_start), android.widget.Toast.LENGTH_SHORT).show()
        
        RuStorePayClient.instance.getPurchaseInteractor().purchase(
            params = ru.rustore.sdk.pay.model.ProductPurchaseParams(productId = ru.rustore.sdk.pay.model.ProductId(productId)),
            preferredPurchaseType = PreferredPurchaseType.ONE_STEP
        )
        .addOnSuccessListener { paymentResult ->
            Log.d("Billing", "Purchase request sent/completed: $paymentResult")
            // Мы вызываем restorePurchases чтобы достоверно обновить статус подписки
            restorePurchases()
        }
        .addOnFailureListener { error ->
            Log.e("Billing", "Purchase Error", error)
            _isSubscribed.value = false // Строгий фикс: сброс при обрыве связи
            android.widget.Toast.makeText(context, context.getString(com.glazev.celebrationai.R.string.msg_billing_error, error.message ?: "Network error"), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun restorePurchases() {
        RuStorePayClient.instance.getPurchaseInteractor().getPurchases()
            .addOnSuccessListener { purchases ->
                var isActive = false
                var invoiceId: String? = null

                for (purchase in purchases) {
                    if (purchase is SubscriptionPurchase) {
                        if ((purchase.productId.value == "1_month" || purchase.productId.value == "1_year") && purchase.status == SubscriptionPurchaseStatus.ACTIVE) {
                            isActive = true
                            invoiceId = purchase.invoiceId?.value ?: purchase.invoiceId.toString()
                        }
                    } else if (purchase is ProductPurchase) {
                        if ((purchase.productId.value == "1_month" || purchase.productId.value == "1_year") && (purchase.status == ProductPurchaseStatus.PAID || purchase.status == ProductPurchaseStatus.CONFIRMED)) {
                            isActive = true
                            invoiceId = purchase.invoiceId?.value ?: purchase.invoiceId.toString()
                        }
                    }
                }

                _isSubscribed.value = isActive
                if (invoiceId != null) {
                    appSettings.invoiceId = invoiceId
                }
                
                Log.d("Billing", "Restored purchases. Subscribed: ${_isSubscribed.value}")
            }
            .addOnFailureListener { error ->
                Log.e("Billing", "Restore Error", error)
                _isSubscribed.value = false // Строгий фикс: сброс при недоступности сети/VPN
            }
    }
}
