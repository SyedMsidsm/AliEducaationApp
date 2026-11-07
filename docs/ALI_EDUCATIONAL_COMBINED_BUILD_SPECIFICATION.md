# Ali Educational — Combined Implementation Handoff

Date: 31 October 2026
Scope: Two-phone billing and synchronization, service billing, and owner-only service revenue/expense reporting, integrated into the existing Android app.

## Read this first — instructions for the implementing developer

Extend the existing app; do not replace it with a fresh prototype. This document contains the FULL two-phone specification and the FULL service specification below, followed by a preservation and release checklist. The source documents have been retained intact. No feature or test section has been omitted.

These are requirements and planned tests, not evidence that implementation or testing is complete. No document can guarantee zero bugs; release requires demonstrated data preservation and regression results.

### Authoritative decisions and precedence

Use the following latest decisions if an older paragraph below calls an already-resolved point provisional:

1. Build ONE shared Android app and ONE signed release APK for two phones, using Owner and Billing device roles. Do not create two separate business-logic implementations or install two production copies on the owner's phone.
2. Both phones use the same shop Wi-Fi. Remote sync and a desktop server are outside this phase. Hotspot support is optional device-tested compatibility, not a replacement requirement.
3. Offline billing is approved: persist bills locally and send them when connectivity returns. Delivery retries must have exactly-once EFFECTS through deduplication; network delivery itself may repeat.
4. The billing phone may override item prices on the current bill and apply whole-bill discounts. It may not edit saved catalog prices.
5. Initial services are Printing, Photocopying, and Sticker printing, plus Add service for reusable definitions. Printing options are A4 B&W and A4 Colour. No default or last-used service rates.
6. Only the OWNER phone records, edits, voids, or accesses service expenses and the service financial overview. This overrides any general permission wording elsewhere in the combined document.
7. Service revenue, recorded service expenses, and their difference are shown separately. The difference is NOT exact service profit or cash available.
8. Local fingerprint/device credentials or PIN protection remains the approach. Device pairing does not require separate online logins.
9. Existing main-app behavior must remain functional when sync is disabled or the billing phone is absent.
10. The latest approved decisions above take precedence; then the service-specific rules apply to service features and the two-phone rules apply to shared transport/roles. Ask about unresolved conflicts rather than silently inventing permissions.

### Unresolved choices — do not block independent work

Confirm one question at a time before implementing the affected policy: photocopy/sticker quantity units; customer creation/credit/repayment permissions on the billing device; return/cancellation permissions; reusable-service creation permission on the billing device; historical bill visibility; final receipt series; delayed offline-sale costing; and backup schedule/destination details. Same-Wi-Fi operation, offline billing, price overrides/discounts, and owner-only service expenses are already settled and must not be asked again.

### Existing app must be identified first

The audit observed installed package `com.alieducational.app` v1.0.0, whereas the previously inspected workspace built `com.alieducational.inventory` v0.2.2-stock. Locate the matching current source, inspect installed package/build identity, and compare signing certificates before deploying an update. Do not assume this workspace is the production source.

Do not uninstall the active app, clear its storage, replace its database, generate dummy transactions in active shop data, or install an incompatible build to bypass an update failure. Existing defects in backup verification, import crash handling, costing/profit, zero prices, Urdu, navigation, and receipt preferences must remain visible in the work plan rather than being masked by the new features.

## Part A — Complete two-phone specification

# Ali Educational — Two-Phone Billing and Sync Specification

Date: 31 October 2026  
Status: One shared app with Owner/Billing device roles, same-Wi-Fi operation, and offline billing approved. Detailed billing permissions remain to be confirmed.

## 1. Purpose and recommendation

Provide a full shop-management experience on the main Android phone and a restricted billing experience on a second Android phone. Keep the current main-app repair work independent of this expansion.

Recommended approach: one shared application codebase and data model, with Owner and Billing device roles. The same signed app can present different screens according to its paired role. Two independent applications with separately implemented billing, costing, and storage would increase the risk of inconsistent behavior and duplicate maintenance.

This is a suitable approach for two phones working together in one shop, subject to testing local connectivity and Android background behavior. It is not yet a finalized choice for phones that must synchronize from different locations. Local-only sync cannot deliver changes while the phones cannot reach each other.

