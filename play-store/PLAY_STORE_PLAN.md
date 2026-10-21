# PrivacyGuard → Google Play: full plan (testing-guard)

> **Background document.** The current plan is [`PLAY_VERSION_PLAN.md`](PLAY_VERSION_PLAN.md).
> This file was written before the Play version was decided, and some facts here are out of date:
> the app is now v0.1 (versionCode 2), and `brand/play-store/` now has the 512 px icon and a feature graphic.
> **Later decisions:** the app is Google Play only, and Pro is paid through Google Play Billing.
> SSLCommerz, the separate "full" ad-blocking build and the GitHub/website distribution described
> below were dropped. See `PLAY_VERSION_PLAN.md` sections 2–3 for why.

Written 2026-10-21 against `elwyndraker/testing-guard` @ `6fe16a6` (tag `v0`).
No code has been changed. This plan replaces `play-store-readiness-prompt.md`, which describes the
old repo (`test-guard`, `com.diu.privacyguard`, v1.3).

## 0. Where the app stands today

| Area | State | Notes |
|---|---|---|
| Package | `com.pg.privacyguard` | Can **never** change after the first Play upload. Decide in step 1.3 before uploading. |
| SDK | minSdk 26, targetSdk 36, compileSdk 37 | Meets the new-app target rule (verify in step 1.1). |
| Version | versionName `0`, versionCode 1 | Every Play upload needs a higher versionCode. |
| Signed builds | OK | `release.yml` builds a signed APK + AAB on `v*` tags; the 4 secrets are set; `v0` release has both files. |
| Upload key | OK | `~/PrivacyGuard-signing/privacyguard-upload.jks`. Back it up off this machine. With Play App Signing, Google holds the real signing key; this one is only the *upload* key. |
| Privacy policy | Live, mostly accurate | https://elwyndraker.github.io/testing-guard/ returns 200. Says "updated for version 1.3" and mentions advertising. |
| Store assets | **Missing** | No `play-store/` folder, no 512×512 icon, no 1024×500 feature graphic, no store screenshots. |
| Ad blocking | **Blocker** | See section 2. |

---

## 1. Sequence of work

### Phase 1: Decisions (you)
1.1 Re-read the live policy pages (links in section 6) the day you start. Google changes them often.
1.2 Choose option A, B or C in section 2.4. **Recommended: A (two flavors).**
1.3 Choose package names. Suggested: Play = `com.pg.privacyguard`, GitHub full = `com.pg.privacyguard.full`.
    The one `v0` APK on GitHub would then be the "Play" package. Anyone who installed it reinstalls once.
    Only one release exists, so changing this now is cheap.
1.4 Choose the store name (section 4.6). "PrivacyGuard" is a generic name and likely already used on Play.

### Phase 2: Code changes (after you approve; small commits)
2.1 Add product flavors `play` and `full` in `app/build.gradle.kts` (`flavorDimensions += "store"`).
    - `full`: `applicationIdSuffix = ".full"`, keeps the current 17 sources and anti-AD.
    - `play`: Play-safe sources only (section 2.3), different built-in list, privacy/security wording.
2.2 Move `Blocklist.SOURCES` into a per-flavor file (`app/src/play/java/.../BlocklistSources.kt` and
    `app/src/full/java/.../BlocklistSources.kt`), or into `BuildConfig`. Keep the shared logic in `main`.
2.3 Per-flavor `assets/hosts.txt`: `full` keeps anti-AD; `play` ships a tracker/malware list
    (licence check in section 4.5).
2.4 Per-flavor strings: `app/src/play/res/values/strings.xml` overrides `status_on_detail`,
    `disclosure_text`, `notification_text` and `blocklist_url` (wording in section 2.5).
2.5 In-app update banner (`AppUpdateBanner.kt`, `play-app-update`): `play` only. The `full` build
    can't use Play updates. Use `playImplementation(libs.play.app.update)` plus a no-op stub in `full`.
2.6 The `play` build must not link to the GitHub full version or to APK downloads. The policy says
    an app "may not modify, replace, or update itself using any method other than Google Play's
    update mechanism", and pointing users to an ad blocker off Play invites a rejection.
