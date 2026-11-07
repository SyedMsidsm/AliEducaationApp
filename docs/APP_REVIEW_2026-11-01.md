# Ali Educational — Updated App Review

Review date: 1 November 2026 (Asia/Calcutta)

## Recommendation

Fix the inventory-edit, pack billing, payment, reversal, import, and recovery issues below before adding two-phone synchronization and service reporting. Otherwise those additions would distribute incorrect records or build new reports on unreliable data.

This review did not change production source or commit a sale, purchase, payment, stock edit, import, refund, or restore on the phone. Automated tests ran in a separate project copy. The new specifications remain plans, not implemented features certified by this review.

## 1. Build identity and scope

- Connected device: Samsung SM-S928B.
- Installed package: `com.alieducational.app`, version `1.0.0`, version code `1`.
- Installed update timestamp: 31 October 2026, 23:21:32, as reported by Android.
- Matching project found at `C:/Users/Musid/.gemini/antigravity/scratch/AliEducational`.
- The project's release APK and the phone's installed base APK have the same SHA-256: `f259e092ce1edc44df3b49baaaf3f29c5e4dd099d3d3c66d01e4a06596f2888f`.
- The older `android` project inside the document workspace is not the app being reviewed. Changes must target the matching Antigravity project.
- Reviewed device Home, Settings, Stock, Backup & Restore, and import entry screens; reviewed source for billing, scanner lookup, payments, receipts, stock receiving, adjustments, accounts, reports, expenses, import/export, database migrations, lock behavior, and backup/restore.
- Later device checks stopped at the fingerprint/PIN prompt. An unlock request was sent to the owner. No attempt was made to bypass it. The invalid-file picker test and additional transaction walkthroughs remain unverified.

**Evidence labels:** Device = observed on the phone; Source = confirmed implementation path in the discovered project, not necessarily exercised with a saved transaction; Test = executed automated check; Risk = a failure path requiring a controlled reproduction.

Source locations below are relative to `app/src/main/java/com/alieducational/app/` in the matching project unless otherwise stated.

## 2. Improvements confirmed since the earlier review

1. Home now has the requested Settings gear and prominent New bill, Add stock, and Record payment shortcuts.
2. Backup & Restore shows the actual folder and export/share controls. A recent archive displays 6 KB rather than 0 KB.
3. An existing backup was downloaded read-only and inspected: ZIP CRC passed and SQLite `PRAGMA integrity_check` returned `ok`.
4. Import now has source-level exception handling, error messages, sheet selection, column mapping, preview, and existing-item skip/update choices. This is progress, but does not establish safe import behavior for every file.
5. Account payment forms now contain payment-method choices and generate a payment voucher.
6. Profit reporting now has Today, This Week, This Month, and All Time filters. Formula/completeness issues remain.
7. Sale voiding and expense voiding exist in source, and ordinary queries exclude voided entries. Reversal correctness and accessible history still need work.
8. Sale and purchasing repository operations generally use database transactions. Bill posting also has an in-memory repeat-tap guard.
9. Eight existing costing tests pass, including allocation/weighted-average scenarios. This is not coverage of the full checkout/persistence flow.

## 3. Critical and high-priority findings

### R01 — Editing a product can add its existing stock again

**Evidence: Source. Priority: critical inventory correctness.**

`ui/stock/StockScreen.kt:454` initializes the edit form's quantity with the product's current stock. At lines 500–502 this becomes the incoming quantity. `data/repository/AppRepository.kt:226` then adds it to existing stock.

Example: stock is 7. Open Edit, change only the name or price, and save with the prefilled quantity. The code path produces 7 + 7 = 14 and records an incoming movement. Adding a supplier in that edit can also create a purchase liability.

**Fix:** Separate Edit product, Receive additional stock, and Set counted stock into explicit operations. Product editing must not change quantity. Incoming stock starts at zero. A count correction computes the difference and records its reason.

