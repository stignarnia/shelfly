# Shelfly

> **Heads up:** this fork was built almost entirely by an AI coding agent, with a human directing it and reviewing the results. Read the code before you trust it with anything you care about, and keep your own backups.

Shelfly is an offline-first TV shows and movies tracker for Android.

Everything you track lives on your own device. There is no account, no sync service, no analytics and no crash reporting: your collection, watch history, ratings and lists are yours, and the only way they leave the phone is a backup you ask for.

The catalog comes from [TMDB](https://www.themoviedb.org), using an API key you supply yourself on first run. No keys are compiled into the APK.

## Privacy

The app ships with no telemetry of any kind. There is no Firebase, no Google Play Services, no analytics SDK and no crash reporter anywhere in the dependency graph, and the app generates no device identifier. The only network calls it makes are to TMDB for the catalog, to OMDB if you supply an optional key for IMDb ratings, and to whatever WebDAV server you point it at.

## Differences from upstream

- Trakt is gone entirely. TMDB is the only catalog source and all tracking state is local.
- Backups can go to your own WebDAV server on a schedule, in addition to a local folder.
- API keys are supplied by the user at runtime rather than compiled into the build.
- The paid tier has been removed.
- Episode notifications fire on the air date rather than at the exact airtime. TMDB exposes a date but no time of day, so the precision is not available.
- Discover has no network filter. TMDB's `with_networks` needs numeric ids that do not map from the channel names the app knows. Genre filtering works.
- The app is dark-only. The theme picker exists but offers a single option until a light palette is written.
- The launcher icon and in-app logo are still upstream artwork, pending a replacement.

## Project setup

1. Clone the repository and open it in a recent Android Studio.
2. Create `app/keystore.properties` with any values for a debug build:

   ```ini
   keyAlias=github
   keyPassword=github
   storePassword=github
   ```

3. Optionally add API keys to `local.properties` in the project root. These only prefill debug builds; release builds ship without keys and ask the user for their own on first run:

   ```ini
   tmdbApiKey="your tmdb api key (v3 auth)"
   omdbApiKey="your omdb api key"
   ```

   Get them from [TMDB](https://www.themoviedb.org/settings/api) and [OMDB](https://www.omdbapi.com/apikey.aspx). Both are free. OMDB is optional and only supplies IMDb ratings.

4. Build and run.

### Verifying a change

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :repository:testDebugUnitTest \
  :ui-discover:testDebugUnitTest :ui-statistics:testDebugUnitTest \
  :ui-statistics-movies:testDebugUnitTest :ui-search:testDebugUnitTest \
  :ui-progress-movies:testDebugUnitTest :data-remote:testDebugUnitTest \
  :ui-backup:testDebugUnitTest
./ktlint
```

Two suites are opt-in and self-skip, so a green run does not mean they ran. `TmdbLiveApiTest` in `:data-remote` hits the real TMDB API and skips when no key is compiled in. `BackupMigrationV2FileTest` in `:ui-backup` skips unless `SHELFLY_V2_BACKUP` points at a real Showly export.

## Importing from Showly

Shelfly reads Showly's backup files. Export from Showly, then use Settings → Backup & Restore → Import data.

Older backups are keyed by ids from a catalog source this fork no longer uses, so they are re-keyed onto TMDB ids on the way in. Anything that cannot be matched is dropped and counted, and the totals are shown when the import finishes — the numbers are never silently wrong.

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB.

Streaming availability data is provided by JustWatch.

## Licence

Shelfly is a fork of [Showly](https://github.com/trakt/showly), created by Michał Drabik and now maintained by Trakt. It is used here under the GNU General Public License v3.0, and Shelfly remains GPL-3.0. See [LICENSE](LICENSE).

Copyright (C) Michał Drabik and Trakt — original Showly work.

Copyright (C) 2026 stignarnia — modifications in this fork.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
