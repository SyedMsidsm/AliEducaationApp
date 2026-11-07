# Ali Educational — Service Billing Specification

Status: Approved feature and visual direction; implementation specification.  
Scope: Service selection, reusable service creation, job entry, mixed bills, receipts, service revenue reporting, and owner-only service expense recording. Updated 31 October 2026.

## 1. Purpose

Allow the shop to bill printing and related services quickly, either alone or alongside products in one bill. Keep service entries separate from inventory products. Enter a rate for every job; do not save default service prices.

## 2. Approved service list

The Services section initially contains:

1. Printing
2. Photocopying
3. Sticker printing
4. **+ Add service**

Do not add other predefined services. The owner can create additional reusable services using Add service.

Printing has these quick options:

- **A4 B&W**
- **A4 Colour**

These options specify the job type, not its price. Do not automatically add further paper sizes or service options.

## 3. Approved visual design

Use the app's black/charcoal background, warm-white text, and gold highlights/buttons. Keep the interface simple, with readable labels and large touch targets.

### Screen A — New bill / Services

- Heading: **New bill**.
- Two tabs: **Products** and **Services**.
- Selecting Services shows Printing, Photocopying, Sticker printing, and Add service as large rows with simple icons.
- Newly saved services appear in this list and remain available for future bills.
- Show the current bill beneath the selection area, with products and services together.
- Each bill line shows its name, quantity, rate, and total when expanded or edited.
- Bottom action: **View bill · ₹[total]**.
- Switching tabs must preserve the current bill.

### Screen B — Enter a service job

- Heading shows the selected service, such as **Printing**.
- Printing shows A4 B&W / A4 Colour option buttons.
- **Quantity** field with its unit clearly identified.
- **Rate per [unit] (₹)** field, initially blank for each new job.
- Helper text: **Enter rate for this job.**
- Calculated **Service total**.
- Primary action: **Add to bill**.
- Back returns to service selection without adding an incomplete line.

The approved preview illustrates quantity as printed sides for printing. Do not confuse a physical sheet with a printed side: two-sided printing may represent two chargeable sides. The shop's precise charging unit must be unambiguous on the form and receipt. Photocopying and sticker-printing unit labels should be confirmed rather than silently inferred.

### Screen C — Add reusable service

- Heading: **Add service**.
- Required field: **Service name**.
- Field: **Unit label**, such as Print, Copy, Sheet, or Job.
- Helper text: **Saved for future bills. Enter a rate each time.**
- Primary action: **Save service**.
- There is no default-rate field.

Saving creates a reusable service definition. It does not add a charge to the current bill automatically. After saving, return to the service list with the new service available to select.

The preview's “Photo printing” entry is an example of a user-created service, not another predefined option.

## 4. Rate and quantity behavior

- Never prefill a new service line with a default or last-used rate.
- Enter quantity and rate for each job.
- Calculate the total immediately after valid values are entered.
- Formula: **quantity × rate = service line total**.
- Editing an existing bill line retains that line's entered values so they can be corrected.
- Reopening a held bill retains the entered rates; this is saved bill data, not a service default.
- Changing the selected printing option should clear a previously entered rate to avoid accidentally charging the B&W rate for colour or vice versa.
- Use consistent currency rounding and money handling with the rest of billing.
- Do not permit a blank or negative rate. An intentional free service, if supported by the existing billing policy, must be explicitly selected rather than inferred from a missing price.
- Require a positive quantity. Define unit-specific quantity rules; printed sides/copies must not silently accept meaningless fractional counts.
- Show validation beside the relevant field and preserve the user's other entries.

## 5. Reusable services

A reusable service stores its identity, name, and unit label. It does not store a selling rate, stock quantity, purchase cost, barcode, or reorder threshold.

Recommended safeguards:

- Trim names and reject empty names.
- Warn about an existing service with the same name rather than silently creating confusing duplicates.
- Keep historical bill-line names, units, and prices unchanged if the reusable service is renamed later.
- If removal is supported, archive services referenced by completed bills so receipt history remains intact.

Permission to create or edit reusable services on a restricted billing device must follow the app's agreed permission policy; the visible Add service design does not grant unrestricted settings access.

## 6. Mixed bills and checkout

Products and services can appear on the same bill. They use the existing checkout workflow for discounts, payment, customer credit, and receipt creation.

Example only — all rates below are entered for this bill:

| Item | Quantity | Rate | Line total |
|---|---:|---:|---:|
| Notebook | 2 | ₹80 | ₹160 |
| Printing — A4 B&W | 20 printed sides | ₹2 | ₹40 |
| **Subtotal** | | | **₹200** |

A 10% whole-bill discount is ₹20, making the final payable ₹180. Apply the discount once using the existing whole-bill discount rules. Do not create a second service-specific discount workflow.

- Add, edit, or remove a service line before completing the bill.
- Keep distinct jobs as distinct lines when their option, rate, or unit differs.
- Prevent repeated taps on Add to bill from unintentionally adding duplicate lines.
- Support service-only bills without requiring a product.
- Do not change the meaning of discount versus unpaid customer credit.

## 7. Inventory behavior

Service lines do not deduct product stock. A mixed bill deducts stock only for its product lines.

Do not automatically consume paper, ink, toner, or sticker sheets in this feature. Consumables tracking would require a separately designed mapping and is outside this specification.

Services must not appear as low-stock items or contribute to inventory valuation. Reversing a service charge must not add units to product stock.

## 8. Receipts and reporting

Receipts should show service name, printing option if applicable, quantity, unit, entered rate, and line total. They should share the same discount/payment summary as product lines and follow the app's existing receipt language selection.

Show product sales and service revenue separately in reports while keeping a combined bill total. For a whole-bill discount, allocate the discount proportionally across lines with rounding that sums exactly to the bill discount, so category totals reconcile.

For the ₹200 mixed-bill example with ₹20 discount:

- Product net revenue: ₹144.
- Service net revenue: ₹36.
- Combined net revenue: ₹180.

Service revenue is not automatically service profit. Show recorded service expenses alongside revenue as specified below, but do not call their difference exact profit. Per-job costing, consumables usage costing, and automatic allocation of supplies consumed remain outside this feature.

## 9. Minimal implementation requirements

Use a distinct service line type within the existing bill model. Preserve completed line snapshots containing:

- Reusable service ID.
- Service name at time of billing.
- Selected option, if any.
- Quantity and unit label.
- Entered rate and line subtotal.
- Allocated whole-bill discount where used for reporting.
- Final net line value.

Keep reusable service definitions separate from bill-specific rates. Route service lines through existing receipt, history, and transaction handling without applying inventory movements. Existing offline saving and bill transfer must preserve service lines and their entered values.

## 10. Acceptance tests

1. Open Services and verify exactly the three initial service types plus Add service.
2. Select Printing and verify A4 B&W and A4 Colour options.
3. Verify the rate is blank on a new job; enter 20 sides at ₹2 and receive ₹40 total.
4. Add the service, start another job, and verify the previous rate is not reused.
5. Change B&W to Colour and confirm the old rate is cleared.
6. Create a reusable service with a name and unit; restart the app and verify it remains available without a saved price.
7. Reject empty names, invalid quantities, blank/negative rates, and handle duplicate names clearly.
8. Add products and services to one bill; verify subtotal, whole-bill discount, payments, and credit.
9. Complete the ₹200/10% example and verify ₹180 payable, ₹144 product revenue, and ₹36 service revenue.
10. Confirm only product lines reduce stock; service-only bills leave inventory unchanged.
11. Edit/remove a draft service line and hold/resume a bill without losing entered values.
12. Inspect receipts in both supported languages for name, option, unit, rate, and correct totals.
13. Rename/archive a reusable service and verify historical receipts remain unchanged.
14. Verify service revenue is not reported as fully earned profit when costs are unknown.
15. Verify repeated taps and reopening a saved bill do not duplicate charges; existing bill synchronization preserves service lines exactly once.

## 11. Boundaries and remaining detail

This feature does not include document upload, editing print files, controlling a physical printer, tracking print-job production status, automatic consumable deductions, or additional predefined service types.

The main detail still to confirm is the quantity unit for photocopying and sticker printing, and whether the printing preview's per-printed-side convention matches shop practice. Keep unit labels explicit and do not assume sheets, sides, and stickers are interchangeable.

The approved image is a design preview. Example prices are illustrative entries and must not become defaults in the implementation.

## 12. Approved expansion — service revenue and expenses

Provide a **Services** tab within **Sales & Profit** on the owner phone. It combines service revenue and recorded service expenses in one view without slowing checkout.

Confirmed decisions:

- Record revenue from completed service bill lines automatically.
- Enter service expenses separately, only on the owner phone.
- Do not ask for estimated cost on each printing job.
- Show a combined services overview with breakdowns by service underneath.
- Keep shared service expenses in a separate bucket rather than guessing allocations.
- Record each expense once and reuse it in both Services and the overall expense report.
- Continue entering selling rates for each job; expense recording does not create default rates.

### Owner-only permissions

Only the Owner role may create, edit, void, attach evidence to, or export service expense records and access the service financial overview. The billing phone continues billing services but cannot access service expense administration or reports.

Enforce restrictions in the underlying commands/data access and incoming synchronization validation, not merely by hiding buttons. Do not send expense records or their attachments to the billing phone. Reject unauthorized expense writes with an explicit error without applying any change. This permission is separate from the still-open question of creating reusable service definitions on the billing phone.

## 13. Services report layout

Keep the black-and-gold style and simple labels. On the owner phone, open **Sales & Profit → Services**.

### Date controls

- Today, This week, This month, and Custom dates.
- Display the selected start/end dates; use the shop timezone consistently.
- Use Monday as the proposed week start and show actual dates so the interpretation is visible.
- Support daily, weekly, or monthly grouping for the graph, independently of the date range.
- Use the selected range for cards, charts, breakdowns, transaction lists, and exports.

### Summary cards

| Card | Definition |
|---|---|
| Net service revenue | Completed service charges minus allocated discounts and service sales reversals in the selected period. Includes credit sales. |
| Recorded service expenses | Non-voided service expense entries dated within the selected period. |
| Revenue minus recorded expenses | Net service revenue minus recorded service expenses; may be positive, zero, or negative. |

Show this explanation next to the third figure: **“Not exact service profit. Supplies may be used across months, and some sales may be unpaid.”** Do not label it cash available, cash flow, take-home income, or net profit.

Show **“Service profit is not calculated”** where necessary to prevent the user mistaking revenue for profit. Do not combine this comparison with product gross profit into an apparently exact overall profit figure.

### Graph and breakdowns

- One readable graph with Revenue / Expenses / Both controls; distinguish series by labels as well as color.
- Provide an accessible numeric alternative and clear values when selecting a period.
- Show revenue for Printing, Photocopying, Sticker printing, and each reusable service with activity.
- Within Printing, allow an A4 B&W / A4 Colour breakdown where those options were recorded.
- Show expenses assigned to individual services and a separate **Shared services** group.
- Show expense categories such as Paper, Ink/Toner, Sticker sheets, Machine repair/maintenance, and Other.
- Do not claim per-service profit by allocating shared expenses without an agreed allocation method.
- Tap a revenue total to open its bill lines; tap an expense total to open its expense entries. Preserve the selected period and filters.
- Empty periods should say no activity and show zero, not an error. A negative comparison is valid and should not be hidden.

### Completeness and exports

Show when service revenue may be incomplete due to pending bills from the second phone, synchronization errors, or historical sales lacking line details. If the second phone is unreachable, do not claim its pending count is known to be zero.

Offer PDF and spreadsheet export using the same filters, labels, amounts, warning text, generation time, and date range. Exports must retain the “not exact service profit” explanation. Totals must reconcile to the on-screen report.

## 14. Owner service expense entry

Provide **Add service expense** on the owner Services report and allow the existing general expense form to classify an entry as a service expense. Both routes save to the same underlying expense ledger.

| Field | Behavior |
|---|---|
| Category | Required: Paper, Ink/Toner, Sticker sheets, Machine repair/maintenance, or Other. |
| Amount | Required positive rupee amount; use existing currency precision. |
| Expense date | Required, default today; editable for previously incurred expenses. |
| Applies to | Required: Printing, Photocopying, Sticker printing, a reusable service, or Shared services. |
| Note | Optional description, supplier reference, or explanation. |
| Photo/document | Optional receipt or invoice attachment. |

Example: Ink/Toner, ₹1,200, expense date 31 October, Shared services, note “Used for printing and photocopying.” This is one expense, not ₹1,200 against each service.

Use the existing payment workflow consistently if expenses already support paid/unpaid status. Payment date and expense date are different concepts: paying an existing expense later must not create another expense. Do not introduce automatic cash-register deductions, supplier due changes, or a new payment model through this feature without a separately defined integration.

### Validation and save behavior

