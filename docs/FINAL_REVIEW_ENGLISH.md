# Ali Educational - Complete App Plan

**Final review edition | 29 October 2026**
This consolidated plan replaces earlier scope summaries. The app is still in planning and interface design; the previews are not a working application. Confirmed preferences, proposed implementation details and later features are distinguished below.

## 1. Purpose and priorities

One stationery shop in Karnataka, approximately 3,000 products and variants. The app will work on Android phones and computers, with simultaneous use by the owner and authorised helpers. The primary questions are: what is in stock, whom do we owe, and who owes us?

Inventory and balances come first. Simple sales records support discounts and actual profit without requiring a full checkout system. AI is optional and comes later. There is no fixed launch deadline; avoiding a recurring hosting fee is preferred.

## 2. Products and stock

- Product name, category/subcategory, brand, SKU when available, optional photo, purchase cost, selling price and suppliers. Track variants such as colour and size separately. A product may have several suppliers with different purchase prices.
- Pieces and packs/boxes share the same stock. Set pack size per product and separate piece and pack selling prices. Use an appropriate base unit for each product; convert packs consistently.
- Show available and damaged quantities separately. Record stock received, sold, used in the shop, given away, lost, damaged or returned to a supplier with reason, date and person.
- Correct stock either by entering the actual total or adding/removing a quantity. Manual adjustments are enough; no dedicated physical-count workflow or expiry tracking is needed.
- Low-stock thresholds: one default with product-specific overrides. The low-stock list displays items; purchase-order automation is not required.
- Search/select manually by name, SKU, brand or category, with recent/favourite items. Quick-add missing products with basic details. Barcode scanning is not a first-release requirement.
- Warn about insufficient stock but allow the owner to proceed. Keep the resulting shortage visible for correction.

## 3. Receiving goods, delivery and supplier dues

Enter goods when they arrive: supplier, product/variant, quantities, purchase costs, invoice number/date and optional photo or scan. Receiving goods increases inventory and records the amount owed, allowing an immediate or partial payment.

Supplier records contain name, phone, address, optional source invoice tax details, bank/payment details, invoices, payments and remaining balance. Save optional payment references and proof. Existing opening balances can be entered when the owner decides which records to bring in.

**Delivery is part of stock cost.** Goods costing ₹500 with ₹50 delivery have a total cost of ₹550. Keep delivery separately visible, then include it once in the product costs used for profit.

**Proposed allocation, still to confirm:** divide delivery across products in proportion to their purchase value. Reconcile rounding so the allocated total exactly matches the delivery charge. Record whether delivery is paid to the supplier, a separate carrier, or already paid; do not add it to the wrong person's balance.

Supplier returns can use a stock adjustment initially. If a return also changes money owed, record the corresponding supplier balance adjustment separately and retain its reason.

## 4. Customer credit and payments

- Customer name and phone; optional customer selection for ordinary sales, required when money remains owed.
- Credit entries, opening balances when available, partial repayments and a running history. Link goods taken on credit to stock-out so inventory is reduced only once.
- Record cash, UPI, card, bank transfer and other digital payment types, with optional transaction reference. A repayment can have an optional receipt.
- No customer credit limits or payment due dates. Show unpaid balances when the app opens, rather than scheduled phone reminders.
- Keep payments and profit separate: collecting an old debt reduces the balance but is not a second sale.

## 5. Simple sales and discounts

A quick stock-out records item, quantity and reason without mandatory billing. For actual profit, record the sold items and their selling amounts. Aggregated cash-sale entries may be used if the quantities and amounts are known; quantity-only entries cannot provide actual profit.

Selling prices can be changed when recording a sale. Discounts apply to the whole sale, not individual line items.

- Enter a discount percentage to calculate the final amount, or enter the agreed final amount to calculate the discount automatically.
- Example: original bill ₹100, final agreed amount ₹90. The app shows ₹10 discount and 10% discount.
- If only ₹90 is received against ₹100 due, ask: **give ₹10 discount, or keep ₹10 as customer credit?** Never automatically assume a short payment is a discount.
- Show original amount, discount amount/percentage, final amount, paid amount and remaining balance in the record and optional receipt.
- Save who applied or changed the discount and when. Support decimal percentages, currency rounding and a safe zero-total case. Proposed validation: percentage between 0 and 100.
- Corrections should be easy while retaining the original record and adjustment history. Linked stock, balances and profit must update consistently without duplication.

