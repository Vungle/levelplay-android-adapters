package com.ironsource.adapters.vungle;

import static com.ironsource.adapters.vungle.Constants.APP_ID;
import static com.ironsource.adapters.vungle.Constants.PLACEMENT_ID;

import android.app.Activity;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.vungle.ads.AdConfig;
import com.vungle.ads.AdSize;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerView;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class VungleBannerAdapter implements AdAdapter {

    private final String providerName;

    private ConcurrentHashMap<String, BannerSmashListener> bannerSmashListeners = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, BannerAd> bannerViews = new ConcurrentHashMap<>();

    private ISBannerSize currentBannerSize = null;
    private final Activity activity = ContextProvider.getInstance().getCurrentActiveActivity();

    public VungleBannerAdapter(@NonNull String providerName) {
        this.providerName = providerName;
    }


    public void loadBanner(String placementId,
                           IronSourceBannerLayout banner,
                           BannerSmashListener listener,
                           @Nullable String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        currentBannerSize = banner.getSize();
        AdSize bannerSize = getBannerSize(currentBannerSize);

        IronLog.ADAPTER_API.verbose("bannerSize = " + bannerSize);

        if (bannerSize == null) {
            IronLog.ADAPTER_API.verbose("size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(providerName));
            return;
        }

        AdConfig adConfig = new AdConfig();
        adConfig.setAdSize(bannerSize);

        BannerAd bannerAd = new BannerAd(banner.getContext(), placementId, adConfig);
        BaseAdListener adListener = new BaseAdListener() {

            @Override
            public void error(@NonNull BaseAd baseAd, @NonNull VungleException adError) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId + ", exception = " + adError);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                IronSourceError error;
                if (adError.getExceptionCode() == VungleException.NO_SERVE) {
                    error = new IronSourceError(IronSourceError.ERROR_BN_LOAD_NO_FILL, adError.getLocalizedMessage());
                } else {
                    error = ErrorBuilder.buildLoadFailedError(adError.getLocalizedMessage());
                }

                listener.onBannerAdLoadFailed(error);
            }

            @Override
            public void onAdLeftApplication(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onBannerAdLeftApplication();
            }

            @Override
            public void adClick(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onBannerAdClicked();
            }

            @Override
            public void adEnd(@NonNull BaseAd baseAd) {
                // no-op
            }

            @Override
            public void adImpression(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                listener.onBannerAdShown();
            }

            @Override
            public void adStart(@NonNull BaseAd baseAd) {
                // no-op
            }

            @Override
            public void adLoaded(@NonNull BaseAd baseAd) {
                IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

                if (listener == null) {
                    IronLog.INTERNAL.verbose("listener is null");
                    return;
                }

                if (currentBannerSize == null) {
                    IronLog.INTERNAL.verbose("banner size is null");
                    return;
                }

                if (!isBannerAdAvailable(placementId)) {
                    IronLog.ADAPTER_CALLBACK.error("can't play ad");
                    listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("can't play ad"));
                    return;
                }
                BannerView bannerView = bannerAd.getBannerView();

                if (bannerView != null) {
                    listener.onBannerAdLoaded(bannerAd.getBannerView(), getBannerLayoutParams(currentBannerSize));
                } else {
                    IronLog.ADAPTER_CALLBACK.error("banner view is null");
                    listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(providerName + " LoadBanner failed - banner view is null"));
                }
            }
        };
        bannerAd.setAdListener(adListener);
        bannerAd.load(serverData);

        bannerViews.put(placementId, bannerAd);
        bannerSmashListeners.put(placementId, listener);
    }

    @Override
    public void onInitializationSuccess() {
        for (BannerSmashListener listener : bannerSmashListeners.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onInitializationFailure(String error) {
        for (BannerSmashListener listener : bannerSmashListeners.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    protected AdSize getBannerSize(ISBannerSize size) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                return AdSize.BANNER;
            case "RECTANGLE":
                return AdSize.VUNGLE_MREC;
            case "SMART":
                return AdapterUtils.isLargeScreen(activity) ? AdSize.BANNER_LEADERBOARD : AdSize.BANNER;
        }
        return null;
    }

    protected FrameLayout.LayoutParams getBannerLayoutParams(ISBannerSize size) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, 0);

        Activity activity = ContextProvider.getInstance().getCurrentActiveActivity();

        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                break;
            case "RECTANGLE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 300), AdapterUtils.dpToPixels(activity, 250));
                break;
            case "SMART":
                if (AdapterUtils.isLargeScreen(activity)) {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 728), AdapterUtils.dpToPixels(activity, 90));
                } else {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                }

                break;
        }

        layoutParams.gravity = Gravity.CENTER;

        return layoutParams;
    }

    protected boolean isBannerAdAvailable(String placementId) {
        BannerAd ad = bannerViews.get(placementId);
        if (ad != null) {
            return VungleAds.isInitialized() && ad.canPlayAd();
        }
        return false;
    }

    public void releaseMemory() {
        for (BannerAd bannerAd : bannerViews.values()) {
            bannerAd.finishAd();
        }
        bannerViews.clear();
        bannerSmashListeners.clear();
        currentBannerSize = null;
    }

    public void initBanners(String appId, String placementId, BannerSmashListener listener) {
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + PLACEMENT_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        bannerSmashListeners.put(placementId, listener);
    }

    public void destroyBanner(String placementId) {
        IronLog.ADAPTER_API.verbose("Destroy Banner: placementId = " + placementId);
        BannerAd bannerAd = bannerViews.get(placementId);
        if (bannerAd != null) {
            bannerAd.finishAd();
            bannerViews.remove(placementId);
        }
        currentBannerSize = null;
    }
}
