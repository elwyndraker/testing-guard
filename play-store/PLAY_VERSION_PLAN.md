# PrivacyGuard: Play version plan

Written 2026-10-21 against `elwyndraker/testing-guard` @ `b3c93a4` (v0.1, versionCode 2,
package `com.pg.privacyguard`). **This is a plan only. No code has been changed yet.**

This file is for everyone on the team. It explains **what** we are building for Google Play,
**why** some things we wanted (full ad blocking, SSLCommerz payments) are not possible there, and
**how** to build and publish the app so Play doesn't reject it.

## Decisions at a glance

| Decision | Choice |
|---|---|
| Where the app is published | **Google Play only** (one app, one build) |
| Package name | `com.pg.privacyguard` (permanent after the first Play upload) |
| What it blocks | **Trackers, phone telemetry, malware, phishing, scams.** Never ads. |
| How we earn money | A one-time **Pro** upgrade, paid through **Google Play Billing** |
| SSLCommerz | **Not used** (section 3 explains why) |
| A separate "full" ad-blocking build on GitHub or a website | **Not made** |

---

## 1. Features

### 1.1 Free
| Feature | Status today | Work needed |
|---|---|---|
| Whole-phone DNS filter (local VPN, no server) | Exists | None |
| **Tracker and analytics blocking** | Exists, but with ad lists | Replace the lists (section 5.1) |
| **Phone / OS telemetry blocking** (Xiaomi, Samsung, Huawei, OPPO/realme, etc.) | New | Add the HaGeZi Native Tracker lists |
| **Malware, phishing and scam protection** | New | Add URLhaus, HaGeZi Threat Intelligence, Phishing Army |
| Blocked requests screen (live, in memory only) | Exists | None |
| Automatic weekly updates + "Update now" | Exists | None |
| Works offline from first launch | Exists, but the built-in list is an ad list | Replace `assets/hosts.txt` (section 5.3) |
| Private DNS tip | Exists | None |
| Quick Settings tile (on/off) | New, optional | Keep it free; basic controls shouldn't be paywalled |

### 1.2 Pro (one-time purchase through Google Play Billing)
| Feature | Play category it fits | Notes |
|---|---|---|
| **Adult content filter** | Content filter (firewall) / parental control | Lists in section 5.2 |
| **Gambling filter** | Same | |
| **PIN lock** for filter settings and the on/off switch | Parental control | Only needed if we tick "Parental control" on the VPN form (section 4.1) |
| Block filter bypass (public DoH, proxy and VPN-bypass domains) | Security / parental control | Stops apps and browsers skipping the filter |
| Extra security lists: newly registered domains, cryptojacking, stalkerware | Device security | Newly registered domain lists are very large; test memory (section 4.11) |
| My rules: block or allow single domains | Firewall | Typed one at a time, never a list URL (section 2.4) |
| Blocked history on the phone (e.g. 7 days) + daily stats | — | Stays on the phone, so Data safety stays "no data collected" |

