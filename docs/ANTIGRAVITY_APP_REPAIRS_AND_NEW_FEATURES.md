# Ali Educational — Antigravity implementation and testing instructions

## Latest follow-up — six required fixes and end-to-end backup verification

These requirements supplement the earlier sections and take precedence where they conflict. Documentation only: the following are instructions and future tests for Antigravity, not claims of completed repairs or tests.

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

## Latest language decision — English interface and receipts

This decision supersedes EVERY earlier English/Urdu language-selector, Urdu interface, right-to-left app layout, Urdu receipt translation, and bilingual sample/test requirement anywhere in this document, including the preserved historical handoff in Section 13.

- The app interface is English only. Remove the Urdu app-language option and associated language-switching UI. Do not add a replacement language selector for a single-language app.
- Receipts, PDFs, WhatsApp receipt labels, printing, reports and exports use English headings and interface text. Remove the receipt-language selector. No translated Urdu receipt version is required.
- Retain Urdu text support specifically for product-name entry and the faithful display of those saved product names. A product may have an Urdu, English or mixed-script name. Do not translate, transliterate, strip, replace or corrupt that name.
- Urdu product names must remain readable wherever that product is shown: catalog, search results, cart, historical bills, receipts, PDF/WhatsApp attachments and exports. English receipt layout does not mean replacing Urdu product names with English.
- Use Unicode-safe storage, import/export and synchronization. Support suitable fonts and correct Urdu shaping/bidirectional rendering within product-name fields without switching the entire interface or receipt to RTL.
- Use the installed Android keyboard/input method for Urdu typing; do not build a custom keyboard. Preserve text through editing and validation.
- Migration must preserve existing Urdu product names and historical snapshots. If an old installation selected Urdu UI, move it to English safely without clearing other settings or records.
- Do not restrict or delete existing notes/customer data simply because it contains Unicode; the requested Urdu feature scope is product names, not a destructive data-cleaning rule.

### Replacement language acceptance tests

1. Upgrade an old Urdu-language installation: interface is English, language selectors are removed, unrelated settings/data survive.
2. Create/edit a product using Urdu and mixed Urdu/English names; save, restart and search using the entered text. No loss or garbled characters.
3. Bill that product and generate preview, PDF, WhatsApp attachment and print output: English labels/layout with the original correctly shaped product name, correct numbers and unchanged amounts.
4. Verify long mixed-script names wrap without clipping or reversing receipt numbers, prices or quantities.
5. Import/export, backup/restore and two-phone sync preserve product names exactly. Historical names remain unchanged after a later catalog rename.
6. No Urdu receipt selector, translated receipt template or whole-app RTL mode is offered. RC04 now tests Urdu PRODUCT NAMES on an English receipt, not an Urdu-language receipt. RC18 still checks required fonts on the actual release build.

Antigravity must reinterpret all older language-specific test rows accordingly: test English UI/output plus Unicode product-name preservation, rather than implementing the superseded bilingual interface. Deliver English sample receipts, including one with Urdu and mixed-script product names. This latest decision also overrides the earlier chat prompt asking for English/Urdu receipts.

## Master handoff — latest decisions and document order

This file now combines the repair/new-feature instructions with the **complete earlier Combined Implementation Handoff**, reproduced in Section 13. Antigravity can use this single document for both scopes; the earlier handoff is included in full, not summarized away.

Read in this order:

1. This precedence section and the preservation requirements.
2. Sections 2–9: repairs, Excel, Backup Now, reporting, pigmy, giveaways and their tests.
3. Section 12: latest receipt-settings removal and branded WhatsApp receipt requirements/tests.
4. Section 13: full two-phone, services, owner reporting, integration, testing and deployment handoff.

**Latest instructions override conflicting older wording:**

- Remove the Receipt Preferences/Receipts preferences section from Settings. Older requirements to retain receipt defaults, printer/share defaults or receipt-language defaults in Settings are superseded. Do not remove receipt generation, printing, PDF saving, WhatsApp sharing or English/Urdu selection on the receipt screen.
- Repair WhatsApp output as a properly arranged, branded receipt document, as specified in Section 12. Sending unformatted text alone does not satisfy this requirement.
- The full two-phone and service features in Section 13 are included implementation scope, not just regression checks for features that already exist. Inspect the current app, retain working implementations and implement missing approved parts without duplicating them.
- The latest pigmy and product-giveaway decisions in Sections 7–8 apply. Do not add multiple pigmy schemes, withdrawals, interest, cash donations or automatic daily deposit posting.
- Unresolved permissions/policies in the embedded handoff remain unresolved unless a later user decision explicitly settles them. Ask one question at a time before implementing the dependent policy; continue independent work.
- Section 13 retains its original internal section/phase numbering. Those numbers belong to that embedded specification. Use the integrated delivery order below to coordinate the overall work.

### Integrated delivery order

1. Identify current source, preserve existing work/data, reproduce defects and establish isolated test data.
2. Repair crashes, import/export, Backup Now and safe recovery, billing/stock/payment correctness and migration safety.
3. Apply Home/layout changes and Section 12 receipt settings/WhatsApp improvements; validate existing single-phone behavior.
4. Implement or complete service definitions and mixed billing, then the Owner-only service expense/reporting requirements in Section 13. Keep this usable without a second phone.
5. Complete Sales & Profit, pigmy deposits and product giveaways, using shared ledgers and consistent accounting classifications.
6. Implement or complete sync foundations, pairing and two-phone delivery behind explicit Owner activation. Preserve pending records and current single-phone behavior.
7. Execute all tests from Sections 3–9 and 12 plus every test in embedded Parts A, B and C. Do not replace detailed tests with the shorter overall checklist.
8. Rehearse upgrade/recovery, then prepare one compatible signed release APK and the evidence/setup/distribution handoff. Do not claim untested functionality is complete.

The presence of a connected phone is not an instruction for this chat to run anything. This chat is updating documentation only; Antigravity is the intended implementer.

## Purpose and authorization

This document specifies repairs and additions for the existing Ali Educational Android app. Give this file to Antigravity to carry out the work. It is a specification, not a statement that these features have been implemented or that the app has passed testing.

The user's current request to this chat is documentation only: do not build, install, run, or modify the app from this chat. Antigravity should implement the requirements in the actual current project, preserving existing functionality and shop records.

Use `APP_REVIEW_2026-11-01.md` alongside this document for detailed existing findings and reproduction examples. Reproduce findings against the latest source before fixing them: some may already have been addressed. Do not overwrite newer work with an older project snapshot.

## 1. Preserve the existing app and its data

1. Identify the latest project and installed app version. The previously reviewed matching project was `C:/Users/Musid/.gemini/antigravity/scratch/AliEducational`, package `com.alieducational.app`. The older workspace `android` project is not automatically the correct target.
2. Record the current source state and create a recoverable source snapshot or version-control branch before editing.
3. Obtain a verifiable backup of current shop records before any production update. A successful share dialog or nonzero ZIP size is not sufficient proof of recovery.
4. Use a separate debug application ID and disposable test database. Do not test imports, deletions, refunds, migrations or restores on the brother's live records.
5. Preserve application ID and signing identity for production updates. Never uninstall the production app or clear its data as a migration strategy. Never distribute the signing key or its passwords.
6. Use explicit, tested database migrations. Do not use destructive migration fallback for production data.
7. Preserve existing billing, inventory, accounts, service billing and two-phone features wherever already implemented. Do not create a competing second cash ledger or duplicate billing logic.
8. New records and fields must participate in backup, restore, export, relevant sync and permissions. If two-phone support exists, pigmy and giveaway management belong to the Owner role unless the user approves otherwise.
9. Make changes in phases below. Each phase needs regression checks before proceeding to release.

## 2. Confirmed scope

The user requests:

- Repair product-stock spreadsheet import and clear wrong-file validation.
- Repair Cash In/Out spreadsheet import, export and crashes.
- Improve cramped or incorrectly arranged screens.
- Remove Recent Activity from Home.
- Repair complete backups, sharing, file visibility and restoration.
- Fix the user-reported **Backup Now button does not work** issue, including its action, progress, error handling and result display.
- Improve the Gross Profit report and analysis.
- Remove Receipt Preferences from Settings and repair branded, properly arranged WhatsApp receipt documents.
- Include the complete two-phone billing/sync, services and Owner-only service expense implementation handoff in Section 13, including all its tests and preservation requirements.
- Add a pigmy daily savings-deposit tracker.
- Track products given away free or donated.
- Test every existing feature and fix reproduced errors, preserving existing data.

Keep the interface simple, suitable for phone use, and consistent with Ali Educational's existing black-and-gold theme. Retain English and Urdu support, including right-to-left Urdu layouts. No GST collection or GST-registered identity should be introduced: the shop is not GST registered.

## 3. Phase 1 — Excel and cash-ledger repairs

### 3.1 Real reference files

User-supplied references:

- `C:/Users/Musid/Downloads/Ali Educational 29-Oct-2026.xls`
- `C:/Users/Musid/Downloads/Ali Educational 31-Oct-2026.xls`

Both files were inspected and contain a `Transactions` cash-ledger sheet. They are not product-stock lists. Their header is on row 5:

`Date | Notes | Description / Category | Income | Expense | Balance`

There are title/date rows above it and a Previous Balance row below it. Transaction descriptions can be in Notes while Description / Category is blank. The first file has 180 row positions and the second 953; these are not transaction counts. Data can change: never hardcode their values, dates or record counts into the implementation.

Use copies as private test inputs. Do not bundle the shop's financial files in a shipped APK or public repository. Ask for a representative product-stock spreadsheet to complete stock-format acceptance testing. Do not infer product quantities or prices from cash-ledger amounts.

### 3.2 Shared file-handling requirements

- Support real `.xls`, `.xlsx` and CSV where advertised. A renamed non-spreadsheet must not be accepted based solely on its extension.
- Work with Android's document picker and content URIs, including files selected from local storage and a Drive document provider.
- Do file reading and parsing off the UI thread. Show progress and disable repeated import/export taps while an operation is active.
- Close streams and workbooks reliably on success, failure and cancellation.
- Handle missing permissions, inaccessible files, corrupt files, empty workbooks, encrypted files, wrong formats and low storage with an actionable message. Do not crash or leave a spinner running.
- Preserve coroutine cancellation. Do not hide failures by treating invalid data as an empty successful import.
- Detect headings below title rows, show/select worksheets when needed, and use the same selected sheet and mapping in preview and import.
- Bound file size/row processing appropriately and show a useful message when a limit is exceeded.
- Do not guess columns and write records when the file structure is unrecognized.

### 3.3 Product-stock import

Provide this flow: Choose file → validate type → choose sheet → map columns → preview rows and issues → confirm → import result.

- Recognize a cash-ledger file and explain: “This file contains Cash In/Out transactions, not product stock. Import it from Cash In/Out Tracker.” No products may be written.
- Map product name, category, brand, SKU/barcode if present, piece/pack units and pack size, quantity, purchase cost, selling prices and low-stock threshold as supported by the existing app.
- Show which fields are required and which will remain unknown/defaulted. Missing purchase cost must not silently become a known zero cost.
- Preserve barcode/SKU text and leading zeros. Do not round identifiers or put them in scientific notation.
- Parse currency prefixes (`₹`, `Rs`, `Rs.`), commas, decimals and formatted numeric cells correctly. Use exact currency rounding into paise.
- Reject invalid quantities, pack sizes and prices with row/column explanations. Do not silently replace malformed values with zero.
- Show duplicate matches by SKU/barcode and ambiguous name matches. Do not silently merge unrelated variants.
- Make the effect of any update explicit: product-detail update, setting counted stock, and receiving additional stock are different actions. Do not silently add the full opening quantity every time the same file is imported.
- Prefer a validated import batch committed atomically, with products, stock movements and batch history consistent. If partial import is supported, require an explicit choice and identify every accepted/rejected row; never call it an unqualified success.
- Prevent repeated confirmation/retry from duplicating the batch. Offer a visible duplicate warning and an explicit reviewed override for a genuinely separate import.

