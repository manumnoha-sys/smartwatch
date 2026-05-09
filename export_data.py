#!/usr/bin/env python3
"""Fetch health data from the server and write static JSON files for GitHub Pages."""

import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import urllib.error
from datetime import datetime, timedelta, timezone

REPO_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")
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


def _run(cmd, cwd=None):
    result = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"  WARN git: {result.stderr.strip()}", file=sys.stderr)
    return result.returncode == 0


def _push_to_gh_pages():
    if not GITHUB_TOKEN:
        print("  SKIP push (GITHUB_TOKEN not set)")
        return

    dashboard_dir = os.path.dirname(os.path.abspath(__file__))
    worktree = tempfile.mkdtemp(prefix="gh-pages-")
    try:
        remote = f"https://{GITHUB_TOKEN}@github.com/manumnoha-sys/health-dashboard.git"
        _run(["git", "remote", "set-url", "origin", remote], cwd=REPO_DIR)
        _run(["git", "fetch", "origin", "gh-pages"], cwd=REPO_DIR)

        # Remove stale worktree if exists
        subprocess.run(["git", "worktree", "remove", worktree, "--force"],
                       cwd=REPO_DIR, capture_output=True)
        _run(["git", "worktree", "add", "--track", "-b", "gh-pages-deploy",
              worktree, "origin/gh-pages"], cwd=REPO_DIR)

        # Copy index.html + data/
        shutil.copy(os.path.join(dashboard_dir, "index.html"), worktree)
        data_src = os.path.join(dashboard_dir, "data")
        data_dst = os.path.join(worktree, "data")
        if os.path.exists(data_dst):
            shutil.rmtree(data_dst)
        shutil.copytree(data_src, data_dst)

        _run(["git", "config", "user.email", "bot@health-dashboard"], cwd=worktree)
        _run(["git", "config", "user.name", "Health Dashboard Bot"], cwd=worktree)
        _run(["git", "add", "index.html", "data/"], cwd=worktree)

        ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        committed = _run(["git", "commit", "-m", f"data: export {ts}"], cwd=worktree)
        if committed:
            if _run(["git", "push", "origin", "HEAD:gh-pages"], cwd=worktree):
                print("  OK   pushed to gh-pages")
            else:
                print("  WARN push failed", file=sys.stderr)
        else:
            print("  SKIP nothing changed")
    finally:
        subprocess.run(["git", "worktree", "remove", worktree, "--force"],
                       cwd=REPO_DIR, capture_output=True)
        subprocess.run(["git", "branch", "-D", "gh-pages-deploy"],
                       cwd=REPO_DIR, capture_output=True)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    now = datetime.now(timezone.utc)

    print("Fetching snapshot...")
    write("snapshot.json", fetch("/health/snapshot?window_minutes=1440"))

    print("Fetching sleep (14 days)...")
    write("sleep.json", fetch("/health/sleep?days=14"))

    print("Fetching daily summaries (last 7 days for charts)...")
    daily = []
    for i in range(6, -1, -1):
        d = (now - timedelta(days=i)).strftime("%Y-%m-%d")
        result = fetch(f"/health/summary/daily?date={d}&tz=UTC")
        if result:
            daily.append(result)
    write("daily-7.json", daily)

    print("Fetching individual daily files (last 30 days for date picker)...")
    for i in range(29, -1, -1):
        d = (now - timedelta(days=i)).strftime("%Y-%m-%d")
        result = fetch(f"/health/summary/daily?date={d}&tz=UTC")
        write(f"daily/{d}.json", result)

    print("Fetching Tesla latest...")
    write("tesla/latest.json", fetch("/health/tesla/latest"))

    print("Fetching Tesla history (24h)...")
    write("tesla/history-24h.json", fetch("/health/tesla/history?hours=24"))

    print("Writing meta...")
    write("meta.json", {"updated_at": now.strftime("%Y-%m-%dT%H:%M:%SZ")})

    print("Done.")
    _push_to_gh_pages()


if __name__ == "__main__":
    main()
