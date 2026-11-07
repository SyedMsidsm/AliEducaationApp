**31 October update: Home / Stock / Billing / Accounts replaces prior navigation. See BILLING_UPGRADE_PLAN.md for authoritative billing flows and four new phone pictures.**

# Ali Educational: UI/UX implementation reference

Current scope: one Android phone, one user, local storage. This specification overrides outdated login, computer-sync and five-tab elements in the earlier preview images.

## Visual system

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

## Logo rules

- Source of truth: `assets/brand/ali-logo-original.png`.
- Dark placement concept: `assets/brand/ali-logo-dark-concept.png`. Preserve gold details; black parts become warm white; transparent background. This needs production cleanup, not a silent replacement with an invented logo.
- Brand reference only: `assets/brand/ali-educational-poster-reference.jpg`. Do not use its product collage as the screen layout/background.
- Unlock: centered logo above Ali Educational, clear space, fingerprint/device-unlock action below; no account selector, email or app password field.
- Home: compact logo beside shop name without pushing stock actions below the fold.
- Receipt: small black-only logo on white beside shop name. A clean monochrome production asset has not yet been produced.
- Preserve aspect ratio; no stretching, cropping the mark, shadows or pale rectangular background on dark surfaces.
- Android launcher/adaptive icon and splash asset must be prepared from the approved logo during implementation. They do not exist yet; check crop-safe placement on actual Android launchers.

## Screen inventory and flow

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

## Common states and safeguards

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

## Existing preview inventory

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

## Design acceptance

The brother should complete receiving stock, recording item sales, recording a daily total, choosing discount/credit, entering repayment and making/restoring a backup in an isolated prototype/test app. Record confusion and revise before full adoption. Test one-handed use, smaller screens, long labels, large text, keyboard appearance, scroll behaviour, RTL and both themes. A static PDF does not prove these interactions work.


## Confirmed cost display

Show delivery as proportionally allocated by purchase value. Product detail shows weighted-average unit cost. Actual profit uses the saved average cost at sale time. Purchase summaries show expected profit, not batch-specific realised profit once stock is mixed. These decisions override the proposed labels in the earlier PNG/PDF previews.
