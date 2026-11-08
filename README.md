# Ali Educational

Android shop-management application and its design, requirements and testing documentation.

## Current application

The root Gradle project is the current application copied from the working Antigravity project on 7 November 2026. Its configuration declares version **1.0.15 (15)**, Room schema **9**, minimum SDK **26**, target/compile SDK **36**, and JVM target **17**.

Source includes inventory and purchasing, billing and held bills, customer/supplier accounts, cash and expenses, service billing, Pigmy savings, reports, receipts, spreadsheet import/export, backup/recovery, local two-phone synchronization and a slim launcher widget.

Presence in source is not certification that every workflow is correct or verified on a phone. This publication does not run a new Android build or install an update.

## Repository map

- `app/`: current Kotlin/Compose application, resources and tests.
- `gradle/`, wrapper and build scripts: current build configuration.
- `docs/`: requirements, reviews, implementation handoffs and acceptance plans. Historical proposals can conflict; later explicit decisions supersede earlier ones.
- `docs/SECURITY.md`: security architecture, RBAC, PIN lockout policy, keystore-sealed secrets, ALIBK1 streaming encrypted backups, and two-phone TLS synchronization.
- `docs/artifacts/pdf/`: PDF reports, including the complete system reference and code appendix.
- `docs/artifacts/widget-reference/`: approved widget design board (middle slim-bar concept).
- `docs/artifacts/stock-test/`: synthetic stock workbook and associated previews/generator.
- `assets/`: branding reference assets.
- `scripts/`: historical document/preview generators; some require the original local runtime and paths.
- `legacy/android-prototype/`: earlier prototype retained for history. It is **not** the current root app.

## Build and verification

Open the root project in Android Studio with an SDK installation supporting API 36 and a compatible JDK/Gradle environment. Set your SDK path locally via Android Studio or an ignored `local.properties`.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

On macOS/Linux use `./gradlew` for the equivalent commands. Dependency downloads and an appropriate Android SDK are required. Debug builds have the `.debug` application ID suffix.

Release signing is intentionally not included. The current build reads a local `keystore.properties` containing `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Obtain the existing signing material securely from the owner to produce compatible updates; never commit it. Never uninstall or clear a live installation to bypass signing or migration failures.

All Room schema artifacts (schemas 1 through 9, each 26–55 KB) are fully exported and validated in `app/schemas/com.alieducational.app.data.local.AppDatabase/`. Automated migration tests (`MigrationSqlTest`, `RoomMigrationTest`) and data integrity audits verify schema migrations up to version 9.

## Architecture

Compose screens and navigation feed ViewModels, repository/domain calculations, Room DAOs/SQLite and DataStore preferences. Services provide PDF receipts, Apache POI spreadsheets, backup packages and local synchronization. CameraX/ZXing support barcode scanning. Native widget resources route to existing app flows.

See [the detailed system reference](docs/artifacts/pdf/Ali_Educational_Complete_System_Reference_2026-11-07.pdf) and the Kotlin implementation for exact behavior. The PDF is a dated snapshot; source can evolve independently.

## Data and credentials

This repository excludes signing keys/passwords, machine-local SDK configuration, app databases, live shop backup archives, build caches and compiled APKs. It also excludes unrelated workspace material and disposable review copies. Treat local production data and authentication material separately from versioned source.

## Test status

Application test source is included. Historical documentation may describe earlier results; it does not establish current release acceptance. Follow the detailed handoffs for financial reconciliation, actual device behavior, two-phone tests and isolated full-backup restoration before calling a build final.