**Required regression:** Start at 7; rename and reprice; quantity and supplier balance remain unchanged. Receive 3 → 10. Set counted stock to 8 → one adjustment of −2. Repeat taps must not duplicate stock.

### R02 — Pack prices can be multiplied by pack size twice

**Evidence: Source. Priority: critical billing correctness.**

`ui/AliViewModel.kt:384` selects the pack's selling price as the draft unit price. At lines 653–655 posting sends the number of base pieces together with that pack price. `data/repository/AppRepository.kt:570` multiplies base-piece quantity by the supplied price.

Example: one 10-piece pack priced at ₹100 displays ₹100 in the draft but the save path computes 10 × ₹100 = ₹1,000. The amount received is calculated from the draft's ₹100, so this can also create an unintended remaining balance. Receipt rendering labels base quantity as packs in places.

**Fix:** Persist selected-unit quantity, selected-unit price, pack-size snapshot, base-stock quantity, and exact line amount distinctly. Do not solve it by dividing pack price into rounded per-piece prices and losing the remainder.

**Required regression:** One pack of 10 for ₹100 → sale ₹100 and stock −10. Two packs for ₹199 each → ₹398 and stock −20. Verify draft, saved receipt, credit, discount allocation, and profit agree.

### R03 — Repeated product lines reuse the first allocation

**Evidence: Source. Priority: high.**

Sale and purchase allocation inputs use product ID as line ID. `AppRepository.kt:601` retrieves the first matching allocation for each sale line; purchase paths do the same at lines 430 and 444. Billing can hold the same product as both piece and pack lines.

Different lines for the same product can therefore reuse the first line's net amount/discount. Purchase delivery allocation and cost updates have the same ambiguity if the product appears more than once.

**Fix:** Give each line its own stable ID and match allocations by that ID, or explicitly consolidate equivalent lines before calculation. Keep piece and pack prices distinct.

**Required regression:** Same product on two differently priced/unit lines, with a whole-sale discount. Sum of line amounts must equal the sale. Repeat with purchase lines at different costs and delivery charges.

### R04 — Partial cash can leave credit without a customer

**Evidence: Source. Priority: high.**

The customer-required guard at `ui/AliViewModel.kt:641` applies only to CREDIT/OTHER. Cash checkout permits an amount below the bill total. The repository calculates remaining credit but updates a customer balance only if a customer ID exists.

Example: walk-in bill ₹100, cash entered ₹90, Save without choosing discount or customer. ₹10 can remain on the sale without being attributed to a customer account.

**Fix:** For every payment mode, require an explicit decision for any shortfall: discount, identified customer credit, or additional payment. Enforce this in the repository as well as the UI. Separate tendered cash from applied payment and change.

**Required regression:** ₹100/₹90 cannot complete as an unidentified debt. Discount gives ₹90 net and no debt; customer-credit choice gives ₹100 net, ₹90 paid, ₹10 linked debt.

### R05 — Exact cash button parses its amount as zero

**Evidence: Source and executed failing regression test. Priority: high.**

The Exact button in `ui/billing/PaymentScreen.kt` passes formatted currency such as `₹80.25` to `setCashReceived`. `BillingState.cashReceivedPaise` uses `toDoubleOrNull`, which rejects the currency symbol and returns zero.

The focused test expected 8,025 paise and received 0. A plain `80.25` control test passed. Posting sometimes masks this through its zero-means-exact fallback, but the internal state and displayed amount are inconsistent.

Posting also clamps tendered cash to the net total before storing it, so saved `changeGivenPaise` becomes zero even when the payment screen showed change.

**Fix:** Keep an exact numeric money representation internally. Populate the input with an editable numeric string, format only for display, validate parsing errors, and persist tendered/applied/change amounts separately.

**Required regression:** Exact ₹80.25 saves precisely ₹80.25. Tender ₹100 for ₹80.25 preserves ₹19.75 change on the saved receipt. Invalid input must not silently mean paid in full.

### R06 — Adding a shortfall discount can replace an existing percentage discount incorrectly

**Evidence: Source. Priority: high.**