### 3.4 Cash In/Out import

- Recognize the supplied layout even when the title/date rows change.
- Preserve Date, Notes and Description / Category. Use meaningful Notes as the displayed description when the category is blank.
- Treat Income and Expense as separate directions. Preserve valid entries on either side; flag ambiguous rows rather than silently losing values.
- Exclude title rows, repeated headings, totals, subtotals and closing balances from transactions.
- Treat Previous/Opening Balance separately: show it in the preview and explain whether it will establish an opening balance or is informational. Never count it as new sales income or an ordinary expense.
- Validate dates strictly. Never replace an invalid historical date with today without the user explicitly correcting it.
- Use balances as reconciliation information, not transactions. Explain discrepancies without changing source amounts to force agreement.
- Show transaction counts, date range, income total, outflow total, skipped summary rows and row-level errors before confirmation.
- Detect repeat files and overlapping entries. Do not silently skip two legitimate identical payments merely because date and amount match; ambiguous duplicates need review.
- Save a batch atomically with provenance sufficient to review or reverse the import.
- Imported cash income is not automatically a new bill or sales revenue. Imported stock purchases, supplier payments and pigmy transfers are not automatically operating expenses. Add classification/review so reports do not double-count or misclassify them.
- Do not automatically create pigmy records merely because Notes contains “Pigmy”. Existing file labels may reflect historical naming. Require review and prevent a linked transfer from being imported twice.

### 3.5 Cash export and sharing

- Export exactly the selected period and filters. An empty period must show “No transactions in this period”, not export all records.
- Write numeric amount cells and real dates. Preserve time when applicable. Keep descriptions and notes readable.
- Label opening balance, period inflows/outflows and closing balance accurately. A period starting from zero is a net movement, not necessarily the actual cash balance.
- Exclude cancelled entries from active totals while allowing their history to remain accessible.
- Avoid Apache POI desktop-only font/graphics functionality such as `autoSizeColumn` on Android. Use Android-compatible export APIs and fixed readable widths.
- Verify Excel dependencies on actual Android and the release/minified build, not just JVM tests.
- Use app-owned staging files plus Android FileProvider for sharing, and the Storage Access Framework or appropriate Android storage APIs for Save a copy. Do not depend on unrestricted public-folder writes.
- Show distinct Save and Share actions, the saved filename/location when known, and errors without claiming delivery. Opening a chooser does not mean WhatsApp sent the document.
- A generated cash export must import back without turning its totals row into extra payments.

### Phase 1 acceptance tests

1. Import both supplied files into an empty disposable database and compare row-level amounts/dates and totals with the source.
2. Select both files from product import: useful wrong-file message, zero product writes.
3. Import a real stock file once supplied; test pieces, packs, optional fields, leading-zero barcodes and variants.
4. Exercise corrupt, empty, password-protected, renamed text/image, unsupported and inaccessible files.
5. Test invalid dates, `Rs. 25.50`, `₹1,000.25`, decimals, blank versus zero, formulas/cached values, repeated headers and totals.
6. Test duplicate confirmation taps, app interruption, repeat file and overlapping imports. No partial or duplicated stock/payment effects.
7. Export an empty period, one row and a large ledger. Open exported files in a spreadsheet viewer and re-import into a fresh test database.
8. Verify Save a copy, share permissions and readable attachments on the phone. Cancelled sharing must not be reported as delivered.

## 4. Phase 2 — Existing data correctness and UI repairs

Reproduce and fix the following previously identified issues where still present:

| Area | Required behavior and regression example |
|---|---|
| Product edit | Renaming/repricing a product with stock 7 leaves stock 7 and supplier dues unchanged. Receive 3 gives 10. Set counted stock to 8 records one −2 adjustment. Preserve pack settings and independent pack prices. |
| Pack billing | One 10-piece pack at ₹100 saves a ₹100 bill and removes 10 pieces. Two packs at ₹199 each save ₹398. Store selected units, pack-size snapshot and exact line amount distinctly. |
| Repeated product lines | A piece line and pack line for the same product each get their correct discount/cost allocation. Use unique line IDs, not product IDs as allocation keys. |
| Short payment | ₹90 received against ₹100 requires a choice: ₹10 discount or customer-linked credit. Never create unassigned customer debt. |
| Discount interaction | ₹60 less 10% is ₹54; taking ₹50 and discounting the remaining ₹4 must save ₹50, not replace the earlier discount. Display total discount amount and effective percentage. |
| Cash and change | Exact on ₹80.25 records ₹80.25. ₹100 tendered for ₹80.25 shows ₹19.75 change. Distinguish tendered cash, applied payment and change. |
| Returns and voids | Restore stock once. A damaged return must not subtract already-sold sellable stock a second time. Reconcile customer payments made after the sale; avoid a negative credit balance or incorrect refund. |
| Held bills | Resuming does not delete the only durable copy. Process death before completing a resumed bill must allow recovery. |
| Account payments | Prevent duplicate saves, handle overpayment explicitly and reconcile invoice/account balances. Do not hide outstanding dues by archiving an account. |
| Barcode lookup | Edited mappings cannot return stale products; archived items must not be silently billed. Use the correct pack mapping and preserve scanning/manual search workflows. |
| Invoice numbers | Generate unique durable numbers that remain safe offline and across existing sync. Do not rely on a small random number space. |
| Receipts | Use persisted sale ID/date/amounts after restart. Show paid/due/discount correctly. Real printing must invoke supported Android printing; do not show a fake success toast. Paginate long receipts and retain approved English/Urdu choices. |
| App lock | Respect configured fingerprint/device credential behavior on cold launch and resume. Avoid showing private content before lock state is loaded. Respect configured timeout. |

### Layout and Home

- Remove the Recent Activity section from Home only. Preserve stock movement/audit records and their relevant history screens.
- Keep Settings as a visible gear icon.
- Review every screen on the actual phone size and at larger system font settings.
- Fix vertically broken labels, cramped payment buttons, overlapping fields, clipped totals and controls hidden by the keyboard.
- Use wrapping rows or appropriately sized grids for quick-payment actions. Do not shrink text to hide layout defects.
- Keep important actions easy to find and consistent. Destructive actions need a clear effect and appropriate confirmation.
- Use reversal/archive where deletion would damage financial or inventory history; allow straightforward deletion of safe unsaved drafts. Explain this distinction in the UI.
- Verify dark/light themes, English/Urdu, right-to-left ordering, scrolling, back navigation, empty states and validation messages.

## 5. Phase 3 — Complete backup and restore

### Explicit reported defect — Backup Now does not work

**User-reported issue:** Tapping Backup Now does not produce a working backup. Reproduce this on the current build using disposable test data; do not assume the root cause has already been established.

- Trace the full action from the button's enabled state and click handler through the ViewModel/background operation, database snapshot, archive writing, validation and refreshed UI. Inspect errors to identify where it fails.
- One tap must promptly show a visible “Creating backup…” state. Prevent repeated taps from starting overlapping jobs, while keeping navigation responsive.
- Create and validate a complete backup of the latest committed records using the requirements below. Do not report success merely because the click handler ran or a ZIP filename was allocated.
- If a destination must be selected or its permission renewed, explain this and open the supported folder/file picker. A cancelled picker must return to an idle state without a success message.
- On success, refresh the backup list immediately and show the new file's name, creation time, size, verified status and actual location, with Save a copy and Share available.
- Update “Last successful backup” only after creation and validation succeed. Keep the previous successful backup available.
- On failure, stop the progress indicator, re-enable the button and show an actionable error with Retry. Handle storage full, missing destination, revoked permission, snapshot failure and archive-write/validation failure. Do not silently swallow exceptions or leave the button permanently disabled.
- Handle backgrounding, screen rotation/recreation and interrupted jobs without duplicate backups, stale success messages or an endless spinner. A partially written file must not appear as a valid completed backup.
- Keep backup creation separate from sharing: cancelling Share must not delete the completed backup or change its success status. Never claim a backup was sent to another app unless that delivery can actually be confirmed.

### Backup Now acceptance tests

1. From the actual screen, tap Backup Now once with a seeded test database. Confirm visible progress, one completed validated archive, an immediately refreshed list and an updated last-success timestamp.
2. Add a new test record after the previous backup, tap Backup Now again and restore that new archive into a separate test database. Confirm the new record and all related data are included.
3. Tap repeatedly and recreate/background the screen during creation. Verify no overlapping operations, duplicate effects or stuck loading state.
4. Test low storage, revoked destination access, unavailable destination and injected snapshot/write/validation failures. Verify useful errors, working retry, unchanged last-success timestamp and preservation of the previous backup.
5. Cancel destination selection and sharing. Verify accurate state, no false “sent” message and preservation of any successfully created archive.
6. Interrupt creation and reopen the app. Verify partial files are not listed as valid backups and a fresh attempt can succeed.
7. Test the button on the actual Android release/minified build, followed by a full restore-and-compare test in an isolated database. A unit test of the backup service alone is insufficient.

### Required backup contents

- All persisted shop data: products, variants, pack/barcode mappings, stock movements, purchases, customers/suppliers, balances/payments, sales/lines, returns/void history, held bills, expenses/cash records, import history and audit records.
- Pigmy deposits, their linked transfers, giveaways and all other new entities.
- Relevant settings and branding, and actual photo/invoice/proof attachments where supported—not only references to inaccessible old paths.
- Existing sync/device identity state must be handled deliberately. Restoring must not replay already-synced transactions or clone a billing-device identity accidentally.
- Include a manifest with format version, real database schema version, creation time, attachment inventory and verification information. Never hardcode an incorrect schema number.
- Do not export biometric secrets, device-keystore material or signing credentials. Restore device authentication through the supported secure setup flow.

### Creation and visibility

- Create a consistent SQLite snapshot including committed WAL data. Do not casually copy an actively changing main DB file after ignoring checkpoint errors.
- Validate the snapshot/archive before calling it successful. Report actual validation performed, not a decorative “Verified” label.
- Use a temporary file and finalize only after successful writing/validation. Avoid timestamp collisions and partially valid-looking ZIPs.
- Show filename, creation time, size, verification status, location and last successful backup.
- Provide Save a copy and Share. Let the user choose a visible folder or Drive destination through Android. Distinguish app-local recovery copies from copies saved outside the app.
- Explain that saving to the same phone alone does not protect against losing the phone. Do not silently claim cloud backup exists when only a local file was created.
- Scheduled backup settings, if already implemented, must show destination and failure/retry status accurately.

### Safe restoration

1. Read the archive into a bounded staging location and reject path traversal/duplicate critical entries.
2. Validate archive completeness, database integrity, app identity and supported schema/migrations before touching the active database.
3. Show what backup is being restored and obtain confirmation of replacement.
4. Create a consistent recoverable pre-restore backup.
5. Stop conflicting writes/sync and replace data using a recoverable procedure.
6. Restore settings/attachments consistently and reopen or restart the app safely; no stale closed Room instance may remain in use.
7. On failure, retain/recover the prior working data and show the reason.
8. Verify restored records and relationships before claiming success.

### Backup acceptance tests

- Seed every entity type and representative attachments; create a backup and restore into a separate fresh installation/database.
- Compare counts, balances, individual records, relations, settings and attachment contents—not only ZIP size.
- Test committed data still in WAL, concurrent save attempts, low storage and interrupted backup.
- Test truncated/corrupt ZIP, missing DB, missing attachments, unsupported newer schema and malicious paths.
- Simulate interruption/failure at restore stages; prior data remains recoverable.
- Verify migrations from existing supported production schemas without destructive fallback.
- Verify sharing and Save a copy from the actual Android release build.

## 6. Phase 4 — Sales & Profit report and analysis

