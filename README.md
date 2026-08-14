# Shelfly

> **Heads up:** this fork was built almost entirely by an AI coding agent, with a human directing it and reviewing the results. Read the code before you trust it with anything you care about, and keep your own backups.

Shelfly is a fork of [Showly](https://github.com/trakt/showly) (a TV shows and movies tracker for Android) that syncs to infrastructure you control.

## Install

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="54">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22xyz.stignarnia.shelfly%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fstignarnia%2Fshelfly%22%2C%22author%22%3A%22stignarnia%22%2C%22name%22%3A%22Shelfly%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Shelfly%5C%22%7D%22%7D)

Obtainium tracks this repository's GitHub releases and updates the app as new ones are published. You can also grab the APK straight from the [latest release](https://github.com/stignarnia/shelfly/releases/latest).

## Why this exists

Showly kept your watch history on Trakt, but they now charge for API access, so downloading your history via an automatic script becomes paid. You can still do so manually via Showly's export feature but I didn't like the move and decided to fix it myself.

Shelfly replaces Trakt sync with **WebDAV backup to your own server**. Point it at a Nextcloud instance, a NAS, an online storage service or anything that speaks WebDAV, and the app backs your collection up there on a schedule and restores from it. There is no account, no subscription and no middleman: your history lives on your device and on hardware you already run.

Showly could also do scheduled exports, but only via the Android Storage Framework, which means it could only reliably do so to the phone itself, not a network resource.

The catalog comes from [TMDB](https://www.themoviedb.org) instead, using a free API key you supply yourself on first run. No keys are compiled into the APK.

## Why not a Pull Request instead?

I think there is little chance this gets merged upstream given the repository's acquisition by Trakt itself. If this gets traction and they are willing to merge the WebDAV backup feature I will probably go back to the original, as removing the Trakt dependency was more necessary for me to not have to buy VIP during development than for the user facing feature I wanted to add.[Showly](https://github.com/trakt/showly)

## Differences from upstream

- **Trakt is gone entirely.** TMDB is the only catalog source and all tracking state is local.
- **WebDAV backup.** Scheduled backup and restore against your own server, alongside the existing local folder target.
- **You supply the API keys**, at runtime, rather than the build shipping someone else's.
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
  :ui-backup:testDebugUnitTest :data-webdav:testDebugUnitTest
./ktlint
```

Two suites are opt-in and self-skip, so a green run does not mean they ran. `TmdbLiveApiTest` in `:data-remote` hits the real TMDB API and skips when no key is compiled in. `BackupMigrationV2FileTest` in `:ui-backup` skips unless `SHELFLY_V2_BACKUP` points at a real Showly export.

## Backing up to WebDAV

Set the server up in Settings → Backup & Restore → WebDAV server: the URL of the folder backups go into, plus credentials. "Test connection" tells you specifically what is wrong — wrong password, folder not found, rejected certificate, unreachable host — rather than just failing. The folder is created for you if the parent allows it.

Saving a server does not start using it. Pick it under "Backup destination", which appears once a server is configured, then choose a cadence under Export data. Backups are verified by reading them back and parsing them before the timestamp is recorded, and the five newest are kept.

Pulling down on the progress list runs a backup immediately.

To restore, use Import data → Import from WebDAV and pick which backup you want.

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
