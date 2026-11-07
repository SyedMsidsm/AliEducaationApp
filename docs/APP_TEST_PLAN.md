# Ali Educational: dedicated testing and acceptance plan

## Current status

User clarification: this file represents the shop's real record-keeping style, which may change. It is not a fixed app template or production seed. The controls below apply only to this supplied snapshot. Test altered layouts and newly computed totals as well as this baseline; do not hardcode the baseline counts or amounts into application logic.

The source spreadsheet has been inspected and its arithmetic reconciled. No Android app exists yet, so application tests below are NOT RUN. Source arithmetic checks are not proof of app behaviour.

## Supplied test data

Source: `C:\Users\Musid\Downloads\Ali Educational 29-Oct-2026.xls`, sheet `Transactions`.

The source is a genuine legacy Excel `.xls` workbook, not `.xlsx`. It was read without modifying it or executing spreadsheet macros. Use it as a read-only fixture; never overwrite the original or import test transactions into live shop data without an explicit migration step.

- Used range: A1:F180. Headers at row 5: Date, Notes, Description / Category, Income, Expense, Balance.
- Row 6: Previous Balance, zero.
- Rows 7–177: 171 dated transactions across 29 distinct dates, 1–29 October 2026. The title refers to the full October month; actual supplied entries stop on 29 October.
- Rows 178–180: summary rows, not transactions.
- Numbers are stored as text with Indian-style separators; parse `4,47,241` as 447241. Dates need explicit parsing, not locale guessing.
- It is an income/expense ledger. It has no product catalog, SKU, item quantity, unit cost or structured customer/supplier balances.

### Verified source controls

| Check | Observed result |
| --- | --- |
| Dated transaction count | 171 |
| Sum of Income, transaction rows only | ₹447,241 |
| Sum of Expense, transaction rows only | ₹501,315 |
| Opening balance | ₹0 |
| Closing balance | −₹54,074 |
| Running balance discrepancies | 0 across all 171 transactions |
| Rows labelled exactly Sales, ignoring case/outer spaces | 28 |
| Income on those Sales rows | ₹247,241 |
| Other income | ₹200,000; must not automatically be classified as sales |

The closing balance is a cash-ledger result, not product gross profit or a customer/supplier debt. Expense notes alone do not establish purchase quantities, who is still owed money, or whether delivery is included. Do not infer those fields.

## Test stages

1. During each implementation phase: run calculation and persistence tests for the feature being added.
2. Phase 7 integration testing: complete linked flows on a clean test database and verify stock/account/sales consistency.
3. Real-file import testing: exercise this workbook or a reviewed conversion of it, with a documented mapping and reconciliation.
4. Android device testing: app lock, offline use, interruption, accessibility, Urdu, performance and storage failure.
5. Brother's acceptance pilot: realistic shop tasks followed by comparison against written expected results.
6. Release gate: fix critical defects, repeat affected tests and restore a backup on another test installation before live use.

## Real-file tests

| ID | Action | Expected result |
| --- | --- | --- |
| DATA-01 | Open the supplied legacy file | Supported reader recognises `.xls`; if conversion is needed, tell the user and verify converted values against the original. Do not rename the extension as a conversion. |
| DATA-02 | Detect headings and summaries | Skip decorative rows 1–4, treat row 5 as headers, row 6 as opening balance and rows 178–180 as controls; exactly 171 transactions. |
| DATA-03 | Parse dates and formatted numeric strings | Correct October dates and decimal amounts; negative balance retained; blanks not silently confused with required zeros. |
| DATA-04 | Reconcile imported values | Income ₹447,241; expense ₹501,315; closing −₹54,074; every running balance matches source. |
| DATA-05 | Classify income | 28 Sales entries sum to ₹247,241. Other ₹200,000 requires review; total income is not reported as total sales. |
| DATA-06 | Try importing this as opening stock | Explain that quantity/product columns are absent. Create no invented products, stock, supplier dues or profit. |
| DATA-07 | Import daily sales as money-only historical records, if approved | Preserve date and amount, mark missing item/cost detail, make no stock deduction and calculate no product profit. |
| DATA-08 | Repeat the same import | Detect prior import or warn before committing. No automatic doubling of transaction counts or totals. |
| DATA-09 | Review repeated notes/dates | Preserve legitimately repeated transactions; do not deduplicate solely by date, description or amount. |
| DATA-10 | Cancel or interrupt import | No partial live import; safely resume/retry or roll back with a clear result. |
| DATA-11 | Export and reopen imported test data | Same accepted records, amounts, dates and totals; no summary rows double-counted. |

Historical cash-ledger/expense import is not silently added to the initial product-import scope. The file is authorised as test data. Decide separately whether the brother wants old money-only sales imported into the live app. Keep unrelated expense categories unchanged until their meaning is confirmed.

## Functional and financial tests

