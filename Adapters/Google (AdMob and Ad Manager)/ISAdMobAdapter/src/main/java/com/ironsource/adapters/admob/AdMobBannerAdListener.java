package com.ironsource.adapters.admob;

import android.view.Gravity;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;

import java.lang.ref.WeakReference;

public class AdMobBannerAdListener extends AdListener {
    // data
    private String mAdUnitId;
    private WeakReference<AdMobAdapter> mAdapter;
    private BannerSmashListener mListener;
    private AdView mAdView;

    AdMobBannerAdListener(AdMobAdapter adapter, BannerSmashListener listener, String adUnitId, AdView adView) {
        mAdapter = new WeakReference<>(adapter);
        mListener = listener;
        mAdUnitId = adUnitId;
        mAdView = adView;
    }

    @Override
    // closed the ad that was open after a click
    public void onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenDismissed();
    }

    @Override
    public void onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }

    @Override
    public void onAdFailedToLoad(LoadAdError loadAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
        String adapterError;
        IronSourceError ironSourceErrorObject;

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        if (loadAdError != null) {
            adapterError = loadAdError.getMessage() + "( " + loadAdError.getCode() + " ) ";

            if (loadAdError.getCause() != null) {
                adapterError = adapterError + " Caused by - " + loadAdError.getCause();
            }
            ironSourceErrorObject = mAdapter.get().isNoFillError(loadAdError.getCode()) ?
                    new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adapterError) :
                    ErrorBuilder.buildLoadFailedError(adapterError);
        } else {
            adapterError = "Banner failed to load (loadAdError is null)";
            ironSourceErrorObject = ErrorBuilder.buildLoadFailedError(adapterError);
        }

        IronLog.ADAPTER_CALLBACK.error(adapterError + adapterError);
        mListener.onBannerAdLoadFailed(ironSourceErrorObject);
    }

    @Override
    //click banner + ad opened
    public void onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdScreenPresented();
    }

    //load success
    @Override
    public void onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdView == null) {
            IronLog.INTERNAL.verbose("adView is null");
            return;
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        mListener.onBannerAdLoaded(mAdView, layoutParams);
        mListener.onBannerAdShown();

    }

    @Override
    //banner shown - removed adShown callback from here because sometimes, on reload, this callback is called before onAdLoaded
    public void onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose("adUnitId = " + mAdUnitId);
    }
}