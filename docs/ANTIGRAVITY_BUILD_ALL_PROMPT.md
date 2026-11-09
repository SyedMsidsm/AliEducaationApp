# Ali Educational — Build Everything, Then Test (instructions for the coding agent)

Place this file at `docs/ANTIGRAVITY_BUILD_ALL_PROMPT.md`. It is read together with
`docs/ALI_EDUCATIONAL_HARDENING_TESTING_AND_FEATURES_PLAN.md` (called "the plan" below).

## 0. Mission

You are the single coding agent working on the Ali Educational Android app (Kotlin, Jetpack Compose,
Room, Hilt). It handles real shop money. In this run you will:

1. **BUILD** every feature in plan **Section 9** (phases P1 to P8 below) and update the app.
2. **TEST afterwards** (phase T), following plan Sections 5 and 9.8.
3. **Report** (phase R). The owner will then review the app by hand and tell you what to fix.

The owner chose "build first, test after". So: **do not write or run tests during the build phases**,
except the compile check described in rule 6. All testing happens in phase T.

## 1. Ground rules (they override anything in the plan that conflicts)

1. **Local code wins.** The plan's code snippets were written against an older GitHub version. Before each
   phase, open the *current local* files named in that phase and quote the lines you will change. If a
   function, class or field named in the plan does not exist locally, grep for the nearest equivalent, adapt,
   and record the difference under "Deviations" in `docs/verification/BUILD_PROGRESS.md`. Never invent APIs.
2. **Keep every existing feature working**, especially: bulk void and multi-select in Bill History and Cash
   tracker, step-up authentication (`StepUpAuth`), per-bill sync results, encrypted backups, the lock screen,
   theme-aware logo and landing screen, the Owner/Billing roles.
3. **No new dependencies. No Room entity changes. No database migration.** (Section 9 needs none.)
   Mappings are stored in the existing DataStore; import history reads the existing `import_batches` table.
4. **Public APIs stay compatible.** You may add functions, overloads and optional parameters. Do not change
   existing signatures that other code calls. In particular `IntegratedAddStockProductDialog.onSave(...)`
   keeps its exact signature. Stored keys (for example payment key `GPAY`) never change.
5. **Money is `Long` paise.** Never use `Double` or `Float` for money. Parse with `CostCalculator.parseRupeesToPaise`
   or `BigDecimal`. Never truncate with `(x * 100).toLong()`.
6. **Compile after every phase** with `.\gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug`.
   Compiling is not testing. If a phase does not compile after reasonable attempts, STOP and report; do not
   pile more changes on top of a broken build.
7. **Git:** work on the branch `build-section9`. Make **one commit per phase** with the message given in the
   phase. Do not squash. **Do not push, merge, or touch `main`.**
8. **Safety:** never install on, uninstall from, or clear data of a real phone. Use an emulator or debug build
   only. Never commit `keystore.properties`, `*.jks`, backups, databases, APKs or real customer data. Do not
   build a release APK. Do not touch signing config.
9. **Small, targeted edits.** No reformatting, no renaming, no drive-by refactors. Do not edit files that a
   phase does not need.
10. **Keep a progress file.** After every phase update `docs/verification/BUILD_PROGRESS.md` (phase done /
    commit hash / deviations / anything uncertain). If your context is reset, re-read this file, the plan
    sections for the next phase, and `BUILD_PROGRESS.md`, then continue from the first unfinished phase.
11. **Be honest.** Never say something works, compiles, or passes unless you ran it and pasted the output.
    Write "NOT RUN" when you could not run something.

## 2. Decisions already made (defaults; the owner may change them later)

| Topic | Decision for this build |
|---|---|
| Label sizes | Presets: A4 3x8 (current, default), A4 4x10, thermal 50x25 mm, thermal 38x25 mm |
| Old long barcodes | Keep scannable. New codes use the short format. Offer "Add short barcode" alias for legacy products |
| Colours | Use the proposed values in plan 9.2 (UI-00) |
| Payment label | Visible text "UPI"; stored key stays `GPAY` |
| Import | All-or-nothing is the default; "Import valid rows only" is an optional switch. Limits 10 MB and 20,000 rows. Match order SKU, then barcode, then name |
| Import: NOT in this run | Purchase mode (supplier invoice accounting), Undo last import, multi-sheet import. Leave a clearly marked TODO only |
| Add Stock category | Dropdown of existing categories plus "New category..." |
| Billing role | Cannot add products (unchanged) |
| Step-up auth | Required before import commit and before bulk barcode generation, using the existing `StepUpAuth` |
| Version | In the last build commit bump `versionCode` by 1 and the patch part of `versionName` |

