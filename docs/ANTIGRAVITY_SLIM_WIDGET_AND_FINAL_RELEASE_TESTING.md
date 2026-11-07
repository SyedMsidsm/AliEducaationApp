# Ali Educational: slim-bar widget and final release verification

Date: 1 November 2026

## 1. Scope and status

Implement the user's selected **Slim bar** Android home-screen widget, then rigorously test the whole existing app and fix errors before proposing it as final.

This is a standalone handoff. It does not repeat or add the earlier layout, graphs, supplier, Bill History or spreadsheet feature-change requests. Existing implemented and separately approved features are regression-test scope, not authorization for unrelated redesigns.

This document specifies work and acceptance criteria. It does not claim the widget exists, tests have passed or the app is ready for production.

## 2. Selected widget: Slim bar

Use one compact, wide Android launcher widget with a small Ali Educational heading and four equally spaced actions in one horizontal row:

```text
Ali Educational

   [incoming]   [outgoing]   [receipt +]   [box +]
     Cash In      Cash Out     New Bill    Add Stock
```

The illustrated icons above describe their meaning; use crisp, locally bundled assets consistent with the app.

### Appearance

- Match the existing dark charcoal/deep green background, rounded corners, gold accents and readable light labels.
- Cash In: green incoming arrow; Cash Out: orange outgoing arrow; New Bill: gold receipt icon; Add Stock: gold/light box icon.
- Keep the exact labels **Cash In**, **Cash Out**, **New Bill**, **Add Stock**. Do not relabel them Income/Expense: customer repayments, supplier settlements and savings transfers have different accounting meanings.
- Use four equal action areas, icon above label, generous touch targets and consistent alignment.
- Keep the widget compact; no balances, charts, customer information or extra shortcuts.
- Support an appropriate wide launcher span, typically four columns, while measuring the actual available space. Launcher cell dimensions vary; do not promise one universal physical size.
- Set a supported minimum width/height so labels remain readable. Prevent unsupported shrinking rather than clipping text or silently removing an action. Reflow is not a substitute for the user's selected four-action slim layout.
- Test supported resizing, display scaling and accessibility font sizes. Never split Cash Out or Add Stock into broken word fragments.
- Provide clear accessible action descriptions and sufficient contrast. Do not rely solely on icon color.

## 3. Shortcut behavior

| Action | Destination | Required behavior |
|---|---|---|
| Cash In | Existing new Cash In form | Incoming direction preselected; no amount or transaction automatically saved |
| Cash Out | Existing new Cash Out form | Outgoing direction preselected; existing purpose/category workflow retained |
| New Bill | Existing billing flow | Start/resume through the established draft-safe workflow; never discard an unfinished bill |
| Add Stock | Existing stock receiving/add stock flow | Existing product/supplier/purchase validation retained; no stock automatically added |

- These are shortcuts into the same app screens, commands, database and permission checks. Do not create a second widget-specific accounting or billing implementation.
- Tapping any action opens a form; it does not post money, save a bill, change stock or create debt.
- Keep ordinary Save/Confirm/Cancel behavior. Cancelling the form leaves records unchanged.
- Cash In/Out must retain the distinction between sales collections, repayments, supplier settlements, shop expenses, refunds and savings transfers supported by the app.
- If a draft or partially entered form already exists, preserve it and use the existing hold/resume/new-work flow. Repeated widget taps must not stack duplicate screens or overwrite input.
- Back navigation should behave predictably and must not reopen the same action endlessly.
- Tapping the heading may open the app Home screen, if implemented; it must not trigger a financial action.

## 4. Lock, permissions and lifecycle

- If the app requires authentication, unlock first and then continue to the intended action. Authentication cancellation must not open the protected form or commit anything.
- Validate role permissions inside the destination screen/command. Widget navigation is not a permission bypass.
- Owner has the four shortcuts. On a restricted device, retain clear role-appropriate behavior: unavailable actions may show a locked state and explanation, but must not reveal Owner data or grant rights. Do not broaden Billing-role permissions for this widget.
- Handle cold start, warm start, background app, process death, device restart and app update.
- Preserve the intended action through authentication/recreation without replaying a Save command.
- Use distinct, correctly routed action identities so Cash In cannot accidentally open Cash Out or another shortcut.
- Validate externally supplied navigation arguments. Do not accept arbitrary amounts, customer IDs or privileged operations as trusted merely because an action resembles a widget request.
- Support multiple widget instances without shared navigation collisions.
- The widget is a launcher shortcut surface: local actions should not require an internet connection. It does not need polling, scheduled financial refresh or background uploads.
- After an app update, existing widget instances must remain functional or recover gracefully with a clear refresh/setup path.

