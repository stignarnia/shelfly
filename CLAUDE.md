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
./ktlint && ./scripts/check-format.sh && ./gradlew testDebugUnitTest
```

`ktlint` is a self-executing jar, not a Gradle plugin.
No Gradle task runs it, so `check` will never catch a formatting violation - it has to be invoked separately.

`check-format.sh` checks CRLF, hard tabs, and trailing whitespace across the tracked text files ktlint cannot read: Gradle scripts, resources, manifests, workflows, R8 rules, shell, properties, the version catalog, JSON, Markdown, the fastlane metadata, `.editorconfig`, `.gitignore` and `LICENSE`.
Binaries are excluded because the rules are meaningless for them, and `gradlew` / `gradlew.bat` because Gradle regenerates them.
It is a whitespace check rather than a formatter: it will not reindent XML or wrap long lines.
`insert_final_newline` is declared in `.editorconfig` but deliberately not enforced, because 335 files would fail it today.

### Tier 2 - resources, layouts, manifest, strings

```
./gradlew lintDebug && ./scripts/check-config.sh
```

Android Lint is the only thing that checks XML, translations, and accessibility.
The Kotlin compiler cannot see any of it.

**The build scripts are Kotlin DSL because Lint will not read Groovy ones.**
AGP 9 only parses `.gradle.kts`, so while the scripts were `.gradle` no Gradle-DSL check ran anywhere in the tree - `GradleDynamicVersion` on a literal `31.+` produced nothing, and the same dependency in a `.kts` file reports it immediately.
That is worth knowing before anyone converts a build file back for convenience: it silently removes a whole category of checking.
One gap survives the migration - Lint scans library module build scripts but not the application module's, so nothing in `app/build.gradle.kts` is checked, and any Gradle DSL check that reasons about it cannot pass.

`check-config.sh` covers two things Lint cannot see.

The first is unreferenced resources.
`UnusedResources` only reports meaningfully in an application module, because a library's resources may be used by any consumer Lint cannot see - and 28 of the 29 modules here are libraries, so almost the whole resource set falls outside what Lint will judge.
viewBinding hides the remainder: it generates a binding class per layout, which Lint counts as a use, so an orphaned layout looks alive no matter how long nothing has inflated it.
Resolving references across every module at once is sound here precisely because the module graph is closed - nothing outside this repository consumes these resources.
Deleting one resource can orphan whatever it referenced, so the check is worth re-running until it passes rather than once.

The second is the one localization mistake Lint cannot see.
`resourceConfigurations` in `app/build.gradle.kts` pins which locales survive into the APK, so adding `res/values-nb` without adding `nb` to that list strips the translation at build time - and `MissingTranslation` stays quiet, because it only reasons about locales that are already configured.
A complete, correct, silently discarded translation looks exactly like a healthy one.

`lint.checkTestSources` is on in the root `build.gradle.kts`, so `test/` and `androidTest/` sources are linted too - they are skipped by default.
It does not guard against test sources failing to *compile*, which is a Kotlin error rather than a Lint finding - that is what Tier 3 covers.

### Tier 3 - Room entities, DAOs, migrations

```
./gradlew :data-local:connectedDebugAndroidTest
```

Requires a connected device or emulator.
`data-local`'s `androidTest` source set is the only real-database coverage in the repo, and nothing in `check` or CI compiles it - it silently rots.

`MigrationsTest` is the part to care about.
The database is built with `fallbackToDestructiveMigration(dropAllTables = true)`, so a migration that throws does not crash - Room drops every table and the user loses their whole library without being told.
A failing test here is the only warning that would ever be given, so never add or change a migration without running this on a device.
It checks the migrated data as well as the schema, and the exported schemas in `data-local/schemas` are what it validates against - they are build output worth committing, not noise.

`scripts/check-schemas.sh` is the companion that needs no device.
It fails when the Room compiler regenerated a schema that was never committed, and - the part that matters - when a schema released in the last tag was edited in place instead of a new version being added.
A shipped schema describes a database that already exists on users' devices, so rewriting one leaves `MigrationsTest` validating against a file that matches nobody.

When no device is attached, run `./gradlew :data-local:assembleDebugAndroidTest` so the sources cannot drift out of compiling.
It takes about 15 seconds and is a strict superset of `compileDebugAndroidTestKotlin`: it also dexes, merges the test manifest, and runs the duplicate-class and AAR metadata checks, none of which the compile task reaches.

### Tier 4 - dependencies, R8 rules, anything reflective

```
./gradlew :app:assembleRelease
```

Unit tests and debug builds never run R8.
Keep rules for Room, Hilt, Moshi, and WorkManager are only exercised here, and a missing one fails at runtime rather than at compile time.

### Everything - before tagging a release

Run in two parts, because the device half is the only one that needs hardware.

```
./ktlint \
  && ./scripts/check-format.sh \
  && ./scripts/check-config.sh \
  && ./scripts/check-release-notes.sh \
  && SHELFLY_V2_BACKUP=/path/to/showly_export.json ./gradlew \
    clean \
    testDebugUnitTest \
    lintDebug \
    :app:assembleRelease \
    :app:assembleDebug \
    --warning-mode all \
  && ./scripts/check-schemas.sh
