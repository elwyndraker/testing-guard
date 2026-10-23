#!/usr/bin/env python3
"""Rebuild app/src/main/assets/standard.txt, the built-in copy of the free list.

The app ships it so blocking works on first launch, even offline, and then downloads a
fresh copy. Run this before a release so the built-in copy isn't months old:
    python3 scripts/build_builtin_list.py

It reads the sources from Filter.kt (Filter.STANDARD) and the ads exclusion list from
Blocklist.kt, so they never drift apart. The parsing follows Blocklist.parse in Kotlin:
keep the two in step.
"""
import os
import re
import sys
import urllib.request

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DATA = os.path.join(ROOT, "app", "src", "main", "java", "com", "pg", "privacyguard", "data")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "standard.txt")

SINKS = {"0.0.0.0", "127.0.0.1", "::", "::1", "0"}
LOCAL = {"localhost", "localhost.localdomain", "local", "broadcasthost"}
LABEL = re.compile(r"[a-z0-9_-]+")


def sources():
    text = open(os.path.join(DATA, "Filter.kt"), encoding="utf-8").read()
    hagezi = re.search(r'HAGEZI = "([^"]+)"', text).group(1)
    block = re.search(r"STANDARD\((.*?)\n    \),", text, re.S).group(1)
    urls = [u.replace("$HAGEZI", hagezi) for u in re.findall(r'"(https://[^"]+|\$HAGEZI[^"]+)"', block)]
    excl = re.search(r'ADS_EXCLUSION_SOURCE = "([^"]+)"',
                     open(os.path.join(DATA, "Blocklist.kt"), encoding="utf-8").read()).group(1)
    return urls, excl


def is_domain(name):
    if not name or len(name) > 253:
        return False
    labels = name.split(".")
    if len(labels) < 2:
        return False
    for label in labels:
        if not LABEL.fullmatch(label) or label[0] == "-" or label[-1] == "-":
            return False
    return re.search(r"[a-z_-]", name) is not None


def under(name, names):
    parts = name.split(".")
    return any(".".join(parts[i:]) in names for i in range(len(parts)))


def parse(lines, excluded=frozenset()):
    out = set()
    adblock = False

    def add(candidate):
        name = candidate.lower()
        if name not in LOCAL and is_domain(name) and not under(name, excluded):
            out.add(name)

    for raw in lines:
        line = raw.lstrip("﻿").strip()
        if line.startswith("[Adblock") or line.startswith("||"):
            adblock = True
        if not line or line[0] in "![":
            continue
        if any(m in line for m in ("##", "#@#", "#?#", "#$#")):
            continue
        parts = line.split("#")[0].split()
        if len(parts) >= 2:
            if parts[0] in SINKS:
                for part in parts[1:]:
                    add(part)
        elif parts:
            rule = parts[0]
            if rule.startswith("||"):
                if rule.endswith("^"):
                    add(rule[2:-1])
            elif adblock:
                pass
            elif rule.startswith("*."):
                add(rule[2:])
            else:
                add(rule)
    return out


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "PrivacyGuard"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8", errors="replace").splitlines()


def main():
    urls, excl_url = sources()
    excluded = frozenset(parse(fetch(excl_url)))
    if len(excluded) < 10_000:
        sys.exit(f"ads exclusion list looks broken: {len(excluded)} domains")
    domains = set()
    for url in urls:
        found = parse(fetch(url), excluded)
        print(f"{len(found):>8}  {url}")
        domains |= found
    if len(domains) < 10_000:
        sys.exit(f"list looks broken: {len(domains)} domains")
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("# PrivacyGuard built-in list: trackers and security, ads domains removed.\n")
        f.write("# Built by scripts/build_builtin_list.py from the sources in Filter.STANDARD.\n")
        f.write("# Credits and licences: see the app's Licences screen and the README.\n")
        for name in sorted(domains):
            f.write(name + "\n")
    print(f"{len(domains)} domains ({len(excluded)} ads domains excluded) -> {OUT}")


if __name__ == "__main__":
    main()
