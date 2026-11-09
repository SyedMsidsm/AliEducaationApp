# Ali Educational — Security, Reliability, Testing and Feature Plan

Prepared 8 November 2026 from a static read of `SyedMsidsm/AliEducaationApp` (app version 1.0.15, Room schema 8).
This document is written so a fast AI coding assistant can execute it **one small task at a time**. Every task names the files, the exact problem, the approach, a ready-to-paste prompt, and the checks that prove it is done.

> **Updated 9 November 2026:** Section 9 (at the end) adds the barcode generator, a stock-import upgrade, a redesign of Bill History / Cash In-Out / Add Stock, and new tests. Remaining original tasks (TEST-04 to TEST-10 and CI) are unchanged.

> **Read this first — limits of this review.** It is a static review. The code was not compiled and no test was run (the review environment could not reach the Android SDK). Every code snippet below is a *starting point* that must be compiled and tested. Each item carries a confidence tag:
> **[Verified]** = I read the exact lines. **[Likely]** = strongly implied by the code but needs a device check. **[Check]** = I have not confirmed; the task starts by confirming.

---

## 0. How to use this document with an AI coding assistant

Fast coding models are good at small, well-specified edits and are weak at large refactors, remembering earlier files, and honestly reporting whether tests passed. So this plan is built around that:

1. **One task per chat session.** Start a fresh session for each task ID (for example `SEC-01`). Do not paste the whole document; paste Section 0.1 (the rules) plus that one task block.
2. **Every task = one git branch = one small pull request.** Branch name is given in each task. Never combine tasks.
3. **The assistant must read before editing** and must show the exact command output of build/test runs. "Tests should pass" is not evidence; the pasted output is.
4. **You (the owner) run the build on your machine** and install on a *debug* build or emulator. Never test a new build by uninstalling or clearing data on the live shop phone.

### 0.1 Master rules — paste at the start of every session

```text
You are modifying an existing production Android app (Kotlin, Jetpack Compose, Room, Hilt).
It handles real shop money, so correctness matters more than speed.

RULES
1. Work only on the task I paste below. Do not refactor, rename, reformat, or "improve" anything else.
2. Before editing any file, open it and quote the exact lines you will change. If a file or function
   I mention does not exist, STOP and tell me instead of inventing it.
3. Do not add any new library/dependency unless the task says so. If you think one is needed, ask first.
4. Never change database entities or add a migration unless the task says so. If you do, bump the
   version, write the Migration, and add a migration test.
5. Money is stored as Long paise. Never use Double or Float for money.
6. Do not weaken or delete an existing test to make it pass. If a test fails, explain why.
7. After the change, run exactly: ./gradlew testDebugUnitTest lintDebug
   and paste the real final output. If you cannot run it, say "NOT RUN" — never claim it passed.
8. Finish with: (a) list of files changed, (b) what you did NOT do, (c) anything uncertain.
9. Keep each answer small: show diffs/changed functions, not whole files, unless the file is under 80 lines.
```

### 0.2 Definition of Done (applies to every task)

- Code compiles; `./gradlew testDebugUnitTest lintDebug` output pasted with no new failures and no new lint *errors*.
- New behaviour has at least one automated test (unit or instrumented) that **fails before** the change and **passes after**.
- You manually checked the listed "Manual check" on a real debug install.
- Version bumped (`versionCode` +1) only when you cut a release candidate, not per task.

### 0.3 Suggested order

| Phase | Tasks | Why this order |
|---|---|---|
| 1. Close the holes | SEC-01, SEC-02, SEC-03, SEC-08 | Smallest changes, biggest security gain |
| 2. Protect secrets and data | SEC-04, SEC-05, SEC-06, SEC-10 | Needs the step-up helper from SEC-03 |
| 3. Make sync safe | SYNC-01, SYNC-02, SEC-09 | Protects money integrity |
| 4. Database | DB-01 (migration 8→9) | Do after sync changes so only one migration ships |
| 5. Features | FEAT-01 (bulk void), FEAT-02 (landing/logo) | Rely on SEC-03 and DB-01 |
| 6. Cleanup | PERF-01, PERF-02, CODE-01…03, BUILD-01 | Safe to do last |
| Throughout | TEST-01…TEST-10 (Section 5) | Write the test **first** for each task |

---

## 1. Honest security assessment

The app is **not** without security. These parts are good and should be kept: owner PIN uses salted PBKDF2 (120,000 rounds) with persisted throttling; Billing↔Owner sync uses TLS with an out-of-band pinned certificate fingerprint and a constant-time token comparison; cost quotes use Android Keystore AES-GCM so the Billing phone cannot read purchase costs; backup restore rejects path traversal; device role fails closed (`"UNKNOWN"` until loaded); attachment file names are stripped of directories.

But the *front door* is open, and several data copies are unprotected. In practice the biggest weaknesses are:

1. The lock screen can be skipped with one tap (SEC-01).
2. Nothing sensitive requires a fresh authentication (SEC-03).
3. Backups and exports are plain, readable files, some possibly in the public Documents folder (SEC-06).
4. The recovery secret's digest is public (SEC-04).

### 1.1 Threat model (who and what we defend against)

| Threat | Example in a shop | Main defences |
|---|---|---|
| Someone holding the unlocked/unattended phone | Staff, a customer, a child | SEC-01, 02, 03 |
| Lost or stolen phone | Phone left in a rickshaw | Android lock + SEC-06, SEC-07 |
| Another app or a person with a file manager | Reading exported Excel or backups | SEC-06, SEC-08 |
| Someone on the shop Wi-Fi | Probing the Owner's sync port | SEC-09 |
| Honest mistakes | Wrong delete, rotated screen mid-sale | SEC-02, FEAT-01 |

Out of scope (state this to yourself honestly): a determined attacker with a **rooted** phone and unlimited time, and nation-state attacks. This plan makes casual and opportunistic abuse hard and protects money integrity; it does not make the phone tamper-proof.

---

## 2. Security tasks

### SEC-01 — Close the lock-screen bypass  `[Verified]` · Severity: CRITICAL · Branch `sec-01-lock-bypass`

**Problem.** In `app/src/main/java/com/alieducational/app/ui/auth/UnlockScreen.kt`:
- A `TextButton` labelled **"Enter Shop Directly"** calls `onUnlocked` with no authentication.
- `triggerUnlock()` calls `onUnlocked()` when `biometricAuthManager.canAuthenticate()` is false ("If device has no screen lock, allow proceed") — it **fails open**.
- The same file's second button, "Use Device PIN / Password", just calls the same function.

Result: the app lock setting protects nothing. On the Owner phone, anyone past the lock can reach cost, profit, backup/restore and settings, because those screens are gated only by device role, not by who is holding the phone.

**Fix approach.**
1. Delete the "Enter Shop Directly" button entirely. (If you want a staff-convenience mode, build it properly later as an explicit *Billing role* with its own limited screens — not as a bypass.)
2. Fail closed: if no device credential exists, show an explanatory state with a button to open Android's security settings; do not unlock.
3. In `BiometricAuthManager`, only report errors from `onAuthenticationError` (terminal). `onAuthenticationFailed` fires on every wrong finger and must *not* be treated as an error that dismisses anything.
4. Handle Android 9–10 (API 28–29): `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is not supported there per AndroidX documentation, while `minSdk` is 26. Pick authenticators by SDK level (for example, `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` when `Build.VERSION.SDK_INT < 30`). **[Check]** test on an API 28/29 emulator before trusting this.
5. Make it impossible to enable the app lock when no credential exists, and require authentication to *disable* it (this uses SEC-03; until SEC-03 exists, at least gate it with `promptBiometric`).

**Starting-point code (`UnlockScreen.kt`):**

```kotlin
var noCredential by remember { mutableStateOf(false) }

fun triggerUnlock() {
    if (activity != null && biometricAuthManager.canAuthenticate()) {
        noCredential = false
        biometricAuthManager.promptBiometric(
            activity = activity,
            title = context.getString(R.string.unlock_title),
            subtitle = context.getString(R.string.unlock_prompt),
            onSuccess = onUnlocked,
            onError = { /* stay on the unlock screen */ }
        )
    } else {
        noCredential = true      // FAIL CLOSED — never call onUnlocked() here
    }
}
// In the layout: if (noCredential) {
//   Text("Set up a screen lock in Android Settings to use Ali Educational.")
//   Button(onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }) { Text("Open settings") }
// }
// DELETE the TextButton(onClick = onUnlocked) { Text("Enter Shop Directly") } block.
```

**Prompt to paste (after 0.1):**
```text
TASK SEC-01. File: app/src/main/java/com/alieducational/app/ui/auth/UnlockScreen.kt (and util/BiometricAuthManager.kt).
1) Remove the "Enter Shop Directly" TextButton completely.
2) In triggerUnlock(), when activity is null or canAuthenticate() is false, DO NOT call onUnlocked().
   Instead set a state flag and show a message plus a button opening Settings.ACTION_SECURITY_SETTINGS.
3) In BiometricAuthManager.promptBiometric, onAuthenticationFailed must NOT call onError.
4) Make allowed authenticators depend on SDK: API >= 30 use BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
   below 30 use BIOMETRIC_WEAK or DEVICE_CREDENTIAL. Apply the same constant in canAuthenticate() and the PromptInfo.
5) Add a Compose instrumented test: when locked, no node with text "Enter Shop Directly" exists.
Do not touch any other screen.
```

**Done when.** No code path calls `onUnlocked` without a successful `BiometricPrompt` result. A device with no screen lock cannot get past the unlock screen.
**Tests.** TEST-05 (security UI tests). **Manual check.** Remove the screen lock on a test emulator → app stays on "set up a screen lock"; set it → fingerprint/PIN unlocks.

---

### SEC-02 — Lock state must survive rotation; hide sensitive screens in Recents  `[Verified]` · Severity: HIGH · Branch `sec-02-lock-state`

**Problem.**
- `MainActivity.kt` stores `isAppLocked` as a plain Activity field. With no `configChanges` and no orientation lock in the manifest, rotating the phone or changing system dark mode recreates the Activity and flips `isAppLocked` back to `true`, so the shop owner is asked to authenticate in the middle of a sale. (The relock timer also uses wall-clock time, so changing the phone clock affects it.)
- There is no `FLAG_SECURE`/recents-screenshot protection, so the Recents thumbnail can show the Profit or Cash screen after the app is locked.

**Fix approach.**
1. Move lock state into `AliViewModel` (survives configuration changes): `var isAppLocked by mutableStateOf(true)`. Obtain it with `by viewModels()` in the activity.
2. Use `SystemClock.elapsedRealtime()` (monotonic) for the background timer instead of `System.currentTimeMillis()`.
3. Hide the Recents thumbnail: on API 33+ call `setRecentsScreenshotEnabled(false)`; below that, set `FLAG_SECURE` on the window (this also blocks screenshots, which is acceptable for a finance app; make it a setting later if staff complain).
4. Re-lock immediately when the screen turns off if lock timeout is 0 (optional).

**Prompt:**
```text
TASK SEC-02. Files: MainActivity.kt, ui/AliViewModel.kt.
1) Move `isAppLocked` from MainActivity into AliViewModel as `var isAppLocked by mutableStateOf(true)`; MainActivity reads/writes it via the ViewModel.
2) Replace System.currentTimeMillis() with SystemClock.elapsedRealtime() for lastPausedTime / elapsed calculation.
3) In onCreate: if Build.VERSION.SDK_INT >= 33 call setRecentsScreenshotEnabled(false) else window.setFlags(FLAG_SECURE, FLAG_SECURE).
4) Add an instrumented test that rotates the activity (ActivityScenario.recreate) while unlocked and asserts the unlock screen is NOT shown.
```

**Done when.** Rotating while unlocked does not prompt; backgrounding for longer than the timeout still locks; Recents shows a blank/blurred card.
**Manual check.** Open Profit, press Recents → no financial numbers visible in the thumbnail.

---

### SEC-03 — Step-up authentication for sensitive actions  `[Verified gap]` · Severity: HIGH · Branch `sec-03-step-up`

**Problem.** Once inside the app, nothing sensitive asks again. `SettingsScreen` can switch the lock off with one tap (`viewModel.setLockEnabled(it)`), backup restore, data export, role changes, Pigmy withdrawals and voiding sales/expenses need no proof of who is holding the phone.

**Fix approach.** Add one reusable helper, `StepUpAuth`, that asks for biometric/device credential and remembers success for a short window (60 s), reset whenever the app locks. Then call it before each sensitive action. Start with this list and add more as you review screens:

| Action | Where |
|---|---|
| Turn app lock off | `SettingsScreen` lock switch |
| Restore a backup | `BackupRestoreScreen` / `BackupRestoreService` |
| Export Excel / share backup | Export buttons (CashTracker, Stock, Backup) |
| Change device role / set or change Owner PIN | `SettingsScreen` |
| Void sale / void expense / bulk void | `BillHistoryScreen`, `CashTrackerScreen` (FEAT-01) |
| Show profit & cost screens (optional, re-prompt after timeout) | `ProfitScreen` |

**Starting-point code (`util/StepUpAuth.kt`):**

```kotlin
@Singleton
class StepUpAuth @Inject constructor(private val bio: BiometricAuthManager) {
    private var lastOk = 0L
    fun reset() { lastOk = 0L }
    private fun fresh(windowMs: Long) = lastOk != 0L && SystemClock.elapsedRealtime() - lastOk < windowMs

    fun require(
        activity: FragmentActivity, title: String,
        windowMs: Long = 60_000L,
        onOk: () -> Unit, onFail: (String) -> Unit
    ) {
        if (fresh(windowMs)) { onOk(); return }
        if (!bio.canAuthenticate()) { onFail("Set up a screen lock first"); return }
        bio.promptBiometric(activity, title,
            onSuccess = { lastOk = SystemClock.elapsedRealtime(); onOk() },
            onError = onFail)
    }
}
```
Call `reset()` whenever `isAppLocked` becomes `true`.

**Prompt:**
```text
TASK SEC-03. Create util/StepUpAuth.kt exactly as the snippet, injected with Hilt (BiometricAuthManager is already @Singleton @Inject).
Expose it to screens through AliViewModel (add `val stepUp: StepUpAuth` as a constructor-injected property; do not rewrite the ViewModel).
Apply it ONLY to these 3 actions in this task: (a) turning the app lock OFF in SettingsScreen, (b) starting a backup restore, (c) changing the device role.
Each must call stepUp.require(activity, ...) and run the original action only inside onOk.
Add unit tests for StepUpAuth using a fake BiometricAuthManager: fresh window skips prompt; expired window prompts; reset() forces prompt.
```

**Done when.** All three actions prompt; repeated actions within 60 s do not re-prompt; locking the app clears the window.

---

### SEC-04 — Owner PIN hardening and recovery-secret rotation  `[Verified]` · Severity: MEDIUM · Branch `sec-04-pin-recovery`

**Problems.**
- **Throttling is gentle.** `authorizeOwner` allows 5 tries, then blocks 60 s, then resets the counter to zero. That is about 7,200 guesses/day on a 6-digit PIN (1,000,000 possibilities).
- **Recovery proof digest is public.** `AppPreferences.recoverOwnerOnTrustedDevice` compares `SHA-256(file contents)` to a constant hardcoded in the (public) repository, and the same constant applies to every installation. If the secret in `owner-recovery-once.txt` is a human-chosen phrase it can be brute-forced offline from the public digest. Recovery also resets the role to OWNER and **removes the owner PIN**. (Device authentication is still required, which is why this is MEDIUM and not CRITICAL.) **[Check]** how random the file contents are.
- A legacy default token string (`AE-SYNC-987654`) is still referenced in code. It is rejected, so it is harmless, but leaving old defaults in a public repo invites confusion.

**Fix approach.**
1. **Escalating lockout** that does not reset on its own: 5 failures → 1 min, next 5 → 5 min, then 15 min, then 1 h (cap). Reset the counter only on success. Persist the "blocked until" time as `SystemClock.elapsedRealtime()`-independent wall time *and* detect clock rollback (if wall time goes backwards, keep the block).
2. **Recovery secret.** Generate a new random secret of at least 32 bytes (`SecureRandom`, Base64) and replace the hardcoded digest. Treat the old digest as exposed. Keep the file out of the repo and out of backups. Better: store only an HMAC with a key from Android Keystore so the digest is not a constant shared by every install.
3. Delete the legacy token constant after confirming no installed device still depends on it.

**Prompt:**
```text
TASK SEC-04. File: data/preferences/AppPreferences.kt.
1) Replace the fixed 60-second lock in authorizeOwner with an escalating lockout: after every 5 consecutive failures,
   block for 1 min, then 5, 15, 60 minutes (cap 60). Persist a failure-round counter (new DataStore key) and
   reset it only on a successful PIN. Do not change the PBKDF2 parameters or the stored format.
