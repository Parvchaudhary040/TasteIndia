# AI / tool‑use disclosure

Per the assignment's "IMPORTANT AI OWNERSHIP RULE": AI involvement is **not** hidden. This
document states what was AI‑assisted, what was human‑directed, and the architectural decisions
the candidate should be able to explain and defend.

## Tools used

- **Claude (Anthropic), driven through an agentic coding CLI.** It read the existing project,
  wrote the Kotlin/Gradle code, wrote the tests, ran Gradle, and drove an Android emulator
  through a device‑automation (MCP) tool to install the app, take screenshots and inspect the
  view hierarchy after each UI phase.
- **Gradle 9.6 / AGP 9.4 / Android SDK 37**, JUnit4, MockWebServer, a Pixel 8 (API 37) emulator.
- The live TheMealDB API was queried a handful of times **during development only** (to discover
  the `a=Indian` → `a=India` data change and to pick realistic filter option values). The
  shipped tests never touch it.

## How the work was structured

Ten phases, each: inspect → implement one slice → build → test → verify on the emulator →
explain → **one meaningful commit**. The commit history (`git log`) is the record:

```
chore: initialize TasteIndia Android project        (pre‑existing template)
feat: add project architecture and dependencies
feat: add TheMealDB API layer
feat: implement Indian recipe list
feat: add search, filters and sorting
feat: add recipe details
feat: add favourites persistence
fix: improve error and image failure handling
test: add deterministic repository and filter tests
docs: add architecture and AI disclosure
```

## What was AI‑assisted vs human‑directed

- **Human‑directed:** the requirements, the phase‑by‑phase process, the "explain every decision"
  constraint, "no unnecessary libraries / abstractions", and review of each phase before the
  next.
- **AI‑produced:** essentially all of the Kotlin and test code, the Gradle wiring, and this
  documentation, following those instructions.
- **AI judgement calls that a reviewer should scrutinise** (all are explained in `README.md`):
  - **`a=India` instead of `a=Indian`.** The brief's exact query returns `{"meals":null}` now;
    the AI verified this against the live API and switched, with a code comment and a README
    note. *Alternative:* keep `a=Indian` and ship an empty app — rejected.
  - **Hand‑written `AppContainer` DI** instead of Hilt. *Alternative considered:* Hilt — rejected
    as heavier than the problem, extra KSP processor.
  - **Local‑only search**, `search.php` unused. *Alternative:* server search with debounce +
    `flatMapLatest` — rejected because it needs another Indian‑boundary intersection and
    reintroduces stale responses; the brief says "prefer local filtering".
  - **Indian boundary enforced in the repository** (no method returns un‑intersected ids)
    rather than in the ViewModel. Chosen so no future screen can bypass it.
  - **Curated `FilterOptions` lists** because `list.php` is outside the allowed endpoints and
    the base response has no category data.
  - **Kotlin 2.3.20 via a `buildscript` classpath.** AGP 9's built‑in Kotlin defaults to 2.2.10,
    which can't read the `kotlin-stdlib:2.4.10` that Coil 3.6 + the Feb‑2026 Compose BOM pull;
    AGP 9 also forbids applying `org.jetbrains.kotlin.android`. The documented fix is a newer
    KGP (+ matching KSP 2.3.12) on the buildscript classpath.
  - **Inline `ImageVector` icons** instead of `androidx.compose.material:material-icons-core`
    (frozen at 1.7.8, unmanaged by the BOM).
  - **Detail cache stored as a raw DTO JSON blob** in Room rather than exploded columns.
  - **`WhileSubscribed(5_000)`** stop timeout on every `stateIn`.

## No hidden generated code

There is no generated code the candidate cannot explain. The only code generation in the build
is **Room's DAO/schema** (via KSP) — standard, from the `@Dao`/`@Database` annotations — and the
`BuildConfig` class. No Hilt, no Moshi/Gson codegen, no Parcelize.

## Ownership

The candidate is expected to review the implementation and be able to explain every important
architectural decision above. This file exists so that review starts from an honest baseline.