**Price:** one non-consumable product, e.g. `pro_unlock`, about **USD 1.99**, with Play's local price
for Bangladesh. Google keeps **15%** (on a developer's first USD 1M a year). The app always shows the
price Play gives it; never hard-code it.

**Rule:** the free app must be useful on its own. Tracker, malware and phishing blocking are never
behind the paywall.

### 1.3 Never in this app
- Ad lists, ad presets, or any text, screenshot, video or link about blocking ads.
- "Add a list from a URL" (section 2.4).
- Any payment method other than Google Play Billing, or links to one.
- Downloading APKs or updating outside Google Play.

---

## 2. Why we can't do full ad blocking

### 2.1 What we wanted
The first version of PrivacyGuard merged **17 public blocklists** (about 1 million domains). 7 of
them are ad-only lists, 6 mix ads and trackers, and only 4 are tracker-only. The built-in offline
list (anti-AD) is an ad list too. The app, its notification and its privacy policy all said it
blocks "tracking and ad domains".

### 2.2 The rules that stop it
PrivacyGuard works by using Android's **VpnService**, so Google's VPN rules apply to it.

**VpnService policy** ([official](https://support.google.com/googleplay/android-developer/answer/12564964)):
- An app may use VpnService only if VPN is its main purpose, or if it is in one of these categories:
  "Parental control and enterprise management apps", "Device security apps (for example,
  anti-virus, mobile device management, firewall)", "Network-related tools (for example, remote
  access)", "Web browsing apps", "App usage tracking", "Carrier apps…".
  **Ad blocking is not on the list.**
- "**Any manipulation of ads that can impact monetization will result in rejection.**"

**Device and Network Abuse policy** ([official](https://support.google.com/googleplay/android-developer/answer/9888379)):
- Listed as a violation: "**Apps that block or interfere with another app displaying ads.**"

In plain words: a VPN app on Play may block trackers and dangerous sites (that counts as a
firewall), but it may not block ads. Breaking this means the app is rejected, or removed later, and
the developer account can get a strike or be banned.

### 2.3 "But we only block website ads, not app ads"
That doesn't help:
- **DNS can't tell a website's ad from an app's ad.** The same ad domains (for example
  `doubleclick.net`) serve both. Blocking one blocks both.
- **The browser is "another app".** Blocking ads that Chrome shows is interfering with another app displaying ads.
- **Websites earn money from ads too**, so it is still "manipulation of ads that can impact monetization".

### 2.4 Workarounds we looked at, and why they fail
| Idea | Why it fails |
|---|---|
| Ship the ad lists hidden, turn them on after Play approves the app | The app reviewers test must be the app users get. This is deceptive behaviour and can get the developer account banned. |
| Let users paste ad list links, and give them the links | We are still supplying ad blocking, just one step removed. |
| Ad lists as presets or one-tap options | Same as shipping them. |
| A neutral "add any list URL" feature with no ad lists suggested | No written rule bans it, but users would add ad lists and Google can still decide the app blocks ads. Similar apps have been removed or had updates held. **Not worth the risk for a first app.** |
| Keep the ad lists but stop saying "ads" | Reviewers test what the app does, not only what it says. |
| Pay to unlock the ad lists (Pro) | Still ad blocking, and now we earn money from it, which is worse. |

### 2.5 "But AdGuard is on Play and blocks ads"
We checked this (AdGuard Content Blocker, `com.adguard.android.contentblocker`):
- Its Play listing says: "Adblocker **Only for Yandex.Browser and Samsung Internet**." Its open-source
  code has **no VPN**. It hands filter lists to those two browsers through their official ad-blocker
  add-on system, and **the browser blocks ads inside its own pages**. That is allowed, the same way a
  browser with a built-in ad blocker is allowed.
- On a phone that uses Chrome, it blocks nothing; when opened it asks you to pick Samsung Internet
  or Yandex Browser. **Chrome on Android has no add-on system for ad blockers**, so no Play app can
  block ads inside Chrome the allowed way.
- AdGuard's real whole-phone ad blocker ("AdGuard for Android") is **not on Play**. AdGuard says
  Google removed it on 25 Nov 2014
  ([AdGuard blog](https://adguard.com/en/blog/adguard-google-play-removal.html)), and it is sold
  from AdGuard's website instead.

So AdGuard confirms the rule: whole-phone ad blocking is not allowed on Play.

### 2.6 What PrivacyGuard can honestly say on Play
"Blocks trackers, malware and phishing on your whole phone." Some tracker domains belong to ad
companies, so a few ads may also disappear. We **never** claim, advertise or show that.

---

## 3. Why we can't use SSLCommerz

### 3.1 What we wanted
Charge a one-time fee through SSLCommerz (a Bangladeshi payment gateway) to unlock extra lists or
features.

### 3.2 The rules that stop it
**Payments policy** ([official](https://support.google.com/googleplay/android-developer/answer/9858738)):
- Apps that charge for in-app features or content "**must use Google Play's billing system**".
- Apps "**may not lead users to a payment method other than Google Play's billing system**". That
  rules out an SSLCommerz checkout in the app, a webview, a button, or a "pay on our website" link.
- Google's programs that allow other payment systems only cover the **UK, the EEA and the US**
  ([official](https://support.google.com/googleplay/android-developer/answer/17161464)).
  **Bangladesh is not covered.**
- Other payment systems are only allowed for **physical goods or real-world services**. Pro is a
  digital feature, so that doesn't apply.

Also, the extra lists we wanted to sell were the ad lists, which section 2 already rules out.

### 3.3 What we use instead: Google Play Billing
- Google handles payment (cards, mobile wallets and carrier billing where available) and refunds.
- Google keeps 15% (first USD 1M a year).
- Bangladesh has had Play merchant accounts since 2017, paid out in USD to a bank account.
- No server, no trade licence or merchant account with a gateway, and no payment data in our app.

---

## 4. Play rules this app must follow

### 4.1 VpnService
| Rule | What we do |
|---|---|
| Must fit an allowed category | Declare **Device security → firewall**. Add **Parental control** only if Pro has the PIN lock. |
| No ad manipulation | No ad lists (section 5). Optional: remove from the merged list any domain that is also on a known ads-only list (section 5.4). |
| No redirecting traffic for monetization | DNS only; allowed lookups forwarded unchanged. |
| "Document use of the VpnService in the Google Play listing" | A fixed paragraph in the description (section 8). |
| "encrypt the data from the device to the VPN tunnel endpoint" | The tunnel never leaves the phone (no remote endpoint). Say so plainly in the declaration. If a reviewer insists, the fix is DNS-over-HTTPS upstream (a later code change). |
| Prominent disclosure and consent | **Already OK**: the dialog shows before Android's VPN prompt (`HomeScreen.kt:141`); Cancel doesn't start the VPN, Continue does. Update its text only (section 6). |
| VPN declaration form with a ≤90 s video of the VPN in use and a video of the disclosure | Section 9.1. |

### 4.2 Device and Network Abuse
| Rule | What we do |
|---|---|
| Don't block or interfere with other apps' ads | Section 2. |
| An app "may not modify, replace, or update itself using any method other than Google Play's update mechanism" | Keep the Play in-app update banner (allowed). No APK downloads or GitHub update checks. Blocklists are data, not code, so downloading them is fine. |

### 4.3 Deceptive behaviour
- What reviewers test must be what users get: no remote switch that changes what the app blocks.
- The listing, screenshots and videos must match the real app.

### 4.4 Payments
| Rule | What we do |
|---|---|
| Use Google Play Billing for Pro | Section 7. |
| No other payment methods or links to them | None in the app, listing or screenshots. |
| Play Billing Library **8.0.0 or later** for new apps and updates from **31 Aug 2026** (extension to 1 Nov 2026) | Use the latest 8.x/9.x. |
| Acknowledge purchases (unacknowledged ones are refunded after 3 days) | Acknowledge right after a successful purchase; handle **pending** payments. |
| Refunds | Re-check purchases on launch; remove Pro if refunded. |

### 4.5 Technical requirements
| Rule | State | What we do |
|---|---|---|
| New apps and updates must target **API 36** from 31 Aug 2026 ([official](https://developer.android.com/google/play/requirements/target-sdk)) | ✓ targetSdk 36 | Expect API 37 to be required around Aug 2027. |
| Upload an **AAB**, with Play App Signing | ✓ `release.yml` builds an AAB | Section 10, step 5. |
| versionCode increases on every upload | ✓ comment in `build.gradle.kts` | Bump before every upload. |
| 16 KB memory page support (apps with native libraries) | ✓ no `.so` files | Re-check with APK Analyzer after adding Billing. |
| Foreground service types declared on the App content page (targetSdk 34+) ([official](https://support.google.com/googleplay/android-developer/answer/13392821)) | `systemExempted` | `systemExempted` isn't in that page's list. If Play Console asks: "keeps the user-enabled local VPN filter running". |
| Notification permission (Android 13+) | ✓ requested at runtime (`HomeScreen.kt:124`) | — |

### 4.6 Data safety ([official](https://support.google.com/googleplay/android-developer/answer/10787469))
Google defines "collect" as "transmitting data from your app off a user's device". Data "only
processed locally on the user's device" doesn't count.

| Data flow | Leaves the phone? | Answer |
|---|---|---|
| Blocked list, history, stats, settings, Pro status | No | Not declared |
| Play Billing purchase | Google collects payment details directly; the app only reads the purchase state on the phone | Not declared, **as long as no server checks purchases** |
| In-app update check | Handled by the Play Store app | Not declared |
| Blocklist downloads | Only an HTTP request (the list host sees your IP) | Usually not declared; listed in the privacy policy |
| Allowed lookups forwarded to **the network's DNS** | Yes, as the phone would do anyway | Not declared (normal behaviour; in the privacy policy) |
| Allowed lookups sent to the **1.1.1.1 / 8.8.8.8 fallback** | Yes, to a third party the app chose | **⚠ Unclear.** Domain names can count as "web browsing history". **Safer:** make the public fallback an opt-in setting. Otherwise declare "Web browsing history → shared, processed ephemerally, for app functionality". |

Expected label after the fallback change: **"No data collected · No data shared"**.

### 4.7 Store listing and metadata policy
| Rule | What we do |
|---|---|
| Icon 512×512, 32-bit PNG, ≤1 MB | ✓ `brand/play-store/icon-512.png` (512×512 RGBA, 38 KB) |
| Feature graphic 1024×500, JPEG or 24-bit PNG | ✓ size and format, **✗ says "Block ads and trackers"**: change `brand/svg/feature-graphic.svg`, regenerate with `brand/generate.py` |
| 2–8 phone screenshots | **Missing**, section 8.3 |
| Title ≤30, short description ≤80, full description ≤4000 characters | Section 8.1 |
| No misleading claims, rankings ("best", "#1"), keyword stuffing, or other brands' names except to state facts | Drafts follow this |
| Privacy policy link in the listing and inside the app | ✓ both exist; update the text (section 6) |

### 4.8 App content forms
| Form | Answer |
|---|---|
| Ads | **No**, the app contains no ads |
| App access | Everything works without login. Note: "Pro features are a one-time in-app purchase." |
| Content rating | Utility / tools. With the adult filter, **hide blocked adult domain names** on the Blocked screen (show "Adult site blocked") so the app never shows adult words. |
| Target audience | **18+** (or 13+). Not children, so the Families policy doesn't apply. "Parental control" means a tool *for parents*. |
| VPN declaration | Section 9.1 |
| Foreground service | Only if asked (section 4.5) |
| Data safety | Section 4.6 |
| Government / financial / health / news | Not applicable |

### 4.9 Developer account
- Personal accounts created after 13 Nov 2023 must run a **closed test with at least 12 testers
  opted in for the last 14 days in a row**, then apply for production
  ([official](https://support.google.com/googleplay/android-developer/answer/14151465)).
  Unofficial guides say the 14-day clock restarts if testers drop below 12.
- USD 25 one-time fee and identity verification. An organisation account needs a D-U-N-S number but skips the 12-tester rule.
- A **payments profile** (merchant account) is needed to sell Pro.

### 4.10 Licences
- The repo has **no LICENSE file**. Decide one before selling Pro. Bundling GPL-3.0 lists (HaGeZi,
  EasyPrivacy) in a paid closed-source app is a gray area. Two clean options: make the app open
  source under GPL-3.0 (you can still sell Pro on Play), or bundle only permissive or CC0 lists
  (URLhaus is CC0) and let the app download the GPL lists.
- Add an in-app "Licences and list credits" screen. The `blocklist_url` link must show the Play lists only.

### 4.11 Quality (pre-launch report and Android vitals)
- Google's robot taps around the app before release: it must not crash when the VPN or notification prompt is cancelled.
- Big Pro lists (newly registered domains) use memory; `largeHeap` is on. Test on a 2 GB RAM emulator during an update.
- List updates already run off the main thread (`Dispatchers.IO`); show progress so it never looks frozen.

---

## 5. Blocklists
*Check each list's own README before shipping. Licences marked "check" are not verified yet.*

### 5.1 Free lists
| List | Type | Licence | In the current 17? |
|---|---|---|---|
| EasyPrivacy | Trackers | GPL-3.0 / CC BY-SA 3.0 | Yes |
| r-a-y/mobile-hosts AdguardMobileSpyware | Mobile trackers/spyware | check | Yes |
| FadeMind add.2o7Net | Adobe tracking | check | Yes |
| quidsup NoTrack trackers | Trackers | check | Yes |
| HaGeZi Native Tracker (per phone maker) | OS / phone telemetry | GPL-3.0 | New |
| HaGeZi Threat Intelligence Feeds (TIF) | Malware, phishing, scam | GPL-3.0 | New |
| abuse.ch URLhaus (hostfile) | Malware | CC0 | New |
| Phishing Army | Phishing | check | New |

Remove the other 13 current sources (ads or mixed; notracking is no longer maintained). Expect
roughly 150k–400k domains instead of about 1 million, so never say "1 million".

### 5.2 Pro lists
| List | Feature |
|---|---|
| HaGeZi NSFW | Adult filter |
| HaGeZi Gambling | Gambling filter |
| HaGeZi DoH/VPN/TOR/Proxy bypass | Bypass blocking |
| HaGeZi newly registered domains (14 days) | Extra security (large; test memory) |
| NoCoin (hoshsadiq/adblock-nocoin-list) | Cryptojacking |
| A stalkerware list (HaGeZi or AssoEchap indicators) | Stalkerware |

### 5.3 Built-in offline list
Replace anti-AD (`app/src/main/assets/hosts.txt`) with a small tracker + malware list (e.g. URLhaus
+ a trimmed tracker list), within the licence choice in 4.10. Check that `MIN_DOMAINS = 10_000`
(`Blocklist.kt:47`) still fits.

### 5.4 Optional safety step
When merging, drop any domain that is also on a well-known ads-only list. Then the app provably
doesn't block ad delivery, which is easy to show a reviewer.

---

## 6. Text that must change

| Where | Now | New |
|---|---|---|
| `status_on_detail` | Tracking and ad domains are being blocked on this phone. | Known trackers, malware and phishing sites are being blocked on this phone. |
| `disclosure_text` | …Lookups for known tracking and ad domains are blocked… | PrivacyGuard uses a local VPN on this phone to check website lookups (DNS). Lookups for known trackers, malware and phishing sites are blocked; all others go to your normal DNS server.\n\nNo VPN server is used. Nothing is sent to us, and no data leaves your phone because of this app. |
| `notification_text` | Blocking tracking and ad domains on this phone | Blocking trackers and harmful sites on this phone |
| `blocklist_url` | README#blocklist-sources (lists ad lists) | The new list credits (in the app or the README) |
| Feature graphic | "Block ads and trackers on your whole phone" | "Block trackers and harmful sites on your whole phone" |
| Privacy policy `docs/index.html` | "…blocks known tracking and advertising domains…", "updated for version 1.3" | "…blocks known tracking, malware and phishing domains…"; current version and date; a **Purchases** section (Google Play handles payment; the app only checks on the phone whether Pro was bought); the fallback as opt-in; the new list hosts |
| README | "blocks ad and tracker domains", 17 lists, GitHub APK steps | Tracker/security wording, the new lists, "Get it on Google Play" |

Words never to use in the app, listing or screenshots: ad, ads, adblock, ad blocker, "remove ads",
"block ads", "1 million", best, #1, "100% protection", "blocks everything".

---

## 7. Pro: how to build it with Google Play Billing
1. Add Play Billing Library (latest 8.x/9.x) to `app/build.gradle.kts` via `gradle/libs.versions.toml`.
2. Create one **non-consumable** in-app product `pro_unlock` in Play Console → Monetize → In-app products.
3. Purchase: `launchBillingFlow` → on `PURCHASED`, **acknowledge** → save "Pro" on the phone
   (DataStore, next to the existing settings). On `PENDING`, show "Payment pending" and unlock when it completes.
4. On every launch: `queryPurchasesAsync` to restore Pro on a new phone and remove it after a refund.
5. No server and no account, so Data safety stays "no data collected".
6. Pro screen: show the local price from `ProductDetails`. No countdowns, fake discounts or pressure wording.
7. Test with **licence testers** (Play Console → Settings → License testing) on the internal testing track.

---

## 8. Store listing draft

### 8.1 Text
- **Title (≤30):** `PrivacyGuard: Tracker Blocker` (29). Change it if the name check finds a conflict.
- **Short description (≤80):** `Block trackers, malware and phishing on your whole phone. No account needed.` (76)
- **Full description:**

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

### 8.2 Graphics
- Icon: ✓ `brand/play-store/icon-512.png`
- Feature graphic: change the text (section 6), then regenerate.

### 8.3 Screenshots (`adb exec-out screencap -p > N.png`)
1. Home, protection on
2. Blocked screen with tracker domains
3. Filters screen (list size, last update)
4. Disclosure dialog
5. Pro screen (adult and gambling filters; no adult words visible)
6. Optional: Quick Settings tile

No ad domains, no "ads", no other brands' logos, no made-up numbers.

---

## 9. Play Console answers

### 9.1 VPN declaration
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
  Record with `adb shell screenrecord`.

### 9.2 Other forms
Section 4.8 (forms) and section 4.6 (Data safety).

---

## 10. Work order (after the team approves)
Small commits, one topic each. Build with the Android Studio JBR `JAVA_HOME`.

**Step 1: Decisions** (section 13).

**Step 2: Remove ad blocking**
1. Replace `Blocklist.SOURCES` (`data/Blocklist.kt`) with the free lists (5.1).
2. Replace `assets/hosts.txt` (5.3).
3. Change the strings (section 6).
4. Optional: the ads-only subtraction (5.4).

**Step 3: Pro**
5. Play Billing and the Pro screen (section 7).
6. Adult and gambling filters, with masked names on the Blocked screen.
7. PIN lock (if parental control is chosen), bypass list, My rules, on-device history.

**Step 4: Other changes**
8. Public DNS fallback as an opt-in setting (4.6).
9. Licences and list credits screen; add a LICENSE file (4.10).
10. Optional: Quick Settings tile.

**Step 5: Release pipeline**
11. `release.yml`: build the signed **AAB** for Play upload (keep it as a workflow artifact).
    **Stop attaching APKs to public GitHub releases.** The app is distributed through Play only.
    Mark the existing `v0` and `v0.1` GitHub releases as test builds, or delete them.
12. Play App Signing: Google generates the app signing key; `~/PrivacyGuard-signing/privacyguard-upload.jks`
    becomes the **upload key**. Keep a backup. Anyone with a GitHub APK installed must uninstall it
    before installing from Play (the signatures differ).

**Step 6: Tests and checks**
13. Unit tests: no ads-only list in `SOURCES`; Pro gating; purchase states (purchased, pending, refunded).
14. `./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease`
15. On real phones (Xiaomi, Pixel, another brand): Home, Blocked, Filters, disclosure, notification,
    Pro purchase with a licence tester, refund removes Pro, VPN prompt cancelled, no crash.

**Step 7: Docs and assets**
16. Privacy policy, README, feature graphic text, screenshots, `play-store/listing.md`.

---

## 11. Testing and release on Play
1. **Developer account:** pay USD 25, verify your identity, set up a payments profile.
2. **Create the app** in Play Console, fill in the store listing and every App content form (section 9).
3. **Internal testing** (up to 100 testers, available within minutes): upload the AAB, test the Pro
   purchase with licence testers, and test the in-app update banner by uploading a second build with a
   higher versionCode.
4. **Closed testing:** at least **12 testers** opted in for **14 days in a row**. A Google Group works as
   the tester list (classmates, friends, family). Ship a small update each week so testers keep using
   the app, and ask for feedback (Google asks about this when you apply).
5. **Apply for production** from the Play Console dashboard.
6. **Production:** staged rollout 20% → watch crashes and ANRs in Android vitals → 100%.

---

## 12. If Play rejects the app
1. Read the email: it names the **policy** and often the **screen or text** at fault.
2. Fix exactly that, bump versionCode, resubmit. Don't argue with a clear finding.
3. Appeal (Policy status page → Appeal) only if the reviewer misread the app, for example by
   thinking it blocks ads. Attach the declaration text from 9.1 and a short video.
4. Never resubmit the same build unchanged; repeated rejections can lead to account strikes.

---

## 13. Decisions still needed
1. **Store name:** keep "PrivacyGuard" after a Play and trademark search, or pick another.
2. **Parental control:** add the Pro PIN lock and tick Parental control, or stay firewall-only.
3. **Pro price.**
4. **App licence:** open source (GPL-3.0), or closed source with only permissive lists bundled.
5. **DNS fallback:** opt-in setting (recommended) or declare it in Data safety.
6. **Ads-only subtraction step:** yes or no.
7. **Developer account:** personal (12 testers / 14 days) or organisation (D-U-N-S number).

---

## 14. Glossary
| Term | Meaning |
|---|---|
| DNS | The phone asking "what's the address of example.com?" before connecting. PrivacyGuard answers "doesn't exist" for blocked names. |
| VpnService | Android's API for VPN apps. PrivacyGuard uses it only to see DNS lookups on the phone; there's no VPN server. |
| AAB | Android App Bundle, the file you upload to Play. Play builds the APKs from it. |
| Play App Signing | Google keeps the key that signs the app for users; you sign uploads with your own upload key. |
| versionCode | A number that must go up with every upload. versionName (e.g. 0.1) is what users see. |
| Non-consumable product | Something bought once and kept forever, like Pro. |
| Licence tester | An account that can buy in your app without being charged, for testing. |
| Internal / closed / production testing | Internal = quick private tests; closed = invited testers (needed 14 days for new accounts); production = public. |
| Data safety | The form behind the "data collected / shared" label on the store page. |
| Prominent disclosure | Our own dialog explaining the VPN, shown before Android's VPN prompt. |

---

## 15. Sources (read 2026-10-21; re-check before submitting)
- VpnService policy: https://support.google.com/googleplay/android-developer/answer/12564964
- Device and Network Abuse: https://support.google.com/googleplay/android-developer/answer/9888379
- Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
- Alternative billing programs: https://support.google.com/googleplay/android-developer/answer/17161464
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Foreground service declarations: https://support.google.com/googleplay/android-developer/answer/13392821
- Target API: https://developer.android.com/google/play/requirements/target-sdk
- Testing requirements for new personal accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- AdGuard Content Blocker on Play: https://play.google.com/store/apps/details?id=com.adguard.android.contentblocker
- AdGuard Content Blocker source: https://github.com/AdguardTeam/ContentBlocker
- Why AdGuard isn't on Play (AdGuard, unofficial for Google policy): https://adguard.com/en/blog/adguard-google-play-removal.html
- Play Billing Library 8 deadline (unofficial summary of Google's notice): https://www.revenuecat.com/blog/engineering/play-billing-8-migration/
- Play merchant accounts in Bangladesh (2017 news, unofficial): https://www.androidpolice.com/2017/11/07/developers-bangladesh-south-africa-can-now-sell-apps-play-store/
