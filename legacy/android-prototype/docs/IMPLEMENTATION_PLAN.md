# Implementation record — 30 October 2026

## Authority and review

The top-level ALI_EDUCATIONAL_FINAL_APP_PLAN.md is authoritative. SHA256:
B6CB84847C103991B1758E776BCED220F188811AA2590313CDC59285EBB91060.
The plan, UI_UX_SPECIFICATION.md and THEME_GUIDE.md match their ZIP entries byte for byte.
The existing android/docs/ACCEPTED_SPECIFICATION.md is an older copy and must not override the final plan.
Earlier login/sync mockups are visual references only. The original logo was visually inspected;
production transparency, monochrome receipt artwork and adaptive icon remain unfinished.

## Confirmed by user in this chat

- Native Kotlin / Compose, Room transactional local database; Android 11+.
- Configurable one-minute inactivity/background lock.
- Whole pieces and packs only.
- Manual product entry first (confirmed 30 October). Excel product import remains in scope for a later supplied product sheet.
- The re-supplied Ali Educational 29-Oct-2026.xls is the income/expense ledger, not a product catalog. It is read-only test material, not authorization to post old transactions or create stock.
- Encrypted local database and encrypted portable manual backups first;
  optional Google Drive scheduling before release. Recovery passphrase stays private.
- Proportional delivery allocation and moving weighted-average costing.
- Confirmed exception rules: zero-value purchases require explicit delivery shares that reconcile exactly; negative/unknown costs stay unresolved until an explicit audited correction.
- Backdated corrections retain their original business date but post their effects now. Preserve historical cost snapshots; do not silently replay later transactions.

## Architecture

- Compose screens, ViewModels, a domain calculation layer, repositories and Room.
- Atomic posting of transaction header, lines, movements, cost snapshots and account entries.
- Stable IDs and idempotency keys; drafts never post stock or money.
- Integer paise for money, higher-precision decimal unit cost; deterministic allocation residuals.
- Save sale cost snapshots; no historical changes from later receipts or price edits.
- Android biometric/device credential prompt, no custom password or authentication bypass.
- Phase 1 uses Room 2.6.1 with SQLCipher Android 4.10.0 and Android Keystore key wrapping.
  Device-bound keys alone cannot recover a replacement phone: portable encrypted backup recovery is separate.
- Planned recovery: versioned backup manifest, integrity validation, attachments/settings, staged restore and recovery copy. Not implemented yet.
- Separate development application ID com.alieducational.inventory.dev, visible TEST branding.
  Release ID reserved as com.alieducational.inventory; no production seed records.
- Private release keystore outside this OneDrive workspace; never commit or distribute signing credentials.
  Release signing/recovery custody to be settled before generating a production key.

## Phase gates

0. Disposable native workflow prototype; English/Urdu and themes checked on phone. User accepted layout and requested Phase 1. Independent brother usability pilot remains pending.
1. Authentication, encrypted persistence, settings, drafts and process restart.
2. Products, packs, import preview, stock ledger and adjustments.
3. Purchases, proportional delivery, supplier/customer credit and partial payments.
4. Sales, discounts, moving average, summaries and other-money separation.
5. Receipt exports, reports, bilingual and accessibility polish.
6. Encrypted manual backup, restore, optional Drive scheduling and failure handling.
7. Target-device tests, 3,000-product performance, migration/upgrade and recovery tests,
   independent reconciliation and brother's acceptance.
Release only after critical gates pass: signed APK, checksum, installation/update instructions.

## Decisions still required at the relevant gate

- Zero-value purchases with delivery: propose explicit per-line allocation, sum enforced.
- Negative/unknown stock costs: propose visibly unresolved profit until an audited resolution.
- Backdated corrections: choose posting-order correction versus replay/revaluation before costing is implemented.
- Expected-profit default piece/pack basis.
- Product sample import columns, opening balances and optional historical ledger import scope.
- Backup schedule/retention, recovery passphrase setup, Drive authorization configuration.
- Receipt contact details and printer/paper size if physical printing is wanted.
- Confirm whether connected phone is also brother's final target phone.

## Environment

ADB detects SM-S928B with authorized debugging; Android 16 / API 36.
Primary profile has no package matching com.alieducational.
All-profile package inspection is denied by Android for protected user 150; do not bypass it.
SDK platform 36 and build-tools 36.0.0 are installed. Gradle 8.14.3 and JBR 21 are available.
The PATH Java is Java 8 and unsuitable; Android Studio bundled Java 25 is not selected.
Initial prototype dependencies are pinned, subject to compatibility verification during build.

References:
- https://developer.android.com/compose
- https://developer.android.com/training/data-storage/room
- https://developer.android.com/identity/sign-in/biometric-auth
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
