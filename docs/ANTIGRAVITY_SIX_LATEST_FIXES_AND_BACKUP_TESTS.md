# Ali Educational — Six latest fixes and backup tests

This standalone handoff covers ONLY the six latest requests below. Preserve completed earlier work. These are implementation instructions and future tests, not claims of completion. Use the current project, not an older development copy. English-only UI/receipt labels remain required; preserve Urdu product names. Receipt Preferences stays removed. Use manual on-demand Google Drive / Files saving for this release.

## Latest overrides — receipt cleanup, expandable schemes and icon

These instructions supersede conflicting earlier requirements below. Implement in the current app without undoing completed fixes.

### Receipt placement and removal of unwanted text

Latest user-supplied receipt reference:
`C:/Users/Musid/Downloads/Receipt_AE-M-20261101-1036.pdf`

The user reports that placement is STILL incorrect. This documentation update does not establish the precise layout defect from an independent PDF inspection. Antigravity must render and inspect this actual PDF, identify the remaining alignment/spacing issues and compare corrected generated output before declaring completion.

- Remove the visible “Not GST registered” statement from the receipt header/body wherever generated. This is a presentation change only: do not add GST charges, GSTIN or a GST Tax Invoice label, and do not change the shop's underlying tax status.
- Remove the footer asking the customer to retain/keep the bill, including equivalent wording such as “Please retain this bill”. Do not replace it with another retention instruction.
- Remove the empty layout space left by these lines so the header/body/footer remain compact and balanced.
- Correct the actual logo/shop-heading alignment, receipt metadata, item columns, totals/payment placement and margins after inspecting the PDF. Preserve all necessary bill information and exact amounts.
- Use measured layout constraints and text wrapping instead of guessed absolute positions that collide on longer content. Verify short, long and multi-page receipts, English labels and mixed/Urdu product names.
- Apply the cleanup consistently to receipt preview, PDF, WhatsApp attachment, saved copy and printing. Regenerate from the selected saved bill; do not alter historical transaction data.
- Deliver before/after rendered evidence from the supplied example with a short explanation of the specific placement corrections. A successful share alone does not prove the layout is fixed.

Tests: assert both unwanted statements are absent in generated receipt text; visually inspect no blank reserved bands/overlaps; verify logo proportions and aligned monetary columns; compare item/payment totals with stored records; test long names, multiple pages, mixed services/products and a fresh WhatsApp attachment rather than a cached old PDF.

### Expandable pigmy schemes

There are THREE schemes today, but three is NOT a permanent app limit. Replace any fixed-three storage, UI or validation assumptions with a data-driven scheme list.

- Owner can use Add scheme to create more schemes in future, entering a name and usual deposit amount. No real scheme names or amounts may be fabricated.
- Each new scheme gets a stable unique ID and the same separate history, totals, add-deposit action, date filters and linked Cash In/Out behavior as existing schemes.
- Validate required name and positive usual amount; warn about duplicate names without confusing scheme identity. Keep names editable and preserve references when renamed.
- Keep the combined summary at the top and use a scrollable list of readable cards/rows; do not cram all schemes into a single horizontal row.
- Provide archive/reactivate for schemes with history instead of destructive deletion. Archived schemes remain accessible in historical reports and included in all-scheme historical totals for the selected period, but do not accept new deposits until reactivated. Clearly label active-only filters if offered.
- Migration keeps the current three schemes and every deposit/link intact. Adding a fourth scheme must not reset the first three or create any payment automatically.
- Backup/restore, exports, permissions and applicable sync must support any recorded number of schemes without hardcoded indexes or exactly-three validation.

Tests: add a fourth and fifth scheme; deposit in each and reconcile individual/combined totals and cash links; rename/archive/reactivate; verify archived history remains counted correctly; restore five schemes with all IDs/links; test long names and enough schemes to require scrolling; duplicate taps must not create unintended duplicates.

### Replace piggy icon with Bitcoin-style icon

Replace the piggy/piggy-bank icon used for Pigmy Daily and its scheme screens with a clean Bitcoin-style circular coin icon containing the recognizable B symbol with vertical strokes, as requested.