PaymentScreen's Treat as discount action uses `discountValuePaise + shortPaise`. For a percentage discount, `discountValuePaise` is not the computed `actualDiscountPaise`.

Example: ₹100 gross with 10% discount gives ₹90 payable. Customer pays ₹80 and the remaining ₹10 is treated as a further discount. The intended total discount is ₹20, but the code can replace it with a fixed ₹10 and leave ₹90 payable.

**Fix:** Base the operation on the actual current discount, or calculate the desired final discount from gross minus the agreed final payable. Preserve the distinction from credit.

**Required regression:** Percentage and fixed discounts followed by shortfall-as-discount, including rounding and a 100% intentional discount.

### R07 — Damaged sale returns deduct sellable stock a second time

**Evidence: Source. Priority: high.**

`AppRepository.kt:759` calls `moveToDamagedStock` for a returned damaged sale. That DAO operation subtracts from sellable stock and adds to damaged stock. The sold item was already deducted when sold.

Example: start with 10, sell 2 → 8 sellable. Return those 2 damaged should leave 8 sellable and 2 damaged. The current path produces 6 sellable and 2 damaged.

**Fix:** Use an inbound damaged-return operation that only increases damaged stock. Keep transfer-from-current-sellable for damage to items still in the shop. Confirm the desired return workflow: the earlier agreement was return to sellable then manually adjust damage.

**Required regression:** Sellable return, damaged return, no stock restoration, and repeated void attempts; ledger changes must reconcile exactly.

### R08 — Voiding a credit sale ignores later repayments

**Evidence: Source. Priority: high.**

`recordAccountPayment` adjusts account balance without allocating repayment to a sale. `voidSale` refunds the original sale's received amount and subtracts its original remaining credit (`AppRepository.kt:779–796`). It does not account for repayments received afterward.

Example: ₹100 credit sale, then ₹100 repayment, then cancellation. The current approach can subtract the original ₹100 debt again and record no cash refund because the original sale received zero.

**Fix:** Define how repayments are allocated, and compute cancellation from current settlement state. Offer an explicit refund or customer-credit outcome. Do not claim money was returned merely because a record was voided.

**Required regression:** Unpaid, partly paid, fully repaid, overpaid/advance-policy, and multiple-bill customer scenarios. Confirm actual refund, remaining balance, history, and report period.

### R09 — Database configuration permits destructive fallback

**Evidence: Source risk. Priority: high before any schema expansion.**

`di/DatabaseModule.kt:37` calls `fallbackToDestructiveMigration()`. Migrations 1→2→3→4 are present, but a future missing migration path can recreate the database instead of preserving shop records.

**Fix:** Remove destructive fallback for production data. Supply and test every supported migration before introducing service, expense, role, or sync tables. Fail safely rather than erase data.

**Required regression:** Upgrade populated fixtures for all supported versions; compare record IDs, counts, balances, stock, bills, held drafts, and receipts. Test an unsupported migration path without clearing data.

### R10 — Backup archive is readable, but recovery is not sufficiently validated

**Evidence: Device, archive/database checks, and source risks. Priority: high.**

Observed backup path: `/storage/emulated/0/Documents/AliEducational/Backups`.

Archive tested: `ali_backup_20261031_233324.zip`, 6,453 bytes. It contained a 135,168-byte database and metadata. ZIP CRC passed; SQLite integrity check returned `ok`. This is a genuine improvement over the earlier unexplained 0 KB display.

However:

- `BackupRestoreService.kt` validates mainly the presence of `ali_educational.db`, not SQLite integrity, compatible schema, or expected tables.
- Metadata says `schema=1` while the extracted database reports schema version 4.
- Creation checkpoints WAL then copies the live database without a coordinated immutable snapshot or blocking later writes; concurrent updates can compromise consistency.
- Restore overwrites the live database after closing the singleton database, with no complete staged validation or atomic replacement/rollback strategy.
- The safety copy copies the main file without ensuring pending WAL content is included.
- Restore callers do not catch errors. No controlled database reinitialization/restart is shown after closing the singleton.
- Backup contains the database and metadata only; DataStore preferences are not included. Do not describe it as a complete device-settings backup.