## 3. BUILD PHASES

For each phase: read the listed plan section, open the local files, implement, compile, update the progress
file, commit. Do the phases in this order.

### P0 — Setup (no feature code)
- `git status` must be clean. If it is not, commit the existing work to a branch `wip-before-section9`,
  tell me, then continue from that state.
- `git tag pre-section9` (rollback point), then `git checkout -b build-section9`.
- Run `assembleDebug` once as the baseline. If the baseline fails, STOP and report.
- Create `docs/verification/BUILD_PROGRESS.md` with the phase checklist.
- Open the current local versions of: `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/billing/BillHistoryScreen.kt`,
  `ui/reports/CashTrackerScreen.kt` (add and edit dialogs), `ui/stock/StockScreen.kt`
  (`IntegratedAddStockProductDialog`), `ui/stock/ImportCatalogScreen.kt`, `data/service/ImportExportService.kt`,
  `data/service/BarcodeGeneratorService.kt`, `util/StepUpAuth.kt`. Note in the progress file what already
  differs from the plan's assumptions (for example features added by earlier tasks).
- Commit message: `P0: baseline and progress file`

### P1 — UI-00: theme tokens (plan 9.2)
- Add `surfaceContainerLowest..Highest` to both color schemes; add `AliStatusColors` + `LocalAliStatusColors`
  (light and dark); add `AliDimens`.
- Do not restyle any screen yet.
- Visible result: every existing dialog is warm cream in light mode (not lavender) and green-tinted charcoal in dark.
- Commit message: `P1 UI-00: theme container and status colours`

### P2 — IMPORT-01: import correctness (plan 9.7, IMPORT-01a to 01d)
Implement the staged importer: Read -> Parse -> Plan -> Commit.
- 01a: `CellReader` (POI `DataFormatter` + `FormulaEvaluator`) and `parseQty/parseMoney/parsePackSize/parseBarcode`
  returning Ok or Err with a human message. No silent defaults. Accept `₹`, `Rs`, `/-`, Indian grouping,
  `.50`, trailing `.0`; reject text and negatives.
- 01b: pass the detected header row index into the import (`ColumnMapping.headerRowIndex`, default 0).
  New `CsvReader`: BOM stripping, quoted multi-line fields, escaped quotes, delimiter sniffing (`,` `;` tab),
  charset fallback (UTF-8 then Windows-1252). Apply the 10 MB / 20,000-row limits with a friendly message.
- 01c: `PlannedRow`, `RowStatus`, `RowIssue`, `planImport(...)` with NO database writes; detect duplicates
  inside the file; `commit(plan, mode, validRowsOnly)` in one `db.withTransaction`.
- 01d: register `product_barcodes` for imported barcodes (report a conflict as an ERROR row when the barcode
  maps to a different product); `ImportMode { NEW_ONLY, ADD_STOCK, SET_STOCK, PRICES_ONLY, DETAILS_ONLY }`;
  stock movements carry the batch id as `referenceId`.
- Keep the old `importProducts(...)` signature working by delegating to the new code with sensible defaults.
- Commit message: `P2 IMPORT-01: staged, validated stock import`

### P3 — BARCODE-01: generator and labels (plan 9.6, BARCODE-01a to 01d)
- 01a: `ShopBarcode.newPayload()` (`AE-P` + 8 Crockford base-32 chars; pack suffix `-PK{n}` only for packs),
  `moduleWidthMm(...)`, `isEan13Valid(...)`. **First** grep for code that parses the old `-PC` / `-PK`
  suffixes; keep both formats working.
- 01b: `AliViewModel.suggestShopBarcode(...)` with collision retry (5 tries, checks `lookupBarcode` and
  `getProductByBarcode`). `generateAndRegisterBarcode` uses the short format for new codes, never overwrites a
  manufacturer barcode, and can create a short alias when only a legacy long AE-P code exists. Keep `requireOwner()`.
- 01c: in `IntegratedAddStockProductDialog` add the Generate icon to the barcode field (hidden when the field is
  non-empty), the barcode preview card, and "Print labels after saving". Do not change `onSave`. (UI-03 later
  restyles this dialog; keep these pieces working through that.)
- 01d: rewrite `generateLabelsPdf` to draw vector bars from the ZXing `BitMatrix`, auto-fit the human-readable
  text, support the four presets and a list of (product, barcode, price, copies) items; add a multi-product
  label sheet and a "Generate barcodes for items without one" action in the product list (owner-only,
  step-up auth, one transaction per product).
