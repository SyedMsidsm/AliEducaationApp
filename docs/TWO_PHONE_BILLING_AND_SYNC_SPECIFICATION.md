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
