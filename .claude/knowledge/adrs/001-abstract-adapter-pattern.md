# ADR-001: AbstractAdapter Base Class Pattern

## Status
Accepted

## Context
Unity LevelPlay (ironSource) mediates ads from 10 network SDKs on Android. Each adapter must handle SDK initialization, ad format registration, and lifecycle management. Significant boilerplate is shared across all adapters.

## Decision
All network adapters extend the `AbstractAdapter` base class provided by the ironSource SDK:
- `AbstractAdapter` provides shared initialization lifecycle and format registration
- Each adapter implements `INetworkInitCallbackListener` for initialization callbacks
- The adapter class (`IS{Network}Adapter`) overrides methods for each supported ad format
- Initialization, ad loading, and ad display follow the template method pattern

## Consequences
- Consistent adapter structure across all 10 networks
- Shared initialization and lifecycle logic reduces duplication
- Adapters focus on network-specific integration code
- AbstractAdapter updates (from ironSource SDK) affect all adapters
- Must keep adapter implementations compatible with the base class contract
- New ad formats require AbstractAdapter support before adapters can implement them
