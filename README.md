# TasteIndia

A native Android app for browsing Indian recipes from [TheMealDB](https://www.themealdb.com/api.php)'s
free v1 API. It supports search, category/ingredient filtering, sorting, a detail screen, and
favourites that persist locally and work offline. Built as an Android intern assignment submission.

## Features

- Recipe list scoped to Indian meals only, with thumbnail, name, and a favourite toggle per row.
- Search by name (local, debounced, always reflects the latest query).
- Filter by category and by main ingredient, from a curated list of real TheMealDB values.
- Favourites-only filter, combinable with search/category/ingredient.
- Sort A–Z / Z–A.
- Active-filter chips (each individually removable) plus "Clear all", and a live result count.
- Recipe detail screen: photo, category/area chips, tags, ingredients with measures, instructions,
  and links to the YouTube video / source recipe when TheMealDB provides them.
- Favourite a meal from the list or from the detail screen; a separate Favourites destination
  lists everything saved and works with no network for anything already seen.
- State restoration: search text, category, ingredient, favourites-only, sort, and list scroll
  position all survive Recipes → Details → Back, and search/filter/sort also survive process
  recreation.
- Deliberate loading / empty / error / offline states with retry, everywhere a request can fail.
- Branded splash screen (a custom Canvas animation) and a one-screen welcome page before the
  recipe list.
- Light and dark theme (follows the system setting).

## Tech Stack

| Component | Version |
| --- | --- |
| Kotlin | 2.3.20 |
| Android Gradle Plugin (AGP) | 9.4.0 |
| Gradle | 9.6.0 |
| KSP | 2.3.12 |
| compileSdk | 37 |
| minSdk | 24 |
| targetSdk | 37 |
| Jetpack Compose (BOM) | 2026.02.01 |
| Material 3 | via Compose BOM |
| Navigation Compose | 2.10.1 |
| Lifecycle (ViewModel / runtime-compose) | 2.11.0 |
| Retrofit | 3.0.0 |
| OkHttp | 5.5.0 |
| kotlinx.serialization | 1.11.0 |
| kotlinx.coroutines | 1.10.2 |
| Room | 2.8.5 |
| Coil (image loading) | 3.6.2 |
| JUnit4 | 4.13.2 |
| kotlinx-coroutines-test | 1.10.2 |
| OkHttp MockWebServer | 5.5.0 |
| androidx.test.ext:junit | 1.1.5 |
| Espresso core | 3.5.1 |

All versions above are read directly from `gradle/libs.versions.toml` and
`gradle/wrapper/gradle-wrapper.properties`. Kotlin is pinned via a `buildscript` classpath entry
in the root `build.gradle.kts` because AGP 9's built-in Kotlin defaults to an older compiler than
the Compose BOM / Coil in this project require; see `AI_DISCLOSURE.md` for the full reasoning.

## Requirements

- JDK capable of running Gradle 9.6 and compiling against Java 17 source/target compatibility
  (Android Studio's bundled JDK satisfies this; the project's `settings.gradle.kts` includes the
  `foojay-resolver-convention` plugin so Gradle can provision a toolchain JDK automatically if
  needed).
- Android SDK with **platform 37** and matching build tools installed (Android Studio's SDK
  Manager handles this).
- An Android emulator or physical device on **API 24 or newer** to run the app.
- Internet access on the device/emulator to reach TheMealDB — **no API key or secret is
  required**, TheMealDB v1 is a free public API.
- No `local.properties` secrets, no `.env` file, nothing to configure before building.

## Setup

1. Clone or copy the project folder to your machine.
2. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio.
3. Let Android Studio sync Gradle (or run `./gradlew --version` once from a terminal to trigger
   the same sync headlessly).
4. Start an emulator (API 24+) from the Device Manager, or connect a physical device with USB
   debugging enabled.
5. Run the app from Android Studio (Run ▶), or from a terminal:
   ```bash
   ./gradlew installDebug
   ```

To run the tests:

```bash
./gradlew testDebugUnitTest          # JVM unit tests, no device needed
./gradlew connectedDebugAndroidTest  # Room instrumented tests, needs a running device/emulator
```

## Architecture

Single Gradle module (`:app`), layered by responsibility:

```
UI (Compose)  →  ViewModel  →  Repository  →  Remote (Retrofit/OkHttp) / Local (Room)
```

- **`data/remote`** — `MealApi` (Retrofit interface), request/response DTOs (`MealSummaryDto`,
  `MealDetailDto`, ...), `NetworkModule` (OkHttp/Retrofit construction), `ConnectivityInterceptor`,
  and `Throwable.toAppError()` (the one place every network exception is mapped to a typed error).
- **`data/local`** — Room: `TasteIndiaDatabase`, `FavouriteMealEntity`/`FavouriteMealDao`,
  `CachedMealDetailEntity`/`CachedMealDetailDao`.
- **`data/repository`** — `MealRepositoryImpl`, `FavouritesRepositoryImpl`, and the DTO → domain
  mapping functions (`MealMappers.kt`). This is the only layer that talks to Retrofit or Room
  directly.
- **`domain/model`** — plain Kotlin data classes with no Android/network/DB dependency: `Meal`,
  `MealDetail`, `Ingredient`, `SortOrder`, `AppError` (a sealed error type), `DataResult<T>` (a
  typed success/failure wrapper used instead of throwing across layers).
- **`domain/repository`** — `MealRepository` / `FavouritesRepository` interfaces. ViewModels
  depend on these interfaces, not the `data` implementations.
- **`presentation/{recipes,details,favourites,splash,welcome,common}`** — one `ViewModel` +
  one immutable `@Immutable data class ...UiState` per screen, plus the Compose screens
  themselves. `RecipesViewModel` additionally owns a pure, unit-tested `applyFilters()` function
  that turns `(base list, filters, favourite ids, resolved category/ingredient ids)` into the
  final sorted list — no filtering logic lives in the Composable.
- **`navigation`** — `Destination` (a `sealed interface` of `@Serializable` type-safe routes) and
  `TasteIndiaNavHost`.
- **`di`** — `AppContainer` (hand-written dependency container) and `TasteIndiaApp`.

**Why DTOs are separate from domain models:** TheMealDB's detail response is a flat 40-field shape
(`strIngredient1..20` / `strMeasure1..20` plus assorted `str*` fields, several blank rather than
absent). Nothing outside `data/repository` ever sees a DTO — the repository maps each one to a
domain `Meal`/`MealDetail` with clean, nullable fields and a normalized `List<Ingredient>`.

**Repository responsibility:** own every read against TheMealDB and Room, cache what's worth
caching, and — critically — be the single place that enforces the Indian boundary (see the
dedicated section below). A repository method never returns raw, un-intersected ids.

**ViewModel responsibility:** hold no `Context`/`Activity`/`NavController`; combine repository
flows and one-shot results into a single `StateFlow<UiState>` via
`combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)`;
expose intent functions (`onSearchQueryChange`, `toggleFavourite`, `retry`, ...) that mutate
`SavedStateHandle` or call the repository. Compose collects state with
`collectAsStateWithLifecycle()`.

**Room** is used for two tables: `favourite_meals` (the actual favourites feature) and
`cached_meal_details` (an offline/performance cache of previously fetched recipe details, storing
the raw serialized DTO JSON rather than exploded columns).

**Dependency injection** is a small hand-written container (`AppContainer` interface +
`DefaultAppContainer`), not Hilt/Koin. Everything is a `by lazy` singleton (`OkHttpClient`,
`Retrofit`, `MealApi`, the Room database and DAOs, the two repositories, the shared `Json`
instance) built in one file and handed to ViewModels through `viewModelFactory { initializer {} }`
reading the container off the `Application`. This avoids an annotation-processing dependency for
a project of this size, at the cost of no compile-time graph validation.

## Route Map

```
Splash
  ↓ (auto, ~2s animation)
Welcome
  ↓ (Explore recipes)
Recipes
  ↓ (tap a recipe)          ↓ (favourites icon)
Details/{mealId}          Favourites
                             ↓ (tap a recipe)
                           Details/{mealId}
```

Routes are type-safe `@Serializable` objects/data classes on `Destination` (`Splash`, `Welcome`,
`Recipes`, `Favourites`, `Details(mealId: String)`). Splash and Welcome are each popped from the
back stack once left (`popUpTo(...) { inclusive = true }`), so Recipes is the effective home and
back from it exits the app.

`Destination.Details` carries **only `mealId: String`** — no `Meal`/`MealDetail` object is ever
passed through navigation. Both entry points (Recipes and Favourites) navigate to the same
`Details/{mealId}` route, and `DetailsViewModel` re-resolves the full recipe by id through
`MealRepository.getMealDetail`, which is served from an in-memory cache, then a Room cache, before
any network call. `RecipesViewModel` is scoped to its own `NavBackStackEntry`, so returning from
Details lands on the same ViewModel instance with its filter state intact.

## API Endpoints

Only TheMealDB's free **v1 public** API is used, base URL
`https://www.themealdb.com/api/json/v1/1/`. No key, no private/paid/alternative API.

| Endpoint | Used for |
| --- | --- |
| `filter.php?a=India` | **The authoritative Indian base collection** — every meal id the app will ever show comes from this response. |
| `filter.php?c={category}` | Meals in a category, across all cuisines. |
| `filter.php?i={ingredient}` | Meals by main ingredient, across all cuisines. |
| `lookup.php?i={mealId}` | Full detail for one meal, by id. |

`search.php` is not used (see *Indian Boundary and Filtering*, "Search is local"). `list.php` is
not used (see *Data Normalization / filter options* below).

The category and ingredient endpoints return meals from **every** area TheMealDB has, not just
India — they do not define an Indian collection on their own. Their results are only ever used
after being intersected with the `filter.php?a=India` response; see the next section.

**Deviation from the literal assignment text:** the brief specifies `filter.php?a=Indian`. As of
this build, TheMealDB tags Indian meals with `strArea="India"` (the country name) instead;
`a=Indian` returns `{"meals":null}` while `a=India` returns the ~15 Indian meals. This was
verified against the live API during development and is a data change on TheMealDB's side, not a
different endpoint or a misreading of the brief.

**Empty is not an error.** TheMealDB returns HTTP 200 with `{"meals":null}` when nothing matches a
filter/category/ingredient query — that maps to a successful empty list, never `AppError`. The one
exception is `lookup.php` returning no meal for a requested id, which maps to `AppError.NotFound`
because a details screen has nothing to render.

## Indian Boundary and Filtering

The Indian base set (`filter.php?a=India`) is loaded once and held in memory. It is the only
source of truth for "is this meal in TasteIndia":

```
Indian base IDs
      ∩
category IDs (from filter.php?c=...)
      ∩
ingredient IDs (from filter.php?i=...)
      =
valid result IDs
```

`MealRepository.getIndianMealIdsForCategory(category)` and `getIndianMealIdsForIngredient(...)`
each call the matching `filter.php` endpoint and intersect the returned ids with the Indian base
id set before returning — there is no method on `MealRepository` that hands back an un-intersected
id set, so no ViewModel or screen can bypass the boundary.

On top of that, `RecipesViewModel`'s pure `applyFilters()` starts from the in-memory Indian base
list itself (not from an arbitrary meal list) and filters *that* down by the resolved
category/ingredient id sets, favourites, and search text. Because the starting list already is the
Indian base set, filtering by those resolved ids is a second intersection with the same boundary.

**Favourites are checked against the same boundary, not re-derived from a lookup.** Favourite ids
are persisted in Room as bare strings, independent of any single request. When the Favourites
screen resolves them into rows, each id is looked up in the current in-memory Indian base set
first. An id that isn't currently in the base set (offline before the base set has loaded, or an
id TheMealDB has since reclassified out of the area) is resolved from this app's own previously
cached detail (`MealRepository.getCachedMealDetail`, memory + Room only) if available, and left as
"unresolved" otherwise. **A successful `lookup.php` response is deliberately never treated as
proof that a meal is Indian** — `getMealDetail` (the method that does make a live `lookup.php`
call) has no area check, so using its success as a validity signal would let a foreign or
reclassified id back into the app. Only the base-set intersection, or this app's own prior cache,
establishes membership.