## 2. Confirmed needs and open assumptions

### Latest owner decisions

- One shared app with two device roles is approved.
- Both phones will operate on the same Wi-Fi. Remote synchronization is outside the current scope.
- Offline billing is approved: the billing phone must save completed bills durably when the main phone is off or unreachable, show a pending-sync count, and automatically deliver them exactly once when connectivity returns. Pending bills must survive app/device restart. Stock shown while disconnected is last-known stock; combined reports and main-phone backups remain incomplete until pending bills arrive.
- Roles do not require separate online accounts. Proposed setup: initialize the main phone as Owner, then authorize/pair the second phone as Billing from the main phone.
- Each phone can retain local fingerprint/device-credential or PIN protection. A Billing device must not be able to promote itself to Owner through an unprotected role selector.
- Separate named user accounts are not part of this change unless requested later.
- Billing-phone price overrides and whole-bill discounts are approved. An override changes the item price on that bill only; it does not edit the catalog price. Preserve original catalog price, overridden unit price, discount, final total, and origin device in history. Apply the whole-bill discount after item price overrides; do not count an override again as a discount. Keep discount-versus-unpaid-credit selection explicit.

Confirmed needs:

- Main phone manages all shop records and can also perform billing.
- Second phone provides billing access and sends its bills to the main phone.
- Relevant stock, product, price, payment, and customer-credit changes stay consistent.
- Records are backed up and recoverable.
- No desktop is required for this phase.
- Existing preferences favor keeping active data on owned devices and avoiding monthly fees. Google Drive was accepted as a backup destination.

Provisional implementation baseline, not yet approved:

- Both phones normally share shop Wi-Fi or a hotspot.
- Billing can continue during disconnection, with explicit pending-sync status.
- The main phone coordinates accepted shared records; the billing phone durably retains its own pending transactions.
- One billing phone is paired initially. Design identifiers to allow future expansion without implementing extra devices now.

Do not promise remote live sync, always-on background sync, or conflict-free offline stock until the corresponding requirements and device tests are complete.

## 3. Protect ongoing main-app development

### Independent delivery tracks

**Track A — main app reliability:** Continue the existing audit fixes: backup validation, Excel crash handling, stock and profit calculations, zero-price checks, Urdu, receipt defaults, record actions, navigation, and reports. These fixes must work with a single phone and without any sync service.

**Track B — shared billing foundations:** Define transaction identifiers, immutable completed-sale snapshots, database migrations, record history, and shared calculation functions. Introduce changes through backward-compatible migrations. Do not rewrite the entire application to add sync.

**Track C — optional two-phone capability:** Add pairing, roles, transport, pending queues, acknowledgements, reconciliation, and sync status behind a disabled-by-default capability. Activate it only after isolated testing.

### Rules preventing interference

- Locate the source matching the installed app before implementation. The audit found a package/version mismatch in the current workspace.
- Preserve the existing main-phone application ID and signing identity for upgrades unless a separately approved migration is necessary.
- Use one source of truth for billing calculations, discounts, units, receipts, and transaction validation.
- Keep device-role UI separate from shared business rules. Hiding a button alone is not a permission boundary.
- Main-app launch, billing, stock, and backup must not depend on a paired phone or an available network.
- Isolate network work from the UI thread and database transactions; network failure must not freeze checkout.
- Add database schema versions and migration tests. Never reset the database as an upgrade strategy.
- Keep sync disabled for existing installations until the owner explicitly pairs a device.
- Give both tracks stable interface contracts. A screen redesign must not require rewriting the sync protocol.
- Release and regression-test single-phone behavior before enabling the second phone.
- Disabling new pairing must not discard queued bills. An emergency stop must preserve data and offer recovery/export.

## 4. Device roles and proposed permissions

| Capability | Owner/Main phone | Billing phone |
|---|---|---|
| Sell and scan products | Yes | Yes |
| Search catalog and view selling prices | Yes | Yes |
| View available quantity | Yes | Yes, with last-sync timestamp |
| Receive stock, edit costs, adjust inventory | Yes | No by default |
| View profit, supplier dues, expenses | Yes | No by default |
| Change app-wide settings and pair devices | Yes | No |
| Full backup and restore | Yes | No |
| Reprint/share own completed bills | Yes | Proposed yes |
| Override item price on a bill or apply whole-bill discount | Yes | Approved; preserve original and effective prices in history |
| Edit saved catalog prices | Yes | No |
| Create customers or sell on credit | Yes | To confirm |
| Receive old customer repayments | Yes | To confirm |
| Return, exchange, cancel, reverse | Yes | Owner approval or permission; to confirm |
| View other device's historical bills | Yes | To confirm |

