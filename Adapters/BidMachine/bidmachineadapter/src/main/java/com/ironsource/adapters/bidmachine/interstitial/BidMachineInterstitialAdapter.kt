package com.ironsource.adapters.bidmachine.interstitial

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import io.bidmachine.AdPlacementConfig
import io.bidmachine.interstitial.InterstitialAd
import io.bidmachine.interstitial.InterstitialRequest

class BidMachineInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<BidMachineAdapter>(networkSettings) {

    private var interstitialAd: InterstitialAd? = null

    // region LevelPlay Interstitial API

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: InterstitialAdListener
    ) {
        val placementId = adData.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        interstitialAd = InterstitialAd(context.applicationContext).apply {
            setListener(BidMachineInterstitialListener(listener))
        }

        val adPlacementConfig = createInterstitialPlacementConfig(placementId)
        val interstitialRequest = InterstitialRequest.Builder(adPlacementConfig)
            .setBidPayload(adData.serverData)
            .build()

        interstitialAd?.load(interstitialRequest)
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: InterstitialAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.ADAPTER_API.error(BidMachineConstants.AD_NOT_READY)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                BidMachineConstants.AD_NOT_READY
            )
            return
        }

        interstitialAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return interstitialAd?.let { ad ->
            ad.canShow() && !ad.isExpired
        } ?: false
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        interstitialAd?.setListener(null)
        interstitialAd?.destroy()
        interstitialAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val placementId = adData?.getString(BidMachineConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        val adPlacementConfig = createInterstitialPlacementConfig(placementId)
        networkAdapter.collectBiddingData(context, biddingDataCallback, adPlacementConfig)
    }

    // endregion

    // region Helper Methods

    private fun createInterstitialPlacementConfig(placementId: String?): AdPlacementConfig {
        val adPlacementConfigBuilder = AdPlacementConfig.interstitialBuilder()
        if (!placementId.isNullOrEmpty()) {
            adPlacementConfigBuilder.withPlacementId(placementId)
        }
        return adPlacementConfigBuilder.build()
    }

    // endregion
}
