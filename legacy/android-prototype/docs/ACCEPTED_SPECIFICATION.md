> SUPERSEDED COPY: use ../../ALI_EDUCATIONAL_FINAL_APP_PLAN.md as the source of truth.
> This earlier snapshot contains outdated decisions. Retained for provenance only.

# Ali Educational: complete Android app specification

Final consolidated specification | 29 October 2026

Status: agreed product direction with explicitly listed implementation decisions still open. The application is not yet built. Stock is the primary focus; daily money records support it.

This document supersedes previous plans wherever they conflict. The latest decisions are: one user (the owner's brother), one Android phone, local data storage, no app account or registration, and Android device authentication. Computer support, multi-device sync, helper accounts and permissions are removed from the initial scope.

## 1. What we are building

A simple app for one stationery shop in Karnataka with approximately 3,000 products and variants. It should answer:

1. What stock do we have, and what is running low?
2. Whom do we owe money?
3. Who owes us money?
4. What did we sell today, what discounts did we give, and what gross profit can we calculate?

Inventory and balances are the priority. Recording sales supports stock accuracy, discounts and profit without a complicated point-of-sale workflow. All core tasks work without internet. AI is optional and deferred.

## 2. Decisions already agreed

| Topic | Decision |
| --- | --- |
| Shop identity | Ali Educational; supplied Ali logo and black-and-gold branding |
| Platform | Android phone first; only the brother uses it |
| Storage | Save working data directly on the phone |
| Accounts | No sign-up, app account, email login or helper accounts |
| Unlock | Fingerprint or supported biometric, with Android device PIN/pattern/password fallback |
| Languages | English and Urdu script; right-to-left Urdu layouts |
| Appearance | Dark default, optional light theme; simple touch-friendly screens |
| Main navigation | Home, Stock, Accounts, More |
| Customer/supplier credit | Running balances and partial payments |
| Discounts | Whole-sale percentage or automatic calculation from the agreed final amount |
| Delivery | Included once in stock cost, separately visible |
| Profit | Expected gross profit and actual gross profit on complete sales records |
| Receipts | Optional; configurable default action and language; override per receipt |
| Initial catalog | Manual entry and spreadsheet import |
| Backup | Restore capability; optional Google Drive backup, separate from app access |
| GST setup | Use the latest supplied status: not GST registered; ordinary receipts |

No fixed launch deadline. Avoid a mandatory monthly subscription or hosted server.

## 3. Phase overview

The phases below are implementation order, not separate promises of unfinished releases. Real shop use should begin only after the backup, data integrity and acceptance checks in Phases 6 and 7 pass.

| Phase | Deliverable | Completion criterion |
| --- | --- | --- |
| 0 | Clickable phone prototype and workflow review | Brother can understand the main tasks without explanation |
| 1 | Android foundation, local database, app lock and settings | Records survive restarts; protected access works offline |
| 2 | Products, opening stock, import and stock movements | Stock stays correct through receiving, packs and adjustments |
| 3 | Suppliers, purchases, delivery and customer accounts | Credit and partial payments produce correct balances |
| 4 | Daily sales, discounts and gross profit | Sale, stock, payment and profit figures agree |
| 5 | Receipts, reports, graphs and bilingual polish | Outputs are readable, accurate and easy to share |
| 6 | Backups, restore and recovery implementation | Backup and restore features are implemented |
| 7 | Dedicated testing, real-data validation and brother's acceptance pilot | Critical tests pass, totals reconcile and restore is demonstrated |
| 8 | Optional future features | Added only after the core is stable and requested |

## 4. Phase 0: simple phone experience

### Navigation

- **Home:** compact stock/balance overview and everyday actions.
- **Stock:** catalog, search, quantities, low-stock list and movement history.
- **Accounts:** two clear views: **We owe** (suppliers) and **Customers owe us**.
- **More:** daily sales, profit, reports, receipts, backups and settings.

### Home screen

- Three prominent actions: **Add stock**, **Remove stock**, **Record payment**.
- **Record sale** is available inside Remove stock and can be pinned as a Home shortcut.
- Cards for stock value, low-stock count, money owed to suppliers and customer dues.
- Recent activity and configurable shortcuts; keep profit details on a separate screen.
- Unpaid balances are visible when opening the app. No scheduled debt reminders or payment due-date system is required.
- No device-sync status: there is only one phone. Show save errors and a compact last-successful-backup status with an easy **Back up now** action.

### Interaction rules

- Show essential fields first. Place notes, invoice photos and references under **More details**.
- Use plain labels, large readable text, clear contrast and comfortable touch targets.
- Show recent/favourite products and remember common supplier choices, allowing review before saving.
- Display stock in familiar units, for example **2 boxes + 3 pieces** alongside the base-unit total. A box of ten makes this 23 pieces. Keep one underlying quantity so the two displays cannot diverge.
- Preserve unfinished entries. Drafts must not change stock or account balances until saved.
- Show a clear Saved message. Prevent accidental duplicate saves from repeated taps.
- Corrections should be easy and retain history. Avoid confirmation dialogs for harmless navigation; clearly confirm actions that reverse records or replace data.
- When a payment is short, offer **Give discount** or **Keep remaining amount as credit**.

### Branding and language

- Brand: **Ali Educational**. Use only the poster's black/gold identity and supplied logo, not its product collage.
- Dark palette: background `#111411`, cards `#1C211D`, text `#F6F2E9`, gold `#D9AC46`.
- Optional light palette: off-white background, white cards, dark text and restrained gold accents.
- Clean the supplied logo background and preserve the design. On dark screens, use warm-white shapes and original gold details. Use a small black logo on receipts.
- Logo on the unlock/welcome screen, Home and receipts. Existing sign-in mockups must become an unlock screen with no account selector.
- Urdu uses proper script and RTL layout. Currency, numbers, product codes and mixed-language names must remain readable.
- App language, theme and receipt language are separate settings.

Validate a prototype by asking the brother to receive goods, record a sale, give a discount, add credit and record repayment. Existing mockups are references and need the latest navigation/account changes before approval.

## 5. Phase 1: local Android foundation and app lock

### First use

1. Choose English or Urdu.
2. Confirm the shop name; optionally add contact details and logo.
3. Enable app lock using Android's device authentication.
4. Offer backup setup during onboarding, explain the destination, and let the brother connect Google Drive or choose a manual export destination. He may skip setup, but Home/Settings should truthfully show that no successful backup exists yet.
5. Start with an empty catalog or import products/opening stock.

No app registration, email login, server account or separate app password is required. If the phone has no secure device lock, guide the user to Android settings to set one before enabling protected access.

### Unlock behaviour

- Use Android's authentication prompt for supported biometrics or device PIN/pattern/password.
- Do not store fingerprints, device credentials or a custom copy of the phone password.
- Fingerprint is the preferred everyday path; device credentials are the fallback when needed.
- Lock after inactivity or returning from the background beyond the chosen interval. Avoid repeatedly interrupting brief app switches.
- Exact timeout is still open; propose a configurable timeout with a short default.
- The app must not offer a bypass if device authentication fails.

### Local storage and integrity

- Use a transactional local database as the source of truth; exact framework/database choices come during implementation.
- Save products, stock movements, purchases, sales, credits, payments, settings and attachments on the phone.
- Related updates save together: a credit sale cannot reduce stock without also recording the correct customer debt.
- Use stable internal record IDs even where names or SKUs change.
- Store money with fixed currency precision; avoid floating-point rounding errors. Default currency is Indian rupees.
- Record timestamps and business dates consistently. Historical transactions retain their recorded prices and costs after product prices change.
- Local screens read the same saved records, so stock, sales and account totals update together. This is internal consistency, not multi-device synchronization.
- App lock and database/backup encryption are different concerns. Choose and verify at-rest protection and recovery handling during implementation; do not assume fingerprint unlock alone encrypts data.
- Phone loss, app uninstall or clearing app data can remove local records. Recovery depends on a usable backup.

## 6. Phase 2: catalog, import and inventory

### Product details

- Name, category, subcategory, brand, optional SKU, optional photo and notes.
- Variants such as colour, notebook size and paper type have separate stock quantities.
- Purchase costs, piece/base-unit selling price and separate pack/box selling price.
- Multiple suppliers with different recorded purchase prices.
- Low-stock threshold: shop default with per-product override.
- Archive unused products without removing their historical transactions.

### Units

- Select an appropriate base unit, usually pieces, and define units per pack/box for each product.
- Packs and pieces use the same stock. Selling two packs of ten deducts twenty pieces.
- Do not assume that one pack always has the same size across products.
- Preserve historical conversion values if a pack size is later changed.
- Fractional quantities, if needed for any product type, require a confirmed unit rule; do not silently round them.

### Import existing stock

- Support manual product entry and spreadsheet import for approximately 3,000 records.
- Plan for Excel `.xlsx` and CSV; confirm exact supported formats against sample records.
- Suggested columns: product name, category, brand, SKU, variant, base unit, pack size, opening quantity, purchase cost, selling prices, supplier and low-stock threshold.
- Preview rows and map spreadsheet columns before saving. Show invalid/missing fields and duplicate SKUs.
- As requested, import normally creates new products. Let the user decide on detected duplicates; do not silently overwrite or merge.
- Existing Google Drive spreadsheets can be downloaded or selected as files for import. Direct Drive-link or Google Sheets integration is not promised in the initial release.
- Opening quantities may also be entered manually. Opening costs are needed for reliable valuation and actual profit on that stock.
- Prevent accidental repeat import from doubling opening stock. Record import batches and provide a clear correction path.

### Stock movement actions

- Receive stock; record sale; shop use; giveaway; damage/loss; supplier return; manual correction.
- Correction can set the counted total or add/remove a quantity. Retain the calculated adjustment and reason.
- Track sellable and damaged quantities separately. Moving goods to damaged stock reduces sellable quantity without pretending they were sold.
- Warn about insufficient stock, but allow proceeding and clearly show any shortage.
- Search by name, SKU, category or brand; recent/favourite items and quick-add product entry.
- Low-stock list is for viewing/filtering. No automated reorder workflow initially.
- Record date, quantity, reason and linked transaction for each movement. A single user needs change history, not multi-user audit permissions.

No dedicated physical-count sessions, expiry tracking, product bundles or required barcode scanning in the initial scope.

## 7. Phase 3: purchases and accounts

### Receive goods

- Choose supplier, products/variants, quantities and purchase costs.
- Save supplier invoice number/date, optional photo/scan and notes.
- Record total goods value, delivery charge, amount paid and remaining payable.
- Saving increases stock and records supplier debt/payment once, in one operation.
- Purchase orders are not required; enter stock when it arrives.

### Delivery charges

- Goods costing ₹500 plus ₹50 delivery have a total stock cost of ₹550.
- Keep delivery visible separately while including it once in item cost.
- **Proposed, pending confirmation:** distribute delivery according to each item's purchase value.
- Reconcile rounding so allocated costs equal the full charge. A zero-value purchase needs an explicit alternative allocation, not division by zero.
- Identify whether delivery is owed to the supplier, paid separately to a carrier or already settled. Do not increase the wrong account's balance.
- Multiple purchases with different costs need a chosen costing policy before actual profit is enabled; see open decisions.

### Suppliers: We owe

- Supplier name, phone, address, optional source-invoice tax details, bank/payment details and notes.
- Bill history, payments, outstanding amounts and total running balance.
- Full and partial payments; cash, UPI, card, bank transfer and other digital methods.
- Optional payment reference and proof attachment.
- Optional opening balance when the brother chooses to bring in old dues.
- Supplier returns can initially use stock adjustments. Any financial credit must have a corresponding linked account adjustment; changing quantity alone must not silently alter money owed.

### Customers: Customers owe us

- Name and phone; optional for fully paid ordinary sales, required for credit.
- Credit entries linked to stock where applicable, optional opening balance and partial repayments.
- Account history with credits, payments, adjustments and current balance.
- Optional payment receipt for repayments.
- No credit limit or due date; unpaid balances appear on opening the app.
- Old debt repayments reduce the balance and are not new sales or new profit.
- Standalone monetary credit entries must be distinguishable from itemised sales so they do not invent stock movements or product profit.

## 8. Phase 4: daily sales and discounts

### Record a sale

1. Select products and quantities, in pieces or packs.
2. Use saved selling prices or change them for this sale.
3. Apply an optional whole-sale discount.
4. Record payment method, amount received and any remaining customer credit.
5. Save: stock, sales and accounts update together.
6. Optionally preview, print, save or share a receipt.

This is a short sale-recording flow, not a requirement for a full counter POS. A customer is optional when fully paid. Whole-sale discounts are included; item-level discounts are not required.

### Record item sale or record daily total

- **Individual entries:** record each sale as it happens.
- **End-of-day summary:** enter the additional sales not already entered, with quantities and amounts per product.

When preparing a summary, show existing recorded totals and clearly label the new entry as additional unrecorded sales. Never count the same sale twice. If stock was already deducted in a quantity-only movement, attach the selling details to that movement rather than deducting again.

Also allow **Record daily total** for busy days when only the monetary total is known. Ask whether it is the full day's net sales or additional unrecorded sales. For a full-day total, show already recorded net sales and calculate only the unrecorded remainder; if the entered total is less than recorded sales, require review rather than creating a negative balancing sale. Show the date and scope clearly before saving.

Money-only entries do not identify which products left the shop. Label them **Stock details needed** and **Profit unavailable** where appropriate. Keep them in monetary sales totals without inventing inventory movements or costs. Adding item details later must replace/link the existing summary allocation, not add a second sale. Quantity-only stock-outs similarly need selling amounts for actual profit.

Because stock is the priority, the brother should normally use Record item sale or enter sold quantities by product at day-end. The app should list unresolved stock details for follow-up without blocking unrelated work. Do not claim inventory is fully current while sales quantities are missing.

### Separate types of money movement

Keep **Sales**, **Other money received**, and **Expenses/payments** distinct. A deposit or money introduced into the shop is not automatically sales income. Supplier payments and old customer repayments retain their existing account links and must not be duplicated in a separate ledger.

Provide simple optional date, amount, category and note entries for other money received and shop expenses in More, with clear classification and no invented stock movements. This basic separation is included in Phase 4; detailed expense analytics remain later. Shop expenses stay separate from product gross profit, and cash-flow balance must not be labelled profit. Opening balances are separate from current-period income.

### Daily view

- Sales before discounts, total discounts and net sales.
- Amounts paid at sale and customer credit created.
- Customer repayments displayed separately from sales.
- Gross profit only for entries with sufficient cost and selling data; show coverage/incomplete entries.
- Today, week and month shortcuts, with selectable date range where useful.

### Discounts

- Enter a percentage and calculate the final price.
- Or enter the agreed final price and automatically calculate the discount value and percentage.
- Example: ₹100 reduced to ₹90 gives ₹10 discount, or 10%.
- If ₹90 is received against ₹100 due, explicitly ask whether the other ₹10 is a discount or unpaid credit. No silent forgiveness.
- Keep original amount, discount value/percentage, final amount, received amount and balance in the record and receipt.
- Allow decimal percentages, rounded currency amounts, and zero-total handling. Proposed percentage range: 0 to 100.
- Changing a product's selling price and giving an explicit discount are distinct actions. Do not mislabel every price edit as a discount.
- Corrections retain history and consistently adjust linked stock, money and profit.

## 9. Phase 4: expected and actual gross profit

### Expected gross profit

Potential sales at saved selling prices minus the stock's purchase cost including delivery. Show after receiving goods if selling prices and costs are available. It is an estimate, not money already earned.

### Actual gross profit

Recorded sales after discounts minus the cost of the units sold, including allocated delivery. Unsold goods remain inventory; do not deduct the entire purchase cost from the first sale.

Rent, electricity and other shop expenses are excluded. Label these figures **gross profit / profit after item costs**, not overall net business profit. Payment collection and sales profit are separate: a credit sale creates a debt; later repayment does not create profit again.

### Worked example

| Entry | Amount |
| --- | --- |
| 20 pens at ₹10 | ₹200 |
| 10 notebooks at ₹30 | ₹300 |
| Delivery | ₹50 |
| Total stock cost | ₹550 |

Using the proposed purchase-value allocation, pens receive ₹20 delivery and notebooks ₹30. Cost is ₹11 per pen and ₹33 per notebook.

At selling prices of ₹15 per pen and ₹45 per notebook, potential sales are ₹750 and expected gross profit is ₹200.

Sell 10 pens: original sale ₹150; 10% discount ₹15; net sale ₹135; cost of sold pens ₹110; actual gross profit ₹25. Expected gross profit on remaining stock is ₹160.

### Accuracy rules

- Unknown purchase costs or selling amounts must be marked incomplete, not treated as zero.
- Sales against negative stock need a defined cost-resolution rule; do not invent a reliable profit value before cost is known.
- Select a consistent policy for changing purchase costs, such as FIFO or weighted average, before implementation of profit calculations. This choice is still open.
- Define how purchase-level profit works under that costing policy; the example uses one purchase and does not settle mixed-batch behaviour.
- Retain historical transaction costs and discounts when product prices change.
- Prevent delivery from being counted both in unit cost and as another subtraction from profit.
- Expected profit depends on piece versus pack selling prices; identify the price basis used, rather than claiming one estimate fits every sale mix.

## 10. Phase 5: receipts and reports

### Ordinary receipts

Use the latest supplied shop setup: not GST registered. The initial app produces ordinary receipts, without a shop GSTIN, GST charge breakdown or GST-tax-invoice wording. This is the requested configuration, not an independent legal registration check. Tax filing and input-credit calculations are out of scope.

Receipts show shop name/logo/contact, reference/date, relevant items or payment, original amount, discount, final total, paid amount and remaining balance. Supplier invoice source data can be retained without making outgoing receipts into tax invoices.

- Automatic receipt references with a configurable prefix; no duplicate references.
- Plain black-and-white design and small black logo.
- English or Urdu receipt; proper Urdu layout and readable numbers.
- Optional receipts for simple sales, customer repayments and supplier payments.
- No compulsory receipt for stock adjustments.

### Receipt settings

Choose one default action: no action, preview, print, save PDF or share, including WhatsApp. Choose default English or Urdu. Both can be overridden for an individual receipt.

WhatsApp opens the sharing flow with the receipt ready; the brother chooses and sends it. No automatic messaging. Printer model, connection method and paper width remain to be selected; PDF/share should work before printer-specific integration is finalised.

### Reports and graphs

- Stock on hand and value, low stock, damaged stock and movement history.
- Purchases and delivery costs; supplier/customer balances and payment histories.
- Daily sales, discounts and gross profit with incomplete-data warnings.
- Sales graphs with selected date range and daily, weekly or monthly grouping, based on recorded sales.
- PDF and spreadsheet exports; export totals must agree with in-app records.
- Keep advanced analysis off Home so the everyday interface remains simple.

## 11. Phase 6: backup, restore and reliability

### Backup design

- Working data stays on the phone. No shop computer or live cloud database is needed.
- Provide user-controlled backup export and restore. Include the database and required attachments/settings, with a clear backup timestamp/version.
- Optional Google Drive connection is only for backup; it is not app login and must not be required to use local features.
- Allow a chosen automatic backup schedule and manual backup. Keep dated copies so an earlier one can be selected.
- A schedule is a target: phone background restrictions, no internet or full storage may delay it. Show last successful backup, failure and retry state truthfully.
- Decide backup encryption, retention, recovery method and handling of unavailable Google accounts before implementation.
- A backup held only on the same phone does not protect against losing the phone. Make the destination and success clear.

### Restore

- Preview backup date and contents before restoring.
- Make clear that restoring an older backup can replace newer local records; preserve a recovery copy where possible.
- Validate backup integrity/schema before replacing data, and recover safely if interrupted.
- Demonstrate restore onto a replacement/test phone before relying on the app for real shop records.

### Required testing

- App restart/process interruption while saving; no partially applied sale or payment.
- Repeated taps do not duplicate transactions.
- Piece/pack conversions, damaged stock, negative-stock warning and opening import duplicates.
- Purchase plus delivery, partial supplier payment and customer credit repayments.
- ₹100 to ₹90 discount versus ₹10 remaining credit.
- Individual sales plus additional daily summary without double-counting.
- Old debt repayment excluded from new sales; stock-only entries marked incomplete for profit.
- Worked profit example, historical price changes and agreed costing policy.
- English/Urdu display, mixed-language names, receipts, exports and large text.
- Fingerprint/device-credential fallback, lock timeout and drafts preserved appropriately.
- Backup failure, missing internet, full storage and successful restore.
- Smooth search and practical operation with about 3,000 products and representative sales history.

Pilot with a small set of real products and balances alongside the brother's existing records. Reconcile daily stock and amounts, note confusing steps and improve the interface before expanding to all products. Use an isolated pilot database and explicitly reviewed live opening balances; never merge pilot activity into live records by accident.

## 12. Phase 7: dedicated testing and acceptance

Testing runs during every development phase, followed by a dedicated release gate. See Appendix A in this document for test IDs, expected results and pass criteria.

The supplied `Ali Educational 29-Oct-2026.xls` is an income/expense ledger with 171 dated transactions, not a product stock list. Its totals and running balances have been checked. It will test file handling, numeric/date parsing, daily-sale classification and reconciliation. Product quantities, purchase costs, pack conversions, credit and delivery require separate labelled fixtures.

The initial importer must either support legacy `.xls` or offer a clear verified conversion route in addition to `.xlsx` and CSV. It must not interpret money-only ledger entries as stock quantities or product profit. Historical ledger import remains a separate scope decision; providing this file for tests does not automatically authorise importing it into live balances.

Run calculation tests, linked workflow tests, real-file import tests, offline/interruption tests, Android authentication, Urdu and large-text checks, receipts/exports, realistic volume checks, and backup restoration. The brother must trial the everyday tasks in an isolated test database. Log expected versus actual results and fix critical defects before real use. No application tests are claimed to have passed yet because the app is not built.

Release only after all applicable critical checks pass, source records reconcile without duplicates, and a backup restores successfully into a clean test installation. Keep original test files unchanged and synthetic fixtures out of live data.

## 13. Phase 8: optional future scope

These are not prerequisites for the phone-only first release:

- Full POS, multiple parked carts, complex split-tender checkout and cash-register opening/closing.
- Full returns, refunds and exchanges. Earlier preference: returned items enter sellable stock, then damaged items are manually moved; reconfirm when building this module. Refund by chosen method or reduction of credit; collect/refund exchange price differences.
- Detailed expense analytics beyond the basic money-category separation; services such as printing/lamination with editable default prices and no stock quantities.
- Historical sales import and richer analytics.
- Optional local AI for English/Urdu text questions and explanations only. No record changes or external AI data upload. Validate exact functions and offline performance on the actual phone; RAM alone is not a guarantee.
- Computer access, more users and multi-device synchronization only if requested again. They are removed from the current architecture requirements.

No bundles, separate wholesale price lists, expiry tracking, credit limits or dedicated stock-count sessions are currently required.

## 14. Data records to support the features

This is a conceptual model, not a fixed database schema:

- Shop settings, language/theme, receipt defaults and lock preferences.
- Product, variant, unit/pack conversion, category, supplier-product association and photo.
- Opening-stock/import batch and stock movement ledger.
- Purchase/invoice lines, delivery allocation, payment and supplier balance adjustments.
- Customer, credit entry and repayment.
- Sale and lines, price snapshots, discounts, payment, remaining credit and linked stock movement.
- Daily monetary summaries with date/scope, allocation to item sales and unresolved stock-detail status; other-money and expense entries linked to any existing payment to avoid duplication.
- Cost allocations/snapshots determined by the chosen costing policy.
- Receipt reference, export metadata, attachment and draft.
- Correction/reversal history and backup metadata.

Prefer balances derived from linked transactions rather than unrelated editable totals. Editing opening balances or correcting mistakes must leave an explainable adjustment trail. Archiving a product or account must preserve its history.

## 15. Still-open implementation choices

These do not prevent reviewing the specification. Resolve them in the relevant phase, one question at a time:

1. Actual Android model/version and preferred lock timeout.
2. Development framework, local database and supported Android versions.
3. Delivery allocation rule and handling of separately paid delivery.
4. FIFO versus weighted-average costs, including purchase-level profit and negative stock.
5. Whether any products need fractional quantities; default basis for expected pack/piece profit.
6. Sample Excel files, product codes, opening quantities, opening costs and old account balances.
7. Printer and paper size, if physical printing is wanted immediately.
8. Google Drive backup choice, schedule, retention and encryption/recovery approach.
9. Final cleaned logo and shop contact details for receipts.

## 16. Initial release definition

The app is ready when the brother can unlock it, receive goods, track stock, record daily sales/discounts, manage both kinds of debt, see justified gross profit, issue optional receipts and safely restore his data, all with a simple English/Urdu phone interface and without needing a computer or app account.

The current deliverable is this specification. Previous mockups and PDFs must be treated as older references wherever they still show account login, helpers, computer sync or the old five-tab navigation.

## 17. Handover, installation and updates

For the initial one-person Android release, deliver a signed release APK through a private download link (for example Google Drive). This is the installable application, not its source code or an Android App Bundle. Include version number, release notes and a short installation/backup guide. Test installation on the brother's Android version before handover. Review applicable Android developer-verification requirements for his device/region at release time.

The brother downloads the APK, permits installation from the chosen source when Android requests it, and installs it. This installation permission is separate from shop-data access or app login. No need to turn off Play Protect. Google Drive used to deliver the APK is also separate from optional Google Drive backup integration inside the app.

Keep a stable application ID and securely retain the release signing key for future updates. Never send the key or its passwords along with the app. Deliver a higher-version signed APK for updates and install over the existing app, without uninstalling or clearing its data. Before updates, make a usable backup; test database migrations and verify existing stock, accounts and attachments survive. Do not promise preservation without upgrade tests.

Start with an isolated pilot and then an empty live database or an explicitly reviewed import. Do not bundle the supplied shop ledger as production seed data. Add install, upgrade with existing records, interrupted migration and restore-on-replacement-phone checks to Phase 7.

Distribution references: [Android alternative distribution](https://developer.android.com/distribute/marketing-tools/alternative-distribution), [App signing](https://developer.android.com/studio/publish/app-signing).

## 18. Changing shop records: import requirements

The supplied spreadsheet represents how the shop may keep its real records. Treat its current values and layout as a sample snapshot, not a permanent schema, fixed opening balance, or universal set of totals. Dates, row counts, note wording, categories, columns and amounts can change in future files.

The importer must offer file preview, sheet/header selection and mapping to supported fields. Suggest recognised mappings but let the user correct them. Identify totals and opening balances explicitly, preserve source notes, and report skipped/invalid rows before committing. Save mapping presets only with validation against the next file's structure.

Keep product/opening-stock import distinct from daily money-ledger import. Recognising a row as income does not make it a sale; purchase-like wording does not establish stock quantities or outstanding supplier debt. Where product, quantity or cost information is missing, preserve supported money records without inventing stock or gross profit. The brother should review classification before import.

Future cash-ledger import scope must be confirmed before implementation; the representative file establishes the need to design for this workflow, not permission to silently import every row into live records. Daily money-only records should remain possible without compulsory item detail, with explicit limits on inventory/profit calculations. Basic new expense/other-money entry is included in Phase 4. Bulk historical expense import still requires a reviewed migration mapping and explicit scope decision.

Test both the supplied snapshot and changed copies: different dates/amounts, reordered or renamed columns, added categories, blank rows, missing required fields and overlapping imports. Snapshot totals in the test plan apply only to the unchanged supplied file. New files require newly calculated reconciliation controls.


## 19. Final scope priorities

1. Keep stock accurate with quick item/quantity entry and familiar box/piece displays.
2. Keep supplier and customer balances correct with simple payment records.
3. Offer daily monetary totals as a fallback, with visible missing-stock and missing-profit information.
4. Keep sales, other money received and expenses clearly separate without turning Home into an accounting dashboard.
5. Make backup setup, last-success status, manual backup and tested recovery part of the first release.
6. Prove the workflows in a small parallel pilot with the brother before full adoption.

This final document consolidates the feature plan, phased delivery, distribution approach and detailed test plan. Open technical choices remain listed in section 15; examples and suggested defaults are not silently treated as final decisions.


### Appendix A. Detailed testing and acceptance plan

### Current status

User clarification: this file represents the shop's real record-keeping style, which may change. It is not a fixed app template or production seed. The controls below apply only to this supplied snapshot. Test altered layouts and newly computed totals as well as this baseline; do not hardcode the baseline counts or amounts into application logic.

The source spreadsheet has been inspected and its arithmetic reconciled. No Android app exists yet, so application tests below are NOT RUN. Source arithmetic checks are not proof of app behaviour.

### Supplied test data

Source: `C:\Users\Musid\Downloads\Ali Educational 29-Oct-2026.xls`, sheet `Transactions`.

The source is a genuine legacy Excel `.xls` workbook, not `.xlsx`. It was read without modifying it or executing spreadsheet macros. Use it as a read-only fixture; never overwrite the original or import test transactions into live shop data without an explicit migration step.

- Used range: A1:F180. Headers at row 5: Date, Notes, Description / Category, Income, Expense, Balance.
- Row 6: Previous Balance, zero.
- Rows 7–177: 171 dated transactions across 29 distinct dates, 1–29 October 2026. The title refers to the full October month; actual supplied entries stop on 29 October.
- Rows 178–180: summary rows, not transactions.
- Numbers are stored as text with Indian-style separators; parse `4,47,241` as 447241. Dates need explicit parsing, not locale guessing.
- It is an income/expense ledger. It has no product catalog, SKU, item quantity, unit cost or structured customer/supplier balances.

#### Verified source controls

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

### Test stages

1. During each implementation phase: run calculation and persistence tests for the feature being added.
2. Phase 7 integration testing: complete linked flows on a clean test database and verify stock/account/sales consistency.
3. Real-file import testing: exercise this workbook or a reviewed conversion of it, with a documented mapping and reconciliation.
4. Android device testing: app lock, offline use, interruption, accessibility, Urdu, performance and storage failure.
5. Brother's acceptance pilot: realistic shop tasks followed by comparison against written expected results.
6. Release gate: fix critical defects, repeat affected tests and restore a backup on another test installation before live use.

### Real-file tests

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

### Functional and financial tests

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

Add boundary cases: 0/100% discounts, decimals and paise rounding, empty fields, malformed quantities, different pack sizes, zero-value delivery-allocation bases, and mixed purchases after the costing policy is agreed.

### Android, language and recovery tests

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

### Brother's acceptance pilot

Use an isolated test database. Ask the brother to add stock, find a product, record a sale, choose discount versus credit, receive a repayment and recover a backup without coaching. Record confusion, wrong taps and failed tasks; revise the interface and retest. Confirm no test data survives into live opening balances.

### Release criteria and evidence

- All applicable critical data-integrity, calculation, unlock and restore cases pass on the target Android phone.
- Source-import totals match exactly to paise, with all 171 rows accounted for whenever cash-ledger import is included.
- No unresolved defect that loses data, silently duplicates money/stock, misstates debt/profit or bypasses app lock.
- Any unavailable feature or deferred test is explicitly documented; it is not marked Passed.
- Maintain a results log with test ID, app build, device, fixture, expected/actual result, status, evidence and defect reference.
- Brother accepts the core workflows and a restore has been demonstrated before launch.

Current execution status: source parsing and arithmetic checks completed; all app/import/UI/restore cases remain NOT RUN until implementation exists.

### Additional acceptance cases for the final workflow

| ID | Action | Expected result |
| --- | --- | --- |
| FINAL-01 | Stock is 23 pieces, 10 per box | Display 2 boxes + 3 pieces and base total 23; pack and piece actions reconcile. |
| FINAL-02 | Already recorded net sales ₹600; enter full-day net total ₹1,000 | Add only ₹400 unallocated monetary sales; daily total ₹1,000, not ₹1,600. |
| FINAL-03 | Enter additional unrecorded sales ₹400 after ₹600 | Daily total ₹1,000, with the extra entry scope preserved. |
| FINAL-04 | Later supply item details for that ₹400 | Allocate/replace the summary; no second sales total and no duplicate stock deduction. |
| FINAL-05 | Enter full-day total below recorded sales | Request review; do not invent negative sales or silently rewrite transactions. |
| FINAL-06 | Record other money received and rent expense | Neither changes product stock nor gets misreported as sales or product gross profit. |
| FINAL-07 | Record a supplier payment or old debt repayment | Appears once in money history and linked account; no duplicate ledger event. |
| FINAL-08 | Skip backup setup, then complete a manual backup | Initially show no successful backup; afterwards display actual success time/destination. |

All application cases remain NOT RUN until the application is implemented. The supplied spreadsheet's arithmetic checks are the only completed data checks described here.

