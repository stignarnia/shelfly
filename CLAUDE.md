# Project Guidelines & Instructions

## Git Commit Guidelines

### Format
Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <concise description>

[optional body: direct and factual bullet points of changes]
```

### Allowed Types
- `feat:` A new feature
- `fix:` A bug fix
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `style:` Changes that do not affect the meaning of the code (white-space, formatting, etc.)
- `perf:` A code change that improves performance
- `test:` Adding missing tests or correcting existing tests
- `chore:` Changes to the build process, tooling, dependencies, or auxiliary tools
- `ci:` Changes to CI configuration files and scripts (e.g. GitHub Actions)
- `docs:` Documentation only changes

### Rules for Commit Messages
- **Always use standard commit prefixes** (`feat:`, `fix:`, `refactor:`, etc.) — never omit them.
- **Direct & concise subject lines**: Use imperative mood (e.g. `refactor(ui): consolidate modal dialogs into ModalView`).
- **NO AI fluff, storytelling, or rambling meta-commentary**:
  - Do NOT write essays explaining thought processes, conversational filler, or self-absorbed post-mortems (e.g. "ebd1e35a read the ask wrong...").
  - Do NOT write narrative stories about what prompt or ask was misinterpreted.
- **Keep descriptions factual, short, and technical**: State what is added, removed, or changed.

### Examples

#### ❌ BAD (Do NOT do this):
```
Put every modal on the "?" popup's own component

ebd1e35a read the ask wrong. It kept MaterialAlertDialogBuilder and restyled
@style/AlertDialog to resemble the tip popup, down to a tween interpolator
standing in for that popup's spring. The result was two modal implementations
agreeing by value - two scrims, two surface colours, two entrance curves -
where one was wanted.

TipOverlayView is now ModalView and is the only modal in the app...
[...followed by 4 more paragraphs of verbose rambling AI essay...]
```

#### ✅ GOOD:
```
refactor(ui): replace MaterialAlertDialogBuilder with unified ModalView

- Rename TipOverlayView to ModalView and consolidate modal implementations
- Replace MaterialAlertDialogBuilder dialogs across all fragments
- Add secondary button support and custom content slots to ModalView
- Handle dismiss on outside scrim tap and hardware back button
```

---

## Comment Formatting Rules

- **Sentence-per-line (Newline on period)**: Every sentence in comments must be on its own line ending with a period (`.`).
- **No arbitrary mid-sentence hard-wrapping**: Do NOT break lines in the middle of sentences to fit an arbitrary character width limit (e.g. 80/100/120 columns). Rely on editor soft-wrapping (Alt+Z / word wrap) instead.
- **Applies to ALL file types**: Kotlin (`.kt`), Gradle (`.gradle`, `.kts`), YAML (`.yml`), XML (`.xml`), R8 / ProGuard (`.pro`), Shell (`.sh`), properties, etc.
- **Preserve comment structure**: Maintain comment prefixes (`//`, `#`, ` * `, `<!--`), list items, doc tags (`@param`, `@return`), URLs, code snippets, and paragraph spacer lines.

---

## Development & Build Commands

- **Run ktlint**: `./ktlint`
- **Run Unit Tests**: `./gradlew testDebugUnitTest` (or a single module: `./gradlew :app:testDebugUnitTest`)
- **Build Release APK**: `./gradlew :app:assembleRelease`
- **Build Debug APK**: `./gradlew :app:assembleDebug`
- **Install on a connected device**: `./gradlew :app:installDebug`

---

## Verification

No single command catches everything, and the full sweep takes minutes.
**Match the command to the change** rather than running everything on every edit or trusting unit tests alone.

Each tier below is additive: it assumes the tiers above it also ran.

### Tier 1 - any Kotlin change

```
./ktlint && ./gradlew testDebugUnitTest
```

`ktlint` is a self-executing jar, not a Gradle plugin.
No Gradle task runs it, so `check` will never catch a formatting violation - it has to be invoked separately.

### Tier 2 - resources, layouts, manifest, strings

```
./gradlew lintDebug
```

Android Lint is the only thing that checks XML, translations, and accessibility.
The Kotlin compiler cannot see any of it.

`lint.checkTestSources` is on in the root `build.gradle`, so `test/` and `androidTest/` sources are linted too - they are skipped by default.
It does not guard against test sources failing to *compile*, which is a Kotlin error rather than a Lint finding - that is what Tier 3 covers.

### Tier 3 - Room entities, DAOs, migrations

```
./gradlew :data-local:connectedDebugAndroidTest
```

Requires a connected device or emulator.
`data-local`'s `androidTest` source set is the only real-database coverage in the repo, and nothing in `check` or CI compiles it - it silently rots.
When no device is attached, run `./gradlew :data-local:assembleDebugAndroidTest` so the sources cannot drift out of compiling.
It takes about 15 seconds and is a strict superset of `compileDebugAndroidTestKotlin`: it also dexes, merges the test manifest, and runs the duplicate-class and AAR metadata checks, none of which the compile task reaches.

### Tier 4 - dependencies, R8 rules, anything reflective

```
./gradlew :app:assembleRelease
```

Unit tests and debug builds never run R8.
Keep rules for Room, Hilt, Moshi, and WorkManager are only exercised here, and a missing one fails at runtime rather than at compile time.

