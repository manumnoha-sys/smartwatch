# Health Dashboard

[![Docker Image](https://img.shields.io/badge/ghcr.io-watch--dev-blue?logo=docker)](https://github.com/manumnoha-sys/health-dashboard/pkgs/container/watch-dev)
[![Platform](https://img.shields.io/badge/platform-linux%2Farm64-lightgrey)](https://github.com/manumnoha-sys/health-dashboard)

Personal health tracking platform that aggregates **Galaxy Watch sensors**, **CGM glucose readings**, and **CrossFit workouts** into a single cloud backend.

---

## Architecture

```
┌──────────────────────┐        ┌──────────────────────┐
│   Galaxy Watch       │        │   Android Phone       │
│   (Wear OS)          │        │                       │
│                      │        │  ┌─────────────────┐  │
│  Health Services:    │        │  │ Glucose Monitor │  │
│  · Heart rate        │        │  │ (Nightscout /   │  │
│  · SpO2              │        │  │  Dexcom Share)  │  │
│  · Steps             │        │  └────────┬────────┘  │
│  · Calories          │        │           │            │
│  · Accelerometer     │        │  ┌────────┴────────┐  │
│                      │        │  │ Wodify CrossFit │  │
└──────────┬───────────┘        │  │  (REST API)     │  │
           │  HTTPS             │  └────────┬────────┘  │
           │  POST /ingest/watch└───────────┼────────────┘
           │                               │  HTTPS
           ▼                               ▼
┌──────────────────────────────────────────────────────┐
│               Cloud Server  (FastAPI)                │
│                                                      │
│   POST /ingest/watch                                 │
│   POST /ingest/glucose                               │
│   POST /ingest/workout                               │
│   GET  /health/snapshot                              │
│   GET  /health/summary/daily                         │
│                                                      │
│   ┌────────────────────────────────────────────────┐ │
│   │          PostgreSQL 15                         │ │
│   │  · watch_readings  (time-series)               │ │
│   │  · glucose_readings (deduped by external_id)   │ │
│   │  · workouts         (upserted from Wodify)     │ │
│   └────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

---

## Repository Structure

```
.
├── infra/          # Docker dev environment (arm64, IntelliJ IDEA)
├── watch-app/      # Wear OS app — reads sensors, POSTs to server
├── android-app/    # Android phone app — bridges CGM + Wodify to server
└── server/         # FastAPI backend — ingests and queries all health data
```

---

## Quick Start

### 1. Run the server

```bash
cd server
cp .env.example .env          # fill in API_KEY and POSTGRES_PASSWORD
docker compose up -d
```

API docs at `http://localhost:8000/docs`.

### 2. Build the apps (inside the dev container)

```bash
bash infra/run.sh && bash infra/into.sh

# Watch app
cd ~/projects/watch-app && ./gradlew assembleDebug

# Android companion app
cd ~/projects/android-app && ./gradlew assembleDebug
```

---

## Authentication

All endpoints require `X-Api-Key` header. Generate a key once:

```bash
openssl rand -hex 20
```

Set it in `server/.env` and `local.properties` in both Android projects.

---

## Docs

- [`infra/SETUP.md`](infra/SETUP.md) — arm64 Docker dev environment and workarounds
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — API reference, data models, data flow