- This is a visual icon change only. Keep labels “Pigmy” or “Pigmy savings” and all amounts in rupees. Do not rename the feature Bitcoin, convert deposits to cryptocurrency, add wallets/trading or introduce crypto calculations.
- Use a locally bundled vector asset or an appropriate existing icon asset, with gold/charcoal/white colors consistent with the current theme and readable contrast in light/dark modes.
- Apply consistently to the Home shortcut and relevant Pigmy screens. Accessibility descriptions must describe “Pigmy savings”, not a Bitcoin account.
- Verify sharp rendering, adequate touch-target spacing, no missing asset in release/minified builds and no remaining piggy-bank icon in this feature.

## Latest approved update — three pigmy savings schemes

This section overrides all older single-scheme wording in F1 and earlier specifications. The user confirmed THREE pigmy savings accounts/schemes. Preserve the deposits-only scope and explicit payment confirmation.

### Display and entry

- Let the Owner name each of the three schemes and configure its own usual daily deposit amount. Do not invent bank names, account details or real amounts.
- Pigmy Home shows three clearly labelled cards, each with scheme name, usual amount, today's confirmed deposit total, total recorded deposits and an Add deposit action. Also show a clearly labelled combined total across all three.
- Totals are recorded deposits, not verified bank balances; no inferred interest, withdrawals or maturity payouts.
- Tapping a scheme opens its own dated history and totals. Provide All schemes and individual-scheme filters, with the active date range visible.
- Add deposit from a card preselects that scheme and prefills its usual amount. Allow editing the amount/date and optional note before confirmation. A general Add deposit action requires explicit scheme selection.
- Never infer that all three were paid because one was paid. Opening a form or changing a usual amount creates no transaction. Warn about possible same-day duplicates without blocking a legitimate additional deposit.
- Keep layout readable on a phone: stack cards if needed; do not squeeze three cards into unreadable columns.

### Linked Cash In/Out behavior

- Save stable scheme ID, deposit ID and linked cash-transfer ID. One confirmed cash deposit creates exactly one Cash Out entry labelled with its scheme, for example “Pigmy — [scheme name]”.
- Persist the deposit and linked transfer atomically. A bank-funded deposit must not reduce physical shop cash; retain the source clearly.
- Edit/cancel updates or reverses the linked entry once. Reassigning a mistaken scheme changes attribution without creating another cash outflow.
- These are savings transfers, excluded from operating expenses and profit. Combined and per-scheme totals must reconcile to active linked transfers for matching dates/sources.
- Renaming a scheme preserves relationships and history. A scheme with recorded deposits cannot be hard-deleted in a way that orphans records.

### Existing data and backup

- Migrate existing single-scheme records without copying them into all three schemes. Preserve IDs and cash links. If their correct scheme is unknown, retain a clearly labelled legacy/unassigned group until the Owner assigns them; do not guess or recreate cash effects.
- Include schemes, usual amounts, deposits, audit history and transfer links in backup/restore and applicable sync. Maintain Owner-only administration and prevent duplicate effects during retries or restore reconciliation.
- Manual on-demand Save Copy to Google Drive / Files remains the selected release approach. Do not add unattended OAuth-based upload work for this release. Keep local-backup and external-save status distinct.

### Mandatory additional tests

Use synthetic schemes A/B/C with usual amounts ₹100/₹200/₹300; these are test values, never production defaults.

1. Save one cash deposit into each: individual totals ₹100/₹200/₹300; combined ₹600; exactly three linked Cash Out transfers totalling ₹600; no operating-expense/profit change.
2. Deposit into A only on another day: B/C must not appear paid that day. Verify date-filtered and all-time totals independently.
3. Edit B's deposit from ₹200 to ₹250: B total ₹250 and combined ₹650, with no extra transfer. Cancel C: active combined ₹350, with C's history retained and its outflow reversed once.
4. Reassign A's deposit to B: overall cash movement stays unchanged; scheme attribution/totals change correctly.
5. Double tap/retry/restart: no duplicate deposit or cash effect. Cancelling an unsaved form changes nothing.
6. Rename a scheme, filter histories and export: attribution remains correct. Test invalid amounts, missing scheme selection and long scheme names.
7. Upgrade old single-scheme data: original deposits/IDs/links survive once; unresolved attribution is visible; no fabricated backdated cash entries.
8. Extend the F6 Excel/backup scenario with all three schemes. Restore separately and compare per-scheme/combined totals, settings, IDs and linked entries. Existing F6 inventory expectations remain unchanged by additional savings transfers.

## Mandatory testing checkpoint before production installation approval

