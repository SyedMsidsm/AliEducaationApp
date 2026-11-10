# Ali Educational App - Project Invariants and Safety Rules

This document defines non-negotiable rules for all AI assistants and developers working on the Ali Educational Android app (`com.alieducational.app`). Every agent working in this repository must strictly adhere to these guidelines.

---

## 1. Project Context
- **Workspace Location**: `C:\Users\Musid\.gemini\antigravity\scratch\AliEducational`
- **Application ID**: `com.alieducational.app`
- **Current Version**: `1.0.17` (versionCode `17`)
- **Room Database Schema**: Version `9` (schemas 1–9 exported in `app/schemas/`)
- **Active Git Branch**: `main` (synchronized with `https://github.com/SyedMsidsm/AliEducaationApp.git`)

---

## 2. Shop Data Safety & Database Integrity (STRICT)
- **NEVER use destructive database migration**: `fallbackToDestructiveMigration` must NEVER be added to `AppDatabase.kt` or `DatabaseModule.kt`.
- **NEVER clear or wipe data on devices**: Upgrades on user or shop devices must preserve all existing products, inventory movements, sales, bills, customer balances, supplier ledgers, expenses, and Pigmy savings records.
- **Soft-delete with archiving**: Products and services with transaction or billing history must be soft-deleted (`isArchived = true`), preserving all past bills and reports. Only unused records with zero transaction history may be permanently deleted.
- **Do not introduce unnecessary schema bumps**: Check `Entities.kt` and `Daos.kt` before adding schema migrations. Version 9 supports `isArchived` for products and services.

---

## 3. Financial Invariants
- **Integer paise precision**: All monetary values (cost, selling price, wholesale price, bill totals, tax, discounts, paid amount, due balance, expenses, savings) MUST be calculated and stored as integer `Long` paise (1 INR = 100 paise).
- **NEVER use floating point (`Double` or `Float`) for financial math or balance calculations**.
- **Preserve historical prices and costs**: Past sales must always retain their recorded unit cost and sale-time pricing (`CostCalculator.kt`), preventing retroactive financial drift.

---

## 4. UI & Feature Stability
- **Preserve existing features & layouts**: Do not redesign screens, change established navigation flows, or replace working features unless explicitly requested by the user.
- **Font Scaling Independence**: App-chosen font size (`Theme.kt`) must operate independently of the phone's system font size without compounding multipliers.
- **Viewport Insets & Keyboard Clearance**: All forms, dialogs, and lists must include soft-keyboard insets (`.imePadding()`) and bottom scroll clearance (`80.dp` / `120.dp`) so content is never obscured.
- **Accessibility Contrast**: Keep text and status badges compliant with WCAG AA (> 4.5:1 contrast) across Light and Dark themes, using semantic status tokens (`statusColors.paidFg`, `statusColors.dueFg`).

---

## 5. Verification & Testing Standards
- **Automated Tests**: Before considering any code change complete, run:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
  All 156 unit tests (including security, cost calculator, migration, and sync suites) must pass with 0 failures.
- **Release Build**: Verify release assembly without errors:
  ```powershell
  .\gradlew.bat assembleRelease
  ```

---

## 6. Deployment & Git Boundaries
- **DO NOT push to remote Git or merge branches** without explicit user confirmation.
- **DO NOT install on a connected phone** without explicit user confirmation.
- **Non-destructive install**: When installing on a physical device, ALWAYS use:
  ```powershell
  adb install -r <apk-path>
  ```
  Never uninstall the previous app or use `-d` (downgrade) or clear data flags.
