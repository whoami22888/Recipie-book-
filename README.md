# Beyond Human Kitchen — v0.4

Production-oriented Android + backend rebuild using the supplied 424-recipe cookbook as the authoritative catalogue.

## Verification gates
- 424/424 recipe invariant
- Android lint/test/build in CI
- Backend unit/integration tests
- No API secrets in the APK
- Source section/category data preserved
- Multi-ingredient search
- Share/import confirmation before persistence
- Vision output remains user-confirmed before recipe matching

The supplied cookbook remains the content source of truth. Questionable or generic source records are flagged, not silently rewritten.

## Current verification
Local backend: PASS — 6 tests.
Catalogue invariant: PASS — 424/424.
Android compilation: UNVERIFIED locally because the current build environment has no Gradle executable; CI is required for this gate.
