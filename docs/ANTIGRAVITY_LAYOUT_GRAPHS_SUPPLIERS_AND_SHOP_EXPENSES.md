# Ali Educational: layout fixes, graphs, suppliers and shared Shop Expenses

Date: 1 November 2026

## 1. Purpose and status

This is a detailed implementation handoff for the existing Android app. It specifies requested changes and acceptance tests; it does not claim that code has been changed, an APK rebuilt, or tests passed.

Apply changes to the current app without resetting existing records or undoing completed features. The current source location discovered during review is `C:/Users/Musid/.gemini/antigravity/scratch/AliEducational`. Verify its current branch/build before editing. The older `android` folder in the document workspace is not the current app. Do not replace the current project with an earlier review copy.

Inputs: the supplied Pigmy Savings, Sales & Profit, Accounts and Add Stock & Product screenshots, the earlier consolidated feature review, and the user's latest answers. Screenshots establish visible layout problems, not complete behavior or database correctness.

### Explicitly confirmed expense decisions

1. A Cash Out recorded as Shop Expense appears in Shop Expenses automatically, using one shared expense record. It must never be counted twice.
2. Only an explicitly linked expense refund recorded as Cash In reduces expenses. Ordinary Cash In does not reduce expenses.
3. Service costs, including paper, ink and printer repairs, belong in the same expense system with Products / Services / Shared classification.

The layout, graph and supplier details below specify the recommended implementation requested for this handoff. Do not invent historical amounts, supplier matches or expense classifications when migrating uncertain data.

## 2. Scope and priorities

Deliver:

- Responsive placement fixes for Pigmy, Sales & Profit, Accounts and stock entry.
- Readable profit summaries with consistent time filters and useful graphs.
- One supplier directory shared by Accounts and stock receiving.
- One expense system shared by Cash In/Out, Shop Expenses and service reporting.
- Linked expense refunds, synchronized edits/cancellations, attachments and accurate totals.
- Nondestructive migration, backup coverage and evidence from isolated tests.

Preserve English interface labels, Urdu/mixed-script product names, offline use, Owner/Billing restrictions, existing receipt changes, expandable Pigmy schemes and interest-free withdrawals. Do not add cloud services or automatic uploads for these changes.

Prioritize financial consistency and data preservation before cosmetic refinements. Do not change stored amounts merely to make dashboard values look better.

## 3. Common layout rules

- Preserve the charcoal/gold visual identity and optional light theme.
- Use consistent screen padding, card spacing and aligned text baselines; approximately 16 dp screen padding and 12-16 dp gaps are starting values, not rigid constraints.
- Respect status/navigation bars and keyboard insets. Remove redundant top spacers rather than removing required safe-area padding.
- Keep primary controls at least 48 dp in touch size.
- Never split a button word such as Withdraw across lines. Reflow controls or stack them instead of shrinking text to an unreadable size.
- Do not truncate important monetary amounts. Use readable Indian rupee formatting with grouping and two decimal places, including negative amounts.
- Use content-driven card heights. Long names and larger fonts must not collide with icons, totals or adjacent cards.
- At narrow widths or larger font sizes, convert two-column fields/cards into a single column.
- Keep scrolling content clear of fixed bottom actions and navigation. Keyboard opening must not hide the focused field or make Save unreachable.
- Provide accessible names for icons and distinguish selected, disabled and error states using text/shape as well as color.

## 4. Pigmy Savings placement fixes

### Observed problems

- Withdraw wraps as “Withdra” and “w”.
- Deposit, Withdraw and an unexplained history count compete for one row.
- Several management icons crowd each scheme heading.
- Summary and scheme metadata need more consistent alignment.

### Screen structure

1. Compact app bar with Back, Pigmy Savings and Add scheme; move Add scheme below the title if needed at larger text sizes.
2. Summary card showing Tracked savings balance as the main value.
3. Deposits, Withdrawals and Today's deposits as clearly labelled secondary metrics. Stack/reflow rather than squeeze.
4. Pigmy schemes heading and Active / All filter.
5. Vertically scrollable scheme cards.

### Scheme card structure

```text
Scheme name                              More
Tracked balance
₹45.00

Deposits       ₹115.00
Withdrawals     ₹70.00
Usual deposit  ₹100.00

[ + Deposit ]       [ - Withdraw ]
[ View history (4)                  v ]
```

- Deposit and Withdraw have equal visual emphasis, width and height; stack at accessibility sizes if necessary.
- View history occupies its own row and explains the displayed count. Count the history entries actually available under the stated filter; do not show an unexplained number.
- Put Edit scheme, confirmed opening balance management and Archive/reactivate in a labelled overflow menu as applicable to the existing implementation. Preserve all existing permitted actions.
- Long scheme names wrap without pushing actions outside the card.
- Disabled withdrawal remains readable; explain insufficient tracked savings when relevant. Do not weaken withdrawal validation.
- Keep balance, deposit and withdrawal semantics unchanged: savings transfers do not affect sales or operating profit.

## 5. Sales & Profit layout and meanings

### Date controls

Replace the clipped chip strip with a compact period selector supporting Today, This Week, This Month, All Time and Custom dates. Show the actual selected date range below it. Preserve the app's documented week/timezone convention and apply it consistently.

Use the same range for summary cards, charts, detail lists and exports. Current stock projections must explicitly say “Current stock” because they are not historical period sales.

### Recommended Products tab order

1. Products / Services tabs.
2. Period selector and visible date range.
3. Prominent Gross profit card with a secondary, explicitly labelled gross margin.
4. Product sales and Cost of goods sold cards.
5. Recorded product expenses, linked refunds and result after product expenses.
6. Trend graph.
7. Expense breakdown graph.
8. Expected gross profit from current stock card, separated from realized results.

Use a full-width amount below the title instead of forcing title, amount and percentage onto one row. Render a loss with a clear minus sign and label, not only red color.

### Screenshot reconciliation

| Displayed component | Screenshot value |
|---|---:|
| Product sales | ₹145.78 |
| Cost of goods sold | ₹108.50 |
| Gross profit | ₹37.28 |
| Displayed expenses | ₹12,235.00 |
| Gross profit minus displayed expenses | -₹12,197.72 |

The arithmetic is consistent. The screenshot does not establish whether all ₹12,235 should be attributed to products. Inspect the contributing entries before changing the label or classification. Never silently remove legitimate expenses to improve the displayed result.

### Calculation rules

- Net product sales = product revenue after allocated discounts and linked returns/reversals.
- Product gross profit = net product sales minus the associated historical product cost snapshots.
- Gross margin = gross profit divided by net product sales, multiplied by 100. If sales are zero or negative, show Not applicable rather than an infinite or misleading percentage.
- Net recorded product expenses = active Products-classified expenses minus their active linked refunds under the selected effective-date filter.
- Result after product expenses = product gross profit minus net recorded product expenses.
- Label this result according to what it actually includes. Show that Shared expenses are excluded; do not call a partial calculation complete shop net profit.
- Shared expenses remain separately visible and are counted once in any combined overview. Do not arbitrarily allocate them to Products or Services.
- Services shows net service revenue, recorded service expenses, linked service-expense refunds and revenue minus net recorded service expenses. This is not a claim of exact service profit.
- Missing sale costs must produce an incomplete/known-cost warning. Never treat missing costs as zero to claim exact profit.
- Customer repayments are collections, not additional sales. Supplier settlements and Pigmy transfers are not ordinary operating expenses.
- Current stock potential uses current sellable stock and known costs/prices; disclose missing costs and problematic negative stock.

## 6. Graph requirements

### A. Sales and gross profit over time

- Place below the summary, using actual persisted transactions.
- Show separate labelled series for net product sales and product gross profit, with currency axes and a visible zero baseline.
- Use daily buckets for short ranges and weekly/monthly buckets for longer ranges. State the grouping. For Today, a single daily point is acceptable; do not fabricate hourly detail.
- Include all buckets in the range, including verified zero-activity days. Distinguish missing/incomplete data from zero.
- Negative profit must extend below zero. Do not clip losses.
- Tapping a bucket shows period, sales, COGS and gross profit, with access to contributing records when feasible.
- Sum of displayed buckets must reconcile to the selected-range cards, using integer paise or equivalent exact monetary calculation.

### B. Expense breakdown

- Use readable horizontal bars by expense category.
- State whether the view shows Products, Services, Shared or All. On the Products tab default to Products and disclose that scope.
- Display expense outflows and linked refunds in the detail; the headline category amount is net recorded expense.
- Support negative net values for periods with refunds of earlier expenses. Do not force such values to zero.
- Tap a category to open the same filtered expense list used by Shop Expenses.
- Avoid pie charts for signed expense/refund data.

### General behavior

- No decorative sample data in production. Empty periods show “No transactions in this period”.
- Use readable legend labels, accessible contrast and a text summary/table alternative.
- Graphs work offline and respect Owner access. Do not send shop data to an external chart service.
- Large histories must not freeze navigation; aggregate efficiently using the same financial rules as cards.
- Edits, refunds, cancellations and date changes refresh both graphs and totals consistently.

## 7. One shared supplier directory

