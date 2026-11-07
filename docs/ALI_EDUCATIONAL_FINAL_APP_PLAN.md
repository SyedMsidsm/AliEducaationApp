# Ali Educational: complete Android app specification

Final consolidated specification | 29 October 2026

**Latest scope update (31 October 2026): billing and inventory are both primary. Phone-camera barcodes plus search, favourites and categories; no short-code entry. The Billing upgrade appendix overrides older conflicting deferrals and navigation. Design-only update; the existing Android code was not modified.**

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
| Delivery | Proportional allocation by purchase value; included once in stock cost |
| Costing | Weighted-average cost per product/variant |
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
- **Confirmed:** distribute delivery proportionally according to each line's purchase value (quantity × purchase price after purchase discounts, before delivery). Line delivery share = total delivery × line purchase value / total purchase value.
- Reconcile rounding so allocated costs equal the full charge. A zero-value purchase needs an explicit alternative allocation, not division by zero.
- Identify whether delivery is owed to the supplier, paid separately to a carrier or already settled. Do not increase the wrong account's balance.
- The confirmed costing policy is weighted-average cost per product/variant. Use the moving weighted-average implementation described in the profit section when new stock arrives.

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

Using the confirmed proportional purchase-value allocation, pens receive ₹20 delivery and notebooks ₹30. Cost is ₹11 per pen and ₹33 per notebook.

At selling prices of ₹15 per pen and ₹45 per notebook, potential sales are ₹750 and expected gross profit is ₹200.

Sell 10 pens: original sale ₹150; 10% discount ₹15; net sale ₹135; cost of sold pens ₹110; actual gross profit ₹25. Expected gross profit on remaining stock is ₹160.

### Accuracy rules

- Unknown purchase costs or selling amounts must be marked incomplete, not treated as zero.
- Sales against negative stock need a defined cost-resolution rule; do not invent a reliable profit value before cost is known.
- Confirmed policy: weighted-average cost. For this transaction-based app, implement a moving average per product/variant: new average = (existing stock cost value + incoming goods cost including allocated delivery) / (existing quantity + incoming quantity). Use base-unit quantities; never average different products together.
- Expected profit for a new purchase may use that purchase's landed cost. Actual gross profit uses the product's weighted-average unit cost at the time of each sale. When purchases are mixed, show actual profit by product/sale/period, not an asserted actual profit attributable to one supplier invoice. Keep purchase summaries clearly labelled as estimates.
- Retain historical transaction costs and discounts when product prices change.
- Prevent delivery from being counted both in unit cost and as another subtraction from profit.
- Expected profit depends on piece versus pack selling prices; identify the price basis used, rather than claiming one estimate fits every sale mix.



### Confirmed cost calculations and controls

Delivery allocation and weighted-average costing are confirmed user decisions, supplied in the attached decision screenshots. They are no longer open choices.

Implementation detail: use a moving weighted average after each receipt. Example: 10 existing pieces with total cost ₹100; receive 10 more with goods cost ₹140 and allocated delivery ₹20. Combined quantity is 20, combined cost ₹260, and average cost ₹13 per piece. Sell 5 pieces for ₹20 each with a 10% whole-sale discount: gross sales ₹100, discount ₹10, net sales ₹90, cost ₹65 and actual gross profit ₹25. Remaining stock is 15 pieces with cost ₹195.

Store each sale's applied cost so later receipts do not retroactively change previously recorded profit. Retain sufficient internal precision for per-unit costs, round displayed currency to paise, and reconcile total allocated delivery exactly. For multiple sale lines, allocate the whole-sale discount consistently by pre-discount line value before reporting product profit.

When stock is zero with no unresolved cost, the next receipt establishes the new average. Negative stock, missing opening cost, returns and backdated edits must not use the ordinary formula blindly: flag unresolved profit and define an auditable correction/recalculation rule before release. Keep sellable-to-damaged transfers at the applicable cost; never create profit from a quantity transfer.

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
3. Zero-value purchase allocation exception and handling of separately paid delivery. Proportional allocation for positive-value purchases is confirmed.
4. Negative-stock cost resolution and backdated corrections. Weighted-average costing is confirmed; only these exceptional behaviours need final implementation rules.
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

Add boundary cases: 0/100% discounts, decimals and paise rounding, empty fields, malformed quantities, different pack sizes, zero-value delivery-allocation bases, and mixed purchases under the confirmed weighted-average costing policy.

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

## Appendix B. UI/UX, theme and logo assets