- Commit message: `P3 BARCODE-01: short shop barcodes, Add-mode generate, vector labels`

### P4 — UI-01: Bill History (plan 9.3)
- Non-wrapping scrollable period chips (`maxLines = 1`, `softWrap = false`); custom range shown in the chip with
  a clear (X); `DateRangePicker` with the UTC-to-local conversion helpers; the chip always re-opens the picker.
- Fix the blank band above the title (apply insets once). Hero summary card; status chips with counts;
  day-grouped list with sticky headers and day totals; redesigned row (status stripe, pill with icon and text,
  time, payment icon); empty state with "View this week" / "Clear filters". Use `LocalAliStatusColors`;
  remove hard-coded status colours.
- **Keep selection mode and bulk void exactly as they work now.**
- Commit message: `P4 UI-01: Bill History redesign and range-picker fix`

### P5 — UI-02: Cash In / Cash Out (plan 9.4)
- One shared `CashEntryForm` used by both the Add and the Edit dialogs: amount first (large, autofocus, decimal
  keypad, validation with `parseRupeesToPaise`), wrapped category chips (`FlowRow`), the red "Remove..." button
  moved into a "Manage" action, segmented Cash / UPI / Bank, compact date-time line, collapsed "More details"
  (extra note and receipt buttons that never wrap), pinned Save/Cancel, "Save & add another".
- Saved records must be identical to before for the same inputs. Pigmy behaviour unchanged.
- Commit message: `P5 UI-02: Cash In/Out dialogs`

### P6 — UI-03: Add / Edit Stock (plan 9.5)
- Full-screen dialog with sections (Product, Stock, Pricing, Supplier bill) and a pinned bottom bar with a live
  summary. Same fields, same `onSave` signature, same validations.
- Clear labels; blank-with-placeholder number fields, correct keyboards, `ImeAction.Next`, select-all on focus;
  exact paise math for the live preview; category dropdown (existing categories plus "New category...");
  warnings (selling below cost, selling 0, barcode already used); supplier-bill section auto-expands when
  quantity > 0; discard confirmation; edit mode shows "In stock now: N pcs" after you confirm the semantics of
  the "Pcs" field in the code.
- Commit message: `P6 UI-03: Add/Edit Stock dialog`

### P7 — IMPORT-02: import features (plan 9.7, IMPORT-02a to 02e)
- Auto-mapping with a synonym table and confidence badges; saved mappings keyed by a hash of the normalised
  headers (DataStore); a 5-step flow (file, columns, review, options, result); the Review step with the summary
  line ("adds N pcs worth Rs X at cost; a new, b updates, c skipped, d errors") computed in `Long` paise, status
  filters, and "Export error report (.xlsx)".
- Options step: mode, valid-rows-only switch, default margin (Gross margin or Markup on cost) for rows with no
  selling price, "Generate shop barcodes for rows without one". Extra columns: wholesale, boxes x pack size,
  pack price to piece price. Near-duplicate warnings (normalised names, no new library) and an old-to-new price
  table for updates. Step-up auth before commit. Progress bar with cancel; the import must survive rotation.
- Commit message: `P7 IMPORT-02: validated import flow and features`

### P8 — IMPORT-03: template, export, import extras (plan 9.7, IMPORT-03a to 03c)
- "Download template" and "Export current stock" (owner-only). Any exported text cell starting with `=`, `+`,
  `-` or `@` must be written as safe text to prevent spreadsheet formula injection.
- Barcodes-on-import and the "Print labels" button in the result step (uses P3).
- Read-only Import history screen from `import_batches` with the stored counts.
- Leave `// TODO(next run): undo last import, purchase mode, multi-sheet` and nothing more for those.
- Commit message: `P8 IMPORT-03: template, export, history`

### P9 — Wrap-up (no new features)
- Bump `versionCode` (+1) and the patch part of `versionName`.
- Update `README.md` (version, features) and fix the stale schema note mentioned in the plan (DOC-01) only if
  it is still wrong.
- Add a **Deviations** section to the progress file listing every place where local code differed from the plan.
- Confirm `assembleDebug` succeeds from a clean state (`clean assembleDebug`).
- Commit message: `P9: version bump and docs`

## 4. TEST PHASE (start only after P9 is committed)