## 5. Widget acceptance tests

| ID | Scenario | Expected |
|---|---|---|
| W01 | Add widget from launcher picker | Named Ali Educational; slim layout with four correct labels/icons |
| W02 | Tap each action from terminated app | Correct destination and direction; no automatic record creation |
| W03 | Tap each action from warm/background app | Correct screen without duplicated navigation or lost input |
| W04 | Locked app, successful authentication | Selected destination opens after unlock |
| W05 | Cancel/fail authentication | Protected destination/data unavailable; no financial change |
| W06 | Existing bill/form, then widget action | Draft/input preserved; no silent replacement |
| W07 | Rapid repeated taps | No duplicate bill/form save or financial posting |
| W08 | Save then return/back/reopen | One committed transaction; no repeated submission |
| W09 | Cancel each opened form | No stock, money, account or bill-count changes |
| W10 | Restricted role uses each shortcut | Existing permissions enforced at navigation and command layers |
| W11 | Airplane mode | Local permitted flows work and persist normally |
| W12 | Restart device/process, rotate where supported | Routing and pending form behavior remain correct |
| W13 | Resize and large text/display scaling | All four actions readable and reachable at supported size |
| W14 | Multiple widget instances | Each action routes correctly without stale/crossed intents |
| W15 | Update existing installed test app | Widget remains functional; app records preserved |
| W16 | Supported launcher/device variations | Touch areas, clipping, background and labels verified on actual devices |
| W17 | Accessibility navigation/readout | Four meaningful action names and predictable focus order |
| W18 | Actual saved action initiated through widget | Same financial result as initiating it inside the app |

## 6. Final testing mandate: test the entire app together

Before calling the app final, inventory every current screen, action, setting, transaction type, importer/exporter, integration and permission. Map each to tests. Do not limit testing to the widget or happy-path screenshots.

Inspect implementation and persisted effects as well as UI output. Correct reproducible bugs and rerun affected scenarios. Do not hide wrong values, erase records or weaken validation to make tests pass.

Use existing detailed approved specifications as expected behavior where applicable. If implementation differs from an ambiguous or unresolved business rule, document the difference and ask a focused question rather than inventing financial policy. Ordinary coding defects do not require repeated permission to repair within the authorized scope.

### Before execution

1. Identify current source revision, uncommitted changes, app ID, schema, version, signing compatibility and candidate APK hash.
2. Preserve current work. Test with disposable databases/devices and synthetic records; never modify live shop records to test a theory.
3. Record device/Android version, timezone, locale, font scale, permissions and connectivity state.
4. Prepare independent expected records and exact totals. Do not calculate expected results using the same application function being tested.
5. Inventory prior defects and unverified features. Include them explicitly; earlier successful tests do not certify later changed builds.

## 7. Whole-app regression coverage

For every area below test: valid use, invalid input, zero/decimal/large amounts where applicable, edit, cancel/reverse, duplicate tap, restart, interruption and cross-screen reconciliation.

| Area | Required checks |
|---|---|
| Navigation and settings | Every button/shortcut opens the correct screen; back navigation, theme and lock settings persist; keyboard and larger fonts do not hide controls |
| Products and stock | Add/edit/receive/count correction distinct; variants, units/packs, barcodes, search, categories, low stock, archives, photos and multilingual names work |
| Purchase costs | Weighted-average calculations, exact transport allocation, actual payees and historical sale cost snapshots remain consistent |
| Suppliers and purchases | Supplier identity reused; multi-line invoice counted once; paid/part-paid/unpaid purchases, later settlements, opening debt and reversals reconcile |
| Billing | Products/services/mixed bills, manual prices, quantity/unit changes, discounts, drafts, split cash/digital/credit payments and change reconcile |
| Customers and Accounts | Distinct debtor count, due amount, repayment allocation, credits/openings and bill/account cross-links agree; no unassigned walk-in debt |
| Bill history | Completed/cancelled/held statuses and counts correct; search/date filters, original bill identity and later payment history preserved |
| Returns and cancellations | Stock, original revenue/cost, payments and debt adjusted once; later repayments handled; history retained |
| Services | Required job rates, correct units, option changes, held rates and service expenses; service-only lines do not change product stock |
| Cash In/Out | Correct direction, source/destination, effective dates, purposes, categories and attachments; bank does not affect physical cash |
| Shop expenses | One canonical expense across screens; classification, linked refunds, edits and cancelled records produce correct totals without duplication |
| Pigmy | Expandable schemes, openings, deposits, withdrawals, linked cash/bank effects, archives and tracked balances; no sales/expense/interest confusion |
| Giveaways and damage | Stock changes once, known cost separately visible, no fabricated revenue/debt, valid reversals |
| Reports and graphs | Identical scope/dates across cards, buckets, detail and export; negative/empty/incomplete data shown honestly |
| Formatting | Indian commas, currency decimals, negative amounts and Urdu/mixed names; identifiers and leading zeros unchanged |
| Spreadsheet import | Supported formats, mapping, validation, wrong-type files, repeat imports, malformed dates/currency, leading zeros and atomic recovery |
| Spreadsheet export | Effective-date/category/source/purpose filters, empty results, exact preview/file reconciliation, numeric cell types and safe text |
| Receipts and sharing | Correct original bill, discount/payment totals, readable long/multi-page layout, fonts, PDF content permissions, save/print/share cancellation |
| Backup and restore | Complete consistent data/photos/settings/branding/links; actual external save/retrieval and isolated restore; no replayed transactions |
| Migration and update | Supported older schemas and signing compatibility; no destructive fallback; records and widgets preserved |
| Security and roles | Lock lifecycle, permitted actions, deep links, exports and sync enforce the same restrictions |
| Two-phone operation | Pairing, offline durability, retries/acknowledgements, identity/receipt collisions, shortages and incomplete-sync reporting |
| Reliability and scale | Representative large catalog/history, storage exhaustion, permission loss, process death, rapid taps and concurrent operations |