**Fix:** Create a consistent snapshot, validate archive/database/schema, accurately describe coverage, stage and validate restoration before replacement, protect current data on failure, and reinitialize/restart cleanly. Never test destructive restoration over active shop data.

**Required regression:** Isolated restore of known records; corrupt ZIP; ZIP containing a fake/empty database; unsupported schema; interrupted write/restore; low storage; active write during backup; settings recovery policy. The successful integrity check here is not a successful end-to-end restore test.

### R11 — Product import can partially commit and silently substitute zero values

**Evidence: Source. Priority: high.**

`ImportExportService.kt:250` defaults invalid quantities to zero. The currency parser at line 450 defaults invalid prices to zero. It removes `Rs` but leaves the dot in `Rs. 25.50`, which then fails parsing. The existing currency test copies different parsing logic that removes `Rs.` and therefore does not catch this production discrepancy.

Product changes and stock movements are separate DAO calls, not one transaction. A product can be saved even when its movement fails. Earlier rows remain committed after later failures. The final result sets `isSuccess=true` despite row errors. Import Update adds incoming stock, so repeated import can add it again.

**Fix:** Validate mapped rows before committing; reject/flag invalid amounts rather than silently zeroing them; provide explicit partial-import policy; use atomic transactions or recoverable staged batches; identify repeat imports; show actual failure reasons and row errors. Test production parser functions, not copies of them.

**Required regression:** Invalid quantity, negative quantity, `Rs. 25.50`, formatted/decimal numbers, duplicate imports, mid-row failure, process restart, and mixed valid/invalid rows. Check both stock and movement history.

### R12 — Supplied spreadsheet is a cash ledger, not a product catalog

**Evidence: Executed workbook read plus source review. Priority: high import usability.**

`Ali Educational 29-Oct-2026.xls` opens with the app's POI library. It has a Transactions sheet with 180 row positions. The first rows contain title/date information; the column headings are on row 5: Date, Notes, Description / Category, Income, Expense, Balance.

The catalog importer assumes the first nonblank row is the header and allows importing with only a mapped name column. It does not establish that the file represents stock products. A title or cash record can therefore be treated as product data through an incorrect mapping.

**Fix:** Detect known cash-ledger structure, explain that it belongs in Cash In/Out import, and reject it as a product catalog unless the owner deliberately supplies a valid product sheet. Add header-row selection, required-field validation, and a meaningful preview. Do not label this valid workbook corrupt.

The device invalid-file preview check was prepared but not completed because the app required fingerprint/PIN unlock. Passing a desktop POI read does not prove on-device import is safe, nor does it import any rows.

### R13 — Profit cards treat incomplete costs as zero and summaries as fully cost-free revenue

**Evidence: Source. Priority: high.**

`ui/reports/ProfitScreen.kt` sums every non-voided sale's net value, but COGS comes only from itemized lines; null costs become zero. Daily summaries can overlap itemized sales, and the Full Day versus Additional distinction is stored largely in notes. The page still displays gross/net profit amounts.

Expected inventory profit also multiplies negative stock by the selling-cost difference, so negative stock can generate a misleading positive estimate. The stock card shows a zero-cost item as 100% margin without distinguishing missing cost from a genuinely free acquisition.

**Fix:** Exclude/flag unknown cost contributions, reconcile overlapping summaries, label incomplete profit clearly, exclude negative quantities from sellable inventory potential, and distinguish revenue from collections. Preserve expense separation. Do not extend these formulas to service revenue as if service cost were zero.

**Required regression:** Itemized-only, summary-only, overlapping summary/itemized, missing cost, intentional zero cost, negative stock, credit sale/repayment, and later reversal across date ranges.

## 4. Additional fixes and missing behavior

### R14 — Resuming a held bill deletes its durable draft immediately

