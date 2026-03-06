# Health Dashboard

[![Platform](https://img.shields.io/badge/platform-android-green)](https://github.com/manumnoha-sys/health-dashboard)
[![Server](https://img.shields.io/badge/server-FastAPI-blue)](https://github.com/manumnoha-sys/health-dashboard)

Personal health tracking platform that aggregates data from **multiple devices and services** into a single cloud backend with a unified API.

---

## Connected Devices & Services

| Source | Data | Sync |
|---|---|---|
| **Galaxy Watch** (via Health Connect) | Heart rate, SpO2, steps, calories, exercise sessions | Every 30 min |
| **Fitbit** (OAuth2 + PKCE) | Heart rate (1-min), steps (15-min) | Every 30 min |
| **Google Fit** (OAuth2) | Heart rate, steps, calories | Every 30 min |
| **CGM / Nightscout** | Blood glucose readings | Every 5 min |
| **CGM / Dexcom Share** | Blood glucose readings | Every 5 min |
| **Wodify** (CrossFit) | Workout performance records | Every 30 min |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        Android Phone                             │
│                                                                  │
│  ┌─────────────────┐  ┌────────────┐  ┌────────────────────┐   │
│  │  Health Connect  │  │   Fitbit   │  │    Google Fit      │   │
│  │  (Galaxy Watch   │  │  REST API  │  │    REST API        │   │
│  │   + Google Fit)  │  │  OAuth2    │  │    OAuth2          │   │
│  └────────┬─────────┘  └─────┬──────┘  └────────┬───────────┘  │
│           │                  │                   │              │
│  ┌────────┴──────────────────┴───────────────────┴──────────┐  │
│  │               HealthBridge Android App                    │  │
│  │  · SamsungHealthSyncWorker   (Health Connect)             │  │
│  │  · FitbitSyncWorker          (Fitbit API)                 │  │
│  │  · GoogleFitSyncWorker       (Google Fit API)             │  │
│  │  · CgmSyncWorker             (Nightscout / Dexcom)        │  │
│  │  · WodifySyncWorker          (Wodify API)                 │  │
│  └────────────────────────────┬──────────────────────────────┘  │
└───────────────────────────────┼──────────────────────────────────┘
                                │ HTTPS  X-Api-Key
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                    FastAPI Server                                 │
│                                                                  │
│   POST /ingest/watch     — HR, SpO2, steps, calories             │
│   POST /ingest/glucose   — CGM readings                          │
│   POST /ingest/workout   — workout sessions                      │
│   GET  /health/snapshot  — latest readings from all sources      │
│   GET  /health/summary/daily                                     │
│                                                                  │
│   ┌────────────────────────────────────────────────────────────┐ │
│   │                    PostgreSQL 15                           │ │
│   │  · watch_readings   (time-series, keyed by device_id)      │ │
│   │  · glucose_readings (deduped by external_id)               │ │
│   │  · workouts         (upserted from Wodify)                 │ │
│   └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## Repository Structure

```
.
├── android-app/    # Android companion app — all data sources → server
├── watch-app/      # Wear OS app — direct sensor bridge (Galaxy Watch)
├── server/         # FastAPI backend — ingests and queries all health data
├── infra/          # Docker dev environments (arm64 + x86)
└── docs/           # Architecture and API reference
```

---

## Quick Start

### 1. Server

```bash
cd server
cp .env.example .env          # set API_KEY and POSTGRES_PASSWORD
docker compose up -d
# API docs at http://localhost:8000/docs
```

### 2. Android App

Configure `android-app/local.properties`:
```
sdk.dir=/path/to/android-sdk
serverUrl=http://YOUR_SERVER:8000
apiKey=YOUR_API_KEY
fitbitClientId=YOUR_FITBIT_CLIENT_ID
googleFitClientId=YOUR_GOOGLE_CLIENT_ID
googleFitClientSecret=YOUR_GOOGLE_CLIENT_SECRET
```

Build and install:
```bash
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

After installing, open the app to connect Fitbit and Google Fit via OAuth.

---

## Device Setup

### Galaxy Watch (Samsung Health)
- Install **Health Connect** on phone
- Open Samsung Health → Settings → Connected services → Health Connect → enable sync
- Open HealthBridge app → grant Health Connect permissions

### Fitbit
- Register app at [dev.fitbit.com](https://dev.fitbit.com/apps/new) (type: Personal)
- Set redirect URI: `com.manumnoha.healthbridge://fitbit-callback`
- Copy Client ID → `fitbitClientId` in `local.properties`
- Open HealthBridge app → tap **Connect Fitbit**

### Google Fit
- Enable Fitness API in [Google Cloud Console](https://console.cloud.google.com)
- Create OAuth 2.0 credentials (Web application type)
- Add redirect URI: `com.manumnoha.healthbridge://google-fit-callback`
- Copy Client ID + Secret → `local.properties`
- Open HealthBridge app → tap **Connect Google Fit**

---

## Authentication

All server endpoints require `X-Api-Key` header:
```bash
openssl rand -hex 20   # generate a key
```

---

## Docs

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — API reference, data models, data flow
- [`infra/SETUP.md`](infra/SETUP.md) — Docker dev environment setup