Rules for this phase: **report every failure.** Fix a failure only when it is an obvious defect in code written
during this run; make each such fix its own commit named `fix: <what>` and list it in the report. **Never
weaken, delete, skip or loosen a test or a threshold to make it pass.** Pre-existing or unclear findings are
only reported (with the random seed, the steps and the file). Performance limits: measure first, record the
numbers, do not invent thresholds.

Order (highest risk first):
- **T0** Run the whole existing suite: `.\gradlew.bat testDebugUnitTest lintDebug`. Record the baseline
  (counts, failures, new lint errors versus the baseline file if one exists).
- **T1 TEST-11** import fixtures and invariants (plan 9.8): formula quantities, leading-zero barcodes, text
  prices (`".50"`, `"19.999"`, `"12 /-"`, `"N/A"`), `"12 pcs"`, title row above headers, BOM and semicolon and
  Windows-1252 CSV, multi-line quoted fields, duplicate rows inside a file, all-or-nothing rollback leaves the
  database unchanged, valid-rows-only counts, SET_STOCK idempotency, 20,000-row file, cash-ledger file refused.
- **T2 TEST-12** barcode quality: uniqueness over 10,000 codes, module width, label text fits, legacy codes and
  aliases resolve to the same product, Add-mode save registers `product_barcodes`, **round-trip decode with the
  scanner's ZXing reader** (including a 50% downscale and mild blur) for 200 random codes, plus a test showing
  the old 39-character code fails where the new one passes.
- **T3 TEST-04** two-phone sync fault injection (all 11 scenarios in plan 5).
- **T4 TEST-05** security tests (lock screen, rotation, step-up, PIN throttle, SecretBox, BackupCrypto, zip safety,
  FileProvider, exported components, intent fuzz, cleartext flag).
- **T5 TEST-06** backup and restore rigor (full round trip of all tables).
- **T6 TEST-07 and TEST-13** Compose UI flows and the screen matrix (light and dark, font scale 1.0 and 1.3):
  no letter-by-letter wrapping, Save/Cancel visible, touch targets >= 48 dp, cart kept across rotation, bulk
  void dialogs, Cash dialog saves identical records, Add Stock `onSave` values identical to before for 5 sample inputs.
- **T7 TEST-08** lint baseline and gate. **T8 TEST-09** secrets and dependency audit (report only; do not upgrade
  anything). **T9 TEST-10** performance and soak with a seeded large dataset (record numbers).
- **T10** `.github/workflows/ci.yml` as in plan 5.4 (do not run it).
- **T11** Run the instrumented suite if an emulator is available (`adb devices`); otherwise mark those tests
  NOT RUN and say why.

Also run the **money-invariant test (TEST-02)** and the **migration chain test (TEST-03)** if they exist; if they
do not, write them (plan 5).

## 5. FINAL REPORT (phase R)

Create `docs/verification/BUILD_REPORT_2026-11-09.md` containing:
1. **Phases:** table of P0 to P9 with commit hash and one-line result.
2. **Deviations** from the plan and the reason for each.
3. **Test results:** table per test group (written, run, passed, failed, NOT RUN) with the real counts, plus
   every failure (name, file, message, seed or steps) and any production fix commit.
4. **Findings** (things the tests exposed that were not fixed).
5. **Owner manual-check list** in this order: (a) light-mode logo and dialogs are cream, not lavender;
   (b) Bill History chips never wrap, custom range can be changed, bills near midnight on the right day;
   (c) add a new product, tap Generate, save, scan the barcode with the app scanner, print one label and scan it
   from 15 cm; (d) Cash In and Cash Out: amount first, nothing clipped, saved record correct, receipt photo works;
   (e) Add Stock: all sections reachable, preview matches saved margin; (f) import the owner's real stock sheet
   **on an emulator copy**, review the preview, check the totals line, export the error report; (g) re-import an
   edited file in each mode; (h) bulk void and multi-select still work; (i) rotate the phone on every changed
   screen; (j) dark mode on every changed screen.
6. **Not done / deferred:** purchase mode, undo import, multi-sheet import, anything skipped, anything uncertain.
7. **How to install** the debug build on an emulator (commands) and how to roll back (`git checkout pre-section9`).

When the report is written, stop and wait. Do not push. The owner will review and send a list of fixes.

## 6. Stop and ask (do not guess) if

- a phase does not compile and you cannot fix it in a few attempts;
- a change would need a database migration, an entity change, or a new dependency;
- a plan item contradicts the local code in a way that could change saved money data;
- you are about to delete or replace an existing feature that is not named in a phase.
