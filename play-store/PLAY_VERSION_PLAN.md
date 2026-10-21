# PrivacyGuard: Play version plan

Written 2026-10-21 against `elwyndraker/testing-guard` @ `b3c93a4` (v0.1, versionCode 2,
package `com.pg.privacyguard`). **Plan only. No code has been changed.**

The Play version is a **tracker, malware and phishing blocker** (Play's "device security / firewall"
category) with a one-time **Pro** upgrade sold through Google Play Billing. It never blocks ads.
Ad blocking lives elsewhere: a Samsung Internet ad-blocker app on Play, or the full build sold on
your own site (see `WHY_IT_FAILS_AND_HOW_TO_FIX.md`).

Background: `PLAY_STORE_PLAN.md` (first audit) and `WHY_IT_FAILS_AND_HOW_TO_FIX.md` (why the
current build would be rejected).

---

## 1. Features

### Free
| Feature | Status | Notes |
|---|---|---|
| Whole-phone DNS filter (local VPN, no server) | Exists | Same engine as today |
| **Tracker and analytics blocking** | Change lists | Lists in section 3.1 |
| **Phone / OS telemetry blocking** (Samsung, Xiaomi, Huawei, OPPO/realme, etc.) | New list | HaGeZi Native Tracker lists |
| **Malware, phishing and scam protection** | New lists | URLhaus, HaGeZi Threat Intelligence, Phishing Army |
| Blocked requests screen (live, kept in memory) | Exists | |
| Automatic weekly updates + "Update now" | Exists | |
| Works offline from first launch | Change the built-in list | Built-in list must be tracker/malware, not anti-AD (section 3.3) |
| Private DNS tip | Exists | |
| Quick Settings tile (on/off) | New, optional | Keep it free; basic controls shouldn't be paywalled |

### Pro (one-time purchase, Google Play Billing)
| Feature | Category it fits | Notes |
|---|---|---|
| **Adult content filter** | Content filter (firewall) / parental control | Lists in section 3.2 |
| **Gambling filter** | Same | |
| **PIN lock** for filter settings and the on/off switch | Parental control | Only add this if you also want to tick "Parental control" on the VPN form (section 2.1) |
| Block "filter bypass" services (public DoH, proxy and VPN-bypass domains) | Security / parental control | Stops apps and browsers skipping the filter |
| Extra security lists: newly registered domains, cryptojacking, stalkerware | Device security | Newly registered domain lists are very large; test memory (section 6) |
| My rules: block or allow single domains | Firewall | Typed one at a time, never a list URL (section 2.3) |
| Blocked history saved on the phone (e.g. 7 days) + daily stats | — | On the device only, so Data safety stays "no data collected" |

### Never in the Play version
- Ad lists, ad presets, or any text, screenshot or link about blocking ads.
- "Add a list from a URL" (a gray area; leave it out of the first release, see section 2.3).
- Any link or mention of the full version, your website's shop, or SSLCommerz.
- Downloading APKs or updating outside Google Play.

### Price
One non-consumable in-app product, e.g. `pro_unlock`. Suggested price about **USD 1.99**, with
Play's local price for Bangladesh. Google keeps 15% (on the first USD 1M a year). Always show the
price from Play (`ProductDetails`); never hard-code it.

---

## 2. Play rules this version must follow

Each row: the rule, then what the Play version does to comply.

