# LevelPlay (ironSource) — Vungle Android Adapter Class Index

## Adapter Entry Point

| Class | Superclass | Package |
|-------|-----------|---------|
| `VungleAdapter` | `AbstractAdapter` | `com.ironsource.adapters.vungle` |

**Initialization**: `init(activity, appKey, userId)` → calls `VungleAds.init(context, appKey)`
**Network Key**: `"Vungle"` (registered in ironSource mediation)

## Format Classes

| Format | Class | Listener Interface |
|--------|-------|--------------------|
| Interstitial | `VungleInterstitialAdapter` | `InterstitialAdListener` |
| Rewarded | `VungleRewardedVideoAdapter` | `RewardedAdListener` |
| Banner | `VungleBannerAdapter` | `BannerAdListener` |

## Listener Classes (6 total)

| Listener | Delegates For | Key Callbacks |
|----------|--------------|---------------|
| `VungleInterstitialAdListener` | Interstitial load | `onAdLoaded`, `onAdFailedToLoad` |
| `VungleInterstitialAdShowListener` | Interstitial show | `onAdStart`, `onAdEnd`, `onAdClicked`, `onAdImpression` |
| `VungleRewardedAdListener` | Rewarded load | `onAdLoaded`, `onAdFailedToLoad` |
| `VungleRewardedAdShowListener` | Rewarded show | `onAdStart`, `onAdEnd`, `onAdClicked`, `onAdRewarded` |
| `VungleBannerAdListener` | Banner load | `onAdLoaded`, `onAdFailedToLoad` |
| `VungleBannerAdShowListener` | Banner show | `onAdImpression`, `onAdClicked` |

## Callback Mapping (Vungle → LevelPlay)

| Vungle Callback | LevelPlay Callback | Context |
|----------------|-------------------|---------|
| `onAdLoaded(ad)` | `onAdOpened(adInfo)` / `onAdReady()` | Load success |
| `onAdFailedToLoad(ad, error)` | `onAdLoadFailed(error)` | Load failure |
| `onAdStart(ad)` | `onAdShowSucceeded(adInfo)` | Fullscreen shown |
| `onAdImpression(ad)` | `onAdVisible(adInfo)` | Impression tracked |
| `onAdClicked(ad)` | `onAdClicked(adInfo)` | Click |
| `onAdEnd(ad)` | `onAdClosed(adInfo)` | Fullscreen closed |
| `onAdRewarded(ad)` | `onAdRewarded(adInfo)` | Reward granted |
| `onAdFailedToPlay(ad, error)` | `onAdShowFailed(error)` | Show failure |

## Bidding Support

- Implements `INetworkBiddingProvider` interface
- `collectBiddingData(context, params)` → calls `VungleAds.getBiddingToken(context)`
- Token passed to ironSource auction, bid payload returned via `serverData`

## Key Patterns

1. **Separate load/show listeners**: Each format has distinct listener for load vs show phase
2. **AdInfo wrapping**: Vungle ad objects wrapped into ironSource `AdInfo` for callback forwarding
3. **Singleton VungleAds**: Single initialization, placement IDs from server data
4. **Banner size mapping**: ironSource `ISBannerSize` → Vungle `BannerAdSize` (BANNER, MREC, LEADERBOARD)
5. **Privacy**: GDPR consent, CCPA via `VunglePrivacySettings`, COPPA via `setCoppa()`