Recommended name: **Sales & Profit**, replacing the ambiguous Gross Profit entry point. Keep the summary simple, with details available on tap.

### Separate these concepts

- Net product sales: itemized product revenue after discounts and returns.
- Product cost of goods sold: cost snapshots for the units actually sold.
- Product gross profit: net product sales minus their known cost of goods sold.
- Money collected: payments received, including repayments of older customer credit; this is not automatically current-period sales.
- Services revenue and service expenses: retain a separate section if implemented. Do not invent per-job costs or show service revenue as exact service profit.
- Operating expenses: separate from inventory purchasing, supplier debt settlement and savings transfers.
- Giveaways/donations: stock cost shown separately from paid-sale COGS.
- Pigmy deposits: savings transfers, excluded from operating expenses and gross profit.

### Accuracy and presentation

- Apply the same selected date range to every card, graph and completeness warning.
- Provide Today, Week, Month and custom date range; graphs support daily, weekly and monthly grouping.
- Show discounts, returns and incomplete-cost coverage visibly.
- If costs are missing, label the result incomplete/known-cost profit; do not silently assume zero cost or display an apparently exact 100% margin.
- Keep manually entered daily sales summaries distinct from itemized bills. Clarify whether a summary is additional unbilled sales; do not count a full-day summary plus all its bills twice.
- Separate expected stock profit from actual realized product profit. Expected profit uses current sellable quantities/prices and known costs; negative stock must not produce misleading positive opportunity/profit.
- Use the approved weighted-average costing policy and proportional delivery-charge allocation. Preserve total allocated paise exactly; round allocation remainders deterministically.
- For ₹500 stock plus ₹50 delivery, the allocated inventory cost totals ₹550. Expected profit depends on selling prices; actual profit appears only as stock sells, with discounts/returns applied.
- Do not call an overall result “net profit” unless the included revenue and expense coverage supports it. Explain exclusions beside the result.
- Provide useful drill-down to underlying sales, costs, expenses and transfers. Exports must reconcile with the visible report.

### Report tests

Test known and missing costs, price changes after sale, discounts, returns, later debt repayments, supplier payments, freight, service revenue, giveaways, pigmy transfers, summaries versus bills and period boundaries. Reconcile totals independently with saved transactions. Changing today's product cost must not rewrite historical sale cost snapshots.

## 7. Phase 5 — Pigmy savings deposits

### Confirmed user decisions

- One savings account/scheme.
- Usually the same daily amount, with an option to change it.
- Prefill that amount, but record a deposit only after the brother confirms he paid.
- Deposits only for now. No withdrawals, maturity payouts or interest recording in this version.
- A deposit from shop cash automatically creates one linked savings-transfer entry in Cash In/Out.

### Suggested simple flow

Owner opens Pigmy → sees recorded deposit total and recent deposit history → taps Add deposit → date and usual amount are prefilled → optionally changes amount/adds a note or reference → confirms payment.

- Let the brother configure the usual amount in setup/settings. Do not hardcode amounts from the example files.
- Persist deposit ID, account/scheme identifier, paid date/time, amount in paise, payment source, optional note/reference, creation/update time, status and linked cash-entry ID.
- The payment source must be clear. Cash deposits affect shop cash; a bank/digital-funded deposit must not incorrectly reduce physical cash. Use existing payment/source conventions where available.
- Persist deposit and linked cash transfer in one transaction. One confirmed action means one deposit and one cash effect, even after retry or repeated taps.
- Editing date/amount/source updates the linked transfer consistently. Cancelling preserves history and reverses the cash effect once.
- A second deposit on the same date may be legitimate: warn about a possible duplicate and let the user review it rather than imposing an arbitrary one-per-day limit.
- Show date filters, count and total recorded deposits, per-day history and clear cancelled status.
- Label totals “Recorded deposits” rather than implying a verified bank balance. Past unrecorded deposits and interest are not known.
- No automatic expense creation, no automatic daily deposit and no guessed interest.
- Include data in backups/restores and relevant Owner exports. Restrict management on a Billing-only device.
- Historical migration/import must be reviewed; never create another cash effect for a cash-ledger row already counted.

### Pigmy tests

1. Setting a usual amount creates no financial transaction.
2. Opening/cancelling the deposit form saves nothing.
3. Confirming one cash deposit produces one deposit and one linked outflow classified as savings transfer.
4. Double tap, retry, app interruption and existing sync do not duplicate it.
5. Changing/cancelling a deposit updates or reverses the link once.
6. Different-day and same-day deposits, backdated entries and custom amounts work with correct totals.
7. Zero/negative/invalid amounts are rejected.
8. Gross profit and operating expenses are unchanged by a deposit; cash movement changes appropriately.
9. Restore preserves deposits and their links without replaying the transfer.
10. Billing-role access cannot bypass Owner restrictions.

## 8. Phase 6 — Free products and donations

Confirmed scope: **products only**. Cash donations are not requested.

### Recommended implementation

- Extend the existing stock-out workflow where practical rather than building an unrelated ledger.
- Offer explicit reasons: Free item and Donation, alongside existing stock-out reasons such as damage/shop use where already supported.
- Select product/variant, pieces or pack quantity, date, reason and optional recipient/note.
- Show the quantity leaving sellable stock and its known cost before confirmation.
- Save the giveaway and stock movement atomically. Reduce stock exactly once. Create no sales income, bill payment or customer credit.
- Preserve unit/base quantity, pack-size snapshot and cost at the time of the giveaway. Missing cost remains marked unknown.
- Show a simple history and period totals by quantity and known cost; separate giveaways from paid sales.
- Allow corrections/reversal with audit history. Cancelling a giveaway restores stock once and reverses its report effect.
- Respect the existing negative-stock warning policy, with a clear warning before proceeding.
- Products archived later must remain readable in historical records.
- Include records in backup/restore and applicable sync without double stock deduction.

### Giveaway tests

- Give 2 pieces from stock 10 → stock 8, zero sales income and zero customer debt.
- Give one pack of 10 → stock decreases by 10, correct cost snapshot.
- Repeated confirmation, interrupted save and sync retry have one stock effect.
- Cancel once → original stock restored; repeated cancellation has no further effect.
- Repricing or changing later purchase costs does not rewrite the saved giveaway cost.
- Unknown-cost item shows incomplete cost, not a fabricated zero-cost gift.
- Date filters, archived products, exports and restoration remain correct.

## 9. Phase 7 — Full regression and release gate

Build a checklist from all actual screens and repository operations. For each feature record: scenario, starting state, expected result, actual result, test evidence, fix and retest result.

Cover at least:

- App start, fingerprint/device credential lock, resume, process death and offline use.
- Settings, language/RTL, theme, removal of Receipt Preferences, per-receipt language/actions, navigation and accessibility.
- Product/variant/category create/edit/archive; stock receive/count correction/damage/shop-use/giveaway; pack and barcode handling.
- Spreadsheet preview/mapping/import and exports across supported formats/providers.
- Suppliers/customers, opening balances, partial payments, outstanding dues and record corrections.
- Billing by camera/search/favorites, mixed pieces/packs, flexible prices, whole-sale discounts, split payment, credit, change, holds, completion, returns and exchanges.
- Printing/photocopying/sticker and reusable services where present; service costs/reports and Owner-only access.
- Receipts, long bills, real printing, language choice, sharing and persisted historical receipt lookup.
- Cash In/Out, expenses, pigmy deposits and all report totals.
- Backup creation, visible saving/sharing, restoration and migration across supported versions.
- Two-phone pairing/permissions, offline pending bills, reconnection, repeated messages, conflict handling and exactly-once stock/payment effects where already implemented.
- Large realistic catalog/ledger, search speed, memory, storage exhaustion and error recovery.

Run unit tests for calculations/parsers, database integration and migration tests for persistence, Android device tests for platform integrations, and manual usability checks. Test the release/minified build as well as debug.

Release only after critical data-integrity/crash failures are fixed and retested. Clearly list any untested or blocked feature. Provide a change summary, test evidence, migration notes, backup/rollback instructions and an appropriately signed update APK. Do not describe the app as fully tested if important device paths or recovery tests remain unchecked.

## 10. Clarifications and scope boundaries

- Still needed: representative product-stock spreadsheet for real-format testing.
- Confirm any proposed handling of historical opening cash balances before committing them as new records.
- If historical pigmy amounts should be brought into the new tracker, review the mapping and duplication rules with the user first.
- The Sales & Profit presentation and giveaway form above are recommendations implementing the stated needs. Keep them simple and show the user the proposed layout before making unrelated redesigns.
- Do not add cash donations, multiple pigmy accounts, automatic deposits, withdrawals or interest features in this phase.

## 11. Note about earlier work in this chat

Before the user clarified documentation-only intent, a separate development copy was created at `C:/Users/Musid/.codex/visualizations/2026/10/25/01a17358-b9dd-74e2-9bf4-9050fa382bac/ali-app-fixes`. It contains partial code changes and tests and must not be treated as the finished app or automatically copied over the current Antigravity project.

Some JVM tests were run. An Android test attempt first failed because dependencies were unavailable offline; a subsequent attempt was interrupted. There is no confirmed successful Android test/deployment from that attempt. Original production source was not intentionally edited by that work, and no live shop records were intentionally changed.

Antigravity should work from the latest verified project and use this document as the implementation scope. The earlier partial copy is optional diagnostic material only.

## 12. Latest receipt changes — remove preferences and repair WhatsApp output

### 12.1 Reported problem and requested outcome

The user reports that WhatsApp sharing sends a receipt, but its arrangement is poor and it lacks the shop logo. Preserve the working ability to share while replacing the deficient output with a readable, branded receipt.

Remove **Receipt Preferences** from Settings. This supersedes older instructions to configure default receipt actions or receipt languages in Settings, including those in the embedded handoff and earlier theme/UI documents. Do not relocate the same preferences panel elsewhere under a different settings label.

Recommended receipt flow: save the bill once → open its receipt preview → select English or Urdu for this receipt → tap WhatsApp, Share, Save PDF or Print as needed. These are actions on a receipt, not persistent Settings preferences. Showing the preview must not automatically send, print or save another external copy.

### 12.2 Settings and migration behavior

- Remove the Receipt Preferences entry, screen, related navigation and unused UI controls on both roles where present.
- Keep unrelated Settings intact: shop details/logo, app language, theme, security, backup and connected-device management.
- Stop applying old stored automatic receipt-action preferences. An old “share automatically” or “print automatically” value must not trigger an unexpected action after upgrade.
- Handle old preference fields compatibly; do not wipe the settings store or reset unrelated values to remove a screen.
- Keep app language separate from the receipt's chosen language. Offer English and Urdu on the preview without requiring a trip to Settings. In the absence of a per-receipt choice, use the current app language if supported; allow a clear override before generation/sharing.
- Reopening a saved bill must use that bill's stored transaction, not the most recent bill in memory. Regenerating a receipt must not create another sale or stock/payment effect.

### 12.3 Logo and branding assets

- Use the approved Ali logo and exact shop name **Ali Educational**. Do not invent a replacement logo, stretch it, crop its identifying parts or use the full poster on the receipt.
- Workspace logo source: `assets/brand/ali-logo-original.png` relative to the document workspace. Verified absolute path: `C:/Users/Musid/OneDrive/Documentos/ChatGPT/Ali Education Inventory app/assets/brand/ali-logo-original.png`.
- Additional existing reference assets: `assets/brand/ali-logo-dark-concept.png` and `assets/brand/ali-educational-poster-reference.jpg`. The dark concept is an app-placement reference, not the production receipt logo.
- Prepare a clean monochrome black receipt asset from the approved design for a white page. A production monochrome asset is a deliverable, not something assumed to exist already. Preserve proportions, remove the pale rectangular background cleanly and retain recognizable design details.
- Keep the approved app theme, but receipts remain black on white for clear viewing and economical printing. Do not put white app-logo lettering onto a white receipt.
- Bundle the receipt logo locally so offline generation and sharing do not depend on a network image or a temporary file path. Include custom shop branding in backup/restore where applicable.
- Use a compact logo with clear margins beside or above the shop name. Show only actual configured address/contact information; omit empty fields gracefully. Do not invent an address, phone number, GSTIN or tax registration.