### Everything - before tagging a release

```
./ktlint && SHELFLY_V2_BACKUP=/path/to/showly_export.json ./gradlew \
  clean \
  testDebugUnitTest \
  lintDebug \
  connectedDebugAndroidTest \
  :app:assembleRelease \
  :app:assembleDebug \
  --warning-mode all
```

`clean` is what forces every task - and every Lint SARIF report - to regenerate.
Without it, Lint tasks go `UP-TO-DATE` and the reports on disk are from a previous run.
Do not add `--rerun-tasks` or `--no-build-cache` on top of `clean`; they are redundant.

**Never pass `--no-configuration-cache`.**
`gradle.properties` enables the configuration cache, so disabling it *removes* a check and tests a configuration that no ordinary build uses.

**Never run `./gradlew test`.**
It is the lifecycle task, so it runs the release unit test variant on top of the debug one - 1465 tasks against 732, for the same result.
Unit tests never run R8, so the release variant only re-executes the same sources.
`testDebugUnitTest` is what CI runs and what you should run.

**Prefer `testDebugUnitTest lintDebug` over `check`.**
`check` pulls in `testReleaseUnitTest` as well, which is the same duplication as `./gradlew test`.

### Enforced conventions

Two of the rules in this file are checked by tooling rather than by review.

- **Conventional Commits**: `scripts/hooks/commit-msg` rejects any subject that is not `<type>(<scope>): <description>` with a type from the list above. Merges, reverts and rebase scratch commits are left alone, and `--no-verify` bypasses it. Enable it once per clone with `git config core.hooksPath scripts/hooks`.
- **Release notes**: `scripts/check-release-notes.sh` fails when the first line of `release_notes.txt` is not `Shelfly <versionName>` from `versions.gradle`, or when the heading has no notes beneath it. Run it before tagging.
- **Translation completeness**: Android Lint's `MissingTranslation` is error severity, so `lintDebug` already fails when a string is added to `values/strings.xml` without reaching every other locale. This needs no extra tooling - it is why the localization rule holds.

What none of them check is *content*: a commit can carry a valid prefix and still ramble, a release note can exist without describing the change that shipped, and a translation can be present but wrong.
Those stay review-time concerns.

The comment formatting rules are **not** enforced by anything.
`ktlint` does not read comment prose, so sentence-per-line remains a review-time concern.

### What a green run still does not prove

Two suites are opt-in and skip themselves, so a green run does not mean they ran:

- `data-remote`'s `TmdbLiveApiTest` calls the real TMDB API. The key comes from `tmdbApiKey` in `local.properties` and is compiled into the **debug** BuildConfig, so it does run locally under `testDebugUnitTest` and skips only on CI. It has caught real mapping defects.
- `ui-backup`'s `BackupMigrationV2FileTest` skips unless `SHELFLY_V2_BACKUP` points at a real v2 export.

**Nothing above runs the app.**
There are no UI or integration tests in the repo, so runtime behaviour is only ever verified by installing on a device.
Debug builds stamp epoch seconds into `versionName` (`4.0.6-debug-<stamp>`) so the installed build can be told apart from the previous one.

### Warnings: two separate systems

| System | Setting | State |
| --- | --- | --- |
| Kotlin compiler | `allWarningsAsErrors` in the root `build.gradle` | **On.** The tree compiles warning-free; any new warning fails the build. |
| Android Lint | `lint.warningsAsErrors` | **Off.** ~420 warnings outstanding. |

These are unrelated knobs.
`allWarningsAsErrors` has no effect on Lint, and `--warning-mode all` is a third thing again - it only surfaces deprecated *Gradle API* usage, not Kotlin or Lint warnings.

Lint's `abortOnError` is on, but it only fails the build on **error** severity.
All outstanding findings are warnings, so `lintDebug` passes while reporting them.
There is no command-line property that changes this - making Lint warnings fail requires `lint { warningsAsErrors = true }` in the build file, ideally with a `baseline` so existing findings are grandfathered.

**Do not enable `lint.checkAllWarnings`.**
It switches on every check that is off by default, which are overwhelmingly stylistic.
Measured on `ui-model`, it took that module from 1 finding to 95 - the extra 94 being `DuplicateStrings` and `TypographyQuotes`, which fire constantly on a localized app and bury the findings that matter.
To pick up a specific off-by-default check, name it in `lint.enable` instead.

---

## Localization & Strings

- **Always update all languages**: When adding, modifying, or removing string resources, always check and update all locale folders (`res/values-*/strings.xml`) across the modules, not just the default English `res/values/strings.xml`. Ensure consistent and accurate translations across all supported languages.

---

## Release Notes

`app/src/main/assets/release_notes.txt` is shown to users in the What's New screen. It is part of "done", not a release-time chore.

- **Update it with any user-visible change**: new features, fixed bugs, changed behaviour. Purely internal work (refactors, tooling, tests) does not belong there.
- **Keep the heading in sync with the version**: the first line is `Shelfly <versionName>`, matching `versions.gradle`. When the version is bumped, start a fresh list under the new heading.
- **Write for users, not for the diff**: one `•` bullet per change, describing what is different in the app - not which class changed.