### User experience

Keep Add supplier in Accounts. Replace free-text Supplier Name in stock receiving with a full-width searchable selector:

```text
Supplier
[ Search or select supplier                 v ]
+ Add new supplier
```

- Both entry points read/write the same supplier directory with stable supplier IDs.
- Add new supplier opens the same creation form and returns with the new supplier selected.
- Preserve all entered product, quantity, price, invoice and attachment values while creating the supplier.
- Require a supplier name; keep optional contact details optional unless an existing requirement says otherwise.
- Normalize case and whitespace for duplicate warnings. Do not automatically merge two businesses solely because their names match.
- Renaming a supplier preserves all transaction links. Archive instead of deleting referenced suppliers.
- A supplier with a zero balance remains visible under All suppliers.
- A product can be received from different suppliers over time. Store the actual supplier on each receiving/purchase transaction. An optional preferred supplier is only a convenience, not a rewrite of history.

### Accounts placement

- Use clear Suppliers / Customers terminology, with Amount we owe and Amount customers owe as summary labels.
- Keep the amount summary separate from the Add supplier button; use the app's primary gold treatment rather than implying that supplier creation itself is a loss.
- Add search and All / Outstanding filters.
- Place an explanatory empty state beneath the controls. Distinguish “No suppliers yet” from “No outstanding supplier balances”.
- Supplier detail shows purchases, payments, adjustments/reversals and outstanding amount with effective dates and links to source records.

### Purchases and dues

Creating or selecting a supplier does not create a liability. Recording receipt of purchased stock captures invoice amount, paid amount, payment source and unpaid amount using the existing purchase model.

Example: receive ₹1,000 of stock, pay ₹600, and show ₹400 due. A fully paid ₹1,000 purchase appears in history with ₹0 due. Paying the remaining ₹400 later settles the liability; it must not create another inventory purchase or operating expense.

- Credit purchases require an identified supplier.
- Preserve existing workflows for opening inventory and product-only edits; do not invent a new purchase or debt for them.
- Adding stock, editing product details and correcting counted stock remain separate operations.
- Transport may belong to landed inventory cost, but its payee can differ from the supplier. Do not automatically add every transport charge to supplier dues or also count capitalized transport as an operating expense.
- Supplier invoice/payment save operations must be atomic and safe against repeated taps.

### Existing supplier-name migration

Inventory may already contain supplier names with no directory link. Produce a reviewable migration mapping. Automatically reuse a link only where identity is unambiguous under verified existing data rules. Keep ambiguous/unmatched names visible for Owner review. Preserve old text for provenance. Creating directory entries from old names must not invent opening balances, purchases or payments.

## 8. Add Stock & Product form layout

Use a full-screen form or a sufficiently spacious adaptive sheet with a scrollable body and keyboard-safe actions.

Recommended sections:

1. Product details: name, optional barcode/scanner, category, brand.
2. Stock: received quantity/unit and low-stock threshold.
3. Pricing: cost per piece, selling price per piece, optional wholesale price and clearly defined margin.
4. Purchase details: supplier selector, invoice number, effective date, payment/dues information appropriate to the existing flow.
5. Transport and cost summary.
6. Save and Cancel.

- Supplier, product name and long labels occupy full-width rows.
- Keep currency/unit labels above values rather than wrapping placeholders such as “Wholesale (₹/pc)” inside a narrow box.
- Verify whether the existing Margin field means gross margin or markup; label and calculate consistently. Do not change persisted prices by assuming the two are equivalent.
- Preserve existing weighted-average costing, exact transport allocation and historical cost snapshots.
- Show validation beside the relevant field and retain values after validation failures.

## 9. Cash In/Out and Shop Expenses: one shared system

### Core rule

A Shop Expense entered through Cash Out is the same economic expense shown in Shop Expenses and financial reports. Do not create one expense for each screen.

The database may use a canonical expense plus a linked cash/bank movement if needed. That is acceptable only with stable links, atomic writes and one reporting source of truth. Two related records must not become two independent expenses.

### Cash Out form

Keep transaction purpose separate from category:

```text
Transaction purpose: Shop expense
Category: Electricity
Applies to: Shared
Amount: ₹500.00
Paid from: Shop cash
Effective date and time: ...
Notes: ...
Optional photo: ...
```

- Shop expense purpose explicitly opts the transaction into expense reporting.
- Categories describe the expense, such as Rent, Electricity, Paper or Repairs. Category text alone does not determine accounting treatment.
- Applies to must be Products, Services or Shared. Do not silently assign all existing or new expenses to Products.
- For Services, retain an optional specific-service target where supported; broader service costs remain distinguishable from expenses shared across the whole shop.
- Preserve the existing ability to add a category mid-form without losing input.
- Record actual funding source: cash-funded entries reduce physical shop cash; bank-funded entries affect bank records, not physical cash.
- Saving creates one expense and its one applicable payment movement, then refreshes Shop Expenses automatically.

### Shop Expenses entry point

- The existing Shop Expenses shortcut opens the shared list.
- Add expense from this screen uses the same validation and save operation as Shop Expense in Cash Out.
- A paid expense created here appears in the relevant Cash In/Out view with its payment source identified.
- If the current app supports unpaid/accrued expense records, preserve and label that behavior; do not fabricate a cash payment for an unpaid expense. These new cash-entry flows represent actual payments.

### Shop Expenses screen

Display:

1. Period selector and actual date range.
2. Gross recorded expenses.
3. Linked expense refunds received in the period.
4. Net recorded expenses = expenses minus refunds.
5. Products / Services / Shared breakdown.
6. Category breakdown graph and transaction list.

Filters: date, category, classification, payment source and active/cancelled status as appropriate. Default financial totals exclude cancelled entries; history retains them visibly.

Each list row shows amount, expense/refund direction, category, classification, effective date, source/destination, note preview and photo indicator. Detail opens the canonical record and related refund/payment history.

All-filter totals must equal the sum of Products, Services and Shared, plus an explicitly shown Unclassified legacy bucket if unresolved old records exist. Never hide uncategorized or unclassified history.

## 10. Cash In expense refunds

### Explicit linked flow

Cash In gains or uses an explicit Expense refund purpose. The user selects the original expense, then enters refund amount, receipt destination, effective date/time, optional note/reference and optional photo.

- Offer Record refund from expense detail, preselecting the original expense.
- Link by stable expense ID, not matching descriptions.
- Show original amount, prior active refunds and remaining refundable amount.
- Permit partial and multiple refunds up to the remaining refundable amount. Reject zero/negative amounts and excess refunds.
- The refund inherits the original expense's category and classification for reporting, including its specific service target if present.
- A refund received into cash increases physical cash once; one received into a bank does not increase physical cash.
- A refund reduces recorded expenses, not product sales or service revenue.
- Ordinary sales collections, customer repayments, savings withdrawals, owner contributions and unrelated Cash In never reduce Shop Expenses.

### Date behavior

Use the refund's actual effective receipt date. Do not silently rewrite the original expense date or restate an earlier period.

Example: an October ₹1,000 expense refunded by ₹200 in November produces October expense ₹1,000, November expense refund ₹200, and combined net expense ₹800. November net expense may be negative if no other expenses occur; display it honestly and explain the earlier-period refund.

### Refund versus cancellation

- Refund means a real payment was received back and creates an incoming movement.
- Cancellation corrects an erroneous entry and reverses that entry's recorded effects with audit history. It must not pretend that a real refund occurred.
- Prevent cancellation or amount reduction of an expense while active refunds would become invalid. Explain the linked records and require their appropriate correction first. Do not silently delete refund history.
- Changing the original expense's classification updates the reporting attribution of its linked refunds consistently, without creating another money movement. Retain audit history.

## 11. Editing, consistency and attachments

- Opening the same expense from Cash In/Out or Shop Expenses must show the same identity and current values.
- Editing amount updates the existing payment effect by the correct difference; it does not add another full payment.
- Changing payment source reverses the prior source effect and applies the new one atomically.
- Changing effective date moves the same entry between report periods without duplication.
- Editing category, classification, notes or photo alone does not change cash.
- Cancellations reverse committed effects once and remain visible in audit history.
- Repeated taps, process restarts, retry and applicable sync must preserve exactly-once financial effects.
- Photos use durable managed storage. Both screens reference the same attachment; avoid independent copies that can diverge.
- Photo replacement failure keeps the previous valid photo and entered form values. Do not silently save without requested evidence.
- Backups include actual photo bytes, IDs, expense/refund links, payment links, category/classification and effective/audit timestamps.
- Owner financial data and evidence must remain inaccessible to restricted Billing roles, including through sync, exports and deep links.

## 12. Expenses that must not be double-counted

| Event | Shop Expenses treatment |
|---|---|
| Rent/electricity paid as Shop expense | Include once in chosen classification |
| Service paper/ink paid as Shop expense | Include once under Services |
| Explicit refund linked to an expense | Reduce that expense classification on refund date |
| Pigmy deposit or withdrawal | Savings transfer; exclude |
| Customer repayment | Debt collection; exclude |
| Supplier payment settling stock purchase | Liability settlement; exclude from operating expenses |
| Stock purchase and capitalized delivery | Inventory cost; do not also post as operating expense |
| Product sale return/refund | Sales reversal; not an expense refund |
| Transfer between cash and bank | Transfer; exclude |
| Photo/category edit | No financial effect |

