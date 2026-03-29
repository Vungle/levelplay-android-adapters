# ADR-002: Separate Load and Play Listeners Per Format

## Status
Accepted

## Context
Each ad format (Rewarded Video, Interstitial, Banner) has distinct lifecycle events. Some events occur during ad loading (success, failure) while others occur during ad playback/display (impression, click, close, reward). Combining these in the adapter class creates large, unwieldy files.

## Decision
Each ad format has two separate listener classes:
- **Load Listener** (`IS{Network}{Format}LoadListener`): Handles ad load success and failure callbacks
- **Play/Show Listener** (`IS{Network}{Format}Listener`): Handles ad display events (show, click, close, reward)

These listener classes are instantiated by the adapter and registered with the network SDK for their respective lifecycle phases.

## Consequences
- Clean separation between loading and display concerns
- Each listener class has a focused, single responsibility
- Adapter class orchestrates listeners but delegates callback handling
- More files per adapter (2 listeners x 3 formats = 6 listener classes)
- Listeners must hold references to the adapter or mediation callback interfaces
- Easier to test individual lifecycle phases in isolation
