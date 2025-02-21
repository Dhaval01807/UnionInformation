package com.files.fileexplorer.filecleanup.cleaner.Activity

import android.app.Dialog
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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
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
import com.files.fileexplorer.filecleanup.cleaner.R
import com.files.fileexplorer.filecleanup.cleaner.Utils.AdLoader
import com.files.fileexplorer.filecleanup.cleaner.Utils.AppConstant
import com.files.fileexplorer.filecleanup.cleaner.Utils.RemoteValue
import com.files.fileexplorer.filecleanup.cleaner.Utils.StringConstant
import com.files.fileexplorer.filecleanup.cleaner.databinding.ActivityProScreenBinding
import com.google.common.collect.ImmutableList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ProScreen : AppCompatActivity() {
    lateinit var billingClient: BillingClient
    val SKU_Lifetime_AddBucket_120: String = "file_manager_pro_monthly"
    val SKU_Lifetime_AddBucket_240: String = "file_manager_pro_yearly"
    private var purchaseItem: Purchase? = null
    var price: MutableLiveData<Double> = MutableLiveData(0.0)
    var yearly: MutableLiveData<Double> = MutableLiveData(0.0)

    private lateinit var binding: ActivityProScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var plan: Boolean = true
        val intent = intent
        var fromPro: Boolean = false
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()


        getPrice()

        intent?.let {
            fromPro = it.getBooleanExtra("fromPro", true)
        }


        val sharePref = applicationContext.getSharedPreferences(
            StringConstant.packageName,
            MODE_PRIVATE
        )




        if (fromPro) {
            binding.ivClose.visibility = View.VISIBLE
            binding.btnWithAd.visibility = View.GONE
        } else {
            if (RemoteValue.rewardAdId.isNotEmpty()) {
                AdLoader.loadRewardedAd(RemoteValue.rewardAdId, this)
                AdLoader.rewardAdLoaded.observe(this) {

                    if (AdLoader.rewardFailed) {
                        val newIntent = Intent(this, HomeScreen::class.java)
                        startActivity(newIntent)
                        finish()
                    }
                    if (binding.progressbar.visibility == View.VISIBLE) {
                        AdLoader.showRewardedAd(this)
                    }
                }
            }
        }


        binding.btnBuyPro.setOnClickListener {
            if (plan) {
                if (binding.monthlyPlanPrice.visibility == VISIBLE)
                    purchaseProduct(SKU_Lifetime_AddBucket_120)
            } else {
                if (binding.yearlyPlanPrice.visibility == VISIBLE)
                    purchaseProduct(SKU_Lifetime_AddBucket_240)
            }
        }

        binding.yearlyPlan.setOnClickListener {
            plan = false
            binding.yearlyPlan.background =
                AppCompatResources.getDrawable(this, R.drawable.primary_color_border)
            binding.yearlyPlanTick.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_tick
                )
            )
            binding.monthlyPlan.background = null
            binding.monthlyPlanTick.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_untick
                )
            )
        }

        binding.monthlyPlan.setOnClickListener {
            plan = true
            binding.monthlyPlan.background =
                AppCompatResources.getDrawable(this, R.drawable.primary_color_border)
            binding.monthlyPlanTick.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_tick
                )
            )
            binding.yearlyPlan.background = null
            binding.yearlyPlanTick.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_untick
                )
            )
        }



        binding.ivClose.setOnClickListener {
            finish()
        }

        binding.btnWithAd.setOnClickListener {
            if (RemoteValue.rewardAdId.isEmpty()) {
                val newIntent = Intent(this, HomeScreen::class.java)
                startActivity(newIntent)
                finish()
            } else {
                if (AdLoader.rewardFailed) {
                    val newIntent = Intent(this, HomeScreen::class.java)
                    startActivity(newIntent)
                    finish()
                } else {
                    if (AdLoader.rewardAdLoaded.value!!) {
                        AdLoader.showRewardedAd(this)
                    } else {
                        binding.progressbar.visibility = View.VISIBLE
                    }
                }
            }

        }

        val termSpanning: SpannableString = SpannableString(binding.term.text)
        val privacy = SpannableString(binding.privacy.text)
        termSpanning.setSpan(
            ForegroundColorSpan(getColor(R.color.colorBlue)),
            0,
            binding.term.text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        )
        termSpanning.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(RemoteValue.privacyLink))
                    startActivity(intent)
                }

            },
            0,
            binding.term.text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        privacy.setSpan(
            ForegroundColorSpan(getColor(R.color.colorBlue)),
            0,
            binding.privacy.text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        privacy.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(RemoteValue.privacyLink))
                    startActivity(intent)
                }

            },
            0,
            binding.privacy.text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.term.isClickable = true
        binding.term.movementMethod = LinkMovementMethod.getInstance()
        binding.term.text = termSpanning

        binding.privacy.isClickable = true
        binding.privacy.movementMethod = LinkMovementMethod.getInstance()
        binding.privacy.text = privacy

    }

    fun purchaseProduct(id: String) {
        Log.e("TAG", "purchaseProduct: $id")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        ImmutableList.of(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(id).setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    ).build()

                billingClient.queryProductDetailsAsync(queryProductDetailsParams,
                    object : ProductDetailsResponseListener {
                        override fun onProductDetailsResponse(
                            billingResult: BillingResult,
                            productDetailsList: MutableList<ProductDetails>
                        ) {
                            for (i in productDetailsList) {
                                val offerToken = i.subscriptionOfferDetails!![0].offerToken
                                val productParamsList = ImmutableList.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(i).setOfferToken(offerToken).build()
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

                    })
            }

        })

    }

    private fun getPrice() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val executorService: ExecutorService = Executors.newSingleThreadExecutor()
                    executorService.execute {
                        val queryProductDetailsParams =
                            QueryProductDetailsParams.newBuilder()
                                .setProductList(
                                    ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId(SKU_Lifetime_AddBucket_120)
                                            .setProductType(BillingClient.ProductType.SUBS)
                                            .build(),
                                        QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId(SKU_Lifetime_AddBucket_240)
                                            .setProductType(BillingClient.ProductType.SUBS)
                                            .build()
                                    )
                                )
                                .build()
                        billingClient.queryProductDetailsAsync(
                            queryProductDetailsParams
                        ) { billingResult, productDetailsList ->
                            Log.e("TAG", "onBillingSetupFinished: ${productDetailsList.size}")
                            for (productDetails in productDetailsList) {
                                try {
                                    checkNotNull(productDetails.subscriptionOfferDetails)
                                    val id = productDetails!!.productId
                                    val formattedprice = productDetails.subscriptionOfferDetails!!.first().pricingPhases.pricingPhaseList.first().formattedPrice
                                    if (id == SKU_Lifetime_AddBucket_120) {
                                        runOnUiThread {
                                            binding.montlyProgress.visibility = GONE
                                            binding.monthlyPlanPrice.visibility = VISIBLE
                                            binding.monthlyPlanPrice.text = formattedprice
                                        }
                                    } else if (id == SKU_Lifetime_AddBucket_240) {
                                        runOnUiThread {
                                            binding.yearlyProgress.visibility = GONE
                                            binding.yearlyPlanPrice.visibility = VISIBLE
                                            binding.yearlyPlanPrice.text = formattedprice
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("TAG", "onBillingSetupFinished: ",e )
                                }
                            }
                        }
                    }
                    runOnUiThread {
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
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
            val i: Intent = Intent(this@ProScreen, HomeScreen::class.java)
            startActivity(i)
            finish()
        }
        if (!isFinishing) {
            dialog.show()
        }
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
                AppConstant.isPremium = true
                successDialog()
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                AppConstant.isPremium = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                AppConstant.isPremium = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                AppConstant.isPremium = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                AppConstant.isPremium = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
                AppConstant.isPremium = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR) {
                AppConstant.isPremium = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                AppConstant.isPremium = false
            } else {
                Toast.makeText(this, "Try Again", Toast.LENGTH_SHORT).show()
            }
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
                AppConstant.isPremium = true
            }

        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            AppConstant.isPremium = false
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            AppConstant.isPremium = false
        }
    }

    var acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                AppConstant.isPremium = true
                successDialog()
            }
        }
}
