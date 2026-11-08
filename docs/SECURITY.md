# Security & Architecture Policy — Ali Educational

This document outlines the security architecture, authentication controls, and data protection policies implemented in the Ali Educational Android shop-management system.

---

## 1. Role-Based Access Control (RBAC)

The application enforces a dual-role operational model:
- **OWNER**: Full administrative control over shop records, inventory corrections, supplier management, profit/loss analysis, cash-in/out categorisation, database backups, restoration, and bulk transaction voids.
- **STAFF**: Restricted point-of-sale operational role for bill creation, customer search, cash collection, and receipt printing/sharing.

### Route Protection & Fail-Closed Guard
Access to administrative screens is guarded by the reusable `OwnerOnly` Compose boundary:
```kotlin
@Composable
fun OwnerOnly(vm: AliViewModel, title: String, onBack: () -> Unit, content: @Composable () -> Unit)
```
- **Fail-Closed Default**: Any unrecognized role (`"UNKNOWN"`, `""`, null) or `"STAFF"` immediately renders `OwnerPermissionDeniedScreen` and prevents UI composition of sensitive elements.
- **Two-Layer Enforcement**: UI routes prevent navigation; `AppRepository` and Room DAO operations independently enforce `requireOwner()` within database transactions.

---

## 2. Lock Screen & App Protection

- **Fail-Closed Authentication**: The app lock screen does not permit direct bypass. If device biometrics or device PIN credentials are not configured or fail, the app locks down access to protected records.
- **Monotonic Timeout**: App timeout is evaluated against `SystemClock.elapsedRealtime()` to prevent bypass via manual clock tampering.
- **Recents Protection**: The app window enables `FLAG_SECURE` when locked, preventing sensitive financial numbers, debt figures, and customer data from appearing in the Android Recents multitasking switcher.

---

## 3. Step-Up Authentication

A **60-second step-up authentication window** is required for high-risk operations:
- Changing device roles (Staff to Owner or vice versa).
- Restoring a database backup.
- Disabling the application lock.
- Initiating bulk voids on bills or cash tracker transactions.

After 60 seconds of inactivity, the elevated privilege expires automatically, requiring re-authentication.

---

## 4. PIN Lockout & Recovery Secrets

- **Escalating Lockout Policy**: Repeated incorrect owner PIN attempts trigger progressive lockout intervals:
  - 5 failed attempts: 1-minute lockout.
  - 8 failed attempts: 5-minute lockout.
  - 10 failed attempts: 15-minute lockout.
  - 12+ failed attempts: 60-minute lockout.
- **Keystore-Sealed Secrets**: Owner recovery tokens are encrypted using AES-256-GCM via `SecretBox` backed by the hardware Android Keystore. Master keying material never leaves secure hardware.

---

## 5. Streaming Encrypted Backups (`ALIBK1`)

- **Chunked AES-GCM Format**: Backups are encrypted into the streaming `ALIBK1` container using 1 MiB chunked AES-256-GCM authenticated encryption. This prevents memory exhaustion on low-RAM shop devices while ensuring complete archive integrity.
- **Passphrase Requirement**: Encrypted backups require an owner passphrase. Plaintext unencrypted backups to unmanaged public directories are strictly prohibited.
- **Private Scoped Storage**: Backups are stored in the app's sandboxed private storage directory (`context.filesDir/backups`). Public `Documents/` folder writes are disabled to prevent unauthorized external access.

---

## 6. Two-Phone Synchronization Security

Local synchronization between the Main Phone (Owner Server) and Counter Phone (Staff Client) operates over an encrypted local Wi-Fi channel:
- **Transport Security**: Enforced `TLSv1.3` / `TLSv1.2` with pre-shared certificate pinning. Cleartext HTTP traffic is rejected at both the network security config and socket layer.
- **IP Rate Limiting & Concurrency Cap**: `SyncIpRateLimiter` restricts connection frequency, imposing escalating backoff penalties on untrusted or failing IP addresses. Server socket concurrency is capped at 4 concurrent connections.
- **Per-Bill Atomic Ingestion**: Incoming bills are ingested individually inside `db.withTransaction`. If an item fails validation, only that specific bill is rejected with status `SYNC_REJECTED` and an explicit reason code; previously ingested valid bills remain intact.

---

## 7. Data Privacy & Integrity

- **Paise-Only Money Arithmetic**: All financial calculations (gross, net, discounts, dues, cost of goods, taxes, profits) use 64-bit integer (`Long`) paise to eliminate IEEE-754 floating-point rounding errors.
- **ProGuard Log Stripping**: Production release builds strip verbose, debug, and informational logging (`Log.v`, `Log.d`, `Log.i`) using R8/ProGuard rules to prevent leakage of customer identities, phone numbers, and financial details into device logcat.
- **Safe File Provider**: `file_paths.xml` grants strict, scoped read access only to explicit receipt and export subdirectories; root storage directories (`root-path`) are strictly forbidden.
