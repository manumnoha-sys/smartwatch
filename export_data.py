#!/usr/bin/env python3
"""Fetch health data from the server and write static JSON files for GitHub Pages."""

import json
import os
import sys
import urllib.request
import urllib.error
from datetime import datetime, timedelta, timezone

API_URL = os.environ.get("HEALTH_API_URL", "http://192.168.1.26:8000")
API_KEY = os.environ.get("HEALTH_API_KEY", "ae43d94ce0f674df640831189d3462c0a5d51b87")
OUT_DIR = os.path.join(os.path.dirname(__file__), "data")


def fetch(path):
    url = f"{API_URL}{path}"
    req = urllib.request.Request(url, headers={"X-API-Key": API_KEY})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read())
    except urllib.error.URLError as e:
        print(f"  WARN: {url} → {e}", file=sys.stderr)
        return None


def write(name, data):
    if data is None:
        print(f"  SKIP {name} (no data)")
        return
    path = os.path.join(OUT_DIR, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, default=str)
    print(f"  OK   {name}")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    now = datetime.now(timezone.utc)

    print("Fetching snapshot...")
    write("snapshot.json", fetch("/health/snapshot?window_minutes=1440"))

    print("Fetching sleep (14 days)...")
    write("sleep.json", fetch("/health/sleep?days=14"))

    print("Fetching daily summaries (last 7 days)...")
    daily = []
    for i in range(6, -1, -1):
        d = (now - timedelta(days=i)).strftime("%Y-%m-%d")
        result = fetch(f"/health/summary/daily?date={d}&tz=UTC")
        if result:
            daily.append(result)
    write("daily-7.json", daily)

    print("Writing meta...")
    write("meta.json", {"updated_at": now.strftime("%Y-%m-%dT%H:%M:%SZ")})

    print("Done.")


if __name__ == "__main__":
    main()