**Evidence: Source.** `ui/AliViewModel.kt:605–617` loads into in-memory state and deletes the stored held bill before checkout completes. A process loss afterward can lose the bill.

Keep the durable draft while active, autosave updates, and remove it only after successful completion or explicit discard. Test resume → process death → reopen, including an existing active cart.

### R15 — Receipt printing is a placeholder; receipt options are not fully connected

**Evidence: Source and device Settings.** `ui/billing/BillReceiptScreen.kt:489` only displays “Receipt sent to printer spool”; no printing API is called. Share uses plain text. Settings has descriptive receipt text but no working default choice. `receiptAction` is recorded in state but not used to execute the selected action in the inspected completion path.

Implement actual Android print/PDF/share flows with truthful result handling and persistent defaults. Use saved bill data, not only `lastSavedSale` in memory. Reopening a historical receipt after process restart must fetch the correct record by ID. Share must include paid/due information, accurate unit quantities, original bill date, and discount details.

The separate PDF generator uses one fixed-height page without wrapping/pagination and does not apply its `isUrdu` argument. Test long names and 20+ lines; verify Urdu output instead of assuming the text receipt's Urdu branch covers PDF.

### R16 — Urdu selection is absent and layout is forced left-to-right

**Evidence: Device and Source.** The inspected Settings screen has no language selector. `MainActivity.kt:51` forces `LayoutDirection.Ltr` despite collecting language preferences. Many screens remain hardcoded English.

Restore language selection and real localized resources, including dialogs, validation and receipts. Test Urdu shaping, currency, mixed identifiers and persistence. This is not fixed merely by removing the old RTL toggle.

### R17 — Lock timeout setting is not honored; cold-start protection needs verification

**Evidence: Source risk; a real fingerprint prompt was observed after returning to the app.** Settings offers several timeouts, but `MainActivity.kt:108` uses a hardcoded 120 seconds. It starts `isAppLocked=false`, so cold-start protection needs a specific regression. Changing a navigation start destination alone should not be relied upon as the complete security gate.

Use the selected timeout and a top-level locked state that prevents access to protected UI on cold start/resume/deep link. Test all timeout choices, cancelled authentication and process restart. Do not infer that authentication is entirely broken: it did prompt during this review.

### R18 — Random receipt numbers can collide

**Evidence: Source.** `AppRepository.kt:567` uses a random four-digit suffix each day, with no shown uniqueness retry/constraint for receipt number. Internal UUIDs do not make the displayed receipt number unique.

Use a durable sequential or otherwise collision-safe receipt series now, and a defined device series for future offline billing. Test concurrent completions, restart and restoration without duplicate visible receipt numbers.

### R19 — Pack configuration and saved pack price can be lost in editing

**Evidence: Source.** The integrated stock form takes existing pack size or defaults to 1; it no longer offers the full box/piece configuration in the inspected path. Repository updates always reset pack selling price to piece price × pack size, overwriting any independently set pack price.

Preserve existing pack configuration and provide explicit independent piece/pack prices, as agreed. Test editing unrelated fields without altering pack price, then piece-and-pack checkout together.

### R20 — Barcode linking can become inconsistent

**Evidence: Source risk.** Lookup prioritizes `product_barcodes`; editing a product's primary barcode does not fully reconcile old mappings. Linking has no visible repository ownership check before insert. Mapped lookup fetches a product by ID without rejecting archived products. The mapping's pack quantity is returned but ignored by `handleScannedBarcode`, which uses the product pack size instead.

Enforce barcode uniqueness/ownership, explicitly preserve or remove old aliases, reject archived items during sale lookup, and define authoritative pack quantity. Test relinking, archived products, multiple mappings and differing mapped pack sizes.

### R21 — Record actions and account ledger handling are incomplete

**Evidence: Source review.** Product archive exists in the DAO but is not wired through the inspected stock UI. Account archive functions do not guard outstanding balances; archived accounts are excluded from displayed due totals. Payment entry only checks a positive amount in UI; the repository has no overpayment/ownership validation or persistent repeat-request ID. Later payments change running balances without updating individual purchase/sale due records.