The complete current UI/UX specification is embedded below so this plan remains understandable without separate files. Links refer to files shipped beside this Markdown document; keep the folder structure when sharing. Earlier PDFs/images remain dated references and do not override the current single-phone design.

### Asset manifest

- [Original supplied logo](assets/brand/ali-logo-original.png)
- [Dark logo concept - cleanup pending](assets/brand/ali-logo-dark-concept.png)
- [Poster - branding reference only](assets/brand/ali-educational-poster-reference.jpg)
- [Detailed current UI/UX file](UI_UX_SPECIFICATION.md)
- [Theme guide text](THEME_GUIDE.md)
- [Earlier theme PDF](output/pdf/Ali_Educational_Theme_Guide.pdf)
- [11-screen phone preview PDF](output/pdf/Ali_Educational_Phone_Previews.pdf)
- [Earlier desktop preview - future reference only](output/pdf/Ali_Educational_App_Screen_Previews.pdf)
- [actual-profit screen image](output/mobile-preview/actual-profit.png)
- [customer screen image](output/mobile-preview/customer.png)
- [discount screen image](output/mobile-preview/discount.png)
- [expected-profit screen image](output/mobile-preview/expected-profit.png)
- [home screen image](output/mobile-preview/home.png)
- [receive-cost screen image](output/mobile-preview/receive-cost.png)
- [settings screen image](output/mobile-preview/settings.png)
- [short-payment screen image](output/mobile-preview/short-payment.png)
- [signin screen image](output/mobile-preview/signin.png)
- [stock screen image](output/mobile-preview/stock.png)
- [supplier screen image](output/mobile-preview/supplier.png)

### Logo previews

![Original Ali logo](assets/brand/ali-logo-original.png)

![Dark logo placement concept - not final production artwork](assets/brand/ali-logo-dark-concept.png)

### Ali Educational: UI/UX implementation reference

Current scope: one Android phone, one user, local storage. This specification overrides outdated login, computer-sync and five-tab elements in the earlier preview images.

#### Visual system

| Role | Dark default | Optional light |
| --- | --- | --- |
| Background | #111411 | #F7F6F1 |
| Cards, navigation, dialogs | #1C211D | #FFFFFF |
| Main text | #F6F2E9 | #171A17 |
| Secondary text | #B9C0B8 | #566057 |
| Gold accent | #D9AC46 | #A47720 |
| Borders | #354038 | #DADFD8 |
| Success | #75B890 | #24754C |
| Warning | #E4B765 | #915E13 |

Use gold for primary actions, selection and focus, not entire screens. Pair warnings/errors with explicit text rather than colour alone. Final error colours and component sizes must be tested in both themes. Proposed implementation defaults: 16sp body text, 24–28sp screen headings, at least 48dp interactive targets, 16dp page padding, 8dp spacing increments and restrained 8–12dp corner radii. These are starting values for device testing, not previously approved fixed measurements.

Use readable Android fonts with reliable Urdu shaping and system text scaling. Do not force Urdu into Latin typography or use decorative fonts for numeric balances. Check RTL reading order, mixed product codes, rupee values and long names. Keep typography and icon styles consistent. Avoid clutter, heavy shadows and decorative imagery.

#### Logo rules

- Source of truth: `assets/brand/ali-logo-original.png`.
- Dark placement concept: `assets/brand/ali-logo-dark-concept.png`. Preserve gold details; black parts become warm white; transparent background. This needs production cleanup, not a silent replacement with an invented logo.
- Brand reference only: `assets/brand/ali-educational-poster-reference.jpg`. Do not use its product collage as the screen layout/background.
- Unlock: centered logo above Ali Educational, clear space, fingerprint/device-unlock action below; no account selector, email or app password field.
- Home: compact logo beside shop name without pushing stock actions below the fold.
- Receipt: small black-only logo on white beside shop name. A clean monochrome production asset has not yet been produced.
- Preserve aspect ratio; no stretching, cropping the mark, shadows or pale rectangular background on dark surfaces.
- Android launcher/adaptive icon and splash asset must be prepared from the approved logo during implementation. They do not exist yet; check crop-safe placement on actual Android launchers.

#### Screen inventory and flow