| ID | Scenario | Expected result |
| --- | --- | --- |
| CORE-01 | Receive 30 pieces, sell 2 packs of 10 | 10 sellable pieces remain; no second deduction for recording payment. |
| CORE-02 | Move 3 of 10 pieces to damaged | 7 sellable, 3 damaged; no fake sale. |
| CORE-03 | Sell more than available | Warning, allowed confirmation, visible shortage and unknown-cost warning where applicable. |
| CORE-04 | Receive ₹500 goods + ₹50 delivery | Stock cost ₹550 exactly once. Payee and outstanding balance reflect who is owed delivery. |
| CORE-05 | Apply agreed proportional allocation to ₹200 pens/₹300 notebooks | ₹20/₹30 delivery; ₹11 cost for each of 20 pens and ₹33 for each of 10 notebooks. |
| CORE-06 | Sell 10 of those pens at ₹15 with 10% discount | ₹150 original, ₹15 discount, ₹135 net sales, ₹110 cost, ₹25 gross profit. |
| CORE-07 | ₹100 due, ₹90 received: choose discount | 10% / ₹10 discount, ₹90 final bill and no remaining debt. |
| CORE-08 | Same payment: choose credit | No discount, ₹100 sale, ₹90 paid, selected customer owes ₹10. |
| CORE-09 | Customer owes ₹500 and repays ₹200 | ₹300 remaining; no new sale or repeated profit. |
| CORE-10 | Supplier bill ₹550, payment ₹200 | ₹350 payable if all charges belong to supplier; stock receipt is not duplicated. |
| CORE-11 | Individual sales plus additional daily summary | Each sale counted once. Attaching amounts to a prior stock-out does not deduct stock again. |
| CORE-12 | Missing cost or selling amount | Clear incomplete-profit status, not a made-up zero-cost profit. |
| CORE-13 | Change product price after a sale | Historical recorded sale/profit remains unchanged. |
| CORE-14 | Correct/reverse a saved sale or payment | Linked inventory and balances reconcile; original and correction remain explainable. |
| CORE-15 | Double tap Save or terminate during save | Exactly one complete transaction or no committed transaction; never half-applied updates. |

Add boundary cases: 0/100% discounts, decimals and paise rounding, empty fields, malformed quantities, different pack sizes, zero-value delivery-allocation bases, and mixed purchases under the confirmed weighted-average costing policy.

## Android, language and recovery tests

- Install the signed release APK on the brother's phone from the chosen private distribution link. Verify device compatibility and applicable installation/verification requirements.
- Update an installed earlier release with real-looking local data using the same app identity/signing lineage. Preserve stock, account balances, receipts and attachments through migrations, without uninstalling.
- Test failed/interrupted upgrades and recovery from a pre-update backup. Release keys must not be packaged with the APK.
- Test reordered/renamed columns, new categories, different dates/amounts and overlapping files using disposable fixture copies. Mapping preview and new reconciliation controls must adapt; no assumptions tied to the original row count or totals.

- Fingerprint success/failure, device-credential fallback, absent biometric enrolment and lock timeout. No account login introduced.
- App force-stop/restart, airplane mode, interrupted attachment save, low/free storage and repeated launches.
- English and Urdu layout, mixed codes/numbers, large font settings, long product names and legible receipts.
- Search/import at approximately 3,000 products and a representative long transaction history. Synthetic records must be clearly labelled and kept separate from the supplied ledger.
- Receipt default and per-receipt overrides; cancelled WhatsApp share must not cancel or duplicate the sale.
- Manual and scheduled backup, no network, rejected Drive access, missed schedule and honest last-success status.
- Backup/restore must preserve records, totals, attachments and settings. Corrupt/incompatible backups must leave current data intact.
- Restore a dated backup into a clean test installation and independently reconcile stock, debts and sales against pre-backup controls.

## Brother's acceptance pilot

Use an isolated test database. Ask the brother to add stock, find a product, record a sale, choose discount versus credit, receive a repayment and recover a backup without coaching. Record confusion, wrong taps and failed tasks; revise the interface and retest. Confirm no test data survives into live opening balances.

## Release criteria and evidence

- All applicable critical data-integrity, calculation, unlock and restore cases pass on the target Android phone.
- Source-import totals match exactly to paise, with all 171 rows accounted for whenever cash-ledger import is included.
- No unresolved defect that loses data, silently duplicates money/stock, misstates debt/profit or bypasses app lock.
- Any unavailable feature or deferred test is explicitly documented; it is not marked Passed.
- Maintain a results log with test ID, app build, device, fixture, expected/actual result, status, evidence and defect reference.
- Brother accepts the core workflows and a restore has been demonstrated before launch.

Current execution status: source parsing and arithmetic checks completed; all app/import/UI/restore cases remain NOT RUN until implementation exists.


### Confirmed costing acceptance cases

| ID | Scenario | Expected result |
| --- | --- | --- |
| COST-01 | Allocate ₹50 delivery across goods values ₹200 and ₹300 | Shares ₹20/₹30; total cost ₹550; no second delivery deduction. |
| COST-02 | 10 pieces cost ₹100; receive 10 with goods ₹140 + delivery ₹20 | 20 pieces; stock value ₹260; moving average ₹13. |
| COST-03 | Sell 5 from COST-02 for ₹100 less 10% discount | Net sale ₹90; cost ₹65; gross profit ₹25; 15 pieces costing ₹195 remain. |
| COST-04 | Receive additional stock at a different cost after COST-03 | Remaining average updates; saved ₹25 historical sale profit does not change. |
| COST-05 | Several products, packs, fractional delivery shares | Calculate each variant separately in base units; shares reconcile exactly to delivery total. |
| COST-06 | Mixed purchase batches | Actual profit uses weighted-average sale cost; purchase screen does not claim batch-specific realised profit. |
| COST-07 | Negative quantity, unknown opening cost, or zero purchase-value allocation base | Explicit exception/review; no divide-by-zero or invented reliable profit. |

These application tests are NOT RUN until implementation. Weighted-average policy and proportional delivery allocation are confirmed; edge-case handling remains subject to implementation review.
