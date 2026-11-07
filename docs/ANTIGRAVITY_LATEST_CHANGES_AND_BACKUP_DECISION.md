# Ali Educational — Latest changes and answer to Antigravity

Date: 1 November 2026

## Latest approved Cash In/Out addition — categories and photo evidence

The user confirmed that each Cash In/Out transaction needs a dedicated Category selector, with selection from existing categories or creation of a new category, plus an optional photo attachment.

### Entry and detail screens

- Show Category as a clearly labelled field separate from description/notes, amount, date/time and payment source/destination.
- Provide a searchable existing-category list and an Add category action. Creating a category from an entry form returns to the same form with the new category selected, preserving all entered values and any photo.
- Require a category for new manually entered transactions, with General available when appropriate. Existing uncategorized records remain visible and must not disappear from totals.
- Preserve existing user categories. Suggested familiar examples include Rent, Electricity and Transport; examples are not instructions to overwrite current classifications.
- Category creation needs a nonblank trimmed name; prevent confusing duplicates using case/whitespace-normalized matching. Use stable category IDs so renaming does not break records.
- Allow Owner rename/archive. Archive hides a category from new selection but preserves historical labels, records and reporting; do not cascade-delete transactions.
- Show category and an attachment indicator in history/detail. Add category filtering with an All categories option; date/source filters and totals must continue to reconcile.
- Categories are organizational labels, separate from accounting type. Selecting a category must not silently convert a savings transfer into an operating expense or sale.

### Pigmy integration

- Automatically assign the Pigmy category to linked deposits and withdrawals. Keep scheme ID/name and deposit/withdrawal ID as separate data; do not encode all logic only in the text label.
- Deposit from cash = linked Cash Out savings transfer; withdrawal into cash = linked Cash In savings transfer. Neither is sales income or an operating expense.
- Protect the system category's identity even if its display name changes. Manual classification as Pigmy does not automatically create a scheme deposit or withdrawal; guide the user to the Pigmy workflow when a linked scheme transaction is intended.
- For linked entries, perform financial edits/cancellation through the corresponding Pigmy record or a shared atomic command so the two records cannot diverge. Evidence/category display must not produce another cash entry.

### Optional photo attachment

- Offer Attach photo with Take photo and Choose photo using supported Android camera/photo-picker flows. One optional photo per transaction is sufficient for this requested scope; do not add unrelated document-management features.
- Show a thumbnail and allow full preview, replacement or removal before saving. Allow the Owner to add/replace/remove evidence on an existing entry without changing its amount/date or creating a duplicate transaction.
- Cancellation or denied permission leaves the entered form intact. Show actionable errors for unreadable/unsupported images or insufficient storage. Do not silently drop the photo while reporting successful attachment.
- Copy the selected image into durable app-managed attachment storage, retaining correct orientation and enough resolution to read receipt details. A temporary picker URI or camera cache path is not permanent evidence storage.
- Coordinate file and database persistence so interrupted saves cannot leave a transaction claiming a nonexistent attachment. Preserve the previous photo until replacement succeeds. Clean up abandoned temporary files safely.
- If image processing fails, let the user explicitly retry or choose Save without photo. Saving without evidence must not happen silently.
- Optional photo/note editing on Pigmy-linked entries must preserve the same financial record and cash link. Use a shared attachment reference or a single clearly defined owner record to avoid inconsistent duplicate copies.
- Respect existing Owner/Billing restrictions. Do not send Owner-only financial evidence to a restricted Billing phone. Do not attach transaction evidence to customer WhatsApp receipts automatically.

### Backup, restore and exports

- Include actual photo bytes, attachment IDs, transaction relationships and required metadata in complete backups, including manual copies saved through Drive/Files. Backing up only a phone-local path is insufficient.
- Restore attachments into valid new locations and reconnect references. Verify a photo can be opened after restore on a separate installation, even when the original phone path is unavailable.
- Preserve category definitions, archived status and stable relationships through migration, backup, restore and applicable sync.
- Add a Category column to cash spreadsheet exports and accept it when importing supported layouts. Keep Notes separate. Older files without a category must show a clear preview/default rather than being rejected or silently misclassified.
- Spreadsheet exports can show an Attachment present indicator; clearly state that a standalone spreadsheet does not contain the original photo or constitute a complete backup. Do not export private temporary paths as useful attachment links.

### Acceptance tests

