# Stationery Shop Inventory App — Revised Feature Plan

**Planning status:** revised after the shop's priorities and GST status were clarified; implementation has not started.  
**Shop:** one stationery shop in Karnataka, about 3,000 products and variants. GST registration is **unconfirmed and currently believed to be absent**.  
**Primary purpose:** know what is in stock, whom the shop owes, and who owes the shop. Daily sales and billing are secondary.  
**Devices and languages:** Android phones and computers; English and Urdu script with a right-to-left Urdu layout.

## 1. Recommendation

Build a **stock and balances app first**, not a full point-of-sale system. The first version should make it easy to receive goods, correct stock, record goods leaving the shop, enter supplier bills and payments, and track customer credit and repayments. These records should give the owner a reliable answer to three questions: **What do we have? Whom do we owe? Who owes us?**

Stock cannot remain accurate if goods sold or given away are never deducted. To keep the focus away from daily billing, use a **quick stock-out** action: choose an item and quantity, then mark the reason (sale, shop use, giveaway, loss, or supplier return). Several small cash sales can be entered as one end-of-day stock-out if that is easier, though this reduces sales detail. When an item leaves on customer credit, link the stock-out to that customer's balance. Full checkout and an invoice for every sale can wait.

## 2. First release: inventory and money owed

| Area | First-release behavior |
| --- | --- |
| Product catalog | About 3,000 items and variants; categories and subcategories; brand, SKU when known, optional photo, and multiple suppliers with different purchase costs. Manual entry and spreadsheet import with preview and duplicate-SKU warning. |
| Units and stock | Keep a base quantity in pieces. Set product-specific pieces per pack/box and separate piece/pack prices. Show sellable and damaged quantities separately. Set a default low-stock threshold with product overrides. Record every change with quantity, reason, time, and user. Correct stock by setting a new total or adding/removing pieces. |
| Goods received | Enter quantities when they arrive, linked to a supplier and purchase invoice. Save purchase cost, invoice number/date, total amount, and optional invoice photo/scan. Receiving stock increases quantity and can create a supplier payable in the same step. No purchase-order workflow is needed initially. |
| Supplier dues | Store supplier name, phone, address, optional GSTIN, and payment details. Show each bill, unpaid amount, total balance, partial payments, payment method, optional reference/proof, and history. Allow opening balances when the records are available. |
| Customer dues | Store customer name and phone. Create a credit entry for goods taken or an opening balance, link it to stock-out when applicable, and record partial repayments. Show every outstanding entry and total balance. No credit limit or due date is required; show unpaid balances when the app opens. |
| Quick stock-out | Search/select products by name, SKU, category, or brand; recent/favorite items. Enter pieces or packs, quantity, reason, and optional customer. Warn if recorded stock is insufficient but allow the change. Record shop use and giveaways separately. |
| Dashboard and reports | Make **low stock, stock value, supplier dues, customer dues, and recent stock movements** the main dashboard. Reports for stock on hand, damaged stock, stock movements, purchases, supplier balances, and customer balances; PDF and spreadsheet export. A simple optional sales summary can be derived only from stock-outs marked as sales. |
| Access and history | Owner controls each helper's permissions. Password/PIN and supported biometric unlock. Show what changed, when, and by whom. Allow corrections while retaining the original record and the adjustment history. |
| Offline, sync, backup | Work on each device without internet; sync over shop Wi-Fi, show pending changes and errors, and back up to the owner's Google Drive on a selectable schedule. Restore a chosen dated backup. |

### Receipt setting

In **Settings > Receipts**, choose **one default action**: **do nothing, show on screen, print, save as PDF, or open the share flow (including WhatsApp)**. Every time a receipt is created, the owner can override that default before completing the action. Set a default receipt language of English or Urdu as well, with a per-receipt override. A receipt may be issued for a customer credit repayment, supplier payment, or simple sale record; ordinary stock adjustments do not need receipts. The first WhatsApp version opens the share flow with the receipt ready for the owner to send. Printer-specific support can be finalized after a printer is chosen.

## 3. Defer from the first release

- Full counter checkout, paused carts, split payments on each sale, item-level discounts, automatic invoices for every sale, detailed returns/exchanges, and daily cash-register closing.
- Detailed sales and profit reports and customizable sales graphs. They need consistent transaction-level sales data; an inventory-first app will otherwise show incomplete figures. Add them when the shop decides to record sales in detail.
- Expenses separate from stock purchases, services without stock, and historical sales import.
- Optional AI. Later, a read-only local AI feature may explain stock patterns or answer questions about dues in English and Urdu. It should never edit records or send shop data to an external AI service. The core app must work fully with AI off.

