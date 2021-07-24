package com.yukido.subscriptiondemo

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

class MyBillingImpl {

    companion object {
        private var instance: BillingClient? = null

        fun getInstance(context: Context, listener: PurchasesUpdatedListener): BillingClient {
            return if (instance != null)
                instance as BillingClient
            else initBillingClient(context, listener)
        }

        private fun initBillingClient(
            context: Context,
            listener: PurchasesUpdatedListener
        ): BillingClient {
            instance = BillingClient.newBuilder(context)
                .setListener(listener)
                .enablePendingPurchases()
                .build()
            return instance as BillingClient
        }
    }

}