For optional/unimplemented features, state NOT IMPLEMENTED or NOT APPLICABLE with evidence and scope justification. Do not mark them PASS. Missing required approved features remain incomplete work.

## 8. Gross profit and combined reporting: mandatory audit

Trace every displayed financial total back to canonical contributing records. In particular, audit the reported expense aggregation rather than assuming all cash outflows are expenses.

- Net product sales = product revenue after discounts and linked returns.
- Product COGS = the saved costs of those sold items, adjusted by valid reversals.
- Product gross profit = net product sales minus product COGS.
- Net operating expenses = active recorded operating expenses minus explicit linked expense refunds, each counted once.
- Combined recorded operating result, if shown = product gross profit + net service revenue - net operating expenses.
- Shared expenses count once in a combined result, not once in each subtotal and again in the total.
- Customer repayments do not add sales. Supplier stock payments do not create a second expense. Pigmy transfers do not affect profit.
- Unsold inventory cost/potential is not realized sales/profit. Current cash balance is not profit.
- Missing costs and incomplete sync produce clear warnings. Unknown is not zero.
- Period filtering uses effective dates consistently. Date-filtered movements are not automatically opening/closing balances.
- Changes to catalog prices/costs must not rewrite saved historical sale amounts/costs.
- Verify signed losses and margin calculation, including zero or negative net-sales denominators.

For a combined screen, reconcile product-only, service-only and Shared components and list precisely what the combined result includes. Do not claim complete accounting net profit when some costs/history are unrecorded.

## 9. End-to-end financial fixture

Use an isolated database with confirmed opening cash ₹10,000 and zero bank, debt and savings openings. Perform all actions within the same selected day. Record stable IDs and before/after balances at every step.

1. Receive 100 notebooks for ₹1,000 from Supplier A; pay ₹600. Supplier due is ₹400.
2. Pay a separate transporter ₹100 and capitalize it into that stock receipt. Inventory is ₹1,100, cost ₹11 per notebook; supplier due stays ₹400.
3. Sell 10 notebooks at ₹20 each to Customer A with ₹20 total discount. Net bill ₹180; receive ₹100; customer due ₹80; COGS ₹110.
4. Complete a service-only bill for ₹50, paid in cash. No product stock change.
5. Receive ₹30 customer repayment. Customer due becomes ₹50; sales unchanged.
6. Pay supplier the remaining ₹400. Supplier due becomes zero; stock and operating expenses unchanged.
7. Record expenses: Shared electricity ₹120, Services paper ₹80, Products repair ₹40. All paid in cash through the existing shared expense flow.
8. Receive ₹20 cash expense refund explicitly linked to electricity. Net expenses become ₹220.
9. Deposit ₹300 into Pigmy from cash; withdraw ₹100 back to cash. Tracked savings ₹200; profit unchanged.
10. Return two saleable notebooks from step 3 and explicitly refund ₹36 cash, reflecting their discounted ₹18 price. Restore stock at saved ₹11 cost. This is a cash refund, not also a debt reduction; remaining customer due stays ₹50.

### Exact expected final state