### 12.4 Receipt document layout

Recommended primary WhatsApp attachment: a professionally arranged **PDF receipt**. Use a shared receipt model for preview, PDF and printing so all amounts agree. Plain text may accompany the attachment as a short message, but must not be the only receipt.

Arrange the document in this order:

1. **Brand header:** compact logo, Ali Educational, configured address/contact details.
2. **Bill details:** receipt number, original sale date/time and customer name/contact when recorded. Show Walk-in only when appropriate; do not expose unnecessary personal information.
3. **Item table:** item/service description, quantity with unit, unit rate and line amount. Align monetary values consistently. Wrap long names instead of clipping them.
4. **Totals:** subtotal, whole-bill discount in rupees and its effective percentage when meaningful, then final payable amount with clear emphasis.
5. **Payment summary:** payment method(s), amount applied/paid, remaining credit and, for cash, tendered amount/change where applicable. Do not confuse money tendered with sales revenue or show a debt as a discount.
6. **Status/footer:** Paid, Part paid/Credit, Cancelled or Returned as supported by the actual record, followed by a short courteous footer if desired. Do not obscure financial details with decoration.

Additional rules:

- For ₹100 settled at ₹90 by explicit discount: show ₹10 discount (10%), total ₹90, paid ₹90 and due ₹0.
- For ₹100 with ₹90 paid and ₹10 remaining credit: total remains ₹100, paid ₹90 and due ₹10; do not display a discount.
- For a ₹80.25 bill with ₹100 tendered: payable/applied ₹80.25 and change ₹19.75. Keep stored tender, applied amount and change consistent.
- Price overrides must use the final unit rate. If original prices/savings are shown, clearly distinguish them from the explicit whole-bill discount; never subtract the override twice.
- Packs show the sold unit and quantity correctly. One pack of ten at ₹100 must not render as ten packs or a ₹1,000 line.
- Services show service name, option such as A4 B&W, quantity, unit, entered rate and amount. A service-only bill is valid. No stock information, purchase cost or profit belongs on a customer receipt.
- For a mixed ₹160 product + ₹40 service bill with 10% discount: subtotal ₹200, discount ₹20, final total ₹180. Internal report allocations must reconcile without cluttering the customer receipt.
- Historical receipts use saved sale-time names/options/prices, sale date and original receipt number. Sync or catalog edits must not silently change them.
- The shop is not GST registered. Do not show fabricated GSTIN, GST collection, tax breakdowns or “GST Tax Invoice”. Use an appropriate ordinary Bill/Receipt label.
- Long receipts must paginate safely: repeat item headings where helpful, keep columns aligned, avoid splitting a row's amount away from its description, and keep totals/payment details readable. Do not force all lines onto one fixed-height page or shrink text until unreadable.
- Use Unicode fonts that render Urdu shaping, right-to-left text and rupee/currency symbols correctly. Handle mixed Urdu, English product names, numbers and barcodes without reversing identifiers or corrupting values. Embed appropriate fonts when required by PDF generation.
- Phone preview must be readable and zoomable. A narrow phone screen must not clip the actual generated document.

### 12.5 WhatsApp sharing behavior

- Generate the receipt from the persisted bill selected by the user, in the selected language, before starting the share action.
- Save a nonempty, valid PDF in an appropriate app-owned location with a stable, sanitized receipt-based filename. Use a distinct file/URI per generated receipt or version so a second share does not overwrite a document WhatsApp is still reading.
- Share using a supported Android content URI/FileProvider, `application/pdf` MIME type, temporary read permission and compatible stream/ClipData handling as required. Do not expose raw private filesystem paths.
- Provide WhatsApp as a clear action on the receipt screen. Support WhatsApp/WhatsApp Business through Android's available supported handlers; if multiple choices exist, let the user select. If neither is available, show a helpful message and offer the standard share sheet or Save PDF.
- Let the user select and confirm the recipient within WhatsApp. Do not auto-send to an inferred customer number or automate WhatsApp's UI.
- Sharing works without requiring network access to generate the document; WhatsApp delivery itself may wait for connectivity.
- Keep the file available long enough for the receiving app to read it. Do not immediately delete it after launching the share intent; apply a safe cache cleanup policy later.
- On generation failure, show the error and a retry action without creating duplicate bills. On chooser cancellation, leave the bill saved and unchanged.
- Prevent repeated taps from causing overlapping generation/share attempts. Disable only the active action and show progress while generating.
- Do not display “Sent” or “Delivered” merely because Android handed the file to WhatsApp. Use accurate wording such as “Opened in WhatsApp”; external recipient confirmation is not available through an ordinary share intent.
- Opening/reprinting/sharing an older bill must never change its timestamp, invoice number, balances, stock, sync state or report totals.
- Respect device permissions: Billing may access only receipts allowed by the final approved history policy. Sharing must not leak Owner-only cost, expense or profit records.

### 12.6 Receipt acceptance tests

| ID | Scenario | Required result |
|---|---|---|
| RC01 | Upgrade an installation with existing receipt defaults | Receipt Preferences disappears; no old automatic share/print action runs; other settings and records survive. |
| RC02 | Open Settings on Owner and Billing roles | No receipt-preferences entry or broken route remains; unrelated allowed settings work. |
| RC03 | Generate a one-item English receipt | Correct approved logo, shop name, margins, columns, original date/number and exact saved amounts; valid readable PDF. |
| RC04 | Generate the same receipt in Urdu | Proper Urdu shaping/RTL and readable mixed-language names, numbers and currency; amounts equal English output. |
| RC05 | Share via WhatsApp and WhatsApp Business when installed | Correct selected PDF is attached and can be opened by the recipient in a controlled test; no unauthorized real-customer messages. |
| RC06 | WhatsApp missing, chooser cancelled, or generation fails | Useful fallback/error; bill remains saved once; no false sent status, stuck spinner or data change. |
| RC07 | ₹100 → ₹90 discount versus ₹10 customer credit | Receipt distinctly matches both examples in Section 12.4; neither case is mislabeled. |
| RC08 | Cash tendered ₹100 for ₹80.25 and a split-payment bill | Applied amount, change, paid/due and method breakdown reconcile to stored values. |
| RC09 | Piece/pack, mixed product/service, and service-only bills | Correct units, options, entered rates, line totals and single bill discount; no leaked cost/profit. |
| RC10 | Very long names, many lines, large amounts and multi-page output | No clipping/overlap, missing lines or unreadable shrinking; complete totals and payment summary. |
| RC11 | Reopen an older bill after app restart/catalog rename/price change/sync | Its own original snapshot, date and number render; it is not replaced by the last saved bill. |
| RC12 | Repeated share taps and sharing two different bills consecutively | No duplicate transactions or overwritten attachments; each recipient preview corresponds to the selected bill. |
| RC13 | Share with no internet, app backgrounding, or delayed receiving-app read | Local PDF generation works; URI remains readable as required; no unsupported delivery claim. |
| RC14 | Save PDF, preview and real Print of the same bill | All three outputs agree with WhatsApp PDF amounts and content; actual print integration works. |
| RC15 | Cancelled/returned/part-paid bill and later repayment | Status/payment presentation follows the defined persisted record/history policy and never fabricates paid status or reverses revenue twice. |
| RC16 | Restore backup then regenerate historical receipt | Required branding and bill data remain available, original values preserved, no new stock/payment effects. |
| RC17 | Billing-role receipt access and forged restricted request | Only authorized bill receipts can be accessed; Owner-only financial data is absent. |
| RC18 | Release/minified build on the actual phone | Fonts, logo resource, PDF creation, content permissions and WhatsApp attachment all work, not only JVM/debug rendering. |

### 12.7 Required deliverables from Antigravity

- Updated receipt screen and removal of Receipt Preferences, with no regression to other settings.
- Clean approved monochrome logo asset and one shared, tested receipt layout/model.
- Working PDF generation, WhatsApp sharing, generic sharing, saving and actual printing as supported by the app.
- English and Urdu sample receipts using synthetic data: short, long/multi-page, mixed services/products and partial payment.
- Visual evidence of the actual PDF output and phone preview, plus test results for RC01–RC18. Do not use a design mockup as evidence that generated receipts work.
- Migration/regression evidence and any unresolved policy choices clearly stated. Never claim successful delivery to customers based solely on intent launch.


---

## 13. Complete earlier Combined Implementation Handoff

The original combined handoff follows in full. Its historical date and internal numbering are retained. The master precedence section and Section 12 above override older receipt-default/settings requirements. All other applicable requirements and detailed tests remain included.

<!-- BEGIN COMPLETE ORIGINAL COMBINED HANDOFF -->

# Ali Educational — Combined Implementation Handoff

Date: 31 October 2026
Scope: Two-phone billing and synchronization, service billing, and owner-only service revenue/expense reporting, integrated into the existing Android app.

## Read this first — instructions for the implementing developer

Extend the existing app; do not replace it with a fresh prototype. This document contains the FULL two-phone specification and the FULL service specification below, followed by a preservation and release checklist. The source documents have been retained intact. No feature or test section has been omitted.

These are requirements and planned tests, not evidence that implementation or testing is complete. No document can guarantee zero bugs; release requires demonstrated data preservation and regression results.

### Authoritative decisions and precedence

Use the following latest decisions if an older paragraph below calls an already-resolved point provisional:

1. Build ONE shared Android app and ONE signed release APK for two phones, using Owner and Billing device roles. Do not create two separate business-logic implementations or install two production copies on the owner's phone.
2. Both phones use the same shop Wi-Fi. Remote sync and a desktop server are outside this phase. Hotspot support is optional device-tested compatibility, not a replacement requirement.
3. Offline billing is approved: persist bills locally and send them when connectivity returns. Delivery retries must have exactly-once EFFECTS through deduplication; network delivery itself may repeat.
4. The billing phone may override item prices on the current bill and apply whole-bill discounts. It may not edit saved catalog prices.
5. Initial services are Printing, Photocopying, and Sticker printing, plus Add service for reusable definitions. Printing options are A4 B&W and A4 Colour. No default or last-used service rates.
6. Only the OWNER phone records, edits, voids, or accesses service expenses and the service financial overview. This overrides any general permission wording elsewhere in the combined document.
7. Service revenue, recorded service expenses, and their difference are shown separately. The difference is NOT exact service profit or cash available.
8. Local fingerprint/device credentials or PIN protection remains the approach. Device pairing does not require separate online logins.
9. Existing main-app behavior must remain functional when sync is disabled or the billing phone is absent.
10. The latest approved decisions above take precedence; then the service-specific rules apply to service features and the two-phone rules apply to shared transport/roles. Ask about unresolved conflicts rather than silently inventing permissions.

### Unresolved choices — do not block independent work

Confirm one question at a time before implementing the affected policy: photocopy/sticker quantity units; customer creation/credit/repayment permissions on the billing device; return/cancellation permissions; reusable-service creation permission on the billing device; historical bill visibility; final receipt series; delayed offline-sale costing; and backup schedule/destination details. Same-Wi-Fi operation, offline billing, price overrides/discounts, and owner-only service expenses are already settled and must not be asked again.

### Existing app must be identified first

The audit observed installed package `com.alieducational.app` v1.0.0, whereas the previously inspected workspace built `com.alieducational.inventory` v0.2.2-stock. Locate the matching current source, inspect installed package/build identity, and compare signing certificates before deploying an update. Do not assume this workspace is the production source.

