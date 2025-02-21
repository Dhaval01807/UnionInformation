package com.files.fileexplorer.filecleanup.cleaner.Activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.files.fileexplorer.filecleanup.cleaner.R
import com.files.fileexplorer.filecleanup.cleaner.Utils.AppConstant
import com.files.fileexplorer.filecleanup.cleaner.Utils.AppConstant.Companion.isPremium
import com.files.fileexplorer.filecleanup.cleaner.Utils.RemoteValue
import com.files.fileexplorer.filecleanup.cleaner.Utils.StringConstant
import com.files.fileexplorer.filecleanup.cleaner.databinding.ActivitySplashScreenBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.material.snackbar.Snackbar
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    lateinit var sharedPreferences: SharedPreferences
    private var consentInformation: ConsentInformation? = null
    private lateinit var billingClient: BillingClient
    var adShowing: Boolean = false
    val isInternetAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isInternetAvailable.update {
            checkForInternet(this)
        }
        sharedPreferences =
            applicationContext.getSharedPreferences(StringConstant.packageName, MODE_PRIVATE)
        lifecycleScope.launch {
            isInternetAvailable.collectLatest {
                if(it){
                    AppConstant.isProEnable = sharedPreferences.getBoolean(StringConstant.proEnable, false)


                    val backgroundScope = CoroutineScope(Dispatchers.IO)
                    backgroundScope.launch {
                        MobileAds.initialize(this@SplashScreen) {

                        }
                    }
                    val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()

                    consentInformation = UserMessagingPlatform.getConsentInformation(this@SplashScreen)

                    consentInformation?.requestConsentInfoUpdate(this@SplashScreen, params, {
                        if (consentInformation?.isConsentFormAvailable!!) {
                            loadForm()
                        }
                    }, { formError -> })

                    billingClient = BillingClient.newBuilder(this@SplashScreen).setListener(purchasesUpdatedListener)
                        .enablePendingPurchases().build()
                    quaryPurchase()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        if (!adShowing) {
                            redirectTo()
                        }
                    }, 13000)
                }else{

                    val snack: Snackbar = Snackbar.make(binding.main, "Please Turn On Internet", Snackbar.LENGTH_LONG)
                    val view = snack.view
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    view.layoutParams = params
                    snack.show()

//
//                    val snackbar = Snackbar.make(this@SplashScreen,binding.main, "Please Turn On Internet", Snackbar.LENGTH_LONG)
//                        .setAction("UNDO") {
//                            val snackbar =
//                                Snackbar.make(this@SplashScreen,binding.main, "Please Turn On Internet", Snackbar.LENGTH_LONG)
//                            snackbar.show()
//                        }
//                    snackbar.show()
                }
            }
        }
    }


    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(this, { consentForm: ConsentForm ->
            if (consentInformation?.getConsentStatus() === ConsentInformation.ConsentStatus.REQUIRED) {
                consentForm.show(
                    this
                ) { formError: FormError? ->
                    if (consentInformation?.getConsentStatus() === ConsentInformation.ConsentStatus.OBTAINED) {
                        fetchFirebaseRemoteConfig()
                    }
                    loadForm()
                }
            }
        }, { formError: FormError? -> })
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases -> }

    private fun checkForInternet(context: Context): Boolean {

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun fetchFirebaseRemoteConfig() {
        FirebaseApp.initializeApp(this@SplashScreen)
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings: FirebaseRemoteConfigSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(10).build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(
            this@SplashScreen,
        ) { task ->
            if (task.isSuccessful) {

                val privacyLink = mFirebaseRemoteConfig.getString("privacyPolicy")
                val supportedEmail = mFirebaseRemoteConfig.getString("supportedEmail")
                val maintenance = mFirebaseRemoteConfig.getBoolean("maintenance")

                if (privacyLink.isNotEmpty()) {
                    RemoteValue.privacyLink = privacyLink
                }

                if (supportedEmail.isNotEmpty()) {
                    RemoteValue.supportEmail = supportedEmail
                }

                RemoteValue.showAd = mFirebaseRemoteConfig.getString("showAd") == "1"
                if (RemoteValue.showAd) {
                    RemoteValue.showAppOpen = mFirebaseRemoteConfig.getString("showAppOpen") == "1"
                    if (RemoteValue.showAppOpen) {
                        RemoteValue.appOpen =
                            mFirebaseRemoteConfig.getString("initialAppOpen")
                    }

                    RemoteValue.rewardAdId = mFirebaseRemoteConfig.getString("rewardAd")
                    RemoteValue.interstitialAdCount = mFirebaseRemoteConfig.getLong("interstitialCount").toInt()
                    RemoteValue.interstitialAdId = mFirebaseRemoteConfig.getString("interstitialAd")
                    RemoteValue.ftpRewardAd = mFirebaseRemoteConfig.getString("ftpRewardAd")
                    RemoteValue.imageBanner = mFirebaseRemoteConfig.getString("imageBanner")
                    RemoteValue.videoNative = mFirebaseRemoteConfig.getString("videoNative")
                    RemoteValue.ftpNative = mFirebaseRemoteConfig.getString("ftpNative")
                    RemoteValue.quickCleanReward = mFirebaseRemoteConfig.getString("cleanReward")
                }

                if (!maintenance) {


                    if (RemoteValue.showAd) {
                        if (RemoteValue.appOpen.isNotEmpty()) {
                            loadAppOpen(RemoteValue.appOpen)
                        } else {
                            redirectTo()
                        }
                    } else {
                        redirectTo()
                    }
                } else {
                    adShowing = true
                    showMaintenanceDialog(mFirebaseRemoteConfig.getString("maintenanceText"))
                }

            } else {
                redirectTo()
            }
        }
    }

    fun quaryPurchase() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                isPremium = false
                fetchFirebaseRemoteConfig()
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                    try {
                        billingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS).build()
                        ) { billingResult1, puchaseList ->
                            if (puchaseList.isNotEmpty()) {
                                for (purchase in puchaseList) {
                                    if (purchase != null && purchase.isAcknowledged) {
                                        isPremium = true
                                        redirectTo()
                                    }
                                }
                            } else {
                                isPremium = false
                                fetchFirebaseRemoteConfig()
                            }
                        }

                    } catch (e: Exception) {
                        isPremium = false
                        fetchFirebaseRemoteConfig()
                    }
                } else {
                    isPremium = false
                    fetchFirebaseRemoteConfig()
                }
            }
        })

    }


    private fun showMaintenanceDialog(txt: String) {
        val dialog = Dialog(this, R.style.FullWidthDialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window!!.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window!!.attributes = layoutParams
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val view = layoutInflater.inflate(R.layout.maintenance_dialog, null)


        val maintenanceText = view.findViewById<TextView>(R.id.maintenance_text)

        maintenanceText.text = txt
        dialog.setContentView(view)

        dialog.show()
    }

    private fun loadAppOpen(id: String) {
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            this, id, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {

                        override fun onAdDismissedFullScreenContent() {
                            redirectTo()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("TAG", "onAdFailedToShowFullScreenContent: $adError")
                            redirectTo()
                        }

                        override fun onAdShowedFullScreenContent() {
                        }
                    }
                    adShowing = true
                    appOpenAd.show(this@SplashScreen)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("TAG", "onAdFailedToLoad: $loadAdError")
                }
            },
        )
    }

    private fun redirectTo() {
        adShowing = true
        val showOnBoarding = sharedPreferences?.getBoolean(
            StringConstant.onBoarding, true
        ) ?: true
        if (showOnBoarding) {
            val newIntent = Intent(this, OnBoarding::class.java)
            startActivity(newIntent)
            finish()
            return
        }
        val privacyShow = sharedPreferences?.getBoolean(
            StringConstant.privacy, true
        ) ?: true
        if (privacyShow) {
            val newIntent = Intent(this, PrivacyScreen::class.java)
            startActivity(newIntent)
            finish()
            return
        }

        if (!AppConstant.isProEnable) {
            val newIntent = Intent(this, ProScreen::class.java)
            newIntent.putExtra("fromPro", false)
            startActivity(newIntent)
            finish()
            return
        }

        val newIntent = Intent(this, HomeScreen::class.java)
        startActivity(newIntent)
        finish()
    }

}
