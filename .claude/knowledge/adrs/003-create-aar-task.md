# ADR-003: Custom Gradle createAAR Build Task

## Status
Accepted

## Context
Adapter libraries need to be distributed as AAR (Android Archive) files for integration by publishers. The standard Gradle `assembleRelease` task produces AARs in individual module build directories, but a centralized output location simplifies release management and distribution.

## Decision
A custom Gradle task `createAAR` is defined that:
1. Builds all adapter modules in release configuration
2. Copies the resulting AAR files to a centralized `ReleaseCandidates/` directory
3. Names AAR files with adapter name and version for clarity

The build command is: `./gradlew createAAR`

## Consequences
- Single command builds all adapters for release
- Centralized output directory simplifies artifact collection
- AAR naming convention makes version identification easy
- ReleaseCandidates/ directory serves as the staging area for distribution
- Custom task must be maintained as adapters are added or removed
- Developers use this task instead of standard `assembleRelease` for releases
