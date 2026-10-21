# PrivacyGuard: why it would fail on Google Play, and how to fix it

> **Background document.** The current plan is [`PLAY_VERSION_PLAN.md`](PLAY_VERSION_PLAN.md).
> This file was written before the Play version was decided, and some facts here are out of date:
> the app is now v0.1 (versionCode 2), and `brand/play-store/` now has the 512 px icon and a feature graphic.

Written 2026-10-21 for `elwyndraker/testing-guard` (`com.pg.privacyguard`, v0).
Companion to `PLAY_STORE_PLAN.md` and `PLAY_VERSION_PLAN.md` (the step-by-step plan).

**Short answer:** the app as it is today would fail on Google Play, but the app itself isn't the
problem. The code (local DNS filter, VpnService, list merging, UI, signed builds) is solid. What fails
is **what it blocks (ads)** and **how it would charge money (SSLCommerz inside the app)**.
Change those two things and it becomes a normal Play app, and the full ad-blocking version can still
be sold outside Play.

---

## Part 1: Why it would fail

### Failure 1: It blocks ads, and Play forbids that for VPN apps
| Rule (official wording) | Where | What PrivacyGuard does |
|---|---|---|
| "Any manipulation of ads that can impact monetization will result in rejection." | [VpnService policy](https://support.google.com/googleplay/android-developer/answer/12564964) | Blocks ad domains for every app and browser on the phone. |
| "Apps that block or interfere with another app displaying ads." (listed as a violation) | [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379) | Same. |
| VPN apps must be a VPN, or be one of: parental control, **device security (anti-virus, firewall)**, network tools, browsers, usage tracking, carrier apps. | VpnService policy | Ad blocking is not on the list. |

- 7 of the 17 downloaded lists are ad-only, 6 are mixed (ads + trackers), only 4 are tracker-only.
  The built-in offline list (anti-AD) is an ad list.
- The status text, disclosure dialog, notification, privacy policy and README all say "ad".
- **"Only website ads" doesn't help:** DNS can't tell a website's ad from an app's ad (the same
  domains serve both), the browser is "another app", and websites' ad income is "monetization" too.

**Result:** rejection at review, or removal later with a strike on the developer account.

### Failure 2: Charging through SSLCommerz inside the Play app
| Rule | Where | Problem |
|---|---|---|
| Apps that charge for in-app features "must use Google Play's billing system" | [Payments policy](https://support.google.com/googleplay/android-developer/answer/9858738) | SSLCommerz unlock = not Play Billing. |
| Apps "may not lead users to a payment method other than Google Play's billing system" | Payments policy | A "pay on our website" link is also banned. |
| Alternative billing programs exist only for the UK, EEA and US | [Alternative billing](https://support.google.com/googleplay/android-developer/answer/17161464) | Bangladesh isn't covered. |

And what would be sold (unlocking ad lists) is the thing Failure 1 forbids, so paying makes it worse,
not better.

### Failure 3: Workarounds that still fail
| Idea | Why it fails |
|---|---|
| Hide the ad lists and download them after approval | The app reviewers test isn't the app users get. Treated as deceptive behaviour and can get the developer account banned. |
| Give users ad list links to paste in | You are still supplying ad blocking, just one step removed. |
| Ad lists as presets or one-tap options | Same as shipping them. |
| Rename "ads" to something vague but keep the same lists | Reviewers test what the app does, not only what it says. |

### Smaller problems (easy fixes)
- The store needs a 512×512 icon, a 1024×500 feature graphic and screenshots, and none exist yet.
- The privacy policy says "updated for version 1.3", but the app is v0.
- "PrivacyGuard" is a generic name and may already be used on Play.
- The anti-AD MIT copyright notice is only in the README, not in the app.
- One list (notracking) is no longer maintained.

---

## Part 2: Why the app is still worth publishing

- **Tracker blocking is a real product on Play.** DuckDuckGo's App Tracking Protection is a big
  example, and NetGuard and RethinkDNS are on Play too.
- **Malware and phishing blocking fits "device security / firewall"**, which is an allowed category.
- **Outside Play, ad blocking is fine.** AdAway and full Blokada are distributed off Play and are popular.
- **Everything hard is already built:** the VPN DNS filter, merging lists, the UI, signed release builds,
  a live privacy policy.

---

## Part 3: How to fix it

### Fix 1: Two builds from one codebase
| | Play build | Full build |
|---|---|---|
| Package | `com.pg.privacyguard` | `com.pg.privacyguard.full` |
| Blocks | Trackers, telemetry, malware, phishing, scams | Everything (current 17 lists) |
| Lists | EasyPrivacy, AdGuard Mobile Spyware, 2o7.net, NoTrack trackers, HaGeZi native trackers + threat feeds, URLhaus, Phishing Army | Current lists (drop notracking) |
| Built-in list | A tracker/malware list | anti-AD |
| Wording | "Blocks trackers, malware and phishing" (no "ad" anywhere) | Can say "ads" |
| Updates | Play in-app update banner | Its own update check |
| Distributed via | Google Play | Your website / GitHub / F-Droid |

Technically this is Gradle product flavors: the source lists, strings and built-in asset move into
`app/src/play/` and `app/src/full/`. The VPN and UI code stays shared.

### Fix 2: Earn money the allowed way in each build
| | Play build | Full build |
|---|---|---|
| Payment | **Google Play Billing** (one-time "Pro"; Google keeps 15%) | **SSLCommerz** on your website |
| What's sold | Pro features: extra security lists (scam, cryptojacking, newly registered domains), family/adult filter, own block/allow rules, history and stats, Quick Settings tile, schedule | Full version licence key / supporter payment |
| Needs | A Play merchant account (available in Bangladesh, paid in USD) | A trade licence, TIN and NID for a live merchant account (probably; confirm with SSLCommerz), a small server to confirm payments and check licence keys, an updated privacy policy |
| Must not | Unlock ad lists; mention the full version or SSLCommerz | — |

Charge for **your app and its features**, not for the free public lists. Check each list's licence
for commercial-use limits before selling a build that includes it.

### Fix 3 (optional): A neutral "custom blocklist" feature in the Play build
Users can add any blocklist URL or file. To keep the risk low:
- Neutral wording ("your own filter list"), with no examples, placeholders, presets or links to ad lists.
- Don't show it in the screenshots or the VPN declaration video.
- The Play app never links to pages that explain how to block ads.
- **Risk is still low to medium.** Google has no written rule on this; similar apps exist on Play but
  have sometimes been removed or had updates held.

### Fix 4: The small items
New store assets, an updated privacy policy, a name check, an in-app licences screen, drop notracking.

---

## Part 4: Order of work
1. Decide: package names, store name, Pro features and price.
2. Add the `play`/`full` flavors, Play-safe lists, Play wording (no "ad").
3. Add Play Billing for Pro in `play`; leave SSLCommerz for the website and `full` build later.
4. Update the privacy policy, README, store assets and listing text.
5. Play Console: internal test → closed test (12+ testers for 14 days) → production.
6. Later: website + SSLCommerz sandbox → live merchant account → sell the full build.

## Bottom line
| Version | On Play? |
|---|---|
| The app as it is today | ✗ Fails (ad blocking + SSLCommerz) |
| Play build: tracker/malware blocker + Play Billing Pro | ✓ Fits the rules |
| Full build with ads + SSLCommerz, sold on your own site | ✓ Allowed, because it isn't on Play |

Policy pages read 2026-10-21. Google changes them often, so re-check before submitting.