- Reject missing category, target, date, or amount, invalid numbers, and negative/zero amounts.
- Save exactly once despite repeated taps; preserve form values after recoverable errors.
- Do not show success until the record is durably saved.
- Reject unsupported/unreadable attachments with a useful message. Do not silently lose the amount or other entered fields.
- Permit saving without an attachment by an explicit choice if attachment processing fails.
- Warn on a likely duplicate (matching date, amount, category, and target) but allow intentional separate expenses after review.
- Record creation/update timestamps, stable expense ID, and origin device.

### Editing and removal

- Show Edit and Void in the owner expense detail actions.
- Require a reason when voiding a posted expense; retain its original amount, date, and history.
- Editing updates the same record and records before/after values; it must not append an unintended duplicate expense.
- Voided entries are excluded from active totals but available in history with a clear status.
- Changing the date moves the expense to the correct reporting period; changing its target/category moves the breakdown without changing overall total.
- Archiving a service does not delete its expenses or historical revenue.

## 15. Financial rules and reconciliation

### Revenue recognition

Include service charges when the bill is completed, whether paid immediately or sold on credit. Exclude draft/held bills. Later repayment settles credit and must not add service revenue again.

Store discounts allocated to bill lines in paise with deterministic rounding so line net amounts sum exactly to the final bill total. Refunds/reversals use the original line's net charge and already-refunded quantity/amount to avoid over-reversing revenue.

Report a later service cancellation/return as a linked sales reversal on its effective date, preserving the original bill date and amount. A cash refund payment is not a second revenue deduction if the sale reversal has already reduced revenue. Returning a product from a mixed bill must not reduce service revenue.

### Expense recognition and no double counting

Use the expense's entered effective date in the report, not its attachment date or later payment date. The Services report is a filtered view of the existing expense ledger, not a second ledger.

- A ₹500 service expense appears as ₹500 in Services and contributes ₹500 exactly once to overall expenses.
- Shared services expense is included once in the combined services total and stays in its shared bucket.
- An expense reclassified from General to Services remains one record; overall expenses do not increase.
- Do not record the same inventory purchase as a service expense merely to make it appear in this report. A supply acquired for service use needs a clear expense classification, avoiding duplicate purchase/cost entries.
- Buying supplies does not establish how much was consumed. This is why the revenue-minus-expenses figure remains a comparison, not exact profit.

### Worked report example

For one selected period:

| Entry | Amount |
|---|---:|
| Printing net revenue | ₹1,000 |
| Photocopying net revenue | ₹600 |
| Sticker printing net revenue | ₹400 |
| Total net service revenue | ₹2,000 |
| Printing paper expense | ₹300 |
| Shared ink/toner expense | ₹500 |
| Sticker sheets expense | ₹100 |
| Total recorded service expenses | ₹900 |
| Revenue minus recorded expenses | ₹1,100 |

Label ₹1,100 as the comparison described above. If ₹400 of those sales is unpaid, revenue remains ₹2,000 and credit remains ₹400; do not imply ₹2,000 was collected. If unrelated shop expenses are ₹700, overall expenses are ₹1,600, not ₹2,500.

## 16. Data integrity, offline use, and backup

- Owner expense creation and reports work from local stored data without internet.
- Save expense records and audit history atomically. An interruption must leave either a saved record or an unsaved draft/error, not a partial counted record.
- Expense IDs remain stable across exports, backup, restore, and any retry.
- Include service definitions, service bill snapshots, expense records, audit history, and attachments in backup/recovery coverage, with explicit exclusions if any.
- Receiving a delayed billing-phone service bill updates the appropriate sales period using its recorded effective date and unique ID. Display sync completeness separately.
- Do not distribute service expenses to the billing phone as part of service catalog synchronization.
- Use integer minor currency units or equivalent exact decimal handling; do not accumulate binary floating-point errors in totals.

## 17. Detailed report and expense test plan

These are required future tests, not claims of tests already run. Use an isolated dataset and test both roles. Preserve expected values, actual results, build version, and evidence for each case.

