package com.example.financialcalc.Activitys

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.financialcalc.Ads.AdConstant
import com.example.financialcalc.Ads.AdLoad
import com.example.financialcalc.MainActivity
import com.example.financialcalc.R
import com.example.financialcalc.Utils.AppConstant
import com.google.common.collect.ImmutableList


class ProScreen : AppCompatActivity(){
    lateinit var billingClient: BillingClient
    val SKU_Lifetime_AddBucket_120: String = "financial_pro"
    private var purchaseItem: Purchase? = null
    private var mProgressDialog:ProgressDialog? = null
    var price: MutableLiveData<String> = MutableLiveData("0.0")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pro_screen)
        val closeBtn: ImageView = findViewById(R.id.iv_close)
        val termText: TextView = findViewById(R.id.tv_privacy_policy_text)
        val continueWithAd = findViewById<CardView>(R.id.btn_with_ad)
        val purchaseBtn = findViewById<CardView>(R.id.btn_buy_pro)
        val progressBar = findViewById<LinearLayout>(R.id.progress)
        val priceView: TextView = findViewById(R.id.price)
        val intent = intent
        var fromPro: Boolean = false
        setupBillingClient()
        intent?.let {
            fromPro = it.getBooleanExtra("fromPro", false)
        }


        val sharePref = applicationContext.getSharedPreferences(
            AppConstant.PACKAGE_NAME,
            MODE_PRIVATE
        )


        var localPrice = sharePref.getInt("price", 0)

        price.value = localPrice.toString()
        price.observe(this) {
            priceView.text = "${it.toString()}/Lifetime"
        }


        if (fromPro) {
            closeBtn.visibility = View.VISIBLE
            continueWithAd.visibility = View.GONE
        } else {
            if (AdConstant.rewardAdId.isNotEmpty()) {
                AdLoad.loadRewardedAd(AdConstant.rewardAdId, this)
                AdLoad.rewardAdLoaded.observe(this) {

                    if (AdLoad.rewardFailed) {
                        val newIntent = Intent(this, MainActivity::class.java)
                        startActivity(newIntent)
                        finish()
                    }
                    if (progressBar.visibility == View.VISIBLE) {
                        AdLoad.showRewardedAd(this)
                    }
                }
            }
        }

        purchaseBtn.setOnClickListener {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog?.show()
            purchaseItem(SKU_Lifetime_AddBucket_120)
        }




        closeBtn.setOnClickListener {
            finish()
        }

        continueWithAd.setOnClickListener {
            if (AdConstant.rewardAdId.isEmpty()) {
                val newIntent = Intent(this, MainActivity::class.java)
                startActivity(newIntent)
                finish()
            } else {
                if (AdLoad.rewardFailed) {
                    val newIntent = Intent(this, MainActivity::class.java)
                    startActivity(newIntent)
                    finish()
                } else {
                    if (AdLoad.rewardAdLoaded.value!!) {
                        AdLoad.showRewardedAd(this)
                    } else {
                        progressBar.visibility = View.VISIBLE
                    }
                }
            }

        }

        Log.e("TAG", "onCreate: ${termText.text}")
        val termSpanning: SpannableString = SpannableString(termText.text)
        val indexOfTerm = termText.text.indexOf("Term")
        val indexOfPrivacy = termText.text.indexOf("Privacy")

        termSpanning.setSpan(
            ForegroundColorSpan(getColor(R.color.colorBlue)),
            indexOfTerm,
            indexOfTerm + 16,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        )
        termSpanning.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(AppConstant.privacyLink))
                    startActivity(intent)
                }

            },
            indexOfTerm,
            indexOfTerm + 16,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        termSpanning.setSpan(
            ForegroundColorSpan(getColor(R.color.colorBlue)),
            indexOfPrivacy,
            indexOfPrivacy + 14,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        termSpanning.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(AppConstant.privacyLink))
                    startActivity(intent)
                }

            },
            indexOfPrivacy,
            indexOfPrivacy + 14,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        termText.isClickable = true
        termText.movementMethod = LinkMovementMethod.getInstance()


        termText.text = termSpanning
    }

    private fun setupBillingClient() {
        val instance = BillingClient.newBuilder(this)
        .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        this.billingClient = instance

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                }
            }
        })
        instance.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == 0) {
                    return
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    fun purchaseItem(item: String) {

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        ImmutableList.of(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(SKU_Lifetime_AddBucket_120)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        )
                    ).build()

                billingClient.queryProductDetailsAsync(
                    queryProductDetailsParams,
                    object : ProductDetailsResponseListener {
                        override fun onProductDetailsResponse(
                            billingResult: BillingResult,
                            productDetailsList: MutableList<ProductDetails>
                        ) {
                            for (i in productDetailsList) {
                                val productParamsList = ImmutableList.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(i).build()
                                )
                                val billingFlowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productParamsList)
                                    .build()

                                billingClient.launchBillingFlow(
                                    this@ProScreen,
                                    billingFlowParams
                                )
                            }
                        }

                    },
                )
            }

        })
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult: BillingResult, purchases: List<Purchase?>? ->

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    purchase?.let {
                        handlePurchase(purchase)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                AppConstant.PURCHASE_STATUS = true
                successDialog()
                applicationContext.getSharedPreferences(AppConstant.PACKAGE_NAME, MODE_PRIVATE).edit().putBoolean("PURCHASE", true).apply()
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                AppConstant.PURCHASE_STATUS = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                AppConstant.PURCHASE_STATUS = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                AppConstant.PURCHASE_STATUS = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                AppConstant.PURCHASE_STATUS = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
                AppConstant.PURCHASE_STATUS = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR) {
                AppConstant.PURCHASE_STATUS = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                AppConstant.PURCHASE_STATUS = false
            } else {
                Toast.makeText(this, "Try Again", Toast.LENGTH_SHORT).show()
            }
            mProgressDialog?.dismiss()

        }

    fun handlePurchase(purchase: Purchase) {
        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

        val listener =
            ConsumeResponseListener { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                }
            }
        billingClient.consumeAsync(consumeParams, listener)

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(
                    acknowledgePurchaseParams,
                    acknowledgePurchaseResponseListener
                )
                successDialog()
                AppConstant.PURCHASE_STATUS = true
                applicationContext.getSharedPreferences(AppConstant.PACKAGE_NAME, MODE_PRIVATE).edit().putBoolean("PURCHASE", true).apply()
            }

        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            AppConstant.PURCHASE_STATUS = false
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            AppConstant.PURCHASE_STATUS = false
        }
    }

    var acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                AppConstant.PURCHASE_STATUS = true
                successDialog()
            }
        }

    private fun queryProductDetails() {
        val productList = ImmutableList.of(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_Lifetime_AddBucket_120)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()


        // Query product details asynchronously
        billingClient.queryProductDetailsAsync(
            params,
            ProductDetailsResponseListener { billingResult, productDetailsList ->
                Log.e("TAG", "queryProductDetails: ${productDetailsList.size}")
                for (productDetails in productDetailsList) {
                    try {
                        val id = productDetails!!.productId
                        val formattedprice =
                            productDetails.oneTimePurchaseOfferDetails?.formattedPrice

                        price.postValue(formattedprice)
                    } catch (e: Exception) {
                        Log.e("TAG", "onBillingSetupFinished: ", e)
                    }
                }
            })


    }




    private fun successDialog() {
        val dialog = Dialog(this, R.style.s_permission)
        dialog.setContentView(R.layout.my_purchase_dialog)
        dialog.setCancelable(false)
        val buttonYes = dialog.findViewById<TextView>(R.id.buttonOk)
        buttonYes.setOnClickListener {
            dialog.dismiss()
            val i: Intent = Intent(this@ProScreen, MainActivity::class.java)
            startActivity(i)
            finish()
        }
        if (!isFinishing) {
            dialog.show()
        }
    }

    fun handleItemAlreadyPurchase(list: List<Purchase>) {
        purchaseItem = null
        val next = list[0]
        this.billingClient.consumeAsync(
            ConsumeParams.newBuilder()
                .setPurchaseToken(next.purchaseToken).build(),
            ConsumeResponseListener { billingResult, s ->
                if (next.purchaseToken.equals(
                        SKU_Lifetime_AddBucket_120,
                        ignoreCase = true
                    )
                ) {
                    val sharePref = applicationContext.getSharedPreferences(
                        AppConstant.PACKAGE_NAME,
                        MODE_PRIVATE
                    )
                    sharePref.edit().putBoolean("PURCHASE", true).apply()
                }
            })
    }
}