```

Then, with a device attached:

```
./gradlew :data-local:connectedDebugAndroidTest
```

Keeping them apart matters more than it looks.
In a single `&&` chain the first failure hides every later signal, and `connectedDebugAndroidTest` fails immediately when nothing is plugged in - so an unplugged phone would silently cost you the release build, the debug build and the schema check, none of which need a device.
Splitting also means the long half can run while the phone is elsewhere.

`check-schemas.sh` comes after the build because it reads outputs the build just regenerated; the other three scripts come first because they need no build at all.

`clean` is what forces every task to actually run.
Without it, Lint tasks go `UP-TO-DATE` and nothing is re-checked - a green `lintDebug` then only tells you the *previous* run was green.
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

These rules are checked by tooling rather than by review.

- **Conventional Commits**: `scripts/hooks/commit-msg` rejects any subject that is not `<type>(<scope>): <description>` with a type from the list above. Merges, reverts and rebase scratch commits are left alone, and `--no-verify` bypasses it. Enable it once per clone with `git config core.hooksPath scripts/hooks`.
- **Release notes**: `scripts/check-release-notes.sh` fails when the first line of `release_notes.txt` is not `Shelfly <versionName>` from `versions.gradle.kts`, or when the heading has no notes beneath it. Run it before tagging.
- **Non-Kotlin whitespace**: `scripts/check-format.sh` fails on CRLF, hard tabs, or trailing whitespace across the tracked text files ktlint does not parse, excluding binaries and the generated Gradle wrapper scripts.
- **Version and locale configuration, and unreferenced resources**: `scripts/check-config.sh` fails when a tag on HEAD disagrees with `versionName`, when the locale directories and `resourceConfigurations` disagree in either direction, and when any resource is declared but referenced nowhere in the project.
- **Room schemas**: `scripts/check-schemas.sh` fails on uncommitted schema drift, and on any schema released in the last tag having been modified rather than superseded.
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
There are no UI or integration tests in the repo, so runtime behaviour is only ever verified by installing on a device:

```
./gradlew :app:installDebug && scripts/logcat.sh
```

`logcat.sh` follows the app's own process, so a crash or a swallowed exception is visible without grepping the whole buffer - the app has to be running before it starts.
Debug builds stamp epoch seconds into `versionName` (`4.0.6-debug-<stamp>`) so the installed build can be told apart from the previous one.

### Warnings: two separate systems

| System | Setting | State |
| --- | --- | --- |
| Kotlin compiler | `allWarningsAsErrors` in the root `build.gradle.kts` | **On.** The tree compiles warning-free; any new warning fails the build. |
| Android Lint | `lint.warningsAsErrors` | **On.** The tree lints clean; any new finding fails the build. |

These are unrelated knobs.
`allWarningsAsErrors` has no effect on Lint, and `--warning-mode all` is a third thing again - it only surfaces deprecated *Gradle API* usage, not Kotlin or Lint warnings.

Both live in the root `build.gradle.kts`, in the `subprojects` block keyed on `com.android.base`, so every module inherits them and none can opt out locally.

Lint's `abortOnError` only fails the build on **error** severity, so `warningsAsErrors` is what gives the rest of the checks teeth - it promotes every warning to an error first.
Neither can be set from the command line: there is no `-Plint.warningsAsErrors` and no equivalent flag, in either direction.
Changing this means editing the build file, which makes it a reviewable commit rather than something one person's shell alias quietly turns off.

**Findings get fixed.**
No `lint-baseline.xml`, no `tools:ignore`, no `@Suppress`, no disabling the check that caught it.
The message Lint prints on failure recommends `updateLintBaseline`; that advice does not apply here, because a baseline grandfathers findings in and this tree is at zero.
If a check looks wrong about the code, it is usually right about something adjacent - fix that.

**Do not enable `lint.checkAllWarnings`.**
It switches on every check that is off by default, which are overwhelmingly stylistic.
Measured on `ui-model`, it took that module from 1 finding to 95 - the extra 94 being `DuplicateStrings` and `TypographyQuotes`, which fire constantly on a localized app and bury the findings that matter.
To pick up a specific off-by-default check, name it in `lint.enable` instead.

---

## Localization & Strings

- **Always update all languages**: When adding, modifying, or removing string resources, always check and update all locale folders (`res/values-*/strings.xml`) across the modules, not just the default English `res/values/strings.xml`. Ensure consistent and accurate translations across all supported languages.
- **The locale config is generated, not written**: `androidResources.generateLocaleConfig` is on, so AGP derives the per-app language list from the `values-*` directories and injects `android:localeConfig` into the merged manifest itself. There is deliberately no `res/xml/locales_config.xml` - a hand-written one duplicated the list in `resourceConfigurations` and could drift from it silently, since `check-config.sh` only ever validated the latter. `app/src/main/res/resources.properties` declares which locale the unqualified `values/` folder holds, and the build fails without it.

---

## Release Notes

`app/src/main/assets/release_notes.txt` is shown to users in the What's New screen. It is part of "done", not a release-time chore.

- **Update it with any user-visible change**: new features, fixed bugs, changed behaviour. Purely internal work (refactors, tooling, tests) does not belong there.
- **Keep the heading in sync with the version**: the first line is `Shelfly <versionName>`, matching `versions.gradle.kts`. When the version is bumped, start a fresh list under the new heading.
- **Write for users, not for the diff**: one `•` bullet per change, describing what is different in the app - not which class changed.

