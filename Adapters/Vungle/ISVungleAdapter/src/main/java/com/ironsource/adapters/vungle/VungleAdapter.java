package com.ironsource.adapters.vungle;

import static com.ironsource.adapters.vungle.Constants.APP_ID;
import static com.ironsource.adapters.vungle.Constants.PLACEMENT_ID;
import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import com.vungle.ads.AdConfig;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.Plugin;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.ads.VungleSettings;
import com.vungle.ads.internal.network.VungleApiClient;
import com.vungle.ads.internal.privacy.PrivacyConsent;
import com.vungle.ads.internal.privacy.PrivacyManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class VungleAdapter extends AbstractAdapter implements INetworkInitCallbackListener {

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // Meta data flags
    private static final String VUNGLE_COPPA_FLAG = "vungle_coppa";
    private static final String ORIENTATION_FLAG = "vungle_adorientation";

    // Vungle Constants
    private static final String CONSENT_MESSAGE_VERSION = "1.0.0";
    private static final String ORIENTATION_PORTRAIT = "PORTRAIT";
    private static final String ORIENTATION_LANDSCAPE = "LANDSCAPE";
    private static final String ORIENTATION_AUTO_ROTATE = "AUTO_ROTATE";

    private static final String LWS_SUPPORT_STATE = "isSupportedLWSByInstance";

    // members for network
    private static Boolean mConsent = null;
    private static Boolean mCCPA = null;
    private static String mAdOrientation = null;

    // init state possible values
    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    VungleBannerAdapter bannerAdapter = new VungleBannerAdapter(getProviderName());
    VungleInterstitialAdapter interstitialAdapter = new VungleInterstitialAdapter();
    VungleRewardedVideoAdapter rewardedVideoAdapter = new VungleRewardedVideoAdapter();

    // Handle init callback for all adapter instances
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    private static InitState mInitState = InitState.INIT_STATE_NONE;

    //region Adapter Methods
    public static VungleAdapter startAdapter(String providerName) {
        return new VungleAdapter(providerName);
    }

    // o
    private VungleAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose("");
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Activity activity) {
        IntegrationData ret = new IntegrationData("Vungle", VERSION);
        ret.validateWriteExternalStorage = true;
        return ret;
    }

    @Override
    // get adapter version
    public String getVersion() {
        return VERSION;
    }

    @Override
    //get network sdk version
    public String getCoreSDKVersion() {
        return getAdapterSDKVersion();
    }

    public static String getAdapterSDKVersion() {
        return com.vungle.ads.BuildConfig.VERSION_NAME;
    }

    //endregion

    //region Initializations methods and callbacks
    private void initSDK(final String appId) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(VungleAdapter.this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("appId = " + appId);

            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            Plugin.addWrapperInfo(VungleApiClient.WrapperFramework.ironsource, getVersion());

            VungleSettings vungleSettings = new VungleSettings();
            Context context = ContextProvider.getInstance().getApplicationContext();
            VungleAds.init(context, appId, new InitializationListener() {
                @Override
                public void onSuccess() {
                    IronLog.ADAPTER_CALLBACK.verbose("Succeeded to initialize SDK");

                    mInitState = InitState.INIT_STATE_SUCCESS;

                    if (mConsent != null) {
                        setConsent(mConsent);
                    }

                    if (mCCPA != null) {
                        setCCPAValue(mCCPA);
                    }

                    for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                        adapter.onNetworkInitCallbackSuccess();
                    }

                    initCallbackListeners.clear();
                }

                @Override
                public void onError(@NonNull VungleException vungleException) {
                    IronLog.ADAPTER_CALLBACK.verbose("Failed to initialize SDK");

                    mInitState = InitState.INIT_STATE_FAILED;

                    for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                        adapter.onNetworkInitCallbackFailed("Vungle sdk init failed - " +
                                vungleException.getLocalizedMessage());
                    }

                    initCallbackListeners.clear();
                }
            }, vungleSettings);
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        rewardedVideoAdapter.onInitializationSuccess();
        interstitialAdapter.onInitializationSuccess();
        bannerAdapter.onInitializationSuccess();
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        rewardedVideoAdapter.onInitializationFailure(error);
        interstitialAdapter.onInitializationFailure(error);
        bannerAdapter.onInitializationFailure(error);
    }

    @Override
    public void onNetworkInitCallbackLoadSuccess(String placementId) {
    }
    //endregion

    //region Rewarded Video API
    @Override
    // Used for flows when the mediation needs to get a callback for init
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);
        rewardedVideoAdapter.initRewardedAd(placementId, appId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Vungle SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                break;
        }
    }

    @Override
    // used for flows when the mediation doesn't need to get a callback for init
    public void initAndLoadRewardedVideo(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        rewardedVideoAdapter.initAndLoadRewardedAd(placementId, appId, listener);//add to rewarded video listener map

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                if (rewardedVideoAdapter.isRewardedVideoAdAvailable(placementId)) {
                    IronLog.ADAPTER_API.verbose("ad already cached for placement Id " + placementId);
                    listener.onRewardedVideoAvailabilityChanged(true);
                } else {
                    IronLog.ADAPTER_API.verbose("placementId = " + placementId);
                    rewardedVideoAdapter.loadRewardedAd(placementId, listener, null, createAdConfig());
                }
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onRewardedVideoAvailabilityChanged(false);
                break;
        }
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, final RewardedVideoSmashListener listener, String serverData) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        rewardedVideoAdapter.loadRewardedAd(placementId, listener, serverData, createAdConfig());
    }

    @Override
    public void fetchRewardedVideoForAutomaticLoad(final JSONObject config, final RewardedVideoSmashListener listener) {
        rewardedVideoAdapter.fetchRewardedVideoForAutomaticLoad(config, listener, createAdConfig());

    }

    @Override
    public void showRewardedVideo(JSONObject config, final RewardedVideoSmashListener listener) {
        rewardedVideoAdapter.showRewardedVideo(config, listener);

    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        return rewardedVideoAdapter.isRewardedVideoAdAvailable(placementId);

    }

    @Override
    public Map<String, Object> getRewardedVideoBiddingData(JSONObject config) {
        return getBiddingData();
    }

    //endregion

    //region Interstitial API

    @Override
    public void initInterstitialForBidding(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        initInterstitial(appKey, userId, config, listener);
    }

    @Override
    public void initInterstitial(final String appKey, final String userId, final JSONObject config, InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + PLACEMENT_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Vungle SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                break;
        }
    }


    @Override
    public void loadInterstitialForBidding(JSONObject config, InterstitialSmashListener listener, String serverData) {
        final String placementId = config.optString(PLACEMENT_ID);
        loadInterstitialInternal(placementId, listener, serverData);
    }

    @Override
    public void loadInterstitial(JSONObject config, final InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        loadInterstitialInternal(placementId, listener, null);
    }

    private void loadInterstitialInternal(final String placementId, final InterstitialSmashListener listener, String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        interstitialAdapter.loadInterstitial(placementId, listener, serverData, createAdConfig());
    }

    @Override
    public void showInterstitial(JSONObject config, final InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        interstitialAdapter.showInterstitial(placementId, listener);

    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        String placementId = config.optString(PLACEMENT_ID);
        return interstitialAdapter.isInterstitialAdAvailable(placementId);
    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config) {
        return getBiddingData();
    }
    //endregion

    //region Banner API

    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        initBanners(appKey, userId, config, listener);
    }

    @Override
    public void initBanners(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        bannerAdapter.initBanners(appId, placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                listener.onBannerInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Vungle SDK init failed", IronSourceConstants.BANNER_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadBannerForBidding(IronSourceBannerLayout banner, JSONObject config, BannerSmashListener listener, String serverData) {
        final String placementId = config.optString(PLACEMENT_ID);
        bannerAdapter.loadBanner(placementId, banner, listener, serverData);
    }

    @Override
    public void loadBanner(final IronSourceBannerLayout banner, JSONObject config, final BannerSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        bannerAdapter.loadBanner(placementId, banner, listener, null);
    }

    @Override
    public void reloadBanner(final IronSourceBannerLayout banner, final JSONObject config, final BannerSmashListener listener) {
        IronLog.INTERNAL.warning("Unsupported method");
    }

    @Override
    public void destroyBanner(JSONObject config) {
        final String placementId = config.optString(PLACEMENT_ID);
        bannerAdapter.destroyBanner(placementId);
    }

    @Override
    //network does not support banner reload
    //return true if banner view needs to be bound again on reload
    public boolean shouldBindBannerViewOnReload() {
        return true;
    }

    @Override
    public Map<String, Object> getBannerBiddingData(JSONObject config) {
        return getBiddingData();
    }

    //endregion

    // region memory handling
    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            rewardedVideoAdapter.releaseMemory();

        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            interstitialAdapter.releaseMemory();

        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            bannerAdapter.releaseMemory();
        }
    }
    //endregion

    // region progressive loading handling
    @Override
    // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
    public LoadWhileShowSupportState getLoadWhileShowSupportState(JSONObject mAdUnitSettings) {
        LoadWhileShowSupportState loadWhileShowSupportState = mLWSSupportState;

        if (mAdUnitSettings != null) {
            boolean isSupportedLWSByInstance = mAdUnitSettings.optBoolean(LWS_SUPPORT_STATE);

            if (isSupportedLWSByInstance) {
                loadWhileShowSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
            } else {
                loadWhileShowSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK;
            }
        }

        return loadWhileShowSupportState;
    }
    //endregion

    //region legal
    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("consent = " + consent);

        if (mInitState == InitState.INIT_STATE_SUCCESS) {
            PrivacyConsent privacyConsent = consent ? PrivacyConsent.OPT_IN : PrivacyConsent.OPT_OUT;
            VungleAds.updateGDPRConsent(privacyConsent, CONSENT_MESSAGE_VERSION);
        } else {
            mConsent = consent;
        }
    }

    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        // this is a list of 1 value
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value));
        } else if (key.equalsIgnoreCase(ORIENTATION_FLAG)) {
            mAdOrientation = value;
        } else {
            String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);

            if (isValidCOPPAMetaData(key, formattedValue)) {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
            }
        }
    }

    private void setCCPAValue(final boolean ccpa) {
        if (mInitState == InitState.INIT_STATE_SUCCESS) {
            // The Vungle CCPA API expects an indication if the user opts in to targeted advertising.
            // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
            // we will use the opposite value of what is passed to this method
            boolean optIn = !ccpa;
            PrivacyConsent status = optIn ? PrivacyConsent.OPT_IN : PrivacyConsent.OPT_OUT;

            IronLog.ADAPTER_API.verbose("key = Vungle.Consent" + ", value = " + status.name());
            PrivacyManager.INSTANCE.updateCcpaConsent(status);
        } else {
            mCCPA = ccpa;
        }
    }

    private void setCOPPAValue(final boolean isUserCoppa) {
        if (mInitState == InitState.INIT_STATE_NONE) {
            IronLog.ADAPTER_API.verbose("coppa = " + isUserCoppa);
            PrivacyManager.INSTANCE.updateCoppaConsent(isUserCoppa);
        } else {
            IronLog.INTERNAL.verbose("COPPA value can be set only before the initialization of Vungle");
        }
    }

    private boolean isValidCOPPAMetaData(String key, String value) {
        return (key.equalsIgnoreCase(VUNGLE_COPPA_FLAG) && !TextUtils.isEmpty(value));
    }
    //endregion

    //region Helpers
    private Map<String, Object> getBiddingData() {
        if (mInitState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error("Returning null as token since init failed");
            return null;
        }

        String bidderToken = VungleAds.getBiddingToken();
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        String sdkVersion = getCoreSDKVersion();
        IronLog.ADAPTER_API.verbose("sdkVersion = " + sdkVersion);
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("sdkVersion", sdkVersion);
        ret.put("token", returnedToken);
        return ret;
    }

    private AdConfig createAdConfig() {
        AdConfig adconfig = new AdConfig();

        //set orientation configuration
        if (mAdOrientation != null) {
            switch (mAdOrientation) {
                case ORIENTATION_PORTRAIT:
                    adconfig.setAdOrientation(AdConfig.PORTRAIT);
                    break;
                case ORIENTATION_LANDSCAPE:
                    adconfig.setAdOrientation(AdConfig.LANDSCAPE);
                    break;
                case ORIENTATION_AUTO_ROTATE:
                    adconfig.setAdOrientation(AdConfig.AUTO_ROTATE);
                    break;
            }
            IronLog.INTERNAL.verbose("setAdOrientation to " + adconfig.getAdOrientation());
        }

        return adconfig;
    }
    //endregion
}