**Latest state wins.** Category/ingredient selections resolve through `flatMapLatest`, so a slower
earlier request is cancelled and its result can never overwrite a faster later selection. The
search query is `debounce(200ms)` + `distinctUntilChanged()` before it reaches the filter
`combine`.

## Search, Sort and State

- **Search** is local, over the in-memory Indian base list (a case-insensitive substring match on
  the trimmed query), after a 200ms debounce so rapid typing doesn't re-filter on every keystroke.
  `search.php` is intentionally not used — it returns all cuisines and would need its own
  intersection, and local search over a small (~15-meal) base set is instant and never stale.
- **Category** and **ingredient** filters use a curated fixed list of real TheMealDB values
  (`FilterOptions.CATEGORIES` / `FilterOptions.INGREDIENTS`) shown in a filter bottom sheet; each
  selection resolves through the Indian-boundary intersection described above.
- **Favourites-only** filters the visible list down to ids in the current favourite set.
- **Sort** is A–Z / Z–A over the (already filtered) result, case-insensitive.
- **Result count** and active-filter chips (each individually removable, plus "Clear all") are
  derived from the same `RecipesFilters`/`RecipesUiState` the list is built from, so they can never
  disagree with what's on screen.

**Source of truth.** Search text, category, ingredient, favourites-only, and sort all live in
`RecipesViewModel`'s `SavedStateHandle` (five keys, e.g. `recipes.query`, `recipes.category`).
They're exposed as `StateFlow`s via `handle.getStateFlow(...)` and combined into the filter
pipeline; intent functions (`onSearchQueryChange`, `onCategorySelected`, ...) write straight back
into the handle.