Do not uninstall the active app, clear its storage, replace its database, generate dummy transactions in active shop data, or install an incompatible build to bypass an update failure. Existing defects in backup verification, import crash handling, costing/profit, zero prices, Urdu, navigation, and receipt preferences must remain visible in the work plan rather than being masked by the new features.

## Part A — Complete two-phone specification

# Ali Educational — Two-Phone Billing and Sync Specification

Date: 31 October 2026  
Status: One shared app with Owner/Billing device roles, same-Wi-Fi operation, and offline billing approved. Detailed billing permissions remain to be confirmed.

## 1. Purpose and recommendation

Provide a full shop-management experience on the main Android phone and a restricted billing experience on a second Android phone. Keep the current main-app repair work independent of this expansion.

Recommended approach: one shared application codebase and data model, with Owner and Billing device roles. The same signed app can present different screens according to its paired role. Two independent applications with separately implemented billing, costing, and storage would increase the risk of inconsistent behavior and duplicate maintenance.

This is a suitable approach for two phones working together in one shop, subject to testing local connectivity and Android background behavior. It is not yet a finalized choice for phones that must synchronize from different locations. Local-only sync cannot deliver changes while the phones cannot reach each other.

## 2. Confirmed needs and open assumptions

### Latest owner decisions

- One shared app with two device roles is approved.
- Both phones will operate on the same Wi-Fi. Remote synchronization is outside the current scope.
- Offline billing is approved: the billing phone must save completed bills durably when the main phone is off or unreachable, show a pending-sync count, and automatically deliver them exactly once when connectivity returns. Pending bills must survive app/device restart. Stock shown while disconnected is last-known stock; combined reports and main-phone backups remain incomplete until pending bills arrive.
- Roles do not require separate online accounts. Proposed setup: initialize the main phone as Owner, then authorize/pair the second phone as Billing from the main phone.
- Each phone can retain local fingerprint/device-credential or PIN protection. A Billing device must not be able to promote itself to Owner through an unprotected role selector.
- Separate named user accounts are not part of this change unless requested later.
- Billing-phone price overrides and whole-bill discounts are approved. An override changes the item price on that bill only; it does not edit the catalog price. Preserve original catalog price, overridden unit price, discount, final total, and origin device in history. Apply the whole-bill discount after item price overrides; do not count an override again as a discount. Keep discount-versus-unpaid-credit selection explicit.

Confirmed needs:

- Main phone manages all shop records and can also perform billing.
- Second phone provides billing access and sends its bills to the main phone.
- Relevant stock, product, price, payment, and customer-credit changes stay consistent.
- Records are backed up and recoverable.
- No desktop is required for this phase.
- Existing preferences favor keeping active data on owned devices and avoiding monthly fees. Google Drive was accepted as a backup destination.

Provisional implementation baseline, not yet approved:

- Both phones normally share shop Wi-Fi or a hotspot.
- Billing can continue during disconnection, with explicit pending-sync status.
- The main phone coordinates accepted shared records; the billing phone durably retains its own pending transactions.
- One billing phone is paired initially. Design identifiers to allow future expansion without implementing extra devices now.

Do not promise remote live sync, always-on background sync, or conflict-free offline stock until the corresponding requirements and device tests are complete.

## 3. Protect ongoing main-app development

### Independent delivery tracks

**Track A — main app reliability:** Continue the existing audit fixes: backup validation, Excel crash handling, stock and profit calculations, zero-price checks, Urdu, receipt defaults, record actions, navigation, and reports. These fixes must work with a single phone and without any sync service.

**Track B — shared billing foundations:** Define transaction identifiers, immutable completed-sale snapshots, database migrations, record history, and shared calculation functions. Introduce changes through backward-compatible migrations. Do not rewrite the entire application to add sync.

**Track C — optional two-phone capability:** Add pairing, roles, transport, pending queues, acknowledgements, reconciliation, and sync status behind a disabled-by-default capability. Activate it only after isolated testing.

### Rules preventing interference

- Locate the source matching the installed app before implementation. The audit found a package/version mismatch in the current workspace.
- Preserve the existing main-phone application ID and signing identity for upgrades unless a separately approved migration is necessary.
- Use one source of truth for billing calculations, discounts, units, receipts, and transaction validation.
- Keep device-role UI separate from shared business rules. Hiding a button alone is not a permission boundary.
- Main-app launch, billing, stock, and backup must not depend on a paired phone or an available network.
- Isolate network work from the UI thread and database transactions; network failure must not freeze checkout.
- Add database schema versions and migration tests. Never reset the database as an upgrade strategy.
- Keep sync disabled for existing installations until the owner explicitly pairs a device.
- Give both tracks stable interface contracts. A screen redesign must not require rewriting the sync protocol.
- Release and regression-test single-phone behavior before enabling the second phone.
- Disabling new pairing must not discard queued bills. An emergency stop must preserve data and offer recovery/export.

## 4. Device roles and proposed permissions

| Capability | Owner/Main phone | Billing phone |
|---|---|---|
| Sell and scan products | Yes | Yes |
| Search catalog and view selling prices | Yes | Yes |
| View available quantity | Yes | Yes, with last-sync timestamp |
| Receive stock, edit costs, adjust inventory | Yes | No by default |
| View profit, supplier dues, expenses | Yes | No by default |
| Change app-wide settings and pair devices | Yes | No |
| Full backup and restore | Yes | No |
| Reprint/share own completed bills | Yes | Proposed yes |
| Override item price on a bill or apply whole-bill discount | Yes | Approved; preserve original and effective prices in history |
| Edit saved catalog prices | Yes | No |
| Create customers or sell on credit | Yes | To confirm |
| Receive old customer repayments | Yes | To confirm |
| Return, exchange, cancel, reverse | Yes | Owner approval or permission; to confirm |
| View other device's historical bills | Yes | To confirm |

Only send the billing device data needed for its approved role. Avoid distributing supplier balances, purchase costs, profit figures, security settings, or full backups merely because they exist on the main phone.

Use the existing local fingerprint/device-credential or PIN protection approach. Pairing establishes device identity; it does not require introducing an online account. A device label identifies where a transaction originated, not necessarily which person used it.

## 5. User experience

### Main phone

- Keep the full main app and approved Home Settings gear.
- Add Settings → Connected billing device.
- Show device name, connection, last successful exchange, pending/error status, permissions, and revoke action.
- Show incoming bills in normal sale history with an unobtrusive origin-device label.
- Show unresolved sync conflicts in a clear review list, without blocking unrelated local work.

### Billing phone

- Start at New Bill with scan, search, categories, favorites/recent products, quantity, and cart.
- Show a compact status: Connected, Pending sync: N bills, Disconnected, or Needs attention.
- Keep pending bills visible in local history. Explain that they are saved on this phone but have not reached the main phone.
- Provide Sync now and a readable error/retry route.
- Clearly mark stock quantities as last known when disconnected.
- Provide only permitted settings such as language, appearance, local lock, printer/share defaults, and connection details.

Avoid technical protocol language in the product UI. Never say “backed up” solely because a bill was saved locally or synchronized.

## 6. Local connection and pairing

Proposed transport: local Wi-Fi or a hotspot, without requiring internet for billing and exchange. Wi-Fi client isolation, hotspot behavior, changing addresses, battery restrictions, and Android service limitations must be tested on both actual devices.

Pairing sequence:

1. Owner selects Add billing phone from Settings.
2. Main phone creates a short-lived, single-use pairing invitation, represented by a QR code or equivalent verified flow.
3. Billing phone scans it and displays the main shop/device identity.
4. Main phone confirms the new device and permissions.
5. Devices establish authenticated, encrypted communication and store device credentials securely.
6. Main phone sends a role-filtered initial catalog snapshot with a revision marker.
7. Billing phone verifies and commits that snapshot, then requests subsequent changes.
8. Show Ready only after initial synchronization succeeds.

Do not transmit private keys or permanent credentials in ordinary logs. Do not expose an unauthenticated local API. Device revocation must reject future requests. An offline revoked device cannot receive revocation instantly; explain that limitation and define how its pending records will be reviewed rather than silently lost.

## 7. Transaction synchronization contract

### Completing a bill on either phone

Within one local database transaction:

- Validate permitted actions, quantities, amounts, and required customer details.
- Assign a unique transaction ID and origin-device ID.
- Save item, quantity, unit conversion, selling-price, discount, payment, and credit snapshots.
- Save local stock effects and an outbound synchronization event together.
- Mark the bill saved locally and pending delivery where appropriate.

Do not require a network acknowledgement to preserve a completed local sale. A failure to save locally must not be displayed as a completed bill.

### Delivering a billing-phone transaction

1. Billing phone sends pending events in defined order.
2. Main phone authenticates the device, checks permissions and payload validity, and identifies duplicates by transaction/event IDs.
3. Main phone applies the accepted transaction, stock movements, payment/credit effects, and receipt history atomically.
4. Main phone returns an acknowledgement only after durable commit.
5. Billing phone records the acknowledgement durably and clears the pending indicator.
6. Main phone distributes relevant updated catalog/stock/account state.

If a reply is lost, resend the same event with the same identifier. The main phone must return the existing result without applying it twice. Do not generate a new bill simply because a transfer timed out.

### Stock reconciliation

Transmit transactions and stock movements, not blind replacements of absolute quantities. The billing phone must distinguish acknowledged movements from locally pending movements when showing projected stock. Receiving an updated quantity must not deduct its own sale for a second time.

Use revisions/cursors and a consistent snapshot boundary so changes made during initial synchronization are not missed. Handle history gaps through a fresh reconciled snapshot, preserving pending local transactions.

## 8. Identifiers, numbering, and time

- Use globally unique internal identifiers for records and events; do not rely on phone timestamps for uniqueness.
- Maintain a stable registered device identity and per-device ordering mechanism.
- Choose an invoice-number scheme that cannot collide offline, such as owner prefix plus device series and local sequence. Example only: `AE-M-000123` and `AE-B1-000045`; exact presentation requires agreement.
- Do not renumber a shared/printed completed receipt after synchronization.
- Preserve original sale time, local timezone context, main-phone receipt time, and origin device. Detect clock differences; do not sort accounting logic solely by untrusted device time.
- Define behavior after device replacement or restoring old backups so invoice sequences and event IDs cannot be reused.

## 9. Offline and conflict policies

| Situation | Required behavior |
|---|---|
| Main phone is absent/off | Billing may continue if approved; bills stay pending locally. No claim of live synchronization. |
| Both devices sell the final units | Preserve real completed sales, flag resulting negative stock, and request reconciliation. Never silently discard a sale. |
| Owner changes price while billing phone is offline | Existing completed bills keep their sale-time price; refreshed prices apply to new sales according to the agreed policy. |
| Owner archives a product sold offline | Keep the submitted sale and flag if review is needed; do not lose the transaction. |
| Invalid or unauthorized incoming transaction | Retain it with a visible actionable error/review state; avoid silent discard or endless hidden retries. |
| Duplicate customer created offline | Use unique IDs and an owner-reviewed merge workflow. Do not merge solely because names match. |
| Cancellation or return from another device | Link a new reversal/return to the original transaction, validate remaining refundable quantities, and prevent double reversal. |
| Duplicate message/reconnection | Return the recorded result without repeating effects. |

Offline availability and perfectly current cross-phone stock cannot both be guaranteed while disconnected. Show this honestly in the interface.

## 10. Pricing, delivery cost, and profit

Retain the approved proportional delivery allocation and weighted average costing policy. Billing phones need selling prices; they do not need unrestricted purchase-cost access.

The main phone must determine and preserve cost snapshots for accepted sales under a documented ordering policy. A delayed offline sale can arrive after a purchase or price change. Define whether cost is assigned on acceptance or reconstructed using validated transaction order, and disclose unresolved costing. Do not silently claim historically exact profit using whichever average happens to exist when a delayed event arrives.

Keep discount versus remaining credit explicit. A ₹100 bill paid at ₹90 must distinguish ₹10 discount from ₹10 debt. Repayments must not become new sales. Returns, cancellations, and exchanges must update inventory, payment/credit records, and profit consistently across devices.

