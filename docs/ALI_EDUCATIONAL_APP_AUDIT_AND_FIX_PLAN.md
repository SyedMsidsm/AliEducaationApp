# Ali Educational — App Audit and Fix Plan

Review date: 31 October 2026  
Purpose: Record the problems observed on the connected Android phone, explain the required improvements, and define how to verify them.

## 1. Scope and evidence limits

This report covers inspection of Home, Stock, product editing, Excel import entry, stock receiving, Billing, camera scanner, Accounts, repayment and payment dialogs, Expenses, Gross Profit, Backup & Restore, and Settings.

The inspection did not save a sale, payment, stock receipt, expense, or product change. Urdu was selected to inspect its behavior and English was restored. Lock protection and the two-minute timeout were restored after an accidental navigation tap. No app source was changed.

This was a non-destructive device review, not a complete end-to-end test. Opening a screen does not establish that its saving, calculations, export, or recovery works correctly. Existing records may be test records. Their values are evidence of the current app state, not proof of how the app produced them.

### Installed app and source mismatch

- Phone: Samsung SM-S928B.
- Installed package: `com.alieducational.app`, version `1.0.0`.
- Workspace project: `com.alieducational.inventory`, version `0.2.2-stock`, with a debug application suffix.
- The source in this workspace cannot be assumed to represent the installed app. Obtain or locate the matching source before implementing fixes or attributing a UI issue to a specific code defect.

### Evidence labels

- **Observed:** directly seen during the inspection.
- **Needs verification:** a possible defect or missing feature that requires reproduction, file inspection, or a transaction test.
- **Proposed fix:** recommended behavior, not a claim about the current implementation.

## 2. Fix order

| Order | Area | Reason |
|---|---|---|
| 1 | Backup validity and recovery | Shop records must be recoverable. |
| 2 | Stock costing and profit | Incorrect figures can mislead purchasing and pricing decisions. |
| 3 | Zero-price billing and stock warnings | Prevent accidental incorrect sales. |
| 4 | Urdu localization | A requested daily-use feature is incomplete. |
| 5 | Sales completeness and reporting | Reports must explain exactly which records they include. |
| 6 | Receipt defaults and payment details | Complete the agreed workflows. |
| 7 | Account lists, navigation, report controls | Improve clarity and everyday usability. |

## 3. Detailed findings

### F01 — Backups display 0 KB despite a success message

**Status:** Display behavior observed; actual archive validity unverified. High priority.

The Backup & Restore page listed four dated ZIP backups, each as `0 KB`. It also reported a successful backup on 31 October 2026 at approximately 07:18 am. Home described the backup as stored safely on the phone.

**Why it matters:** These may be empty files, an incorrect size display, or small files rounded down. The UI alone cannot establish which explanation is correct. A success message currently gives confidence that has not been verified.

**Required investigation and fix:**

1. Inspect actual byte sizes and archive contents in the matching app.
2. Verify the database snapshot is complete and internally consistent.
3. Include required records, settings, and attachments, or clearly document exclusions.
4. Validate the completed archive before updating the last-successful-backup timestamp.
5. Display meaningful sizes, including bytes for very small files, and actionable failure messages.
6. Write backups safely so interrupted creation cannot replace a valid backup with an incomplete one.
7. Distinguish a backup on the same phone from a separate copy. Same-phone storage alone does not protect against losing the phone.
8. Confirm the planned Google Drive/manual export options and scheduling against the matching implementation.

**Acceptance tests:** Create a backup containing known products, stock movements, sales, dues, repayments, expenses, and attachments. Restore it into an isolated test installation and compare records and totals. Test a corrupt archive, interrupted backup, insufficient storage, and an incompatible backup version. Existing valid backups must remain usable after a failure. Do not test restoration over the owner's active records.

### F02 — Negative inventory produces suspicious expected profit

**Status:** Figures observed; formula defect needs verification. High priority.

The notebook showed `−2 pieces`, average cost `₹560`, and selling price `₹0`. Home showed stock value `−₹1,120`, while the Gross Profit page showed expected inventory profit `+₹1,120`.