2) Do not change recoverOwnerOnTrustedDevice yet — only add a TODO comment. (Secret rotation is a manual owner step; see plan.)
3) Add unit tests with a fake clock: 5 failures => blocked 1 min; wrong PIN during block stays blocked; correct PIN after block expires succeeds and resets rounds.
```
Then the **owner** rotates the recovery secret by hand: generate a new random value, update the constant in one commit, provision the file on the trusted phone only, test recovery once on the emulator.

**Tests.** TEST-05. **Done when.** Brute-forcing is measurably slower (unit test asserts the schedule).

---

### SEC-05 — Encrypt secrets stored in DataStore  `[Verified storage, Likely risk]` · Severity: MEDIUM · Branch `sec-05-secret-box`

**Problem.** `ali_educational_preferences` (under `files/datastore/`) holds the sync token and certificate pin in clear text, plus the device role and PIN hash. The Android sandbox protects it on a normal phone, but it is included in device-to-device transfer (`dataExtractionRules`), copied by some backup tools, and readable if the phone is rooted. Your own docs say tokens and keys are "deliberately not portable" — encrypt them so that is actually true.

**Fix approach.** Add a small `SecretBox` (Android Keystore AES-GCM, same pattern as your existing `CostQuote`) and seal only the sync token and certificate pin before saving; unseal on read. Return `""` if unsealing fails (for example after a device transfer), which makes the phone ask to pair again — the intended behaviour.

```kotlin
object SecretBox {
    private const val ALIAS = "ali-secret-box-v1"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(ALIAS)) KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
        return ks.getKey(ALIAS, null) as SecretKey
    }
    fun seal(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return "v1:" + Base64.encodeToString(c.iv + c.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }
    fun open(sealed: String): String? = runCatching {
        require(sealed.startsWith("v1:"))
        val b = Base64.decode(sealed.removePrefix("v1:"), Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b.copyOfRange(0, 12))) }
        String(c.doFinal(b.copyOfRange(12, b.size)))
    }.getOrNull()
}
```

**Migration of existing values.** On read, if the stored value does not start with `v1:`, treat it as legacy plaintext, use it once, and rewrite it sealed. This avoids forcing a re-pair on the live phones.

**Prompt:**
```text
TASK SEC-05. Create data/service/SecretBox.kt as in the plan (copy the snippet). In AppPreferences, seal values for SYNC_TOKEN and OWNER_CERT_PIN on write and open them on read.
If a stored value does not start with "v1:", accept it as legacy plaintext and immediately re-save it sealed.
Do not touch OWNER_PIN (already hashed). Add Robolectric-free unit tests only for the legacy-detection logic by extracting a pure function; the Keystore part is covered by an instrumented test (seal/open round trip, tampered ciphertext returns null).
```

**Caveat.** Keystore keys do not move between devices; after a phone replacement the Billing phone must re-pair. That matches your existing design for cost-quote keys.

---

### SEC-06 — Protect backups and exports  `[Verified: no encryption; Check: public-folder writes]` · Severity: HIGH · Branch `sec-06-backup-protection`

**Problems.**
1. `BackupRestoreService` builds a ZIP with the full database (customer names, phone numbers, balances) and saves it unencrypted, including through the chosen Drive folder.
2. `BackupRestoreService.kt:262` and `CashTrackerExcelService.kt:54` reference `Environment.getExternalStoragePublicDirectory(DOCUMENTS)`. Files in the public Documents folder are visible to file managers and to other apps with broad storage access. **[Check]** on a real Android 13/14 phone whether these writes succeed under scoped storage or silently fall back to the app folder.
3. Shared files (receipts, stock exports) are created and handed to `ACTION_SEND` but there is no cleanup of temp copies. **[Check]**

**Fix approach.**
- **Encrypt backups** with a passphrase the owner types at backup time (and again to restore). Use PBKDF2-HMAC-SHA256 (≥ 210,000 iterations for new code; you already use 120,000 for the PIN) to derive an AES-256 key, and AES-GCM. File layout: `magic "ALIBK1" | version | salt(16) | baseIV(12) | ciphertext chunks`.
- **Chunk the encryption** (1 MiB chunks; per-chunk nonce = baseIV XOR chunk counter; last chunk flagged in the AAD) so a large backup is never held fully in memory. If that is too complex for the first version, cap the backup size at a safe limit and fail with a clear message above it.
- **Keep restoring old unencrypted backups** so no existing backup becomes useless. Show "unencrypted backup" as a warning in the UI.
- **Never write exports to public folders.** Write to `getExternalFilesDir("exports")` or `cacheDir`, share via `FileProvider`, and delete the temp file after sharing/after 24 h. Saving a copy to Documents becomes an explicit user action using the system file picker (`ACTION_CREATE_DOCUMENT`) which needs no storage permission.
- A **lost passphrase means a lost backup** — the UI must say this clearly, and your docs should tell the owner to keep the passphrase somewhere safe.

**Prompt (split into two sessions):**
```text
TASK SEC-06a. Add data/service/BackupCrypto.kt with:
  fun encrypt(input: InputStream, out: OutputStream, passphrase: CharArray)
  fun decrypt(input: InputStream, out: OutputStream, passphrase: CharArray): Boolean   // false on wrong passphrase/tamper
Format: magic "ALIBK1", version byte, 16-byte salt, 12-byte base IV, then 1 MiB AES-GCM chunks (nonce = baseIV XOR chunkIndex, AAD = chunkIndex + isLastFlag).
Key: PBKDF2WithHmacSHA256, 210000 iterations, 256-bit. Pure Kotlin, no Android classes, so it is JVM-unit-testable.
Tests: round trip (empty, 1 byte, 1 MiB exactly, 5 MiB random); wrong passphrase => false; flip one byte anywhere => false; truncated file => false; chunks cannot be reordered.

TASK SEC-06b. In BackupRestoreService wrap the existing archive creation with BackupCrypto when the user supplies a passphrase,
and on restore detect the "ALIBK1" magic and ask for the passphrase. Detect unencrypted legacy ZIPs by the ZIP signature and still restore them.
Do not change the archive contents or the validation steps that already exist. Add the UI prompts in BackupRestoreScreen (passphrase + confirm).

TASK SEC-06c. Search for getExternalStoragePublicDirectory. Replace each with getExternalFilesDir("exports") and share via FileProvider only. Delete temp exports older than 24h at app start.
```

**Done when.** A backup opened in a hex viewer shows no readable customer names; a wrong passphrase never produces a partial restore; old backups still restore.

---

### SEC-07 — Database encryption at rest: a decision, not a task  · Severity: LOW for now

Room's `ali_educational.db` is plain SQLite inside the app's private folder. On modern phones that folder is already encrypted by the operating system (file-based encryption) when a screen lock is set, and other apps cannot read it. Adding SQLCipher would protect against a rooted phone or a dumped image, but it:
- changes the database file format (a risky migration on live shop data),
- complicates your backup/restore and the existing snapshot logic,
- adds a native library and APK size.

**Recommendation:** do **not** add SQLCipher now. First finish SEC-01…06 (they remove the realistic risks). Revisit only if the phone will hold data that must survive a rooted-device threat. If you decide to do it later, treat it as its own project with its own migration rehearsal on copied production data.

---

### SEC-08 — Manifest, FileProvider and build hygiene  `[Verified]` · Severity: MEDIUM · Branch `sec-08-manifest`

| Item | Problem | Fix |
|---|---|---|
| `usesCleartextTraffic="true"` | All sync traffic is HTTPS now; no `http://` string exists in the source | Set to `false` |
| `CHANGE_WIFI_MULTICAST_STATE` | Declared but no `MulticastLock`/`NsdManager` usage found | Remove (unless you adopt NSD in SYNC-02) |
| `file_paths.xml` | `external-path "."` and `files-path "."` expose entire roots, including `files/datastore/` | Replace with specific sub-folders only (see below) |
| Widget receiver `exported="true"` | Required for widgets; the code only refreshes widgets on any broadcast | Keep exported; make `onReceive` ignore actions other than the widget actions it knows |
| `MainActivity` exported with extras | Another app can send an intent with `extra_widget_action` and navigate an *unlocked* app | Accept widget extras only when `intent.action == ACTION_WIDGET_CLICK` **and** the data URI scheme is `aliedu`; ignore everything else |
| `allowBackup="true"` | `fullBackupContent="false"` and `dataExtractionRules` already restrict cloud backup | Keep; document that manual encrypted backup is the recovery path |

**Safer `file_paths.xml` (adjust to the folders the code really uses — first run `grep -rn "getUriForFile" app/src/main` and list each folder):**

```xml
<paths>
    <cache-path name="exports" path="exports/" />
    <cache-path name="camera" path="camera_temp/" />
    <external-files-path name="exports_ext" path="exports/" />
    <files-path name="receipts" path="receipts/" />
</paths>
```

**Prompt:**
```text
TASK SEC-08. (1) AndroidManifest.xml: set usesCleartextTraffic false; remove CHANGE_WIFI_MULTICAST_STATE.
(2) List every FileProvider.getUriForFile call site and the folder it points at; then rewrite res/xml/file_paths.xml with ONLY those sub-folders (no path="." entries). Show me the list before editing.
(3) In MainActivity.extractWidgetAction accept the extra only if intent.action == AliSlimBarWidgetProvider.ACTION_WIDGET_CLICK.
(4) Build debug, then manually share a receipt, a stock export and a backup and confirm each still opens.
```

**Done when.** All share flows still work, and `grep -n '"\."' app/src/main/res/xml/file_paths.xml` finds nothing.

---

### SEC-09 — Harden the sync server  `[Verified]` · Severity: MEDIUM · Branch `sec-09-sync-server`

**Problems in `TwoPhoneSyncService.handleClientRequest`:**
- Any failed request, including a stranger's malformed TLS handshake or a bad token, sets the Owner's visible state to `Error(...)` even though hosting is fine. Anyone on the Wi-Fi can make the Owner's status flicker to an error.
- No limit on simultaneous connections; one coroutine per accepted socket.
- Failed-token attempts are not rate-limited per address.
- One shared token for all Billing devices; rotating it disconnects everyone.
- `HttpsURLConnection` objects on the client are not disconnected on failure paths.

**Fix approach.**
1. Do not set the user-visible `Error` state for authentication or handshake failures; log them to an internal counter and show "N blocked attempts" in Settings instead.
2. Cap concurrent connections with a `Semaphore(4)`; reject (close immediately) when full.
3. Rate-limit by remote IP: after 5 bad tokens in 10 minutes, ignore that IP for 15 minutes (in memory is enough).
4. Per-device tokens: the token map `deviceId → token`; revoking one device does not break the other. (Needs a small UI in Settings; plan it after SYNC-01.)
5. Set `sslParameters.protocols = arrayOf("TLSv1.3","TLSv1.2")` explicitly on the server socket.
6. Call `disconnect()` in a `finally` on every client `HttpsURLConnection`.

**Prompt:**
```text
TASK SEC-09. File data/service/TwoPhoneSyncService.kt only.
1) In handleClientRequest, failures caused by invalid token, malformed request or TLS handshake must NOT assign SyncConnectionState.Error. Count them in a new MutableStateFlow<Int> `blockedAttempts`.
2) Add a Semaphore(4); acquire with tryAcquire() at the start of each connection; if it fails, close the socket and return.
3) Add an in-memory rate limiter keyed by socket.inetAddress: 5 bad-token responses within 10 minutes => close that IP's sockets immediately for 15 minutes.
4) In the client code wrap each HttpsURLConnection use in try/finally with disconnect().
Keep response formats unchanged. Add unit tests for the rate limiter as a separate pure class.
```

---

### SEC-10 — Logging, error messages and release hardening  `[Verified/Likely]` · Severity: LOW-MEDIUM · Branch `sec-10-logging-release`

- `Log.e("AliSync", "Sync request rejected", e)` and 10 places that show `e.message` / `it.message` to the user can leak internal details (paths, SQL, class names). Show a short friendly message to the user; keep the detail in a debug-only log.
- Strip verbose logging from release builds by adding to `proguard-rules.pro`:
  ```
  -assumenosideeffects class android.util.Log { public static *** d(...); public static *** v(...); public static *** i(...); }
  ```
  (Keep `w` and `e` for crash diagnosis, but never log phone numbers, amounts tied to named customers, tokens, PINs or certificate fingerprints.)
- Narrow the ProGuard keep rules. `-keep class org.apache.** { *; }` and `-keep class dagger.hilt.** { *; }` stop R8 from shrinking those libraries, inflating the APK. Do this carefully: shrink one rule, build a **release** APK, and run a full export/import test, because POI uses reflection. If a feature breaks, restore that rule.
- Pin the Gradle distribution: add `distributionSha256Sum=<official checksum>` to `gradle/wrapper/gradle-wrapper.properties` and set `validateDistributionUrl=true`. Get the checksum from the official Gradle release page, not from memory.
- Confirm no `android:debuggable="true"` in release (AGP default is false) and that `keystore.properties` never enters git (already in `.gitignore`; also add a secret scan, see TEST-09).

**Prompt:**
```text
TASK SEC-10. (1) Add the Log assumenosideeffects rule to proguard-rules.pro. (2) Grep for places that display e.message or it.message in ui/ and replace each with a fixed friendly string, keeping the raw message only in Log.w. Show me the list first.
(3) Do NOT touch the POI/Hilt keep rules in this task.
```

---

## 3. Reliability and data-integrity tasks

### SYNC-01 — One bad bill must not block all syncing  `[Verified]` · Severity: HIGH · Branch `sync-01-per-bill`

**Problem (all in `data/service/TwoPhoneSyncService.kt`).**
1. The Owner ingests up to 1000 bills inside a **single** `db.withTransaction`. Any `require(...)` that fails (unknown product, bad totals, conflicting replay, duplicate line id) rolls back **everything**. The Billing phone then re-sends the same oldest-500 bills (`getPendingSyncSales().take(500)`) forever, so one poisoned bill blocks every later bill.
2. The "replay" check compares the stored JSON text with the incoming JSON text (`original.payloadJson == billObj.toString()`). The client builds that JSON afresh on every attempt, and it contains the customer's *current* name and phone. The client refreshes customers from the Owner catalog **before** delivering. Sequence that deadlocks: bill delivered and committed on the Owner → acknowledgement lost (Wi-Fi drop) → customer's name/phone edited → next retry JSON differs → Owner answers "Conflicting replay of bill identity" for ever.
3. Rejected bills have no visible state; the user only sees a generic "Sync request rejected" on the Owner side.

