# Ali Educational Android

Status: Phase 2 stock increments under verification, not ready for shop use.

The authoritative specification is ../ALI_EDUCATIONAL_FINAL_APP_PLAN.md.
See docs/IMPLEMENTATION_PLAN.md for confirmed choices and docs/TEST_RESULTS.md for actual results.
Do not use the older docs/ACCEPTED_SPECIFICATION.md to resolve requirements.

## Build

Use JDK 21 (available locally) or a compatible JDK, Android SDK platform 36,
and the checked-in Gradle 8.14.3 wrapper. Point JAVA_HOME and ANDROID_HOME at their installations.
Run from this directory:

    .\gradlew.bat :app:assembleDebug

Debug output: app/build/outputs/apk/debug/app-debug.apk.
The .dev application ID and TEST label isolate the prototype from the intended production app.
Real Android authentication and encrypted settings/test-draft storage are implemented.
Manual products, opening stock and stock adjustments are implemented.
Purchases/accounts, spreadsheet import and backup/recovery are not implemented yet.
The Phase 0 workflow examples were fixed illustrative text, not accounting functions.

## Phone review

Open Ali Educational TEST. Authenticate privately using fingerprint or device credentials.
Set the shop name, language/theme and lock timeout. Settings persist in the encrypted database.
Use Open offline test draft to enter disposable text; verify Saved before restarting.
This draft cannot change stock or balances. Real shop data must not be entered.
Backup remains unavailable and no backup success is claimed.

The user must authenticate personally when biometric/device-credential tests are implemented.
Never request their PIN, fingerprint data or backup passphrase in chat.

## Release gate

The current APK is debug signed and is not a release deliverable.
Release installation/update instructions will be supplied after signed upgrade, migration,
backup/restore, authentication and critical workflow checks pass.
Never uninstall or clear a real app to make an update succeed.
Keep production signing keys and recovery credentials outside this OneDrive repository.