## 11. Backup and recovery

Sync and backup are separate functions. The main phone creates the combined shop backup after accepted transactions are durably stored. Preserve existing dated backups and support verified export to the owner's chosen destination; Google Drive is a backup option, not the live transaction database.

Backup UI must show:

- Actual location or explanation of private app storage.
- File name, meaningful size, date, integrity status, and included data.
- Last backup separately from last sync.
- A warning when known billing-phone transactions are pending. If a device is unreachable, state that its current pending count is unknown.
- View backups, export/share, and restore actions.

The billing phone must durably retain unsynced transactions. Decide whether to provide an encrypted emergency export of pending bills; do not imply that a main-phone backup covers them. Loss of the billing phone before transmission can lose records unless a separate copy was made.

Restore must be tested in isolation. A restore onto the main phone requires a controlled reconciliation procedure with paired devices: old acknowledgements and queued events must not duplicate or erase transactions. Preserve transaction identities and introduce a documented recovery generation/epoch or equivalent protocol. Never restore one phone's entire database blindly onto the other role.

## 12. Suggested component boundaries

- **Shared domain:** money/rounding, quantities, discounts, sale validation, transaction effects, and receipt data.
- **Local persistence:** versioned database, atomic commits, stock ledger, immutable completed records, outbox/inbox and acknowledgement state.
- **Owner features:** purchasing, inventory administration, accounts, reporting, backup and device management.
- **Billing features:** checkout, scanner, permitted customer actions, local bill history and receipt actions.
- **Sync protocol:** versioned messages, identifiers, authentication, cursors, deduplication and reconciliation.
- **Transport:** discovery, local connection, reconnect/retry, Android lifecycle integration.

These are logical boundaries, not a requirement to adopt a particular framework. Select implementation details after inspecting the matching project. Reject incompatible protocol versions clearly; do not silently process data with an unsupported schema.

## 13. Phased implementation and release gates

### Phase 1 — stabilize the existing app

Resolve data-loss/crash risks and validate backup recovery, billing, stock/costing, and upgrade migrations. Existing single-phone tests must pass. No paired device is needed.

### Phase 2 — build sync-ready foundations

Introduce stable IDs, device attribution, transactional outbox, versioned contracts, and role-aware boundaries while sync remains disabled. Verify current billing still behaves correctly.

### Phase 3 — pair and share catalog

Implement authenticated pairing, role-filtered initial snapshot, incremental updates, revocation, and visible connection status. Test on both actual phones with Wi-Fi and hotspot.

### Phase 4 — synchronize completed bills

Implement durable delivery, acknowledgements, duplicate prevention, stock reconciliation, credit/payment effects, and unique receipt numbering. Test failures before and after every commit/acknowledgement boundary.

### Phase 5 — offline conflicts and recovery

Test simultaneous sales, pending queues, changed products/prices, invalid events, delayed costs, returns, backup/restore, device loss and replacement. Resolve policy questions before enabling affected features.

### Phase 6 — controlled rollout

Use isolated test data first, then a small supervised pilot with verified backups. Compare daily totals on both phones and inspect pending queues. Broaden use only when accepted bills reconcile and no known release-blocking failures remain.

## 14. Mandatory acceptance scenarios

1. Main phone works normally with sync disabled and no second device.
2. Pair only an approved device; reject unauthorized requests and wrong permissions.
3. Sync a catalog of approximately 3,000 changing products without freezing checkout.
4. Start with 20 notebooks, sell 2 on the billing phone, and show 18 on both after acknowledgement. Main phone receives exactly one bill and one stock deduction.
5. Drop the acknowledgement and retry repeatedly; totals remain unchanged after the first commit.
6. Disconnect, complete multiple bills, restart the billing phone, reconnect, and receive every bill exactly once.
7. Sell the last stock on both disconnected phones; retain bills and flag shortage.
8. Test discounts, cash/digital split payments, credit, subsequent repayments, and refunds with matching totals.
9. Update prices/archive products while offline sales exist; preserve historical bill snapshots.
10. Interrupt initial sync, catalog update, local save, incoming commit, and backup. Recover without silent partial records.
11. Test screen-off, app background, battery saver, locked phones, hotspot changes, and address changes. Show honest status if background delivery is delayed.
12. Test Urdu/English independently on each phone and verify receipt output.
13. Export and restore a backup into an isolated environment, then reconcile pending and acknowledged events without duplication.
14. Upgrade one phone before the other; handle supported and unsupported protocol versions safely.
15. Replace or revoke a billing phone without reusing identities or invoice sequences.

## 15. Questions to resolve one at a time

Price overrides for the current bill and whole-bill discounts are approved for the billing phone. Other permission questions below remain open.

1. Resolved: both phones will use the same Wi-Fi; remote sync is outside the current scope.
2. Resolved: billing must continue when the main phone is off or unavailable, with durable pending bills and later synchronization.
3. Which actions may the billing phone perform: discounts, price overrides, credit, customer creation, repayments, returns and cancellations?
4. Should the billing phone see only its own bills or all shop bills?
5. Is a device-specific receipt series acceptable?
6. What is the preferred backup destination and schedule, and should pending bills have an emergency export?
7. Which actual phone models and Android versions will be used for connectivity testing?

Do not block unrelated main-app fixes while these decisions are pending. Do not implement dependent policies as if they were already approved.

## 16. Relationship to existing plans

This document expands Section 10 of `ALI_EDUCATIONAL_APP_AUDIT_AND_FIX_PLAN.md`. Existing audit findings and fixes remain required. Optional AI remains outside this phase. The confirmed Home Settings gear, non-GST receipts, Urdu support, Excel import, delivery allocation, weighted average cost, and other agreed main-app requirements continue to apply.

This specification describes planned behavior, not completed implementation or a guarantee that every Android device can provide uninterrupted local background hosting.

## 17. Price/discount and two-device verification examples

Run these on isolated test data using two separate Android installations. Prefer both actual phones for Wi-Fi, camera, battery, lock, and restart tests. An emulator can assist development but cannot establish real-phone connectivity reliability.

- Catalog notebook price ₹80, stock 20: override to ₹75 and sell 2. Bill total ₹150, stock 18, catalog price still ₹80 on both phones after sync.
- Without an override, sell 2 at ₹80 and apply 10% whole-bill discount: subtotal ₹160, discount ₹16, payable ₹144.
- Combine a ₹75 unit override for 2 items with 10% whole-bill discount: subtotal ₹150, discount ₹15, payable ₹135. Preserve ₹80 original unit price, ₹75 effective unit price and ₹15 explicit discount; do not double-count the override.
- If test cost is ₹55 per item, the combined example has cost of goods sold ₹110 and gross profit ₹25, excluding shop expenses.
- Enter ₹90 received against a ₹100 bill: discount choice settles at ₹90 with ₹10/10% discount; credit choice retains ₹100 sale value and ₹10 due.
- Verify the receipt, local history, synchronized main-phone bill, stock ledger, payments/credit, and profit agree for each example.
- Disconnect after pairing, save bills, restart the billing phone, reconnect, and verify durable pending records arrive exactly once. Repeat with main-phone restart and a lost acknowledgement.
- Submit simultaneous sales of the last units and verify both bills survive with an explicit shortage warning.
- Test invalid negative prices, excessive discounts, rounding, explicit free-item handling, repeated Save taps, receipt-sharing cancellation, and permissions preventing catalog edits from the billing phone.
- Verify pending bills are clearly excluded from a main-phone backup until received; test backup restoration only in an isolated installation.

Record expected values, actual values, build versions, device identities, and pass/fail evidence. These are future acceptance tests, not tests already executed.


## Part B — Complete service billing and reporting specification

# Ali Educational — Service Billing Specification

Status: Approved feature and visual direction; implementation specification.  
Scope: Service selection, reusable service creation, job entry, mixed bills, receipts, service revenue reporting, and owner-only service expense recording. Updated 31 October 2026.

## 1. Purpose

Allow the shop to bill printing and related services quickly, either alone or alongside products in one bill. Keep service entries separate from inventory products. Enter a rate for every job; do not save default service prices.

## 2. Approved service list

The Services section initially contains:

1. Printing
2. Photocopying
3. Sticker printing
4. **+ Add service**

Do not add other predefined services. The owner can create additional reusable services using Add service.

Printing has these quick options:

- **A4 B&W**
- **A4 Colour**

These options specify the job type, not its price. Do not automatically add further paper sizes or service options.

## 3. Approved visual design

Use the app's black/charcoal background, warm-white text, and gold highlights/buttons. Keep the interface simple, with readable labels and large touch targets.

### Screen A — New bill / Services

- Heading: **New bill**.
- Two tabs: **Products** and **Services**.
- Selecting Services shows Printing, Photocopying, Sticker printing, and Add service as large rows with simple icons.
- Newly saved services appear in this list and remain available for future bills.
- Show the current bill beneath the selection area, with products and services together.
- Each bill line shows its name, quantity, rate, and total when expanded or edited.
- Bottom action: **View bill · ₹[total]**.
- Switching tabs must preserve the current bill.

### Screen B — Enter a service job

- Heading shows the selected service, such as **Printing**.
- Printing shows A4 B&W / A4 Colour option buttons.
- **Quantity** field with its unit clearly identified.
- **Rate per [unit] (₹)** field, initially blank for each new job.
- Helper text: **Enter rate for this job.**
- Calculated **Service total**.
- Primary action: **Add to bill**.
- Back returns to service selection without adding an incomplete line.

The approved preview illustrates quantity as printed sides for printing. Do not confuse a physical sheet with a printed side: two-sided printing may represent two chargeable sides. The shop's precise charging unit must be unambiguous on the form and receipt. Photocopying and sticker-printing unit labels should be confirmed rather than silently inferred.

### Screen C — Add reusable service

- Heading: **Add service**.
- Required field: **Service name**.
- Field: **Unit label**, such as Print, Copy, Sheet, or Job.
- Helper text: **Saved for future bills. Enter a rate each time.**
- Primary action: **Save service**.
- There is no default-rate field.

Saving creates a reusable service definition. It does not add a charge to the current bill automatically. After saving, return to the service list with the new service available to select.

The preview's “Photo printing” entry is an example of a user-created service, not another predefined option.

## 4. Rate and quantity behavior

- Never prefill a new service line with a default or last-used rate.
- Enter quantity and rate for each job.
- Calculate the total immediately after valid values are entered.
- Formula: **quantity × rate = service line total**.
- Editing an existing bill line retains that line's entered values so they can be corrected.
- Reopening a held bill retains the entered rates; this is saved bill data, not a service default.
- Changing the selected printing option should clear a previously entered rate to avoid accidentally charging the B&W rate for colour or vice versa.
- Use consistent currency rounding and money handling with the rest of billing.
- Do not permit a blank or negative rate. An intentional free service, if supported by the existing billing policy, must be explicitly selected rather than inferred from a missing price.
- Require a positive quantity. Define unit-specific quantity rules; printed sides/copies must not silently accept meaningless fractional counts.
- Show validation beside the relevant field and preserve the user's other entries.

## 5. Reusable services

A reusable service stores its identity, name, and unit label. It does not store a selling rate, stock quantity, purchase cost, barcode, or reorder threshold.

Recommended safeguards:

- Trim names and reject empty names.
- Warn about an existing service with the same name rather than silently creating confusing duplicates.
- Keep historical bill-line names, units, and prices unchanged if the reusable service is renamed later.
- If removal is supported, archive services referenced by completed bills so receipt history remains intact.

Permission to create or edit reusable services on a restricted billing device must follow the app's agreed permission policy; the visible Add service design does not grant unrestricted settings access.

## 6. Mixed bills and checkout

Products and services can appear on the same bill. They use the existing checkout workflow for discounts, payment, customer credit, and receipt creation.