These features remain possible later. Keep the data model ready to add sales and returns without replacing the stock and balance records.

## 4. GST status and receipts

Until registration is verified, configure the shop as **not GST registered**. Outgoing receipts should show the shop name, address, phone, logo, items/amount, and an internal reference number, but **no shop GSTIN, GST charge, CGST/SGST breakdown, or claim that they are GST tax invoices**. CBIC guidance says an unregistered seller may use an ordinary commercial invoice and cannot issue a GST tax invoice; the CGST Act prohibits an unregistered person from collecting GST. [CBIC sectoral FAQ](https://cbic-gst.gov.in/sectoral-faq.html), [CGST Act, Section 32](https://cbic-gst.gov.in/hindi/CGST-bill-e.html)

The app may retain a supplier's GSTIN and the **total amount actually paid** on an incoming supplier invoice as source information. Do not calculate input-tax credit or subtract “recoverable GST” from stock cost while the shop is treated as unregistered. For simple stock valuation, use the actual landed purchase cost, with optional extra charges if the shop wants to record them. Ask the accountant to verify the shop's registration and obligations before finalizing tax behavior. Registration status can also be checked through the GST portal's taxpayer search when a GSTIN is available. [GST portal taxpayer-search guidance](https://tutorial.gst.gov.in/userguide/taxpayersdashboard/Search_Taxpayer.htm)

If the shop later becomes registered, add a separate, reviewed GST mode from its effective date. Do not silently alter historical receipts or recalculate old balances.

## 5. Offline design and a practical limit

Use an on-device database on each device, with the shop computer as the main sync node while it is on. Record stock movements and payments as separate events so balances can be rebuilt and audited. When two offline devices change the same stock, keep both movements and show any resulting shortage; conflicting edits to product details should be reviewed instead of overwritten. Offline writes need an explicit conflict-resolution rule. [Android offline-first guidance](https://developer.android.com/topic/architecture/data-layer/offline-first)

The phone can show the latest shop data at home after the computer is switched off **only if all devices sync before the owner leaves**. The app should make “all devices synced” a clear closing check. Google Drive is for dated backup and recovery, not the live sync database. Use a narrow Drive permission where practical. [Google Drive scope guidance](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)

## 6. First-release acceptance checks

1. Import sample catalog data and opening quantities, including variants and different pack sizes.
2. Receive a supplier bill, increase stock, then part-pay it; confirm the remaining supplier balance.
3. Record a customer credit stock-out and two partial repayments; confirm both stock and customer balance.
4. Record goods leaving for cash sales without making a detailed bill for each sale; confirm stock remains correct.
5. Adjust a damaged item and review the full movement history.
6. Change the default receipt action and language in Settings, then override both for one receipt.
7. Work on two devices while offline, sync, review a conflict, back up to Google Drive, and restore a dated backup.

## 7. Inputs still needed before implementation

1. Verify whether the shop has a GST registration or GSTIN; an accountant should review this status before any tax-related feature is used.
2. Review a few current spreadsheets and paper records to define the import format, opening stock, and any opening customer/supplier balances. SKU availability remains to be confirmed.
3. Confirm the Android phone, computer operating system, printer, backup schedule, and how helpers will access the shop Wi-Fi.
4. Decide whether simple sales should be recorded item by item or as end-of-day stock-outs. This determines how detailed future sales reports can be.

## 8. Suggested build order

1. **Foundation:** catalog, variants, units, opening stock, stock movement ledger, local storage, permissions, audit history, and import.
2. **Core operations:** goods received, supplier bills and payments, customer credit and repayments, quick stock-out, low-stock alerts, and balances dashboard.
3. **Reliability and sharing:** offline sync, conflict review, Google Drive backup/restore, English/Urdu interface, default and per-receipt delivery/language choices, reports and exports.
4. **Later modules:** full billing/POS, detailed sales and profit graphs, expenses, and optional local AI.

The first release is ready when stock and both kinds of balances stay correct through realistic shop use, including offline edits, partial payments, a sync conflict, and a tested backup restore.

## 9. Approved visual direction

Use the **Ali Educational** poster only for its black-and-gold brand identity. The app opens in a dark theme with a near-black background, warm-white text, and restrained gold accents; a light theme is available in Settings. Place the supplied Ali monogram on sign-in and the dashboard, adapting its black shapes to warm white on dark surfaces while keeping its gold details. Put a small black version with the shop name on plain black-and-white receipts. Do not use the poster's product panels as the app layout. Default receipt delivery and language remain selectable in Settings and overridable for each receipt. See the separate theme guide for colors, sizing, and placement.