**Why it matters:** Multiplying a negative quantity by a negative margin can produce a positive number mathematically, but this is not meaningful expected profit from stock available to sell. Existing data could also be inconsistent.

**Proposed behavior:**

- Flag negative stock as a discrepancy requiring reconciliation.
- Do not present negative quantities as profitable inventory potential. Exclude them from the sellable-stock estimate and disclose excluded products.
- Show missing-price and missing-cost warnings instead of silently treating unknown values as reliable profit inputs.
- Keep expected profit, realized gross profit, stock value, and expenses clearly separate.
- Preserve historical cost applied to each sale so later purchases do not rewrite past profit.
- Verify delivery allocation and weighted average costing, which were explicitly selected in the plan.

**Required calculation example:** Receive 10 units costing ₹500 plus ₹50 delivery. Landed total is ₹550 and cost per unit is ₹55. At ₹80 selling price, expected gross profit before any sale is ₹250. Sell 2 units for ₹160: cost of goods sold is ₹110 and realized gross profit is ₹50. Remaining expected profit is ₹200. If the two-unit sale receives a ₹16 discount, realized gross profit becomes ₹34. Shop expenses remain separate.

**Weighted average example:** With 10 units at ₹55, receive another 10 at a landed cost of ₹65 each. The new average cost is ₹60 across 20 units. For an oversold product, explicitly define and test the reconciliation policy rather than blindly applying this formula to negative stock.

**Delivery allocation test:** For purchase lines worth ₹300 and ₹200 and delivery of ₹50, allocate ₹30 and ₹20 respectively. Handle rounding so allocations sum exactly to the entered delivery charge. Define behavior for zero-value purchase lines.

### F03 — Zero selling prices and zero-value recorded sales

**Status:** Observed; origin of records and validation behavior unverified. High priority.

The notebook's selling price was ₹0. Billing displayed a zero price, and two existing itemized sales displayed net totals of ₹0. These records may have been created for testing.

**Proposed fix:**

- Clearly distinguish a missing selling price from an intentionally free item.
- Prompt for a price before normal checkout when the price is missing.
- Require an explicit free-item or full-discount choice when zero value is intentional.
- Warn about below-cost selling without silently preventing an intentional discount.
- Show negative-stock warnings and require the owner to acknowledge an overselling decision, consistent with the agreed warn-but-allow policy.
- Do not alter existing zero-value sales automatically; review their origin first.

**Acceptance tests:** Missing price, explicit free item, full discount, below-cost sale, no stock, negative stock, piece/box prices, and repeated checkout taps. Confirm stock deducts only once and payment, credit, receipt, and profit records agree.

### F04 — Urdu selection changes direction without complete translation

**Status:** Confirmed visible issue. High priority.

Choosing Urdu changed the layout to right-to-left, but most labels, navigation content, forms, dialogs, settings, reports, and scanner instructions remained English. Mixed text and currency also appeared awkwardly ordered, including trailing minus signs.

**Required fix:**

- Translate all user-facing text: headings, buttons, empty states, validation messages, confirmations, scanner instructions, reports, and errors.
- Use a consistent language resource system; changing layout direction alone is insufficient.
- Support Urdu shaping and an appropriate readable font.
- Format dates, quantities, currency, minus signs, invoice numbers, phone numbers, SKUs, and barcodes correctly in mixed-direction text.
- Preserve entered English product names when no Urdu name exists. Interface translation does not automatically translate product data.
- Persist the selected language across restart and device lock.
- Treat receipt language as a separate choice with a default and per-receipt override.

**Acceptance tests:** Walk every screen and dialog in both languages, including error and empty states. Check positive/negative amounts, long names, English brand names inside Urdu screens, large system text, receipt previews, generated PDFs, and printing. Verify English-to-Urdu-to-English switching without lost form data or incorrect navigation.

### F05 — Some daily sales totals cannot support stock or profit calculations

**Status:** Observed report warning; not necessarily a defect by itself.