1. Create a cash entry, select a category, save/restart and verify category, amount, date/time and notes remain correct.
2. Add a new category mid-form: form values/photo survive and the new category becomes selected. Reject blank/duplicate names without duplicate records.
3. Rename/archive a used category: historical transactions remain accessible and counted; new-entry selection respects archive state.
4. Filter by category and date: matching transaction totals reconcile with All categories and exports.
5. Record Pigmy deposits and withdrawals in several schemes: automatic category, correct scheme attribution, one cash effect and no expense/profit misclassification.
6. Add a photo from camera and gallery; verify orientation, readability, restart persistence and full preview. Cancel/deny permission without losing entered values.
7. Replace/remove a photo on an existing entry: no change to cash totals, no duplicate transaction, old image retained until replacement commits.
8. Simulate storage failure/interruption during save or replacement: recoverable form/error and consistent transaction/attachment references.
9. Back up categorized entries with photos, save through Drive/Files, retrieve and restore in isolation. Compare categories, IDs, amounts, timestamps and image contents; open restored photos successfully.
10. Import old/new cash-ledger layouts and re-export: categories/notes remain distinct; spreadsheet attachment indicator does not falsely promise photo recovery.
11. Test repeated taps, linked-record editing and existing sync retries for exactly-once financial effects; verify restricted-role access to entries/photos.

This is an implementation and test specification only. No app changes or tests are performed by this documentation update.

## Latest approved Pigmy expansion — withdrawals without interest

This section supersedes the earlier deposits-only/no-withdrawals restriction. The user confirmed that his brother withdraws deposited savings and receives NO interest. Preserve expandable schemes and all earlier deposit/cash-link behavior.

### Withdrawal flow

- Add Record withdrawal to each scheme's detail screen. Preselect the scheme; enter a positive amount, effective date/time, destination (shop cash or bank) and optional note/reference. Save only after confirmation of actual receipt.
- A withdrawal received into shop cash automatically creates exactly one linked Cash In entry labelled “Pigmy withdrawal — [scheme name]”. A bank receipt must not increase physical shop cash.
- Classify it as return of savings/transfer, not sales revenue, service revenue, customer repayment or profit. Do not add interest fields, calculated interest or interest income.
- Save withdrawal and destination-ledger effect atomically using stable IDs. Repeated taps, retries and sync must not duplicate effects.
- Editing/cancelling updates or reverses the linked receipt once; retain audit history. Changing destination reverses the old destination effect and applies the new one atomically.

### Scheme balances and history

- Show Deposits, Withdrawals and Tracked balance separately for each scheme and in the combined summary, applying date ranges consistently.
- Balance = confirmed opening balance + active deposits − active withdrawals. Distinguish period net movement from closing balance; use earlier activity when calculating an ending balance.
- Keep a dated combined history with direction and destination/source, plus separate filters for deposits and withdrawals. Cancellation status remains visible in history but excluded from active totals.
- Label this a tracked balance, not a bank-verified figure. If historical deposits are missing, clearly mark incomplete history; do not invent deposits or silently treat an unknown opening balance as confirmed zero.
- Before introducing a historical opening balance, ask the Owner to confirm the amount and effective date. This establishes starting savings only and must NOT create a new Cash Out entry or be counted as a new deposit.
- If withdrawal exceeds available tracked savings, show the discrepancy and require review/correction of missing history or opening balance before posting. Do not silently create negative savings or fabricate a balancing deposit.
- Preserve current deposit/transfer IDs through migration. Include withdrawals, opening-balance provenance, links and audit history in backup/restore, exports and relevant sync. Owner-only administration remains required.

### Required regression tests

1. Complete test history: deposits ₹10,000, withdrawal ₹3,000 to shop cash → tracked savings ₹7,000 and one Cash In transfer ₹3,000; sales and profit unchanged.
2. Same withdrawal to bank → savings ₹7,000 and bank receipt ₹3,000; physical shop cash unchanged.
3. Edit ₹3,000 withdrawal to ₹2,500 → balance ₹7,500 and linked receipt ₹2,500, not an additional receipt. Cancel → balance ₹10,000 with receipt reversed once.
4. Double tap/retry/restart/sync: one committed withdrawal and one destination effect. Cancelling an unsaved form changes nothing.
5. Validate missing scheme/destination, blank/zero/negative amount and withdrawal above tracked funds. Explain incomplete history without guessing a starting amount.
6. Confirm a historical opening balance: savings changes appropriately, no new cash transfer or deposit income is created. Date ranges distinguish opening, movement and closing totals correctly.
7. Use several schemes: withdrawing from A leaves B/C unchanged; combined totals reconcile. Renaming a scheme retains links.
8. Restore an isolated backup and compare deposits, withdrawals, opening values, effective timestamps, balances and linked cash records. No replayed cash entries.
9. Reports/exports show withdrawals as savings transfers and contain no interest income. Existing cash filters, deposits and bill collections remain correct.

These are implementation requirements and future tests for Antigravity, not confirmation that the app already supports withdrawals.

## Message to Antigravity

Implement the latest requirements below in the current app. Preserve all completed work and existing shop data. This document specifies requested changes and tests; it does not claim the app has already been changed.

My answer to your backup-policy question is: **keep manual on-demand “Save Copy to Google Drive / Files” for this release. Do not implement unattended automatic background uploads or require Google Cloud OAuth credentials for this phase.**

Follow these detailed requirements and complete isolated testing before asking to install a production update. Do not repeat unrelated earlier implementation work.

