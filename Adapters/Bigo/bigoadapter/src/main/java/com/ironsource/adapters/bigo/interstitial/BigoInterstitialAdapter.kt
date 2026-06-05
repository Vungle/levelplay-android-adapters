package com.ironsource.adapters.bigo.interstitial

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.adapters.bigo.BigoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import sg.bigo.ads.api.InterstitialAd
import sg.bigo.ads.api.InterstitialAdLoader
import sg.bigo.ads.api.InterstitialAdRequest
import java.lang.ref.WeakReference

class BigoInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<BigoAdapter>(networkSettings) {

    private var interstitialListener: BigoInterstitialListener? = null
    private var interstitialAd: InterstitialAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
        val slotId = adData.getString(BigoConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = BigoConstants.Logs.SERVER_DATA_EMPTY
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        interstitialListener = BigoInterstitialListener(listener, WeakReference(this))

        val interstitialAdLoader = InterstitialAdLoader.Builder()
            .withAdLoadListener(interstitialListener)
            .withExt(BigoAdapter.getMediationInfo())
            .build()

        val interstitialAdRequest = InterstitialAdRequest.Builder()
            .withBid(serverData)
            .withSlotId(slotId)
            .build()

        interstitialAdLoader.loadAd(interstitialAdRequest)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                BigoConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        interstitialAd?.setAdInteractionListener(interstitialListener)
        interstitialAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return interstitialAd != null && interstitialAd?.isExpired == false
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        interstitialAd?.setAdInteractionListener(null)
        interstitialAd?.destroy()
        interstitialAd = null
        interstitialListener = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            val errorMessage = BigoConstants.Logs.ADAPTER_UNAVAILABLE
            IronLog.INTERNAL.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setInterstitialAd(ad: InterstitialAd) {
        interstitialAd = ad
    }

    // endregion
}