### Daily sales recording

Daily sales recording is included. Enter individual sales or an end-of-day summary of additional sales not already recorded. For accurate stock and profit, enter quantities and sales amounts per product. Show existing recorded sales while preparing a summary to prevent double-counting. The daily view shows original sales, discounts, net sales, amounts paid at sale, new customer credit and gross profit where costs are known. Old debt repayments stay separate. A money-only total cannot identify products sold; missing detail must be flagged. Adding amounts to an existing stock-out must not deduct stock again. Receipts remain optional; full POS and cash-register closing remain later features.

## 6. Expected and actual profit

**Expected gross profit:** potential sales at saved selling prices minus the cost of the stock, including allocated delivery. Show it after receiving goods once selling prices are available. Missing prices should be flagged, not assumed to be zero.

**Actual gross profit:** recorded net sales after discounts minus the cost of the units sold, including delivery. Unsold stock remains stock; it is not all deducted from the first sale. Other shop expenses are excluded, so this is not net business profit. Credit sales and cash collected must remain distinguishable.

Example using the proposed delivery allocation:

- Receive 20 pens at ₹10 and 10 notebooks at ₹30: goods ₹500 plus delivery ₹50 = total cost ₹550.
- Allocate ₹20 delivery to pens and ₹30 to notebooks. Cost per pen is ₹11; cost per notebook is ₹33.
- Selling prices ₹15 per pen and ₹45 per notebook give potential sales ₹750 and expected gross profit ₹200.
- Sell 10 pens: original amount ₹150, discount 10% = ₹15, net sales ₹135. Their cost is ₹110, so actual gross profit is ₹25.
- Expected profit on the remaining stock is ₹160. Discounts lower the profit actually earned.

Flag sales without amounts and stock without known costs as incomplete. They must not silently enter a reliable profit total. The method for changing purchase costs across batches (FIFO or weighted average) remains to be chosen; the example above uses one purchase only.

## 7. Dashboard, reports and graphs

Main dashboard: low stock, stock value, supplier dues, customer dues and recent stock movements. Let the owner choose shortcuts such as receive stock, stock out and record payment.

Reports cover stock on hand, damaged stock, movements, purchases, customer balances, supplier balances, discounts, and the newly requested expected/actual gross profit. Export as PDF or spreadsheet. Provide today/week/month presets. Sales graphs use a selectable date range and daily, weekly or monthly grouping when sales amounts are recorded; incomplete coverage must be visible. Broader sales analytics can follow the core release.

## 8. Receipts and current shop setup

Use the latest supplied status: the shop is not GST registered. Configure ordinary receipts without shop GSTIN, GST charges or GST-tax-invoice wording. This describes the requested app configuration, not a verification of the shop's legal registration. Retain supplier invoice totals as purchase source data and use the actual cost paid; tax filing and input-credit calculation are outside this release.

Receipts show Ali Educational, a small black logo, shop contact details, reference/date, relevant items or payment, discounts, total, paid amount and remaining balance. Use automatic references with a configurable prefix; numbering across offline devices must avoid duplicates. Receipt creation is optional for simple sales and payments; a full invoice for every stock adjustment is unnecessary.

In Settings choose **one default receipt action**: no action, preview, print, save PDF, or share (including WhatsApp). Allow an override for each receipt. Also choose default English or Urdu and allow an individual override. WhatsApp sharing prepares the receipt for the owner to send; it is not automatic messaging. Printed/shared receipts use plain black-and-white styling. Printer model and paper size remain undecided.

## 9. Appearance and phone experience

Brand name: **Ali Educational**. Use only the supplied poster's black-and-gold identity and logo, not the product collage. Dark is the default, with optional light mode. Dark background #111411, cards #1C211D, warm-white text #F6F2E9 and gold #D9AC46. Light surfaces use white/off-white with dark text and restrained gold.

Preserve the supplied Ali logo design, remove its pale background and use warm-white shapes with gold details on dark surfaces. Place it neatly on sign-in, dashboard and receipts; use black artwork on receipts. The current edited logo remains a concept requiring a clean final asset.