### 2.1 VpnService policy ([official](https://support.google.com/googleplay/android-developer/answer/12564964))
| Rule (quoted or summarised) | What we do |
|---|---|
| VPN must be core, or the app must be in an allowed category, e.g. "Device security apps (for example, anti-virus, mobile device management, firewall)", "Parental control…" | Declare **Device security → firewall**. Add **Parental control** only if Pro has the PIN lock. |
| "Any manipulation of ads that can impact monetization will result in rejection." | No ad lists anywhere. Optional: remove from the merged list any domain that appears in a known ads-only list (section 3.4). |
| "Redirect or manipulate user traffic from other apps on a device for monetization purposes" is forbidden | DNS only; allowed lookups forwarded unchanged; no money involved. |
| "Document use of the VpnService in the Google Play listing" | A fixed paragraph in the full description (section 5). |
| "encrypt the data from the device to the VPN tunnel endpoint" | The tunnel never leaves the phone (no remote endpoint). Say so plainly in the declaration. If a reviewer insists, the fix is DNS-over-HTTPS upstream (a later code change). |
| Collecting data needs prominent disclosure and consent | Disclosure dialog before Android's VPN prompt: **already OK** (`HomeScreen.kt:141`; Cancel doesn't start the VPN, Continue does). Update its text only. |
| VPN declaration form, with a video of the VPN in use (≤90 s) and a video of the disclosure | Record with `adb shell screenrecord` on the Play build (section 7.1). |

### 2.2 Device and Network Abuse ([official](https://support.google.com/googleplay/android-developer/answer/9888379))
| Rule | What we do |
|---|---|
| "Apps that block or interfere with another app displaying ads" is a violation | Tracker and security lists only; never say "ads". |
| An app "may not modify, replace, or update itself using any method other than Google Play's update mechanism" | Keep the Play in-app update banner (allowed). No APK downloads or GitHub update checks in the Play build. Blocklists are data, not code, so downloading them is fine. |

### 2.3 Deceptive behaviour and user-added lists
- What reviewers test must be what users get: no remote switch that turns ad lists on after approval.
- "Add list from URL" is not banned in writing, but users would use it for ad lists, and Google may see
  that as the app blocking ads. **Leave it out of the first release.** Pro's "My rules" (single
  domains, typed by the user) is fine.

### 2.4 Payments ([official](https://support.google.com/googleplay/android-developer/answer/9858738))
| Rule | What we do |
|---|---|
| In-app features you charge for "must use Google Play's billing system" | Pro is sold only through Play Billing. |
| Apps "may not lead users to a payment method other than Google Play's billing system" | No website, SSLCommerz or "full version" links in the app, listing or screenshots. |
| Play Billing Library **8.0.0 or later** for new apps and updates from **31 Aug 2026** (extension to 1 Nov 2026) | Use the latest 8.x/9.x. |
| Acknowledge purchases (unacknowledged purchases are refunded after 3 days) | Acknowledge right after a successful purchase; handle **pending** purchases (cash or carrier payments). |
| Refunds | Re-check purchases on launch with `queryPurchasesAsync`; remove Pro if refunded. |