The installation prompt in the screenshot is a request for approval, not evidence that testing passed. Antigravity must complete this checkpoint BEFORE asking to install over the live app.

1. Identify the current production package/version/schema and candidate APK version/code/signing identity. Preserve existing source work. Record the candidate SHA-256 and source revision so the proposed APK can be matched to the tested build.
2. Use an isolated test app/database/device. Never uninstall the production app, clear its storage, restore over live records or import synthetic transactions into the real shop database. A connected phone does not authorize production installation.
3. Execute every F1–F6 acceptance test below using actual screen actions. Check Pigmy navigation/save, custom cash amounts and layout, backdated date/time entry, generated receipt placement, Drive behavior and full Excel import/backup/restore.
4. Create the F6 synthetic Excel fixtures and expected-value manifest. Restore the backup into an independent test database; compare every seeded record, ID, relationship, stock quantity, balance, date, setting and attachment. A ZIP filename, nonzero size or success toast is not proof of recoverability.
5. Establish a verified recoverable backup of current production data before any live update. Validate a private copy only in isolation; do not modify live records for testing.
6. Rehearse migration from the currently installed supported schema/build using representative existing records. Verify historical bills, IDs, balances, stock, settings and attachments survive. Exercise migration failure/low storage without destructive fallback.
7. Test Android-specific paths on a real device using a release-equivalent/minified build: PDF fonts/logo, content URIs, picker permissions, share attachment, Backup Now and restoration. Explain any difference between the isolated test variant and production candidate. A JVM/debug-only result is insufficient.
8. Run affected billing/reporting regressions, including product/service mixed bills where present, discounts versus credit, change, exactly-once stock effects, savings-transfer classification, restart and offline behavior. If sync is affected, run two-device tests; list unavailable hardware tests as unverified.
9. Verify compatible signing and incremented version code. Do not uninstall to bypass an incompatible signature. Do not package test records, private financial files or signing secrets in the release APK.
10. Present a test table with ID, expected/actual results, build, device, pass/fail/unverified and evidence. Include corrected phone screenshots, actual PDF samples, Excel fixtures, pre/post-restore comparisons, migration results, backup location/time/coverage, candidate APK hash and remaining issues.
11. Fix critical crashes, failed backup recovery/migrations and unexplained money/stock mismatches before requesting installation. Never treat a checklist or successful compilation as proof that all tests passed. After code/build changes, rerun affected checks and identify the new candidate.
12. Prepare a practical recovery procedure accounting for records created after a backup. An older APK is not automatically a safe rollback after schema changes.
13. Only then request explicit approval for installation on the named production device/package. Identify the verified backup and exact APK; preserve storage without uninstalling/clearing data. Do not silently substitute a different unverified build after approval.
14. After approved installation, perform nondestructive smoke checks of launch/unlock, existing record counts/totals, navigation, settings and backup visibility. Do not create real test sales or reset data to resolve failures.

## Latest fixes and detailed acceptance tests

### F1. Pigmy Daily button opens nothing — confirmed user reproduction

The user confirmed that tapping Pigmy Daily on Home does not open any screen. Trace the Home click callback, navigation destination, route registration, role checks and screen initialization. Repair the actual path; do not substitute a toast or placeholder screen.

- Owner tap opens the working Pigmy screen with recorded deposit totals, history and Add deposit.
- Back navigation returns correctly. Empty history must still allow the first deposit.
- Preserve the already-approved single scheme, editable usual amount, explicit payment confirmation and deposits-only scope.
- Confirming a cash deposit saves one deposit and one linked savings-transfer outflow atomically, excluded from operating expenses/profit.
- Test Home navigation, first-time setup, first deposit, restart persistence, repeated taps, edit/cancel and linked Cash In/Out reconciliation. Test Owner permissions and actual release build.

### F2. Cash Received layout and any entered amount

The supplied Payment & Change screenshot shows Exact, +10, +50, +100 and ₹500 broken into multiple lines, and a crowded Customer Credit option. Fix responsive placement rather than reducing font sizes.