The Gross Profit page reported one sale record without item details and marked actual profit incomplete. Two itemized records were also visible: `AE-20261031-4162` and `AE-20261031-1973`, both with net ₹0.

**Required behavior:**

- Distinguish itemized sales from summary-only daily cash entries.
- Derive product stock changes and cost of goods sold from itemized records.
- Do not invent product costs or stock deductions for a summary-only cash entry.
- Clearly identify excluded records, affected dates, and why profit is incomplete.
- Avoid counting a daily summary and the underlying itemized bills twice.
- Make correcting or linking an incomplete record traceable through history.

**Acceptance tests:** An itemized-only day, a summary-only day, and a day with both. Verify sales totals, outstanding credit, payment collections, stock movement, and gross profit without duplication. Repayment of an earlier credit sale must not become new sales revenue.

### F06 — Default receipt-sharing preferences were not found

**Status:** Needs verification; controls were not visible in inspected Settings.

Settings showed descriptive text about ordinary receipts, no GSTIN, a small logo, and black-and-white printing. The requested default delivery-method selector was not found.

**Required behavior:**

- Provide a default such as Ask each time, WhatsApp/share PDF, print, or save PDF, subject to supported device capabilities.
- Provide default receipt language: English or Urdu.
- Allow a one-time override when issuing a receipt.
- Keep receipts plain black and white with the approved logo and shop details.
- The shop is not GST registered; do not present a GSTIN or describe the receipt as a GST tax invoice.
- Keep a digital record even when the owner does not print or share it.
- Show items, quantities, prices, discount amount/percentage, final total, payment, and remaining credit clearly.

**Acceptance tests:** Save defaults and restart. Preview both languages, override once, cancel sharing, and handle unavailable print/share apps. Cancelling sharing must not create another sale or undo the completed sale. Do not send a real customer's receipt as a test.

### F07 — Payment method missing from account settlement dialogs

**Status:** Amount and notes/reference fields observed; no method selector visible.

Supplier payments and customer repayments should record how money moved, not only how much moved.

**Proposed fix:** Add Cash, UPI, Card, Bank Transfer, and Other; retain optional reference and notes. Support the agreed optional supplier payment attachment and customer repayment receipt. Show the payment in account history with date, amount, method, and balance afterward.

**Acceptance tests:** Partial payment, exact settlement, blank/zero/negative input, overpayment, duplicate taps, cancellation, and app interruption. Overpayment must follow an explicit policy rather than silently creating a confusing negative due. Validate that repayments are not counted as new sales. Preserve the owner's chosen manual cash-register adjustment policy for supplier/expense payouts.

### F08 — Zero-balance customer included under Customers Owe

**Status:** Observed.

Al-Huda School appeared under Customers Owe with a ₹0 balance, while the tab count showed one account and offered Receive.

**Proposed fix:** Count only customers with an outstanding positive balance in the debt view. Keep settled customers in All Customers with accessible history. Explain or disable repayment when nothing is owed unless customer advances are deliberately supported. Apply equivalent clarity to suppliers.

**Acceptance tests:** Positive, zero, and credit/advance balances; full settlement; payment reversal; filters and counts updating immediately.

### F09 — Requested report date controls were not found

**Status:** Needs verification; absence is not yet confirmed across the full app.

The inspected Gross Profit screen did not show the requested custom date range or daily/weekly/monthly graph controls.

**Required behavior:** Today, week, month, and custom date range; daily/weekly/monthly aggregation; clear labels for sales, gross profit, and collections. State when stock value is current rather than historical. PDF/spreadsheet exports must use the same filters and totals as the screen.

**Acceptance tests:** Day/week/month boundaries, empty periods, discounts, returns, credit sales, repayments, and matching exported totals.

### F10 — Selected navigation tab may not match displayed content

**Status:** Observed once; reproduction required.

After dismissing a repayment dialog and using Back, Home was highlighted while the customer accounts content remained visible. A transition timing issue is possible; this is not yet a confirmed persistent navigation defect.