Do not add together an old service-expense table and its new canonical replacement. Define one query/source of truth for each report. If legacy migration is uncertain, show review status instead of deleting or guessing.

## 13. Migration, imports, exports and recovery

- Inspect the existing schema and transaction paths before choosing implementation details. Stable IDs and links are requirements; a particular schema layout is not mandated.
- Back up before migration and test upgrades from supported old schemas in isolation.
- Reconcile existing Shop Expenses, service expenses and cash entries. Merge/link only demonstrably identical economic records; amount/date/category similarity alone is insufficient evidence.
- Preserve original IDs where possible, or retain an explicit stable mapping with provenance.
- Preserve supplier balances, cash totals, stock quantities and historical costs unless a separately reviewed correction is required.
- Do not infer expense type from arbitrary words in historical Notes. Preserve unresolved legacy classification visibly.
- Existing supported spreadsheet import flows should preview purpose/classification/refund links where available. Older files lacking these fields remain reviewable; do not manufacture links.
- Export selected filters only, including empty results. Include category, classification, purpose, original expense/refund ID where appropriate and attachment presence.
- A spreadsheet is not a complete photo backup. Do not export inaccessible local paths as if they were portable evidence.
- Restore a complete test backup into a separate installation and verify photos, IDs, links, supplier dues and expense totals. Restoring records must not replay their cash effects.

## 14. Acceptance tests

Use synthetic data in a disposable app/database. Record expected and actual results, not only a ticked checklist.

### Layout and graphs

| ID | Scenario | Expected |
|---|---|---|
| L01 | Pigmy at narrow phone width | Deposit/Withdraw readable; history on separate row |
| L02 | Large font and long scheme name | No clipped actions, overlapping amounts or split button words |
| L03 | Stock form with keyboard open | Focused field and Save accessible; input retained |
| L04 | Large currency values | Full amounts readable with grouping; no collision |
| L05 | Report period selection | No clipped label; range visible; cards/graphs/list agree |
| L06 | Zero sales / no activity | Honest empty state; no fabricated chart or undefined margin |
| L07 | Loss and refund-heavy period | Negative values shown below zero and in summaries |
| L08 | Graph bucket totals | Exactly reconcile to underlying selected-range totals |
| L09 | Missing cost or pending sync | Incomplete result disclosed, never shown as verified complete |
| L10 | Light/dark themes and accessibility | Readable labels, contrast, focus order and touch targets |

### Suppliers

| ID | Scenario | Expected |
|---|---|---|
| S01 | Add supplier in Accounts | Selectable immediately in stock receiving |
| S02 | Add supplier during stock entry | Appears in Accounts; entered stock form values survive |
| S03 | Receive ₹1,000, pay ₹600 | Stock added once; supplier due ₹400; paid source reduced ₹600 |
| S04 | Pay remaining ₹400 | Due zero; no extra stock purchase or operating expense |
| S05 | Fully paid purchase | Supplier retained in All; excluded from Outstanding |
| S06 | Rename/archive supplier | History and links intact; outstanding balances remain accessible |
| S07 | Similar legacy supplier names | Ambiguous identities reviewed, not silently merged |
| S08 | Edit product name only | No new stock, debt or cash movement |
| S09 | Same product from another supplier | Each receipt retains its actual supplier |
| S10 | Repeated save/restart | One purchase, one payment and correct remaining liability |

### Shared expenses and refunds

For E01-E06 start from an isolated cash balance of ₹5,000 with no expenses/refunds. Keep all events in one test period unless stated otherwise.

| ID | Action | Expected |
|---|---|---|
| E01 | Cash Out ₹500 electricity, Shared | Cash ₹4,500; one expense; gross/net expenses ₹500 |
| E02 | Add ₹200 paper, Services through Shop Expenses | Cash ₹4,300; gross/net expenses ₹700; service expenses ₹200 |
| E03 | Cash In ₹100 refund linked to electricity | Cash ₹4,400; gross ₹700; refunds ₹100; net ₹600; Shared net ₹400 |
| E04 | Ordinary Cash In ₹300 | Cash ₹4,700; net expenses remain ₹600 |
| E05 | Edit paper expense ₹200 to ₹250 | Cash ₹4,650; gross ₹750; refunds ₹100; net ₹650 |
| E06 | Cancel paper expense with no linked refund | Cash ₹4,900; gross ₹500; refunds ₹100; net ₹400; history retained |
| E07 | Bank-funded expense | Bank effect once; physical cash unchanged; expense included once |
| E08 | Bank-received linked expense refund | Bank increases once; cash unchanged; expense reduced once |
| E09 | Multiple partial refunds | Cumulative amount validated against original expense |
| E10 | Refund above remaining amount | Rejected without financial writes |
| E11 | Cancel/edit expense below active refunded amount | Clear linked-record validation; no orphan refund or hidden deletion |
| E12 | Refund of prior-period expense | Original period unchanged; refund period shows reduction; combined net reconciles |
| E13 | Change expense date/category/classification | Same identity; filters refresh; no duplicate cash effect |
| E14 | Replace/remove photo | Financial values unchanged; both screens show same evidence state |
| E15 | Retry/double tap/interrupted save | One committed expense/refund and one applicable movement |
| E16 | Record Pigmy/supplier settlement/customer repayment | Excluded from Shop Expenses |
| E17 | Products/Services/Shared filters | Breakdown sums to All, including disclosed unresolved legacy bucket |
| E18 | Restore backup elsewhere | Same IDs/totals/links; photos open; cash movements not replayed |
| E19 | Restricted Billing access | Owner expenses/photos blocked across UI, data and applicable sync |
| E20 | Export empty selected period | Empty result; never silently exports all records |
| E21 | Migration with suspected duplicate legacy expense | No automatic double count or destructive guessing; review evidence retained |
| E22 | Create category during expense entry | Amount/date/classification/photo preserved; new category selected |

### Profit regression

- Product sales ₹1,000 and saved COGS ₹600 give gross profit ₹400.
- Products expenses ₹100 and linked Products refund ₹20 give net product expenses ₹80 and result after product expenses ₹320.
- Shared expense ₹50 stays separately visible; it is not silently subtracted from both Products and Services.
- Service expense appears once in the service view and once as the same record in the All expenses view, never twice in a combined total.
- Editing current catalog cost does not rewrite the ₹600 historical COGS.
- Credit repayments do not add revenue; savings transfers do not change profit.

## 15. Delivery and release evidence

Provide:

1. Changed-file summary and concise explanation of canonical supplier/expense links.
2. Before/after screenshots for all four affected screens at normal and larger text sizes.
3. Graph screenshots with positive, negative and empty periods and numerical reconciliation.
4. Expense/refund and supplier purchase/payment expected-versus-actual results.
5. Migration and isolated backup/restore evidence, including restored photos.
6. Passed, failed and unverified tests, with device/build details. A successful compile does not prove Android UI or accounting behavior.
7. Candidate APK identity/version/hash and known limitations if a build is produced.

Do not uninstall the live app, clear its storage or use destructive migration. Production installation is a separate action requiring explicit user approval. This document requests implementation preparation and verification, not authorization to overwrite live shop data.

## 16. Approved addition: Bill History linked to customer Accounts

The user approved a dedicated Bill History screen and explicitly requires it to link to Accounts, where the app shows how many customers owe the shop and the total amount owed. These are shared financial records, not independent balances maintained by each screen.

### Navigation and contents

- Add a clearly labelled History action in Billing and a Today's bills shortcut on Home.
- Show completed bill count, net billed amount, payments applied to the selected bills and remaining bill dues. Show cancellations and refunds separately. Held/draft bills do not count as completed sales or customer debt.
- Filter by Today, This Week, This Month, All Time or Custom dates, payment status, products/services and originating device where available.
- Search by receipt number, customer name or phone number. Search results do not change the underlying records.
- Each row shows persisted receipt number, original date/time, customer or Walk-in, amount, payment status and device where applicable.
- Bill detail shows item/service snapshots, quantities, prices, discounts, tendered/applied payment and change where relevant, outstanding amount, later repayments, returns/refunds and cancellation history.
- Reopen, print, save PDF and share the persisted bill without creating another sale, payment or stock movement.

### One customer identity and one source of truth

- Link credit bills to the same stable customer ID used by Accounts. Do not match customer debt by display name or phone text alone.
- Use the same customer selector and Add customer flow in Billing and Accounts. Preserve the active bill when adding a customer. Warn about possible duplicates without automatically merging identities.
- A sale with any unpaid amount requires an identified customer. Do not allow unassigned Walk-in debt.
- Accounts lists all customers, with an Outstanding filter. Customers with zero due remain accessible in All customers and retain their history.
- Display Customers who owe as the count of distinct customers with a positive reconciled amount due, not the count of unpaid bills.
- Display Total customers owe as the sum of positive reconciled customer balances. Show customer credits/advances separately; do not let one customer's credit hide another customer's debt.
- Derive both screens from the same canonical bills, payments, allocations and linked adjustments. Do not add cached bill dues to an already aggregated customer balance.
- Preserve reviewed opening balances and any valid legacy non-bill debt separately with provenance. Do not invent historical bills to represent an opening balance.