- Make Cash Received a clearly editable, full-width amount field with a decimal numeric keyboard. The user must be able to enter any valid amount, not only quick-button values. “Random amount” means custom/manual amount, not randomly generated money.
- Support clearing/replacing the current value, decimal paise, paste validation and an explicit zero when allowed by the payment/credit flow. Reject malformed/negative values and excess precision with clear feedback.
- Use a wrapping row or two-row grid with adequate minimum widths and touch targets. Keep Exact and currency amounts intact; never wrap individual digits or letters vertically.
- Define quick actions clearly: Exact sets the payable amount, +₹10/+₹50/+₹100 add the displayed increment, and ₹500 sets ₹500. Do not mix additive/set behavior without explicit labels.
- Arrange payment methods in a responsive grid or separate rows; Customer Credit must be readable without overlapping adjacent controls.
- Update change/due immediately. Underpayment requires the approved discount-versus-customer-credit choice; do not silently settle a short payment.
- Keep Save bill accessible with the keyboard open, prevent double posting and preserve entered amounts through validation errors.
- The screenshot's Receipt after saving dropdown must not revive removed receipt preferences or automatic sharing. Follow the latest receipt-preview/action flow and English-only receipt labels.

Tests: ₹48 bill paid ₹48, ₹50, ₹100, ₹47.50 and custom ₹123.45; verify change ₹0, ₹2, ₹52, explicit ₹0.50 shortfall and ₹75.45 respectively. Test blank input, zero, malformed paste, decimal input, Exact, each increment/set button, split payments, large system fonts, narrow screens and keyboard visibility. No save may produce different amounts from preview.

### F3. Cash In/Out entry date and time

Use the supplied income/expense-entry image as a workflow reference, retaining Ali Educational's existing visual theme.

- Both Cash In and Cash Out forms need visible editable Date and Time fields with calendar/time pickers.
- Default to the current local date/time when creating a new entry. Allow earlier dates/times for recording past payments.
- Retain the selected date/time when editing, after validation errors and through screen recreation. Saving must not silently replace it with the current time.
- Store effective transaction time separately from created/updated audit timestamps. Use the shop timezone consistently for display, filtering and exports.
- Future-dated entries must not silently appear as completed historical cash: warn and require review, or agree on the future-entry policy before implementation.
- History ordering, daily/weekly/monthly filters, exports, backup/restore and report totals must use the effective date/time consistently.
- If implementing Save and continue, clearly retain the selected date while resetting amount/description; show the date prominently to avoid accidental backdating of subsequent entries. Do not create duplicates.

Tests: backdated cash in/out, editing time without changing amount, crossing midnight/month boundaries, 12-hour AM/PM correctness, device 24-hour preference, cancellation of picker, restart and export/re-import preservation. Audit creation time must remain distinguishable from the transaction date.

### F4. Receipt placement refinement

The user likes the receipt overall but reports incorrect placement. Preserve the approved branding/content and improve alignment. Inspect and render the actual supplied reference PDF before deciding which elements are misplaced:

`C:/Users/Musid/Downloads/Receipt_AE-M-20261101-3541.pdf`

The PDF's precise layout defect has not been independently diagnosed in this documentation update. Do not claim it has been fixed based only on the payment-screen screenshot.

- Render every page and identify actual clipping, spacing, alignment, logo/header proportions and table/totals placement problems.
- Use consistent page margins, a compact proportionate logo and aligned shop header; align bill metadata without collisions.
- Allocate stable column widths for description, quantity/unit, rate and amount. Right-align money, wrap descriptions and keep amounts associated with their rows.
- Place subtotal/discount/payable/paid/due/change together with clear spacing and emphasis; keep them within the printable area and paginate when necessary.
- Preserve English headings and Urdu/mixed-script product names as previously agreed. No Urdu receipt-language option.
- Use the same layout/model for preview, PDF sharing and printing, with appropriate paper-size adaptation.
- Verify the exact sample bill after changes, plus short, long/multi-page, mixed service/product, pack and partially paid examples. Compare the actual generated PDF and WhatsApp attachment, not a design mockup. Do not send test documents to real customers.

### F5. Save backups directly to the brother's Google Drive

Add a clearly identified Drive destination while keeping local backup and cloud upload status separate.