**Proposed investigation:** Reproduce with waits after navigation. Check the current route and selected tab use the same state. Review Android Back behavior, dialog dismissal, language switching, and restoration after backgrounding.

**Acceptance tests:** Navigate between every tab, open/cancel a dialog, press Back, switch language, and resume the app. Selected tab and screen must agree after transitions settle. In-progress billing should follow an explicit save/discard/resume behavior.

### F11 — Offline status wording is ambiguous

**Status:** Wording observed; connectivity defect not established.

Billing displayed “Offline 100%” while the phone displayed a mobile-network indicator. This may mean fully offline-capable rather than disconnected.

**Proposed fix:** Use wording such as “Saved on this phone” or “Works offline,” and show backup status separately. Do not imply that local storage means an external backup exists.

**Acceptance tests:** Airplane mode, Wi-Fi/mobile data, interrupted backup/export, restart, and return to connectivity. Ordinary stock and billing work must remain available offline.

## 4. Features present but not fully validated

| Area | What was seen | Remaining verification |
|---|---|---|
| Camera scanning | Camera opens and browse/search fallback exists | Real barcode recognition, repeat scans, unknown codes, permission denial, torch, speed |
| Product editing | Product fields and pricing fields visible | Save validation, variants, pieces/boxes, duplicate barcodes |
| Excel import | `.xls`, `.xlsx`, `.csv` and about 3,000-row support advertised | Parse actual shop format, preview, mapping, errors, duplicates, rollback and performance |
| Goods receiving | Supplier, invoice, lines, delivery and payment/due fields | Correct saved quantities, costing, proportional allocation and supplier dues |
| Accounts | Supplier/customer lists and settlement dialogs | History, calculations, reversal, receipts, attachments and duplicate prevention |
| Expenses | Existing entries and add form | Save/edit behavior, reports, attachments if supported, separation from gross profit |
| Security | Lock toggle and timeout setting | Biometric/device credential behavior, timeout, app resume and recovery |
| Receipts | Settings description | Actual PDFs, Urdu text, logo, totals, sharing and printer output |

## 5. Remaining end-to-end test plan

Use a separate test installation or isolated dataset. Do not clear active shop data. Treat the supplied stock spreadsheet as an example of a changing shop file format, not a fixed permanent catalog.

### A. Import and opening stock

Test actual legacy `.xls` as well as `.xlsx` and CSV; changing column order; blank cells; invalid quantities; Urdu/English names; duplicate SKU/barcode warnings; manual opening stock; and approximately 3,000 products. Confirm a preview before commit, clear row errors, no partial silent import, and correct totals after restart.

### B. Fast billing

Test known and unknown barcodes, repeated scans, manual search, category browsing, favorites/recent items, quick-add, piece/box conversion, multiple suspended bills, and resuming a bill. No short-code workflow is required. Check response speed on the actual phone.

### C. Discounts and credit

For a ₹100 bill paid at ₹90, ask whether ₹10 is a discount or remaining credit. The discount case must show ₹10 and 10% and leave no debt. The credit case must preserve ₹100 sale value, ₹90 paid, and ₹10 owed. Test direct percentage and rupee discounts, rounding, split payments, and a later repayment. Whole-sale discounts were requested.

### D. Inventory and profit

Run the delivery and weighted-average examples in F02. Test low-stock defaults and overrides, negative-stock warnings, manual adjustments with reasons, damaged stock, shop use/giveaways, and separate pack prices. Test transactions after restarting the app and verify history.

### E. Returns, exchanges, and corrections

Test partial return, full return, cash/digital refund, credit reduction, and exchanges with money collected/refunded. The agreed policy is to return items to sellable stock first, then manually adjust damaged returns. Ensure cost/profit reversals correspond to the original sale. Test cancellation/editing without losing the audit history or double-adjusting stock.

### F. Reports and daily cash

Test opening cash, daily count, manual adjustments, cash versus digital collections, customer repayments, supplier payments, expenses, summary-only sales, and itemized sales. Verify no double counting and consistent filtered exports.

### G. Data resilience and release