**State restoration.** Because these five values live in `SavedStateHandle`, they survive both a
Recipes → Details → Back navigation (the ViewModel is scoped to the Recipes back-stack entry and
is never recreated for that round trip) and process death / recreation (`SavedStateHandle` is
backed by the saved-instance-state mechanism). The recipe list's scroll position uses
`rememberLazyListState()`, which is itself `rememberSaveable`-backed and preserved by
`NavHost`'s per-back-stack-entry saved state, so scrolling down, opening a recipe, and pressing
Back returns to the same scroll offset.

## Caching and Network Strategy

| Cache | Where | Purpose |
| --- | --- | --- |
| Indian base set | in-memory (`@Volatile` field behind a `Mutex`, double-checked) | loaded once per process; `forceRefresh` re-fetches on retry |
| Category/ingredient id sets | in-memory `ConcurrentHashMap` keyed by the filter value | re-selecting a filter, or re-selecting after Clear all, does zero network work |
| Meal detail (memory) | `ConcurrentHashMap<id, MealDetail>` | returning to a previously opened recipe is instant |
| Meal detail (disk) | Room `cached_meal_details` table | survives process death; enables offline detail viewing for previously seen meals |

**Detail request handling (`getMealDetail`)** checks the memory cache, then the Room cache, before
ever calling the network. Concurrent calls for the same id share one in-flight request
(`ConcurrentHashMap.computeIfAbsent` around a `Deferred`, cleared on completion), and total
concurrent detail requests are bounded by a `Semaphore(4)`. The recipe list itself never triggers
per-row detail calls (a row only needs image, name, and favourite state), so there is no N+1
pattern to begin with; the bounding above covers detail requests triggered from elsewhere (e.g.
resolving several favourites at once).

