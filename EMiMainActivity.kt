package com.example.financialcalc.Activitys

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.financialcalc.Ads.AdConstant
import com.example.financialcalc.Ads.AdLoad
import com.example.financialcalc.MainActivity
import com.example.financialcalc.Utils.AppConstant
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Objects


class SplashScreen : AppCompatActivity() {
    private var sharedPreferences: SharedPreferences? = null
    private var firebaseDatabase: FirebaseDatabase? = null
    private var mDatabase: DatabaseReference? = null
    private var versionString: String = ""
    private var consentInformation: ConsentInformation? = null

    var adShowing: Boolean = false
    fun initializeView() {
        firebaseDatabase = FirebaseDatabase.getInstance(AppConstant.FIREBASE_LINK)
        mDatabase = firebaseDatabase?.getReference("version")
        mDatabase?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                versionString = Objects.requireNonNull(dataSnapshot.getValue()).toString()
                Log.d("SplashScreen", "versionString: $versionString")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("SplashScreen", "Cancelled version calling....")
            }
        })
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            this,
            { consentForm: ConsentForm ->
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
            },
            { formError: FormError? -> }
        )
    }

    private fun fetchFirebaseRemoteConfig() {
        FirebaseApp.initializeApp(this@SplashScreen)
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(10)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener(
                this@SplashScreen,
                OnCompleteListener<Boolean?> { task ->
                    if (task.isSuccessful) {

                        AdConstant.showAd = mFirebaseRemoteConfig.getString("showAd") == "1"
                        AdConstant.showAppOpen =
                            mFirebaseRemoteConfig.getString("showAppOpen") == "1"
                        AdConstant.appOpen = mFirebaseRemoteConfig.getString("initialAppOpen")
                        AdConstant.onBoardingNative =
                            mFirebaseRemoteConfig.getString("onBoardingNative")
                        AppConstant.privacyLink =
                            mFirebaseRemoteConfig.getString("privacyPolicy")
                        AdConstant.homeBanner = mFirebaseRemoteConfig.getString("homeBanner")
                        AdConstant.interstitialAdCount =
                            mFirebaseRemoteConfig.getLong("interstitialCount").toInt()
                        AdConstant.interstitialAdId =
                            mFirebaseRemoteConfig.getString("interstitialAd")

                        AdConstant.calculatorNativeAd =
                            mFirebaseRemoteConfig.getString("calculatorNative")
                        AdConstant.rewardAdId = mFirebaseRemoteConfig.getString("rewardAd")




                        if (AdConstant.showAd) {
                            val showOnBoarding = sharedPreferences?.getBoolean(
                                AppConstant.OnBoardingStatus,
                                true
                            ) ?: true
                            if (showOnBoarding) {
                                AdLoad.onBoardingNative(
                                    AdConstant.onBoardingNative,
                                    this@SplashScreen
                                )
                            }
                            if (AdConstant.showAppOpen) {
                                if (AdConstant.appOpen.isNotEmpty()) {
                                    loadAppOpen(AdConstant.appOpen)
                                } else {
                                    redirectTo()
                                }
                            } else {
                                redirectTo()
                            }
                        } else {
                            redirectTo()
                        }


                    } else {
                        redirectTo()
                    }
                },
            )
    }

    private fun redirectTo() {
        val showOnBoarding = sharedPreferences?.getBoolean(
            AppConstant.OnBoardingStatus,
            true
        ) ?: true
        if (showOnBoarding) {
            val newIntent =
                Intent(this, OnBoardingScreen::class.java)
            startActivity(newIntent)
            finish()
            return
        }
        val privacyShow = sharedPreferences?.getBoolean(
            AppConstant.onPrivacyStatus,
            true
        ) ?: true
        if (privacyShow) {
            val newIntent = Intent(this, PrivacyScreen::class.java)
            startActivity(newIntent)
            finish()
            return
        }

        val permissionShow = sharedPreferences?.getBoolean(
            AppConstant.onPermissionStatus,
            true
        ) ?: true
        if (permissionShow) {
            val newIntent =
                Intent(this, PermissionScreen::class.java)
            startActivity(newIntent)
            finish()
            return
        }

        if (!AppConstant.PURCHASE_STATUS) {
            val newIntent = Intent(this, ProScreen::class.java)
            newIntent.putExtra("fromPro", false)
            startActivity(newIntent)
            finish()
            return
        }

        val newIntent = Intent(this, MainActivity::class.java)
        startActivity(newIntent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.financialcalc.R.layout.activity_splash_screen)

        val layout = findViewById<ConstraintLayout>(com.example.financialcalc.R.id.layout)
        sharedPreferences =
            applicationContext.getSharedPreferences(AppConstant.PACKAGE_NAME, MODE_PRIVATE)

        initializeView()
        AppConstant.PURCHASE_STATUS = sharedPreferences?.getBoolean("PURCHASE", false) ?: false


        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@SplashScreen) {

            }
        }
        val params =
            ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation?.requestConsentInfoUpdate(
            this,
            params,
            {
                if (consentInformation?.isConsentFormAvailable!!) {
                    loadForm()
                }
            },
            { formError -> }
        )
        fetchFirebaseRemoteConfig()

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            if (!adShowing) {
                redirectTo()
            }
        }, 13000)

        if (sharedPreferences?.getString(AppConstant.CURRENCY, "null") != "null") {
            AppConstant.CURRENCY_SELECTED = sharedPreferences?.getString(AppConstant.CURRENCY, null)
                ?: AppConstant.CURRENCY_SELECTED
        }
    }

    private fun loadAppOpen(id: String) {
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            this, id, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                        override fun onAdDismissedFullScreenContent() {
                            redirectTo()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            redirectTo()
                        }

                        override fun onAdShowedFullScreenContent() {
                        }
                    }
                    adShowing = true
                    appOpenAd.show(this@SplashScreen)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {

                }
            },
        )
    }


}