Test force-close during a save, double taps, device restart, interrupted import, insufficient storage, backup corruption, and recovery on an isolated installation. Confirm atomic transactions: sale, payment, credit, and stock movement either all save consistently or none commit. Test a signed app update preserving data before sending an update to the brother's phone.

## 6. Implementation phases

1. **Match the source:** Locate the source and build configuration for installed v1.0.0; preserve a safe copy of existing data; reproduce findings.
2. **Data reliability:** Repair/verify backups, costing, negative-stock treatment, zero-price validation, and transaction consistency.
3. **Daily workflows:** Complete discounts/credit choices, account payment details, receipts, and sales/report completeness.
4. **Language and usability:** Complete Urdu, mixed-direction formatting, report controls, settled-account views, and navigation behavior.
5. **Acceptance testing:** Run the scenarios above on isolated data, record results and screenshots, and fix failures.
6. **Delivery:** Produce a signed upgrade, verify installation over the prior version without data loss, and provide backup/recovery and update instructions.

Optional AI remains outside this repair work. The current priority is dependable phone-based inventory, billing, dues, reports, and recovery.

## 7. Completion criteria and developer handoff

- Each issue has a recorded outcome: reproduced and fixed, verified as correct with evidence, or deferred with an explicit reason.
- Backup restoration succeeds in a separate installation with matching records and balances.
- Monetary calculations pass the documented examples and rounding checks.
- No accidental free sale, duplicate stock deduction, or repayment counted as new revenue.
- Urdu works throughout the interface and receipts, including errors and mixed text.
- Receipt defaults persist and can be overridden per receipt.
- Import works with the actual shop format and changing input files.
- Navigation, lock/resume, offline behavior, and signed upgrades pass device checks.
- Record the exact build tested and distinguish tests passed from screens merely inspected.

This document is a review and proposed fix/test plan. It does not mean the fixes have been implemented or that untested workflows have passed.

## 8. Follow-up changes and owner feedback

These additions record the owner's subsequent feedback. The Home gear icon is an explicitly confirmed choice. The other behaviors below are proposed requirements to review section by section; adding them to this document does not mean they are implemented.

### F12 — Settings placement: confirmed Home gear icon

**Decision:** Put a clearly visible Settings gear icon at the top-right of the English Home screen, in the header, accessible without scrolling. Do not add a More tab for this change.

- Use a comfortable touch target and an accessible “Settings” label.
- Keep the header accessible when the Home content is long.
- Open the same Settings page every time; Back should return to Home without losing its state.
- In Urdu, mirror header placement consistently with the app's right-to-left design and verify that the icon remains easy to find.
- Group Settings into Language & Appearance, Security, Receipt Preferences, and Backup & Restore. This grouping is proposed for review.

**Acceptance:** The gear is visible on opening Home, works at larger text sizes, opens Settings with one tap, and behaves correctly in both languages and after restarting.

### F13 — Missing record deletion and correction actions

**Status:** Owner reports that records cannot be deleted in several places. Exact screens and record types still need to be identified during the walkthrough.

**Proposed approach:** Provide a visible actions menu on record details and a consistent route from lists. Use action names that explain what will happen.

| Record | Proposed action | Required effect |
|---|---|---|
| Unused product with no stock/history links | Delete | Confirm the product name before removing it. |
| Product referenced by transactions | Archive | Hide from normal selection while retaining historical bills, stock history, and reports. Allow unarchive. |
| Unsaved or held bill | Discard | Remove the draft without changing stock or balances. |
| Completed bill | Cancel/reverse with a reason | Correct stock, payments, credit, and profit consistently; retain original and reversal history. |
| Recorded customer/supplier payment | Reverse with a reason | Restore the appropriate balance without creating new sales or purchases. |
| Goods receipt or stock adjustment | Correct/reverse through a defined workflow | Reconcile quantities, supplier dues, delivery cost, and costing; account for stock already sold. |
| Expense | Edit or void with history | Update expense totals and any linked payment consistently. |
| Customer or supplier with history | Archive | Preserve linked transactions; unresolved balances must remain visible and actionable. |