### Cross-navigation

- Customer Accounts detail includes outstanding bills, settled bill history, opening/legacy balance items, repayments and linked adjustments.
- Tapping a bill in Accounts opens that same Bill History detail.
- Tapping the customer or View customer account from Bill History opens that customer's account.
- Accounts summary is the current outstanding position unless explicitly offering a documented as-of view. A Bill History date filter selects bills by their original bill date; it does not silently limit the customer's overall account balance.
- Clearly label Remaining due on selected bills separately from Total customer balance, which may include older bills and opening debt.

### Payments and allocations

- Saving a completed credit/part-paid bill atomically records the sale, initial applied payments, customer debt and existing required stock effects once.
- Record a repayment from either Accounts or bill detail using the same payment command and stable payment ID. A repayment updates the cash/bank destination and customer/bill balances once; it is not another sale or new revenue.
- Bill-detail repayment preselects that bill. From Accounts, offer allocation to outstanding bills and separately recorded opening debt, with an explicit preview. An oldest-first suggestion is acceptable, but the user must see the allocation before saving.
- One payment may settle multiple bills. Preserve allocation amounts and require their sum to equal the amount applied; never credit the full payment independently to every bill.
- If overpayment/advances are already supported, keep the excess as an explicit customer credit with clear confirmation. Otherwise reject amounts above the amount being settled; do not silently create negative bill dues.
- Keep payment receipt date separate from original bill date. Payments applied to bills selected by bill date and collections received during a date range are different summaries and must be labelled separately.
- Edits, cancelled payments, returns and bill reversals recalculate linked balances atomically with audit history. Account for later repayments before reversing a bill; any resulting refund/customer credit must use an explicit supported resolution rather than orphaning payments or silently deleting money history.
- Customer rename/archive preserves all links. Outstanding customers remain visible and counted even when archived.

### Worked example

1. Customer A has a ₹1,000 bill, pays ₹600 and owes ₹400. Bill History shows that bill due ₹400; Accounts shows one customer owing ₹400.
2. Customer A receives another unpaid bill for ₹200. Accounts still shows one customer owing, now ₹600; Bill History shows two outstanding bills.
3. Customer B has an unpaid bill for ₹300. Accounts shows two customers owing a combined ₹900.
4. Customer A repays ₹500, explicitly allocated ₹400 to the first bill and ₹100 to the second. The first bill is paid, the second has ₹100 due, and Accounts shows A ₹100 plus B ₹300: two customers owing ₹400 total.
5. Customer A pays the remaining ₹100. Accounts shows one customer owing ₹300. No repayment changes completed bill count, original sales revenue or stock.

### Permissions, synchronization and recovery

- Owner sees all accepted bills. Billing-device history defaults to its own permitted bills; customer-account access and repayment actions must follow existing role permissions. Do not grant new broad financial access through a history link.
- Offline accepted sales and repayments use stable IDs and durable event handling. Retry must not duplicate debt, collections or bills.
- Disclose pending/unknown synchronization and incomplete Owner totals. Do not claim an offline device has zero pending bills without evidence.
- Backups and restores preserve customer IDs, bill IDs, original receipt numbers, payment IDs, allocations, opening balances, reversals and audit history. Restore must not repost payments or debt.

### Acceptance tests

| ID | Scenario | Expected |
|---|---|---|
| H01 | Complete the worked example above | Counts and balances match every step in both screens |
| H02 | One customer has several unpaid bills | Customer counted once; each bill retains correct due |
| H03 | Attempt unpaid Walk-in sale | Customer selection required before committing credit |
| H04 | Repay through Accounts, then open bill | Same repayment/allocations visible; due updated once |
| H05 | Repay through bill detail, then open Accounts | Same payment visible; customer total refreshed once |
| H06 | Change Bill History date filter | Selected-bill totals change; overall customer balance remains correctly labelled |
| H07 | Later-period repayment of earlier bill | Collection appears on payment date; original revenue/date unchanged |
| H08 | Opening balance plus bill debt | Both retained separately; no fabricated bills or double-counted debt |
| H09 | Reprint/share/reopen or retry save/sync | No extra bill, stock deduction, payment or debt |
| H10 | Return/cancel bill with later repayments | Explicit valid refund/credit handling; no orphan allocations |
| H11 | Rename/archive customer | History and outstanding totals retained |
| H12 | Held/cancelled bills | Excluded from active completed-sale/debt totals according to status; audit history retained |
| H13 | Customer credit and another customer's debt | Credit shown separately; positive debt total not understated |
| H14 | Restore isolated backup | Same bills, customer counts, balances and allocations without replay |
| H15 | Restricted device follows account link | Unauthorized data/actions remain blocked |

Include Bill History and linked Accounts screenshots, payment-allocation reconciliation and these test results in the release handoff. This section adds requirements; it does not certify implementation.

## 17. Latest navigation decision and unified Accounts approach

### Bill History placement - supersedes the earlier Home shortcut proposal

The user requests Bill History in the position currently occupied by the Settings shortcut below Cash In/Out. Replace that Home shortcut with Bill History, opening the full register with its period filters. Do not add a second Today's bills shortcut in another Home position as previously proposed. Keep a History action within Billing for convenience.

Retain access to Settings through the gear icon; this moves its Home shortcut, not its functionality. Verify the current Home layout before implementation, since the supplied screenshots do not show that location. Preserve Cash In/Out and bottom navigation. Test both the replacement shortcut and Settings gear, including back navigation and larger fonts.

### Accounts is the shared balance view, not another manual ledger

Use the same design on both sides:

| Activity | Canonical transaction | Accounts effect |
|---|---|---|
| Completed customer bill | Sale with customer ID and applied payments | Unpaid portion becomes customer due |
| Customer repayment | Payment with allocations to bills/opening debt | Reduces that customer's due once |
| Receive purchased stock | Purchase/receipt with supplier ID and applied payments | Unpaid portion becomes supplier due |
| Supplier payment | Payment with allocations to purchases/opening debt | Reduces that supplier's due once |
| Add customer or supplier | Directory record | No balance effect |
| Edit product details | Catalog change | No purchase, stock receipt or debt effect |

Accounts has two clear sections: Customers owe us and We owe suppliers. Each shows the number of distinct parties with positive dues and total positive dues, plus All / Outstanding views. Keep customer credits and supplier advances separate from positive amounts owed. Do not offset unrelated parties or customer and supplier balances automatically.

### Receiving stock versus editing a product

The existing Add Stock & Product flow may remain convenient, but it must clearly distinguish Create product / Receive stock from Edit product details. Purchase details belong to a receipt/purchase, not just a supplier-name field on the product.

- Select a supplier from the shared Accounts directory, or add one inline.
- Record received items, invoice reference, effective date, purchase amount, paid now, payment source and remaining due.
- Show a review summary before committing: stock received, payment made and supplier due created.
- Save purchase, stock movements, initial payment and supplier liability together, with repeat-save protection.
- Support a supplier invoice containing several product lines as one purchase where the existing workflow permits. Do not record the whole invoice liability separately for each product. Where entry is product-by-product, explicitly link lines to the intended purchase and prevent repeated invoice-level charges/payments. Do not guess invoice identity from invoice number alone across different suppliers.
- Later price/name edits never repost the original purchase or opening stock.
- Do not create supplier debt merely from choosing a supplier or importing a catalog supplier name.

Example: one supplier invoice has notebooks ₹600 and pens ₹400. Pay ₹700. Accounts must show one ₹1,000 purchase with ₹300 remaining due, not two ₹1,000 purchases or a second debt entered manually in Accounts. A later ₹300 settlement clears the due without adding stock or operating expense.

### One financial event, consistent views

Bills and purchases create the obligations. Payments and linked returns/reversals adjust them. Accounts, Bill History, stock-receipt history and Cash In/Out display the relevant views of those same transactions. Avoid independent editable totals on each screen; any performance caches must remain reconcilable to the canonical records.

For linked customer repayments and supplier settlements, opening an entry from Cash In/Out must use the same transaction/allocations as Accounts. Editing or cancellation must update its source balance and cash/bank effect atomically. Do not let a generic cash edit detach the payment from its bill/purchase. Existing Cash In/Out entry points for these purposes must select the party and show allocation rather than creating unrelated manual cash records.

Keep the accounting purposes distinct: customer collections are not new sales; supplier stock payments are not Shop Expenses; actual Shop Expenses use the shared expense system in Sections 9-12. Preserve explicitly confirmed opening balances, advances and history without inventing balancing records.

Additional acceptance checks: multi-line supplier invoice and partial settlement; duplicate invoice/save warning without guessing identity; receipt edit/reversal after supplier payment; no new liability after product-only edits; correct distinct supplier count; linked cash-payment edits update Accounts once; Home Bill History replaces the specified Settings shortcut while the gear still opens Settings.

