# PrivacyGuard on Google Play

This file is for everyone on the team. It explains what PrivacyGuard is on Google Play, which
Play rules it follows and how, and what is left before publishing. The texts to paste into the
Play Console are in [listing.md](listing.md).

Updated 2026-10-23 (version 0.2, versionCode 3, package `com.pg.privacyguard`).

## What PrivacyGuard is

An on-device **content blocker for privacy and security**. It filters DNS lookups on the phone
through a local VPN and blocks known threats in every app and browser:

| Free, always on | Pro (one-time purchase through Google Play Billing) |
|---|---|
| Trackers and analytics | Adult content filter |
| Phone maker telemetry (Xiaomi, Samsung, Huawei, OPPO/realme, vivo) | Gambling filter |
| Malware, phishing and scam sites, fake shops | Filter bypass blocking (public encrypted DNS, VPN and proxy services) |
| | Newly registered domains, cryptojacking, stalkerware |
| | PIN lock, My rules (block or allow single sites), 7-day history on the phone |

**Rule:** the free app is useful on its own. Tracker, malware and phishing blocking are never
behind the paywall.

## Decisions

| Decision | Choice |
|---|---|
| Where the app is published | **Google Play only** (one app, one build). GitHub has only test builds. |
| Package name | `com.pg.privacyguard` (permanent after the first Play upload) |
| What it blocks | Privacy and security threats only (the table above) |
| How we earn money | A one-time **Pro** upgrade, product `pro_unlock`, through **Google Play Billing** |
| Other payment providers (for example SSLCommerz) | **Not used**: Play doesn't allow them for digital features in Bangladesh |
| Parental control | Yes: Pro has a PIN lock, so the VPN form declares Parental control as well as firewall |
| App licence | Open source, GPL-3.0 (`LICENSE`) |
| Public DNS fallback (1.1.1.1 / 8.8.8.8) | Opt-in setting, off by default |
| Pro price | **Not decided yet** (about USD 1.99 was suggested). Google keeps 15%. |
| Developer account | **Not decided yet**: personal (12 testers for 14 days) or organisation (D-U-N-S number) |
| Store name | "PrivacyGuard"; check for conflicts on Play before the first upload |

## Play rules and how the app follows them

### VpnService ([policy](https://support.google.com/googleplay/android-developer/answer/12564964))
| Rule | What the app does |
|---|---|
| VpnService only for allowed categories | Declared as **Device security (firewall)** and **Parental control** |
| Must not interfere with other apps' monetization | Only privacy and security lists. Every list is cleaned of the domains on EasyList when it is downloaded, so those are never blocked (`Blocklist.EXCLUSION_SOURCE`). A unit test (`BlocklistFiltersTest.onlyApprovedSources`) fails if an unreviewed list is added. |
| No redirecting traffic for monetization | DNS only; allowed lookups are forwarded unchanged |
| Document the VPN in the store listing | A fixed paragraph in the description ([listing.md](listing.md)) |
| Encrypt data to the tunnel endpoint | The tunnel never leaves the phone (no remote endpoint); the declaration says so |
| Prominent disclosure and consent | Dialog before Android's VPN prompt; "Not now" doesn't start the VPN |
| VPN declaration with videos | Text and video plan in [listing.md](listing.md) |

### Device and Network Abuse ([policy](https://support.google.com/googleplay/android-developer/answer/9888379))
- Doesn't interfere with other apps (see above).
- Updates only through Google Play (in-app update banner). Blocklists are data, not code, so
  downloading them is allowed.

### Deceptive behaviour
- What reviewers test is what users get: no remote switch changes what the app blocks.
- The listing, screenshots and videos show the real app.
- Describe only what the app blocks. No rankings ("best", "#1"), no "100% protection", no
  "blocks everything", no big domain counts.

