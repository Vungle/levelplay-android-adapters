package com.vungle.ironsource.app


import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.integration.IntegrationHelper
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerListener
import com.vungle.ironsource.app.databinding.ActivityDemoMrecBinding

class DemoMrecActivity : Activity() {

    companion object {
        const val TAG = "DemoMrecActivity"
    }

    private lateinit var binding: ActivityDemoMrecBinding

    private var mIronSourceMrecLayout: IronSourceBannerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoMrecBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //The integrationHelper is used to validate the integration. Remove the integrationHelper before going live!
        IntegrationHelper.validateIntegration(this)
        initUIElements()

        val appKey = intent.getStringExtra("APP_KEY")
        binding.appKeyTV.text = "appKey: $appKey"

        val advertisingId = IronSource.getAdvertiserId(this@DemoMrecActivity)
        // we're using an advertisingId as the 'userId'
        initIronSource(advertisingId, appKey!!)

        //Network Connectivity Status
        IronSource.shouldTrackNetworkState(this, true)
    }

    private fun initIronSource(userId: String?, appKey: String) {
        // set the IronSource user id
        IronSource.setUserId(userId)
        // init the IronSource SDK
        IronSource.init(this, appKey)
    }

    override fun onResume() {
        super.onResume()
        // call the IronSource onResume method
        IronSource.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        // call the IronSource onPause method
        IronSource.onPause(this)
    }

    /**
     * initialize the UI elements of the activity
     */
    private fun initUIElements() {
        binding.isLoadMrecButton.setOnClickListener {
            createAndloadBanner()
        }
        binding.isCloseMrecButton.setOnClickListener {
            mIronSourceMrecLayout?.let {
                IronSource.destroyBanner(it)
            }
        }
    }

    /**
     * Creates and loads IronSource Banner
     *
     */
    private fun createAndloadBanner() {
        // instantiate IronSourceBanner object, using the IronSource.createBanner API
        mIronSourceMrecLayout = IronSource.createBanner(this, ISBannerSize.RECTANGLE)

        mIronSourceMrecLayout?.let {
            // add IronSourceBanner to your container
            val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            )

            binding.bannerFooter.addView(it, 0, layoutParams)

            // set the banner listener
            it.bannerListener = object : BannerListener {
                override fun onBannerAdLoaded() {
                    Log.d(TAG, "onBannerAdLoaded")
                    // since banner container was "gone" by default, we need to make it visible as soon as the banner is ready
                    binding.bannerFooter.visibility = View.VISIBLE
                }

                override fun onBannerAdLoadFailed(error: IronSourceError) {
                    Log.d(TAG, "onBannerAdLoadFailed $error")
                }

                override fun onBannerAdClicked() {
                    Log.d(TAG, "onBannerAdClicked")
                }

                override fun onBannerAdScreenPresented() {
                    Log.d(TAG, "onBannerAdScreenPresented")
                }

                override fun onBannerAdScreenDismissed() {
                    Log.d(TAG, "onBannerAdScreenDismissed")
                }

                override fun onBannerAdLeftApplication() {
                    Log.d(TAG, "onBannerAdLeftApplication")
                }
            }

            // load ad into the created banner
            IronSource.loadBanner(it)
        } ?: run {
            Toast.makeText(
                    this@DemoMrecActivity,
                    "IronSource.createBanner returned null",
                    Toast.LENGTH_LONG
            ).show()

        }
    }

}