| Measure | Expected |
|---|---:|
| Notebook quantity | 92 |
| Notebook inventory cost | ₹1,012.00 |
| Net product sales | ₹144.00 |
| Product COGS | ₹88.00 |
| Product gross profit | ₹56.00 |
| Service revenue | ₹50.00 |
| Gross operating expenses | ₹240.00 |
| Explicit expense refunds | ₹20.00 |
| Net operating expenses | ₹220.00 |
| Products / Services / Shared net expenses | ₹40 / ₹80 / ₹100 |
| Combined recorded operating result | -₹114.00 |
| Physical cash | ₹8,624.00 |
| Bank balance | ₹0.00 |
| Customer due / distinct customers owing | ₹50.00 / 1 |
| Supplier due / distinct suppliers owed | ₹0.00 / 0 |
| Pigmy tracked balance | ₹200.00 |
| Completed original bills | 2 |
| Linked sale returns | 1 |

Independent checks: net billed revenue ₹194 = net customer collections ₹144 + customer due ₹50. Inventory ₹1,100 - COGS ₹88 = ₹1,012. Cash ₹8,624 + inventory ₹1,012 + customer due ₹50 + savings ₹200 = ₹9,886, equal to opening ₹10,000 plus the recorded result -₹114 for this deliberately complete fixture.

Run the scenario through actual UI workflows, including widget entry points where available. Compare UI, persistence, reports, graph buckets and exported files. Repeat checks after restart and after restoring a real backup into another disposable installation. Do not repost transactions after restore.

Also test separate branches: bank instead of cash, debt-credit instead of cash refund, later-period repayment/refund, zero-sales period, missing cost, mixed product/service discount and purchase after a previous sale. Do not combine incompatible branches into one expected final balance.

## 10. Failure and recovery tests

- Interrupt operations before and after database commit and before acknowledgement where applicable. Require either no committed operation or one complete operation; never partial stock/money updates.
- Double tap Save, retry after timeout and reopen after process death. Stable identity must prevent repeated financial effects.
- Fail attachment processing/replacement; preserve input and the previous valid photo. Do not silently discard requested evidence.
- Simulate unavailable provider, denied permissions, offline sharing/save, low storage and corrupt files. Require useful error/retry with no data mutation.
- Reject unsafe or incompatible backup archives before altering live database state. Retain a recoverable pre-restore copy in the supported restore workflow.
- Test real Android release-equivalent/minified behavior for fonts, camera, files, authentication and widget navigation.
- Final two-phone lifecycle/connectivity verification needs two actual devices. Simulated or one-device results must remain qualified.

## 11. Fix-and-retest procedure

1. Reproduce each failure in isolation; record exact input, expected outcome, actual outcome and affected records.
2. Determine root cause in the relevant UI, calculation, persistence, routing or integration path.
3. Implement the smallest complete fix consistent with approved requirements. Preserve historical data and other features.
4. Add meaningful regression coverage for financial/data-loss/security defects and the discovered failure boundary.
5. Rerun the failed case, related feature tests and affected end-to-end reconciliation. Repeat the complete golden scenario after changes to shared financial logic.
6. Verify migrations/recovery again if schema, serialization or linked record semantics changed.
7. Record fix revision and final evidence. Do not mark a defect resolved solely because the app compiles or a button opens.

Do not automatically repair uncertain production balances by inventing transactions. Any historical discrepancy needs an explainable, reviewed correction path separate from testing.

## 12. Required evidence and final release gate

For every test, record:

```text
Test ID / feature:
Build version, source revision and APK SHA-256:
Device / Android / schema / timezone:
Initial fixture and record IDs:
Steps actually executed:
Expected result:
Actual result:
Status: NOT RUN / PASS / FAIL / BLOCKED / PARTIAL / NOT APPLICABLE
Evidence location:
Defect or blocking dependency:
Fix revision and retest result:
```

Provide:

- Feature-to-test coverage inventory covering every actual feature/action.
- Widget screenshots at supported sizes and evidence for all four destinations, lock routing and draft preservation.
- Test counts by status, with every omission explained.
- Expected/actual/difference reconciliation of stock, cash/bank, customer/supplier dues, sales, COGS, expenses/refunds, gross profit, combined result and savings.
- Graph/card/export and receipt reconciliation evidence.
- Actual backup retrieval/isolated restore and nondestructive migration evidence.
- Fixed defects and remaining limitations with severity, impact and recovery guidance.
- Candidate APK filename/version/code/hash and whether it is exactly the artifact tested.

Do not call the app final while critical crashes, data loss, unauthorized access, duplicate postings, incorrect profit, unexplained balance differences or failed migration/recovery remain. Required but blocked/unrun tests must remain visible; lack of testing is not a pass.

No finite test suite guarantees absence of every possible bug. The release claim must be bounded by actual coverage, tested devices and remaining limitations, not a blanket promise that everything is error-free.

Never install the production update, uninstall the live app, clear its storage or overwrite live shop data without explicit user authorization. Prepare and verify the candidate first; production installation is a separate approval step.
