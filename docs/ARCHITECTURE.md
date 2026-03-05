# Architecture

## Components

| Component | Directory | Language | Runs on |
|-----------|-----------|----------|---------|
| Cloud server | `server/` | Python / FastAPI | Cloud VM / Docker |
| Watch app | `watch-app/` | Kotlin / Wear OS | Samsung Galaxy Watch |
| Phone companion | `android-app/` | Kotlin / Android | Android phone |

---

## Data Flow

### Watch → Server (every 5 minutes)

```
SensorCollectorService (foreground)
  │  TYPE_HEART_RATE / Health Services API → lastHr
  │  TYPE_STEP_COUNTER                     → lastSteps
  │  TYPE_ACCELEROMETER                    → lastAccel
  │  every 10s: insert SensorReading into Room DB
  │
SyncWorker (WorkManager, PeriodicWork, NETWORK_CONNECTED)
  │  SELECT up to 500 unsynced rows from Room
  │  POST /ingest/watch  { device_id, readings[] }
  │  On 200: markSynced, delete rows older than 24h
  └─ Retry: exponential backoff, max 3 attempts
```

### Phone → Server

```
BridgeService (foreground, keeps process alive)
  │
  ├─ CgmSyncWorker  (every 5 min)
  │    Primary:  Nightscout REST API → GET /api/v1/entries.json
  │    Fallback: Dexcom Share API    → POST .../LoginPublisherAccountByName
  │                                    GET .../ReadPublisherLatestGlucoseValues
  │    POST /ingest/glucose  { readings[] }
  │
  └─ WodifySyncWorker  (every 30 min)
       Wodify REST API → GET /v2/performance_records?date_from=...&date_to=...
       POST /ingest/workout  { workouts[] }
```

---

## API Reference

Base URL: `https://your-server.example.com`
Auth: `X-Api-Key: <key>` header on all endpoints.

### Ingest

#### `POST /ingest/watch`

```json
{
  "device_id": "galaxy-watch-1",
  "readings": [
    {
      "recorded_at": "2025-08-01T14:30:00Z",
      "heart_rate_bpm": 72.0,
      "heart_rate_accuracy": 3,
      "spo2_percent": 98.0,
      "steps_total": 4521,
      "calories_kcal": 312.5,
      "accel_x": 0.1, "accel_y": -9.8, "accel_z": 0.3
    }
  ]
}
```

Response: `{ "accepted": 1, "duplicate_skipped": 0 }`

#### `POST /ingest/glucose`

```json
{
  "readings": [
    {
      "external_id": "64abc123def",
      "source": "nightscout",
      "recorded_at": "2025-08-01T14:25:00Z",
      "glucose_mgdl": 112.0,
      "trend": "Flat",
      "device": "dexcom"
    }
  ]
}
```

Response: `{ "accepted": 1, "duplicate_skipped": 0 }`

#### `POST /ingest/workout`

```json
{
  "workouts": [
    {
      "external_id": "wr_abc123",
      "performed_at": "2025-08-01T10:00:00Z",
      "class_name": "CrossFit",
      "wod_name": "Fran",
      "result": "3:24",
      "result_type": "Time",
      "rx": true,
      "coach": "Jane",
      "location": "Main"
    }
  ]
}
```

Response: `{ "accepted": 1, "updated": 0, "duplicate_skipped": 0 }`

### Query

#### `GET /health/snapshot?window_minutes=60`

Returns the most recent value of every metric in the last N minutes.

```json
{
  "as_of": "2025-08-01T15:00:00Z",
  "window_minutes": 60,
  "latest_heart_rate_bpm": 68.0,
  "latest_spo2_percent": 98.0,
  "steps_in_window": 843,
  "calories_in_window": 42.1,
  "latest_glucose_mgdl": 108.0,
  "glucose_trend": "FortyFiveUp",
  "last_workout_performed_at": "2025-08-01T10:00:00Z",
  "last_workout_result": "3:24"
}
```

#### `GET /health/summary/daily?date=2025-08-01&tz=America/New_York`

Full daily aggregation: avg/min/max HR, SpO2, steps, calories, glucose stats including Time-in-Range (70–180 mg/dL), and all workouts.

#### `GET /healthz`

Liveness probe (no auth required): `{ "status": "ok" }`

---

## Database Schema

### `watch_readings`

| Column | Type | Notes |
|--------|------|-------|
| `id` | bigserial PK | |
| `device_id` | varchar(64) | |
| `recorded_at` | timestamptz | measurement time from watch |
| `heart_rate_bpm` | float | nullable |
| `heart_rate_accuracy` | smallint | 0=no contact, 3=high |
| `spo2_percent` | float | nullable |
| `steps_total` | int | cumulative counter since boot |
| `calories_kcal` | float | cumulative since boot |
| `accel_x/y/z` | float | m/s² |
| Unique | `(device_id, recorded_at)` | dedup key |

### `glucose_readings`

| Column | Type | Notes |
|--------|------|-------|
| `external_id` | varchar(128) UNIQUE | Nightscout `_id` or `dexcom_<epochMs>` |
| `source` | enum | `nightscout` \| `dexcom_share` |
| `recorded_at` | timestamptz | |
| `glucose_mgdl` | float | |
| `trend` | varchar(32) | `"Flat"`, `"FortyFiveUp"`, etc. |
| `trend_rate_mgdl_per_min` | float | nullable |

### `workouts`

| Column | Type | Notes |
|--------|------|-------|
| `external_id` | varchar(128) UNIQUE | Wodify record ID |
| `performed_at` | timestamptz | |
| `wod_name` | varchar(256) | e.g. `"Fran"` |
| `result` | varchar(256) | e.g. `"3:24"`, `"150 reps"` |
| `result_type` | varchar(64) | `Time`, `Reps`, `Load`, etc. |
| `rx` | boolean | Rx or Scaled |

---

## Authentication

Single API key in `X-Api-Key` header. Generate:

```bash
openssl rand -hex 20
```

Set in `server/.env` as `API_KEY=...` and in both Android projects' `local.properties` as `apiKey=...`.

---

## Adding a New Data Source

1. Add a model in `server/app/models/`
2. Add Pydantic schemas in `server/app/schemas/`
3. Add a service in `server/app/services/`
4. Add a router endpoint in `server/app/routers/ingest.py`
5. Add a client + worker in the relevant Android app