**Cancellation.** Every ViewModel does its work inside `viewModelScope`; `Throwable.toAppError()`
explicitly rethrows `CancellationException` rather than mapping it to an error, so cancelling an
in-flight request (e.g. a superseded category selection via `flatMapLatest`) doesn't surface as a
user-visible failure.

**Timeouts.** OkHttp is configured with a 10s connect timeout, 15s read timeout, and 20s overall
call timeout, each mapped to `AppError.Timeout`.

**HTTP errors.** A non-2xx response throws Retrofit's `HttpException`, mapped to
`AppError.Http(code)` and shown as a plain-language message including the status code.

**Offline behavior.** `ConnectivityInterceptor` checks `ConnectivityManager` before a request
leaves the app and fails fast with `AppError.Network` if there's no active network, so the user
sees the offline state immediately rather than waiting out a connect timeout. This is a fast-path
optimization only — the interceptor's guess is never treated as authoritative on its own; if it
misjudges connectivity, the real OkHttp call still runs and any resulting `IOException`/timeout is
mapped the same way. This app does not claim to work fully offline: only previously fetched data
(the Indian base list, cached recipe details, and Room-stored favourite ids) is available without
a network connection.

**Retry.** Every error state (Recipes list, filter resolution, Details, Favourites) presents a
"Try again" action that re-runs the failed operation.

## Favourites

- Favourites are a single Room table (`favourite_meals`), storing only `idMeal` and `addedAt` —
  no meal content is duplicated into the favourites table.
