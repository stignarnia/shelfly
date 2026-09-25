# Privacy Policy for Shelfly

**Effective Date:** September 25, 2026

Shelfly is an open-source TV show and movie tracking application designed with privacy as a foundational principle. This Privacy Policy explains how Shelfly handles your information.

## 1. Zero Data Collection & Tracking

Shelfly does **not** collect, store, record, transmit, or sell any personal information, usage analytics, telemetry, or crash data to the developers or any third-party advertising or analytics networks.

- No user accounts with Shelfly developers are required or supported.
- There are no advertisements.
- There are no third-party tracking or analytics SDKs embedded in the app.

## 2. Network Services & Third Parties

Shelfly interacts directly with network services solely to provide app functionality requested by you:

### The Movie Database (TMDb)
Shelfly uses the TMDb API to provide movie, TV show, and actor information, posters, artwork, and search functionality.
- Requests are made directly from your device to TMDb servers using either the default API key or an API key you provide.
- These requests transmit only the search queries or IDs necessary to retrieve media metadata.
- Shelfly is not endorsed or certified by TMDb. For information regarding TMDb's data practices, please consult [TMDb's Privacy Policy](https://www.themoviedb.org/privacy-policy).

### WebDAV Synchronization & Backup (Optional)
If you configure WebDAV sync or backup in Shelfly:
- Your watch history, lists, and settings are backed up directly to your own self-hosted or designated WebDAV server (such as Nextcloud, ownCloud, or a NAS).
- Your WebDAV server credentials (URL, username, and password) and backup files are stored locally on your device and transmitted directly and exclusively between your device and your specified WebDAV server.
- The developers of Shelfly never have access to your server, credentials, or backup data.

## 3. Device Permissions

Shelfly requests only the permissions necessary for local features:
- **Internet Access (`android.permission.INTERNET`)**: Required to query TMDb for media metadata and connect to your chosen WebDAV server.
- **Post Notifications (`android.permission.POST_NOTIFICATIONS`)**: Optional, used solely on your device to notify you of upcoming episode premieres or releases you have chosen to track.
- **Run at Startup / Foreground Service (`RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`)**: Used locally on device to schedule and run background WebDAV sync tasks and notification alarms.

## 4. Children's Privacy

Shelfly does not collect any personal information from anyone, including children under the age of 13.

## 5. Changes to This Policy

If this Privacy Policy is updated, the revised version will be published in the public source repository with an updated effective date.

## 6. Contact & Source Code

Shelfly is free and open-source software licensed under the GNU General Public License v3.0 (GPL-3.0).

- **Source Code:** [https://github.com/stignarnia/shelfly](https://github.com/stignarnia/shelfly)
- **Issue Tracker:** [https://github.com/stignarnia/shelfly/issues](https://github.com/stignarnia/shelfly/issues)