Only send the billing device data needed for its approved role. Avoid distributing supplier balances, purchase costs, profit figures, security settings, or full backups merely because they exist on the main phone.

Use the existing local fingerprint/device-credential or PIN protection approach. Pairing establishes device identity; it does not require introducing an online account. A device label identifies where a transaction originated, not necessarily which person used it.

## 5. User experience

### Main phone

- Keep the full main app and approved Home Settings gear.
- Add Settings → Connected billing device.
- Show device name, connection, last successful exchange, pending/error status, permissions, and revoke action.
- Show incoming bills in normal sale history with an unobtrusive origin-device label.
- Show unresolved sync conflicts in a clear review list, without blocking unrelated local work.

### Billing phone

- Start at New Bill with scan, search, categories, favorites/recent products, quantity, and cart.
- Show a compact status: Connected, Pending sync: N bills, Disconnected, or Needs attention.
- Keep pending bills visible in local history. Explain that they are saved on this phone but have not reached the main phone.
- Provide Sync now and a readable error/retry route.
- Clearly mark stock quantities as last known when disconnected.
- Provide only permitted settings such as language, appearance, local lock, printer/share defaults, and connection details.

Avoid technical protocol language in the product UI. Never say “backed up” solely because a bill was saved locally or synchronized.

## 6. Local connection and pairing

Proposed transport: local Wi-Fi or a hotspot, without requiring internet for billing and exchange. Wi-Fi client isolation, hotspot behavior, changing addresses, battery restrictions, and Android service limitations must be tested on both actual devices.

Pairing sequence:

1. Owner selects Add billing phone from Settings.
2. Main phone creates a short-lived, single-use pairing invitation, represented by a QR code or equivalent verified flow.
3. Billing phone scans it and displays the main shop/device identity.
4. Main phone confirms the new device and permissions.
5. Devices establish authenticated, encrypted communication and store device credentials securely.
6. Main phone sends a role-filtered initial catalog snapshot with a revision marker.
7. Billing phone verifies and commits that snapshot, then requests subsequent changes.
8. Show Ready only after initial synchronization succeeds.

Do not transmit private keys or permanent credentials in ordinary logs. Do not expose an unauthenticated local API. Device revocation must reject future requests. An offline revoked device cannot receive revocation instantly; explain that limitation and define how its pending records will be reviewed rather than silently lost.

## 7. Transaction synchronization contract

### Completing a bill on either phone

Within one local database transaction:

- Validate permitted actions, quantities, amounts, and required customer details.
- Assign a unique transaction ID and origin-device ID.
- Save item, quantity, unit conversion, selling-price, discount, payment, and credit snapshots.
- Save local stock effects and an outbound synchronization event together.
- Mark the bill saved locally and pending delivery where appropriate.

Do not require a network acknowledgement to preserve a completed local sale. A failure to save locally must not be displayed as a completed bill.

### Delivering a billing-phone transaction

1. Billing phone sends pending events in defined order.
2. Main phone authenticates the device, checks permissions and payload validity, and identifies duplicates by transaction/event IDs.
3. Main phone applies the accepted transaction, stock movements, payment/credit effects, and receipt history atomically.
4. Main phone returns an acknowledgement only after durable commit.
5. Billing phone records the acknowledgement durably and clears the pending indicator.
6. Main phone distributes relevant updated catalog/stock/account state.

If a reply is lost, resend the same event with the same identifier. The main phone must return the existing result without applying it twice. Do not generate a new bill simply because a transfer timed out.

### Stock reconciliation

Transmit transactions and stock movements, not blind replacements of absolute quantities. The billing phone must distinguish acknowledged movements from locally pending movements when showing projected stock. Receiving an updated quantity must not deduct its own sale for a second time.

Use revisions/cursors and a consistent snapshot boundary so changes made during initial synchronization are not missed. Handle history gaps through a fresh reconciled snapshot, preserving pending local transactions.

