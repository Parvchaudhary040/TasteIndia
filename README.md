# TasteIndia

A native Android app that browses Indian recipes from [TheMealDB](https://www.themealdb.com/api.php)
(free v1 API), with search, filtering, sorting, a recipe detail screen, and locally‑persisted
favourites that work offline.

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Coroutines/StateFlow ·
Retrofit + OkHttp · kotlinx.serialization · Room · Coil · manual DI.

---

## Build & run

Requirements: JDK is provisioned by Gradle (toolchain Java 25); Android SDK with **platform 37**
and **build‑tools 36**; an emulator or device on **API 24+**.

```bash
./gradlew assembleDebug          # build the APK
./gradlew testDebugUnitTest      # 54 JVM unit tests
./gradlew connectedDebugAndroidTest   # 7 Room instrumented tests (needs a device)
./gradlew lintDebug              # 0 errors, 9 "newer version available" warnings (see Known issues)
./gradlew installDebug           # install on the running device
```

Everything is Gradle‑standard; open the folder in Android Studio and Run.

---

## Architecture

Proportional layered architecture, one Gradle module:

```
data/
  remote/      MealApi (Retrofit), DTOs, NetworkModule, ConnectivityInterceptor, error mapping
  local/       Room: TasteIndiaDatabase, FavouriteMealEntity/Dao, CachedMealDetailEntity/Dao
  repository/  MealRepositoryImpl, FavouritesRepositoryImpl, DTO→domain mappers
domain/
  model/       Meal, MealDetail, Ingredient, SortOrder, AppError, DataResult
  repository/   MealRepository, FavouritesRepository (interfaces)
presentation/
  recipes/     RecipesViewModel + immutable RecipesUiState, screen, filter sheet, chips,
               pure applyFilters()
  details/     DetailsViewModel + DetailsUiState, screen
  favourites/  FavouritesViewModel + FavouritesUiState, screen
  common/      MealImage (Coil), state views, URL launcher, TasteIndiaIcons
navigation/    Destination (type‑safe routes), TasteIndiaNavHost
di/            AppContainer, TasteIndiaApp
ui/theme/      Material 3 "spice" palette, light + dark
```

**Separation of models.** Wire DTOs (`MealDetailDto` with 40 flat `strIngredient/Measure`
fields) never leave `data/`. The repository maps them to domain models (`Meal`, `MealDetail`,
`Ingredient`). Persistence models (`FavouriteMealEntity`, `CachedMealDetailEntity`) are separate
again. The presentation layer has its own `RecipeListItem` / `RecipesUiState` etc.

**State.** ViewModels expose a single immutable `StateFlow<...UiState>` (`data class`, `@Immutable`)
built by `combine(...).stateIn(viewModelScope, WhileSubscribed(5s), initial)`. Compose collects
with `collectAsStateWithLifecycle()`. ViewModels hold no Activity / NavController / Context;
dependencies arrive through `viewModelFactory { initializer { ... } }` reading `AppContainer`
via `APPLICATION_KEY`. There is one small Activity (`MainActivity` just hosts the `NavHost`) and
three focused ViewModels — no god objects.

**Dependency injection.** Hand‑written `AppContainer` (interface + `DefaultAppContainer`) owned
by `TasteIndiaApp`. `by lazy` singletons: `OkHttpClient`, `Retrofit`, `MealApi`, Room DB, DAOs,
the two repositories, the shared `Json`. No Hilt/Koin — the assignment asks for a *lightweight*
approach; this is ~60 lines, no annotation processing, and the whole graph is readable in one
file. Trade‑off: no compile‑time graph validation, manual wiring — fine at this size.
*Alternative considered:* Hilt — rejected as heavier than the problem and another KSP processor.

**Navigation.** Single‑Activity, type‑safe `@Serializable` routes. `Destination.Details` carries
**only `mealId: String`** — no `Meal`/`MealDetail` object is ever passed through navigation; the
detail screen re‑resolves by id (from cache). `RecipesViewModel` is scoped to its
`NavBackStackEntry`, so Recipes → Details → Back returns to the *same* instance with filters
intact.

---

## API / endpoints

Only TheMealDB **v1 public** API (`https://www.themealdb.com/api/json/v1/1/`). No private,
scraped, paid or alternative APIs.

| Endpoint | Used for |
| --- | --- |
| `filter.php?a=India` | the **Indian base set** (id, name, thumbnail only) |
| `filter.php?c={category}` | meals in a category (all cuisines — intersected, see below) |
| `filter.php?i={ingredient}` | meals by main ingredient (all cuisines — intersected) |
| `lookup.php?i={mealId}` | full detail for one meal |

`search.php?s=` is **not** used — see *Search strategy*. `Retrofit` `@Query` handles URL
encoding, so category/ingredient values are passed raw.

> **Deviation from the brief:** the assignment specifies `filter.php?a=Indian`. Between the brief
> being written and this build, TheMealDB re‑tagged its Indian meals with `strArea="India"` (the
> country name). `a=Indian` now returns `{"meals":null}`; `a=India` returns the ~15 Indian meals.
> `MealRepositoryImpl.INDIAN_AREA` is `"India"` with a comment. Verified against the live API on
> 2026‑09‑10. This is a data change upstream, not a change of endpoint.

**Empty is not an error.** Every endpoint returns HTTP 200 `{"meals":null}` when nothing matches.
That maps to `DataResult.Success(emptyList())`, never an `AppError` — the classic bug with this
API. The one exception: `lookup.php` returning no meal for a requested id → `AppError.NotFound`,
because a detail screen has nothing to render.

---

## Indian‑boundary / filtering strategy

**The Indian base set is authoritative.** `filter.php?a=India` is loaded once and held in memory
(`MealRepositoryImpl`, behind a `Mutex` with double‑checked caching). It is the universe of meal
IDs the app will ever show.

`filter.php?c=` and `filter.php?i=` return meals from **every** cuisine. The repository never
hands those back raw:

```
getIndianMealIdsForCategory(cat):
    ids = filter.php?c=cat  →  retainAll( indianBaseIds )   →  Set<String>
```

There is **no method on `MealRepository` that returns un‑intersected IDs**, so no ViewModel or
screen can escape the Indian boundary — it is enforced in one place, in the data layer.

The presentation layer applies filters with a **pure function** (`applyFilters`, unit‑tested):
starting from the in‑memory Indian base list, it keeps only meals whose id is in the resolved
category set / ingredient set / favourites set, then substring‑matches the search query, then
sorts. Because the starting list *is* the Indian base set, filtering by the resolved id sets is a
**second** intersection with the boundary.

**Latest state wins.** Category/ingredient selections resolve through `flatMapLatest`, so a slow
earlier request is cancelled and never "arrives late". The search query is `debounce(200ms)` +
`distinctUntilChanged` before it reaches the filter `combine`.

**Search strategy.** Search is **local** over the ~15‑item in‑memory base set — instant, and it
cannot return stale results. `search.php` is deliberately unused: it would return all cuisines
(needing yet another intersection) and reintroduce the stale‑response problem the brief warns
about. Trade‑off: the app can't find an Indian meal that is absent from `filter.php?a=India`;
accepted, and it matches the brief's "prefer local filtering of the loaded Indian base set".

**Filter options.** `FilterOptions` ships short, curated lists of real TheMealDB category /
ingredient values (`Chicken`, `Garam Masala`, …). `list.php?c=list` / `list.php?i=list` are
outside the allowed endpoint set, and the base‑set response carries no category/ingredient data
to derive options from without enriching every row. An option that matches no Indian meal simply
yields the empty state.

---

## Cache & performance strategy

| Cache | Where | Purpose |
| --- | --- | --- |
| Indian base set | in‑memory (`@Volatile` + `Mutex`) | loaded once; `forceRefresh` on retry |
| Category / ingredient id sets | in‑memory `ConcurrentHashMap` keyed by value | re‑selecting a filter or re‑selecting after *Clear all* does zero network |
| Meal detail (memory) | `ConcurrentHashMap<id, MealDetail>` | returning to a recipe is instant |
| Meal detail (disk) | Room `cached_meal_details` (raw DTO JSON blob) | survives process death; enables offline detail |
| Images | Coil, reusing the app's single `OkHttpClient` | one connection pool + timeout policy |

**N+1 protection.** The recipe list shows image + name + favourite only, so it makes **no
per‑row detail calls** — there is no N+1 to begin with. On top of that, `getMealDetail(id)` is:
memory → Room → network; concurrent callers for the same id **share one `Deferred`**
(`computeIfAbsent`, cleared on completion so a later refetch isn't blocked); total detail
concurrency is bounded by `Semaphore(4)`. No network work is launched from a `LazyColumn` item.

The detail cache stores the raw DTO JSON rather than exploded columns because it is only ever
read by id, and this avoids ~20 Room `TypeConverter` pairs; a corrupt/stale row is swallowed and
refetched.

---

## Reliability — deliberate states

`AppError` is a sealed type: `Network`, `Timeout`, `Http(code)`, `NotFound`, `Serialization`,
`Unknown`. `Throwable.toAppError()` maps every exception the stack can throw in one tested place;
`CancellationException` is rethrown, never swallowed.

| State | Behaviour |
| --- | --- |
| **Loading** | spinner + "Loading recipes…", `liveRegion` announced |
| **Success** | list; `"N recipes"` / `"… match your filters"` count |
| **Empty / no results** | context‑aware message (no favourites yet / no match / nothing loaded) |
| **API error** | `Http(code)` → "The recipe service is having trouble (error 503)…" |
| **Timeout** | OkHttp connect 10s / read 15s / call 20s → `AppError.Timeout` → "took too long" |
| **Offline** | `ConnectivityInterceptor` checks `ConnectivityManager` **before** the request and throws — the offline state appears immediately, not after a connect timeout |
| **Retry** | every error state (Recipes, Details, filter resolution, Favourites) has *Try again* |
| **Partial data** | list rows with a blank id/name are dropped; the detail screen renders only non‑empty fields; a "name‑only" meal doesn't crash |
| **Image failure** | `MealImage` (Coil `SubcomposeAsyncImage`) has explicit loading and error slots that both fill the caller's fixed box — the row never collapses; a null/blank URL routes straight to the error slot |

The user is never left on an infinite spinner: every load resolves to success, empty, or an
error state with a retry.

Network: explicit HTTP status handling (Retrofit `HttpException`), configured timeouts,
structured‑concurrency cancellation, safe URL construction (`@Query`), no requests inside
composable bodies (all in ViewModel coroutines).

---

## Data normalization

- **Ingredients:** `strIngredient1..20` / `strMeasure1..20` → `List<Ingredient>`. A pair is kept
  only when the ingredient name is non‑blank after trimming; a blank/absent measure becomes
  `""`; a slot with a measure but no name is dropped as noise. Unicode whitespace (e.g. NBSP) is
  trimmed.
- **Tags:** comma‑split, trimmed, blanks dropped, de‑duplicated, order preserved.
- **Instructions:** CRLF → LF so Compose renders paragraphs.
- **Blank → null:** every optional string field is trimmed and turned to `null` when empty, so
  the detail screen only checks for `null` / empty list.
- **URLs:** validated (`http`/`https` + host) before a button is shown; opened via `ACTION_VIEW`
  intent wrapped so a malformed/unhandleable link shows a toast, never a crash; accessibility
  labels name the host ("… on youtube.com"), never a raw URL.

---

## UI / UX

Material 3, light + dark (`isSystemInDarkTheme()`), custom "spice" palette with the warm
`surfaceContainer*` tonal steps defined for both themes. **Dynamic colour is intentionally off**
so the app has one consistent identity for review; easily re‑enabled.

Accessibility: 48dp touch targets (favourite buttons are separately‑focusable `Button`s with
"Add/Remove *name* to favourites" labels); `contentDescription` on icon buttons and images;
`liveRegion` on counts and state views; text uses `MaterialTheme.typography` (scales with system
font size); `clearAndSetSemantics` on ingredient rows so a screen reader reads
"2 large, sliced Onion" as one node; no raw URLs as labels.

Iconography: the nine glyphs used are inline `ImageVector`s (`TasteIndiaIcons`, Material Symbols
path data) — `material-icons-core` is frozen at 1.7.8 and no longer managed by the Compose BOM,
so pinning it would mean a stale, mismatched artifact.

---

## Testing

**61 tests, deterministic, no network** — fixtures / in‑memory fakes / MockWebServer only.

**Unit (`testDebugUnitTest`, 54)**

| Suite | Covers |
| --- | --- |
| `IngredientNormalizationTest` (7) | **required #1** — ingredient/measure normalization + tag splitting, `lookup_normal` / `lookup_missing_fields` (NBSP) / `lookup_minimal` fixtures |
| `IndianBoundaryIntersectionTest` (7) | **required #2** — category/ingredient ∩ Indian base, no‑overlap → empty set, partial‑row drop, base fetched once |
| `RecipeFilteringTest` (8) | pure `applyFilters` — AND‑combination, trimmed case‑insensitive search, unresolved‑filter passthrough, sort, base‑list intersection |
| `LatestFilterStateWinsTest` (4) | **required #3** — a 1s category never overrides a faster later one (asserts it never reached the UI); only the final query in a burst applies |
| `RecipesStateRestorationTest` (3) | **required #4 (restoration)** — `SavedStateHandle` round‑trip rebuilds search/category/favourites‑only/sort and the exact filtered list |
| `FavouritesRepositoryImplTest` (3) | **required #4 (persistence)** — toggle add/remove, observe, idempotency |
| `FavouritesViewModelTest` (4) | resolve against base set, empty, live un‑favourite, unresolved id |
| `DetailsViewModelTest` (3) | load by id, favourite reflect/toggle, `NotFound` |
| `MealDetailCacheTest` (5) | memory/Room cache, write‑through, concurrent de‑dup, `NotFound` |
| `NetworkErrorsTest` (6) | the exception → `AppError` table |
| `MealApiReliabilityTest` (6) | **real** OkHttp + Retrofit + kotlinx‑serialization vs MockWebServer — empty → success, 500 → `Http(500)`, malformed → `Serialization`, slow → `Timeout`, offline → `Network` (0 requests reach the wire) |

**Instrumented (`connectedDebugAndroidTest`, 7)** — real Room:

| Suite | Covers |
| --- | --- |
| `FavouriteMealDaoTest` (3) | insert/exists/delete, most‑recent‑first, IGNORE‑on‑conflict |
| `FavouritesPersistenceTest` (2) | **required #4** — write → close DB (as process death) → reopen from file → rows (and removals) persist |
| `CachedMealDetailDaoTest` (2) | absent → null, upsert REPLACE |

Fixtures: `filter_indian`, `filter_indian_partial`, `filter_category_seafood`,
`filter_ingredient_chicken`, `filter_empty`, `filter_malformed`, `lookup_normal`,
`lookup_missing_fields`, `lookup_minimal`.

---

## Final audit — requirements checklist

Legend: **PASS** = implemented and tested (unit and/or on the emulator via Android MCP).

### Tech stack
| Requirement | Status | Notes |
| --- | --- | --- |
| Kotlin, Jetpack Compose, Material 3 | PASS | |
| Navigation Compose | PASS | type‑safe routes |
| Coroutines, StateFlow | PASS | |
| Retrofit + OkHttp | PASS | Retrofit 3, OkHttp 5 |
| kotlinx.serialization | PASS | codegen, no reflection |
| Room for favourites | PASS | + detail cache |
| Lightweight DI | PASS | hand‑written `AppContainer` |
| ViewModels, Repository pattern | PASS | 3 ViewModels, 2 repositories behind interfaces |
| Only TheMealDB v1 API | PASS | 4 endpoints, no `search.php`, no `list.php` |

### Core features
| # | Requirement | Status | Notes |
| --- | --- | --- | --- |
| 1 | Recipes screen: `LazyColumn`, image + name + favourite + stable `idMeal` key | PASS | no category on rows |
| 2 | Search by name, safe vs rapid typing / stale results, local‑first | PASS | debounce + local filter over base set |
| 3 | Filters: category, main ingredient, favourites‑only | PASS | |
| 3 | Sort A–Z / Z–A | PASS | segmented buttons |
| 3 | Active filters, result count, empty state, Clear all | PASS | removable chips + live count |
| 4 | Category/ingredient intersect with Indian base IDs; boundary can't be escaped | PASS | enforced in `MealRepository`; unit‑tested |
| 5 | Navigate by meal ID only; never pass `Meal` object | PASS | `Destination.Details(mealId)` |
| 5 | Details show every non‑empty field (hero, name, category, area, ingredient/measure, instructions, tags, source, video) | PASS | conditional rendering |
| 5 | Normalize `strIngredient/Measure` into clean `Ingredient`; trim; omit empty | PASS | unit‑tested incl. NBSP |
| 6 | Favourite from list and from details | PASS | |
| 6 | Persist favourite IDs with Room; work offline | PASS | instrumented persistence test |
| 6 | Separate Favourites destination; opens same ID‑based Details | PASS | |
| 7 | Recipes→Details→Back restores search / category / ingredient / favourites‑only / sort / list position | PASS | ViewModel scoped to back‑stack entry + `SavedStateHandle` + hoisted `LazyListState`; verified on emulator + unit test |
| 8 | Deliberate states: loading, success, empty, API error, timeout, offline, retry, partial data, image failure | PASS | see *Reliability*; offline is instant |
| 8 | Never leave the user on an infinite spinner | PASS | |
| 9 | Explicit HTTP status handling, timeouts, cancellation, safe URL construction | PASS | |
| 9 | No network requests inside composable bodies | PASS | all in ViewModel coroutines |
| 10 | No uncontrolled detail requests per visible row | PASS | list makes zero detail calls |
| 10 | Repository enrichment, caching, dedup, bounded concurrency | PASS | memory+Room cache, shared `Deferred`, `Semaphore(4)` |
| 10 | Never launch network work from `LazyColumn` item rendering | PASS | |

### Architecture
| Requirement | Status |
| --- | --- |
| `data` / `domain` / `presentation` / `navigation` / `di` layout | PASS |
| DTOs / domain / UI state / persistence models kept separate | PASS |
| Immutable UI state; ViewModels expose immutable `StateFlow` | PASS |
| Lifecycle‑aware collection in Compose | PASS |
| No Activity / NavController / mutable UI in ViewModels | PASS |
| No giant Activity; no single god ViewModel | PASS |

### Testing
| Requirement | Status |
| --- | --- |
| Ingredient/measure normalization test | PASS |
| Indian + category/ingredient intersection test | PASS |
| Latest search/filter state wins test | PASS |
| Favourite persistence / navigation restoration test | PASS |
| Deterministic fixtures, no public API | PASS |
| Fixtures: normal / missing fields / ingredients / image / tags / instructions | PASS |

### UI/UX
| Requirement | Status |
| --- | --- |
| Light + dark theme | PASS (verified on emulator) |
| Accessible touch targets, screen‑reader semantics, scalable text | PASS |
| Clear loading / useful empty / useful error states | PASS |
| Accessible favourite buttons; descriptive source/video links; no raw URLs as labels | PASS |
| Clean hierarchy, spacing, consistent cards, polished details | PASS |

### Build / quality gates run for this audit
| Gate | Result |
| --- | --- |
| `./gradlew clean assembleDebug` | **SUCCESS** |
| `./gradlew testDebugUnitTest` | **54 passed, 0 failed** |
| `./gradlew connectedDebugAndroidTest` | **7 passed, 0 failed** |
| `./gradlew lintDebug` | **0 errors, 9 warnings** (all "newer version available" — deliberate pins) |
| Emulator smoke test (Pixel 8, API 37, Android MCP) | list, search, all filters, sort, details, back‑state, favourites, offline+retry, light+dark — all OK |

**NEEDS REVIEW:** none.

---

## Assumptions

- The Indian collection is small (~15 meals). Local search / filter is instant, so no
  server‑side search is needed.
- Users only favourite meals they can see, i.e. Indian meals from the list or their detail
  screen. The Favourites screen resolves ids from the base set, then the detail cache — an id
  that resolves to neither (offline + never viewed) is shown as an "unresolved" count.
- `filter.php?a=India` is the correct current query for "the Indian collection" (see API note).
- Category/ingredient filter values are a curated fixed list, since the list endpoints are out
  of scope and the base response has no category data.
- One Gradle module is proportional for this size; no `:core` / `:feature` split.

## Trade‑offs

| Decision | Upside | Downside |
| --- | --- | --- |
| Hand‑written DI | tiny, no codegen, fully readable | no compile‑time graph checks; manual wiring |
| Local‑only search | instant, never stale, one intersection fewer | can't find Indian meals absent from `a=India` |
| Detail cache = raw JSON blob in Room | no TypeConverters, one mapping site | not queryable by field (never needed) |
| Curated filter option lists | no extra endpoints, no per‑row enrichment | options may not all match Indian meals |
| `WhileSubscribed(5s)` on `stateIn` | frees upstream when screen is away | a >5s detour re‑runs `combine` on return (from cache, no network) |
| Kotlin pinned to 2.3.20 | matches Gradle's own Kotlin, exact KSP pairing, reads the 2.4.x stdlib the libs pull | lint flags newer 2.4.x plugins as available |
| Enrichment not done for list rows | zero N+1, matches "don't show category until available" | list rows can't show category/area (by design) |

## Known issues

- **`filter.php?a=Indian` is empty upstream** — worked around with `a=India` (documented above).
- **Lint: 9 "newer version available"** — `compose-bom 2026.09.00`, Kotlin plugins 2.4.20,
  coroutines 1.11.0, some AndroidX test libs. Left on the pinned toolchain deliberately
  (KSP 2.3.12 pairs with Kotlin 2.3.20; a Kotlin bump forces a matched KSP bump). Not blocking.
- **Release build:** `minifyEnabled` / R8 is off (`optimization { enable = false }` from the
  template). The app ships as debug for review; a release pass would enable R8 with keep rules
  for kotlinx.serialization + Room.
- **No pull‑to‑refresh** — retry is via the error‑state button and the Filters sheet; a
  `PullToRefreshBox` on the list would be a small, natural addition.
- **Details screen has no dedicated `SavedStateHandle` restoration** beyond the meal id in the
  route (it re‑loads from cache instantly, so there is nothing else to restore).
- **`data_extraction_rules.xml` / `backup_rules.xml`** are the template defaults; Auto Backup
  would include the favourites DB, which is the desired behaviour, but the rules haven't been
  reviewed field‑by‑field.

## Time spent

Roughly **9–11 hours** equivalent effort: ~1h inspection & planning, ~1h dependency/toolchain
resolution (AGP 9 built‑in Kotlin vs the libraries' stdlib), ~1.5h data layer + error handling,
~1.5h recipe list + image loading, ~2h search/filters/sort + Indian‑boundary logic, ~1h details
+ ID navigation, ~1h favourites, ~1h reliability hardening, ~1.5h tests, ~1h audit + docs.
Delivered as 10 incremental commits.

## AI / tool‑use disclosure

This project was implemented **with AI assistance (Claude, in an agentic coding CLI)** across the
10 phases in `PROMPT`/the task brief. See [`AI_DISCLOSURE.md`](AI_DISCLOSURE.md) for what the AI
did, what was human‑directed, and which decisions to be ready to explain in review.