2.7 Check `MIN_DOMAINS = 10_000` (`Blocklist.kt:47`) against the smaller `play` list. It should still
    pass, but confirm with real numbers.
2.8 Update `release.yml`: build `bundlePlayRelease` (AAB, for Play) and `assembleFullRelease` (APK,
    for GitHub). Name them clearly. Don't attach the Play AAB to GitHub releases.
2.9 Unit tests for the per-flavor source lists, for example "the `play` sources contain no ads-only list".
    Run `./gradlew testPlayDebugUnitTest testFullDebugUnitTest lintPlayDebug assemblePlayDebug assembleFullDebug bundlePlayRelease`
    with the JBR `JAVA_HOME` (see the memory note on local builds).

### Phase 3: Docs and assets
3.1 Privacy policy (`docs/index.html`): remove "advertising" from the description, drop the
    "version 1.3" line, and list the `play` hosts. Keep the 1.1.1.1/8.8.8.8 and in-app update sections.
    If the `full` flavor downloads from extra hosts, add a short "GitHub version" section, or one policy
    covering both builds.
3.2 README: two sections, "Google Play version (privacy and security)" and "GitHub version (full lists)".
3.3 Create `play-store/`: 512×512 icon (32-bit PNG), 1024×500 feature graphic, 4–8 phone screenshots
    of the **play** build (Home on, Blocked list showing tracker domains, Filters, disclosure dialog).
    Capture them with `adb exec-out screencap -p > shot.png`. The screenshots must not show ad domains
    or the word "ads".
3.4 `play-store/listing.md` with the title, short and full description (draft in section 4.1).

### Phase 4: Play Console (you, with a checklist from me)
4.1 Create a developer account: $25, identity verification, personal vs organisation (organisation needs a D-U-N-S number).
4.2 Create the app → upload the `play` AAB to **Internal testing** first. Opt in to Play App Signing with the upload key.
4.3 Fill in App content: privacy policy URL, ads ("No"), app access ("no login"), content rating,
    target audience (18+ or 13+, **not** children, to stay out of Families), Data safety (section 4.3),
    VPN declaration (section 3), foreground service declaration if asked (section 4.4),
    and "not applicable" for the news, health and financial forms.
4.4 **Closed testing** (new personal accounts): 12+ testers opted in for 14 consecutive days, who
    actually use the app. Then apply for production access. Plan: a Google Group as the tester list;
    friends, classmates, r/AndroidClosedTesting-style swaps (unofficial); and a weekly update
    (versionCode bump) so testers keep using it. This also lets you test the in-app update banner.
4.5 Production rollout at 20%, then 100%. If rejected, read the email's policy name, fix, bump versionCode, resubmit.
    Appeal only if the reviewer clearly misread the app.

---

## 2. The ad-blocking problem (in detail)

### 2.1 What the rules say (official text)