## 8. Identifiers, numbering, and time

- Use globally unique internal identifiers for records and events; do not rely on phone timestamps for uniqueness.
- Maintain a stable registered device identity and per-device ordering mechanism.
- Choose an invoice-number scheme that cannot collide offline, such as owner prefix plus device series and local sequence. Example only: `AE-M-000123` and `AE-B1-000045`; exact presentation requires agreement.
- Do not renumber a shared/printed completed receipt after synchronization.
- Preserve original sale time, local timezone context, main-phone receipt time, and origin device. Detect clock differences; do not sort accounting logic solely by untrusted device time.
- Define behavior after device replacement or restoring old backups so invoice sequences and event IDs cannot be reused.

## 9. Offline and conflict policies

| Situation | Required behavior |
|---|---|
| Main phone is absent/off | Billing may continue if approved; bills stay pending locally. No claim of live synchronization. |
| Both devices sell the final units | Preserve real completed sales, flag resulting negative stock, and request reconciliation. Never silently discard a sale. |
| Owner changes price while billing phone is offline | Existing completed bills keep their sale-time price; refreshed prices apply to new sales according to the agreed policy. |
| Owner archives a product sold offline | Keep the submitted sale and flag if review is needed; do not lose the transaction. |
| Invalid or unauthorized incoming transaction | Retain it with a visible actionable error/review state; avoid silent discard or endless hidden retries. |
| Duplicate customer created offline | Use unique IDs and an owner-reviewed merge workflow. Do not merge solely because names match. |
| Cancellation or return from another device | Link a new reversal/return to the original transaction, validate remaining refundable quantities, and prevent double reversal. |
| Duplicate message/reconnection | Return the recorded result without repeating effects. |

Offline availability and perfectly current cross-phone stock cannot both be guaranteed while disconnected. Show this honestly in the interface.

## 10. Pricing, delivery cost, and profit

Retain the approved proportional delivery allocation and weighted average costing policy. Billing phones need selling prices; they do not need unrestricted purchase-cost access.

The main phone must determine and preserve cost snapshots for accepted sales under a documented ordering policy. A delayed offline sale can arrive after a purchase or price change. Define whether cost is assigned on acceptance or reconstructed using validated transaction order, and disclose unresolved costing. Do not silently claim historically exact profit using whichever average happens to exist when a delayed event arrives.

Keep discount versus remaining credit explicit. A ₹100 bill paid at ₹90 must distinguish ₹10 discount from ₹10 debt. Repayments must not become new sales. Returns, cancellations, and exchanges must update inventory, payment/credit records, and profit consistently across devices.

## 11. Backup and recovery

Sync and backup are separate functions. The main phone creates the combined shop backup after accepted transactions are durably stored. Preserve existing dated backups and support verified export to the owner's chosen destination; Google Drive is a backup option, not the live transaction database.

Backup UI must show:

- Actual location or explanation of private app storage.
- File name, meaningful size, date, integrity status, and included data.
- Last backup separately from last sync.
- A warning when known billing-phone transactions are pending. If a device is unreachable, state that its current pending count is unknown.
- View backups, export/share, and restore actions.

The billing phone must durably retain unsynced transactions. Decide whether to provide an encrypted emergency export of pending bills; do not imply that a main-phone backup covers them. Loss of the billing phone before transmission can lose records unless a separate copy was made.

Restore must be tested in isolation. A restore onto the main phone requires a controlled reconciliation procedure with paired devices: old acknowledgements and queued events must not duplicate or erase transactions. Preserve transaction identities and introduce a documented recovery generation/epoch or equivalent protocol. Never restore one phone's entire database blindly onto the other role.

## 12. Suggested component boundaries

- **Shared domain:** money/rounding, quantities, discounts, sale validation, transaction effects, and receipt data.
- **Local persistence:** versioned database, atomic commits, stock ledger, immutable completed records, outbox/inbox and acknowledgement state.
- **Owner features:** purchasing, inventory administration, accounts, reporting, backup and device management.
- **Billing features:** checkout, scanner, permitted customer actions, local bill history and receipt actions.
- **Sync protocol:** versioned messages, identifiers, authentication, cursors, deduplication and reconciliation.
- **Transport:** discovery, local connection, reconnect/retry, Android lifecycle integration.