## 18. Latest confirmation and complete integration-test mandate

The user confirmed the supplier explanation: create a supplier once, select that same supplier while receiving stock, record paid-now and remaining due, and settle the remainder through Accounts. Do not require duplicate manual entry of the debt. Both supplier creation entry points share the same directory. Fully paid purchases retain history with zero due.

The user requests thorough testing of ALL features together, especially the combined figures on Sales & Profit. This section expands the required implementation testing; it does not replace the earlier L, S, E and H acceptance cases. Run those cases AND this integration/regression suite against the final candidate build, fixing failures and rerunning affected flows.

### Honest status at document update

- Specification updated; no application tests were run by this Markdown update.
- The arithmetic of the synthetic end-to-end fixture below was independently checked with Python. This verifies expected numbers only, not app behavior.
- Every application scenario starts as NOT RUN. Prior screenshots, an earlier APK test or a successful build cannot be substituted for results from the changed candidate.
- Earlier documents may retain obsolete statements such as no app existing, deposits-only Pigmy, bilingual UI or scheduled cloud uploads. Do not reintroduce those requirements. Follow the latest decisions consolidated here and the latest approved feature scope.

## 19. Combined profit: compulsory financial reconciliation

### Never combine unlike totals under Gross Profit

Gross profit must remain a defined calculation, not the sum of cash received, outstanding balances, inventory potential and savings.

| Measure | Meaning | Exclusions / qualifications |
|---|---|---|
| Net product sales | Product bill revenue after discounts and linked returns | Not later repayments or all Cash In |
| Product COGS | Saved costs associated with those sold quantities, adjusted for valid reversals | Not every inventory purchase or supplier payment |
| Product gross profit | Net product sales minus product COGS | Operating expenses are shown separately |
| Net service revenue | Service revenue after discounts/returns | Not exact service profit |
| Net operating expenses | Products + Services + Shared expenses, less linked expense refunds | Count each canonical expense once |
| Combined recorded operating result | Product gross profit + net service revenue - net operating expenses | Qualified result based on recorded costs/expenses; not guaranteed complete accounting net profit |
| Product result after expenses | Product gross profit - net Products expenses | Excludes Shared expenses, clearly disclosed |
| Service result after recorded expenses | Net service revenue - net Services expenses | Excludes Shared expenses; supplies may span periods |
| Cash balance | Confirmed opening cash + actual cash inflows - actual cash outflows | Bank payments excluded; differs from profit |
| Customer dues / supplier dues | Outstanding obligations and settlements | Not amounts to add to profit |
| Expected stock profit | Projection on current unsold inventory | Never added to realized profit |
| Pigmy tracked balance | Confirmed opening savings + deposits - withdrawals | Neither sales nor operating profit |

If a combined overview is displayed, use a clear breakdown with product gross profit, service revenue, each expense classification, refunds and the resulting recorded operating result. Keep product-only and service-only results separately understandable. Show Shared expenses exactly once in the combined result. Do not sum a combined expense total and its category subtotals again.

Inspect the current ₹12,235 expense aggregation in the screenshot: list every contributing record with ID, purpose, classification, effective date and amount. Reconcile the list to the displayed total. Check specifically for purchases, supplier settlements, Pigmy transfers, duplicate service expenses and unrelated date ranges. Report any defect with evidence; do not assume the number is wrong solely because the loss is large.

### Mandatory invariants

1. Every displayed amount can be traced to contributing canonical records and exact rounding rules.
2. Summary cards, graphs, drill-downs and exports for the same scope/range reconcile to the paise.
3. Payments settle receivables/payables; they do not create another sale/purchase/expense.
4. Supplier purchases increase inventory; corresponding saved cost enters product COGS when sold. Do not deduct inventory cost twice.
5. Refunds retain their accounting type: sale refund reverses revenue; expense refund reduces expense; Pigmy withdrawal returns savings.
6. Zero-cost products must be distinguishable from unknown costs. Unknown costs produce incomplete labels, not false exact margins.
7. Original sale prices, discounts and cost snapshots survive later catalog edits and receipt regeneration.
8. Closed-period views use consistent effective dates. Creation/update audit times do not replace transaction dates.
9. Current customer/supplier balances and current stock are not silently presented as date-filtered period movement.
10. Unknown pending sync means potentially incomplete reports, not confirmed zero missing transactions.

## 20. Golden end-to-end scenario: exact expected results

Use a new disposable database, one Owner test device, no pre-existing records, and all following transactions in one selected test day. Confirm opening cash ₹10,000; bank balance, supplier/customer opening debt and savings openings are confirmed zero. These are test values, never production defaults.

Create Supplier A, Customer A, one product Notebook and a reusable photocopy service. Use integer paise internally. All payments below use shop cash. Add representative photos to at least one expense and one linked refund.

| Step | Action | Expected immediate effect |
|---|---|---|
| G01 | Add Supplier A in Accounts | Available in stock selector; no debt, expense or stock change |
| G02 | Receive 100 notebooks for ₹1,000; pay supplier ₹600 | 100 pieces; supplier due ₹400; cash ₹9,400 |
| G03 | Record ₹100 delivery paid to a separate transporter, capitalized into this receipt | Landed stock cost ₹1,100; unit cost ₹11; cash ₹9,300; supplier due still ₹400; no operating expense |
| G04 | Bill Customer A for 10 notebooks at ₹20 each; apply ₹20 whole-bill discount; collect ₹100 | Bill net ₹180; COGS ₹110; gross profit ₹70; stock 90; due ₹80; cash ₹9,400 |
| G05 | Separate service-only bill: 5 photocopies at newly entered ₹10 each, fully paid | Service revenue ₹50; product stock unchanged; cash ₹9,450; two completed bills total |
| G06 | Receive ₹30 repayment against Customer A's first bill through Accounts | Due ₹50; cash ₹9,480; sale count/revenue/COGS unchanged |
| G07 | Pay Supplier A remaining ₹400 through Accounts | Supplier due ₹0; cash ₹9,080; stock/operating expense unchanged |
| G08 | Cash Out Shop expense: electricity ₹120, Shared | Gross expenses ₹120; cash ₹8,960; same entry visible in Shop Expenses |
| G09 | From Shop Expenses add paper ₹80, Services | Gross expenses ₹200; cash ₹8,880; same movement visible in Cash In/Out |
| G10 | Cash Out Shop expense: shelving repair ₹40, Products | Gross expenses ₹240; cash ₹8,840 |
| G11 | Receive ₹20 explicit linked electricity expense refund | Refunds ₹20; net expenses ₹220; Shared net ₹100; cash ₹8,860 |
| G12 | Confirm ₹300 Pigmy deposit | Savings ₹300; cash ₹8,560; no expense or profit effect |
| G13 | Withdraw ₹100 Pigmy savings into shop cash | Savings ₹200; cash ₹8,660; no sales/profit effect |
| G14 | Return 2 saleable notebooks from G04, refund ₹36 in cash using explicit cash-refund option | Restore 2 pieces at saved ₹11 cost; sales reverse ₹36 and COGS ₹22; cash ₹8,624; customer remaining due stays ₹50 |

G14 uses an explicit cash refund of the returned items' net discounted value (₹18 each), not a debt-credit choice. The previously paid ₹130 covers that ₹36 refund. Test the alternative debt-reduction choice separately in a fresh branch of the fixture; do not apply both treatments to the same return.

### Final expected dashboard and ledger values

| Check | Expected |
|---|---:|
| Sellable notebooks | 92 pieces |
| Remaining notebook inventory cost | ₹1,012.00 |
| Net product sales | ₹144.00 |
| Product COGS | ₹88.00 |
| Product gross profit | ₹56.00 |
| Product gross margin | 38.89% when displayed to 2 decimals |
| Net service revenue | ₹50.00 |
| Combined net billed revenue | ₹194.00 |
| Gross recorded operating expenses | ₹240.00 |
| Linked expense refunds | ₹20.00 |
| Net operating expenses | ₹220.00 |
| Net Products expenses | ₹40.00 |
| Net Services expenses | ₹80.00 |
| Net Shared expenses | ₹100.00 |
| Product result after Products expenses | ₹16.00 |
| Service revenue minus Services expenses | -₹30.00 |
| Combined recorded operating result: ₹56 + ₹50 - ₹220 | -₹114.00 |
| Physical cash | ₹8,624.00 |
| Bank balance | ₹0.00 |
| Customers with positive due | 1 |
| Total customer due | ₹50.00 |
| Suppliers with positive due | 0 |
| Supplier A due | ₹0.00 |
| Pigmy tracked savings | ₹200.00 |
| Completed original bills | 2 |
| Linked sale return records | 1; not an extra completed sale |
| Net customer collections against those bills after sale refund | ₹144.00 |

Checks: ₹144 customer collections + ₹50 remaining due = ₹194 net billed revenue. Initial stock cost ₹1,100 - net COGS ₹88 = ₹1,012 remaining inventory. Product result ₹16 + service result -₹30 - Shared net expenses ₹100 = combined result -₹114.