- `FavouritesRepository.toggle(id)` adds or removes a row and reports the resulting state;
  `observeFavouriteIds()` is a `Flow<Set<String>>` that both the Recipes screen (for the heart
  icon state / favourites-only filter) and the Favourites screen collect.
- Favourites work with no network: reading, adding, and removing rows is Room-only.
- Favourites persist across app relaunch (Room writes to disk; verified with an instrumented test
  that closes and reopens the database file).
- Favourite ids are validated against the Indian boundary when displayed (see *Indian Boundary and
  Filtering*): the Favourites screen resolves each saved id against the current Indian base set,
  falling back only to this app's own cached detail, never to a fresh unrestricted lookup.
- A Room write failure (e.g. a disk error) is caught in `FavouritesRepositoryImpl.toggle()` and
  reported as the pre-toggle state instead of throwing, so a local-storage hiccup can't crash a
  screen; `CancellationException` is still rethrown so cancellation keeps working correctly.

## Data Normalization

`MealMappers.kt` is the only place DTOs are converted to domain models.

- **Ingredients:** TheMealDB's detail response has 20 positional `strIngredient1..20` /
  `strMeasure1..20` fields, each independently possibly `null`, empty, or whitespace.
  `normalizeIngredients` treats a pair as meaningful only if the ingredient name is non-blank after
  trimming; the measure is trimmed and kept (or becomes `""` if absent); a slot with a measure but
  no name is dropped as noise. The result is a clean `List<Ingredient>` with no empty slots.
- **Tags:** `strTags` is a comma-separated string; `splitTags` trims each piece, drops blanks,
  de-duplicates, and preserves order.
- **Blank → null:** every other optional string field (category, area, instructions, thumbnail,
  YouTube URL, source URL) is trimmed and normalized to `null` when empty, so the UI only has to
  check for `null` / empty list, never for blank strings.
- **Instructions:** CRLF line endings are normalized to LF so Compose renders paragraphs correctly.

## Error and Empty States

| State | Behaviour |
| --- | --- |
| Loading | Spinner with a "Loading recipes…" label, announced via `liveRegion`. |
| No results | Context-aware empty message (no favourites yet / nothing matches this filter combination / nothing loaded), with "Clear all" where relevant. |
| API/provider error | A non-2xx response shows a plain-language message including the HTTP status code, with a "Try again" button. |
| Timeout | A configured OkHttp timeout maps to a "took too long, try again" message. |
| Offline | Detected before the request is sent; shown as an immediate offline message with retry, not a long spinner. |
| Retry | Present on every error state (Recipes, filter resolution, Details, Favourites). |
| Partial/malformed detail data | List rows with a blank id/name are dropped from the base set; on the details screen, only non-empty fields render, so a sparsely-populated recipe still displays cleanly. |
| Image failure | `MealImage` (Coil `SubcomposeAsyncImage`) has explicit loading and error slots, both sized to fill the same box as a successful image, so a failed or missing thumbnail doesn't collapse the row layout. A `null`/blank URL routes straight to the error slot. |

No load path is left on an indefinite spinner: every request resolves to success, empty, or an
explicit error state with retry.

## Accessibility

