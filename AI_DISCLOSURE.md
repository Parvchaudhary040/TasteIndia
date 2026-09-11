# AI Assistance Disclosure

## Overview

AI tools were used throughout development as development and review assistants: Claude Code for
implementation, and Gemini for an independent review pass over the finished implementation. The
submitted implementation was reviewed, tested, and adapted before final submission — AI output was
not committed unmodified-and-unchecked at any stage. I remain responsible for the correctness,
accessibility, maintainability, and final runtime behavior of everything submitted, and for being
able to explain and modify any part of it in review.

## Tools Used

### Claude Code

Claude Code (Anthropic's agentic coding CLI) was used across the whole build, phase by phase:

- Project architecture and dependency setup (`data`/`domain`/`presentation`/`navigation`/`di`
  layering, Gradle version catalog, toolchain resolution).
- The TheMealDB API layer (Retrofit interface, DTOs, error mapping, connectivity handling).
- The Indian recipe list, search, filters, and sorting implementation, including the
  Indian-boundary intersection logic in the repository.
- The recipe details screen and ID-only navigation.
- Favourites persistence (Room) and the Favourites screen.
- Error-state, empty-state, and image-failure handling.
- Splash/welcome screen implementation.
- Test writing (unit tests with fixtures/fakes, instrumented Room tests).
- Build/test iteration: running `./gradlew` tasks, reading failures, and fixing them.
- Android emulator interaction through Android MCP (installing/launching the app, capturing
  screenshots, inspecting the on-screen element tree) for manual verification and for producing
  the screenshots now in `docs/screenshots/`.
- A later review-and-fix pass: reading the actual source against an independent Gemini review (see
  below), classifying each finding, and implementing the two that were confirmed valid.
- This disclosure and the README were also drafted by Claude Code, from the actual repository
  state (Gradle files, source, and test output), not from memory of what was intended.

Claude Code did not independently decide what to build or which findings mattered — each phase
was scoped by explicit instruction, and each proposed change was reviewed before being built,
tested, and committed. Where Claude Code made a judgment call inside that scope (e.g., exactly how
to fix a confirmed issue), that is called out below rather than presented as something I
personally hand-wrote.

### Gemini

Gemini was used as an independent second-pass reviewer over the finished implementation — not as
an implementation tool. It produced a numbered list of findings covering:

- Indian-boundary correctness (whether category/ingredient/favourites results could contain
  non-Indian meals).
- The favourites resolution flow (how a saved id is turned back into a displayable recipe).
- State restoration (search/filter/sort/scroll position across navigation and recreation).
- N+1 request risk in detail fetching.
- Error and Room-failure handling.
- Accessibility.
- Dependency usage.
- Test coverage gaps.

Gemini's findings were treated as claims to verify, not instructions to execute. Every finding was
checked against the actual repository (reading the relevant source files and existing tests)
before any code changed. Several findings turned out to be already handled correctly, or not
applicable, and were left alone; only the findings confirmed as real, high-priority issues were
fixed. See *Example: AI Recommendation That Was Reviewed* below for a specific instance of a
finding that was rejected after verification.

### Android MCP

Android MCP (device-automation tooling) was used to interact with a real Pixel 8 emulator (API 37)
running the built debug APK:

- Installing and launching the app after each build.
- Navigating the actual screens (Welcome → Recipes → Details/Favourites, opening the filter
  sheet, toggling favourites) to confirm behavior matched what the code was supposed to do.
- Inspecting the live on-screen element tree to get accurate tap coordinates and to confirm
  content descriptions/labels were actually present at runtime, not just in source.
- Capturing the screenshots in `docs/screenshots/` (Welcome, Recipes, Filters, Details,
  Favourites) used in the README.

This was runtime/UI verification and asset capture, not a substitute for the Gradle-run test
suite described under *Testing and Verification*.

## Human Review and Ownership

Across the build and the later review pass, I:

- Reviewed the code Claude Code produced before it was built, tested, or committed.
- Ran the Gradle build and test tasks after each meaningful change and required a clean result
  before moving on (`assembleDebug`, `testDebugUnitTest`, `compileDebugAndroidTestSources`).
- Inspected actual runtime behavior on the emulator via Android MCP rather than accepting that a
  passing build meant the feature worked.
- Required Gemini's review findings to be independently verified against the real source before
  any of them were acted on, and reviewed the resulting verification table.
- Explicitly scoped which findings to fix ("the valid Critical and Important fixes... do not
  implement recommendations classified as optional/minor unless they are low-risk"), rejecting
  the rest rather than letting the review dictate scope.
- Approved (or would have rejected) the specific architectural decisions used to fix each
  confirmed issue — e.g., adding a cache-only repository method rather than adding an area check
  inside the existing network-fetching method, and catching-and-reporting-unchanged-state rather
  than adding a broader error-handling framework for the Room-failure fix.
- Verified the assignment's stated requirements (Indian-only scope, ID-only navigation, offline
  favourites, deliberate error states, state restoration) against the implementation and tests,
  not just against Claude's or Gemini's descriptions of them.
- Performed final acceptance checks by running the app on-device through Android MCP.
- Remain responsible for the submitted code, including the parts I did not type character-by-
  character myself.

## Example of AI-Assisted Contribution

### Contribution: Stop favourites from resolving non-Indian ids via a live lookup (commit `43df251`, `fix: stop favourites from resolving non-Indian ids via live lookup`)

**What AI assisted with:** Claude Code implemented the fix: a new
`MealRepository.getCachedMealDetail(id)` method (memory + Room cache only, no network call), swapped
into `FavouritesViewModel.resolve()` in place of the previous `getMealDetail(id)` fallback, plus
updated doc comments and new tests.

**Why the change was needed:** `FavouritesViewModel` resolved a saved favourite id that fell
outside the current Indian base set by calling `getMealDetail(id)` — a live, unrestricted
`lookup.php` call with no area check. A successful response was effectively treated as proof the
meal belonged in TasteIndia, which is backwards: the Indian base set (`filter.php?a=India`) is the
only authority, and TheMealDB had already reclassified an area tag once before (documented
elsewhere in this repo as the `Indian` → `India` change), so an id could plausibly fall out of the
base set over time and still resolve as if it were valid.

**How I reviewed it:** Before any fix was written, I required a verification table checking this
specific Gemini finding against the actual code — reading `FavouritesViewModel.kt`,
`MealRepositoryImpl.kt`, and the existing `applyFilters`/`IndianBoundaryIntersectionTest` logic —
which confirmed the fallback really did call the network-backed `getMealDetail` with no boundary
check. Only after that was confirmed did I authorize the fix.

**What I changed:** No further edits beyond what Claude Code proposed were needed — the diff
(the new interface method, its implementation, the one-line swap in `FavouritesViewModel`, and the
doc-comment updates explaining the invariant) was reviewed and approved as written, because it was
scoped exactly to the confirmed problem and didn't touch unrelated code.

**How I verified it:** Confirmed `./gradlew testDebugUnitTest` passed, including two new
`FavouritesViewModelTest` cases (a favourite id outside the base set with a *resolvable* live
lookup must stay unresolved rather than showing the foreign meal; a favourite id outside the base
set must still resolve when it exists in this app's own cache) and three new `MealDetailCacheTest`
cases asserting `getCachedMealDetail` never triggers a network call. Also confirmed
`./gradlew assembleDebug` and `compileDebugAndroidTestSources` still built cleanly afterward.

**Why I accepted the final implementation:** It closes the actual gap (a live lookup could
launder a non-Indian or reclassified id back into the app) without weakening the offline-favourites
behavior the assignment asks for, adds no new dependency or architectural layer, and is covered by
tests that specifically exercise the scenario the finding described rather than just re-testing
existing behavior.

## Example: AI Recommendation That Was Reviewed

Gemini's review included a finding (#5, "N+1 network pattern in favourites") claiming that
resolving a large saved-favourites list called `getMealDetail` for each unresolved id one by one,
and recommended adding deduplication and bounded concurrency to fix it.

This was **rejected**, not implemented. Before accepting it, I had the actual repository code
checked: `MealRepositoryImpl.getMealDetail(id)` already serves from an in-memory map, then a Room
row, before any network call; concurrent calls for the same id already share one in-flight request
via a `ConcurrentHashMap`-backed `Deferred` (cleared on completion); and total concurrent detail
requests are already bounded by a `Semaphore(4)`. This was backed by an existing, passing test
suite (`MealDetailCacheTest`, five tests covering memory-cache hits, Room write-through, and
concurrent-request de-duplication) that predated the review. The recipe list itself also makes no
per-row detail calls at all (a row only needs image, name, and favourite state), so there was no
N+1 pattern to begin with.

I rejected the recommendation because implementing it would have added a second, redundant
deduplication/bounding layer on top of one that already existed and was already tested — directly
against the standing instruction not to introduce unnecessary architecture. This was verified by
reading the relevant source files directly (not taking Gemini's description at face value) and by
re-running `./gradlew testDebugUnitTest` to confirm the existing `MealDetailCacheTest` suite was
still green before closing the finding as invalid.

## Testing and Verification

AI-generated or AI-assisted code was not accepted because it compiled — a passing build was the
minimum bar, not the acceptance criterion. Verification actually performed:

- `./gradlew testDebugUnitTest` — the full JVM unit test suite (deterministic fixtures, in-memory
  fakes, and OkHttp `MockWebServer`; no live network calls), run after each meaningful change and
  required to pass before committing.
- `./gradlew assembleDebug` — a full debug build, run to confirm the app still compiles and
  packages after each change.
- `./gradlew compileDebugAndroidTestSources` — confirmed the instrumented test sources still
  compile against the current code.
- Manual/runtime verification via Android MCP on a Pixel 8 (API 37) emulator: installing the
  built APK, launching the app, navigating Welcome → Recipes → Details/Favourites, opening the
  filter sheet, and toggling favourites, to confirm the implementation actually behaves as the
  code and tests claim.

The instrumented Room test suite (`connectedDebugAndroidTest` — DAO and persistence tests) exists
in the repository and was verified passing earlier in development, but was not re-run as a Gradle
task in the most recent review/fix/documentation pass — no `adb`-connected test run was performed
in that pass, only the Android-MCP-driven manual checks above. This is stated explicitly in
`README.md`'s *Known Issues* rather than left implied.

## Candidate Responsibility

The candidate is responsible for the final submitted implementation and must be able to explain
and modify the submitted code.

## Limitations

AI suggestions — from both Claude Code and Gemini — can contain incorrect assumptions or
implementation errors, and did in at least one case here (the rejected N+1 finding above was based
on a description of the code that didn't match its actual state). Because of that, every
AI-produced claim about the code's behavior was checked against the actual source and, where
possible, against actual runtime behavior on the emulator, rather than accepted on description
alone; every AI-produced code change was checked against a Gradle build and the test suite before
being kept.