Phone layouts use readable cards, large touch controls and four main sections: Home, Stock, Accounts and More. Accounts separates We owe from Customers owe us. Home prioritises Add stock, Remove stock and Record payment; Record sale is available under Remove stock and as an optional shortcut. Essential fields appear first, with notes and attachments under More details. Profit has a separate screen. Preserve unfinished entries, confirm saves, allow corrections with history and keep sync status unobtrusive except for pending changes or errors. Validate a clickable phone prototype with the owner and brother before full implementation. Computer layouts adapt to the wider screen. English and Urdu script are supported, with right-to-left Urdu layouts. Theme, app language and receipt language are separate settings. The 11-screen phone PDF is a visual reference with fictional data, not a complete interactive flow or final approval.

## 10. Offline work, sync and backup

Keep working data on the user's own devices. Proposed setup: local storage on each device and a shop computer that coordinates syncing over the reliable shop Wi-Fi while switched on. Clearly show pending changes, last sync and errors. Preserve both devices' stock movements; flag shortages and conflicting product edits for review rather than silently losing changes.

Home access while the shop computer is off uses the phone's last synced copy. It is current only if all devices have synced before leaving; later changes on another disconnected device cannot appear instantly. The phone can continue recording offline changes for the next sync.

The user's Google Drive is permitted for automatic dated backups on a chosen schedule, with a selectable restore point. Drive is backup storage, not the live sync database. Failed/missed backups must be visible and retried when the required device and internet are available. Protect backups and test restoration before real use. Exact backup retention and recovery safeguards remain to be defined. No paid cloud hosting is planned; storage limits and final deployment choices still need review.

## 11. Users, security and starting data

Owner plus helpers, with owner-controlled permissions for each helper. Password/PIN and supported biometric unlock. Maintain who changed what and when for products, stock, sales, discounts and payments. Sync and permission checks must apply across devices.

Use manual entry and spreadsheet import for approximately 3,000 products and opening quantities. Preview imports and warn about existing SKUs. Default to creating new records as requested, but let the owner decide when duplicates are detected; never silently overwrite. SKU availability, file formats, old sales/purchase history and opening debts are still to be reviewed. Existing records may be on paper, spreadsheets or Google Drive.

## 12. Later features and exclusions

The earlier broad billing requests remain a future backlog after the stock-and-balances priority change: full POS, multiple paused carts, split-tender checkout, automatic invoices for every sale, full returns/refunds/exchanges and cash-register opening/closing. Earlier return preference was to add returns to sellable stock, then manually move damaged goods; detailed return behaviour should be reconfirmed when that module is built. Exchanges would settle the price difference, with refund method or credit reduction selectable.

Other later items: expenses kept separate from product profit; services such as printing/lamination with editable default prices and no stock; historical sales import; richer graphs; basic computer keyboard shortcuts. If cash closing is added, supplier/expense payouts were requested to be adjusted manually. No bundles, wholesale price lists, expiry tracking, credit limits or dedicated stock-count sessions are required.

Optional local AI may answer questions and explain trends in English and Urdu text. It must not change records or send shop data to external AI services. Desired on-device use includes Android without internet or the computer. Exact AI functions and feasibility must be validated on the actual devices; 8 GB phone RAM and 16 GB computer RAM estimates do not guarantee performance. The main app must work with AI off.

## 13. Open decisions for final review

1. Approve or change proportional delivery allocation; define handling of separate delivery payments.
2. Choose costing for changing purchase prices and how it supports purchase-level profit.
3. Agree how consistently item quantities and selling amounts will be recorded for actual profit.
4. Confirm device models, computer operating system, packaging/installation approach and printer/paper size.
5. Review sample import files, SKUs, opening quantities/debts and whether historical records are needed.
6. Choose backup schedule/retention, recovery method and exact helper permissions; finish logo cleanup.
7. Reconfirm later-feature priorities and exact optional AI functions only after the core works.

## 14. Build order and acceptance

Build order: catalog/units and stock ledger; receiving and supplier/customer balances; simple sale discounts and delivery-aware profit; offline sync, backups, bilingual screens, receipts and exports; then optional later modules. Reliability is part of the first release, not something postponed until after deployment.

Before launch, demonstrate: import with duplicate warnings; piece/pack conversion; receiving and partial supplier payment; linked customer credit and repayments; damaged-stock corrections; ₹100 to ₹90 discount versus ₹10 credit; delivery/profit example above; incomplete-profit warnings; corrections without double-counting; English/Urdu receipt overrides; two-device offline conflict handling; permission enforcement; and a successful dated backup restore.

**Review outcome:** approve the scope or list changes by section number. This document is ready for review, not a claim that implementation or all open decisions are complete.