| Screen | Essential controls and behaviour |
| --- | --- |
| First-use setup | Language, shop identity, Android device-lock setup if needed, optional backup setup and import/empty-start choice |
| Unlock | Logo/name, biometric/device credential prompt; no app account; preserve drafts when relocking |
| Home | Add stock, Remove stock, Record payment; stock and dues cards; low-stock list, recent activity, configurable shortcuts, last backup/Back up now |
| Stock list | Name/SKU search, category and low-stock filters, recent/favourite products, add/import; quantities in boxes + pieces and base units |
| Product detail | Variant, prices, pack size, sellable/damaged quantities, supplier choices, threshold, photo and movement history |
| Add/edit product | Minimal required details first; extra fields below More details; warn about matching SKU without silently merging |
| Receive stock | Supplier, items, quantities, purchase costs, delivery, paid/balance; optional invoice scan; expected profit when inputs are complete |
| Remove stock | Item, quantity and reason; sale route links selling/payment fields; shop use/giveaway/damage/return routes remain quick |
| Accounts | We owe and Customers owe us; search, total balances and add account |
| Supplier/customer detail | Running balance, transaction history and record payment/repayment; optional details/attachments; no invented due dates |
| Record item sale | Items, quantity, prices, whole-sale discount, amount paid/method and customer if credit remains |
| Record daily total | Date, full-day versus additional amount, already recorded sales and remainder preview; unresolved stock details clearly flagged |
| Short payment | Two plain choices: Give discount / Keep remaining amount as credit; show resulting balance before save |
| Discount | Percentage or agreed final amount; original price, discount value/percentage and final total visible |
| Daily records | Sales separate from other money received and expenses; repayments not counted as sales; missing item/cost information visible |
| Profit | Expected and actual gross profit clearly separated; delivery and discounts explained; coverage warnings, no fake zero-cost profit |
| Reports/graphs | Date range, daily/weekly/monthly grouping, simple filters and PDF/spreadsheet export |
| Receipt preview | English/Urdu, black on white, discount/payment/balance detail; default action with per-receipt override |
| Import | File/sheet/header choice, column mapping, preview, errors/duplicates, opening/total-row classification, confirm and result summary |
| Backup/restore | Last successful backup, destination, manual action, optional Drive schedule, dated restore preview and replacement warning |
| Settings | Theme, app language, receipt action/language, stock defaults, lock timing, backup and shop details |
| More | Access secondary tasks without filling Home with reports/settings |

Bottom navigation is **Home / Stock / Accounts / More**. Secondary screens return to their originating section. Avoid nested menus for the three everyday Home actions.

#### Common states and safeguards

- Empty: explain what to add/import with a clear next action; never display sample data as real shop data.
- Draft: retain unfinished work, but do not post balances or stock until saving.
- Saving: prevent duplicate submissions; show Saved only after successful persistence.
- Invalid entry: identify the exact field and fix; preserve the rest of the form.
- Missing stock details: keep money records but flag missing quantities.
- Profit unavailable: identify missing cost or selling value; do not substitute zero.
- Insufficient stock: explain the shortage and allow the agreed proceed option.
- Offline: local work continues; only backup/sharing dependencies need network messaging. Remove all computer/Wi-Fi sync labels.
- Backup failure: show actual last success and retry option; do not claim the scheduled backup ran.
- Edit/reversal: show effect on linked stock/accounts and retain history.
- Accessibility: readable text scaling, labelled controls, focus order and non-colour status cues. Verify in English and Urdu.

#### Existing preview inventory

The earlier phone PDF contains 11 mockups, with illustrative amounts. It is a visual reference, not a completed clickable prototype or approval of its outdated controls.

| File under output/mobile-preview/ | Reference screen |
| --- | --- |
| home.png | Home/dashboard |
| stock.png | Inventory |
| supplier.png | Supplier account |
| customer.png | Customer account |
| settings.png | Receipt defaults |
| signin.png | Old sign-in; replace with device unlock |
| discount.png | Percentage/final-amount discount |
| short-payment.png | Discount versus credit |
| receive-cost.png | Purchase and delivery |
| expected-profit.png | Expected profit |
| actual-profit.png | Recorded actual profit |

Refresh these references during Phase 0: merge supplier/customer tabs into Accounts; remove account login, computer sync and helper references; add backup status, daily-total scope, import mapping and stock-first shortcuts; verify Urdu and light theme; make scrolling/action placement safe above bottom navigation. Existing English mockups do not demonstrate a completed Urdu UI.

#### Design acceptance

The brother should complete receiving stock, recording item sales, recording a daily total, choosing discount/credit, entering repayment and making/restoring a backup in an isolated prototype/test app. Record confusion and revise before full adoption. Test one-handed use, smaller screens, long labels, large text, keyboard appearance, scroll behaviour, RTL and both themes. A static PDF does not prove these interactions work.