These are logical boundaries, not a requirement to adopt a particular framework. Select implementation details after inspecting the matching project. Reject incompatible protocol versions clearly; do not silently process data with an unsupported schema.

## 13. Phased implementation and release gates

### Phase 1 — stabilize the existing app

Resolve data-loss/crash risks and validate backup recovery, billing, stock/costing, and upgrade migrations. Existing single-phone tests must pass. No paired device is needed.

### Phase 2 — build sync-ready foundations

Introduce stable IDs, device attribution, transactional outbox, versioned contracts, and role-aware boundaries while sync remains disabled. Verify current billing still behaves correctly.

### Phase 3 — pair and share catalog

Implement authenticated pairing, role-filtered initial snapshot, incremental updates, revocation, and visible connection status. Test on both actual phones with Wi-Fi and hotspot.

### Phase 4 — synchronize completed bills

Implement durable delivery, acknowledgements, duplicate prevention, stock reconciliation, credit/payment effects, and unique receipt numbering. Test failures before and after every commit/acknowledgement boundary.

### Phase 5 — offline conflicts and recovery

Test simultaneous sales, pending queues, changed products/prices, invalid events, delayed costs, returns, backup/restore, device loss and replacement. Resolve policy questions before enabling affected features.

### Phase 6 — controlled rollout

Use isolated test data first, then a small supervised pilot with verified backups. Compare daily totals on both phones and inspect pending queues. Broaden use only when accepted bills reconcile and no known release-blocking failures remain.

## 14. Mandatory acceptance scenarios

1. Main phone works normally with sync disabled and no second device.
2. Pair only an approved device; reject unauthorized requests and wrong permissions.
3. Sync a catalog of approximately 3,000 changing products without freezing checkout.
4. Start with 20 notebooks, sell 2 on the billing phone, and show 18 on both after acknowledgement. Main phone receives exactly one bill and one stock deduction.
5. Drop the acknowledgement and retry repeatedly; totals remain unchanged after the first commit.
6. Disconnect, complete multiple bills, restart the billing phone, reconnect, and receive every bill exactly once.
7. Sell the last stock on both disconnected phones; retain bills and flag shortage.
8. Test discounts, cash/digital split payments, credit, subsequent repayments, and refunds with matching totals.
9. Update prices/archive products while offline sales exist; preserve historical bill snapshots.
10. Interrupt initial sync, catalog update, local save, incoming commit, and backup. Recover without silent partial records.
11. Test screen-off, app background, battery saver, locked phones, hotspot changes, and address changes. Show honest status if background delivery is delayed.
12. Test Urdu/English independently on each phone and verify receipt output.
13. Export and restore a backup into an isolated environment, then reconcile pending and acknowledged events without duplication.
14. Upgrade one phone before the other; handle supported and unsupported protocol versions safely.
15. Replace or revoke a billing phone without reusing identities or invoice sequences.

## 15. Questions to resolve one at a time

Price overrides for the current bill and whole-bill discounts are approved for the billing phone. Other permission questions below remain open.

1. Resolved: both phones will use the same Wi-Fi; remote sync is outside the current scope.
2. Resolved: billing must continue when the main phone is off or unavailable, with durable pending bills and later synchronization.
3. Which actions may the billing phone perform: discounts, price overrides, credit, customer creation, repayments, returns and cancellations?
4. Should the billing phone see only its own bills or all shop bills?
5. Is a device-specific receipt series acceptable?
6. What is the preferred backup destination and schedule, and should pending bills have an emergency export?
7. Which actual phone models and Android versions will be used for connectivity testing?

Do not block unrelated main-app fixes while these decisions are pending. Do not implement dependent policies as if they were already approved.

## 16. Relationship to existing plans

This document expands Section 10 of `ALI_EDUCATIONAL_APP_AUDIT_AND_FIX_PLAN.md`. Existing audit findings and fixes remain required. Optional AI remains outside this phase. The confirmed Home Settings gear, non-GST receipts, Urdu support, Excel import, delivery allocation, weighted average cost, and other agreed main-app requirements continue to apply.

This specification describes planned behavior, not completed implementation or a guarantee that every Android device can provide uninterrupted local background hosting.

