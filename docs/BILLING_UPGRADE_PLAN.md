# Ali Educational: fast billing upgrade

31 October 2026. Planning and phone UI concepts only; no application code changed. This update supersedes earlier statements that full counter billing and barcode scanning are deferred. Billing and inventory are now both primary features. One Android phone, local offline storage, device authentication, English/Urdu, accounts, backup, proportional delivery allocation and moving weighted-average costing remain unchanged.

## Confirmed requirements

- Use the phone camera to scan manufacturer product barcodes.
- Barcodes are optional. Products without them are selected by name search, favourites and category browsing.
- No short-code feature. Existing internal IDs or imported identifiers may remain for data integrity, but do not introduce short-code entry into the billing workflow.
- Scanned and manually selected items share one bill, saved prices, quantity editing, discounts and inventory rules.
- Four primary tabs: Home, Stock, Billing, Accounts. Settings, reports and backups are available from Home without adding extra bottom tabs.
- Home prioritises New bill alongside Add stock and Record payment. Stock removal for shop use, damage or giveaway remains a distinct action inside Stock.

## Recommended counter workflow

1. Open Billing or New bill. Start an empty local draft.
2. Scan a barcode, search a name, tap a favourite or browse a category. No customer details required for a fully paid sale.
3. Review items, choose piece/pack, adjust quantity or price, and optionally give a whole-bill discount.
4. Choose Cash, UPI, another payment method or customer credit. Cash received may exceed the bill: show change, not a negative credit balance.
5. Save once. Stock, sale, payment, customer credit and cost snapshots commit together.
6. Show Saved with optional receipt action and New bill. Printing/sharing failure must not undo or duplicate the sale.

Keep a cart total and Review/payment action visible above bottom navigation. Use a full-screen scanner only while scanning; manual selection remains a prominent fallback. Avoid asking for unrelated invoice references or notes during checkout.

## Camera and barcode behaviour

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

## Products without barcodes

- Search by product name, brand or variant; category filter; favourites for frequent items.
- Show name/variant, saved selling price and available units in results. Use images only if available; no required photography.
- Select piece or pack before applying conversion. Favourites should identify the saved unit or request unit choice explicitly.
- Example: a loose pen from a box can be selected as one piece even if only the box has a barcode.
- Empty search offers a quick new-product flow with minimal details; do not invent purchase cost. Missing cost makes profit incomplete, not zero-cost profit.

## Bill, payment and receipt rules

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

## Hold and resume: proposed for this upgrade

Multiple held bills are recommended for interrupted service. The user has not explicitly confirmed this feature yet; present it as a proposal. Store each draft locally, show item count/amount, and support resume/discard. Holding a bill does not reserve or deduct stock; recheck stock and changed prices on resume, while preserving the draft's displayed prices until reviewed. Persist drafts across app restarts and relocking.

## UI pictures and design status

The accompanying four phone pictures show: mixed scanned/manual item selection, camera scanning, bill review, and cash payment/change. Camera content is an illustrative placeholder, not a live scan. Sample products and balances are fictional. The UI uses the approved dark-and-gold theme and supplied logo concept, which still needs production cleanup.

English pictures demonstrate layout; production implementation must provide Urdu RTL and light theme too. Review camera permission, unknown barcode, empty results, errors, customer-credit selection and saved receipt screens in the clickable prototype, not just these static examples.

## Implementation phases for the existing app

1. Inspect the actual latest source project and database; back it up. Map current sale/stock/payment APIs before designing migrations. Do not rebuild or erase the existing app.
2. Review a clickable billing prototype and confirm held bills. Test search/category/favourites with the brother.
3. Add optional barcode mappings and favourite state through non-destructive migrations. Preserve existing products with no barcode and historical stock.
4. Build manual item selection/cart first, then camera scanning and barcode linking. Test on the connected phone with genuine manufacturer labels and airplane mode.
5. Implement posting/payment/change, discounts, credit and cost snapshots by extending the existing transaction layer. Keep draft and posted states separate.
6. Add bill history, optional receipt actions, Urdu/light theme, accessibility and held bills if approved.
7. Run regression, migration, recovery and counter-use tests. Package a signed in-place update only after data-preservation tests pass.

## Acceptance tests

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


## Phone UI pictures

![New bill: scan or manually select](output/billing-preview/billing.png)

![Camera scanner concept](output/billing-preview/scan.png)

![Bill review](output/billing-preview/review.png)

![Cash payment and change](output/billing-preview/payment.png)