## 1. Manual Google Drive / Files backup — confirmed decision

### User flow

1. The user taps Back up now. Create and validate a consistent, complete local backup first.
2. Show the result and offer Save Copy to Google Drive / Files.
3. Open Android's native document picker so the user can select an available Drive account/folder or local destination.
4. Write the complete archive through the selected content URI and close the stream successfully. Preserve the original local backup.
5. Display the filename and selected destination information that the provider actually exposes. Do not fabricate a filesystem path for a cloud document.

### Scope and honest status

- Keep the existing native picker approach; no custom Google login/password collection or OAuth project setup for this release.
- Drive must be available through a supported installed document provider. If unavailable, explain this and allow another destination or cancellation without losing the local backup.
- Distinguish “Local backup created” from “Copy saved to selected provider”. A provider can accept a file locally while its cloud upload remains pending.
- Do not say “Uploaded to Google Drive” merely because the picker opened or a share intent launched. Show cloud-confirmed status only when the implementation can actually verify it.
- Show last successful local backup separately from last completed external save. Preserve previous successful timestamps when a new attempt fails.
- Add a simple visible reminder to save an external copy after shop closing and before updates. Do not claim a reminder performs an upload. Notification scheduling is not an approved automatic-upload feature.
- Use dated filenames and retain previous backups. Do not silently overwrite or delete old files.
- User cancellation, offline providers, revoked access, full storage, unavailable account and write failures must leave a clear recoverable state and allow retry.
- Preserve records, settings, attachments and branding in backup coverage. A local database file and metadata alone do not prove everything is covered.
- Drive is a backup destination, not a live database or substitute for two-phone synchronization. Show when pending billing-phone records have not reached the Owner backup.

### Required Drive/backup verification

Create synthetic Excel stock and cash-ledger files, import into a separate test app, perform representative transactions and record expected quantities/balances/IDs. Create a backup through the real button, save a copy to a test Drive folder, retrieve it and restore into another disposable database. Compare all records, links, amounts, dates, settings, branding and attachments against the expected manifest.

Test cancellation, unavailable Drive, offline save behavior, write failure, retries, two dated backups, corrupted archive and unsupported schema. Show actual restore evidence, not just a successful picker or nonzero ZIP size. Never restore over live shop records or send test receipts to real customers.

## 2. Pigmy accounts and automatic Cash In/Out linkage

The brother currently has THREE schemes, with the ability to add more later. Each scheme has its own name, usual daily deposit amount, history and totals; show a combined recorded-deposits total too. Do not hardcode real names/amounts or impose a three-scheme limit.

- Add deposit preselects the chosen scheme and prefills its usual amount. The user can change it and confirms actual payment before saving. No automatic daily postings.
- Save stable scheme/deposit/transfer IDs. A confirmed cash deposit creates exactly one linked Cash Out savings transfer labelled with the scheme name. Save both atomically.
- A bank-funded deposit must not reduce physical cash. Record the actual funding source clearly.
- Editing/cancelling a deposit updates/reverses its existing linked transfer once. Changing its scheme changes attribution without creating another outflow.
- Savings transfers do not count as operating expenses or reduce product profit. No duplicate manual Cash Out entry should be needed.
- Deposits only: no interest, withdrawals or maturity payouts in this phase. Label totals Recorded deposits rather than claiming a verified bank balance.
- Preserve existing deposits when migrating: do not copy them into every scheme. Keep unknown attribution visible for Owner review without generating another cash effect.
- Restrict scheme administration to Owner, preserve history on archive and include schemes/links in backup, restore and applicable sync.

Tests: create schemes A/B/C with synthetic deposits ₹100/₹200/₹300 → totals ₹100/₹200/₹300 and combined ₹600, with exactly three cash transfers. Edit B to ₹250 → combined ₹650. Cancel C → active combined ₹350. Verify no profit effect and no duplicate transfers after retries/restart. Add D and E, rename/archive/reactivate schemes, restore all five and compare IDs, totals and links. These are test values, not production defaults.

## 3. Receipt cleanup and scheme icon changes

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


## 4. Before asking me to install the update

- Identify the current project/build and preserve existing work. Use an isolated test app/database.
- Run the tests above and regressions for affected billing, reports, receipts, navigation, cash entries and backup workflows. Distinguish executed tests from unverified items.
- Show actual before/after rendered receipt evidence, phone screenshots, scheme/cash reconciliation results and successful isolated backup restoration.
- Verify nondestructive migration and compatible signing identity. Never uninstall the live app, clear its storage or use destructive migration to make the update install.
- Provide candidate APK version/code/hash, passed/failed/unverified tests, limitations and recovery steps.
- Request explicit approval before updating the production app. A connected phone is not authorization to overwrite its live data.

Keep the app interface and receipt headings English only. Urdu/mixed-script product names must still render correctly. Receipt Preferences remains removed from Settings. Do not introduce unrelated new features or reset earlier completed work.