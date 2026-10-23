# PrivacyGuard

An Android app that blocks known trackers, phone telemetry, malware, phishing and scam domains on the whole phone, using a local VPN that filters DNS lookups on the device. No root, no account, no server: nothing leaves the phone.

PrivacyGuard is made for Google Play, which doesn't allow VPN apps to block ads. It never blocks ads: every downloaded list is cleaned of the domains on EasyList, an ads-only list (see [Blocklist sources](#blocklist-sources)).

<p>
  <img src="screenshots/home.png" width="200" alt="Home screen with protection on">
  <img src="screenshots/blocked.png" width="200" alt="Blocked requests list">
  <img src="screenshots/filters.png" width="200" alt="Filters screen">
</p>

Screenshots are from an older version (1.2) that still blocked ads; new ones for the Play listing go in `play-store/screenshots/`.

## Download

PrivacyGuard will be published on Google Play only. Until then, [build it from source](#build-from-source). GitHub releases no longer include an APK; the old `v0`–`v0.2` APKs are test builds signed with the upload key, so a phone that has one installed must uninstall it before installing from Play.

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Install a test build without Android Studio](#install-a-test-build-without-android-studio)
- [Build from source](#build-from-source)
- [Run it on a phone or an emulator](#run-it-on-a-phone-or-an-emulator)
- [Testing](#testing)
- [Project structure](#project-structure)
- [Code walkthrough](#code-walkthrough)
- [Known limits](#known-limits)
- [Publishing on Google Play](#publishing-on-google-play)
- [Blocklist sources](#blocklist-sources)
- [Credits](#credits)
- [License](#license)

## Features

| Feature | Where |
| --- | --- |
| Blocks known trackers, phone maker telemetry, malware, phishing and scam domains from [public lists](#blocklist-sources), in every app and browser. Free and always on | Runs in the background as a local VPN |
| Never blocks ads: domains on EasyList are removed from every list when it is downloaded | `Blocklist.ADS_EXCLUSION_SOURCE` |
| One switch to turn protection on and off, plus a "blocked today" counter | **Home** screen, and a Quick Settings tile |
| Live list of blocked domains, newest first; repeats are grouped ("Blocked 4 times"). Adult domains show as "Adult site blocked" | **Blocked** screen |
| List update on demand, automatically when older than 7 days, and right after an app update that changes the sources | **Filters** screen |
| Public DNS fallback (1.1.1.1 / 8.8.8.8), off by default | **Filters** screen |
| Licences and list credits | **Filters** screen |
| Works offline on first launch (a copy of the free list ships inside the app) | `app/src/main/assets/standard.txt` |
| Follows Wi-Fi ↔ mobile data changes (reads the current network for every lookup) | `UpstreamDns.resolve()` |
| Explains the VPN before Android's permission popup (required by Google Play) | First time the switch is turned on |
| Diagnostics (lookups answered/failed, DNS route, network, last problem) and a **Copy details** button for bug reports | **Home** screen |
| Safety switch: turns itself off and explains why if lookups keep failing for 30 s on the same network | `PrivacyVpnService.giveUp()` |

**Pro** (one-time purchase through Google Play Billing, product `pro_unlock`), on the **Pro** and **Filters** screens:

| Feature | Where |
| --- | --- |
| Filters: adult content, gambling, filter bypass (public DoH/VPN/proxy services), newly registered domains, cryptojacking, stalkerware | `Filter.kt` |
| PIN lock for turning protection off and changing filters (a salted hash is saved, never the PIN) | `PinLock.kt` |
| My rules: block or allow single domains; allow wins over every list | `MyRules.kt` |
| Blocked history for the last 7 days, saved on the phone only | `History.kt` |

Minimum Android version: 8.0 (API 26). Target: API 36.

## How it works

Before an app loads anything from the internet, it asks a DNS server for the address of a name such as `tracker.example.com`. PrivacyGuard sits in the middle of those lookups:

```
 App asks for "tracker.example.com"
          │
          ▼
 Android sends the lookup to the VPN's DNS server, 10.111.222.2
          │   (only this one address is routed into the tunnel;
          │    web pages, videos and downloads never enter the app)
          ▼
 PrivacyVpnService reads the packet from the tunnel
          │
   ┌──────┴────────────────────────┐
   │ on a blocklist?               │
   ├── yes ────────────────────────┼── no ──────────────────────────────┐
   ▼                               │                                    ▼
 Reply "does not exist" (NXDOMAIN)  │        Forward the lookup unchanged to the
 → the tracker never loads         │        real network's DNS (Android's resolver;
 → shown on the Blocked screen      │        public DNS only if turned on) and pass the
                                    │        real answer back
```

Key design choices:

- **DNS-only tunnel.** The VPN routes only the fake DNS address (`10.111.222.2/32`), so normal traffic goes straight to the internet. This keeps the app fast and light on battery, and the packet code only has to handle small IPv4/UDP DNS packets.
- **No DNS library.** Reading a domain name and building an NXDOMAIN reply takes about 40 lines (`DnsUtils.kt`), which keeps the code easy to read and explain.
- **The app excludes itself from its own VPN** (`addDisallowedApplication`), so forwarded lookups and blocklist downloads go directly to the real network.
- **Parent-domain matching.** If `example-tracker.com` is listed, `stats.example-tracker.com` is blocked too.

## Install a test build without Android Studio

You don't need USB debugging or Developer options to try a test build:

1. Build the APK yourself (see below), or ask the team for one.
2. Copy the `.apk` file to the phone (USB file transfer, Google Drive, Telegram "Saved messages", email, …).
3. Open the file on the phone with its file manager and tap **Install**. Android asks you to allow "Install unknown apps" for that file manager once; allow it.
4. Open PrivacyGuard and turn the switch on.

On some phones (Xiaomi, Oppo, Vivo, Realme, Huawei), the system may also show a security scan or ask you to confirm again. That's normal for apps installed from outside the store. These phones also tend to stop background apps aggressively. If protection turns off by itself, open the phone's battery settings for PrivacyGuard and choose "No restrictions" / "Allow background activity".

Pro can't be bought in a test build installed this way; Google Play Billing only works for copies installed from Play (use a [licence tester](https://developer.android.com/google/play/billing/test) on the internal testing track).

## Build from source

These steps are for Linux (Ubuntu, Fedora and similar). Windows works too: use `gradlew.bat` instead of `./gradlew` and Windows paths such as `C:/Users/<you>/...`.

Requirements:

- [Android Studio](https://developer.android.com/studio) 2026.2 or newer (it includes the right JDK and Android SDK).
- About 3 GB free disk space for Gradle and the SDK, plus 6–12 GB for an emulator.
- `git`.

### Install Android Studio on Linux

Either of these:

```bash
# Ubuntu and other distros with snap
sudo snap install android-studio --classic

# Or download the .tar.gz from developer.android.com/studio, then:
tar -xzf android-studio-*-linux.tar.gz -C ~/
~/android-studio/bin/studio.sh
```

On first start, the setup wizard downloads the Android SDK to `~/Android/Sdk`. Accept the defaults.

Then get the code:

```bash
git clone https://github.com/elwyndraker/testing-guard.git
cd testing-guard
```

### With Android Studio

1. **File → Open** and choose the `testing-guard` folder. Wait for the Gradle sync to finish.
2. Press **Run ▶** to install on a connected phone or an emulator.
3. To get an APK file: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The file appears at `app/build/outputs/apk/debug/app-debug.apk`.

### From the command line

```bash
./gradlew assembleDebug          # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # runs the unit tests
./gradlew lintDebug              # runs Android Lint
```

If Gradle can't find the Android SDK, create `local.properties` in the project folder (Android Studio does this for you when you open the project):

```properties
sdk.dir=/home/<you>/Android/Sdk
```

If `./gradlew` says "Permission denied", run `chmod +x gradlew` once.

### Release builds and signing

`./gradlew assembleRelease` (APK) and `./gradlew bundleRelease` (AAB for Google Play) sign the app when a `keystore.properties` file exists in the project folder:

```properties
storeFile=/home/<you>/PrivacyGuard-signing/privacyguard-upload.jks
storePassword=...
keyAlias=privacyguard
keyPassword=...
```

The key file and `keystore.properties` are excluded by `.gitignore` and must never be committed. Without `keystore.properties`, release builds are produced unsigned. Every published release must be signed with the same key, otherwise phones refuse to install it over the previous version.

To create a new key (keep it outside the project folder):

```bash
mkdir -p ~/PrivacyGuard-signing && cd ~/PrivacyGuard-signing
keytool -genkeypair -v -keystore privacyguard-upload.jks -alias privacyguard \
  -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` comes with Java; if the command isn't found, use the copy inside Android Studio (`~/android-studio/jbr/bin/keytool`, or `/snap/android-studio/current/jbr/bin/keytool` for the snap).

**Back up the `.jks` file and its passwords in at least two places** (for example a password manager and a USB stick). If the key is lost, phones can't update the app from a newer APK, and users have to uninstall and reinstall.

### Automatic releases

`.github/workflows/release.yml` runs the unit tests and builds the signed AAB on GitHub whenever you push a version tag. The AAB is kept for 90 days as a workflow artifact, ready to upload to the Play Console; no APK is published, because the app is distributed through Google Play only. You don't need your own computer for releases once the key is set up.

**One-time setup: store the signing key as GitHub secrets.**

1. Turn the key file into text:
   ```bash
   base64 -w0 ~/PrivacyGuard-signing/privacyguard-upload.jks > key.txt
   ```
2. On GitHub: repository → **Settings → Secrets and variables → Actions → New repository secret**. Add four secrets:

   | Name | Value |
   | --- | --- |
   | `KEYSTORE_BASE64` | the whole content of `key.txt` |
   | `KEYSTORE_PASSWORD` | the keystore password |
   | `KEY_ALIAS` | `privacyguard` |
   | `KEY_PASSWORD` | the key password |

3. Delete `key.txt` (`rm key.txt`). Keep your own backup of the `.jks` file: GitHub secrets can't be read back, only replaced.

**Each release:**

1. Run `python3 scripts/build_builtin_list.py` so the built-in list in `assets/standard.txt` is fresh.
2. Raise `versionCode` by 1 and set `versionName` in `app/build.gradle.kts` (for example `0.3`), commit and push.
3. Tag that commit with the same version and push the tag:
   ```bash
   git tag v0.3
   git push origin v0.3
   ```
4. After a few minutes, open **Actions → Release →** the run for the tag, and download `PrivacyGuard-v0.3` under **Artifacts**. It contains `PrivacyGuard-v0.3.aab`; upload that to the Play Console.

The workflow stops without an artifact if the tag doesn't match `versionName`, if a unit test fails, if the secrets are missing or if the AAB isn't signed.

Versions used: Android Gradle Plugin 9.4.1, Gradle 9.7.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Play Billing 9.1.0, compileSdk 37. All are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Run it on a phone or an emulator

### Emulator

1. On Linux, the emulator needs KVM. Check with `ls -l /dev/kvm`; if you get "permission denied" when starting the emulator, run `sudo usermod -aG kvm $USER` and log out and back in.
2. In Android Studio, open **Device Manager** (phone icon on the right) → **Create device** → pick a phone → pick a system image (API 26 or newer) → **Finish**.
3. Select the emulator in the toolbar and press **Run ▶**.
4. In the app, turn the switch on and accept Android's "Connection request" popup.

The emulator needs about 6–12 GB free disk space. To keep it on another disk, set the environment variable `ANDROID_AVD_HOME` (for example `export ANDROID_AVD_HOME=/mnt/data/avd` in `~/.profile`) before creating the device.

### Real phone over USB

1. On the phone: **Settings → About phone**, tap **Build number** 7 times, then turn on **Developer options → USB debugging**.
2. Connect the cable and accept the "Allow USB debugging?" popup on the phone.
3. Check with `~/Android/Sdk/platform-tools/adb devices`. If the phone shows as "no permissions" on Ubuntu/Debian, run `sudo apt install android-sdk-platform-tools-common` (it adds the USB rules), then unplug and replug.
4. Select the phone in Android Studio's toolbar and press **Run ▶**.

## Testing

### Unit tests (no phone needed)

```bash
./gradlew testDebugUnitTest
```

| Test class | What it checks |
| --- | --- |
| `PacketUtilsTest` | Reading the domain from IPv4/UDP DNS packets, ignoring non-DNS traffic, NXDOMAIN replies keep the id and question, answer packets swap addresses and ports with a valid IPv4 checksum |
| `BlocklistTest` | Parsing every list format (hosts files with `0.0.0.0`/`127.0.0.1`/`::`, plain domains, `*.` wildcards, adblock `||domain^` rules; comments, localhost and other adblock rules skipped), parent-domain matching, ads-exclusion skipping, no ads list in the sources, Pro filters and My rules only working with Pro, adult names hidden |
| `MyRulesTest` | Cleaning up what people paste as a rule (URLs, `*.`, case), rejecting non-domains |
| `PinLockTest` | PIN hashing and salting, valid PIN lengths, no PIN needed without Pro |
| `HistoryTest` | Saving and reading a day's counts, skipping broken lines, merging counts, day totals |
| `BillingTest` | Pro from purchase states: purchased, pending, refunded, other products |
| `DomainSetTest` | The packed domain set: no duplicates, growth to 50,000 entries, case-insensitive suffix lookups, saving one domain per line |
| `BlockedLogTest` | Repeated blocks within a minute share one row; later ones get a new row |
| `FirstSuccessTest` | The DNS route race: fast route wins, a failed route starts the next at once, a slow route is overtaken, all-fail and timeout return quickly |

### Manual test on a device or emulator

With protection on, run these from a computer with `adb` (or just open the sites in Chrome):

```bash
adb shell ping -c 1 google-analytics.com   # expected: "unknown host"  (blocked, tracker)
adb shell ping -c 1 tracking.miui.com      # expected: "unknown host"  (blocked, phone telemetry)
adb shell ping -c 1 google.com             # expected: "PING google.com (…)"  (allowed)
```

**Checklist for the Play version (not yet run on a device).** Run it on real phones (a Xiaomi, a Pixel and one more brand) before the first Play upload:

| Check | Result |
| --- | --- |
| Switch on → explanation dialog → notification permission → Android VPN popup; cancelling the popup leaves protection off | Not run |
| Trackers above blocked, `google.com` allowed; Blocked screen lists them | Not run |
| Update now downloads the lists; the date and domain count change | Not run |
| Quick Settings tile turns protection on and off | Not run |
| Public DNS fallback off by default; turning it on works | Not run |
| Licences and list credits dialog opens | Not run |
| Pro purchase with a licence tester (internal testing track); Pro filters, PIN, My rules and history unlock | Not run |
| Refund in the Play Console removes Pro | Not run |
| Adult filter on: blocked adult sites show as "Adult site blocked", never by name | Not run |
| PIN set: turning protection off and changing filters ask for it | Not run |
| Wi-Fi ↔ mobile data, 30 minutes screen off, a full day of normal use | Not run |
| No crashes in the crash log | Not run |

On Oct 23, 2026 the `Medium_Phone_API_37.0` emulator didn't finish booting on a 16 GB laptop (the image asks for 16 GB of RAM); use a real phone or a lighter system image.

The results below are from older versions that still blocked ads. The DNS and networking code they test hasn't changed.

**Emulator run of v1.2, Oct 10, 2026 (Android 17):**

| Check | Result |
| --- | --- |
| Switch on → explanation dialog → notification permission → Android VPN popup | Pass |
| Key icon and "PrivacyGuard is protecting you" notification appear | Pass |
| `doubleclick.net`, `googleadservices.com`, `stats.g.doubleclick.net` blocked | Pass |
| `google.com`, `wikipedia.org` resolve normally | Pass |
| theguardian.com and cnn.com load; their trackers and ad networks appear on the Blocked screen | Pass |
| Update now downloads a fresh list; the date changes | Pass |
| Wi-Fi off → mobile data → Wi-Fi on: blocking and browsing keep working | Pass |
| Switch off → tunnel and notification removed, `doubleclick.net` resolves again | Pass |
| No crashes in the crash log | Pass |

**Real-network DNS test (v1.1).** v1.0 failed on a real phone: no website opened. The cause was that v1.0 forwarded lookups only to the *first* DNS server of the network and waited 5 s before falling back. Real phones often list an unreachable server first (commonly IPv6). Reproduced on the emulator with `-dns-server 10.255.255.1,8.8.8.8`, which makes the first server dead:

| Version | Time per lookup with the VPN on | Websites |
| --- | --- | --- |
| v1.0 | 8,088 ms | Too slow; apps give up |
| v1.1 | ~200 ms (Android resolver for the real network) | Load normally |
| No VPN (reference) | ~420 ms | Load normally |

With *all* of the network's DNS servers dead, the app falls back to public DNS and keeps answering.

**Network switching (v1.2).** A tester's Xiaomi phone stopped resolving after switching mobile data → Wi-Fi in v1.1 (could not be reproduced on the emulator). v1.2 reads the current network for every lookup, races several routes, and restarts the safety-switch clock on every network change. Emulator results for v1.2:

| Test | Result |
| --- | --- |
| Wi-Fi → mobile data → Wi-Fi with a lookup every 3–5 s | 39 answered, 0 failed |
| 4 rapid Wi-Fi off/on flips | 46 answered, 0 failed; protection stayed on |
| Dead first DNS server | ~0.6 s per lookup (v1.0: 8 s), 0 failed |

Still to test on real phones: another VPN app taking over.

## Project structure

```
app/src/main/
├── AndroidManifest.xml          permissions, the VPN service and the Quick Settings tile
├── assets/standard.txt          built-in copy of the free list (offline fallback; scripts/build_builtin_list.py)
├── java/com/pg/privacyguard/
│   ├── MainActivity.kt          entry point, sets up Compose
│   ├── BuildConfigInfo.kt       reads the app version for the About dialog
│   ├── billing/Billing.kt       Google Play Billing: buy and restore Pro
│   ├── data/
│   │   ├── Filter.kt            the filters (free + Pro) and their list sources
│   │   ├── Blocklist.kt         loads, parses, updates and checks domains, one set per filter
│   │   ├── DomainSet.kt         memory-saving domain set (packed bytes + hash table)
│   │   ├── BlockedLog.kt        in-memory list of blocked domains + today's count
│   │   ├── History.kt           Pro: 7 days of blocked counts, on the phone only
│   │   ├── MyRules.kt           Pro: the user's own block and allow rules
│   │   ├── PinLock.kt           Pro: PIN lock (salted hash)
│   │   ├── Pro.kt               whether Pro is unlocked
│   │   └── Settings.kt          DataStore: filters, rules, update times, fallback, disclosure
│   ├── vpn/
│   │   ├── PrivacyVpnService.kt the VPN: tunnel, packet loop, forwarding, notification
│   │   ├── VpnController.kt     start/stop + the running state the UI observes
│   │   ├── ProtectionTileService.kt  Quick Settings tile
│   │   ├── UpstreamDns.kt       answers allowed lookups (see FirstSuccess.kt)
│   │   ├── PacketUtils.kt       parse IPv4/UDP packets, build answer packets
│   │   └── DnsUtils.kt          read DNS questions, build NXDOMAIN replies
│   └── ui/
│       ├── PrivacyGuardApp.kt   bottom navigation + weekly auto-update
│       ├── HomeScreen.kt        switch, status, counter, permissions flow
│       ├── BlockedScreen.kt     blocked domains list
│       ├── FiltersScreen.kt     filters, Update now, DNS fallback, About, credits
│       ├── ProScreen.kt         purchase, PIN lock, My rules, history
│       ├── PinDialogs.kt        PIN entry dialogs
│       ├── Credits.kt           list credits and licences
│       ├── AppUpdateBanner.kt   Google Play in-app updates
│       └── theme/Theme.kt       colors
└── res/                         strings, icons, theme
app/src/test/                    unit tests
docs/index.html                  privacy policy page (for GitHub Pages)
play-store/                      Play plan and listing texts
scripts/                         build_builtin_list.py
screenshots/                     images used in this README
```

## Code walkthrough

A good reading order for new developers:

1. **`VpnController.kt`**: the small API the UI uses: `start`, `stop`, and `isRunning`. Only the service changes `isRunning`, so the switch always shows the real state.
2. **`PrivacyVpnService.startTunnel()`**: loads the blocklist, reads the real DNS server, then builds the tunnel with `VpnService.Builder`. Note `addRoute("10.111.222.2", 32)`: that single route is what makes the tunnel DNS-only.
3. **`PrivacyVpnService.runLoop()`**: the tunnel file descriptor is non-blocking, so the loop waits with `Os.poll` and a 1-second timeout instead of spinning. The timeout also lets it notice a stop request.
4. **`handlePacket()`**: `PacketUtils.parseDnsQuery` → `Blocklist.isBlocked` → either `DnsUtils.nxdomain` (blocked) or `UpstreamDns.resolve()` in its own coroutine (allowed) → `PacketUtils.buildResponse` → write back to the tunnel.
5. **`UpstreamDns.kt`** + **`FirstSuccess.kt`**: how allowed lookups are answered, as a staggered race. First Android's own resolver for the real network (`DnsResolver.rawQuery`, Android 10+; it already knows which servers work and handles IPv6 and Private DNS). After 0.6 s, the network's DNS servers directly, over that network. After 1.2 s, the public fallbacks 1.1.1.1 and 8.8.8.8, only if the user turned on Public DNS fallback. If one route fails, the next starts at once; the first answer wins. The network is read fresh for every lookup. Answers must be fast: apps give up on a lookup within a few seconds.
6. **`PacketUtils.buildResponse()`**: builds a fresh IPv4 + UDP packet with the addresses and ports swapped. The UDP checksum is 0, which IPv4 allows.
7. **`Filter.kt`** + **`Blocklist.kt`** + **`DomainSet.kt`**: one domain set per filter, checked with a hash lookup that walks up parent domains (My rules' allow list first). An update first downloads EasyList (the ads exclusion list), then each turned-on filter's sources, merging them and skipping every domain on EasyList and sources that fail (the Filters screen says how many). Each filter is saved as its own file, one domain per line, and only replaces the current one if it has at least the filter's `minDomains`, so a broken download never wipes a list. Large lists would need well over 100 MB as a `HashSet<String>`, so `DomainSet` packs them into one byte array plus an int hash table; the manifest also sets `largeHeap` because an update builds the new list while the old one is still in use.

Things that look odd but are deliberate:

- `android:foregroundServiceType="systemExempted"` with `tools:ignore="ForegroundServicePermission"`: VPN apps may use this type without the exact-alarm permission Lint asks for. Adding that permission would trigger extra Google Play review. Verified working on Android 17.
- `setMetered(false)`: Android otherwise treats every VPN as metered, which makes other apps delay downloads.
- A `generation` counter in the service: it discards a slow start that finishes after the user already turned protection off.

## Known limits

- **Private DNS:** if Settings → Network → Private DNS is set to a provider hostname (for example `dns.google`), Android sends lookups around the VPN and nothing is blocked. "Off" and "Automatic" both work. The Home screen shows this tip.
- **IPv4 DNS only.** Android sends lookups to the VPN's IPv4 DNS address, so this covers normal use. DNS over TCP (rare, used for very large replies) isn't handled.
- **Allow-list is Pro.** If a site breaks because one of its domains is on a list, check the **Blocked** screen; Pro users can allow that domain under **My rules**, others can turn protection off for that site.
- **The PIN lock is not a full parental control.** It protects the app's own switch and settings, but someone can still turn the VPN off in Android's settings or uninstall the app. The Pro screen says so.
- **Pro needs Google Play.** Billing only works for copies installed from Play; test builds from an APK can't unlock Pro.
- **The Blocked list lives in memory** and empties when the app is fully closed (Pro keeps 7 days of counts on the phone).

## Publishing on Google Play

Not published yet. Before the first upload:

1. The privacy policy is live at <https://elwyndraker.github.io/testing-guard/> (GitHub Pages, from `docs/index.html`); the app links to it from the Filters screen.
2. Get the signed AAB from the [release workflow](#automatic-releases), or build it with `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
3. Turn on **Play App Signing**: Google keeps the key that signs the app for users, and `privacyguard-upload.jks` becomes the **upload key**. Keep it backed up.
4. Create the in-app product `pro_unlock` (one-time, non-consumable) under **Monetize → Products**. It must match `Pro.PRODUCT_ID`. The price isn't decided yet.
5. Fill in the store listing and the forms (Ads, App access, Content rating, Target audience, Data safety, VPN declaration) from [`play-store/listing.md`](play-store/listing.md).
6. Upload to **Internal testing** first, add licence testers, and run the [device checklist](#manual-test-on-a-device-or-emulator). A new personal developer account then needs a closed test with 12 testers for 14 days before production.
7. Raise `versionCode` in `app/build.gradle.kts` before every upload.

The reasons behind each answer, and why the app can't block ads or use other payment providers on Play, are in [`play-store/PLAY_VERSION_PLAN.md`](play-store/PLAY_VERSION_PLAN.md).

### In-app updates

`AppUpdateBanner.kt` uses Google Play's [in-app updates](https://developer.android.com/guide/playcore/in-app-updates) (flexible flow). When Play has a newer version, a card at the top of every screen offers **Update**; Play shows its own update screen and downloads in the background, then the card offers **Restart** to install. The app never downloads or installs APKs itself, which Play policy forbids.

The card only appears for copies installed from Google Play. For test builds installed from an APK it simply never shows. To try it before the public release:

1. Upload a build to the **Internal testing** track and add yourself as a tester.
2. Install it on a phone from the tester link in the Play Store.
3. Upload a build with a higher `versionCode` to the same track.
4. Open the Play Store page once (so Play notices the new version), then open PrivacyGuard: the update card appears.

Google's [internal app sharing](https://support.google.com/googleplay/android-developer/answer/9844679) also works for this and doesn't need a review.

## Blocklist sources

The free filter (always on) merges these lists; the Pro filters are listed below. See `Filter.kt`. Each list keeps its own licence; `assets/standard.txt` bundles a copy of the free lists, which is allowed because the app is GPL-3.0. The same credits are shown in the app under **Filters → Licences and list credits**.

| Filter | List | Licence |
| --- | --- | --- |
| Free | [EasyPrivacy](https://easylist.to/) (domain rules only) | GPL-3.0 / CC BY-SA 3.0 |
| Free | [r-a-y/mobile-hosts](https://github.com/r-a-y/mobile-hosts) (AdguardMobileSpyware) | GPL-3.0 |
| Free | [FadeMind/hosts.extras](https://github.com/FadeMind/hosts.extras) (add.2o7Net) | MIT |
| Free | [NoTrack trackers](https://gitlab.com/quidsup/notrack-blocklists) | GPL-3.0 |
| Free | [HaGeZi](https://github.com/hagezi/dns-blocklists) Native Tracker: Xiaomi, Samsung, Huawei, OPPO/realme, vivo | GPL-3.0 |
| Free | [HaGeZi](https://github.com/hagezi/dns-blocklists) Threat Intelligence Feeds (mini) and Fake | GPL-3.0 |
| Pro: Adult content | [HaGeZi](https://github.com/hagezi/dns-blocklists) NSFW | GPL-3.0 |
| Pro: Gambling | [HaGeZi](https://github.com/hagezi/dns-blocklists) Gambling (medium) | GPL-3.0 |
| Pro: Block filter bypass | [HaGeZi](https://github.com/hagezi/dns-blocklists) DoH/VPN/Proxy bypass | GPL-3.0 |
| Pro: Newly registered domains | [HaGeZi NRD](https://github.com/hagezi/nrd) (DGA, 7 days) | GPL-3.0 |
| Pro: Cryptojacking | [NoCoin adblock list](https://github.com/hoshsadiq/adblock-nocoin-list) | MIT |
| Pro: Stalkerware | [Echap stalkerware indicators](https://github.com/AssoEchap/stalkerware-indicators) | CC BY 4.0 |
| Exclusion only | [EasyList](https://easylist.to/): its domains are removed from all of the above, never blocked | GPL-3.0 / CC BY-SA 3.0 |

Adblock lists also contain rules for paths, element hiding and exceptions (`@@`); DNS filtering can't apply those, so only plain `||domain^` rules are used. If an app or site breaks, check the **Blocked** screen to see which domain was blocked.

## Credits

- Blocklists: see [Blocklist sources](#blocklist-sources).
- Design ideas from [DNS66](https://github.com/julian-klode/dns66) (GPL-3.0; no code copied).

## License

PrivacyGuard is free software under the [GNU General Public License v3.0](LICENSE). The blocklists keep their own licences (see [Blocklist sources](#blocklist-sources)).
