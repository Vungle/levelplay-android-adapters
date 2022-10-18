package com.ironsource.adapters.vungle;

import static com.ironsource.adapters.vungle.Constants.APP_ID;
import static com.ironsource.adapters.vungle.Constants.PLACEMENT_ID;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;

import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class VungleRewardedVideoAdapter implements AdAdapter {
    private final ConcurrentHashMap<String, RewardedVideoSmashListener> rewardedVideoSmashListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RewardedAd> rewardedAds = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<String> rewardedVideoPlacementIdsForInitCallbacks = new CopyOnWriteArraySet<>();

    public void onInitializationSuccess() {
        for (String placementId : rewardedVideoSmashListeners.keySet()) {
            RewardedVideoSmashListener listener = rewardedVideoSmashListeners.get(placementId);

            if (rewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                Objects.requireNonNull(listener).onRewardedVideoInitSuccess();
            } else {
                loadRewardedAd(placementId, listener, null, new AdConfig());
            }
        }
    }

    @Override
    public void onInitializationFailure(String error) {
        for (String placementId : rewardedVideoSmashListeners.keySet()) {
            RewardedVideoSmashListener listener = Objects.requireNonNull(rewardedVideoSmashListeners.get(placementId));

            if (rewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }
    }

    @Override
    public void releaseMemory() {
        rewardedVideoSmashListeners.clear();
        rewardedVideoPlacementIdsForInitCallbacks.clear();
        rewardedAds.clear();
    }

    protected boolean isRewardedVideoAdAvailable(String placementId) {
        RewardedAd ad = rewardedAds.get(placementId);
        if (ad != null) {
            return VungleAds.isInitialized() && ad.canPlayAd();
        }
        return false;
    }

    public void initRewardedAd(String placementId, String appId, RewardedVideoSmashListener listener) {
        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + PLACEMENT_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        //add to rewarded video listener map
        rewardedVideoSmashListeners.put(placementId, listener);
        rewardedVideoPlacementIdsForInitCallbacks.add(placementId);
    }

    public void loadRewardedAd(String placementId, RewardedVideoSmashListener listener, String serverData, AdConfig adConfig) {
        RewardedAd rewardedAd = new RewardedAd(placementId, adConfig);
        rewardedAd.setAdListener(new RewardedAdListener() {

            @Override
            public void adRewarded(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);
                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAdRewarded();
            }

            @Override
            public void adLoaded(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAvailabilityChanged(true);
            }

            @Override
            public void adStart(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAdStarted();
            }

            @Override
            public void adImpression(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAdOpened();
            }

            @Override
            public void adEnd(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAdEnded();
                listener.onRewardedVideoAdClosed();
            }

            @Override
            public void adClick(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAdClicked();
            }

            @Override
            public void onAdLeftApplication(@NonNull BaseAd baseAd) {
                // no-op
            }

            @Override
            public void error(@NonNull BaseAd baseAd, @NonNull VungleException adError) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId + ", adError = " + adError);
                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, adError.getLocalizedMessage()));
            }
        });
        rewardedAd.load(serverData);
        rewardedVideoSmashListeners.put(placementId, listener);
        rewardedAds.put(placementId, rewardedAd);
    }

    public void fetchRewardedVideoForAutomaticLoad(JSONObject config, RewardedVideoSmashListener listener, AdConfig adConfig) {
        String placementId = config.optString(PLACEMENT_ID);
        if (isRewardedVideoAdAvailable(placementId)) {
            IronLog.ADAPTER_API.verbose("ad already cached for placement Id " + placementId);

            RewardedVideoSmashListener smashListener = rewardedVideoSmashListeners.get(placementId);
            Objects.requireNonNull(smashListener).onRewardedVideoAvailabilityChanged(true);

        } else {
            loadRewardedAd(placementId, listener, null, adConfig);
        }
    }

    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // change rewarded video availability to false
        listener.onRewardedVideoAvailabilityChanged(false);

        if (isRewardedVideoAdAvailable(placementId)) {
            // TODO: Check if needed
//            if (!TextUtils.isEmpty(getDynamicUserId())) {
//                VungleAds.setIncentivizedFields(getDynamicUserId(), null, null, null, null);
//            }
            Objects.requireNonNull(rewardedAds.get(placementId)).play();
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = " + placementId);
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }


    public void initAndLoadRewardedAd(String placementId, String appId, RewardedVideoSmashListener listener) {
        rewardedVideoSmashListeners.put(placementId, listener);
    }
}