Example only — all rates below are entered for this bill:

| Item | Quantity | Rate | Line total |
|---|---:|---:|---:|
| Notebook | 2 | ₹80 | ₹160 |
| Printing — A4 B&W | 20 printed sides | ₹2 | ₹40 |
| **Subtotal** | | | **₹200** |

A 10% whole-bill discount is ₹20, making the final payable ₹180. Apply the discount once using the existing whole-bill discount rules. Do not create a second service-specific discount workflow.

- Add, edit, or remove a service line before completing the bill.
- Keep distinct jobs as distinct lines when their option, rate, or unit differs.
- Prevent repeated taps on Add to bill from unintentionally adding duplicate lines.
- Support service-only bills without requiring a product.
- Do not change the meaning of discount versus unpaid customer credit.

## 7. Inventory behavior

Service lines do not deduct product stock. A mixed bill deducts stock only for its product lines.

Do not automatically consume paper, ink, toner, or sticker sheets in this feature. Consumables tracking would require a separately designed mapping and is outside this specification.

Services must not appear as low-stock items or contribute to inventory valuation. Reversing a service charge must not add units to product stock.

## 8. Receipts and reporting

Receipts should show service name, printing option if applicable, quantity, unit, entered rate, and line total. They should share the same discount/payment summary as product lines and follow the app's existing receipt language selection.

Show product sales and service revenue separately in reports while keeping a combined bill total. For a whole-bill discount, allocate the discount proportionally across lines with rounding that sums exactly to the bill discount, so category totals reconcile.

For the ₹200 mixed-bill example with ₹20 discount:

- Product net revenue: ₹144.
- Service net revenue: ₹36.
- Combined net revenue: ₹180.

Service revenue is not automatically service profit. Show recorded service expenses alongside revenue as specified below, but do not call their difference exact profit. Per-job costing, consumables usage costing, and automatic allocation of supplies consumed remain outside this feature.

## 9. Minimal implementation requirements

Use a distinct service line type within the existing bill model. Preserve completed line snapshots containing:

- Reusable service ID.
- Service name at time of billing.
- Selected option, if any.
- Quantity and unit label.
- Entered rate and line subtotal.
- Allocated whole-bill discount where used for reporting.
- Final net line value.

Keep reusable service definitions separate from bill-specific rates. Route service lines through existing receipt, history, and transaction handling without applying inventory movements. Existing offline saving and bill transfer must preserve service lines and their entered values.

## 10. Acceptance tests

1. Open Services and verify exactly the three initial service types plus Add service.
2. Select Printing and verify A4 B&W and A4 Colour options.
3. Verify the rate is blank on a new job; enter 20 sides at ₹2 and receive ₹40 total.
4. Add the service, start another job, and verify the previous rate is not reused.
5. Change B&W to Colour and confirm the old rate is cleared.
6. Create a reusable service with a name and unit; restart the app and verify it remains available without a saved price.
7. Reject empty names, invalid quantities, blank/negative rates, and handle duplicate names clearly.
8. Add products and services to one bill; verify subtotal, whole-bill discount, payments, and credit.
9. Complete the ₹200/10% example and verify ₹180 payable, ₹144 product revenue, and ₹36 service revenue.
10. Confirm only product lines reduce stock; service-only bills leave inventory unchanged.
11. Edit/remove a draft service line and hold/resume a bill without losing entered values.
12. Inspect receipts in both supported languages for name, option, unit, rate, and correct totals.
13. Rename/archive a reusable service and verify historical receipts remain unchanged.
14. Verify service revenue is not reported as fully earned profit when costs are unknown.
15. Verify repeated taps and reopening a saved bill do not duplicate charges; existing bill synchronization preserves service lines exactly once.

## 11. Boundaries and remaining detail

This feature does not include document upload, editing print files, controlling a physical printer, tracking print-job production status, automatic consumable deductions, or additional predefined service types.

The main detail still to confirm is the quantity unit for photocopying and sticker printing, and whether the printing preview's per-printed-side convention matches shop practice. Keep unit labels explicit and do not assume sheets, sides, and stickers are interchangeable.

The approved image is a design preview. Example prices are illustrative entries and must not become defaults in the implementation.

## 12. Approved expansion — service revenue and expenses

Provide a **Services** tab within **Sales & Profit** on the owner phone. It combines service revenue and recorded service expenses in one view without slowing checkout.

Confirmed decisions:

- Record revenue from completed service bill lines automatically.
- Enter service expenses separately, only on the owner phone.
- Do not ask for estimated cost on each printing job.
- Show a combined services overview with breakdowns by service underneath.
- Keep shared service expenses in a separate bucket rather than guessing allocations.
- Record each expense once and reuse it in both Services and the overall expense report.
- Continue entering selling rates for each job; expense recording does not create default rates.

### Owner-only permissions

Only the Owner role may create, edit, void, attach evidence to, or export service expense records and access the service financial overview. The billing phone continues billing services but cannot access service expense administration or reports.

Enforce restrictions in the underlying commands/data access and incoming synchronization validation, not merely by hiding buttons. Do not send expense records or their attachments to the billing phone. Reject unauthorized expense writes with an explicit error without applying any change. This permission is separate from the still-open question of creating reusable service definitions on the billing phone.

## 13. Services report layout

Keep the black-and-gold style and simple labels. On the owner phone, open **Sales & Profit → Services**.

### Date controls

- Today, This week, This month, and Custom dates.
- Display the selected start/end dates; use the shop timezone consistently.
- Use Monday as the proposed week start and show actual dates so the interpretation is visible.
- Support daily, weekly, or monthly grouping for the graph, independently of the date range.
- Use the selected range for cards, charts, breakdowns, transaction lists, and exports.

### Summary cards

| Card | Definition |
|---|---|
| Net service revenue | Completed service charges minus allocated discounts and service sales reversals in the selected period. Includes credit sales. |
| Recorded service expenses | Non-voided service expense entries dated within the selected period. |
| Revenue minus recorded expenses | Net service revenue minus recorded service expenses; may be positive, zero, or negative. |

Show this explanation next to the third figure: **“Not exact service profit. Supplies may be used across months, and some sales may be unpaid.”** Do not label it cash available, cash flow, take-home income, or net profit.

Show **“Service profit is not calculated”** where necessary to prevent the user mistaking revenue for profit. Do not combine this comparison with product gross profit into an apparently exact overall profit figure.

### Graph and breakdowns

- One readable graph with Revenue / Expenses / Both controls; distinguish series by labels as well as color.
- Provide an accessible numeric alternative and clear values when selecting a period.
- Show revenue for Printing, Photocopying, Sticker printing, and each reusable service with activity.
- Within Printing, allow an A4 B&W / A4 Colour breakdown where those options were recorded.
- Show expenses assigned to individual services and a separate **Shared services** group.
- Show expense categories such as Paper, Ink/Toner, Sticker sheets, Machine repair/maintenance, and Other.
- Do not claim per-service profit by allocating shared expenses without an agreed allocation method.
- Tap a revenue total to open its bill lines; tap an expense total to open its expense entries. Preserve the selected period and filters.
- Empty periods should say no activity and show zero, not an error. A negative comparison is valid and should not be hidden.

### Completeness and exports

Show when service revenue may be incomplete due to pending bills from the second phone, synchronization errors, or historical sales lacking line details. If the second phone is unreachable, do not claim its pending count is known to be zero.

Offer PDF and spreadsheet export using the same filters, labels, amounts, warning text, generation time, and date range. Exports must retain the “not exact service profit” explanation. Totals must reconcile to the on-screen report.

## 14. Owner service expense entry

Provide **Add service expense** on the owner Services report and allow the existing general expense form to classify an entry as a service expense. Both routes save to the same underlying expense ledger.

| Field | Behavior |
|---|---|
| Category | Required: Paper, Ink/Toner, Sticker sheets, Machine repair/maintenance, or Other. |
| Amount | Required positive rupee amount; use existing currency precision. |
| Expense date | Required, default today; editable for previously incurred expenses. |
| Applies to | Required: Printing, Photocopying, Sticker printing, a reusable service, or Shared services. |
| Note | Optional description, supplier reference, or explanation. |
| Photo/document | Optional receipt or invoice attachment. |

Example: Ink/Toner, ₹1,200, expense date 31 October, Shared services, note “Used for printing and photocopying.” This is one expense, not ₹1,200 against each service.

Use the existing payment workflow consistently if expenses already support paid/unpaid status. Payment date and expense date are different concepts: paying an existing expense later must not create another expense. Do not introduce automatic cash-register deductions, supplier due changes, or a new payment model through this feature without a separately defined integration.

### Validation and save behavior

- Reject missing category, target, date, or amount, invalid numbers, and negative/zero amounts.
- Save exactly once despite repeated taps; preserve form values after recoverable errors.
- Do not show success until the record is durably saved.
- Reject unsupported/unreadable attachments with a useful message. Do not silently lose the amount or other entered fields.
- Permit saving without an attachment by an explicit choice if attachment processing fails.
- Warn on a likely duplicate (matching date, amount, category, and target) but allow intentional separate expenses after review.
- Record creation/update timestamps, stable expense ID, and origin device.

### Editing and removal

- Show Edit and Void in the owner expense detail actions.
- Require a reason when voiding a posted expense; retain its original amount, date, and history.
- Editing updates the same record and records before/after values; it must not append an unintended duplicate expense.
- Voided entries are excluded from active totals but available in history with a clear status.
- Changing the date moves the expense to the correct reporting period; changing its target/category moves the breakdown without changing overall total.
- Archiving a service does not delete its expenses or historical revenue.

## 15. Financial rules and reconciliation

### Revenue recognition

Include service charges when the bill is completed, whether paid immediately or sold on credit. Exclude draft/held bills. Later repayment settles credit and must not add service revenue again.

Store discounts allocated to bill lines in paise with deterministic rounding so line net amounts sum exactly to the final bill total. Refunds/reversals use the original line's net charge and already-refunded quantity/amount to avoid over-reversing revenue.

Report a later service cancellation/return as a linked sales reversal on its effective date, preserving the original bill date and amount. A cash refund payment is not a second revenue deduction if the sale reversal has already reduced revenue. Returning a product from a mixed bill must not reduce service revenue.

### Expense recognition and no double counting

Use the expense's entered effective date in the report, not its attachment date or later payment date. The Services report is a filtered view of the existing expense ledger, not a second ledger.

- A ₹500 service expense appears as ₹500 in Services and contributes ₹500 exactly once to overall expenses.
- Shared services expense is included once in the combined services total and stays in its shared bucket.
- An expense reclassified from General to Services remains one record; overall expenses do not increase.
- Do not record the same inventory purchase as a service expense merely to make it appear in this report. A supply acquired for service use needs a clear expense classification, avoiding duplicate purchase/cost entries.
- Buying supplies does not establish how much was consumed. This is why the revenue-minus-expenses figure remains a comparison, not exact profit.

### Worked report example

For one selected period:

| Entry | Amount |
|---|---:|
| Printing net revenue | ₹1,000 |
| Photocopying net revenue | ₹600 |
| Sticker printing net revenue | ₹400 |
| Total net service revenue | ₹2,000 |
| Printing paper expense | ₹300 |
| Shared ink/toner expense | ₹500 |
| Sticker sheets expense | ₹100 |
| Total recorded service expenses | ₹900 |
| Revenue minus recorded expenses | ₹1,100 |

Label ₹1,100 as the comparison described above. If ₹400 of those sales is unpaid, revenue remains ₹2,000 and credit remains ₹400; do not imply ₹2,000 was collected. If unrelated shop expenses are ₹700, overall expenses are ₹1,600, not ₹2,500.

## 16. Data integrity, offline use, and backup

