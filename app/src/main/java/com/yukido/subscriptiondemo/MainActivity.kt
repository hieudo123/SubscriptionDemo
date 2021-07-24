package com.yukido.subscriptiondemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.yukido.subscriptiondemo.databinding.ActivityMainBinding
import timber.log.Timber


class MainActivity : AppCompatActivity(), PurchasesUpdatedListener {
    companion object {
        const val SUBSCRIPTION_ID = "premium_test"
    }

    private lateinit var binding: ActivityMainBinding
    private var billingClient: BillingClient? = null
    private var skuDetailsList: MutableList<SkuDetails> = mutableListOf()
    private var stringBuilder = StringBuilder()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        Timber.plant(Timber.DebugTree())
        Timber.d("****** onCreate ******")
        setupBillingClient()
        binding.btnSubscription.setOnClickListener {
            if (skuDetailsList.isNotEmpty()) {
                lunchPurchaseFlow()
            }
        }

        setContentView(binding.root)

    }

    private fun setupBillingClient() {
        billingClient = MyBillingImpl.getInstance(this, this)

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Timber.e("****** onBillingServiceDisconnected ******")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val responseCode = billingResult.responseCode
                Timber.d("****** onBillingSetupFinished $responseCode ******")
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                    getPurchase()
                }
            }

        })
    }

    fun querySkuDetails() {
        val skuList = ArrayList<String>()
        skuList.add(SUBSCRIPTION_ID)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        billingClient?.querySkuDetailsAsync(
            params.build()
        ) { billingResult, skuDetailsList ->
            val responseCode = billingResult.responseCode
            Timber.d("\nSkuDetailsResponseListener: $responseCode \nList size: ${skuDetailsList?.size}")

            skuDetailsList?.forEach { item ->
                stringBuilder.append("" +
                        "\nProduct_id: ${item.sku}" +
                        "\nDescription: ${item.description}" +
                        "\nPrice: ${item.price}")
                MainActivity@this.skuDetailsList.add(item)
            }

            binding.tvProducts.text = stringBuilder.toString()

        }
    }

    private fun getPurchase() {
        billingClient?.queryPurchasesAsync(BillingClient.SkuType.SUBS
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!purchaseList.isNullOrEmpty()) {
                    purchaseList.forEach {
                        handlePurchase(it)
                    }
                }
            }
        }
    }

    private fun lunchPurchaseFlow() {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetailsList[0])
            .build()
        val responseCode = billingClient?.launchBillingFlow(this, flowParams)?.responseCode
        Timber.d("lunchPurchaseFlow: $responseCode")
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchaseList: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            for (purchase in purchaseList) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Timber.e("Handle an error caused by a user cancelling the purchase flow.")
        } else {
            Timber.e(billingResult.debugMessage)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        acknowledgePurchase(purchase)
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener {
            if (it.responseCode == BillingClient.BillingResponseCode.OK) {
                binding.tvProducts.text = "Already subscript"
                binding.btnSubscription.isEnabled = false
            } else {
                binding.tvProducts.text = stringBuilder.toString()
                binding.btnSubscription.isEnabled = true
            }
        }
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(
                    acknowledgePurchaseParams,
                    acknowledgePurchaseResponseListener
                )
            }
        }
    }

}