- Icon-only controls (favourite toggle, back button, clear-search, filter chips' remove action,
  external link buttons) carry explicit `contentDescription`/`clearAndSetSemantics` labels
  describing the action and, where relevant, the meal name (e.g. "Add Butter Chicken to
  favourites").
- Result counts and the "N saved recipes" header use `liveRegion` so screen readers announce
  changes without the user needing to navigate to them.
- The favourite toggle is a separately focusable `IconButton` inside a card whose own tap target
  is marked `Role.Button`, so both the row and the toggle are individually reachable by a screen
  reader; icon buttons use Material 3's default touch target sizing.
- Ingredient rows use `clearAndSetSemantics` to merge the name and measure into one spoken node
  (e.g. "2 large, sliced Onion") instead of two separate announcements.
- Purely decorative icons/images (e.g. the thumbnail next to a name that's already read aloud)
  have `contentDescription = null` so they aren't announced twice.
- Text uses `MaterialTheme.typography`, which scales with the system font size setting; no text is
  rendered as a fixed-size bitmap or baked into an image.
- Both light and dark theme are implemented (`isSystemInDarkTheme()`); Material You dynamic colour
  is intentionally off so the app has one consistent visual identity for review.
- State is never communicated by colour alone: loading/error/empty each have their own message and
  icon/illustration, not just a colour change; the favourite icon changes shape (outline vs.
  filled), not only colour.

This was not verified with an automated accessibility scanner (e.g. Accessibility Scanner /
Compose UI test semantics assertions) — the above reflects what's implemented in the composables,
not a tooled audit.

## Testing

All tests are deterministic: they use fixture JSON files, hand-written in-memory fakes, and
OkHttp's `MockWebServer`, never the live TheMealDB API. 61 JVM unit tests currently pass
(`./gradlew testDebugUnitTest`); 7 instrumented Room tests exist across 3 suites and require a
connected device/emulator to run (`./gradlew connectedDebugAndroidTest`).

**Unit tests (`app/src/test`, `testDebugUnitTest`)**

| Suite | What it covers |
| --- | --- |
| `IngredientNormalizationTest` | `strIngredient1..20`/`strMeasure1..20` → `Ingredient` normalization, and tag splitting. |
| `IndianBoundaryIntersectionTest` | Category/ingredient results intersected with the Indian base set; no-overlap → empty set; partial/blank rows dropped; base set fetched from the network only once. |
| `RecipeFilteringTest` | The pure `applyFilters` function: AND-combination of filters, case-insensitive search, sorting, base-set boundary. |
| `LatestFilterStateWinsTest` | A slower earlier category/search selection never overrides a faster later one. |
| `RecipesStateRestorationTest` | A pre-populated `SavedStateHandle` rebuilds search/category/favourites-only/sort and the exact filtered list. |
| `FavouritesRepositoryImplTest` | Toggle add/remove/idempotency, `observeFavouriteIds`, and that a DAO failure on insert/delete doesn't throw and reports the unchanged state. |
| `FavouritesViewModelTest` | Resolving favourites against the base set; an id outside the base set stays unresolved rather than resolving via a live network lookup; an id outside the base set resolves from this app's own cache with zero network calls. |
| `DetailsViewModelTest` | Loading a meal by id, reflecting/toggling favourite state, `NotFound` handling. |
| `MealDetailCacheTest` | Memory/Room cache hits, write-through, concurrent-request de-duplication, `getCachedMealDetail` never calling the network. |
| `NetworkErrorsTest` | The exception → `AppError` mapping table. |
| `MealApiReliabilityTest` | Real OkHttp + Retrofit + kotlinx.serialization against `MockWebServer`: empty response, HTTP 500, malformed JSON, slow response, offline. |

**Instrumented tests (`app/src/androidTest`, `connectedDebugAndroidTest`)** — real Room, no fakes:

| Suite | What it covers |
| --- | --- |
| `FavouriteMealDaoTest` | Insert/exists/delete, most-recent-first ordering, insert-ignore on conflict. |
| `FavouritesPersistenceTest` | Write rows, close the database (simulating process death), reopen from the same file, confirm rows (and removals) persist. |
| `CachedMealDetailDaoTest` | Missing id returns `null`; upsert replaces an existing row. |

## Assumptions

- The Indian collection returned by `filter.php?a=India` is small (~15 meals), which is why local,
  in-memory search/filtering is fast enough with no server-side search.
- Users can only favourite meals they can actually see — a meal from the Recipes list or its
  detail screen. The Favourites screen resolves saved ids against the base set, then this app's own
  cache; an id that resolves to neither (offline, and never previously viewed) is shown as an
  "unresolved" count rather than silently dropped.
- `filter.php?a=India` (not the literal `a=Indian` from the brief) is the correct current query
  for TheMealDB's Indian collection — see *API Endpoints*.
- Category and ingredient filter values are a small curated list rather than fetched from
  `list.php`, because that endpoint is outside the endpoint set this project uses and the base-set
  response carries no category/ingredient data to derive options from.
- A single Gradle module is proportional for a project this size; no `:core`/`:feature` split.

## Tradeoffs

| Decision | Upside | Downside |
| --- | --- | --- |
| Hand-written `AppContainer` instead of Hilt/Koin | Small, no annotation processing, whole graph readable in one file | No compile-time graph validation; wiring is manual |
| Local-only search, `search.php` unused | Instant, never returns a stale result, no extra Indian-boundary intersection | Can't find an Indian meal that TheMealDB doesn't return under `a=India` |
| Detail cache stored as a raw DTO JSON blob in Room | One mapping site, no ~20-field `TypeConverter` set | Cached details aren't queryable by individual field (not currently needed) |
| Curated fixed filter option lists | No extra endpoint, no per-row enrichment to build the list from | Some options may match zero Indian meals and yield the empty state |
| `getCachedMealDetail` (cache-only) fallback for favourites outside the base set | Never lets a network lookup "prove" Indian membership; still works offline for previously seen meals | An id that's outside the base set and was never cached stays unresolved rather than being fetched fresh |
| `WhileSubscribed(5_000)` on every `stateIn` | Frees upstream work while a screen isn't collected | Returning after more than 5s idle re-runs the `combine` (served from cache, not a network call) |
| List rows show only image/name/favourite (no per-row detail enrichment) | Zero N+1 network calls from the list | Category/area aren't visible until the detail screen |

## Known Issues

- **`filter.php?a=Indian` (the brief's literal query) returns no results upstream** — worked
  around with `a=India`; documented in *API Endpoints* and in code comments.
- **No pull-to-refresh** — retry is via the error-state button and reopening the filter sheet; a
  `PullToRefreshBox` on the list would be a natural small addition.
- **Release build is unoptimized** — `minifyEnabled`/R8 is off in the release build type
  (inherited from the project template); the app is intended to be reviewed as a debug build. A
  release pass would need R8 keep rules for kotlinx.serialization and Room.
- **No automated accessibility scan** — accessibility semantics were implemented and reasoned
  about (see *Accessibility*) but not verified with an automated scanner or Compose accessibility
  test.
- **Instrumented Room tests need a connected device/emulator** and were not re-run as part of this
  README update (no device was attached in this environment); they were last verified passing on
  an API 37 emulator during development and touch code that hasn't changed since.

There are no known assignment-blocking issues.

## Attribution

- Recipe data, images, and metadata are served by [TheMealDB](https://www.themealdb.com/) v1
  public API (`https://www.themealdb.com/api/json/v1/1/`), used under its free-tier terms. This
  project is not affiliated with, endorsed by, or sponsored by TheMealDB; recipe content and
  photos returned by the API belong to TheMealDB and its contributors.
- Open-source libraries used (see *Tech Stack* for versions), each under its own upstream license:
  Kotlin and kotlinx libraries (serialization, coroutines), AndroidX/Jetpack (Compose, Lifecycle,
  Navigation, Room, Core KTX, Activity), Square's Retrofit and OkHttp, Coil, and JUnit4. Consult
  each library's own repository for its exact license text.
- App icons are inline `ImageVector`s built from Material Symbols path data (`TasteIndiaIcons`),
  used in place of pinning `androidx.compose.material:material-icons-core` (frozen at an older
  version and no longer managed by the Compose BOM).

## Time Spent

Roughly **9–11 hours** of equivalent development effort across the original build (data layer,
recipe list, search/filters/sort, details, favourites, reliability hardening, tests, and docs),
plus an additional focused pass (~1–1.5 hours) for an independent review round that fixed two
issues found in the favourites/Indian-boundary and Room-failure-handling code paths. This is an
approximate figure, not a tracked timesheet.

## AI / Tool Use

This project was built with AI assistance — Claude Code (Anthropic's agentic coding CLI) for
implementation, and Gemini for an independent second-pass review of the finished implementation.
AI involvement is not hidden: every phase was human-directed, each AI-authored change was reviewed
before being built/tested/committed, and Gemini's review findings were independently verified
against the actual code (not applied automatically) before any fix was made. See
[`AI_DISCLOSURE.md`](AI_DISCLOSURE.md) for the detailed disclosure, including which decisions were
AI judgement calls that a reviewer should be ready to see explained.

## Submission Notes

From a fresh checkout:

```bash
./gradlew clean assembleDebug        # build the debug APK
./gradlew testDebugUnitTest          # run the 61 JVM unit tests (no device needed)
./gradlew connectedDebugAndroidTest  # run the 7 Room instrumented tests (needs a device/emulator)
./gradlew installDebug               # install on a running emulator/device
```

No API key, secret, or `local.properties` entry is required — TheMealDB v1 is a free public API
and the app only needs internet access on the device/emulator to reach it. Opening the project
root in Android Studio and syncing Gradle is equivalent to the manual steps above.