| ID | Scenario | Expected result |
|---|---|---|
| SE01 | Run the worked ₹2,000 revenue / ₹900 expense example | Cards show ₹2,000, ₹900, ₹1,100 with the non-profit explanation; service and category breakdowns reconcile. |
| SE02 | Add unrelated expenses ₹700 | Overall expenses ₹1,600; service expenses remain ₹900. |
| SE03 | Mixed bill: products ₹160, services ₹40, whole-bill discount 10% | Net product revenue ₹144, service revenue ₹36, combined ₹180. |
| SE04 | Uneven discount allocation across three lines | Rounded allocations sum exactly to the bill discount; no missing/extra paise. |
| SE05 | Complete ₹100 service sale with ₹60 paid and ₹40 credit | Service revenue ₹100, collection ₹60, debt ₹40; no implied ₹100 cash collection. |
| SE06 | Repay that ₹40 next period | Debt becomes zero; repayment is collection, not another ₹40 of service revenue. |
| SE07 | Hold a service bill without completing | No report revenue; resume retains entered rate. |
| SE08 | Reverse/refund ₹20 of a service sale | Net service revenue decreases once by ₹20 on reversal date; refund payment does not deduct revenue again; product stock unchanged. |
| SE09 | Return only the product from a mixed bill | Product figures adjust; service revenue remains unchanged. |
| SE10 | Add ₹500 Shared services toner expense | Service expense total increases by ₹500 once; no automatic split across services. |
| SE11 | Reclassify an existing ₹500 general expense as Services | Overall expense total unchanged; service expense total increases ₹500. |
| SE12 | Edit expense ₹300 to ₹250 | Totals decrease ₹50; same record ID with before/after history. |
| SE13 | Void ₹100 expense with reason | Active totals decrease ₹100 once; voided record and reason remain in history. |
| SE14 | Move an expense into another date range | It leaves the old range and enters the new range exactly once. |
| SE15 | Change service target or category | Only breakdown attribution changes; combined expenses unchanged. |
| SE16 | Submit blank, zero, negative, malformed amount or missing required fields | Specific validation; no record created; valid fields retained. |
| SE17 | Double-tap Save, retry after delayed response, restart during save | At most one committed expense; recoverable state without partial totals. |
| SE18 | Enter a likely duplicate | Warning shown; cancel leaves totals unchanged; intentional confirmation creates one additional distinct record. |
| SE19 | Add/replace attachment, then simulate unreadable file or storage failure | Clear error; no false success; saved expense and attachment state remain coherent. |
| SE20 | Try expense routes/commands as Billing role, including forged incoming event | Access denied; no mutation or sensitive expense data delivered. |
| SE21 | Archive or rename reusable service with past entries | Historical bill snapshots retained; expenses remain attributable and counted. |
| SE22 | No activity; then expenses with zero revenue | Zero empty state first; later negative comparison displayed correctly, not hidden. |
| SE23 | Midnight, week/month boundary, and custom dates in shop timezone | Records appear in exactly the intended period; UI states range and grouping clearly. |
| SE24 | Switch daily/weekly/monthly chart grouping | Period total unchanged; buckets sum to cards; graph and accessible values agree. |
| SE25 | PDF/spreadsheet export after filtering | Export dates, rows, totals, completeness warnings, and non-profit explanation match screen. |
| SE26 | Billing phone has unsynced service bill, then reconnects and retries | Incomplete/unknown status before receipt; correct period updates once after receipt. |
| SE27 | Record an expense offline and restart owner phone | Entry persists once; reports work locally. |
| SE28 | Back up and restore into isolated installation | Definitions, service sales, expenses, history, and included attachments reconcile exactly. |
| SE29 | English/Urdu, long service names, large text, negative comparison | Readable layout and valid direction/currency formatting; actions remain reachable. |
| SE30 | Dataset with many service bills and expenses | Filtering, scrolling, and graph changes remain responsive; establish measurable performance targets on actual owner phone before release. |

### Release criteria

- Existing service-billing tests in Section 10 and the report/expense tests above pass.
- No unauthorized billing-phone expense access, duplicate expense counting, or repayment counted as revenue.
- Revenue, discount, reversal, and expense figures reconcile to source records and exports.
- No screen or export presents service revenue or the comparison figure as exact service profit.
- Backup recovery and offline persistence are verified on isolated data.
- Owner can complete expense entry, inspect supporting records, and correct/void an entry without developer assistance.

## 18. Updated scope boundary

Service expense recording is now included and restricted to the owner phone. Per-job cost estimation, consumable usage tracking, depreciation, exact service profit, and automatic shared-cost allocation remain excluded. The service quantity-unit clarifications in Section 11 still apply. This document specifies the feature and tests; it does not claim implementation or test completion.