Provide visible archive/correct/reverse routes, retain debt visibility for archived accounts, define overpayment as an explicit advance, and reconcile allocations/history. Do not silently hide a balance or use hard deletion to fix history. Test partially paid invoices, double taps, advances, voided payments and archives.

### R22 — Existing cash tracker is a manual ledger, not automatically the register balance

**Evidence: Source.** CashTrackerScreen reads the shop-expense/income ledger. It does not derive cash from bills and account collections. Users entering daily sales totals manually can confuse this with the billing revenue report or double-record activity conceptually.

Clearly label its scope and distinguish manual cash records, sales revenue, collections, and expected register cash. Respect the owner's chosen manual treatment of supplier/expense payouts. Define one source per figure before adding Services expenses so the same entry is not duplicated.

### R23 — Single-product stock receiving differs from the full purchase workflow

**Evidence: Source.** The integrated add/edit operation truncates transport cost per piece before calculating incoming total cost; its purchase record has no associated PurchaseLine entries in that path. It also assumes transport is owed to the supplier. The separate receiveGoods operation supports proportional allocation and a delivery payee.

Use one purchase/costing workflow, retain full allocated delivery totals and purchase lines, and make the payee explicit. Test a ₹1 delivery charge over three units, multi-line freight allocation, carrier-paid delivery, and partial supplier payments.

### R24 — Update versioning and build portability need attention

**Evidence: Device and build configuration.** Multiple updates still identify as version 1.0.0/code 1. Increment versions and include a build identifier so a test report identifies the actual release. Preserve signing identity and application ID for upgrades.

The isolated build first failed under Java 25, then failed under a project path containing spaces due to Room/KSP schema arguments. It passed with Java 21 in a temporary path without spaces. Document a supported build environment; this is a development setup issue, not evidence of an on-phone crash.

## 5. Test results and artifacts

| Check | Outcome | Limit |
|---|---|---|
| Installed APK versus project release APK SHA-256 | Exact match | Confirms APK provenance, not every source modification timestamp. |
| Existing unit suite | 13/13 passed | Eight calculator, four import robustness, one cash Excel test; limited integration coverage. |
| Numeric cash input regression | Passed | `80.25` becomes 8,025 paise. |
| Exact-button formatted amount regression | Failed | Expected 8,025 paise, actual 0. |
| Supplied `.xls` opened by same POI dependency | Passed | Cash ledger identified; no product import committed. |
| Existing backup ZIP CRC | Passed | One archive only. |
| Extracted backup SQLite integrity | `ok`, schema 4 | No live restore or concurrent-write stress test. |
| Backup metadata consistency | Failed | Claims schema 1 while DB is schema 4. |
| Further on-device picker/transaction tests | Incomplete | Fingerprint/PIN prompt requires owner unlock. |

Artifacts are in `output/audit-2026-11-01/`: baseline test XML, focused regression source/results, and supplied-workbook read test source/results. Tests were added only to the isolated temporary review project, not production source.

The four existing import tests largely exercise copied parser snippets or POI directly. A green suite does not prove actual ImportExportService mapping, database atomicity, Android file access, or import UI error handling.

## 6. Required fix and verification sequence

1. Protect data: verified backup/recovery and non-destructive migrations.
2. Correct product editing and pack/base-unit accounting, then unique line allocation.
3. Correct short payments, discount composition, tender/change and repayment-aware reversals.
4. Fix import validation/staging and distinguish cash-ledger files from catalog files.
5. Correct profit completeness and manual cash-ledger labeling.
6. Finish real receipt printing/defaults/history, Urdu, lock settings and record actions.
7. Run isolated end-to-end tests for all of the above, including interruption and restart.
8. Only then add service lines/expenses and two-phone roles/sync in phases, keeping single-phone behavior working throughout.

### Before calling the app ready