Independent balance check for this deliberately simple fixture: cash ₹8,624 + inventory ₹1,012 + customer receivable ₹50 + Pigmy savings ₹200 - supplier payable ₹0 = ₹9,886, equal to opening ₹10,000 plus recorded result -₹114. This check applies to this fixture; it is not permission to infer complete accounting from incomplete real-world data.

Verify every result from UI, persisted records, relevant graph buckets and export. Restart the app and check again. Back up through the real UI, save/retrieve externally, restore into a second disposable installation and check all values/IDs/photos again. Do not replay the actions after restoration to reconstruct missing results.

## 21. Complete feature regression matrix

The tests below supplement all earlier cases. Use fresh fixtures or reset to a verified disposable snapshot between branches so one scenario does not invalidate another's expected values.

### Inventory, receiving and imports

| ID | Test | Required result |
|---|---|---|
| R-I01 | Create product, edit details, receive more, correct count | Each operation has its own effect; editing never re-adds stock |
| R-I02 | Receive 10 pieces at ₹10, then 10 at ₹20, with no intervening sale | 20 pieces, cost ₹300, weighted average ₹15; later sale uses saved applicable cost |
| R-I03 | Proportional transport on multiple lines with paise remainder | Allocations sum exactly to delivery charge; inventory and actual payees reconcile |
| R-I04 | Sell one pack of 10 priced ₹100 | Revenue ₹100, 10 pieces deducted; wholesale per-piece price not mistaken for pack price |
| R-I05 | Different pack sizes and barcode-unit mappings | Correct selected price/unit/quantity; archived mappings handled safely |
| R-I06 | No barcode, leading-zero barcode, long Urdu/mixed name | Search/select/import/export/receipt preserve exact identity and text |
| R-I07 | Low-stock defaults and overrides; archived product | Correct alerts and historical visibility; no erased balances |
| R-I08 | Damage, shop use, free item and donation | Stock decreases once; no sale/debt; known costs separately reported, unknown cost flagged |
| R-I09 | Reverse giveaway/damage; saleable versus damaged return | Correct stock destination once; no double subtraction/restoration |
| R-I10 | XLS/XLSX/CSV file picker, mapping and preview | Advertised formats work; unsupported fields disclosed; leading zeros and currency preserved |
| R-I11 | Cash ledger selected as product import | Explained rejection/routing; no fabricated stock |
| R-I12 | Repeat/overlapping import; legitimate similar rows | No duplicate opening stock; do not discard legitimate transactions based only on matching amounts |
| R-I13 | Corrupt/empty/wrong-type file; provider denial; interruption | Useful error, no partial silent changes; retained input and safe retry |
| R-I14 | Multi-product invoice, paid/part-paid/unpaid | Single correct purchase liability and transport/payment total; supplier history reconciles |
| R-I15 | New purchase after earlier sale and catalog-cost edit | Earlier bill cost snapshots/profit unchanged |

### Billing, payments, returns and receipts

| ID | Test | Required result |
|---|---|---|
| R-B01 | Product-only, service-only and mixed bills | Correct line types; stock deducted only for products |
| R-B02 | Products ₹160 + services ₹40, whole-bill 10% discount | ₹180 total; product revenue ₹144, service revenue ₹36; allocations exact |
| R-B03 | Amount/percentage discount, 0%, 100%, decimal rounding | Valid limits; exact line/bill reconciliation; discount not mistaken for unpaid debt |
| R-B04 | ₹48 bill, cash tender ₹50 | Applied ₹48, change ₹2; physical cash increases net ₹48 |
| R-B05 | ₹48 bill, tender ₹47.50 | Explicit remaining ₹0.50 handling; no unassigned debt or hidden discount |
| R-B06 | Cash/digital/credit split payment | Applied amounts sum to paid total; each destination correct; due preserved |
| R-B07 | Bill-only price override versus catalog-price edit | Override affects current bill only; historical bills unchanged |
| R-B08 | Multiple held bills, restart, resume and cancel draft | Durable drafts; no stock/debt/sales posting until completion |
| R-B09 | Double tap/retry at save and payment boundaries | One sale and exactly one set of financial/stock effects |
| R-B10 | Partial return, exchange and cancellation after repayment | Original links retained; explicit refund/debt-credit allocation; no orphan payment |
| R-B11 | History search/filter and cross-navigation to Accounts | Same persisted bill/customer identity, correct distinct customer count |
| R-B12 | Regenerate old receipt after catalog/customer changes | Original transaction number/date/item/price snapshots retained as required |
| R-B13 | Short/long/multi-page receipt, Urdu names and big amounts | No overlaps/crossing rules; correct font shaping, numeric alignment and pagination |
| R-B14 | Receipt reference 1 photocopy ₹2, discount ₹1 | Gross line/subtotal ₹2; discount ₹1; payable/paid ₹1; due ₹0 |
| R-B15 | Preview/PDF/print/share | Same correct selected bill; removed unwanted receipt statements remain absent |
| R-B16 | WhatsApp missing/cancelled; stale cached PDF; permission lifetime | Valid readable attachment when available; no saved-bill mutation; no claim of delivery from launch alone |
| R-B17 | Save PDF/printing unavailable or cancelled | Clear status and retry; no duplicate bill or lost receipt |

### Accounts, cash, expenses and services

| ID | Test | Required result |
|---|---|---|
| R-A01 | Same supplier created/selected from both entry points | One directory identity; no debt from creation alone |
| R-A02 | Multiple customers/suppliers, multiple bills/purchases each | Distinct positive-balance counts and totals correct; credits separate |
| R-A03 | Payment allocated across several obligations | Allocations sum to applied payment; no repeated full credit per bill |
| R-A04 | Opening balances with missing historical bills | Explicit reviewed amounts/provenance; no fabricated purchases/sales |
| R-A05 | Cash/bank source changed on linked payment | Old source reversed and new applied once; balances consistent |
| R-A06 | Backdate entry; edit date across day/month boundary | Correct period movement; audit date remains separate |
| R-A07 | Future-dated completed entry | Test only against an explicitly documented existing/confirmed policy; unresolved policy remains an open decision |
| R-A08 | Category create/rename/archive/filter | Stable links and totals; archived historical entries remain visible |
| R-A09 | Photo camera/gallery/replacement/removal/permission denial | Durable readable evidence; no amount change; form survives failure |
| R-A10 | Cash ledger Notes with blank category and malformed dates | Notes preserved; malformed dates not replaced by today; summaries not imported as transactions |
| R-A11 | Ordinary cash income and owner transfer | Cash changes by purpose; no automatic sales or expense reduction |
| R-A12 | Refund earlier expense in later month; reclassify original | Correct date-specific reduction and attribution; original cash not replayed |
| R-A13 | Service definition and new job | No saved/default rate; fresh job requires rate; current held job retains its rate |
| R-A14 | Switch A4 B&W to Colour | Entered rate cleared for fresh option; units explicit and correct |
| R-A15 | Service return and service expense | No product stock restored/consumed automatically; no duplicate service expense |
| R-A16 | Shared shop and service-specific costs | Classification clear; shared cost not arbitrarily allocated or subtracted twice |
| R-A17 | Empty/custom filtered export | Exact selected rows only; attachments honestly represented as indicators |

### Pigmy savings

| ID | Test | Required result |
|---|---|---|
| R-P01 | Add fourth/fifth scheme; rename/archive/reactivate | No fixed-three assumption; history/IDs remain intact |
| R-P02 | Deposits A ₹100, B ₹200, C ₹300; edit B ₹250; cancel C | Combined ₹600, then ₹650, then ₹350; linked cash effects once; no profit change |
| R-P03 | ₹10,000 savings; withdraw ₹3,000; edit to ₹2,500; cancel | Tracked balance ₹7,000, ₹7,500, ₹10,000; destination receipt adjusted/reversed once |
| R-P04 | Bank-funded deposit/bank-received withdrawal | Physical cash unchanged; bank/savings effects correct |
| R-P05 | Confirm historical opening savings | No new deposit/outflow; no invented history; correct opening/movement/closing totals |
| R-P06 | Withdrawal above tracked funds or incomplete history | Explained validation/review; no fabricated balancing deposit |
| R-P07 | Manual Pigmy category on generic entry | Does not create a hidden scheme transaction; user directed to linked workflow |
| R-P08 | Retry, restart and restore | No replayed cash transfers; no interest fields/income introduced |

### Reports, graphs and financial boundaries

| ID | Test | Required result |
|---|---|---|
| R-F01 | Golden scenario across all financial screens | All exact values in Section 20 match |
| R-F02 | Today/week/month/custom/all-time, midnight boundaries | Same effective dates, timezone and inclusive/exclusive boundary rules everywhere |
| R-F03 | Backdated sale/expense/refund after initial report | Correct affected buckets and summaries, no stale total |
| R-F04 | Zero sales, loss, returns exceeding current-period sales | Honest signed values; margin Not applicable when denominator is unsuitable |
| R-F05 | Missing costs, negative stock, pending sync | Explicit incompleteness; no misleading precise total or stock projection |
| R-F06 | Product/service/shared expense and refund drill-down | Every contributing record once; sum reconciles exactly |
| R-F07 | Daily manual summary plus underlying individual bills | No duplicate revenue; summary purpose documented before counting |
| R-F08 | Chart bar/point, filtered list and export | Same record scope and exact bucket total |
| R-F09 | Large history and range changes | Responsive screen; no stale asynchronous results from previous filter |
| R-F10 | Supplier/customer payment, Pigmy movement and catalog edit | No unintended change to recognized sales/gross profit |