### 2.5 Technical requirements
| Rule | State | What we do |
|---|---|---|
| New apps and updates must target **API 36** from 31 Aug 2026 ([official](https://developer.android.com/google/play/requirements/target-sdk)) | ✓ targetSdk 36 | Expect API 37 to be required around Aug 2027. |
| Upload as an **AAB** with Play App Signing | ✓ `release.yml` builds an AAB | Add a Play-flavor bundle task (section 8). |
| versionCode must increase on every upload | ✓ comment in `build.gradle.kts` | Bump before every upload. |
| 16 KB memory page support (apps with native libraries) | ✓ no `.so` files | Confirm with APK Analyzer after adding Billing. |
| Foreground service types must be declared on the App content page for targetSdk 34+ ([official](https://support.google.com/googleplay/android-developer/answer/13392821)) | `systemExempted` | `systemExempted` isn't in that page's list of types. If Play Console asks, explain: "keeps the user-enabled local VPN filter running". |
| Notification permission (Android 13+) | ✓ requested at runtime (`HomeScreen.kt:124`) | — |

### 2.6 Data safety ([official](https://support.google.com/googleplay/android-developer/answer/10787469))
Google defines "collect" as "transmitting data from your app off a user's device". Data "only
processed locally on the user's device" doesn't count.

| Data flow | Leaves the phone? | Answer |
|---|---|---|
| Blocked list, history, stats, settings, Pro status | No | Not declared |
| Play Billing purchase | Google collects payment details directly; the app only reads the purchase state on the phone | Not declared, **as long as no server checks purchases**. A server later would mean declaring "Purchase history". |
| In-app update check | Handled by the Play Store app | Not declared |
| Blocklist downloads | Only an HTTP request (your IP goes to the list host) | Usually not declared; listed in the privacy policy |
| Allowed DNS lookups forwarded to **the network's DNS** | Yes, as the phone would do anyway | Not declared (normal OS behaviour; in the privacy policy) |
| Allowed lookups sent to the **1.1.1.1 / 8.8.8.8 fallback** | Yes, to a third party chosen by the app | **⚠ Unclear.** Domain names can count as "web browsing history". **Safer choice:** make the public fallback an opt-in setting (user-initiated, disclosed). Otherwise declare "Web browsing history → shared, processed ephemerally, for app functionality". |

Expected label: **"No data collected · No data shared"**, after the fallback change.

### 2.7 Store listing and metadata policy
| Rule | What we do |
|---|---|
| Icon 512×512, 32-bit PNG, ≤1 MB | ✓ `brand/play-store/icon-512.png` (512×512 RGBA, 38 KB) |
| Feature graphic 1024×500, JPEG or 24-bit PNG | ✓ size and format, **✗ text says "Block ads and trackers"**: change `brand/svg/feature-graphic.svg` and regenerate (e.g. "Block trackers and harmful sites") |
| 2–8 phone screenshots | **Missing.** Take them from the Play build (section 5.3) |
| Title ≤30, short description ≤80, full description ≤4000 characters | Drafts in section 5 |
| No misleading claims, rankings ("best", "#1"), keyword stuffing, or other brands' names except to state facts | Drafts follow this; Cloudflare/Google are mentioned only in the factual fallback sentence |
| A privacy policy link in the listing and inside the app | ✓ both exist; update the text (section 4) |

### 2.8 App content forms
| Form | Answer |
|---|---|
| Ads | **No**, the app contains no ads |
| App access | All functions available without login. Note: "Pro features are a one-time in-app purchase." |
| Content rating | Utility / tools. No user content, no adult content shown. With the adult filter, **hide blocked adult domain names** on the Blocked screen (show "Adult site blocked") so the app never shows adult words. |
| Target audience | **18+** (or 13+). Not children, so the Families policy doesn't apply. "Parental control" means a tool *for parents*. |
| VPN declaration | Section 7.1 |
| Foreground service | Only if asked (section 2.5) |
| Data safety | Section 2.6 |
| Government / financial / health / news | Not applicable |

### 2.9 Developer account
- Personal accounts created after 13 Nov 2023 must run a **closed test with at least 12 testers
  opted in for the last 14 days in a row**, then apply for production
  ([official](https://support.google.com/googleplay/android-developer/answer/14151465)).
  Unofficial guides say the 14-day clock restarts if testers drop below 12.
- USD 25 fee and identity verification. An organisation account needs a D-U-N-S number but skips the 12-tester rule.
- A **payments profile** (merchant account) is required to sell Pro. Bangladesh has been supported
  since 2017, with payouts in USD to a bank account.
- **Android developer verification:** Play apps are covered by your Play Console identity. It
  matters for the *full* build you sideload: it started in Brazil, Indonesia, Singapore and Thailand
  on 30 Sep 2026 and goes worldwide from 2027
  ([official](https://support.google.com/android-developer-console/answer/16561738)).
  Register the full build's package too.

### 2.10 Licences
- The repo has **no LICENSE file**. Decide one before selling Pro. Bundling GPL-3.0 lists (HaGeZi,
  EasyPrivacy) inside a paid closed app is a gray area. Two clean options: make the app open source
  under GPL-3.0 (you can still sell Pro on Play), or bundle only permissive or CC0 lists (URLhaus is CC0).
- Add an in-app "Licences and list credits" screen. The `blocklist_url` link must point to a page
  that lists the Play sources only.

### 2.11 Quality (pre-launch report and Android vitals)
- Robo tests tap around: the app must not crash when the VPN or notification prompt is cancelled.
- Large lists (Pro newly-registered-domains list): watch memory; `largeHeap` is already on. Test
  on a 2 GB RAM emulator during an update.
- Keep the update work off the main thread (it is, `Dispatchers.IO`) and show progress.

---

## 3. Blocklists for the Play version
*Classify each from its own README before shipping. Licences marked "check" are unverified.*

### 3.1 Free lists
| List | Type | Licence | From the current 17? |
|---|---|---|---|
| EasyPrivacy | Trackers | GPL-3.0 / CC BY-SA 3.0 | Yes (#7) |
| r-a-y/mobile-hosts AdguardMobileSpyware | Mobile trackers/spyware | check | Yes (#2) |
| FadeMind add.2o7Net | Adobe tracking | check | Yes (#3) |
| quidsup NoTrack trackers | Trackers | check (GPL-3.0?) | Yes (#13) |
| HaGeZi Native Tracker (per vendor) | OS/phone telemetry | GPL-3.0 | New |
| HaGeZi Threat Intelligence Feeds (TIF) | Malware, phishing, scam | GPL-3.0 | New |
| abuse.ch URLhaus (hostfile) | Malware | CC0 | New |
| Phishing Army | Phishing | check | New |

Drop the other 13 current sources from the Play build (ads or mixed; notracking is unmaintained).
Expect roughly 150k–400k domains, so never say "1 million".

### 3.2 Pro lists
| List | Feature |
|---|---|
| HaGeZi NSFW | Adult filter |
| HaGeZi Gambling | Gambling filter |
| HaGeZi DoH/VPN/TOR/Proxy bypass | Bypass blocking |
| HaGeZi newly registered domains (NRD, 14 days) | Extra security (large; test memory) |
| NoCoin (hoshsadiq/adblock-nocoin-list) | Cryptojacking |
| HaGeZi stalkerware / AssoEchap stalkerware indicators | Stalkerware |

### 3.3 Built-in offline list
Replace anti-AD (`assets/hosts.txt`) with a small tracker + malware list (e.g. URLhaus + a trimmed
tracker list), within the licence choice in 2.10. Check that `MIN_DOMAINS = 10_000`
(`Blocklist.kt:47`) still makes sense.

### 3.4 Optional safety step
When merging, drop any domain that is also on a well-known ads-only list. Then the Play build
provably doesn't block ad delivery, which is easy to explain if a reviewer asks.

---

## 4. Text that must change (Play build)

| Where | Now | Play version |
|---|---|---|
| `status_on_detail` | Tracking and ad domains are being blocked on this phone. | Known trackers, malware and phishing sites are being blocked on this phone. |
| `disclosure_text` | …Lookups for known tracking and ad domains are blocked… | PrivacyGuard uses a local VPN on this phone to check website lookups (DNS). Lookups for known trackers, malware and phishing sites are blocked; all others go to your normal DNS server.\n\nNo VPN server is used. Nothing is sent to us, and no data leaves your phone because of this app. |
| `notification_text` | Blocking tracking and ad domains on this phone | Blocking trackers and harmful sites on this phone |
| `blocklist_url` | README#blocklist-sources (lists ad lists) | A page listing the Play sources only |
| Feature graphic | "Block ads and trackers on your whole phone" | "Block trackers and harmful sites on your whole phone" |
| `docs/index.html` (privacy policy) | "…blocks known tracking and advertising domains…", "updated for version 1.3" | "…blocks known tracking, malware and phishing domains…"; current version and date; a **Purchases** section (Google Play handles payment; the app only checks on the phone whether Pro was bought); the fallback as opt-in; the Pro list hosts |
| README | "blocks ad and tracker domains" | A section for the Play version; keep the full version elsewhere, not linked from the app |

Words never to use in the Play build, listing or screenshots: ad, ads, adblock, ad blocker,
"remove ads", "block ads", "1 million", best, #1, "100% protection", "blocks everything".

---

## 5. Store listing draft

### 5.1 Text
- **Title (≤30):** `PrivacyGuard: Tracker Blocker` (29). Change if the name check finds a conflict.
- **Short description (≤80):** `Block trackers, malware and phishing on your whole phone. No account needed.` (77)
- **Full description (draft):**

> PrivacyGuard blocks known trackers, malware and phishing sites in every app and browser on your phone.
>
> **How it works**
> PrivacyGuard checks the website names your phone looks up (DNS). If a name is on a blocklist of
> known trackers or harmful sites, the lookup is stopped on your phone. Everything else works as normal.
>
> **What it blocks**
> • Trackers and analytics that follow you across apps and sites
> • Telemetry sent by phone makers' system apps
> • Malware, phishing and scam sites
>
> **Private by design**
> • No account and no sign-up
> • No data collected: your blocked list stays on your phone
> • No VPN server: nothing is routed through us
>
> **PrivacyGuard Pro (one-time purchase)**
> • Adult content and gambling filters
> • PIN lock for filter settings
> • Extra security lists and your own block/allow rules
> • Blocked history and daily stats, kept on your phone
>
> **About the VPN permission**
> PrivacyGuard uses Android's VpnService to run a local filter on your phone. Only DNS lookups go
> through it; no other traffic does. It does not connect to a VPN server, change your IP address
> or encrypt your traffic.
>
> **Good to know**
> • If Private DNS is set to a specific provider in Android settings, set it to Off or Automatic so
>   the filter can work.
> • Blocklists are public projects; credits and licences are listed in the app.

### 5.2 Graphics
- Icon: ✓ `brand/play-store/icon-512.png`
- Feature graphic: change the text (section 4), then regenerate with `brand/generate.py`.

### 5.3 Screenshots (Play build, `adb exec-out screencap -p > N.png`)
1. Home, protection on
2. Blocked screen with tracker domains
3. Filters screen (list size, last update)
4. Disclosure dialog
5. Pro screen (adult and gambling filters, no adult words visible)
6. Optional: Quick Settings tile

No ad domains, no "ads", no other brands' logos, no fake numbers.

---

## 6. Pro: how to build it within the rules
- Add Play Billing Library (latest 8.x/9.x) to the **play** source set only.
- One non-consumable product `pro_unlock`, created in Play Console → Monetize → In-app products.
- Purchase flow: `launchBillingFlow` → on `PURCHASED`, acknowledge → save "Pro" on the phone.
  On `PENDING`, show "Payment pending" and unlock once it completes.
- On each launch: `queryPurchasesAsync` to restore Pro on new phones and to remove it after a refund.
- No server and no account, so Data safety stays "no data collected".
- Pro screen: show the localized price from `ProductDetails`. No countdowns, fake discounts or
  pressure wording.
- Test with **licence testers** (Play Console → Settings → License testing) on the internal track.
- Free must be useful on its own: tracker and security blocking are never behind the paywall.

---

## 7. Play Console answers

### 7.1 VPN declaration
- **Is providing a VPN the core functionality?** No.
- **Permitted functionality:** Device security (firewall). Add Parental control only if the Pro PIN lock exists.
- **Description:** "PrivacyGuard is an on-device DNS firewall. It uses VpnService to create a
  local-only tunnel that receives only DNS queries sent to one internal address. Queries for known
  tracker, malware and phishing domains (and, if the user turns them on, adult or gambling domains)
  are answered on the device; all other queries are forwarded unchanged to the device's DNS server.
  No other traffic enters the tunnel, there is no remote VPN server, and the app does not block,
  change or redirect ads or the monetization of other apps."
- **Data collected or shared through VpnService:** None. The blocked list stays on the device.
- **Redirects or manipulates traffic for monetization?** No.
- **Videos:** (1) open the app → disclosure → Android VPN prompt → browse → Blocked screen showing
  tracker domains, ≤90 s. (2) The disclosure dialog with Continue and Cancel.

### 7.2 Other forms
See section 2.8 (forms) and 2.6 (Data safety).

---

## 8. Work order (when you approve; small commits, no code changes yet)

**Step 1: Decisions** (section 10).

**Step 2: Build structure**
1. Add product flavors `play` / `full` in `app/build.gradle.kts` (`full` gets `applicationIdSuffix = ".full"`).
2. Move `Blocklist.SOURCES` to per-flavor files; add the Pro lists to `play`.
3. Per-flavor `assets/hosts.txt` and `res/values/strings.xml` overrides (section 4).
4. `play`-only dependencies: Play app update (already used) and Play Billing.
5. `release.yml`: `bundlePlayRelease` for Play (not attached to GitHub releases), `assembleFullRelease` for GitHub.

**Step 3: Features**
6. Pro purchase screen and billing (section 6).
7. Adult and gambling filters, with masked names on the Blocked screen.
8. PIN lock (if parental control is chosen), bypass list, My rules, on-device history.
9. Public DNS fallback as an opt-in setting (section 2.6).
10. Licences and credits screen.
11. Optional: Quick Settings tile; ads-only subtraction (section 3.4).

**Step 4: Tests and checks**
12. Unit tests: the `play` sources contain no ads-only list; Pro gating; purchase state handling.
13. `./gradlew testPlayDebugUnitTest testFullDebugUnitTest lintPlayDebug assemblePlayDebug bundlePlayRelease`
    (with the Android Studio JBR `JAVA_HOME`).
14. On a phone: Home, Blocked, Filters, disclosure, notification, Pro purchase (licence tester), refund removes Pro.

**Step 5: Docs and assets**
15. Privacy policy, README, feature graphic text, screenshots, `play-store/listing.md`.

**Step 6: Play Console**
16. Developer account → payments profile → create the app → internal testing (try the purchase and the update banner).
17. Fill in all App content forms (section 7).
18. Closed test: 12+ testers for 14 days (a Google Group as the tester list; ship a small update
    each week so testers keep opening the app).
19. Apply for production → staged rollout 20% → 100%.

---

## 9. If Play rejects it
1. Read the email: it names the **policy** and often the **screen or text** at fault.
2. Fix exactly that, bump versionCode, resubmit. Don't argue with a clear finding.
3. Appeal (Policy status page → Appeal) only if the reviewer misread the app, for example by
   thinking it blocks ads. Attach the declaration text from 7.1 and a short video.
4. Never resubmit the same build unchanged; repeated rejections can lead to account strikes.

---

## 10. Decisions needed from you
1. **Store name:** keep "PrivacyGuard" after a Play and trademark search, or pick another.
2. **Package names:** Play `com.pg.privacyguard` (permanent after the first upload), full `com.pg.privacyguard.full`.
3. **Parental control:** add the Pro PIN lock and tick Parental control, or stay firewall-only.
4. **Pro price.**
5. **App licence:** open source (GPL-3.0) or closed with permissive-only bundled lists.
6. **DNS fallback:** opt-in setting (recommended) or declare it in Data safety.
7. **Ads-only subtraction step:** yes or no.
8. **Developer account:** personal (12 testers / 14 days) or organisation (D-U-N-S).

---

## 11. Sources (read 2026-10-21; re-check before submitting)
- VpnService policy: https://support.google.com/googleplay/android-developer/answer/12564964
- Device and Network Abuse: https://support.google.com/googleplay/android-developer/answer/9888379
- Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
- Alternative billing programs (UK, EEA, US only): https://support.google.com/googleplay/android-developer/answer/17161464
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Foreground service declarations: https://support.google.com/googleplay/android-developer/answer/13392821
- Target API: https://developer.android.com/google/play/requirements/target-sdk
- Testing requirements for new personal accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Android developer verification: https://support.google.com/android-developer-console/answer/16561738
- Play Billing Library 8 deadline (unofficial summaries of Google's notice): https://www.revenuecat.com/blog/engineering/play-billing-8-migration/