**Fix approach.**
- **Per-bill transactions.** Extract the loop body into `private suspend fun ingestBill(bill: JSONObject): IngestResult` and call `db.withTransaction { ingestBill(bill) }` once per bill inside a `try/catch`. Return a result for every bill.
- **Response format (backward-compatible, versioned).** Keep `acknowledgedIds` (accepted + duplicates) and add `results: [{id, status: ACCEPTED|DUPLICATE|REJECTED, reason}]` and `protocolVersion: 2`. Both phones should be updated together; the client should refuse to talk to an older server with a clear message rather than guess.
- **Canonical comparison.** Compare a canonical form: parse both, remove the `customer` object (display data), and compare with keys sorted. Better: compute `SHA-256` of the financial fields only (id, receiptNumber, customerId, totals, payment method, timestamp, and the sorted lines' id/product/qty/price/discount/net), stored in the `sync_events` row at ingestion.
- **Client handling.** `ACCEPTED`/`DUPLICATE` → `SYNCED`. `REJECTED` → set `syncStatus = "SYNC_REJECTED"` (it is a plain String column, so **no schema migration is needed**) and write a `sync_events` row with the reason. Show a "N bills need attention" banner on the Billing phone with a list (receipt number, amount, reason) and a **Retry** button that sets the status back to `PENDING_SYNC`.
- **Sanity checks.** Reject bills whose `timestamp` is more than one day in the future, and bills with an empty `lines` array.
- Do not silently drop or auto-edit a rejected bill: it is a real sale. A human must look at it.

**Starting-point code:**
```kotlin
private fun canon(o: JSONObject): String {
    val copy = JSONObject(o.toString()).apply { remove("customer") }
    val keys = copy.keys().asSequence().sorted().toList()
    return keys.joinToString(",", "{", "}") { k -> "\"$k\":${copy.get(k)}" }
}
// replay check:
require(original?.eventType == "SYNC_BILL_INGESTION" &&
        canon(JSONObject(original.payloadJson)) == canon(billObj)) { "Conflicting replay of bill identity" }
```
(Nested `lines` arrays keep their order, which is stable because the client builds them from the same query order; the hash approach above is more robust if you want to remove that assumption.)

**Prompt (two sessions):**
```text
TASK SYNC-01a (small, safe first step). File TwoPhoneSyncService.kt. Only change the replay comparison:
compare canon(stored) == canon(incoming) where canon() removes the "customer" key and sorts keys. Do not change anything else.
Add a unit test (extract canon into a top-level internal function) showing two bills that differ only in customer name/phone are equal, and two bills that differ in netTotalPaise are not.

TASK SYNC-01b. Refactor the ingestion loop: move the body into `ingestBill(bill): IngestResult` (sealed: Accepted, Duplicate, Rejected(reason)).
Call it inside its own db.withTransaction per bill, catching IllegalArgumentException / IllegalStateException => Rejected(message) (the transaction for that bill rolls back; others continue).
Return JSON {status:"SUCCESS", protocolVersion:2, acknowledgedIds:[accepted+duplicate ids], results:[{id,status,reason}]}.
Client: for each result set sale syncStatus to SYNCED or SYNC_REJECTED and insert a SyncEventEntity(eventType="SYNC_BILL_REJECTED", entityId=saleId, payloadJson=reason).
The old "acknowledged set must equal pending set" check must become: every pending id appears exactly once in results.
Add a banner on the Billing screen showing the count of SYNC_REJECTED sales with a Retry action (sets back to PENDING_SYNC). Use only existing DAO methods plus one new query for SYNC_REJECTED sales.
```

**Done when.** A batch of 3 bills where the middle one is invalid results in 2 accepted, 1 rejected-with-reason, and the next sync does not resend the accepted two. Test **TEST-04** covers this.

---

### SYNC-02 — Keep hosting alive and survive IP changes  `[Likely]` · Severity: MEDIUM-HIGH · Branch `sync-02-foreground-service`

**Problem.** The Owner server runs in the singleton's coroutine scope; there is no `Service` in the manifest. Android may stop an unattended app process or throttle it when the screen is off, so the Billing phone can fail to sync at random. The Billing phone also stores the Owner's IP address (`PAIRED_OWNER_HOST`); routers hand out new addresses.

**Fix approach.**
1. **Router fix first (zero code):** give the Owner phone a DHCP reservation so its IP never changes. Note this in the owner guide.
2. **Foreground service:** add `OwnerSyncService` that calls `TwoPhoneSyncService.startOwnerServer()` and shows an ongoing notification "Sync server running". Start it only from a user action (the existing "Host" button) — Android restricts starting foreground services from the background.
3. **Manifest additions:**
   ```xml
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   <service android:name=".service.OwnerSyncService" android:exported="false"
            android:foregroundServiceType="connectedDevice" />
   ```
   Request `POST_NOTIFICATIONS` at runtime on Android 13+ (the service still runs if denied, but the notification may be hidden).
4. **Optional discovery (later):** register the server with `NsdManager` (`_aliedu._tcp`) so the Billing phone can find the Owner by name. Pairing must still verify the certificate fingerprint — discovery is only a convenience, never trust.
5. **Billing client:** retry with backoff (for example on app foreground and every 2 minutes while there are pending bills); a durable WorkManager job is a later improvement (new dependency, so ask first).

**Prompt:**
```text
TASK SYNC-02. Add service/OwnerSyncService.kt (Service, startForeground with an ongoing low-priority notification channel "sync", foregroundServiceType connectedDevice) that calls TwoPhoneSyncService.startOwnerServer() in onStartCommand and stopOwnerServer() in onDestroy.
Register it in the manifest with the three permissions. Wire the existing "Host / Stop hosting" buttons in SettingsScreen to start/stop this service instead of calling the singleton directly. Do not change the protocol or the request handlers.
Show me the manifest diff before building.
```

**Manual check.** Start hosting, lock the Owner phone, wait 10 minutes, sync from the Billing phone → succeeds.

---

### DB-01 — Add indices and a read-only integrity audit (migration 8 → 9)  `[Verified]` · Severity: HIGH (grows with time) · Branch `db-01-indices`

**Problem.** The whole database has one index (unique barcode). There are no foreign keys. `AppRepository.allSales` and `allSaleLines` expose **entire tables** as live `Flow`s, so every insert re-emits the full history to every collector. The DAOs also contain unfiltered queries such as `SELECT * FROM sale_lines`. It is fine with a few hundred bills and becomes slow after years.

**Fix approach.**
1. **Indices** (names must match Room's convention `index_<table>_<col1>_<col2>` or Room will refuse the schema):

| Table | Index columns | Used by |
|---|---|---|
| `sales` | `timestamp` | date-range reports, history |
| `sales` | `customerId` | per-customer history |
| `sales` | `syncStatus` | pending/rejected sync lists |
| `sales` | `receiptNumber` | receipt lookup (see unique note) |
| `sale_lines` | `saleId` | lines for a bill |
| `sale_lines` | `productId` | product profit/trends |
| `stock_movements` | `productId, timestamp` | product history |
| `account_payments` | `accountType, accountId, timestamp` | account statements |
| `purchase_lines` | `purchaseId` | purchase detail |
| `sync_events` | `entityId` | `getEventByEntityId` (used on every ingest) |

   Also open `Daos.kt` and index any other column used in `WHERE`/`JOIN`/`ORDER BY` on a table that grows (expenses by `timestamp`, `isVoided`).
2. **Unique `receiptNumber`?** Only after running `SELECT receiptNumber, COUNT(*) FROM sales GROUP BY 1 HAVING COUNT(*) > 1`. If duplicates exist the migration would fail and brick the app on update. Start with a **non-unique** index; make it unique later only if the data is clean.
3. **Migration + schema:** add `@Entity(indices = [...])`, bump `version = 9`, write `MIGRATION_8_9` with `CREATE INDEX IF NOT EXISTS`, register it in `DatabaseModule`, rebuild so Room exports `9.json`.
4. **Integrity audit (read-only, no repair).** Add a developer-only function that runs these queries and shows counts; it must **never** modify data:
   ```sql
   SELECT COUNT(*) FROM sale_lines WHERE saleId NOT IN (SELECT id FROM sales);
   SELECT COUNT(*) FROM sale_lines WHERE productId IS NOT NULL AND productId NOT IN (SELECT id FROM products);
   SELECT COUNT(*) FROM purchase_lines WHERE purchaseId NOT IN (SELECT id FROM purchases);
   SELECT COUNT(*) FROM stock_movements WHERE productId NOT IN (SELECT id FROM products);
   SELECT COUNT(*) FROM products WHERE currentSellableStock < 0;
   ```
   Your own docs gate #9 ("previously corrupted production transactions need a read-only reconciliation report") is this. Foreign keys can be considered later; adding them needs SQLite table rebuilds and is much riskier than indices.

**Migration pitfalls the assistant must handle:** exact index names; `exportSchema` output committed; the migration test must open a **version 8** database (use your existing schema assets pattern in `MigrationSqlTest` / `AuditDatabaseTest` and copy `9.json` the same way).

**Prompt (three sessions):**
```text
TASK DB-01a. Add Room @Index declarations (non-unique) to the entities in the plan table. Bump AppDatabase version to 9, add MIGRATION_8_9 using CREATE INDEX IF NOT EXISTS with Room's exact naming, register it in di/DatabaseModule.kt addMigrations(...).
Do not touch any other migration. Build so app/schemas/.../9.json is generated, and show me `git status` listing it.
Add a migration test in androidTest that creates a v8 DB from schema 8.json, inserts sample rows, migrates to 9, and checks all rows survive and the indices exist (PRAGMA index_list).

TASK DB-01b. Add a read-only `IntegrityAudit` class running the 5 SQL queries in the plan through db.openHelper.readableDatabase and returning counts. No UI yet; unit/instrumented test with seeded bad rows. It must contain no UPDATE/DELETE/INSERT.

TASK DB-01c. Run the migration rehearsal MANUALLY (owner): restore a copy of a real backup into an emulator on version 1.0.15, install the new build over it (adb install -r), confirm data and totals are unchanged.
```

---

### PERF-01 — Stop loading whole tables  `[Verified]` · Severity: MEDIUM · Branch `perf-01-bounded-queries`

Find every consumer first: `grep -rn "allSales\|allSaleLines\|getAllSales\|getAllSaleLines" app/src/main`. For each screen decide what it truly needs, then:
- Reports and Profit: use `getSalesByDateRange` / `getServiceLinesInRange` (already exist) plus SQL aggregates (`SUM`, `COUNT`, `GROUP BY` date) in the DAO, instead of summing lists in Compose.
- History lists: `LIMIT 100` with a "Load more" button (`OFFSET`); no new library required.
- Home: only the numbers it displays (today's total, pending count), not lists.
- Do **one screen per session**; keep the old flow until the new one is verified, then remove it.

```text
TASK PERF-01 (repeat per screen). Screen: <name>. Read it and list which fields it uses from allSales/allSaleLines.
Add DAO queries returning only those values for the selected date range. Switch the screen. Keep results identical: write a unit/instrumented test that seeds 200 sales and asserts old calculation == new calculation for the same range.
```

### PERF-02 — Printing and Excel import  `[Verified/Likely]` · Severity: MEDIUM · Branch `perf-02-print-import`

- `ReceiptService` calls `runBlocking { createReceiptPdf(...) }` inside `PrintDocumentAdapter.onWrite`, which runs on the main thread by default. Build the PDF on a background thread and call `callback.onWriteFinished` from there; honour `CancellationSignal`.
- `WorkbookFactory.create(stream)` loads an entire workbook into memory. Add a maximum file size (for example 10 MB) and row limit (for example 20,000) with a friendly message *before* parsing. Test with the largest realistic stock workbook on the weakest phone.

### CODE-01 — Swallowed errors and `!!`  · Severity: LOW-MEDIUM · Branch `code-01-errors`
About 13 empty `catch` blocks (`AttachmentManager`, `SettingsScreen:121`, `AliViewModel:936/1410`, `CashTrackerScreen:1944`, `StockScreen:1179`, `BackupRestoreService:268`, several in `TwoPhoneSyncService`, `CashTrackerExcelService`) and 53 uses of `!!`. Rule: an empty catch is allowed only for cleanup (closing a socket). Everywhere else log with `Log.w` and, if the user's action failed, report it through `operationError`. Start with `BackupRestoreService:268` because it concerns backups. One file per session.

### CODE-02 — One reusable owner-only guard  · Severity: LOW · Branch `code-02-owner-guard`
`ui/navigation/NavHost.kt` repeats the same "if role != OWNER show denied screen" block about 12 times. Replace with one composable and keep behaviour identical (including the `"UNKNOWN"` fail-closed state):
```kotlin
@Composable
fun OwnerOnly(vm: AliViewModel, title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val role by vm.deviceRole.collectAsState()
    if (role.uppercase() == "OWNER") content() else OwnerPermissionDeniedScreen(title, onBack)
}
```
Then migrate **three routes per session**, run the navigation tests, repeat. The repository already calls `requireOwner()` inside sensitive operations — keep that second layer.

### CODE-03 — Split the giant files gradually  · Severity: LOW (long term) · Branch per slice
`AppRepository` (1.8k lines), `AliViewModel` (1.6k), `CashTrackerScreen` (2.2k), `PigmySavingsScreen` (1.8k). Do **not** rewrite. Use extraction: create `CashViewModel` containing only cash-related functions, point the cash screen at it with `hiltViewModel<CashViewModel>()`, delete the old functions only after tests pass. Repeat for Pigmy, Stock, Billing, Accounts. Never combine with a feature or a security change.

### BUILD-01 — Dependency and toolchain upgrades  · Severity: LOW-MEDIUM · Branch per upgrade
Currently AGP 8.7.3 builds against SDK 36 with `android.suppressUnsupportedCompileSdk=36`, Compose BOM `2025.01.01`, Room 2.6.1, Hilt 2.51.1, biometric `1.2.0-alpha05` (an alpha), accompanist-systemuicontroller (deprecated; you already use `enableEdgeToEdge`).
Upgrade **one thing per release**, in this order, running the full test suite after each: (1) AGP + Gradle (check the AGP release notes for the first version that officially supports API 36, then delete the suppress flag); (2) Kotlin/KSP; (3) Compose BOM; (4) Room; (5) Hilt; (6) biometric to a stable release if one meets your needs; (7) remove accompanist. Do not let the assistant pick versions from memory — you look them up on the official release pages and give it the exact numbers.

### DOC-01 — Fix documentation drift  · Severity: LOW
`README.md` says version 1.0.12 and that some schema JSONs are "placeholder-sized"; the build is 1.0.15 and all eight schema files are 26–50 KB. Update the README, and add a short `docs/SECURITY.md` describing roles, the lock, the recovery process, and the backup passphrase rule.

---

## 4. New features

### FEAT-01 — Multi-select and bulk **void** in Billing history and Cash In/Out  · Branch `feat-01-bulk-void`

**Why "void", not "delete".** Your repository already voids rather than deletes: `voidSale(saleId, stockCondition, voidReason)` restores stock, records a refund payout, reverses customer credit and marks the sale `isVoided` with an audit trail; `voidExpense(expenseId, reason)` blocks cancelling an entry that has active refunds or would make a savings balance negative. A hard delete would leave stock, customer dues and cash totals wrong and would create gaps in receipt numbers. So bulk "delete" = **bulk void**, and it reuses the existing single-item functions so accounting rules stay in one place. True deletion stays only for drafts (held bills).

**Behaviour to implement.**
1. **Selection mode.** Long-press a row to start; tap rows to toggle; top bar shows "N selected", **Select all (visible)** and **Cancel**. Back button leaves selection mode. Selection survives rotation (store ids in the ViewModel, not in a plain `remember`).
2. **Owner-only.** Hide the action on a Billing-role phone. The repository's `requireOwner()` stays as the second layer.
3. **Step-up authentication** (SEC-03) before the confirmation dialog.
4. **Confirmation dialog** shows: count, total amount, a **required reason** text (non-empty), and for bills a **stock condition** choice (Sellable / Damaged / No stock change) that applies to the whole batch.
5. **Process one item at a time**, each in its own transaction (do **not** wrap the whole batch in one transaction — a single ineligible item would block everything). Collect results.
6. **Result sheet:** "Voided 7, skipped 2" with each skipped item's reason (for example "has active refunds", "would make savings negative", "already voided"). Never swallow a failure silently.
7. **Ordering rules for cash entries:** void refunds (`isExpenseRefund = true`) **before** their parent expenses (a parent with active refunds is refused); void savings **deposits newest-first** so the "balance can't go negative" check is evaluated in a safe order; withdrawals can go in any order.
8. Voided items stay visible, marked, and are excluded from totals (this already works for single voids).
9. **Held bills** (`HeldBillsDialog`): multi-select with real deletion is fine (they are drafts, not records).

**Decisions you must make before coding (the assistant must not decide these):**
- **Refund policy.** The existing `voidSale` always records a refund payout of `min(amountReceived, netTotal)` as a negative payment. That is right when cash really goes back to a customer. It is **wrong for test bills or duplicate entries** where no money moved: the books would show a refund that never happened. Decide whether you want a second explicit option, "Entry mistake — no refund", which is an accounting-policy change and needs its own tests.
- **Limit per batch** (suggest 200) to keep the UI responsive.
- **Billing-phone copies.** Sync only flows Billing → Owner. A bill voided on the Owner will still look active on the Billing phone until a later feature sends voids back. For now, show that limitation in the owner guide.

**Starting-point code (ViewModel):**
```kotlin
data class BulkResult(val done: Int, val skipped: List<Pair<String, String>>)  // id/receipt -> reason

fun voidSales(ids: List<String>, stockCondition: String, reason: String, onResult: (BulkResult) -> Unit) {
    require(reason.isNotBlank() && ids.size <= 200)
    viewModelScope.launch(operationHandler) {
        var done = 0
        val skipped = mutableListOf<Pair<String, String>>()
        for (id in ids) {
            try { repository.voidSale(id, stockCondition, reason); done++ }
            catch (e: IllegalStateException) { skipped += id to (e.message ?: "Not allowed") }
            catch (e: IllegalArgumentException) { skipped += id to (e.message ?: "Not found") }
        }
        onResult(BulkResult(done, skipped))
    }
}

fun voidExpenses(ids: List<String>, reason: String, onResult: (BulkResult) -> Unit) {
    viewModelScope.launch(operationHandler) {
        val entries = ids.mapNotNull { repository.getExpense(it) }          // confirm the real getter name
        val ordered = entries.sortedWith(compareByDescending<ShopExpenseEntity> { it.isExpenseRefund }
                                         .thenByDescending { it.timestamp })
        var done = 0; val skipped = mutableListOf<Pair<String, String>>()
        for (e in ordered) try { repository.voidExpense(e.id, reason); done++ }
                           catch (x: IllegalStateException) { skipped += e.id to (x.message ?: "Not allowed") }
        onResult(BulkResult(done, skipped))
    }
}
```
(The assistant must check the real name of the function that loads one expense; do not let it invent one.)

**Prompt (four sessions):**
```text
TASK FEAT-01a (ViewModel only). In AliViewModel add voidSales(...) and voidExpenses(...) as in the plan; they must call the existing repository.voidSale / repository.voidExpense one item at a time and never wrap the batch in a transaction.
Write unit tests with a fake repository: 5 ids where #3 throws IllegalStateException => done=4, skipped has 1 entry with its message; refunds are voided before parents; empty reason rejected.

TASK FEAT-01b (Billing history UI). In ui/billing/BillHistoryScreen.kt add selection mode (long-press, checkboxes, count, select-all, cancel), owner-only, using selection state held in the ViewModel. Add the confirmation dialog (count, total, required reason, stock condition radio) and the result sheet. Call SEC-03's stepUp.require before the dialog. Do not change how single voids work.

TASK FEAT-01c (Cash In/Out UI). Same for ui/reports/CashTrackerScreen.kt list of entries.

TASK FEAT-01d (held bills). In HeldBillsDialog add multi-select with real deletion (confirm dialog). Add a DAO deleteByIds(ids) query.
After each task: Compose UI test for selection, confirm, and result display.
```

**Done when.** Selecting 10 bills (2 already voided) voids 8, reports 2 skipped with reasons, stock/customer dues/cash totals equal the totals after voiding the same 8 bills one by one (this equivalence is the key automated test).

---

### FEAT-02 — Landing page and theme-correct logo  · Branch `feat-02-landing-logo`

**What I found.**
- Two logo files exist: `ali_logo_dark.png` is cream letters + gold (made for dark backgrounds), 1557×845 px, 664 KB. `ali_logo_light.png` is black letters + gold (made for light backgrounds) but only **232×122 px** — it will look blurry when shown at 190 dp.
- `HomeScreen`, `BillingScreen` and `BarcodeScannerScreen` already pick the logo by theme. **`UnlockScreen.kt:79` and `SetupScreen.kt:77` always use the dark (cream) logo, so in light mode the logo is nearly invisible on the light background.** This is the exact problem you described.
- `MainActivity` draws nothing until the startup settings load (`if (startupSettings == null) return@setContent`), so the first frame is blank.
- The theme is `android:Theme.Material.NoActionBar` with no splash configuration.

**Fix 1 — one logo composable, correct in both themes.** Create `ui/components/AliLogo.kt` and use it everywhere (Unlock, Setup, Home, Billing, Scanner). It reads the app's own theme setting (`viewModel.isDarkTheme`), not a `drawable-night` folder, because your app has its own theme toggle.

```kotlin
@Composable
fun AliLogo(isDark: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (isDark) R.drawable.ali_logo_dark else R.drawable.ali_logo_light),
        contentDescription = "Ali Educational logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
```

**Fix 2 — a sharp light-mode logo.** Replace the 232×122 light file with a full-resolution version generated from the dark logo by recolouring only the cream letters to near-black and keeping the gold (alpha untouched). A tested script is below; the generated `ali_logo_light_hd.png` (1557×845, ~200 KB) is delivered alongside this document. Save it as `app/src/main/res/drawable/ali_logo_light.png` (overwrite) after you look at it on a real light background and approve the dark tone (`DARK = (26, 26, 26)` in the script). Optionally also downscale the 664 KB dark logo to about 1000 px wide or convert both to lossless WebP to reduce APK size.

```python
# make_light_logo.py  —  python3 make_light_logo.py ali_logo_dark.png ali_logo_light_hd.png   (needs Pillow)
import sys
from PIL import Image
SRC, DST = sys.argv[1], sys.argv[2]
DARK = (26, 26, 26); GOLD = (220, 174, 65)
im = Image.open(SRC).convert("RGBA"); px = im.load(); w, h = im.size
for y in range(h):
    for x in range(w):
        r, g, b, a = px[x, y]
        if a == 0: continue
        mx, mn = max(r, g, b), min(r, g, b)
        sat = (mx - mn) / mx if mx else 0.0          # cream ~0.07, gold ~0.70
        t = min(1.0, max(0.0, (sat - 0.15) / 0.30))  # 0 = cream, 1 = gold
        px[x, y] = tuple(round(DARK[i] + (GOLD[i] - DARK[i]) * t) for i in range(3)) + (a,)
im.save(DST, optimize=True)
```

**Fix 3 — system splash with no blank frame.** Add the AndroidX `core-splashscreen` library (**a new dependency; approve it explicitly**), define `Theme.AliEducational.Starting` with `postSplashScreenTheme`, call `installSplashScreen()` before `super.onCreate`, and hold it with `setKeepOnScreenCondition { startupSettings == null }`. The system splash must stay simple (background colour + the square launcher icon) because the wide logo does not fit the splash icon area; the wide logo belongs in the in-app landing screen. Splash colours follow the *system* theme while the app logo follows the *app* theme, so a one-frame mismatch is possible if the two differ; that is acceptable.

**Fix 4 — landing flow (my recommendation).**
- **Lock enabled:** do **not** add a second screen. The unlock screen *is* the landing page: logo animates in, then the fingerprint prompt appears automatically.
- **Lock disabled:** show a short (about 1 s) landing animation, then Home.
- Cold start only. Keep a `landingShown` flag in the ViewModel (not persisted), so rotation, lock/unlock cycles and returning from the background never replay it. A tap skips it. If the system animation scale is 0 ("remove animations"), skip it entirely.

**Landing ideas (pick one; all are cheap in Compose):**

| # | Idea | What the user sees | Notes |
|---|---|---|---|
| 1 | **Fade-up** (safest) | Logo fades in and scales 0.92 → 1.0 over 600 ms, shop name slides up 100 ms later | Almost no risk, works for both themes |
| 2 | **Ledger line** | Logo appears, then a thin gold line draws beneath it left to right and the shop name settles under it | Fits a shop/accounts app; very simple `Canvas` line |
| 3 | **Barcode sweep** (best fit for your app) | A thin gold scan line sweeps once across the logo, like scanning a barcode, then the logo "locks in" | Ties to your scanner feature; clip a gradient to the logo |
| 4 | **Receipt print** | Logo slides out of the top edge of a receipt-paper strip as if printing | Memorable; a bit more animation code |
| 5 | **Welcome strip** (owner only) | After the logo, a one-line greeting with today's sales total fades in | Shows financial data, so only after unlock and only on the Owner role |

My pick: **#3 for the lock-disabled landing** and **#1 on the unlock screen** (so unlocking stays fast). Keep total time under 1.2 s; the shop owner opens this app dozens of times a day.

**Prompt (four sessions):**
```text
TASK FEAT-02a. Create ui/components/AliLogo.kt (snippet in the plan). Replace every direct use of R.drawable.ali_logo_dark / ali_logo_light in UnlockScreen, SetupScreen, HomeScreen, BillingScreen and BarcodeScannerScreen with AliLogo(isDark = ...), using the same isDarkTheme value each screen already has. Add a Compose screenshot-free test: renders in light and dark and asserts the node with contentDescription "Ali Educational logo" exists.

TASK FEAT-02b. (Owner step) Replace res/drawable/ali_logo_light.png with the generated HD file. Rebuild; open Unlock, Setup, Home in LIGHT mode and confirm the logo is dark and sharp.

TASK FEAT-02c. Add androidx.core:core-splashscreen at the exact version I give you: <VERSION>. Create Theme.AliEducational.Starting, set it on MainActivity in the manifest, call installSplashScreen() before super.onCreate, setKeepOnScreenCondition { startupSettings == null }. Do not change colours or icons beyond windowSplashScreenBackground in values and values-night.

TASK FEAT-02d. Implement ui/landing/AliLandingScreen.kt with idea #<N> from the plan: 1.0 s maximum, skip on tap, skip if animation scale is 0, shown once per cold start (flag in AliViewModel, not persisted), only when lock is disabled and not first launch (first launch goes to Setup as today). When lock is enabled, animate the logo on UnlockScreen with idea #1 instead of adding a screen.
```

**Done when.** In light mode the logo is dark and crisp on Unlock, Setup and Home; there is no blank first frame; rotating never replays the animation; the unlock prompt still appears automatically.

---

## 5. Rigorous testing plan

**What exists today (verified):** 58 JVM tests and 42 emulator tests, covering costing, receipts, backup packages, reconciliation, migration SQL, pigmy, import/export, the widget and an audit database suite. **What is missing:** zero UI/ViewModel tests, no security tests, no sync fault-injection tests, no property-based money tests, no performance tests, no CI, no static-analysis gate beyond lint. Your own release notes say passing tests are not release acceptance; this section closes those gaps.

### 5.1 Principles

1. **Test first.** For every task, write the test, run it, see it **fail**, then make the change and see it pass. A test that never failed proves nothing.
2. **Prove the test can fail (mutation check).** After the fix, temporarily break the new code (flip a condition) and confirm the test fails. Ask the assistant to do this and paste both outputs.
3. **Deterministic.** Inject a clock and a seeded random generator; never depend on the real time, network or ordering. Print the random seed on failure so a failure can be replayed.
4. **Every real bug becomes a permanent regression test.**
5. **Money is the priority.** Anything touching stock, customer/supplier balances, cash, discounts or voids gets the strictest tests.
6. **Never test on live shop data.** Use debug builds, emulators and copies of backups.

### 5.2 Test layers

| Layer | Tooling | Runs | What it covers |
|---|---|---|---|
| JVM unit | JUnit (exists) | every PR, seconds | Calculators, allocation, canonicalisation, crypto, throttling, view-model logic with fakes |
| Instrumented database | AndroidX test + in-memory/real Room on emulator | every PR | Migrations, DAOs, transactions, repository money rules |
| Compose UI | `ui-test-junit4` (exists) | every PR | Screens, selection mode, lock screen, rotation |
| Integration (sync) | In-process TLS server on loopback + real Room | every PR | Two-phone protocol and failure modes |
| Static analysis | Lint (exists), optionally detekt | every PR | Empty catches, `!!`, blocking calls, hardcoded text |
| Security | Targeted instrumented + unit tests | every PR | Lock, step-up, crypto, exported components, FileProvider |
| Performance / soak | Seeded large dataset, monkey | nightly / before release | Query speed, memory, ANRs |
| Manual device | Checklist (5.9) | before release | Real hardware, printer, camera, Wi-Fi |

**Dependencies you may need (approve each explicitly; check which already exist in `app/build.gradle.kts` first):** `kotlinx-coroutines-test`, `androidx.room:room-testing` (for `MigrationTestHelper`), `androidx.test:runner/rules`, optionally `app.cash.turbine` (Flow testing) and LeakCanary (debug only). Avoid mocking libraries; hand-written fakes are clearer and the assistant makes fewer mistakes with them.

### TEST-01 — Test foundations  · Branch `test-01-foundations`

- Introduce a tiny `Clock` interface (`fun nowMillis(): Long`) with a system implementation and a fake, and inject it where code currently calls `System.currentTimeMillis()` **in the units under test only** (PIN throttling, step-up auth, sync timestamp checks). Do not refactor the whole app.
- Provide fakes for `BiometricAuthManager`, the DAO interfaces used by the view model, and an in-memory Room builder for instrumented tests (`Room.inMemoryDatabaseBuilder(...)`).
- Add a `TestData` builder (products, customers, sales, expenses) used by all tests so fixtures are consistent.

```text
TASK TEST-01. Add util/Clock.kt (interface + SystemClock object) and test/ fakes (FakeClock, FakeBiometricAuthManager). Add androidTest TestData.kt with builders for ProductEntity, CustomerEntity, SaleEntity+SaleLineEntity, ShopExpenseEntity using real entity constructors (open Entities.kt first and use only existing fields).
No production behaviour change. Show which existing tests (if any) you reused.
```

### TEST-02 — Money invariants (property-style)  · Branch `test-02-money-invariants`

Randomised sequences with a fixed seed. After **every** step, assert these invariants (the assistant must read the repository first and confirm the real definitions; if an invariant below does not match the app's rules, stop and ask):

| # | Invariant |
|---|---|
| 1 | `product.currentSellableStock` equals the sum of its stock movements (opening + purchases + returns − sales ± adjustments) |
| 2 | `sale.netTotal = Σ line.net`, `sale.gross = Σ line.gross`, `net = gross − discount` |
| 3 | Customer balance equals credit still owed on non-voided sales minus payments allocated (confirm the repository's definition) |
| 4 | A voided sale cannot be voided again; voiding is all-or-nothing |
| 5 | No arithmetic overflow: huge amounts are rejected, never wrapped (the repo uses `addExact`) |
| 6 | Cash register totals never include voided entries |
| 7 | **Equivalence:** voiding N bills one by one, or through the bulk function, ends in the identical database state |
| 8 | Pigmy balances never go negative |

Operations to randomise: sale (cash/credit/partial, discounts, packs), receive stock, repayment, refund, void sale/expense, cash in/out, pigmy deposit/withdrawal.

```text
TASK TEST-02. Create androidTest/.../MoneyInvariantTest.kt. Use a seeded java.util.Random (seed from a constant plus an optional system property -Pseed=...). Loop N iterations (default 100; nightly 2000) choosing a random operation, applying it through AppRepository, then asserting invariants 1-6 and 8.
On failure the assertion message MUST include the seed, the iteration number and the operation history. Do not modify production code; if an invariant fails on real code, report it as a finding — do not change the invariant to make it pass.
```
A failing invariant is a **finding about your app**, which is the whole point. Record it in `docs/verification/` with the seed.

### TEST-03 — Migration chain  · Branch `test-03-migrations`

- For **each** schema file 1…8 (and 9 after DB-01), create a database at that version, insert realistic rows, migrate to the latest, verify: all rows preserved, `PRAGMA integrity_check = ok`, Room opens it without schema validation errors, indices exist.
- Add a test that opens your largest real **anonymised** backup copy (if you provide one) and migrates it.
- Confirm there is **no** `fallbackToDestructiveMigration` anywhere (`grep -rn fallbackToDestructive app/src` must return nothing; currently it does not).
- Follow the pattern of the existing `MigrationSqlTest` and `AuditDatabaseTest`; do not invent a second schema-loading mechanism.

### TEST-04 — Two-phone sync fault injection  · Branch `test-04-sync-faults`

Run the Owner server in-process on loopback with a real pinned certificate (or a test certificate with the same pinning code) and in-memory databases for both sides. Scenarios, each its own test:

1. Normal delivery of 1, 10, 500 bills; stock and balances equal on both sides afterwards.
2. **Duplicate delivery** of the same batch → no double posting.
3. **Lost acknowledgement:** server commits, response is dropped; customer is renamed; client retries → bill is accepted as a duplicate (this is the SYNC-01 regression).
4. One invalid bill among three → two accepted, one rejected with a reason; the next sync resends nothing accepted.
5. Unknown product, negative quantity, mismatched totals, duplicate line id, future timestamp, empty lines.
6. Oversell: stock goes negative and the sale is flagged `unresolvedStockDetails`.
7. Wrong token → 401 and no state change; wrong certificate → client refuses; malformed headers; oversize header; oversize body; slow client (10 s timeout) does not block other clients.
8. Client process killed mid-request, then restarted → no duplicate, no lost bill.
9. Two Billing devices deliver at the same time → consistent totals, no lost update.
10. Rate limiter and connection cap (SEC-09).
11. Throughput: 1000 bills complete in an acceptable time (record the number; set the limit after the first measurement).

### TEST-05 — Security tests  · Branch `test-05-security`

| Test | Asserts |
|---|---|
| Lock screen UI | No node with text "Enter Shop Directly"; with credential unavailable the app does not reach Home |
| Rotation | `ActivityScenario.recreate()` while unlocked does not show the unlock screen; after the timeout it does |
| Step-up | Fresh window skips the prompt; expired window prompts; locking resets |
| PIN throttle | Failures 1-5 → 1 min block; next round → 5 min; success resets; clock rollback does not shorten a block |
| SecretBox | Round trip; tampered ciphertext → null; legacy plaintext accepted once and re-sealed |
| BackupCrypto | Round trip for 0 B, 1 B, 1 MiB, 5 MiB; wrong passphrase → false; one flipped byte → false; truncation → false; reordered chunks → false |
| Zip safety | Entries with `../`, absolute paths, duplicate names, nested depth, and a decompression-bomb ratio are rejected (some already tested; extend) |
| FileProvider | A URI for `files/datastore/...` cannot be created (throws), while receipt/export URIs can |
| Exported components | Query `PackageManager` for this package's exported activities/receivers/providers; assert the set equals `{MainActivity, AliSlimBarWidgetProvider}` |
| Intent fuzz | Send 200 random intents/extras to `MainActivity` while locked; no crash, no navigation to a protected screen |
| Cleartext | Parse the merged manifest: `usesCleartextTraffic` is false |
| No secrets in repo | gitleaks (TEST-09) |

### TEST-06 — Backup and restore rigor  · Branch `test-06-backup`

Full round trip: seed every table (all 20 entities) → backup → restore into a **fresh** database → compare row by row. Also: truncated archive; corrupted checksum; wrong version; missing attachment referenced by a record (must refuse a "complete" backup); simulated insufficient storage; process killed during backup (previous valid backup untouched); restore failure mid-way (rolls back, safety snapshot kept); restore on a different device (Keystore-bound secrets dropped, app asks to re-pair); encrypted and legacy-unencrypted archives both restore; wrong passphrase never partially restores.

### TEST-07 — UI tests for critical flows  · Branch per flow, `test-07-ui-<flow>`

Billing happy path (add by search, add by barcode with a fake analyzer, pack vs piece, cash tender and change, receipt opens); credit sale requires a customer; discount edge values (0, exact total, over total); held bill hold/resume/delete; void flow and **bulk void** (FEAT-01); cash in/out add, edit, void; pigmy deposit/withdraw; Accounts repayment; settings role switch (needs PIN); widget deep links; **rotation in the middle of a half-built bill keeps the cart**; Extra-Large text size and light mode on the main screens (no clipped buttons); minimum touch target 48 dp on primary buttons. TalkBack is a manual pass (5.9).

### TEST-08 — Static analysis and lint  · Branch `test-08-static`

- Make `lintDebug` a gate: create a `lint-baseline.xml` from today's 84 warnings so **new** issues fail, and fix the baseline over time. Enable `HardcodedText`-style checks for Compose if you adopt detekt.
- Optional: **detekt** (a new Gradle plugin — approve explicitly) with rules for `EmptyCatchBlock`, `UnsafeCallOnNullableType` (the `!!`), `GlobalCoroutineUsage`, and a custom rule flagging `Double`/`Float` in money-named variables.
- Debug-only `StrictMode`: `detectAll()` with `penaltyLog()`, and `penaltyDeath()` for disk/network on the main thread in instrumented tests, to catch freezes such as the print adapter's `runBlocking`.

### TEST-09 — Supply chain and secrets  · Branch `test-09-supply-chain`

- **gitleaks** on every PR and once over full history (`git log --all -- '*.jks' 'keystore.properties' '*.keystore'` must be empty; your `.gitignore` already excludes them, but history may differ).
- **Gradle wrapper validation** action; plus `distributionSha256Sum` (SEC-10).
- **Dependency vulnerability scan** weekly (OSV-Scanner or the OWASP dependency-check plugin; both are additions you approve). Triage results by hand; a scanner finding does not always mean an exploitable path in a small offline-first app.
- Review `git diff` of `gradle/libs.versions.toml` on every dependency change.

### TEST-10 — Performance, soak and stability  · Branch `test-10-performance`

- **Large dataset generator** (androidTest helper): 50,000 sales, 150,000 sale lines, 10,000 stock movements, 5,000 expenses. Measure with `measureTimeMillis` and record in `docs/verification/`: Home first load, history first page, Profit for one month, customer statement, backup creation, import of a 20,000-row workbook. **Do not invent thresholds**: take a baseline measurement first (before DB-01/PERF-01), then set limits at about 1.5× the post-fix measurement so regressions are caught.
- **Memory:** import the largest workbook and watch for `OutOfMemoryError`; test on a low-RAM emulator profile.
- **Monkey test** on a *debug* build with throw-away data only: `adb shell monkey -p com.alieducational.app.debug --throttle 200 -v 20000`. (A monkey can tap destructive buttons — never run it on a phone with real records.) Any crash or ANR becomes a bug.
- LeakCanary in debug builds, optional (new dependency).

### 5.3 Coverage targets (aim for, measure first)

Add JaCoCo (a Gradle plugin; approve) and track: **≥ 85 % line coverage** for `domain/calculation` and `data/service`; **every branch** of money allocation, void, refund and repayment paths; no percentage target for Compose layout code. Review uncovered lines in void/refund/sync code by hand — these matter more than the headline number.

### 5.4 Continuous integration (GitHub Actions)

Save as `.github/workflows/ci.yml`. **Check each action's current major version on its GitHub page before relying on it**; the tags below are common ones but I did not verify the latest.

```yaml
name: CI
on:
  pull_request:
  push: { branches: [main] }
  schedule: [{ cron: "0 20 * * *" }]   # nightly property/soak run

jobs:
  unit-and-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: gradle/actions/wrapper-validation@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew testDebugUnitTest lintDebug --stacktrace
      - uses: actions/upload-artifact@v4
        if: always()
        with: { name: reports, path: app/build/reports }

  secrets:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: gitleaks/gitleaks-action@v2
        env: { GITHUB_TOKEN: "${{ secrets.GITHUB_TOKEN }}" }

  instrumented:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v4
      - name: Enable KVM
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules && sudo udevadm trigger --name-match=kvm
      - uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          arch: x86_64
          script: ./gradlew connectedDebugAndroidTest
```
Protect the `main` branch so merging requires these three jobs to pass. The signing keystore is never uploaded to CI; CI builds **debug** only.

### 5.5 How to make an AI assistant write *good* tests

```text
TEST AUTHORING RULES (append to the master rules for any test task)
- First write the test and run it against the CURRENT code. If the test is meant to catch a bug it must FAIL; paste the failure.
- Then apply the production change and show the same test PASSING.
- Then do a mutation check: temporarily invert one condition in the new code, show the test failing, and revert it.
- Assert exact values (paise, counts, ids), not "not null" or "no exception".
- Never use Thread.sleep; use fake clocks and runTest/advanceUntilIdle.
- Name tests as sentences: `voidingTwiceIsRejectedAndLeavesStockUnchanged`.
- Do not mock the class under test. Prefer small hand-written fakes.
- If production code must change to be testable, make the smallest possible change and tell me exactly what it was.
```

### 5.6 Reviewing AI-written changes (before merging every PR)

Fast coding assistants most often fail in these ways. Check each:

1. **Hallucinated APIs or function names** → the build fails, or `grep` shows the function doesn't exist. Never merge something that was not compiled.
2. **"Tests pass" without proof** → require pasted terminal output; re-run it yourself.
3. **Scope creep** → run `git diff --stat`. A task meant for 2 files that touches 12 is rejected.
4. **Weakened tests** → run `git diff -- '*Test*'` and look for deleted assertions, `@Ignore`, loosened expected values.
5. **Silent behaviour change** → read the diff for changed constants, removed `require(...)` checks, changed error messages that tests depend on.
6. **Money type slips** → search the diff for `Double`, `Float`, `toDouble`, `/ 100.0`.
7. **New dependencies or permissions** → `git diff -- '*.kts' '*.toml' '*Manifest*'`.
8. **Security regressions** → search the diff for `onUnlocked()`, `exported="true"`, `Log.` with tokens/phone numbers.
9. **Unrelated reformatting** → reject; it hides real changes.

### 5.7 Fixing a failing test properly

If the assistant "fixes" a failing test by editing the *test*, stop. A failing test is either (a) a real bug in the app, (b) an outdated test for a deliberately changed rule, or (c) a flaky test. Decide which with the owner; only (b) may change a test, and the reason goes in the commit message.

### 5.8 Release gates (extends the list already in `REPAIRS_2026-11-08.md`)

A build may reach the shop phones only when **all** are true:
- [ ] CI green: unit, lint (no new errors), instrumented, secret scan.
- [ ] Money-invariant test passed with the nightly iteration count and 3 different seeds.
- [ ] Migration chain test passed, including a copy of a **real** backup migrated on an emulator.
- [ ] Exact signed release APK upgraded in place over that copy (same package, same signature, higher `versionCode`); totals compared before/after (stock value, customer dues, supplier dues, cash, expenses).
- [ ] Backup → restore into a **separate** installation matches; an encrypted backup restores with the right passphrase and refuses the wrong one.
- [ ] Two-phone lifecycle on real devices: normal sync, Wi-Fi loss mid-delivery, app killed during delivery, rejected-bill banner, retry.
- [ ] Manual checklist (5.9) completed on the Owner phone and the Billing phone.
- [ ] Release notes written, including what was **not** tested.
- [ ] A fresh backup of the live phone taken and copied off the phone **before** installing. Never uninstall or clear data.

### 5.9 Manual device checklist

| Area | Check |
|---|---|
| Lock | Cold start prompts; wrong fingerprint stays locked; timeout relocks; rotation does not prompt; removing the device screen lock blocks entry; Recents thumbnail hides content |
| Step-up | Prompt before lock-off, restore, role change, export, void/bulk void; no second prompt within 60 s |
| Billing | Sale by search and by scan; pack vs piece; cash change; credit sale; held bills; receipt share; print on the real printer |
| Cash In/Out | Add, edit, void, bulk void with a refund-linked entry and a savings-linked entry (both must be skipped with a reason) |
| Bulk void | Select 10 bills incl. 2 already voided; totals before/after compared |
| Sync | Hosting with the Owner screen off for 10 min; hotspot vs router; Wi-Fi off/on mid-sync; airplane mode; battery saver; two Billing devices at once |
| Backup | Create encrypted backup, copy off phone, restore on a second device; wrong passphrase; low storage |
| Display | Light and dark; text size Small → Extra Large; logo crisp and correct in both themes; landing animation once per cold start |
| Accessibility | TalkBack reads buttons and totals; touch targets comfortable |
| Time | Change phone clock/time zone: lock timer and receipt dates still correct |
| Permissions | Deny camera → graceful message; deny notifications → hosting still works |
| Devices | Owner phone (S24 Ultra); Billing phone (your real one); an API 29 emulator for the biometric path; a low-RAM emulator for import |

---

## 6. Decisions only you can make (the assistant must not guess these)

1. **Bulk void refund policy** (FEAT-01): always refund as `voidSale` does today, or add a separate "entry mistake — no refund" option?
2. **Backup passphrase policy:** who knows it, where it is stored, and what happens if it is lost.
3. **Is a staff convenience mode wanted?** If yes it needs a proper limited Billing role, not a lock bypass.
4. **Lock timeout default** (currently 120 s) and whether the Recents thumbnail may be blocked.
5. **Landing idea** (#1–#5) and the exact shop name and tagline to show.
6. **New dependencies you approve:** `core-splashscreen`, `kotlinx-coroutines-test`, `room-testing`, JaCoCo, detekt, optionally LeakCanary/Turbine.
7. **Production data for migration rehearsal:** an anonymised copy of a real backup stored **off** GitHub.
8. **Recovery secret rotation** timing (SEC-04) and who holds the new file.
9. **SQLCipher** (SEC-07): revisit after SEC-01 to SEC-06, or never.
10. **Billing phone Android version** (to know whether the API 28/29 biometric fix matters in practice).

## 7. What this review did not cover (be aware)

Not read in depth: cost and rounding rules in `CostCalculator`, the Accounts and Pigmy screens' business logic, `ReceiptService` layout code, the interior of `BackupRestoreService` restore steps beyond what your docs state, `ImportExportService` row validation, the Urdu strings. Not verified on a device: whether writes to the public Documents folder succeed, how the lock behaves on Android 9-10, the sync server's behaviour in Doze, real printer output. Nothing was compiled or run. Treat every `[Likely]`/`[Check]` tag as a to-do, and treat the code snippets as drafts.

## 8. Task index

| ID | Title | Severity | Size | Depends on | New dependency? |
|---|---|---|---|---|---|
| SEC-01 | Close lock bypass | Critical | S | — | No |
| SEC-02 | Lock state survives rotation; hide Recents | High | S | — | No |
| SEC-03 | Step-up authentication | High | M | SEC-01 | No |
| SEC-04 | PIN lockout + recovery secret | Medium | S | — | No |
| SEC-05 | Encrypt DataStore secrets | Medium | M | — | No |
| SEC-06a/b/c | Encrypted backups, safe exports | High | L | SEC-03 | No |
| SEC-07 | DB encryption decision | Low | — | SEC-01…06 | (SQLCipher, deferred) |
| SEC-08 | Manifest / FileProvider hygiene | Medium | S | — | No |
| SEC-09 | Sync server hardening | Medium | M | SYNC-01 | No |
| SEC-10 | Logging and release hardening | Low-Med | S | — | No |
| SYNC-01a/b | Per-bill sync + canonical replay | High | M | TEST-04 | No |
| SYNC-02 | Foreground service for hosting | Med-High | M | — | No |
| DB-01a/b/c | Indices + audit (migration 9) | High | M | SYNC-01 | No |
| PERF-01 | Bounded queries (per screen) | Medium | M each | DB-01 | No |
| PERF-02 | Print on background thread; import limits | Medium | S | — | No |
| CODE-01…03 | Errors, owner guard, split files | Low | S–L | — | No |
| BUILD-01 | Dependency upgrades | Low-Med | S each | — | Version bumps |
| DOC-01 | Fix docs, add SECURITY.md | Low | S | — | No |
| FEAT-01a–d | Bulk void + held-bill delete | Feature | L | SEC-03 | No |
| FEAT-02a–d | Logo, splash, landing | Feature | M | — | `core-splashscreen` |
| TEST-01…10 | Testing programme | — | L | — | Several (see 5.2) |

**Recommended first week:** SEC-01 → SEC-02 → SEC-08 → TEST-01 → SEC-03 → FEAT-02a/b (logo fix is quick and visible) → SYNC-01a (quick patch). Everything else follows the phase table in section 0.3.

---
---

# 9. ADDENDUM — 9 November 2026: barcode generator, stock-import upgrade, UI redesign

New tasks added after you shared four screenshots (Add Stock dialog, Bill History, Cash In dialog) and the Antigravity progress report. Everything in Sections 0–8 still applies (master rules, Definition of Done, review checklist). Task IDs added here: **UI-00, UI-01, UI-02, UI-03, BARCODE-01, IMPORT-01, IMPORT-02, IMPORT-03, TEST-11, TEST-12, TEST-13.**

## 9.0 Read this first

1. **GitHub still holds only the original commits.** I could not see the work Antigravity did locally (security, sync, database, bulk void, logo). I reviewed the *original* code for this addendum. Therefore **every task below must begin with "open the local file and confirm what is there now"**. In particular, FEAT-01 added multi-select/bulk void to Bill History and the Cash tracker; the redesign tasks below must **keep that behaviour** and only restyle around it.
2. **Remaining tasks from your Antigravity report are unchanged:** TEST-04 → TEST-10 and the CI workflow. They stay in the plan.
3. **Recommended order from here** (one task, one branch, one session each):

| Step | Task | Why now |
|---|---|---|
| 1 | TEST-04 (sync fault injection) | Protects money integrity; already queued |
| 2 | TEST-05 (security tests) | Locks in the security fixes already made |
| 3 | **UI-00** theme tokens | Every redesign task below depends on it |
| 4 | **IMPORT-01** (import bug fixes) | Stops silent data corruption — more important than looks |
| 5 | **BARCODE-01** | Small, visible, high value in daily use |
| 6 | **UI-01** Bill History | Fixes a visible bug (squashed "Custom" chip) |
| 7 | **UI-02** Cash In/Out dialogs | |
| 8 | **UI-03** Add/Edit Stock dialog | Uses the barcode generator from step 5 |
| 9 | **IMPORT-02**, then **IMPORT-03** | New import features |
| 10 | TEST-06 → TEST-10, CI, then TEST-11 → TEST-13 | Finish the test programme |

Rule of thumb: **correctness work (IMPORT-01, tests) before appearance work**, but UI-00 first because it is tiny and unblocks the rest.

---

## 9.1 What your screenshots and the code show

| # | Screen | Observation | Cause (verified in code) | Task |
|---|---|---|---|---|
| 1 | Bill History | **"Custom" chip is squashed and its text wraps one letter per line** | `BillHistoryScreen.kt` puts 5 chips in a plain `Row` that cannot scroll; the last chip gets whatever width is left | UI-01 |
| 2 | Bill History | Tapping "Custom" a second time does nothing, so a chosen range cannot be changed | The date pickers open only when `customStartDate == null` | UI-01 |
| 3 | Bill History | Big blank white band above the title | Top-bar and system-bar insets are applied on top of each other (`TopAppBar` + `Scaffold` padding) | UI-01 |
| 4 | Bill History | Plain "No bills found" card; no way out | No empty-state actions | UI-01 |
| 5 | Bill History | Status colours (`0xFFFFEBEE`, `0xFFE8F5E9`…) are hard-coded | Will look wrong in dark mode | UI-00/01 |
| 6 | Cash In dialog | **Dialog background is lavender**, while the app is warm cream/gold | `LightColorScheme` never sets `surfaceContainerHigh`, so Material's default purple-grey is used for every dialog | UI-00 |
| 7 | Cash In dialog | Category chips are cut off ("Salary" clipped) with no hint they scroll | Horizontally scrolling `Row`, no fade/indicator | UI-02 |
| 8 | Cash In dialog | **"GPAY / UPI" wraps onto two lines**; "Choose Photo" wraps | Three equal-weight chips at 12 sp inside a narrow dialog; two equal buttons | UI-02 |
| 9 | Cash In dialog | A red **"Remove…" button sits inline** next to "New Category" in the category row | Destructive action next to selection chips invites mis-taps | UI-02 |
| 10 | Cash In dialog | The most important field (Amount) is the 4th item, below date/time | Field order | UI-02 |
| 11 | Add Stock dialog | **No "Generate barcode" button when adding a new product** | The button exists only inside `if (existingProduct != null)`, because the shop barcode is derived from the product's database id, which does not exist yet | BARCODE-01 |
| 12 | Add Stock dialog | **Printed shop barcodes are very dense** | See 9.6: 39-character payload at about 0.12 mm per bar | BARCODE-01 |
| 13 | Add Stock dialog | "Bill / Invoice No." is cut off at the bottom; the form is a long scroll inside a small dialog | `AlertDialog` with a scrolling `Column` | UI-03 |
| 14 | Add Stock dialog | Labels are ambiguous: "Pcs", "Base Cost", "Margin (%)" sits beside "Wholesale" | Layout/labels | UI-03 |
| 15 | Add Stock dialog | Live profit preview uses `(landedCost * 100).toLong()` (truncating floating-point). The *saved* values use exact `BigDecimal`, so no money is wrong, but the preview can be a paisa off | Display math | UI-03 |
| 16 | Stock import | Silent data corruption cases (formula cells, text numbers, leading-zero barcodes, header-row mismatch) | See 9.7 | IMPORT-01 |

---

## 9.2 UI-00 — Theme tokens so dialogs match the app  · Branch `ui-00-theme-tokens`

**Problem.** `ui/theme/Theme.kt` defines `background`, `surface` and a few others but not the `surfaceContainer*` family. Material 3 dialogs, bottom sheets, menus and cards read `surfaceContainerHigh`, so in light mode they fall back to Material's lavender (`#ECE6F0`), as in your Cash In screenshot. Status colours are hard-coded per screen, so dark mode is unreliable.

**Fix.** Add the container colours to both schemes, and add a small set of semantic status colours that have light and dark variants.

```kotlin
// ui/theme/Color.kt  — proposed values: look at them on a real phone and adjust before committing
val LightSurfaceContainerLowest  = Color(0xFFFFFFFF)
val LightSurfaceContainerLow     = Color(0xFFFBF9F4)
val LightSurfaceContainer        = Color(0xFFF7F4EC)
val LightSurfaceContainerHigh    = Color(0xFFF2EEE4)   // dialogs, sheets
val LightSurfaceContainerHighest = Color(0xFFEBE6DA)

val DarkSurfaceContainerLowest   = Color(0xFF0D100D)
val DarkSurfaceContainerLow      = Color(0xFF181D19)
val DarkSurfaceContainer         = Color(0xFF1C211D)
val DarkSurfaceContainerHigh     = Color(0xFF242A25)   // dialogs, sheets (green-tinted, matches DarkSurface)
val DarkSurfaceContainerHighest  = Color(0xFF2C332D)

@Immutable
data class AliStatusColors(
    val paidBg: Color, val paidFg: Color,
    val dueBg: Color,  val dueFg: Color,
    val voidBg: Color, val voidFg: Color
)
val LocalAliStatusColors = staticCompositionLocalOf {
    AliStatusColors(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFECEFF1), Color(0xFF546E7A))
}
```
In `Theme.kt` pass `surfaceContainerLowest … surfaceContainerHighest` into both `lightColorScheme(...)` and `darkColorScheme(...)`, and provide dark values of `AliStatusColors` (for example paid `0xFF1B3A22`/`0xFF81C784`, due `0xFF3E1F1F`/`0xFFEF9A9A`, void `0xFF2B3236`/`0xFFB0BEC5`) through `CompositionLocalProvider` in `AliEducationalTheme`.

**Also define spacing/shape constants** in one place (`AliDimens`: 4/8/12/16/24 dp; corner radii 8/12/16/28) and use them in the new screens. Minimum chip/button text size 13 sp and touch target 48 dp.

**Check the colour contrast** of every pair you introduce (text on container ≥ 4.5:1; the tool at webaim.org/resources/contrastchecker is enough). Do not trust the numbers above blindly.

```text
TASK UI-00. Files: ui/theme/Color.kt, ui/theme/Theme.kt only.
1) Add the surfaceContainerLowest..Highest colors from the plan to LightColorScheme and DarkColorScheme.
2) Add AliStatusColors + LocalAliStatusColors with light and dark variants, provided inside AliEducationalTheme based on darkTheme.
3) Add object AliDimens with the spacing and corner constants from the plan.
Do not change any screen in this task. Add a Compose UI test that renders an AlertDialog in light and dark theme and asserts it composes without error.
Manual check (owner): open any existing dialog in light mode — it should be warm cream, not lavender.
```

---

## 9.3 UI-01 — Bill History redesign  · Branch `ui-01-bill-history`

**Goals.** Fix the bugs in rows 1–5 above, make the list quicker to scan, and keep every existing function (period filters, search, status filter, open receipt, repay, open customer, **and the multi-select / bulk-void mode from FEAT-01**).

**Layout (wireframe):**
```text
┌────────────────────────────────────┐
│ ←  Bill History              ⋮     │  one 56 dp bar, no extra gap
│     Today · 09 Nov 2026            │
├────────────────────────────────────┤
│ [Today] [Week] [Month] [All] [📅 Custom ▾]   ← ONE scrollable row, never wraps
│ 🔍 Search receipt # or customer      ✕
│ [All 12] [Paid 9] [Due 3] [Voided 0]          ← with counts
│ ┌────────────────────────────────┐
│ │ NET BILLED            ₹4,250.00│  hero number
│ │ 12 bills                       │
│ │ ─────────────────────────────  │
│ │ Collected ₹3,900   Dues ₹350   │
│ └────────────────────────────────┘
│ TODAY                    ₹4,250.00     ← sticky date header with day total
│ ┃ AE-0123 · 02:14 PM        [✓ Paid] │  4 dp status stripe on the left
│ ┃ Walk-in customer             ₹250  │
│ ┃ 💵 Cash · 3 items                  │
│ ┃ AE-0122 · 01:02 PM   [Due ₹350]    │
│ …                                    │
└────────────────────────────────────┘
```

**Changes, in the order I would implement them:**
1. **Chip row that cannot squash.** Use a `LazyRow` (or `Row(horizontalScroll)`) with `FilterChip(label = { Text(p, maxLines = 1, softWrap = false) })`. Show the chosen custom range *inside* the chip ("12 Oct – 18 Oct") and an ✕ to clear it.
2. **Custom range** with Material 3 `DateRangePicker` in a `DatePickerDialog` instead of two chained legacy dialogs; tapping the chip again always reopens it. **Pitfall:** the Material pickers return **UTC midnight** milliseconds. Convert to local-day boundaries or bills near midnight will be dropped:
   ```kotlin
   fun utcPickerMillisToLocalStart(ms: Long): Long =
       Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
           .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
   fun utcPickerMillisToLocalEnd(ms: Long): Long =
       Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
           .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
   ```
   Write a unit test for these two functions including the 23:59 and 00:00 edge cases.
3. **Header gap.** Apply window insets once: either let `TopAppBar` own the status-bar inset and give the `Scaffold` `contentWindowInsets = WindowInsets(0)`, or the reverse; verify on a device with a punch-hole camera (your S24 Ultra) and a gesture-navigation phone.
4. **Summary card:** net billed as the large number; collected and remaining dues below; due amount in the status colour; tap the card to filter to "Due".
5. **Grouped list:** group by local day with a header (Today, Yesterday, `07 Nov`) and a day total. `stickyHeader` may need `@OptIn(ExperimentalFoundationApi::class)`.
6. **Row card:** status stripe + pill using `LocalAliStatusColors` (icon **and** text, never colour alone), time next to the receipt number, payment-mode icon, item count, amount aligned right in a tabular-looking bold style. Keep the whole row tappable; keep the existing repay and customer shortcuts.
7. **Empty state:** icon, "No bills today", and two buttons: **View this week** and **Clear filters** (only the ones that apply).
8. **Selection mode** (from FEAT-01): keep it. Show the contextual top bar ("3 selected · Select all · ✕") in place of the normal bar, and tint selected rows.
9. **Performance:** do not add all-time loading; respect PERF-01 (date-range queries, "Load more").

```text
TASK UI-01 (do in 3 small sessions; one commit each).
FIRST open the current local ui/billing/BillHistoryScreen.kt and list which features exist (filters, search, status chips, bulk selection, repay, open customer). Do not remove any.
01a: Replace the period Row with a non-wrapping scrollable chip row; label maxLines=1 softWrap=false; custom chip shows the chosen range and an X to clear; re-tapping reopens the picker. Replace the two legacy DatePickerDialogs with DateRangePicker + the UTC->local conversion helpers from the plan, with unit tests.
01b: Fix the top gap (insets applied once). Rebuild the summary card (hero net billed) and add status counts to the status chips. Use AliDimens and LocalAliStatusColors from UI-00; remove the hard-coded status colors.
01c: Group by day with sticky headers and day totals; redesign BillHistoryRow (stripe, pill with icon+text, time, payment icon); add the empty state with two actions. Keep selection mode working.
After each: Compose UI tests (chips never wrap at font scale 1.3; Custom re-opens; empty state buttons work), and manual screenshots in light AND dark at normal and large text.
```

**Done when.** At normal and large text sizes nothing wraps letter-by-letter, the custom range can be changed repeatedly, bills near midnight land on the correct day, and light/dark both look intentional.

---

## 9.4 UI-02 — Cash In / Cash Out dialogs  · Branch `ui-02-cash-dialogs`

**Goals.** Fix rows 6–10, make adding an entry fast (the amount first, one thumb), keep every existing option (category, date/time, description with suggestions, payment mode, extra note, receipt photo, Pigmy scheme behaviour, edit dialog).

**Wireframe (Cash In; Cash Out uses the red accent):**
```text
┌ Add Income (Cash In) ─────────────────┐   green title bar/accent
│  ₹  [ 0.00                       ]     │   AMOUNT FIRST: large, autofocus, decimal keypad
│     ₹ 12,500.00                        │   live formatted preview (Indian grouping)
│                                        │
│  Category                       ✎ Manage│  ← manage opens the existing dialog
│  [Electricity] [General✓] [Rent]       │  wrapped chips (FlowRow), no hidden scroll
│  [Salary] [Sales] [＋ New]              │
│                                        │
│  Payment   ┌Cash┬UPI┬Bank┐             │  segmented control, one line each
│                                        │
│  📅 Today, 09 Nov · 02:15 AM   Change  │  one compact line
│                                        │
│  Description (optional)                │
│  [Sales] [Kuree]  ← suggestions        │
│  ▸ More details (note, receipt photo)  │  collapsed; opens Extra note + Add receipt
│                                        │
│          [Cancel]   [ Save ]           │  sticky bottom row
│          Save & add another            │
└────────────────────────────────────────┘
```

**Changes:**
1. **Container:** `AlertDialog` colours come from UI-00 (no more lavender). Prefer a full-width `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))` or a `ModalBottomSheet` so there is room; Save/Cancel stay **pinned** and visible while the content scrolls.
2. **Amount first:** large `OutlinedTextField`, `KeyboardType.Decimal`, `ImeAction.Next`, requested focus on open, error text if empty or ≤ 0, Save disabled until valid. Validate with `CostCalculator.parseRupeesToPaise` (exact), never with `Double`.
3. **Categories as wrapped chips** (`FlowRow`) instead of a hidden horizontal scroll. Move **"Remove…" out of the chip row**; put a small ✎ Manage action in the header that opens the existing manage dialog. Pigmy scheme chips keep their icon and the explanatory card.
4. **Payment mode** as `SingleChoiceSegmentedButtonRow` with three single-line segments: Cash / UPI / Bank (rename "GPAY / UPI" to "UPI"; keep the stored key `GPAY` unchanged so existing data and the Excel import/export mapping do not change).
5. **Date and time** on one compact line with a "Change" action opening the existing pickers.
6. **Receipt photo:** one "Add receipt" row with two icon buttons (Camera, Gallery) that never wrap; show the thumbnail and a remove ✕ after selection. Keep the existing attachment code path (`AttachmentManager`).
7. **"Save & add another"** keeps the dialog open, clears amount and description, keeps category/payment/date.
8. **Accessibility:** all touch targets ≥ 48 dp; chips ≥ 13 sp; the amount field announces "Amount in rupees".
9. Apply the **same layout to the Edit dialog** (`CashTrackerScreen.kt` around line 1594) so adding and editing feel identical. Add and edit should share one composable to prevent the two drifting apart again.

```text
TASK UI-02 (2 sessions).
FIRST open local ui/reports/CashTrackerScreen.kt, list every field/option in the Add dialog and the Edit dialog, and confirm none will be lost.
02a: Extract a shared composable CashEntryForm used by both Add and Edit. Layout per the plan wireframe: amount first, FlowRow categories (move the red Remove button into a Manage action), segmented payment mode (label UPI, key stays GPAY), compact date/time line, collapsed "More details" containing extra note and the receipt buttons. Behaviour and saved data must be IDENTICAL (same ViewModel calls, same stored values).
02b: Pinned Save/Cancel bar and "Save & add another"; amount validation with CostCalculator.parseRupeesToPaise; accessibility sizes.
Tests: Compose tests - Save disabled for empty/zero/"abc"; categories wrap and none is clipped at font scale 1.3; "UPI" segment is one line; Pigmy scheme selected shows the info card; saving writes the same ShopExpenseEntity values as before (compare with the old code path in a unit test using a fake ViewModel).
```

**Done when.** No chip is clipped or wrapped, the red Remove button is gone from the chip row, amount is the first field, and the saved records are identical to before.

---

## 9.5 UI-03 — Add / Edit Stock & Product dialog  · Branch `ui-03-stock-dialog`

**Goals.** Fix rows 11, 13, 14, 15; keep the `onSave(...)` signature unchanged so there is **no ViewModel, repository or database change** (lowest possible risk); add the barcode generator from BARCODE-01.

**Wireframe (full-screen dialog; dark and light via the theme):**
```text
┌ ✕  Add Stock & Product        [Save] ┐  (Save also pinned at the bottom)
│ PRODUCT                                │
│  [ Product name *                  ]   │
│  [ Barcode   ✨Generate  ▣Scan     ]   │  generate + scan icons in the field
│   ┌ barcode preview (Code 128) ┐ 🖨 Print labels
│  [ Category ▾ ]   [ Brand ]            │  category = dropdown of existing + "New"
│ STOCK                                  │
│  [ Quantity received (pcs) ] [ Low-stock alert ]
│  (edit mode: "In stock now: 24 pcs" and the field is "Add to stock (pcs)")
│ PRICING                                │
│  [ Cost price ₹/pc * ]  [ Selling ₹/pc * ]  [ Wholesale ₹/pc ]
│  (Gross margin | Markup on cost)  [ Margin % ]
│  ┌ Profit ₹5.00/pc · 33.3% margin ┐  warning chip if selling < cost
│ ▸ SUPPLIER BILL (optional)  — opens automatically when quantity > 0
│  [ Supplier ▾ ] [ Bill / Invoice No. ] [ Transport ₹ ] [Paid now ▢ ▾ method]
├────────────────────────────────────────┤
│ Stock value ₹1,200 · Profit ₹250   [Cancel] [Save]  ← sticky summary + actions
└────────────────────────────────────────┘
```

**Changes:**
1. **Full-screen dialog with sections** (Product, Stock, Pricing, Supplier bill) and a sticky bottom bar showing a live summary and Save. No more clipped "Bill / Invoice No.".
2. **Clear labels:** "Quantity received (pcs)", "Low-stock alert", "Cost price (₹/pc)", "Selling price (₹/pc)". Put the margin controls under pricing; do not place Margin % beside Wholesale.
3. **Number fields:** default to blank with a `0` placeholder (no need to delete a leading "0"), `KeyboardType.Decimal` for prices and `Number` for counts, `ImeAction.Next`, select-all on focus.
4. **Exact preview math.** Replace every `(x * 100).toLong()` in the dialog with paise arithmetic (`parseRupeesToPaise` once, then `Long` maths) or `BigDecimal`. A tiny helper `paiseOrNull(String)` is enough. Add a unit test: cost `19.99`, selling `29.99` shows profit exactly ₹10.00 and 33.3 %.
5. **Category dropdown** (exposed dropdown with the existing categories plus "New category…") instead of free text, to stop "Stationery"/"Stationary" duplicates.
6. **Supplier-bill section** collapsed by default when editing price only, auto-expanded when quantity > 0.
7. **Warnings, not blocks:** selling below cost, selling = 0, cost = 0 with quantity > 0, a barcode already used by another product (show the product name).
8. **Discard protection:** if any field changed, back/close asks "Discard changes?" (the dialog already blocks outside-tap dismissal; this adds a deliberate confirmation).
9. **Edit-mode clarity:** **[Check]** confirm in the code whether "Pcs" in edit mode *adds* to stock; show "In stock now: N pcs" and label the field accordingly.
10. **Barcode block** exactly as in BARCODE-01.

```text
TASK UI-03 (3 sessions).
FIRST open local ui/stock/StockScreen.kt IntegratedAddStockProductDialog and list every state variable and the exact onSave signature. The signature must NOT change.
03a: Convert to a full-screen Dialog (usePlatformDefaultWidth=false) with sections Product / Stock / Pricing / Supplier bill and a pinned bottom bar (Cancel, Save, live summary). Same fields, same onSave call, same validations.
03b: Rename labels per the plan; number-field behaviour (blank+placeholder, keyboard types, ImeAction.Next, select-all on focus); replace (x*100).toLong() with exact paise math and add the unit tests from the plan; category dropdown from existing categories.
03c: Warnings (selling<cost etc.), supplier-bill auto-expand, discard confirmation, edit-mode "In stock now" line (after confirming the Pcs semantics).
Tests: Compose tests for validation, warnings, Save disabled states, that onSave receives the same values as before for 5 sample inputs (compare to the previous implementation's output via a pure function), and rotation keeps typed values.
```

**Done when.** The whole form is reachable without a clipped field at normal and large text, the saved product is identical to before for the same inputs, and the preview never differs by a paisa from the saved margin.

---

## 9.6 BARCODE-01 — Barcode generator in Add Stock + printable-quality labels  · Branch `barcode-01-generator`

### What exists today (verified)
- `BarcodeGeneratorService` already renders **Code 128** barcodes (ZXing, fully offline) and builds an **A4 PDF label sheet (3 × 8)**; `AppRepository.generateAndRegisterBarcode` registers the code in `product_barcodes` and refuses to overwrite a manufacturer barcode; the Edit dialog shows **"Generate Shop Barcode"** or **"Print / Export Barcode Labels"**.
- **Gap 1:** in the *Add* dialog there is no Generate button, because the payload is derived from the product id (`AE-P<uuid-without-dashes>-PC`), which only exists after saving.
- **Gap 2 (important): the generated payload is too long to print and scan reliably.** It is 39 characters (`AE-P` + 32 hex + `-PC`). A Code 128 symbol needs 11 modules per character, so about 464 modules; drawn into the 164-pt-wide area on the label that is **about 0.12 mm per bar** (my calculation from the code; measure it on paper). Retail guidance is typically 0.25 mm or more; phone cameras and cheap scanners will struggle, especially with home-printer ink spread. The human-readable text under the bars (`AE-P…`, 8.5 pt) is also wider than the label cell. **[Likely]** — confirm by printing one label and scanning it from 15 cm with the app's own scanner and with a cheap USB/Bluetooth scanner if you have one.
- **Gap 3:** labels draw a *bitmap* stretched to size, which blurs bar edges when printed; vector bars are sharper.
- **Gap 4:** the label PDF prints N copies of **one** product; there is no multi-product sheet.

### Design

**1. Short, random shop codes (new codes only).** The code does not need to *contain* the product id; the database already maps barcode → product (`product_barcodes`, unique index on `barcode`). Use `AE-P` + 8 characters from Crockford base-32 (no I, L, O, U), generated from 40 random bits, and check it is unused (retry up to 5 times):

```kotlin
object ShopBarcode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"   // 32 symbols
    fun newPayload(unitType: String = "PIECE", packQty: Int = 1,
                   rng: java.security.SecureRandom = java.security.SecureRandom()): String {
        val b = ByteArray(5).also(rng::nextBytes)
        var v = 0L; for (x in b) v = (v shl 8) or (x.toLong() and 0xFF)
        val core = "AE-P" + (0 until 8).map { ALPHABET[((v shr (5 * (7 - it))) and 31).toInt()] }.joinToString("")
        return if (unitType.equals("PACK", true) && packQty > 1) "$core-PK$packQty" else core
    }
}
```
- Piece code: 12 characters → about 167 modules → **about 0.34 mm** at 164 pt. Pack code: about 16 characters → about 0.26 mm.
- **Existing long codes keep working** for scanning and lookup (`startsWith("AE-P")` checks still match). **First `grep -rn '"-PC"\|"-PK\|-PC\b\|-PK' app/src`** to confirm nothing parses the old suffix; if something does, update it to accept both.
- For a product that only has a legacy long code, offer **"Add short barcode"**, which registers a second row in `product_barcodes` for the same product (the table already supports several barcodes per product) so old printed stickers still scan. `generateAndRegisterBarcode` currently returns the existing AE-P code if one exists; adjust so a short alias can be created.

**2. Generate in the Add dialog.** Inside the barcode field show two trailing icons: **✨ Generate** and **▣ Scan**. Generate calls a new ViewModel function `suggestShopBarcode(unitType, packQty, onResult)` that creates a payload and verifies it is unused through the existing `lookupBarcode` + `productDao.getProductByBarcode`. The code is placed in the field. **No product row is created yet**; `addOrUpdateProductWithStock` already registers the barcode in `product_barcodes` on Save (verified), so saving completes the registration. Hide Generate when the field already contains a code (never overwrite a manufacturer barcode). After generating, show the **preview card** and a **"Print labels after saving"** toggle that opens the label dialog for the new product.

**3. Better labels.**
- Draw bars as **vector rectangles** from the ZXing `BitMatrix` (encode with width `0`, height `1` to get one pixel per module), scale by an exact multiple so the **module width is at least 0.25 mm**; compute and show a warning when a payload cannot meet that on the chosen label size.
- Auto-shrink the human-readable text to fit the cell (`Paint.measureText`) and never overflow it.
- **Label size presets:** A4 3 × 8 (current), A4 4 × 10 small stickers, and thermal **50 × 25 mm** and **38 × 25 mm** (decision for you: which printer/paper do you actually use?).
- **Multi-product sheet:** pick several products, set copies each (default = quantity received), build one PDF. Entry point from the product list multi-select and from the import result (IMPORT-03).
- Keep a quiet zone of at least 10 modules left and right.

**4. Checks when a code is typed or scanned.** For 13-digit numeric codes validate the EAN-13 check digit and *warn* (do not block) if it is wrong; strip hidden/zero-width characters and surrounding spaces; shop codes are upper-cased.

**5. Bulk helper.** In the product list: select items → **"Generate barcodes for items without one"** (owner-only, step-up auth, one transaction per product so one failure does not block the rest) → **Print labels**.

### Tasks
```text
TASK BARCODE-01a (pure logic, JVM tests). Create domain/barcode/ShopBarcode.kt with newPayload() as in the plan, plus fun moduleWidthMm(payload: String, printableWidthPt: Float): Float using (payload.length + 2) * 11 + 13 modules (add 20 modules if you also count the quiet zones in the drawn width), and fun isEan13Valid(code: String): Boolean.
Before coding, run grep for "-PC" and "-PK" and report any code that parses those suffixes.
Tests: 10,000 generated codes are all 12 chars for pieces and unique; alphabet only; pack suffix present; moduleWidthMm(newPayload(), 164f) >= 0.30f; moduleWidthMm of a 39-char legacy code < 0.15f (documents the old problem); EAN-13 valid/invalid vectors (4006381333931 valid, 4006381333932 invalid).

TASK BARCODE-01b (repository + ViewModel). Add AliViewModel.suggestShopBarcode(unitType, packQty, onResult) that retries up to 5 times until lookupBarcode returns null and productDao.getProductByBarcode returns null. Change AppRepository.generateAndRegisterBarcode to use ShopBarcode.newPayload for NEW codes (keep legacy codes valid) and to allow a short alias when the only existing AE-P code is a legacy long one. Keep requireOwner(). Tests (in-memory Room): collision retry works with a fake RNG; a manufacturer barcode is never overwritten; alias creation adds a second product_barcodes row; scanning either code resolves to the same product.

TASK BARCODE-01c (UI in Add dialog). In IntegratedAddStockProductDialog add the Generate icon to the barcode field (hidden when the field is non-blank), the preview card using BarcodeGeneratorService.generateBarcodeBitmap, and the "Print labels after saving" toggle. Do not change the onSave signature. Test: Generate fills the field with a code starting AE-P; the saved product has that barcode in products AND product_barcodes; Generate not shown for a typed manufacturer barcode.

TASK BARCODE-01d (labels). Rewrite generateLabelsPdf to draw vector bars, auto-fit text, support presets (A4 3x8, A4 4x10, 50x25 mm, 38x25 mm) and a list of (product, barcode, price, copies) items. Add a unit test that the PDF is created and a Paint.measureText-based test that the human-readable text always fits. Owner step: print one sheet on your real printer and scan 5 labels with the app scanner.
```

**Decision for you:** the label printer and sticker size you really use; and whether you want to keep printing the old long codes (not recommended).

---

## 9.7 IMPORT — Stock import from Excel: fixes and recommended features

### What the importer does today (verified by reading)
CSV and Excel (`.xlsx`/`.xls` via Apache POI), sheet picker, 15-row preview with automatic header-row guess, **manual** column mapping for 12 fields (name, category, brand, SKU, barcode, base unit, pack size, opening quantity, purchase cost, piece price, pack price, low-stock), duplicate detection by SKU → barcode → case-insensitive name, an "update existing" switch (adds quantity, moving-average cost, new prices), a guard that refuses cash-ledger files, an "already imported" check that blocks the **exact same file + mapping**, Owner-only, and one database transaction for the whole file.

### IMPORT-01 — Bugs to fix first (silent data corruption)  · Branch `import-01-correctness`

| # | Problem (file `ImportExportService.kt`) | Effect on your stock | Confidence |
|---|---|---|---|
| a | **Formula cells:** the formula branch returns `cell.numericCellValue.toString()`, so a quantity produced by a formula (`=B2*C2`) becomes `"1200.0"`; the importer then does `toIntOrNull() ?: 0` | Quantity, pack size and low-stock silently become **0 / 1 / 5** | **[Verified by reading]** — write the failing test first |
| b | **Numbers stored as numbers lose leading zeros:** the numeric branch does `toLong().toString()` | A barcode such as `0123456789012` becomes `123456789012`; scans will not match | **[Verified by reading]** |
| c | **Unparseable values silently become zero:** `parseRupeesToPaise(...) ?: 0L`, `toIntOrNull() ?: 0`. The parser rejects `.50`, `19.999`, `12 /-`, `N/A`, `1.2E3`, negatives | Products imported with cost/price/quantity **0** and no error shown | **[Verified]** |
| d | **Header row mismatch:** the preview guesses the header row, but `importProducts` takes no header-row index and always treats the *first non-blank row* as the header | A sheet with a title line above the headers imports the real header row as a product (and shifts everything) | **[Likely]** — confirm with a test file |
| e | **One bad row cancels everything:** `require(errors.isEmpty())` rolls back the whole file and shows all errors joined into one message | Users cannot fix and continue; large files are painful | **[Verified]** |
| f | **Re-importing an edited file in update mode adds the quantities again** (the duplicate-file guard only catches an identical file + mapping) | Stock doubles after the second import | **[Verified]** |
| g | **Duplicate rows inside the same file** are not reported; the second row finds the first one "existing" | Skipped silently or double-added depending on mode | **[Likely]** |
| h | **CSV reader** uses the default charset, does not remove a UTF-8 BOM (first header becomes `\uFEFFName`), reads line by line (quoted multi-line fields break), comma only (semicolon files become one column), and Windows-1252 exports garble names | Wrong or missing columns | **[Verified]** |
| i | The importer **does not register `product_barcodes`** rows (lookup still works through the product's primary barcode, so scanning is fine) | Barcode-collision checks (which read `product_barcodes`) miss imported codes; pack barcodes cannot be imported | **[Verified]** |
| j | Whole workbook is loaded into memory (see PERF-02) | Out-of-memory on very large sheets | **[Likely]** |

**Approach — split the importer into pure, testable stages:**
1. **Read** (Excel/CSV → rows of typed cells). Use POI `DataFormatter` so the *displayed* text is read (keeps leading zeros that the cell format shows; reads formula results correctly with a `FormulaEvaluator`), plus a robust CSV reader: BOM stripping, quote/escape handling including multi-line fields, delimiter sniffing (`,` `;` tab), charset fallback (UTF-8, then Windows-1252). Pass the **header row index** from the preview into the import.
2. **Parse** each field with a function that returns a **value or an error** (never a silent default): `parseQty`, `parseMoney` (accepts `₹`, `Rs`, `/-`, Indian grouping `1,20,000`, `.50`, trailing `.0`; rejects text and negatives; warns on > 2 decimals), `parsePackSize`, `parseBarcode` (text, trim, strip hidden characters, optional EAN check).
3. **Plan** (no database writes): produce a `PlannedRow` for every row: `NEW`, `UPDATE`, `SKIP_DUPLICATE`, `ERROR`, with a list of issues (error/warning, field, message), the existing product it matched and *why* (SKU/barcode/name), and detection of duplicates **within the file**.
4. **Commit**: apply the plan in one transaction, either **all-or-nothing** (current behaviour, default) or **valid rows only** (new option, with the skipped rows listed). Register `product_barcodes` for every imported barcode and write stock movements with the batch id as `referenceId`.

```kotlin
enum class RowStatus { NEW, UPDATE, SKIP_DUPLICATE, ERROR }
enum class Severity { ERROR, WARNING }
data class RowIssue(val severity: Severity, val field: String, val message: String)
data class PlannedRow(
    val rowNumber: Int, val status: RowStatus, val issues: List<RowIssue>,
    val matchedProductId: String?, val matchedBy: String?   // "SKU" | "Barcode" | "Name"
)
sealed interface Parsed<out T> { data class Ok<T>(val v: T) : Parsed<T>; data class Err(val msg: String) : Parsed<Nothing> }
```

**Prompts (4 sessions). Always start each with the failing fixture test.**
```text
TASK IMPORT-01a (cell reading and number parsing). Files: data/service/ImportExportService.kt plus new data/service/import/CellReader.kt and ImportParsers.kt.
First create test workbooks in code with Apache POI (tests build .xlsx files in memory): (1) a qty column filled with formulas =B2*C2, (2) barcodes stored as NUMBERS with leading zeros and as TEXT, (3) prices written as "₹ 1,20,000", "12.50", ".50", "19.999", "12 /-", "N/A", "-5", (4) quantity "12 pcs", "1,200", "12.0".
Write tests asserting the CURRENT code gives wrong results (qty 0 etc.) and show them failing against the intended behaviour. Then implement CellReader (DataFormatter + FormulaEvaluator) and parseQty/parseMoney/parsePackSize returning Parsed<T> (Ok or Err with a human message). No silent defaults anywhere. Do not change the database logic in this task.

TASK IMPORT-01b (header row and CSV). Pass the detected header row index from previewFile into importProducts via ColumnMapping (new field headerRowIndex, default 0 for backward compatibility). Replace readAllCsv/parseCsvLine/previewCsv with one CsvReader supporting BOM, quoted multi-line fields, escaped quotes, delimiter sniffing and charset fallback. Tests with files: title row above headers; BOM file; semicolon file; Windows-1252 file with ₹ and accented names; quoted field containing a newline.

TASK IMPORT-01c (plan then commit). Introduce PlannedRow/RowStatus/RowIssue and function planImport(rows, mapping, existingLookup): List<PlannedRow> that touches NO database writes (existing lookups are read-only suspend calls). Detect duplicates within the file (same barcode, SKU or normalized name). Then commit(plan, mode, validRowsOnly) in one db.withTransaction. Keep all-or-nothing as the default. Tests: 5-row file with 1 bad row => all-or-nothing rolls back with a per-row error list; validRowsOnly imports 4 and reports 1; duplicate rows inside the file are flagged; totals in the result equal what the database contains.

TASK IMPORT-01d (barcodes and re-import safety). On commit, register product_barcodes for imported barcodes (skip when the barcode already maps to a DIFFERENT product and report that as an ERROR row). Make update mode explicit: the existing "update existing" switch becomes an enum ImportMode { NEW_ONLY, ADD_STOCK, SET_STOCK, PRICES_ONLY, DETAILS_ONLY }. SET_STOCK writes a STOCK_ADJUSTMENT movement for the difference (can be negative). Show a confirmation total (see IMPORT-02). Tests: importing the same edited file twice in ADD_STOCK mode is blocked by a clear warning/confirmation; SET_STOCK twice is idempotent.
```

### IMPORT-02 — Recommended new features  · Branch `import-02-features`

My ranking, with reasons. **Must** = fixes real daily pain/risk; **Should** = clear time saver; **Later** = valuable but needs a decision or more work.

| Rank | Feature | Why it matters | Priority |
|---|---|---|---|
| 1 | **Validation preview before importing**: every row with status (New / Update / Duplicate / Error / Warning), filters, counts, and a plain summary: *"This will add 1,240 pcs worth ₹86,300 at cost; 12 new, 40 updates, 3 skipped, 2 errors"* | You see problems *before* stock changes; replaces the "rolled back, read this long message" experience | **Must** |
| 2 | **Import modes**: *Add new items only* · *Add to stock* · *Set stock to counted quantity (stock-take)* · *Update prices only* · *Update details only* | Today "update" always adds quantity, so re-importing an edited sheet doubles stock | **Must** |
| 3 | **Error report you can fix and re-upload**: export the failed rows to an Excel file with an extra "Problem" column | Fix the sheet in Excel instead of guessing | **Must** |
| 4 | **Template download**: an `.xlsx` with the right headers, 3 example rows, a drop-down for category, and an "Instructions" sheet; plus **Export current stock to Excel** (same columns) so you can edit and re-import with *Update* | Eliminates mapping mistakes; round trip workflow | **Must** |
| 5 | **Auto-detect columns** from header names with synonyms (Item / Product / Particulars → Name; Qty / Stock / Quantity / Pcs → Opening quantity; MRP / Rate / S.P. / Selling → Selling price; Cost / Purchase / C.P. → Cost; EAN / Barcode / Code → Barcode; Wholesale; Brand; Category) with a confidence badge, and **remember the mapping** per file layout (`headers hash`) | Mapping becomes one tap for repeat suppliers | **Should** |
| 6 | **Extra columns**: Wholesale price; **Boxes + Pack size → pieces**; **Pack price → piece price** when only the pack price is given; low-stock alert | The Add dialog has wholesale and packs but the importer does not | **Should** |
| 7 | **Missing selling price? Apply default margin** (choose Gross margin or Markup on cost and a %, same rule as the Add dialog), shown as a column in the preview | Faster pricing of new stock | **Should** |
| 8 | **Auto-generate shop barcodes for rows without one** (BARCODE-01) and offer a **label sheet** for all newly generated codes | Items arrive ready to scan | **Should** |
| 9 | **Near-duplicate detection**: normalise names (case, spaces, punctuation, "pcs"/"pc") and flag likely matches ("Classmate Notebook 200pg" vs "Classmate Note Book 200 pages") as **Review**, never auto-merge | Prevents catalogue clutter | **Should** |
| 10 | **Price and cost change review** for updates: old → new, % change, warning for selling < cost or change above a threshold you set | Catches typing mistakes in the sheet | **Should** |
| 11 | **Barcode checks**: EAN-13 check digit warning; same barcode on two different products in the file or database; leading-zero preservation | Barcode quality | **Should** |
| 12 | **Progress bar with cancel** and a hard limit (size/rows) with a friendly message; import runs in the background so rotating the phone does not interrupt it | Big sheets | **Should** |
| 13 | **Import history** screen: file name, time, counts, mode, who imported, **share the error report** | Audit trail (a table already exists: `import_batches`) | **Should** |
| 14 | **Undo last import** (only while none of the imported products has been sold or adjusted since): reverses the stock movements that carry the batch id | Safety net for a wrong file | **Later** |
| 15 | **Multi-sheet import** ("one sheet per category": sheet name becomes the category) | Common in shop spreadsheets | **Later** |
| 16 | **Purchase mode**: supplier and invoice-number columns create a proper purchase bill, supplier dues and landed cost (transport share) instead of an opening-stock movement | Accurate cost and supplier accounts, but it changes accounting; needs your decision | **Later (decision)** |

**Recommended import flow (5 steps):**
```text
1 Choose file   → 2 Check columns (auto-detected, editable, "use saved mapping")
→ 3 Review (row table with status filter + summary line + "Download error report")
→ 4 Options (mode, valid-rows-only, default margin, generate barcodes)   [step-up auth before commit]
→ 5 Result (counts, what changed, share report, "Print labels for new items", Undo if enabled)
```

```text
TASK IMPORT-02a (auto-mapping + saved mappings). Pure Kotlin AutoMapper.suggest(headers): Map<field, Pair<columnIndex, confidence>> with a synonym table in a Kotlin file (English only for now), and a DataStore or Room-free storage of mappings keyed by a hash of the normalized headers (use existing DataStore; no migration). Tests with 10 realistic header sets (including "Item Name", "S.P.", "C.P.", "Qty in pcs", "EAN").
TASK IMPORT-02b (review screen). Build the Review step from the PlannedRow list: summary line, status filter chips, a row list showing issues, and "Export error report (.xlsx)" using the existing Excel writer utilities. Total value line must be computed with Long paise arithmetic.
TASK IMPORT-02c (modes + options step). Wire ImportMode and validRowsOnly; require SEC-03 step-up auth before commit; add confirmation text with totals.
TASK IMPORT-02d (extra columns). Wholesale, boxes x pack size, pack price -> piece price, default margin for missing selling price (reuse CostCalculator pricing functions; do not duplicate formulas).
TASK IMPORT-02e (near-duplicates + price review). Name normalizer + similarity (token overlap or trigram; no new library) producing Review warnings; old->new price table for updates.
```

### IMPORT-03 — Template, export, barcodes-on-import, history  · Branch `import-03-extras`
```text
TASK IMPORT-03a. Add "Download template" and "Export current stock" (owner-only). Use Apache POI already in the project. SECURITY: any cell text that starts with = + - or @ must be written with a leading apostrophe or as an explicit text cell to prevent spreadsheet formula injection. Test it with a product named "=HYPERLINK(...)".
TASK IMPORT-03b. Optional "Generate shop barcodes for rows without barcode" in the import options (uses BARCODE-01's ShopBarcode, collision-checked inside the same transaction) and, in the result step, a "Print labels" button that opens the multi-product label sheet.
TASK IMPORT-03c. Import history list from import_batches (read-only screen) with the stored counts; add columns only if a migration is unavoidable (then it joins the next DB migration, never its own surprise migration).
TASK IMPORT-03d (Later). Undo last import by reversing movements carrying the batch id, allowed only when the affected products have no later movements.
```

**Decisions for you before IMPORT-02:** (1) whether to build *Purchase mode* (accounting change); (2) the default match priority (SKU → barcode → name is current); (3) maximum file size and rows (suggested 10 MB / 20,000 rows); (4) whether "valid rows only" should be the default instead of all-or-nothing (I recommend keeping all-or-nothing as the default because it is safer for money).

---

## 9.8 New tests for these tasks

**TEST-11 — Import fixtures and invariants**  · Branch `test-11-import`
- Generate workbooks in code (POI) for every case in IMPORT-01 plus: merged header cells, hidden sheet, empty sheet, 20,000 rows, Indian number formats, text with leading/trailing spaces, emoji and Urdu names, a file with a title row, a cash-ledger file (must be refused).
- **Invariants after any import:** (1) number of products = before + NEW rows; (2) for every product, stock = sum of its movements; (3) totals shown in the preview equal totals in the database after commit; (4) all-or-nothing leaves the database byte-for-byte unchanged on failure (compare row counts and a checksum of the key tables); (5) re-running the same import in `SET_STOCK` mode changes nothing; (6) importing, then undo (if built) restores stock.
- Property test: random mixes of NEW/UPDATE/duplicate/error rows with a fixed seed; the plan's counts must equal the commit result's counts.

**TEST-12 — Barcode quality**  · Branch `test-12-barcode`
- Uniqueness (10,000+), format, `moduleWidthMm` ≥ 0.30 mm for new piece codes on every label preset, text fits in the label, PDF opens (page count correct for N copies), EAN-13 vectors, legacy codes still resolve, alias codes resolve to the same product, Add-mode save registers `product_barcodes`.
- **Round-trip decode test:** render the barcode bitmap with `BarcodeGeneratorService`, decode it with the same ZXing `MultiFormatReader` the scanner uses (JVM-friendly with `BufferedImage`-free luminance source), assert the decoded text equals the payload for 200 random codes, and also after downscaling the bitmap to 50 % and adding mild blur, to approximate poor printing. Use this test to *prove* the old 39-character code fails at a size where the new 12-character code passes (a failing test for the old format documents the improvement).

**TEST-13 — Screens in light and dark**  · Branch `test-13-ui-matrix`
- For Bill History, Cash In, Cash Out, Edit cash entry, Add Stock, Edit Stock, Import steps: Compose UI tests that render at **font scale 1.0 and 1.3**, in **light and dark**, and assert: no text node is wrapped letter-by-letter (the "Custom" bug: assert the chip's width ≥ its text width), required buttons are visible without scrolling (Save/Cancel), no touch target < 48 dp, and that empty/error states appear.
- Optional (new dependency, approve first): screenshot testing such as Paparazzi, to compare images between commits. Without it, take the manual screenshots in 5.9 and keep them in `docs/verification/screens/`.

---

## 9.9 Decisions needed before the new tasks (the assistant must not guess)

1. **Label printer/sticker size** (A4 sheet, or thermal 50 × 25 / 38 × 25 mm?) — BARCODE-01d.
2. **Keep legacy long barcodes printable?** Recommended: add short alias codes, keep old ones scannable.
3. **Colours:** approve the proposed cream/charcoal container colours (UI-00) after seeing them on your phone.
4. **"GPAY" label:** rename the visible text to "UPI" while keeping the stored key `GPAY` (recommended), or keep both words.
5. **Import:** purchase mode yes/no; maximum file size/rows; default mode; whether to allow "valid rows only".
6. **Category handling in Add Stock:** dropdown of existing categories only, or free text with suggestions?
7. **Should Billing-role phones be able to add products at all?** (Today `addOrUpdateProductWithStock` requires the Owner role.)

## 9.10 Task index additions

| ID | Title | Severity | Size | Depends on | New dependency? |
|---|---|---|---|---|---|
| UI-00 | Theme container colours + status colours | Medium | S | — | No |
| UI-01a–c | Bill History redesign + range picker fix | Medium (visible bug) | M | UI-00, FEAT-01 | No |
| UI-02a–b | Cash In/Out dialogs | Medium | M | UI-00 | No |
| UI-03a–c | Add/Edit Stock dialog | Medium | L | UI-00, BARCODE-01c | No |
| BARCODE-01a–d | Short codes, Add-mode generate, vector labels | High (usability) | M–L | — | No |
| IMPORT-01a–d | Import correctness fixes | **High (silent corruption)** | L | — | No |
| IMPORT-02a–e | Import features (review, modes, mapping, extras) | Medium | L | IMPORT-01, SEC-03 | No |
| IMPORT-03a–d | Template/export, barcodes on import, history, undo | Medium | M | IMPORT-02, BARCODE-01 | No |
| TEST-11 | Import fixtures and invariants | — | M | IMPORT-01 | No |
| TEST-12 | Barcode quality + round-trip decode | — | M | BARCODE-01 | No |
| TEST-13 | Light/dark/large-text screen tests | — | M | UI-01…03 | Optional (Paparazzi) |

**What this addendum did not verify:** the layout was reasoned from your screenshots and the *original* source, not from the new local code; I did not run the app; barcode print quality and the "0.12 mm" figure are calculations that must be confirmed on paper with your printer and scanner; the proposed colours and wireframes are starting points, not finished design.

---
