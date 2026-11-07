# Phase 1 security and persistence design

Implementation under test; this is not a security certification or a release approval.

- Android platform BiometricPrompt accepts strong biometrics or device credentials on API 30+.
  A separate device-credential action supports direct PIN/pattern/password fallback.
  No application password, fingerprint template, PIN capture or debug unlock bypass exists.
- The UI is covered whenever the activity pauses. Process start is always locked.
  A retained ViewModel avoids relying on serialized unlocked state across activity recreation.
- Foreground inactivity and time in the background use elapsedRealtime, not wall-clock time.
  Default is 60 seconds, configurable to 30/60/300 seconds. Late input cannot revive an expired session.
- Settings and the Phase 1 test draft use Room schema 1 over SQLCipher.
  A random 256-bit database password is wrapped with AES-256-GCM in Android Keystore.
  The wrapping key requires strong biometric/device-credential authentication within 300 seconds.
  This Keystore window is independent of the app's shorter UI session timeout.
- Wrapped key is in noBackupFilesDir, written atomically; key material is never logged or exported.
  Database connections close when locked; retained password byte arrays are zeroed afterward.
  Missing/invalid keys fail closed without regenerating an existing database.
- System cloud backup and device transfer exclude app data. Portable recovery is NOT implemented yet.
  Clearing app data, uninstalling, losing a device key or losing the phone can currently lose data.
  Therefore this build accepts only disposable test data.
- FLAG_SECURE prevents app screenshots/recents disclosure. Device checks can inspect app UI semantics;
  fingerprint and credential entry are always performed privately by the user.
- Settings and draft writes run on one worker. Saved appears only after successful persistence.
  Drafts are distinct records and never affect stock or balances.
- Room schema snapshots are exported; destructive migration fallback is prohibited.
  Actual upgrades with stock/accounts/attachments are still untested.

Dependencies for this compatibility baseline: Room 2.6.1 (Java annotation processing),
SQLCipher Android 4.10.0, SQLite support 2.4.0. Review dependency updates and native-library
page-size support before release. Sources: Android Keystore/BiometricPrompt docs and
https://github.com/sqlcipher/sqlcipher-android/tree/v4.10.0.

Native fingerprint success was confirmed by the user and corroborated by the app's Biometric
authentication result; settings and a disposable draft saved successfully.
Force-stop/relaunch returned to the locked screen. The actual shop.db header is non-plaintext;
the separately instrumented database tests verified wrong-key rejection and correct-key recovery.

Device-credential-only unlock is now corroborated by the native authentication result shown in UI.
The disposable draft survived force-stop and protected reopen. The default foreground inactivity
timeout locked the app during a coarse 64-second observation interval. A five-second background
switch retained the authenticated session without another prompt.

Returning after an 85-second background interval required authentication again.

Cancelling the credential prompt left the app locked. With airplane mode enabled and Wi-Fi disabled,
the test draft could be read and edited. The exact offline edit survived force-stop/restart and
user fingerprint authentication, with offline settings rechecked after reopening.

Required next evidence: failed biometric attempt, missing-key recovery,
no enrollment and no secure lock on a disposable device/emulator, and encrypted portable recovery.
Do not change the user's actual enrolled fingerprints or secure lock merely to test absent enrollment.