### Payments ([policy](https://support.google.com/googleplay/android-developer/answer/9858738))
| Rule | What the app does |
|---|---|
| Digital features must use Google Play Billing | Pro uses Play Billing 9.1.0 (`billing/Billing.kt`) |
| No links or buttons to other payment methods | None in the app, listing or screenshots |
| Other billing programs | Only for the UK, the EEA and the US ([official](https://support.google.com/googleplay/android-developer/answer/17161464)); Bangladesh isn't covered |
| Acknowledge purchases within 3 days | Done right after a purchase, retried on the next check |
| Pending payments | "Payment pending" until Play confirms |
| Refunds | Purchases are checked each time the app comes to the front; a refund removes Pro |
| No pressure selling | The Pro screen shows Play's local price, no countdowns or fake discounts |

### Technical requirements
| Rule | State |
|---|---|
| Target API 36 for new apps and updates ([official](https://developer.android.com/google/play/requirements/target-sdk)) | ✓ targetSdk 36. Expect API 37 to be required around Aug 2027. |
| Upload an AAB with Play App Signing | ✓ the release workflow builds a signed AAB; the upload key is `~/PrivacyGuard-signing/privacyguard-upload.jks` |
| versionCode goes up on every upload | Comment in `app/build.gradle.kts` |
| 16 KB page support (native libraries) | ✓ no `.so` files |
| Foreground service type ([official](https://support.google.com/googleplay/android-developer/answer/13392821)) | `systemExempted`. If the Play Console asks: "keeps the user-enabled local VPN filter running". |
| Notification permission (Android 13+) | ✓ asked at runtime |

### Data safety ([official](https://support.google.com/googleplay/android-developer/answer/10787469))
Google counts data as "collected" only if it leaves the phone. The blocked list, history, rules,
PIN hash, settings and Pro status stay on the phone; Play Billing is handled by Google; lookups go
to the network's own DNS server, as without the app; the public DNS fallback is off by default.
Expected label: **"No data collected · No data shared"**. If a server is ever added to check
purchases, or the fallback is turned on by default, this answer must change.

### App content forms
Answers in [listing.md](listing.md). Target audience 18+, so the Families policy doesn't apply.
Names of blocked adult sites are never shown in the app.

### Licences
The app is GPL-3.0, which allows bundling the GPL-3.0 lists in `assets/standard.txt` and still
selling Pro. Each list's licence is shown in the app (**Filters → Licences and list credits**) and
in the README.

### Quality (pre-launch report and Android vitals)
- Google's robot taps through the app before release: it must not crash when the VPN or
  notification prompt is cancelled.
- Large Pro lists (newly registered domains, about 800,000 domains) use memory; `largeHeap` is on.
  Test an update on a 2 GB RAM phone.

## Developer account ([testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465))
- USD 25 one-time fee and identity verification.
- A personal account created after 13 Nov 2023 must run a **closed test with at least 12 testers
  opted in for the last 14 days in a row** before production. An organisation account needs a
  D-U-N-S number but skips this.
- A **payments profile** (merchant account) is needed to sell Pro. Bangladesh has had Play
  merchant accounts since 2017, paid out in USD to a bank account.

## Release steps on Play

1. **Developer account:** pay, verify, set up the payments profile.
2. **Create the app** and fill in the store listing and every form from [listing.md](listing.md).
3. **Play App Signing:** Google keeps the app signing key; `privacyguard-upload.jks` is the upload key.
   Keep it backed up in two places.
4. **Internal testing** (up to 100 testers, available within minutes): upload the AAB from the
   release workflow, create `pro_unlock`, add testers under **Settings → License testing**, and test
   buying Pro with Play's test card. Test the in-app update banner by uploading a second build with a
   higher versionCode.
5. **Device checklist** in the README on real phones (a Xiaomi, a Pixel and one more brand).
6. **Closed testing:** at least 12 testers for 14 days in a row (personal account). Ship a small
   update each week and ask for feedback; Google asks about it when you apply.
7. **Apply for production**, then a staged rollout: 20% → watch crashes and ANRs in Android vitals → 100%.

Testers can check the Pro features before step 4 with the **beta build** (`./gradlew assembleBeta`,
Pro always on). Never upload the beta build to Play.

## If Play rejects the app
1. Read the email: it names the **policy** and often the **screen or text** at fault.
2. Fix exactly that, raise versionCode, resubmit. Don't argue with a clear finding.
3. Appeal (Policy status page → Appeal) only if the reviewer misread the app. Attach the VPN
   declaration text from [listing.md](listing.md) and a short video showing that only trackers and
   security threats are blocked.
4. Never resubmit the same build unchanged; repeated rejections can lead to account strikes.

## Glossary
| Term | Meaning |
|---|---|
| DNS | The phone asking "what's the address of example.com?" before connecting. PrivacyGuard answers "doesn't exist" for blocked names. |
| VpnService | Android's API for VPN apps. PrivacyGuard uses it only to see DNS lookups on the phone; there's no VPN server. |
| AAB | Android App Bundle, the file you upload to Play. Play builds the APKs from it. |
| Play App Signing | Google keeps the key that signs the app for users; you sign uploads with your own upload key. |
| versionCode | A number that must go up with every upload. versionName (e.g. 0.2) is what users see. |
| Non-consumable product | Something bought once and kept forever, like Pro. |
| Licence tester | An account that can buy in your app without being charged, for testing. |
| Internal / closed / production testing | Internal = quick private tests; closed = invited testers (14 days for new personal accounts); production = public. |
| Data safety | The form behind the "data collected / shared" label on the store page. |
| Prominent disclosure | Our own dialog explaining the VPN, shown before Android's VPN prompt. |

## Sources (read 2026-10-21; re-check before submitting)
- VpnService policy: https://support.google.com/googleplay/android-developer/answer/12564964
- Device and Network Abuse: https://support.google.com/googleplay/android-developer/answer/9888379
- Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
- Alternative billing programs: https://support.google.com/googleplay/android-developer/answer/17161464
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Foreground service declarations: https://support.google.com/googleplay/android-developer/answer/13392821
- Target API: https://developer.android.com/google/play/requirements/target-sdk
- Testing requirements for new personal accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Testing Play Billing: https://developer.android.com/google/play/billing/test