## 17. Price/discount and two-device verification examples

Run these on isolated test data using two separate Android installations. Prefer both actual phones for Wi-Fi, camera, battery, lock, and restart tests. An emulator can assist development but cannot establish real-phone connectivity reliability.

- Catalog notebook price ₹80, stock 20: override to ₹75 and sell 2. Bill total ₹150, stock 18, catalog price still ₹80 on both phones after sync.
- Without an override, sell 2 at ₹80 and apply 10% whole-bill discount: subtotal ₹160, discount ₹16, payable ₹144.
- Combine a ₹75 unit override for 2 items with 10% whole-bill discount: subtotal ₹150, discount ₹15, payable ₹135. Preserve ₹80 original unit price, ₹75 effective unit price and ₹15 explicit discount; do not double-count the override.
- If test cost is ₹55 per item, the combined example has cost of goods sold ₹110 and gross profit ₹25, excluding shop expenses.
- Enter ₹90 received against a ₹100 bill: discount choice settles at ₹90 with ₹10/10% discount; credit choice retains ₹100 sale value and ₹10 due.
- Verify the receipt, local history, synchronized main-phone bill, stock ledger, payments/credit, and profit agree for each example.
- Disconnect after pairing, save bills, restart the billing phone, reconnect, and verify durable pending records arrive exactly once. Repeat with main-phone restart and a lost acknowledgement.
- Submit simultaneous sales of the last units and verify both bills survive with an explicit shortage warning.
- Test invalid negative prices, excessive discounts, rounding, explicit free-item handling, repeated Save taps, receipt-sharing cancellation, and permissions preventing catalog edits from the billing phone.
- Verify pending bills are clearly excluded from a main-phone backup until received; test backup restoration only in an isolated installation.

Record expected values, actual values, build versions, device identities, and pass/fail evidence. These are future acceptance tests, not tests already executed.


## Part B — Complete service billing and reporting specification

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

## Part C — Preservation, integration, and deployment requirements

### C1. Baseline and safe development

- Record the current source revision, app/build identity, signing certificate, schema version, and enabled features before changes.
- Preserve uncommitted user work. Use an isolated development branch/checkpoint and separate test app/device data where possible.
- Establish a baseline for existing screens and transactions so pre-existing defects are distinguished from new regressions.
- Capture a verified backup and restore it into an isolated installation before upgrading active data. A 0 KB label or success toast is not proof of a valid backup.
- Use representative anonymized/test copies for destructive, reversal, restore, and migration tests. Do not send real receipts during testing.
- Keep signing keystore files/passwords and device pairing secrets out of source control, shared project ZIPs, logs, and this document. Reuse the compatible existing release key; do not generate a replacement as an update workaround.

### C2. Schema and accounting integration

- Add versioned, tested database migrations. Never use destructive migration fallback or a reset to resolve schema changes.
- Existing product-only bills retain their meaning, original prices, costs, discounts, dates, receipt numbers, payments, and history. Explicitly migrate their line type to Product if needed.
- Introduce service lines without creating stock movements or default rates. Do not reinterpret old products as services based on names.
- Extend the existing expense ledger with classification/target fields. Do not copy records into a second service-expense table that totals independently.
- Existing expenses should keep their original category/classification unless the owner explicitly reclassifies them. Missing old classification is not permission to infer service expenditure.
- Preserve every existing identifier and relationship. Introduce global identifiers or mappings without breaking historical references.
- Currency rounding, discounts, payment allocation, credit, refunds, and returns must share tested rules across both device roles.
- Keep product gross profit separate from service revenue; do not assign zero service cost and then present revenue as profit.
- Do not assume customer credit collections are new revenue or that expense payment creates another expense.

### C3. Ordered delivery with independent gates

1. Inspect matching source and establish backup/recovery and regression baseline.
2. Stabilize known data-loss, crash, and financial correctness risks needed for this work.
3. Implement service definitions and service bill lines in single-phone mode; pass mixed-bill and existing product-billing tests.
4. Implement owner expense classification and Services reporting; pass no-double-counting and permissions tests.
5. Add transaction IDs/outbox and protocol foundations without enabling pairing; re-run single-phone regressions.
6. Add secure pairing, catalog sharing, bill transfer, acknowledgements, and conflict visibility behind explicit activation.
7. Run all specified two-device, interruption, migration, and backup/recovery tests.
8. Produce a compatible signed release APK and release notes; conduct an isolated upgrade rehearsal before active-device rollout.

