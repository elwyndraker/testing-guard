# PrivacyGuard: Play Console texts and answers

Copy these into the Play Console when the app is published. The Play rules behind each answer
are in [PLAY_RULES.md](PLAY_RULES.md).

PrivacyGuard is a content blocker for privacy and security. Describe only what it blocks:
trackers, phone telemetry, malware, phishing, scams and, with Pro, adult content, gambling,
filter bypass, newly registered domains, cryptojacking and stalkerware. No rankings ("best",
"#1"), no "100% protection", no "blocks everything", no big domain counts.

## Store listing

**App name (≤30):** `PrivacyGuard: Tracker Blocker`

**Short description (≤80):** `Block trackers, malware and phishing on your whole phone. No account needed.`

**Full description:**

```
PrivacyGuard blocks known trackers, malware and phishing sites in every app and browser on your phone.

HOW IT WORKS
PrivacyGuard checks the website names your phone looks up (DNS). If a name is on a blocklist of known trackers or harmful sites, the lookup is stopped on your phone. Everything else works as normal.

WHAT IT BLOCKS
• Trackers and analytics that follow you across apps and sites
• Telemetry sent by phone makers' system apps
• Malware, phishing and scam sites

PRIVATE BY DESIGN
• No account and no sign-up
• No data collected: your blocked list stays on your phone
• No VPN server: nothing is routed through us
• Open source (GPL-3.0)

PRIVACYGUARD PRO (ONE-TIME PURCHASE)
• Adult content and gambling filters
• PIN lock for protection and filter settings
• Extra security lists: filter bypass, newly registered domains, cryptojacking, stalkerware
• Your own block and allow rules
• Blocked history for the last 7 days, kept on your phone

ABOUT THE VPN PERMISSION
PrivacyGuard uses Android's VpnService to run a local filter on your phone. Only DNS lookups go through it; no other traffic does. It does not connect to a VPN server, change your IP address or encrypt your traffic.

GOOD TO KNOW
• If Private DNS is set to a specific provider in Android settings, set it to Off or Automatic so the filter can work.
• Blocklists are public projects; credits and licences are listed in the app.
```

**Category:** Tools · **Contact email:** (your support address) · **Privacy policy:**
https://elwyndraker.github.io/testing-guard/

## Graphics

| Asset | File |
| --- | --- |
| App icon 512×512 | `brand/play-store/icon-512.png` |
| Feature graphic 1024×500 | `brand/play-store/feature-graphic-1024x500.png` (regenerate with `python3 brand/generate.py`) |
| Phone screenshots (2–8) | `play-store/screenshots/` |

Screenshots to take (`adb exec-out screencap -p > N.png`), with only tracker or security domains, no adult names
and no made-up numbers on screen:

1. Home, protection on
2. Blocked screen with tracker domains
3. Filters screen (list size, last update)
4. Disclosure dialog
5. Pro screen
6. Optional: Quick Settings tile

## In-app product

| Field | Value |
| --- | --- |
| Product ID | `pro_unlock` (must match `Pro.PRODUCT_ID`) |
| Type | One-time product, non-consumable |
| Name | PrivacyGuard Pro |
| Description | Adult and gambling filters, PIN lock, extra security lists, your own rules and blocked history. |
| Price | **Not decided yet** |

## App content forms

| Form | Answer |
| --- | --- |
| Ads (does the app show ads?) | No |
| App access | All functionality is available without special access. Note for reviewers: "Pro features are a one-time in-app purchase." |
| Content rating | Utility / tools. Names of blocked adult sites are never shown ("Adult site blocked"). |
| Target audience | 18 and over |
| Government, financial, health, news | Not applicable |

### Data safety

Expected label: **No data collected · No data shared.**

| Question | Answer | Why |
| --- | --- | --- |
| Does the app collect or share any of the required user data types? | **No** | Blocked list, history, rules, PIN hash, settings and Pro status stay on the phone. Lookups go to the network's own DNS server, as without the app. The public DNS fallback is off by default and turned on by the user. |
| Is all user data encrypted in transit? | Not applicable (no data collected) | |
| Can users request that data is deleted? | Not applicable (no data collected); clearing app storage deletes everything | |

If a server is ever added to check purchases, or the public DNS fallback is turned on by
default, this answer must change.

### VPN declaration

- **Is providing a VPN the core functionality?** No.
- **Permitted functionality:** Device security (firewall) and Parental control.
- **Description:**

  ```
  PrivacyGuard is an on-device DNS firewall and content blocker for privacy and security. It uses VpnService to create a local-only tunnel that receives only DNS queries sent to one internal address. Queries for known tracker, phone telemetry, malware, phishing and scam domains (and, if the user turns them on, adult, gambling, filter bypass, cryptojacking or stalkerware domains) are answered on the device; all other queries are forwarded unchanged to the device's DNS server. No other traffic enters the tunnel, there is no remote VPN server, and the app does not block, change or redirect the monetization of other apps. Pro users can set a PIN so that protection and filters can't be changed without it, for parents setting up a child's phone.
  ```

- **Data collected or shared through VpnService:** None.
- **Redirects or manipulates traffic for monetization?** No.
- **Videos:** (1) open the app → disclosure → Android VPN prompt → browse → Blocked screen
  showing tracker domains, 90 s or less. (2) The disclosure dialog with Continue and Not now.
  Record with `adb shell screenrecord`.

### Foreground service

Only if Play asks: the VPN service uses `systemExempted`, which VPN apps may use; it shows the
"protecting you" notification while protection is on.