- Edit product metadata without changing stock.
- Piece/pack totals agree across draft, receipt, stock, profit and credit.
- Every discount/credit shortfall is explicit and every payment is attributed.
- Cancellation after repayment and damaged returns reconcile correctly.
- Valid and invalid imports preserve coherent stock and history.
- Backup restores successfully in a separate installation and incompatible restores leave existing data intact.
- Printing actually opens/uses printing; long and Urdu receipts render correctly.
- App lock obeys configured timeout and protects cold start.
- Existing records survive a signed upgrade with preserved identifiers and balances.
- Any remaining untested scenarios are listed honestly rather than marked passed.

This report is a detailed source/device review with the executed tests above. It is not a claim that every workflow has been exercised on the live phone. Production changes should follow a separate implementation step after reviewing these findings.

## 7. Follow-up device verification after owner unlock

This section supersedes the earlier fingerprint-blocked status for the following checks. No sale, product edit, payment, import, or reversal was committed. Temporary bill items and discount were removed; the app was restarted afterward to clear temporary payment state. Disposable files placed on the phone were removed.

| Check | Observed result | Finding update |
|---|---|---|
| Deliberately invalid `.xlsx` selected through Android file picker | App remained open and displayed Cannot Read Spreadsheet, unsupported file type UNKNOWN, and guidance | This particular invalid-file crash scenario is fixed. Does not certify row validation or atomic import. |
| One ₹60 notebook in an unsaved bill, cash entered ₹50, walk-in customer | Short by ₹10 shown, but Save bill remains enabled before choosing discount or linked customer credit | R04 now has device evidence for the unsafe Save state; save was not pressed. |
| Exact button | Cash field becomes the formatted string `₹60.00` | Corroborates R05's executed parser regression. |
| ₹60 bill with 10% discount, then cash ₹50 | Initial payable correctly ₹54; Treat difference as discount changes payable to ₹56, not ₹50 | R06 reproduced directly on device. |
| Edit existing notebook with stock 7 | Pcs field prefilled with 7; cancelled without saving | Confirms R01's UI input; stock-add behavior established by source, not a live saved edit. |
| Customer and supplier lists | Settled customer ₹0 remains under Customers Owe; supplier ₹0 remains under We Owe; counts include them | Add outstanding-only filters/counts with All accounts access; do not hide history. |
| Customer repayment dialog | Cash, UPI, Bank and note/reference fields present | Earlier missing-payment-method issue improved. Dialog cancelled blank. |
| Financial report All Time → Today | Sales/expense totals changed; Today showed zero sales and expenses | Basic filtering works. |
| Incomplete-sale warning on Today | Still displayed 1 Sales Need Stock Details despite zero sales in Today | Warning uses an unfiltered historical count; scope it to the selected dates or explicitly label it all-time. |
| Cold restart after draft cleanup | Home and financial dashboard opened without requesting fingerprint/PIN | Strengthens R17: resume authentication was observed earlier, but cold-start path does not consistently require it. Confirm current lock preference and test all startup paths before release. |

### R25 — Payment controls wrap into unreadable fragments

**Evidence: Device screenshot.** Quick amount controls render Exact as several fragments and split +10, +50, +100, and ₹500 across lines. Customer Credit / Khata also occupies multiple lines in an uneven chip row. This makes the most frequent checkout actions harder to recognize.

**Fix:** Use a wrapping flow/grid or horizontal scroll with sufficient minimum widths and sensible padding. Keep amount labels on one line without reducing accessibility font size. Use a shorter Credit label with supporting explanation elsewhere. Test the actual device's current font/display settings as well as larger text.

Screenshots: `output/audit-2026-11-01/payment-shortfall.png` and `output/audit-2026-11-01/discount-replacement.png`.

### Remaining verification limits

Saved transaction, refund, import-commit, destructive recovery, actual physical printing, real barcode recognition, and two-device synchronization were not performed against active shop data. Test these in an isolated installation after fixes. The source findings remain actionable without deliberately corrupting live records to reproduce them.
