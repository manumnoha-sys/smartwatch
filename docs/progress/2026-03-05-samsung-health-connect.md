# 2026-03-05 — Samsung Health / Galaxy Watch Integration Complete

## Summary

Full end-to-end data pipeline established from Galaxy Watch → Samsung Health → Health Connect API → Android app → FastAPI server. 121 watch readings (HR, SpO2, steps) and 1 exercise session successfully synced to the server database.

---

## Problems Solved

### 1. App crash on startup — `ForegroundServiceStartNotAllowedException`

**File:** `HealthBridgeApp.kt`, `BootReceiver.kt`

Android 14 blocks `startForegroundService()` calls made from `Application.onCreate()` or a `BroadcastReceiver` when the app is not in the foreground. The app was crashing silently on every launch.

**Fix:** Wrapped both `startForegroundService()` calls in `runCatching { }`.

```kotlin
// Before
startForegroundService(Intent(this, BridgeService::class.java))

// After
runCatching { startForegroundService(Intent(this, BridgeService::class.java)) }
```

---

### 2. Worker silently skipped — strict permission check

**File:** `SamsungHealthSyncWorker.kt`, `SamsungHealthReader.kt`

The worker called `hasPermissions()` which used `containsAll()` — if any single permission was denied (e.g. `READ_CALORIES_BURNED`), the entire sync was skipped with no useful log output.

**Fix:** Added `grantedPermissions()` method returning the intersection of granted vs requested permissions, and changed the worker to proceed with whichever permissions are available.

```kotlin
// SamsungHealthReader.kt — new method
suspend fun grantedPermissions(): Set<String> {
    val c = client ?: return emptySet()
    return c.permissionController.getGrantedPermissions()
        .intersect(HEALTH_CONNECT_PERMISSIONS)
}

// SamsungHealthSyncWorker.kt — partial permission check
val granted = reader.grantedPermissions()
if (granted.isEmpty()) {
    Log.w(TAG, "No Health Connect permissions granted — skipping")
    return Result.success()
}
Log.i(TAG, "Granted permissions: ${granted.size}/${HEALTH_CONNECT_PERMISSIONS.size}")
```

---

### 3. App not appearing in Health Connect "Data and access"

**File:** `AndroidManifest.xml`

Two required declarations were missing:
- `<queries>` element to allow the app to discover the Health Connect package on Android 11+
- `android.health.connect.client.PRIVACY_POLICY_URL` metadata, required by Health Connect on Android 14+ before it will register the app

**Fix:**

```xml
<queries>
    <package android:name="com.google.android.apps.healthdata" />
</queries>

<application ...>
    <meta-data
        android:name="android.health.connect.client.PRIVACY_POLICY_URL"
        android:value="https://github.com/manumnoha-sys/health-dashboard" />
```

---

### 4. "No requestable permission in the request" error

Root cause was the same missing `<queries>` and metadata as above. Once those were added, the Health Connect permission dialog appeared correctly.

---

### 5. Health Connect permissions not triggered correctly

**File:** `MainActivity.kt`

The app had no UI to request Health Connect permissions. Permissions can only be granted via `PermissionController.createRequestPermissionResultContract()` — they cannot be granted manually through system settings without the app initiating the request.

**Fix:** Added a "Connect Health Connect" button using the correct contract, plus a "Sync Now" button for on-demand testing via `OneTimeWorkRequestBuilder`.

```kotlin
private val requestPermissions =
    registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(HEALTH_CONNECT_PERMISSIONS)) {
            Toast.makeText(this, "Health Connect connected!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some Health Connect permissions denied", Toast.LENGTH_LONG).show()
        }
        setContentView(createLayout())
    }

// Sync Now button
layout.addView(Button(this).apply {
    text = "Sync Now (Samsung Health)"
    setOnClickListener {
        val req = OneTimeWorkRequestBuilder<SamsungHealthSyncWorker>().build()
        WorkManager.getInstance(this@MainActivity).enqueue(req)
        Toast.makeText(this@MainActivity, "Sync started — check logcat", Toast.LENGTH_SHORT).show()
    }
})
```

---

### 6. CLEARTEXT HTTP blocked by Android network security policy

**File:** `res/xml/network_security_config.xml` (new), `AndroidManifest.xml`

Android 9+ blocks all cleartext (HTTP) traffic by default. The worker was reading 121 samples successfully but the POST to `http://192.168.1.26:8000` failed with:

```
CLEARTEXT communication to 192.168.1.26 not permitted by network security policy
```

**Fix:** Created `network_security_config.xml` permitting cleartext to the server IPs, and referenced it in the manifest.

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">100.83.146.119</domain>
        <domain includeSubdomains="false">192.168.1.26</domain>
    </domain-config>
</network-security-config>
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

### 7. Server unreachable — Tailscale IP not routable from phone

The `local.properties` had `serverUrl=http://100.83.146.119:8000` (Tailscale IP). The phone (192.168.1.15) is on the LAN but not on Tailscale, so it couldn't reach the Tailscale address.

**Fix:** Changed `serverUrl` to the NUC's LAN IP:

```
serverUrl=http://192.168.1.26:8000
```

---

## Final Verification

```
I/SamsungHealthSync: Granted permissions: 4/5
I/SamsungHealthSync: Reading samples since 2026-03-05T06:38:15Z
W/SamsungHealthReader: Calories read failed: SecurityException (READ_CALORIES_BURNED denied)
I/SamsungHealthSync: Got 121 samples from Health Connect
I/SamsungHealthSync: Watch samples: accepted=121 dup=0
I/SamsungHealthSync: Got 1 sessions from Health Connect
I/SamsungHealthSync: Sessions: accepted=1 updated=1
```

Server `/health/snapshot` confirmed `last_workout_performed_at` and `last_workout_result: "83 min"`.

---

## Current Permission State

| Permission | Status |
|---|---|
| READ_HEART_RATE | Granted |
| READ_STEPS | Granted |
| READ_EXERCISE | Granted |
| READ_OXYGEN_SATURATION | Granted |
| READ_CALORIES_BURNED | Denied (user skipped) |

Calories read failure is handled gracefully via `runCatching` — the other 4 metrics sync normally.

---

## Sync Schedule

The `SamsungHealthSyncWorker` runs every 30 minutes on a `CONNECTED` network constraint, reading the last 24 hours of data. Duplicate samples are deduplicated server-side (`dup=0` on subsequent runs).

---

## Known Issues / Next Steps

- `READ_CALORIES_BURNED` permission denied — user can re-grant via the "Connect Health Connect" button
- Google Fit integration pending (Client ID + Secret not yet provided)
- Consider reducing the 24-hour lookback window back to 35 minutes once the system is running stably