**VpnService policy** ([Play Console Help 12564964](https://support.google.com/googleplay/android-developer/answer/12564964), read 2026-10-21):

- VPN must be the app's core function, or the app must fall into an allowed category:
  "Parental control and enterprise management apps", "**Device security apps (for example, anti-virus,
  mobile device management, firewall)**", "Network-related tools (for example, remote access)",
  "Web browsing apps", "App usage tracking", "Carrier apps…".
  **Ad blocking is not on the list.** PrivacyGuard has to qualify as *device security / firewall*.
- Forbidden: "Redirect or manipulate user traffic from other apps on a device for monetization purposes".
- "**Any manipulation of ads that can impact monetization will result in rejection.**"
- Required: "Document use of the VpnService in the Google Play listing"; "encrypt the data from the
  device to the VPN tunnel endpoint"; follow all other policies.
- Declaration form questions include "Is providing a VPN the core functionality of your application?",
  "Which permitted functionality does your app provide?", a ≤90-second video of the VPN in use,
  a video of the prominent disclosure, and "Does your application redirect or manipulate user
  traffic from other apps on a device for monetization purposes?"

**Device and Network Abuse policy** ([Play Console Help 9888379](https://support.google.com/googleplay/android-developer/answer/9888379), read 2026-10-21):

- Listed as an example violation: "Apps that block or interfere with another app displaying ads."
- Key consideration: "Don't block or interfere with another app displaying ads."
- An app "may not modify, replace, or update itself using any method other than Google Play's update mechanism."

**In plain terms:** blocking tracking and malware at the DNS level fits "device security / firewall".
Blocking ads inside other apps is named as a violation twice. A reviewer decides from what the app
*says* (listing, screenshots, in-app text, video) and what it *does* (which lists it loads).
Today PrivacyGuard says "ad" in the status text, the disclosure, the notification and the privacy
policy, and about 2 of every 3 of its sources are ad lists. As it stands it would very likely be
rejected or removed later, possibly with a strike against the developer account.

### 2.2 What PrivacyGuard blocks today: the 17 sources plus the built-in list

Classified from each project's own description (verify each repo's README before shipping):

| # | Source | What it is | Class | Play build? |
|---|---|---|---|---|
| 1 | jdlingyu/ad-wars | Chinese ad hosts | **Ads** | ✗ |
| 2 | r-a-y/mobile-hosts AdguardMobileSpyware | Mobile spyware/tracking from AdGuard filters | Tracker | ✓ |
| 3 | FadeMind add.2o7Net | Adobe/Omniture 2o7.net tracking | Tracker | ✓ |
| 4 | privacy-protection-tools/anti-AD | Chinese ad/tracker list ("anti-AD") | **Ads** | ✗ |
| 5 | AdroitAdorKhan/antipopads-re | Pop-up/pop-under ads, malvertising | **Ads** (some malware) | ✗ |
| 6 | hagezi/dns-blocklists **pro** | Ads + tracking + malware + phishing | Mixed | ✗ → replace with HaGeZi *native trackers* and *TIF* |
| 7 | EasyPrivacy | Tracking/telemetry | Tracker | ✓ |
| 8 | hBlock | Ads, tracking, malware combined | Mixed | ✗ |
| 9 | rentianyu/Ad-set-hosts | Ad hosts | **Ads** | ✗ |
| 10 | TG-Twilight/AWAvenue-Ads-Rule | **In-app ad SDKs** (exactly what the policy targets) | **Ads** | ✗ |
| 11 | jerryn70/GoodbyeAds | Mobile/in-app ads, YouTube ads | **Ads** | ✗ |
| 12 | lightswitch05 ads-and-tracking-extended | Ads + trackers | Mixed | ✗ (lightswitch05 also has a tracking-only list → ✓) |
| 13 | quidsup/notrack trackers | Trackers only | Tracker | ✓ |
| 14 | notracking/hosts-blocklists | Ads + trackers; **repo is no longer maintained** | Mixed | ✗ (remove from both flavors) |
| 15 | hosts.flossboxin.org.in | Combined hosts list (contents unclear) | Mixed / unclear | ✗ |
| 16 | bongochong/CombinedPrivacyBlockLists | Privacy + malware + some ads | Mixed | ✗ (or check the sub-list) |
| 17 | EasyList IndianList | Regional **ad** filter | **Ads** | ✗ |
| — | built-in `assets/hosts.txt` (anti-AD) | Ads | **Ads** | ✗ → replace |

Tally: 7 ads, 6 mixed, 4 tracker-only. Only #2, #3, #7 and #13 can stay in a Play build as they are.

### 2.3 A Play-safe source list (proposal)

Keep #2, #3, #7 and #13, and add security and tracker-only lists such as:

- HaGeZi **Native Tracker** lists (device/OS telemetry: Samsung, Xiaomi, Apple, Windows, etc.)
- HaGeZi **Threat Intelligence Feeds (TIF)**: malware, phishing, scam, C2
- abuse.ch **URLhaus** (malware hosts; CC0)
- **Phishing Army**, or similar phishing-only lists
- lightswitch05 **tracking-aggressive** (tracking only, no ads)
- Frogeye **first-party trackers** (CNAME cloaking)

Expect roughly 150k–400k domains instead of about 1M. The listing must not say "1 million".

**Remaining risk:** tracker lists still include some domains that also serve ads (for example
Google Analytics and DoubleClick tracking hosts). Tracker-blocking apps already on Play live with
this. An optional safety step: at merge time in the `play` build, drop every domain that appears in
a known ads-only list. Then the Play build provably does not block ad delivery, which is easy to
explain to a reviewer. Recommended, but your call (it adds one download and a few lines of code).

### 2.4 Options

| | A. Two flavors (**recommended**) | B. Play-only privacy app | C. Skip Play |
|---|---|---|---|
| What | `play` = trackers and security; `full` = everything, on GitHub/F-Droid | Remove ad lists everywhere | GitHub + F-Droid only |
| Pros | Play users get a policy-safe app; power users keep full blocking; one codebase | Simplest; one build | No review, no 14-day test, no policy risk |
| Cons | Two builds to test; two package names; README and policy cover both | Loses the main feature for existing users | Much smaller audience; no Play updates |
| Precedent (unofficial, from memory, verify) | NetGuard (Play build has no ad blocking), Blokada (Play = limited, full from website), RethinkDNS | DuckDuckGo App Tracking Protection ("doesn't block ads") | AdAway, full Blokada |

**Why A:** it is what working apps on Play do, it keeps your current users, and the code change is
mostly moving data (source lists, strings, one asset) into flavor folders.

### 2.5 Wording for the `play` build

| Where | Now | Play wording |
|---|---|---|
| `status_on_detail` | Tracking and ad domains are being blocked on this phone. | Known tracker, malware and phishing domains are being blocked on this phone. |
| `disclosure_text` | …Lookups for known tracking and ad domains are blocked… | PrivacyGuard uses a local VPN on this phone to check website lookups (DNS). Lookups for known trackers, malware and phishing sites are blocked; all others go to your normal DNS server.\n\nNo VPN server is used. Nothing is sent to us, and no data leaves your phone because of this app. |
| `notification_text` | Blocking tracking and ad domains on this phone | Blocking trackers and harmful sites on this phone |
| `blocklist_url` | README#blocklist-sources (lists ad lists) | README#google-play-version-sources |
| Privacy policy | "…blocks known tracking and advertising domains…" | "…blocks known tracking, malware and phishing domains…" |
| README | "blocks ad and tracker domains" | Separate sections per build. |

Avoid anywhere in the Play build, listing and screenshots: "ad", "ads", "ad blocker", "adblock",
"block ads in apps/YouTube/games", "remove ads", "1 million", "best", "#1".

---

## 3. VpnService declaration (draft answers)

- **Is providing a VPN the core functionality?** No. The app uses VpnService only to filter DNS on the
  device; it does not connect to a VPN server.
- **Permitted functionality:** Device security apps (firewall).
- **Explanation:** "PrivacyGuard is an on-device DNS firewall. It uses VpnService to create a local-only
  tunnel that captures DNS queries sent to one address (10.111.222.2). Queries for known tracker,
  malware and phishing domains are answered locally with NXDOMAIN; all other queries are forwarded
  unchanged to the device's normal DNS server. No other traffic enters the tunnel. There is no remote
  VPN server, so no user traffic leaves the device through the tunnel. The app does not block, change
  or redirect ads or monetization of other apps."
- **Data collected or shared via VpnService:** None. The list of blocked names is kept in memory only.
- **Redirect or manipulate traffic for monetization?** No.
- **Videos:** (1) open the app → disclosure → Android VPN prompt → Blocked screen filling with tracker
  domains, under 90 seconds; (2) the disclosure dialog. Record with `adb shell screenrecord`.
- **Encryption requirement:** it is unclear whether this applies to a local tunnel with no remote
  endpoint. The safer way to read it: there is no "VPN tunnel endpoint" off the device, so nothing
  leaves it. Say this plainly in the declaration. If a reviewer pushes back, the fallback is to send
  forwarded queries over DNS-over-HTTPS (a feature change; only if needed).
- **Listing note:** put this line in the full description: "Uses Android's VpnService to run a local,
  on-device DNS filter. No VPN server is used and your traffic is not routed off the phone."

---

## 4. Other items

### 4.1 Store listing draft (play build)
- **Title (≤30):** e.g. "PrivacyGuard: Tracker Blocker" (29 characters), depending on the name check.
- **Short (≤80):** "Block trackers, malware and phishing on your whole phone. No account, no server."
- **Full (≤4000):** what it does, how it works (local DNS filter, VpnService line from section 3),
  what it doesn't do (doesn't hide your IP, doesn't encrypt traffic, isn't a remote VPN), privacy
  (nothing collected), the open-source lists credited, and the Private DNS tip.
  Name Cloudflare and Google only in the factual fallback sentence.

### 4.2 Prominent disclosure
The dialog before the VPN consent (`HomeScreen.kt`) is the right pattern: it appears during normal
use and before the system prompt. Check that it has a clear "Continue" and a "Cancel", and that
Cancel doesn't start the VPN. Update its text (section 2.5).

### 4.3 Data safety (draft)
- Collects data: **No**. Shares data: **No**.
- Blocklist downloads send only the IP address the network needs, with no app data, so this is not
  "collection" by you. DNS forwarding is the user's own request to their resolver.
- In-app updates library (Google Play Core): check Google's guidance on whether Play libraries need
  declaring. Generally Play's own system services aren't your data collection, but verify.
- Encrypted in transit: N/A. Deletion request: N/A (nothing stored off the device).

### 4.4 Foreground service
`foregroundServiceType="systemExempted"` + `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`. Android allows
`systemExempted` for apps set up as the device's VPN. Play Console may ask for a foreground service
declaration for apps targeting 34+. Answer: "Keeps the user-enabled local VPN DNS filter running;
the user turns it on and off." Verify in Phase 1 whether the form is shown for this type.

### 4.5 Licences
- anti-AD (MIT): keep its copyright notice if `full` ships it, ideally on an in-app licences screen,
  not only in the README.
- New `play` built-in list: prefer a permissive one (URLhaus CC0), or check that a GPL/CC-BY-SA list
  (EasyPrivacy, HaGeZi) is OK to bundle as data with attribution. Add an in-app licence credit.

### 4.6 Name
Search Play for "PrivacyGuard" / "Privacy Guard". Generic names often already exist, and an
identical title can confuse users or draw a complaint. Alternatives: "PG Tracker Shield",
"QuietDNS", "TrackerWall", "NoTrace DNS". Run a trademark search
(USPTO/WIPO) on the final pick.

### 4.7 Stability (Android vitals, pre-launch report)
- `largeHeap` plus a big list: the smaller `play` list helps. Test a low-RAM emulator (2 GB) during a list update.
- List updates run on `Dispatchers.IO` with a mutex. Check that the UI shows progress and that there's no ANR on slow networks.
- The pre-launch report runs robo tests. Make sure the app doesn't crash when the VPN prompt is cancelled.

---

## 5. What I need from you

1. Option A, B or C (recommended: A).
2. Package names (suggested: Play `com.pg.privacyguard`, full `com.pg.privacyguard.full`).
3. Whether to add the optional "drop known ad-serving domains" step (section 2.3).
4. Store name.

After that I'll implement Phase 2 and 3 in small commits on a branch, run the build and tests, and
ask before pushing.

## 6. Sources
- VpnService policy: https://support.google.com/googleplay/android-developer/answer/12564964 (official)
- Device and Network Abuse: https://support.google.com/googleplay/android-developer/answer/9888379 (official)
- 2022 news coverage of the VPN policy change (unofficial): https://www.techradar.com/news/google-to-stop-android-vpn-apps-blocking-ads ,
  https://www.bitdefender.com/blog/hotforsecurity/google-plans-to-bar-android-vpn-apps-from-blocking-or-interfering-with-ads/
- Still to check in Phase 1: target API rule, closed-testing rule, foreground service declaration,
  Data safety guidance for Play libraries, store asset specs.