## Appendix C. Billing upgrade - latest requirements

### Ali Educational: fast billing upgrade

31 October 2026. Planning and phone UI concepts only; no application code changed. This update supersedes earlier statements that full counter billing and barcode scanning are deferred. Billing and inventory are now both primary features. One Android phone, local offline storage, device authentication, English/Urdu, accounts, backup, proportional delivery allocation and moving weighted-average costing remain unchanged.

#### Confirmed requirements

- Use the phone camera to scan manufacturer product barcodes.
- Barcodes are optional. Products without them are selected by name search, favourites and category browsing.
- No short-code feature. Existing internal IDs or imported identifiers may remain for data integrity, but do not introduce short-code entry into the billing workflow.
- Scanned and manually selected items share one bill, saved prices, quantity editing, discounts and inventory rules.
- Four primary tabs: Home, Stock, Billing, Accounts. Settings, reports and backups are available from Home without adding extra bottom tabs.
- Home prioritises New bill alongside Add stock and Record payment. Stock removal for shop use, damage or giveaway remains a distinct action inside Stock.

#### Recommended counter workflow

1. Open Billing or New bill. Start an empty local draft.
2. Scan a barcode, search a name, tap a favourite or browse a category. No customer details required for a fully paid sale.
3. Review items, choose piece/pack, adjust quantity or price, and optionally give a whole-bill discount.
4. Choose Cash, UPI, another payment method or customer credit. Cash received may exceed the bill: show change, not a negative credit balance.
5. Save once. Stock, sale, payment, customer credit and cost snapshots commit together.
6. Show Saved with optional receipt action and New bill. Printing/sharing failure must not undo or duplicate the sale.

Keep a cart total and Review/payment action visible above bottom navigation. Use a full-screen scanner only while scanning; manual selection remains a prominent fallback. Avoid asking for unrelated invoice references or notes during checkout.

#### Camera and barcode behaviour

- Ask for camera permission when first opening Scan. If refused or camera unavailable, explain and offer manual selection; the app remains usable.
- Run barcode decoding locally with a bundled decoder so first-use scanning works offline; select the exact Android library during implementation.
- Barcode data identifies the product, not the shop's price or current quantity. Store the mapping to a product/variant and unit (piece or a specific pack). Preserve leading zeros and format details; do not parse codes as numbers.
- Support common retail formats found on actual stock, validating real samples during development. A QR code is not automatically a retail product barcode or payment instruction; do not open scanned links.
- One barcode/unit mapping must be unambiguous. Warn on duplicates; never silently choose between two products.
- One product may have multiple legitimate barcodes, including distinct piece and pack codes. A pack scan uses its mapped quantity and pack price. Never assume a box code means one loose piece.
- Unknown barcode: offer Link to existing product, Create product or Cancel. Keep the bill intact. Linking creates no stock, purchase cost or automatic supplier lookup.
- Prevent continuous detections of a code still in view from adding repeatedly. Require it to leave/re-enter the scan frame or a deliberate Add again action. Give a brief visual confirmation, optional vibration and Undo last addition.
- Fast quantity buttons are available for several identical items. Scanning an item intentionally again increases quantity without creating confusing duplicate lines when price/unit/discount treatment matches.
- Camera controls: back, torch when supported, frame guidance, last added item and View bill. Use labelled UI rather than relying on colour or sounds alone.

#### Products without barcodes

- Search by product name, brand or variant; category filter; favourites for frequent items.
- Show name/variant, saved selling price and available units in results. Use images only if available; no required photography.
- Select piece or pack before applying conversion. Favourites should identify the saved unit or request unit choice explicitly.
- Example: a loose pen from a box can be selected as one piece even if only the box has a barcode.
- Empty search offers a quick new-product flow with minimal details; do not invent purchase cost. Missing cost makes profit incomplete, not zero-cost profit.

#### Bill, payment and receipt rules

