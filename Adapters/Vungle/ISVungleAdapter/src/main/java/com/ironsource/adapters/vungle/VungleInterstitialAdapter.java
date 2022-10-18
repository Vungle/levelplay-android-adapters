package com.ironsource.adapters.vungle;

import androidx.annotation.NonNull;

import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class VungleInterstitialAdapter implements AdAdapter {

    Map<String, InterstitialSmashListener> placementIdToInterstitialSmashListener = new ConcurrentHashMap<>();
    Map<String, InterstitialAd> placementIdToInterstitialAds = new ConcurrentHashMap<>();

    @Override
    public void onInitializationFailure(String error) {
        for (InterstitialSmashListener listener : placementIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public void onInitializationSuccess() {
        for (InterstitialSmashListener listener : placementIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }
    }

    @Override
    public void releaseMemory() {
        placementIdToInterstitialAds.clear();
        placementIdToInterstitialSmashListener.clear();
    }

    protected boolean isInterstitialAdAvailable(String placementId) {
        InterstitialAd ad = placementIdToInterstitialAds.get(placementId);
        if (ad != null) {
            return VungleAds.isInitialized() && ad.canPlayAd();
        }
        return false;
    }

    public void showInterstitial(String placementId, InterstitialSmashListener listener) {
        // if we can play
        if (isInterstitialAdAvailable(placementId)) {
            // create Vungle play listener
            Objects.requireNonNull(placementIdToInterstitialAds.get(placementId)).play();
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = " + placementId);
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }


    public void loadInterstitial(@NonNull String placementId, InterstitialSmashListener listener, String serverData, AdConfig adConfig) {
        InterstitialAd interstitialAd = new InterstitialAd(placementId, adConfig);
        interstitialAd.setAdListener(new BaseAdListener() {

            @Override
            public void adLoaded(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);
                listener.onInterstitialAdReady();
            }

            @Override
            public void adStart(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onInterstitialAdShowSucceeded();
            }

            @Override
            public void adImpression(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onInterstitialAdOpened();
            }

            @Override
            public void adEnd(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onInterstitialAdClosed();
            }

            @Override
            public void adClick(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onInterstitialAdClicked();
            }

            @Override
            public void onAdLeftApplication(@NonNull BaseAd baseAd) {
                // no-op
            }

            @Override
            public void error(@NonNull BaseAd baseAd, @NonNull VungleException adError) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId + ", exception = " + adError);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                String errorMessage = " reason = " + adError.getLocalizedMessage() + " errorCode = " + adError.getExceptionCode();
                listener.onInterstitialAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, errorMessage));
            }

        });
        interstitialAd.load(serverData);
        placementIdToInterstitialAds.put(placementId, interstitialAd);
        placementIdToInterstitialSmashListener.put(placementId, listener);
    }
}