- Minimum required flow: Back up now → create and validate complete backup → Save a copy → choose Google Drive/account/folder through supported Android document-provider handling where available.
- If Drive is not available in the document picker, explain how to enable/install/select its provider and offer a supported fallback; do not silently save locally and claim it reached Drive.
- Clarify with the user whether automatic uploads to a remembered Drive folder/schedule are also required. One-time Save a copy and unattended recurring uploads are different capabilities.
- For automatic uploads, use supported Google authentication/API integration, minimal permissions and secure credential storage, subject to current platform requirements. Do not collect the Google password, hardcode tokens or assume picker access guarantees background cloud uploads.
- Display destination account/folder where available, backup filename, local creation/verification time and separately confirmed upload/save status. Do not claim cloud delivery from a share-sheet launch alone; provider handoff may have pending offline upload.
- Handle offline state, sign-out, expired/revoked permission, storage quota, network interruption, duplicate retries and account changes. Keep the valid local copy until remote handling is safely resolved.
- Keep dated backups; do not overwrite/delete older Drive copies without an explicit retention policy.
- Provide Restore from file/Drive using supported selection and full staging/validation. Never restore automatically merely because a Drive file was selected.
- Drive remains a backup destination, not the live database or a replacement for local two-phone sync. State which pending billing-phone records are not yet included.

Tests: correct account/folder, readable downloaded archive, offline provider behavior, interrupted/retried upload, revoked access, unavailable Drive, two consecutive backups, and restoring a downloaded Drive copy into an isolated app. Verify the downloaded bytes/integrity and restored records, not only the existence of a filename.

### F6. Full Excel import → backup → restore evidence

Antigravity must create synthetic test workbooks and execute this plan in a separate test app/database. Do not import test records into live shop data, clear production storage or restore over production records.

1. Create `backup_stock_fixture.xlsx` with documented column mappings and these sample products (all amounts in rupees):

| Product | Opening pieces | Pack size | Unit cost | Piece selling price | Pack selling price |
|---|---:|---:|---:|---:|---:|
| Test Notebook | 20 | 1 | 50.00 | 80.00 | 80.00 |
| Test Blue Pen | 100 | 10 | 5.00 | 8.00 | 75.00 |
| Test Urdu-name product — قلم | 12 | 1 | 10.25 | 15.50 | 15.50 |

Include a text SKU/barcode with leading zeros and optional blank fields. Expected opening pieces: 132. Expected known inventory cost: ₹1,623.00. These are synthetic test values, never production defaults.

2. Create a separate cash-ledger workbook using the supplied header-on-row-5 layout, with dated income ₹1,000.25 and outflow ₹250.50, meaningful Notes, an opening-balance row clearly classified as informational for this test, and a totals row. Expected transaction count 2 and net movement ₹749.75; opening/totals rows must not become transactions.
3. Import both through their actual respective UI flows. Compare persisted rows, identifiers, quantities, exact money, dates and warnings to the fixture manifest. Repeat selection/confirmation to test duplicate handling.
4. Complete a product sale of 2 Test Notebooks at ₹80 each, paid ₹200 cash: total ₹160, change ₹40, notebook stock 18 and known COGS ₹100. Save its receipt and record identifiers.
5. Record one confirmed ₹100 cash pigmy deposit and give away one Test Blue Pen. Expected pen stock 99; the deposit has exactly one linked savings outflow and giveaway no sales revenue. Inventory after sale/giveaway: 129 pieces, known value ₹1,518.00. Verify financial classifications independently.
6. Seed representative customer/supplier credit and partial payments, a service bill, an Owner service expense, held bill, settings, branding and attachments where supported. Record exact expected values/relationships in a machine-readable manifest; do not rely only on overall record counts.
7. Tap the real Backup Now button. Verify complete snapshot, valid manifest, included attachments/settings, size, visible result and correct last-success timestamp.
8. Save the archive locally and to Drive as applicable, then retrieve the Drive copy. Validate the actual archive contents and checksums/integrity, not just a success toast.
9. Restore into a different empty disposable test database/install. Compare every seeded entity, ID, relationship, snapshot, balance, timestamp, product name, setting and attachment to the pre-backup manifest. Reopen the app and generate the historical receipt.
10. Add one new test transaction after the first backup, create a second backup and verify each dated backup restores its own expected state. Do not confuse successful recovery of an older file with inclusion of the newest transaction.
11. Repeat failure cases: wrong file, truncated ZIP, missing DB/attachment, unsupported schema, storage exhaustion and interruption. Failed restore must preserve the previous working test data and offer recovery.
12. Where two-phone sync is implemented, test backup completeness with pending bills, acknowledgements and restore reconciliation. Never lose or double-apply a bill during restore/resync.
13. Deliver the synthetic Excel files, expected-value manifest, test results and evidence showing actual restored contents. Mark every unexecuted step unverified. A ZIP containing `.db` and `manifest.json` alone does not prove full backup coverage.