- Whole-bill discount by percentage or agreed final amount, showing original price, discount and net total. No item-level discount feature is required.
- Short payment explicitly offers discount versus remaining credit; remaining credit requires a customer.
- A Paid in cash action may use exact tender for simple sales; enter cash received when change is required.
- UPI/card/bank selection records a payment chosen by the user; it does not verify a bank transfer or process payments. Do not label it bank-verified without an actual integration.
- Save prevention: stable command ID, atomic database transaction and duplicate-tap protection. Drafts have no stock/debt effect. Scanner additions change only the draft.
- Capture sold unit conversion, selling price, discount allocation and weighted-average cost at posting. Later prices or receipts do not alter historical sale profit.
- Warn on insufficient stock and allow the agreed proceed path. Unknown cost/negative-stock exceptions stay visible.
- Bills receive unique automatic references, with a chosen prefix. Saved bills are reviewable in Billing history; edit/cancel via auditable corrections, not silent deletion.
- Ordinary non-GST receipt, black/white logo, English/Urdu default and per-receipt choice. Default none/preview/print/PDF/share including WhatsApp remains configurable.
- Detailed refund/exchange flows remain separately planned; promoting billing does not imply those flows are already built.
- Daily sales totals derive from posted bills. The existing daily-total fallback must show posted sales and add only unrecorded remainder, never double-count bills or deduct stock twice.

#### Hold and resume: proposed for this upgrade

Multiple held bills are recommended for interrupted service. The user has not explicitly confirmed this feature yet; present it as a proposal. Store each draft locally, show item count/amount, and support resume/discard. Holding a bill does not reserve or deduct stock; recheck stock and changed prices on resume, while preserving the draft's displayed prices until reviewed. Persist drafts across app restarts and relocking.

#### UI pictures and design status

The accompanying four phone pictures show: mixed scanned/manual item selection, camera scanning, bill review, and cash payment/change. Camera content is an illustrative placeholder, not a live scan. Sample products and balances are fictional. The UI uses the approved dark-and-gold theme and supplied logo concept, which still needs production cleanup.

English pictures demonstrate layout; production implementation must provide Urdu RTL and light theme too. Review camera permission, unknown barcode, empty results, errors, customer-credit selection and saved receipt screens in the clickable prototype, not just these static examples.

#### Implementation phases for the existing app

1. Inspect the actual latest source project and database; back it up. Map current sale/stock/payment APIs before designing migrations. Do not rebuild or erase the existing app.
2. Review a clickable billing prototype and confirm held bills. Test search/category/favourites with the brother.
3. Add optional barcode mappings and favourite state through non-destructive migrations. Preserve existing products with no barcode and historical stock.
4. Build manual item selection/cart first, then camera scanning and barcode linking. Test on the connected phone with genuine manufacturer labels and airplane mode.
5. Implement posting/payment/change, discounts, credit and cost snapshots by extending the existing transaction layer. Keep draft and posted states separate.
6. Add bill history, optional receipt actions, Urdu/light theme, accessibility and held bills if approved.
7. Run regression, migration, recovery and counter-use tests. Package a signed in-place update only after data-preservation tests pass.

#### Acceptance tests

| ID | Scenario | Pass criterion |
| --- | --- | --- |
| BILL-01 | Known product barcode | Correct variant/unit/price added once; stock unchanged until posting. |
| BILL-02 | Code held continuously in camera frame | No repeated accidental additions. Deliberate rescan/Add again works. |
| BILL-03 | Unknown or duplicate mapping | Safe link/create/review; no wrong item or inferred quantity. |
| BILL-04 | Leading-zero code and piece/pack codes | Codes preserved; correct conversion and pack price. |
| BILL-05 | Mix camera, search, favourites and categories | One consistent cart; no short-code requirement. |
| BILL-06 | Permission denied, poor light, offline first use | Manual route works; torch if supported; local scanning needs no download. |
| BILL-07 | ₹165 bill, cash ₹200 | ₹35 change; ₹165 sale, not ₹200 revenue. |
| BILL-08 | ₹100 bill, ₹90 paid | Explicit discount versus ₹10 debt choice; account required for debt. |
| BILL-09 | Repeat Save, force-stop during posting | Exactly one complete posting or none; linked records reconcile. |
| BILL-10 | Later daily-total entry | Existing bills included once; stock not deducted again. |
| BILL-11 | Share/print cancelled or failed | Posted sale remains; retry receipt does not post a second sale. |
| BILL-12 | Upgrade app with existing products/records | Existing data, costs, receipts and lock/backup settings remain valid. |
| BILL-13 | If held bills approved: hold/restart/resume | Draft preserved with no stock movement; stock rechecked on save. |
| BILL-14 | Urdu, light theme, large text, keyboard | Totals/actions readable and reachable; no obscured checkout action. |

All app tests are NOT RUN in this design-only update. Device-library selection and scanner compatibility must be validated against the actual app and phone.


#### Phone UI pictures

![New bill: scan or manually select](output/billing-preview/billing.png)

![Camera scanner concept](output/billing-preview/scan.png)

![Bill review](output/billing-preview/review.png)

![Cash payment and change](output/billing-preview/payment.png)