### Two-phone operation, access and Android behavior

| ID | Test | Required result |
|---|---|---|
| R-D01 | Owner-approved pairing and revoked invitation/device | Correct role; no self-promotion or unauthorized financial data |
| R-D02 | Billing completes offline sale, then reconnects | Durable local bill; Owner accepts once; stock/debt/payment effects once |
| R-D03 | Drop connection before remote commit and after commit before acknowledgement | Retry safe; acknowledgement only for durable commit |
| R-D04 | Both devices sell last piece offline | Both legitimate bills retained; shortage flagged; no discarded sale |
| R-D05 | Unique receipt numbers through restart/replacement/restore | No collision or unintended renumbering |
| R-D06 | Delayed offline costing | Documented policy; no unsupported exact historical-profit claim |
| R-D07 | Screen off, battery restrictions, Wi-Fi/address change, restart | Recoverable connection; durable queues; honest status |
| R-D08 | Owner expenses/photos/costs through links/exports/sync | Restricted roles cannot retrieve unauthorized records |
| R-D09 | Biometric/device credentials, resume and lock timer | Existing access policy enforced without data loss |
| R-D10 | Navigation, back button, large fonts and keyboard | No dead shortcut, clipped action, lost form or inaccessible Settings |

Two real devices are required to mark the hardware synchronization/lifecycle tests passed. A single device or JVM simulation can support partial evidence only. Unresolved Billing permissions or offline costing rules must be listed and resolved before claiming the corresponding tests passed; do not silently broaden access.

### Backup, migration and failure recovery

| ID | Test | Required result |
|---|---|---|
| R-R01 | Backup Now while committed writes exist in WAL | Consistent complete snapshot; no missing recently committed records |
| R-R02 | Full archive manifest and attachment inspection | Bills, drafts, stock, parties, allocations, expenses/refunds, Pigmy, settings and branding covered |
| R-R03 | Save Copy to Drive/Files, retrieve and isolated restore | Verified data/IDs/photos match; local backup retained |
| R-R04 | Provider offline/cancelled/absent/revoked/full | Honest local versus provider-save status; no false cloud confirmation |
| R-R05 | Corrupt/unsafe-path/missing-content/unsupported-schema archive | Rejected before replacing current database; recoverable prior data |
| R-R06 | Interrupt save/restore or exhaust storage | No half-posted finances; old data/evidence recoverable |
| R-R07 | Upgrade supported old schema with real-looking linked history | No destructive fallback; IDs/balances/attachments preserved |
| R-R08 | Restore with pending sync events | Reconciliation by identity; no repeated bills/payments or role overwrite |
| R-R09 | Two dated backups, failed later attempt | Prior backups and successful timestamps retained accurately |
| R-R10 | Release-equivalent/minified build | Fonts/logo/camera/picker/URI/backup/lock tested on Android, not inferred from debug build |

## 22. Execution procedure, defect handling and evidence

### Before testing

1. Identify current source revision, local changes, application ID, version code, signing identity and candidate artifact hash. Preserve existing work and do not copy private signing material into reports or fixtures.
2. Create an isolated build/data environment. Keep test records, receipts, Drive folder and devices separate from production. Do not send synthetic receipts to real customers.
3. Record supported Android/device sizes, actual timezone, font size, locale, permission state and database schema version.
4. Prepare independent expected-value fixtures and record IDs. Do not derive the expected answer by calling the same app calculation under test.
5. Resolve documented policy gaps from existing source/accepted decisions where possible. Otherwise list the precise open question; mark dependent tests BLOCKED rather than inventing behavior.

### Test layers

- Calculation tests: exact paise, discount allocation, weighted costs, refund allocations, balance/count calculations and date bucket rules.
- Database/repository integration: transactions, foreign-key relationships, rollback, migrations, unique event IDs and single financial effects.
- UI/device flows: perform saves through real controls; inspect persisted outcomes and cross-screen refresh, accessibility and lifecycle behavior.
- End-to-end: execute the golden scenario, export, restart, externally save backup, restore elsewhere and reconcile the whole app.
- Release regression: rerun affected tests after fixes against the actual candidate. A tested debug APK does not automatically certify a different release artifact.

### Per-test result record

For every L/S/E/H/G/R test, record:

```text
Test ID and title:
Candidate version / source revision / APK SHA-256:
Device / Android / timezone / schema:
Preconditions and fixture snapshot:
Steps actually performed:
Expected values and records:
Actual values and records:
Status: NOT RUN | PASS | FAIL | BLOCKED | PARTIAL
Evidence: screenshots, exported rows, database checks, logs as applicable
Defect ID / exact blocking dependency:
Fix revision and rerun result:
```

PARTIAL is not PASS. Record the unverified part explicitly. Keep secrets and real customer evidence out of shared logs. Distinguish implementation defects from unavailable hardware or unresolved product decisions.

### Required final reconciliation report

Include a table with expected/actual/difference for cash, bank, each product quantity/cost, product revenue/COGS/gross profit, service revenue, expenses/refunds by classification, combined recorded result, each customer/supplier due, distinct debtor counts, bill count and Pigmy balances. Differences must be zero in exact monetary/quantity units, except documented display rounding of percentages.

Also include:

- UI before/after evidence for Pigmy, stock form, Accounts, Sales & Profit, Shop Expenses, Bill History and Home shortcut placement.
- Cross-links and payment-allocation evidence for customers and suppliers.
- Every contributor to the screenshot's expense total, with any correction explained.
- Graph-to-card-to-export reconciliation for several periods, including a loss and earlier-period refund.
- Golden fixture results before restart, after restart and after isolated restore.
- Complete test matrix with pass/fail/blocked/partial/not-run counts; no blanket “everything works” claim while required checks are unverified.
- Remaining issues, affected features, recovery steps and exact build identity.

### Release gate

Do not recommend production installation with unresolved data loss, duplicate postings, unexplained stock/money differences, unauthorized access, broken migration/restore or incorrect gross-profit aggregation. Fix and rerun the affected integration paths. Cosmetic-only issues should still be listed honestly rather than silently marked passed.

Producing this document is not test execution. Implementers must supply build-specific evidence before saying the features work together. Installing on the live shop phone remains a separate explicitly approved action; never clear data/uninstall to bypass a failure.

## 23. Latest addition: readable numbers with Indian digit grouping

The user reports that numbers lack commas and are difficult to read. Apply consistent Indian-style grouping across the app, receipts and appropriate exported display formats.

| Value | Display |
|---|---|
| 999 rupees | ₹999.00 |
| 1000 rupees | ₹1,000.00 |
| 12235 rupees | ₹12,235.00 |
| 100000 rupees | ₹1,00,000.00 |
| 1234567.50 rupees | ₹12,34,567.50 |
| Negative 12197.72 rupees | -₹12,197.72 |
| 123456 pieces | 1,23,456 pcs |

### Coverage and consistency

- Use a shared formatting policy in Home summaries, stock costs/prices/quantities, bills, receipts/PDF/print/share, Bill History, customer/supplier Accounts, Cash In/Out, Shop Expenses, Pigmy, graphs and report detail/export previews.
- Monetary display uses two decimal places consistently. Quantity precision follows the supported unit; integer pieces must not acquire meaningless decimal places.
- Use full grouped values for cards, details and chart tooltips. If a chart axis abbreviates lakhs/crores, label the scale and retain the exact amount in the tooltip/table. Do not truncate or abbreviate the only visible balance.
- Reflow long amounts at narrow widths/larger fonts. Commas must not introduce clipping or split a money amount across lines.
- Do not group receipt/invoice numbers, phone numbers, dates, barcodes, customer IDs or other identifiers. Preserve leading zeros.
- Formatting changes presentation only, not stored amounts, arithmetic, rounding or historical values. Continue exact paise calculations; never calculate using formatted text.
- Amount inputs must remain easy to edit. A plain editable number with a grouped amount preview is acceptable; if live grouping is used, preserve caret position and decimal entry. Correctly handle pasted valid grouped amounts and clearly reject malformed input without silently changing its value.
- Verify the actual Android/PDF formatting implementation produces Indian grouping. Do not assume a locale label alone guarantees the grouping pattern on every runtime.

### Excel numeric representation

- XLSX amount/quantity cells remain numeric, with a tested display format for Indian grouping. Do not export currency strings merely to obtain commas; SUM, sorting and formulas must work.
- Show the currency in column headings or valid numeric formatting. Dates remain real spreadsheet dates/times with a readable unambiguous display.
- Test representative small, lakh, crore, decimal and negative values in supported spreadsheet viewers. Where a viewer does not honor Indian grouping, disclose the limitation rather than corrupting numeric data.
- CSV cannot carry Excel cell formatting. Export machine-readable numeric values with proper CSV quoting where necessary; do not promise persistent visual commas in CSV. Preserve identifier columns as appropriate to the existing import/export contract.