Do not implement a universal hard-delete button for every record. Before confirming, explain the concrete stock and money effects. Prevent duplicate reversals and show success or a useful error. Define refunds separately when cancelling an already-paid sale: changing a record must not falsely claim money was returned.

**Acceptance:** Test linked and unlinked records, outstanding dues, partially paid bills, returned items, accidental double taps, cancellation of the confirmation, restart during processing, and restored archives. History and totals must remain consistent.

### F14 — Backup location and file access are unclear

**Status:** Owner asks where backups are saved and how to see them. The inspection established only that the app describes backups as stored on the phone; the actual folder and archive contents have not been verified.

**Proposed Backup & Restore page:**

- Show each backup's file name, creation date/time, actual size, and verified status.
- Show the exact location where available, or a clear explanation when files are inside private app storage and cannot be browsed normally.
- Add **View backups**, **Export/share backup**, and **Restore** actions.
- If supported, offer **Open folder** for user-accessible storage; otherwise offer export to a location the owner chooses.
- Distinguish on-phone backup from an exported or Google Drive copy. Show separate success/failure status for each destination.
- Explain what the backup includes, especially photos and invoice/payment attachments.
- Before restoration, preview the backup date and explain whether existing records will be replaced. Validate compatibility and integrity before changing data.

**Acceptance:** Export a backup to a chosen folder, locate it through the phone's file picker, and recover it in an isolated installation. Verify behavior when storage permission is unavailable, the destination disappears, or the file is corrupt. This extends F01; it does not establish that current backups are valid.

### F15 — Excel import crashes instead of explaining invalid input

**Status:** Owner-reported crash. Not reproduced during the earlier inspection, which opened only the import entry page. Treat as high priority alongside backup and financial reliability work.

**Required behavior:**

1. Accept the intended `.xls`, `.xlsx`, and `.csv` formats and verify their contents, not just the extension.
2. Handle unsupported, unreadable, corrupt, password-protected, empty, or incorrectly structured files without crashing.
3. Explain the specific problem in plain language and offer a template/example of required columns.
4. For multiple worksheets, let the owner choose the relevant sheet or clearly identify the selected one.
5. Preview detected columns, sample products, row counts, quantities, prices/costs, and any warnings before import.
6. Provide column mapping when headings differ. Do not silently guess ambiguous quantity or price fields.
7. Identify invalid rows and fields with row numbers and understandable reasons. Make any skip-invalid-rows choice explicit.
8. Show duplicate SKU/barcode warnings and let the owner decide, consistent with the requested create-new-products import behavior. Do not silently overwrite existing records.
9. Show progress and a completion summary: created, skipped, and failed rows with reasons.
10. Ensure an unsuccessful import cannot leave unexplained partial stock or duplicate products. Use an atomic import or an explicit recoverable staging workflow.
11. Prevent duplicate commits from repeated taps or retrying after an interruption.

**Acceptance files:** Actual changing shop spreadsheet; valid legacy XLS; valid XLSX/CSV; unrelated spreadsheet; renamed non-spreadsheet file; empty sheet; malformed workbook; password-protected workbook; missing headers; invalid amounts; Urdu names; duplicate identifiers; multiple sheets; and approximately 3,000 rows. Check app responsiveness, persisted totals after restart, and unchanged existing records after a rejected import.

**Diagnostic follow-up:** Identify the exact file, chosen sheet, and action that triggers the crash, then capture a crash trace from the matching installed build. Do not label the owner's file as wrong without evidence; a valid file can expose a parser bug.

### F16 — Billing needs a complete owner walkthrough

**Status:** Owner says billing works but contains many mistakes. Specific additional defects have not yet been described or reproduced; do not treat this statement as a completed billing diagnosis.

Review these steps individually with the owner and record expected behavior, observed behavior, and agreed changes:

1. Start a new bill and find products through scan, search, categories, or favorites/recent items.
2. Handle products without barcodes and unknown or duplicate barcodes; no short codes are wanted.
3. Change quantity, select pieces/boxes, remove an item, and correct a price.
4. Apply a whole-bill rupee or percentage discount and show both its amount and effective percentage.
5. Distinguish a discount from unpaid customer credit, including the ₹100/₹90 example.
6. Choose customer and payment methods, including split payments and optional reference.
7. Review total, paid amount, change, remaining credit, and stock warnings before completing.
8. Save once, generate a numbered digital receipt, and share/print using defaults with an override.
9. Hold/resume bills, reopen historical bills, and correct or cancel through a traceable workflow.
10. Process returns and exchanges and confirm effects on stock, customer balance, payments, and profit.

Use isolated sample transactions for verification. Screen appearance alone is insufficient: compare the resulting bill, stock movements, account balances, payment records, and report totals.

## 9. Collaborative review order and revised priorities

Ask one focused question at a time. The first placement question is resolved: **Home Settings gear icon approved**.

Discussion order: navigation/Settings → stock and record actions → Excel import → billing → accounts → reports → backups/recovery → Urdu and final acceptance.

This discussion order is separate from engineering priority. Backup reliability, the reported import crash, financial calculations, and transaction consistency should be addressed before routine usability polish or real shop rollout. The walkthrough may add defects and refine the proposed behaviors; record those decisions here rather than assuming every suggestion has been approved.

## 10. Proposed two-phone operation — clarification in progress

### Owner's requested change

- The main Android phone manages the full shop: stock, prices, purchases, accounts, billing, reports, settings, and backups.
- A second Android phone provides billing access only.
- Both phones must synchronize relevant changes and the shop data must be backed up.
- This expands the previous single-phone scope. It does not reintroduce a desktop requirement.

### Recommended product approach

Use the same app with two device roles: Owner/Main Phone and Billing Phone. Pair the second phone explicitly from the main phone. Avoid maintaining two unrelated apps. The billing device should receive the product, price, barcode, and stock information needed for checkout and return completed bills, payment details, and relevant stock changes. Restrict cost/profit visibility, settings, exports, and unrelated account data according to agreed permissions.

The exact billing permissions remain undecided: customer creation, credit sales, repayments, price overrides, discounts, returns, cancellations, and historical bill visibility must be clarified individually. Do not assume that billing-only means access to all financial records.

### Sync and backup requirements

- Sync is not backup: a deleted or incorrect record can synchronize to both phones. Keep dated, recoverable backups separately.
- Show connection state, last successful sync, pending bills, and actionable errors on both devices.
- Use unique transaction identifiers and duplicate-safe delivery so reconnecting or retrying cannot duplicate a bill, payment, or stock deduction.
- Assign invoice numbers without collisions across devices, including offline operation; agree on device prefixes or another suitable scheme before implementation.
- Synchronize stock movements and transactions rather than overwriting one phone's stock quantity with the other's quantity.
- Preserve historical prices, discounts, and costs used by completed bills. Define product-edit conflicts and owner permissions explicitly.
- A main-phone backup cannot include bills still pending only on the billing phone. Display this limitation and preserve pending transactions locally until synchronization is acknowledged.
- Pair devices securely, protect sync traffic, allow revocation of a lost billing phone, and retain device attribution in history.
- Test disconnection, duplicate retries, interrupted sync, simultaneous sales of the last units, incorrect device clocks, app restarts, and restoration without replaying old bills twice.

### Connection design is not yet selected

Local Wi-Fi/hotspot sync may suit phones operating together in the shop and preserves the earlier preference to keep active data on owned devices. It depends on the phones being reachable and must accommodate Android background and battery restrictions. Remote synchronization while phones are apart requires an additional reachable service or a separately designed connection arrangement; it must not be promised by local-only sync.

Existing preferences remain relevant: no desktop for now, a preference against monthly fees, and Google Drive acceptable for backups. Drive backup must not be assumed to provide reliable live transaction synchronization.

First clarification: Will both phones normally operate together on the same shop Wi-Fi/hotspot, or must they also stay synchronized when apart? Subsequent questions will establish offline billing needs, permissions, conflict handling, and backup responsibility one at a time.