A phase may ship independently only when its dependencies and tests pass. Do not require the second phone to exist before the owner can use services or record expenses.

### C4. Existing-app regression matrix

| Existing capability | Required preservation evidence |
|---|---|
| Launch and local lock | Old settings retained; biometric/PIN flow, timeout, restart and resume work. |
| Product catalog and variants | Existing names, identifiers, barcode mappings, quantities and piece/box conversions survive migration. |
| Product-only billing | Prices, overrides, discounts, payment/credit, receipts and one-time stock deduction still reconcile. |
| Scanner/manual selection | Existing scan and no-barcode workflows remain available; service screens do not replace them. |
| Purchasing and stock receiving | Supplier dues, proportional freight allocation and weighted average cost retain correct results. |
| Accounts and repayments | Old balances/history preserved; settlement does not count as new revenue. |
| Returns/cancellations | Original transaction references retained; reversal affects stock/money/profit once. |
| Excel import | Valid changing shop files still import; wrong/malformed files report errors without crash or partial silent changes. |
| Expenses | Original entries counted once; reclassification changes service breakdown only. |
| Reports and exports | Historical totals reconcile before/after migration except explicitly documented defect corrections. |
| Urdu and receipts | Both languages retain usable layout, numbers, fonts and output; selected preferences persist. |
| Backup/restore | Old supported backups handled through a tested compatibility path; new backups contain required service/sync data. |
| Single-phone offline mode | All established owner workflows remain available without pairing/network. |

Record counts, balances, stock totals, selected historical receipts, and attachment availability before and after migration. Test upgrade from every supported existing schema, interrupted upgrade behavior, and sufficient/insufficient storage.

### C5. Failure handling and rollback

- Feature switches can stop new pairing or disable unfinished UI; they cannot erase committed records or queues.
- Preserve and expose pending transactions if sync is disabled. Do not mark them acknowledged merely to clear an error.
- Do not treat installing an older APK as a safe rollback after a schema upgrade. Older versions may not understand new records.
- Prefer a tested forward fix. A rollback/restore procedure needs a compatible build, verified backup, and reconciliation of all records created after that backup.
- Never restore a main-phone backup over active data without an explicit, reviewed recovery action and accounting for pending second-phone bills.
- Keep failures actionable: what failed, whether data was saved, and how to retry/recover without duplicating it.

### C6. Test execution and evidence

Run ALL tests in Parts A and B, plus the regression matrix above. Use known sample values and assert actual database/report outcomes, not screenshots alone. Test single-phone, two-phone connected, and disconnected operation separately.

Use two real Android phones for final Wi-Fi, camera, lifecycle, lock, battery saver, and reconnect testing. An emulator is useful during development but is not sufficient proof of real-device reliability. If only one phone is available, clearly list which tests remain unverified and do not call the two-phone release fully tested.

For each test record: test ID, expected/actual values, build versions, devices/Android versions, pass/fail, and supporting evidence. Repeat failed/dependent tests after fixes. Include no-double-counting, no unauthorized expense access, and no data-loss failures as release blockers.

### C7. Build and distribution

- Deliver one signed release APK for both phones, preserving application ID and signing compatibility and incrementing the version code.
- Main phone is Owner; second phone pairs as Billing. Do not expose an unprotected switch that lets Billing promote itself to Owner.
- Send the APK by a user-selected file-sharing method. The brother receives the APK, not the signing keystore or passwords.
- Install compatible updates over the existing app; do not ask the user to uninstall as a routine update step.
- Provide short setup/pairing instructions, backup/file-location guidance, update instructions, and honest limitations for unsynced bills/background connectivity.
- Deliver implementation status, test results, migration evidence, known remaining issues, and release notes alongside the APK.

### C8. Completion definition

The work is complete only when the approved feature set functions, all applicable acceptance/regression tests pass, existing data is verified preserved through upgrade, permissions are enforced, and any untested device behavior is explicitly disclosed. Do not present a design preview, mocked sync indicator, or unexecuted test list as completed implementation.
