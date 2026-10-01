# Step 4 — Product Flow Audit

## Baseline

- Verified baseline commit: `985526993b6cc046d3fe455cfc1e985c5b36fa3b`
- Branch under audit: `orchestrator/v0.5-step4-product-flows`
- Previous CI evidence: workflow run 49 completed successfully.
- Step 1 frozen baseline remains separate and unchanged.

## Source-of-truth content

The supplied cookbook states that it contains all 424 recipes, with a shopping list and method for each recipe. The catalogue currently preserves recipe number, title, description, source section, shopping list, method, source pages and quality flags.

The source itself contains records where the original saved material is incomplete or not actually a conventional recipe. Examples include recipe 420 being a caravan listing and recipe 424 being a community meal-service notice. These must remain flagged/source-faithful rather than silently rewritten.

## Requirements audit

| Requirement | Current state | Classification | Step 4 action |
|---|---|---|---|
| All 424 recipes available offline | Implemented and verified | PASS | Freeze |
| Search by recipe name | Implemented | PASS | Freeze |
| Search by ingredients | Implemented | PASS | Freeze |
| Search by category/section | Section participates in search | PARTIAL | Add explicit category browsing/filter |
| Recipe detail screen | Implemented and instrumented | PASS | Freeze |
| Android text sharing | Implemented and tested | PASS | Freeze |
| Android image sharing | URI received only | GAP | Connect image analysis workflow |
| Camera photo capture | Camera dependencies/permission exist, no user workflow | GAP | Implement capture flow |
| Fridge/cupboard/both workflow | Not implemented | GAP | Implement staged capture + confirmation |
| Ingredient recognition | ML Kit dependency exists, no pipeline | GAP | Implement real label extraction with user confirmation |
| Match recognised ingredients to recipes | Not implemented | GAP | Feed confirmed ingredients into existing repository search |
| AI food-selection chatbot | Backend endpoint exists, Android client/UI absent | GAP | Add Android API client + chat UI |
| URL recipe import | Backend endpoint exists, Android client/UI absent | GAP | Add Android import flow |
| Shared text/image recipe import confirmation | Confirmation exists only as backend response contract | PARTIAL | Add Android confirmation UI |
| No API secret in APK | Backend architecture supports this | PASS | Preserve |
| Offline catalogue operation | Implemented | PASS | Preserve |
| Startup failure handling | Implemented/tested | PASS | Preserve |
| SSRF protection for URL import | Implemented/tested | PASS | Preserve |
| Source quality flags preserved | Implemented/tested | PASS | Preserve |
| Production-ready AI configuration | Backend requires explicit API key/model | PARTIAL | Add deployment/configuration contract; do not embed secrets |
| Persistent database migration strategy | Destructive fallback currently configured | GAP | Replace before user-data persistence is introduced |

## Implementation order

### 4A — Local product flows
1. Explicit category browsing/filter.
2. Camera capture for fridge/cupboard/both.
3. Shared-image analysis.
4. Ingredient candidate confirmation/editing.
5. Confirmed ingredients -> existing recipe search.
6. Instrumented tests for the complete local flow.

### 4B — Network product flows
1. Android API client with configurable backend base URL.
2. AI chat screen.
3. URL import screen.
4. Draft/provenance confirmation before persistence.
5. Network/error/offline tests.

### 4C — Production hardening
1. Replace destructive Room migration before introducing user-owned data.
2. Add configuration/secrets/deployment validation.
3. Audit permissions and privacy behavior.
4. Full regression CI and final Step 4 gate audit.

## Stop conditions

- Do not modify code already verified unless a new failure demonstrates a regression or dependency.
- Do not treat ML Kit generic image labels as authoritative ingredient identification; all detected ingredients require user confirmation.
- Do not claim AI or URL import is complete until the Android client and UI are tested end-to-end against the backend contract.
- Do not alter cookbook records merely to make them look more recipe-like; preserve source content and quality flags.

## Step 4 acceptance target

Step 4 is complete only when the Android app exposes the required local and network product flows, the flows have automated coverage, the existing 424-recipe invariant remains green, and the full CI suite passes without regressions.