- Owner expense creation and reports work from local stored data without internet.
- Save expense records and audit history atomically. An interruption must leave either a saved record or an unsaved draft/error, not a partial counted record.
- Expense IDs remain stable across exports, backup, restore, and any retry.
- Include service definitions, service bill snapshots, expense records, audit history, and attachments in backup/recovery coverage, with explicit exclusions if any.
- Receiving a delayed billing-phone service bill updates the appropriate sales period using its recorded effective date and unique ID. Display sync completeness separately.
- Do not distribute service expenses to the billing phone as part of service catalog synchronization.
- Use integer minor currency units or equivalent exact decimal handling; do not accumulate binary floating-point errors in totals.

## 17. Detailed report and expense test plan

These are required future tests, not claims of tests already run. Use an isolated dataset and test both roles. Preserve expected values, actual results, build version, and evidence for each case.

| ID | Scenario | Expected result |
|---|---|---|
| SE01 | Run the worked ₹2,000 revenue / ₹900 expense example | Cards show ₹2,000, ₹900, ₹1,100 with the non-profit explanation; service and category breakdowns reconcile. |
| SE02 | Add unrelated expenses ₹700 | Overall expenses ₹1,600; service expenses remain ₹900. |
| SE03 | Mixed bill: products ₹160, services ₹40, whole-bill discount 10% | Net product revenue ₹144, service revenue ₹36, combined ₹180. |
| SE04 | Uneven discount allocation across three lines | Rounded allocations sum exactly to the bill discount; no missing/extra paise. |
| SE05 | Complete ₹100 service sale with ₹60 paid and ₹40 credit | Service revenue ₹100, collection ₹60, debt ₹40; no implied ₹100 cash collection. |
| SE06 | Repay that ₹40 next period | Debt becomes zero; repayment is collection, not another ₹40 of service revenue. |
| SE07 | Hold a service bill without completing | No report revenue; resume retains entered rate. |
| SE08 | Reverse/refund ₹20 of a service sale | Net service revenue decreases once by ₹20 on reversal date; refund payment does not deduct revenue again; product stock unchanged. |
| SE09 | Return only the product from a mixed bill | Product figures adjust; service revenue remains unchanged. |
| SE10 | Add ₹500 Shared services toner expense | Service expense total increases by ₹500 once; no automatic split across services. |
| SE11 | Reclassify an existing ₹500 general expense as Services | Overall expense total unchanged; service expense total increases ₹500. |
| SE12 | Edit expense ₹300 to ₹250 | Totals decrease ₹50; same record ID with before/after history. |
| SE13 | Void ₹100 expense with reason | Active totals decrease ₹100 once; voided record and reason remain in history. |
| SE14 | Move an expense into another date range | It leaves the old range and enters the new range exactly once. |
| SE15 | Change service target or category | Only breakdown attribution changes; combined expenses unchanged. |
| SE16 | Submit blank, zero, negative, malformed amount or missing required fields | Specific validation; no record created; valid fields retained. |
| SE17 | Double-tap Save, retry after delayed response, restart during save | At most one committed expense; recoverable state without partial totals. |
| SE18 | Enter a likely duplicate | Warning shown; cancel leaves totals unchanged; intentional confirmation creates one additional distinct record. |
| SE19 | Add/replace attachment, then simulate unreadable file or storage failure | Clear error; no false success; saved expense and attachment state remain coherent. |
| SE20 | Try expense routes/commands as Billing role, including forged incoming event | Access denied; no mutation or sensitive expense data delivered. |
| SE21 | Archive or rename reusable service with past entries | Historical bill snapshots retained; expenses remain attributable and counted. |
| SE22 | No activity; then expenses with zero revenue | Zero empty state first; later negative comparison displayed correctly, not hidden. |
| SE23 | Midnight, week/month boundary, and custom dates in shop timezone | Records appear in exactly the intended period; UI states range and grouping clearly. |
| SE24 | Switch daily/weekly/monthly chart grouping | Period total unchanged; buckets sum to cards; graph and accessible values agree. |
| SE25 | PDF/spreadsheet export after filtering | Export dates, rows, totals, completeness warnings, and non-profit explanation match screen. |
| SE26 | Billing phone has unsynced service bill, then reconnects and retries | Incomplete/unknown status before receipt; correct period updates once after receipt. |
| SE27 | Record an expense offline and restart owner phone | Entry persists once; reports work locally. |
| SE28 | Back up and restore into isolated installation | Definitions, service sales, expenses, history, and included attachments reconcile exactly. |
| SE29 | English/Urdu, long service names, large text, negative comparison | Readable layout and valid direction/currency formatting; actions remain reachable. |
| SE30 | Dataset with many service bills and expenses | Filtering, scrolling, and graph changes remain responsive; establish measurable performance targets on actual owner phone before release. |

### Release criteria

- Existing service-billing tests in Section 10 and the report/expense tests above pass.
- No unauthorized billing-phone expense access, duplicate expense counting, or repayment counted as revenue.
- Revenue, discount, reversal, and expense figures reconcile to source records and exports.
- No screen or export presents service revenue or the comparison figure as exact service profit.
- Backup recovery and offline persistence are verified on isolated data.
- Owner can complete expense entry, inspect supporting records, and correct/void an entry without developer assistance.

## 18. Updated scope boundary

Service expense recording is now included and restricted to the owner phone. Per-job cost estimation, consumable usage tracking, depreciation, exact service profit, and automatic shared-cost allocation remain excluded. The service quantity-unit clarifications in Section 11 still apply. This document specifies the feature and tests; it does not claim implementation or test completion.

## Part C — Preservation, integration, and deployment requirements

### C1. Baseline and safe development

- Record the current source revision, app/build identity, signing certificate, schema version, and enabled features before changes.
- Preserve uncommitted user work. Use an isolated development branch/checkpoint and separate test app/device data where possible.
- Establish a baseline for existing screens and transactions so pre-existing defects are distinguished from new regressions.
- Capture a verified backup and restore it into an isolated installation before upgrading active data. A 0 KB label or success toast is not proof of a valid backup.
- Use representative anonymized/test copies for destructive, reversal, restore, and migration tests. Do not send real receipts during testing.
- Keep signing keystore files/passwords and device pairing secrets out of source control, shared project ZIPs, logs, and this document. Reuse the compatible existing release key; do not generate a replacement as an update workaround.

### C2. Schema and accounting integration

- Add versioned, tested database migrations. Never use destructive migration fallback or a reset to resolve schema changes.
- Existing product-only bills retain their meaning, original prices, costs, discounts, dates, receipt numbers, payments, and history. Explicitly migrate their line type to Product if needed.
- Introduce service lines without creating stock movements or default rates. Do not reinterpret old products as services based on names.
- Extend the existing expense ledger with classification/target fields. Do not copy records into a second service-expense table that totals independently.
- Existing expenses should keep their original category/classification unless the owner explicitly reclassifies them. Missing old classification is not permission to infer service expenditure.
- Preserve every existing identifier and relationship. Introduce global identifiers or mappings without breaking historical references.
- Currency rounding, discounts, payment allocation, credit, refunds, and returns must share tested rules across both device roles.
- Keep product gross profit separate from service revenue; do not assign zero service cost and then present revenue as profit.
- Do not assume customer credit collections are new revenue or that expense payment creates another expense.

### C3. Ordered delivery with independent gates

1. Inspect matching source and establish backup/recovery and regression baseline.
2. Stabilize known data-loss, crash, and financial correctness risks needed for this work.
3. Implement service definitions and service bill lines in single-phone mode; pass mixed-bill and existing product-billing tests.
4. Implement owner expense classification and Services reporting; pass no-double-counting and permissions tests.
5. Add transaction IDs/outbox and protocol foundations without enabling pairing; re-run single-phone regressions.
6. Add secure pairing, catalog sharing, bill transfer, acknowledgements, and conflict visibility behind explicit activation.
7. Run all specified two-device, interruption, migration, and backup/recovery tests.
8. Produce a compatible signed release APK and release notes; conduct an isolated upgrade rehearsal before active-device rollout.

A phase may ship independently only when its dependencies and tests pass. Do not require the second phone to exist before the owner can use services or record expenses.

### C4. Existing-app regression matrix

| Existing capability | Required preservation evidence |
|---|---|
| Launch and local lock | Old settings retained; biometric/PIN flow, timeout, restart and resume work. |
| Product catalog and variants | Existing names, identifiers, barcode mappings, quantities and piece/box conversions survive migration. |
| Product-only billing | Prices, overrides, discounts, payment/credit, receipts and one-time stock deduction still reconcile. |
| Scanner/manual selection | Existing scan and no-barcode workflows remain available; service screens do not replace them. |
| Purchasing and stock receiving | Supplier dues, proportional freight allocation and weighted average cost retain correct results. |
| Accounts and repayments | Old balances/history preserved; settlement does not count as new revenue. |
| Returns/cancellations | Original transaction references retained; reversal affects stock/money/profit once. |
| Excel import | Valid changing shop files still import; wrong/malformed files report errors without crash or partial silent changes. |
| Expenses | Original entries counted once; reclassification changes service breakdown only. |
| Reports and exports | Historical totals reconcile before/after migration except explicitly documented defect corrections. |
| Urdu and receipts | Both languages retain usable layout, numbers, fonts and output; selected preferences persist. |
| Backup/restore | Old supported backups handled through a tested compatibility path; new backups contain required service/sync data. |
| Single-phone offline mode | All established owner workflows remain available without pairing/network. |

Record counts, balances, stock totals, selected historical receipts, and attachment availability before and after migration. Test upgrade from every supported existing schema, interrupted upgrade behavior, and sufficient/insufficient storage.

### C5. Failure handling and rollback

- Feature switches can stop new pairing or disable unfinished UI; they cannot erase committed records or queues.
- Preserve and expose pending transactions if sync is disabled. Do not mark them acknowledged merely to clear an error.
- Do not treat installing an older APK as a safe rollback after a schema upgrade. Older versions may not understand new records.
- Prefer a tested forward fix. A rollback/restore procedure needs a compatible build, verified backup, and reconciliation of all records created after that backup.
- Never restore a main-phone backup over active data without an explicit, reviewed recovery action and accounting for pending second-phone bills.
- Keep failures actionable: what failed, whether data was saved, and how to retry/recover without duplicating it.

### C6. Test execution and evidence

Run ALL tests in Parts A and B, plus the regression matrix above. Use known sample values and assert actual database/report outcomes, not screenshots alone. Test single-phone, two-phone connected, and disconnected operation separately.

Use two real Android phones for final Wi-Fi, camera, lifecycle, lock, battery saver, and reconnect testing. An emulator is useful during development but is not sufficient proof of real-device reliability. If only one phone is available, clearly list which tests remain unverified and do not call the two-phone release fully tested.

For each test record: test ID, expected/actual values, build versions, devices/Android versions, pass/fail, and supporting evidence. Repeat failed/dependent tests after fixes. Include no-double-counting, no unauthorized expense access, and no data-loss failures as release blockers.

### C7. Build and distribution

- Deliver one signed release APK for both phones, preserving application ID and signing compatibility and incrementing the version code.
- Main phone is Owner; second phone pairs as Billing. Do not expose an unprotected switch that lets Billing promote itself to Owner.
- Send the APK by a user-selected file-sharing method. The brother receives the APK, not the signing keystore or passwords.
- Install compatible updates over the existing app; do not ask the user to uninstall as a routine update step.
- Provide short setup/pairing instructions, backup/file-location guidance, update instructions, and honest limitations for unsynced bills/background connectivity.
- Deliver implementation status, test results, migration evidence, known remaining issues, and release notes alongside the APK.

### C8. Completion definition

The work is complete only when the approved feature set functions, all applicable acceptance/regression tests pass, existing data is verified preserved through upgrade, permissions are enforced, and any untested device behavior is explicitly disclosed. Do not present a design preview, mocked sync indicator, or unexecuted test list as completed implementation.


<!-- END COMPLETE ORIGINAL COMBINED HANDOFF -->
