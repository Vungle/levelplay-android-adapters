package com.ironsource.adapters.admob;

import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class AdMobRewardedVideoAdLoadListener extends RewardedAdLoadCallback {

    // data
    private String mAdUnitId;
    private WeakReference<AdMobAdapter> mAdapter;
    private RewardedVideoSmashListener mListener;

    AdMobRewardedVideoAdLoadListener(AdMobAdapter adapter, String adUnitId, RewardedVideoSmashListener listener) {
        mAdapter = new WeakReference<>(adapter);
        mAdUnitId = adUnitId;
        mListener = listener;
    }

    @Override
    public void onAdLoaded(@NotNull RewardedAd rewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().mAdIdToRewardedVideoAd.put(mAdUnitId, rewardedAd);
        mAdapter.get().mRewardedVideoAdsAvailability.put(mAdUnitId, true);

        mListener.onRewardedVideoAvailabilityChanged(true);


    }

    @Override
    public void onAdFailedToLoad(@NotNull LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        int errorCode;
        String adapterError;

        errorCode = loadAdError.getCode();
        adapterError = loadAdError.getMessage() + "( " + errorCode + " )";

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);

        if (mAdapter.get().isNoFillError(errorCode)) {
            errorCode = IronSourceError.ERROR_RV_LOAD_NO_FILL;
            adapterError = "No Fill";
        }

        if (loadAdError.getCause() != null) {
            adapterError = adapterError + "Caused by " + loadAdError.getCause();
        }

        IronLog.ADAPTER_CALLBACK.error("adapterError = " + adapterError);


        mListener.onRewardedVideoAvailabilityChanged(false);
        mListener.onRewardedVideoLoadFailed(new IronSourceError(errorCode, adapterError));


    }
}


