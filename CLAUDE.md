# LevelPlay Android Adapters

## Overview
Unity LevelPlay (ironSource) Android mediation adapters. Contains 10 network adapters for serving ads through the ironSource mediation platform.

## Languages
- **Java** (primary, 36 files) — most adapters
- **Kotlin** (8 files) — Pangle and Yahoo adapters only

## Build System
- **Gradle** with Android Gradle Plugin 4.2.0
- Android Library modules
- Custom `createAAR` Gradle task for building release artifacts
- Build output: `./gradlew createAAR` produces AARs in `ReleaseCandidates/` directory

## Architecture
- Directory structure: `Adapters/{Network}/IS{Network}Adapter/`
- All adapters extend `AbstractAdapter` base class
- Implement `INetworkInitCallbackListener` for SDK initialization
- Separate listener classes per ad format (load listener + play listener)

## Ad Formats
- Rewarded Video (all adapters)
- Interstitial (all adapters)
- Banner (all adapters)

## Vungle Adapter
- Adapter version: v4.3.36
- VungleAds SDK: 7.4.0

## Platform Requirements
- Target SDK: 30
- Min SDK: 16
- IronSource SDK (mediationsdk): 7.2.4.1

## Key Conventions
- Adapter naming: `IS{Network}Adapter`
- Listener separation: distinct load and play listener classes per format
- AbstractAdapter provides shared initialization and lifecycle
- AAR artifacts built via custom Gradle task