## 24. Latest addition: filtered Cash In/Out Excel export and sharing

The user requests selecting a date range and categories when sharing the Cash In/Out spreadsheet. Add a clear Export / Share Excel action that opens a filter-and-preview screen or sheet. Recommend the following controls; reuse existing filters where possible rather than maintaining separate conflicting implementations.

### Export controls

| Control | Behavior |
|---|---|
| From date / To date | Calendar pickers; inclusive calendar dates; show actual selected range |
| Quick periods | Today, This Week, This Month, All Time and Custom |
| Direction | All, Cash In, Cash Out; follow existing ledger terminology for bank entries and label sources clearly |
| Categories | All categories or multi-select categories; include an explicit Uncategorized option for legacy entries |
| Payment source/destination | All supported sources, Shop cash, Bank/digital accounts as actually represented by the app |
| Transaction purpose | All or selected actual types: shop expense, linked expense refund, customer repayment, supplier payment, savings transfer and other supported types |
| Expense classification | All, Products, Services, Shared when expense/refund purposes apply; explain scope or hide irrelevant control |
| Status | Active entries by default; optional audit export includes cancelled/reversed entries clearly separated from active totals |

- Seed the export with the current Cash In/Out screen's filters; let the user adjust them before exporting. Show every active filter visibly.
- Category and transaction purpose remain distinct. Selecting a category called Pigmy does not create/reclassify a transfer, and selecting Shop expense purpose does not include supplier settlements.
- Selecting several categories uses OR within that selection; different filter groups combine with AND. Explain an incompatible/no-match combination with an empty state rather than silently relaxing filters.
- Do not require a new filter capability that exposes Owner-only records to Billing roles. Export permissions match underlying data permissions.

### Date correctness

- Filter by the transaction's effective date/time, not import time, creation time or latest edit time.
- Include the whole To date in the app's established business timezone. Use a start-inclusive / next-day-start-exclusive interval internally instead of an arbitrary 23:59:59 cutoff.
- Reject From later than To. Preserve date selections after validation errors and cancellation.
- When All Time is selected, show the actual covered dates or an explicit no-records state. Do not silently interpret an empty selection as All Time.
- Backdated records appear in their effective period. Refunds and repayments use their own effective dates, with references to the original transaction retained.

### Preview before file creation

Display:

1. Date range and active filters.
2. Number of matching active transactions.
3. Selected inflow total.
4. Selected outflow total.
5. Selected net movement = inflow minus outflow.
6. A preview list and category subtotals where useful.
7. Save Excel and Share Excel actions.

Use “Selected net movement”, not “Profit” or “Shop cash balance”. Filtered electricity spending does not establish the full shop cash balance. All-sources movement is not physical cash. If an opening/closing cash balance is shown, calculate it from the full relevant account ledger and label its scope separately; never derive it from category-filtered rows alone.

For a filtered running column, use “Cumulative selected movement”, starting from zero. Do not label it actual account Balance. Exclude supplier/customer due figures from cash movement unless an actual payment occurred.

No matches: show zero transactions and zero totals, with Change filters. Do not export all transactions instead. An empty workbook may be offered explicitly, but sharing it must not look like a successful export of records that do not exist.

### Workbook contents

Use a real XLSX workbook with:

- **Summary**: export generation time, effective date range/timezone, selected categories/purposes/sources/classifications/status, active transaction count, inflow/outflow/net movement and category subtotals. State that this is a filtered ledger export, not a complete backup or profit statement.
- **Transactions**: stable transaction ID, effective date/time, direction, purpose, category, classification where applicable, description/notes, source/destination, party/scheme where applicable, original bill/purchase/expense reference where available, inflow, outflow, status and attachment-present indicator.
- Optional **Audit** sheet if cancelled/reversed entries were requested. Avoid adding cancelled rows as ordinary active cash movements. Follow the canonical reversal model so neither original nor reversal is counted twice.

Keep Notes separate from Category. Preserve Urdu/mixed-script text, leading-zero identifiers and original receipt references. Store user text as text, not spreadsheet formulas; safely handle notes/names beginning with formula-like characters in supported export formats.

Use readable headers, frozen header row, column filters, useful widths, wrapped notes and numeric currency cells. Long notes must remain accessible without making every column excessively wide. Include category subtotals even when the selection is All categories, provided they reconcile to the selected rows.

Photos are not embedded by default. Show attachment presence without private local filesystem paths. State clearly that the spreadsheet does not contain the original evidence images or constitute a restorable backup.

Summary and Transactions must come from the same consistent snapshot. Concurrent saves during generation must not produce a summary for one set of rows and a sheet for another.

### Save and share behavior

- Use a meaningful dated filename, for example `Ali_Educational_Cash_2026-11-01_to_2026-11-30.xlsx`. Keep detailed filters inside the workbook instead of putting private party names or an excessively long category list in the filename.
- Generate and validate the workbook before opening the share chooser. Provide a readable content URI with the correct XLSX MIME type and temporary read permission.
- Keep the generated file available long enough for the recipient app to read it. The user chooses/confirms the recipient.
- Cancelled sharing, missing recipient app, provider failure, low storage or generation interruption leaves all underlying transactions untouched and offers a useful retry.
- Opening WhatsApp/chooser does not prove delivery. Saving through Files and sharing are separate statuses.
- Work offline for local generation. Do not require a cloud account to produce the workbook.

## 25. Additional formatting and filtered-export acceptance tests

| ID | Scenario | Expected |
|---|---|---|
| N01 | 999, 1000, 100000, 1234567.50, zero and negative amounts | Exact Indian-grouped display and two currency decimals across screens/receipts |
| N02 | Large amounts with larger fonts/narrow screen | No clipped digits, lost minus sign or wrapped partial amount |
| N03 | Phone, receipt number, barcode with leading zeros | No numeric grouping or identity change |
| N04 | Type/edit/paste valid grouped amount | Correct exact value; stable editing; invalid text rejected clearly |
| N05 | Export grouped currency to XLSX | Numeric cells remain summable/sortable and show correct display in tested viewers |
| N06 | Grouping after restart/restore and receipt regeneration | Same amount everywhere; no stored-value migration or rounding drift |
| X01 | From/To inclusive date boundary | Includes midnight on From and final instant of To; excludes next day |
| X02 | Same-day range, leap-day/month boundary, backdated entry | Correct effective-date selection and totals |
| X03 | Invalid range and cancelled date picker | Clear validation; prior form/filter values retained |
| X04 | One category, multiple categories, Uncategorized, All | Correct OR-within-category/AND-between-group behavior; archived history retained |
| X05 | Direction/source/purpose/classification combination | Only matching authorized entries; expense refunds and transfers retain their types |
| X06 | No matching rows | Zero preview; no fallback to exporting all data |
| X07 | Preview versus Summary versus Transactions | Same count/inflow/outflow/net movement to the paise |
| X08 | Category-only export | Net movement not labelled profit or full account balance |
| X09 | Active versus audit export with cancellation/reversal | Active totals correct; cancelled audit history clearly separate; no double counting |
| X10 | Notes/Urdu/leading-zero IDs/formula-like text | Preserved safely as intended data; no accidental executable formula |
| X11 | Save/share/retry/cancel/missing app/offline/low storage | Valid XLSX when successful; truthful state; no transaction mutation or duplication |
| X12 | Large filtered history and concurrent new save | Responsive generation; one consistent snapshot and reconciled summary |
| X13 | Attachment indicator and restricted role export | No private paths/photos or unauthorized Owner information leaked |
| X14 | Reopen shared workbook in supported mobile spreadsheet app | Correct sheet contents, numeric types, date display, grouping and usable column layout |

### Independent export fixture

On disposable data, record:

| Effective date | Direction | Category | Amount | Purpose |
|---|---|---|---:|---|
| 1 Nov 2026 | Out | Electricity | ₹1,000.00 | Shared shop expense |
| 2 Nov 2026 | Out | Paper | ₹200.00 | Services shop expense |
| 3 Nov 2026 | In | Electricity | ₹100.00 | Explicit refund linked to the 1 Nov expense |
| 4 Nov 2026 | In | Other | ₹500.00 | Ordinary non-expense income, with actual supported purpose |
| 5 Nov 2026 | Out | Electricity | ₹300.00 | Shared shop expense |

Select 1-3 November and Electricity: 2 rows, inflow ₹100, outflow ₹1,000, selected net movement -₹900. Select 1-3 November and Electricity + Paper: 3 rows, inflow ₹100, outflow ₹1,200, net -₹1,100. Select 1-3 November, Electricity and Out only: 1 row, outflow ₹1,000, net -₹1,000. The 4/5 November rows must never appear in these exports. The refund remains an inflow linked to its expense; do not rewrite the original expense to ₹900 and also export the ₹100 refund.

Run these cases along with all earlier tests. Add workbook samples, verified numeric cell types and actual preview-to-file reconciliation to the release evidence. This update remains a specification, not a claim that app formatting/export changes have been implemented.
