# Ali Educational: complete backup package, Bill History repair, readable light mode and Accounts layout

Date: 1 November 2026

## 1. Latest user request and execution order

Implement these changes in the current app, rigorously verify them, and **build the candidate APK now**. After the user connects the intended phone for this update, install the compatible tested update while preserving its existing app data.

This document is an implementation handoff, not a claim that fixes/build/tests have already occurred. Do not merely document proposed fixes: inspect, implement, test, correct failures and produce the build.

Required work:

1. Back Up Everything produces both a complete restorable backup and readable Excel records from one consistent snapshot.
2. Make saving/sharing the two outputs and restoring on another installation straightforward.
3. Repair the nonworking Bill History button on Home.
4. Fix light-mode readability throughout the app.
5. Fix placement and spacing on the supplied Accounts screen and its supplier counterpart.
6. Create synthetic test files and transactions; prove backup completeness and restoration with record-level reconciliation.
7. Build now; update the intended phone when connected and ready, using the conditions in Section 12.

Preserve all existing features, relationships, shop records, approved permissions and prior completed work. Do not replace the current project with an old review copy or introduce unrelated redesigns.

## 2. Evidence and limits

The supplied screenshot shows the Customers Owe tab in light mode. The summary title competes with the Add Customer button, the amount is pale against a pale card, the button wraps awkwardly, and the search field shares a crowded row with filters. The empty-state text sits far below the controls.

The supplied `ali_backup_20261101_202423.zip` was inspected read-only and contains:

- `ali_educational.db`: 327,680 bytes.
- `manifest.json`: 221 bytes.
- One JPEG under `attachments/`: 11,673 bytes.

The ZIP CRC check passed. The manifest reports format version 1, schema version 4 and package `com.alieducational.app.debug`. These facts do not prove complete settings/attachment coverage, database integrity, restore success or compatibility with the phone's current installed package. An attempted in-memory database inspection did not complete; no database integrity result was established.

Inspect the current implementation and actual installed package rather than assuming this old archive identifies the next build or signing key. Preserve this archive as a reference; do not overwrite it or restore it over live shop records for testing.

## 3. Back Up Everything: two useful outputs

### One consistent snapshot

On Back Up Everything:

1. Acquire a consistent snapshot of committed data, including any committed WAL data, using a supported database backup/snapshot method.
2. Capture required settings, branding and attachment contents consistently with that snapshot. Prevent concurrent evidence replacement from creating dangling references.
3. Validate the snapshot and build a complete recovery archive.
4. Generate the readable Excel workbook from that exact snapshot, not a second later query of changing live tables.
5. Validate both outputs and their shared snapshot identity.
6. Present accurate per-output status and save/share actions.

Do not casually copy a live database file or separately collect related tables at inconsistent times. Prevent simultaneous backup jobs, show progress, preserve prior successful outputs and allow useful retry after failure.

### Output A: complete recovery archive

Suggested stable safe filename:

`Ali_Educational_Complete_Backup_2026-11-01_20-24-23.zip`

Its purpose is to restore the app. It contains the database and every required portable file/settings resource, plus a truthful manifest and integrity checks. The user should never need to extract it or open its database manually.

### Output B: readable records

Suggested filename using the same timestamp:

`Ali_Educational_Shop_Records_2026-11-01_20-24-23.xlsx`

Its purpose is reading and checking records outside the app. It is not a replacement for the recovery archive and must not be described as a complete restore source.

### Save Both package

Offer a convenient outer package:

```text
Ali_Educational_Backup_Package_2026-11-01_20-24-23.zip
  Complete_Backup.zip
  Shop_Records.xlsx
  Read_Me.txt
```

The recovery archive retains its own manifest and attachment structure. Read_Me explains which file is for recovery and which is for reading, with timestamp and snapshot ID. Do not include passwords, signing keys, private machine paths or unnecessary diagnostics.

Use one outer file for Save Both through Android's native document picker; do not assume every provider supports atomically creating a folder and two files. Also provide Save/Share complete backup only and Save/Share Excel only. If multi-file sharing is supported, handle receiver support honestly; the outer package is a reliable single-file alternative.

Restore Backup must accept either the complete recovery ZIP or the outer package and safely locate the contained recovery archive, so the user does not have to unzip manually. Reject ambiguous packages with multiple candidate backups rather than guessing. Bound nested archive handling; do not recursively unpack arbitrary archives.

## 4. Backup screen and honest status

Recommended completion view:

```text
Backup ready
1 November 2026, 8:24 PM

Complete recovery file       Ready
Readable Excel records       Ready
Photos and files             [verified count]

[ Save Both to Drive / Files ]
[ Share package ]
[ View details ]

Restore Backup
```

- Counts must come from the validated snapshot. Never hardcode “Everything backed up” based on a nonzero ZIP size.
- View details shows snapshot identity, date, app/schema/format versions, file sizes, verified record/file counts and any warnings.
- Distinguish Local backup created, Excel created, Package ready and Copy saved to selected provider.
- If Excel fails but the complete archive succeeds, retain the recovery file and explain the partial result. Offer retry; do not discard a good backup or claim both succeeded.
- If external saving fails, retain local files and previous successful timestamps. Cancellation is not success.
- Use dated filenames and keep previous backups; do not silently overwrite or delete them.
- Keep manual native Drive/Files saving. No unattended uploads or new OAuth setup in this scope.
- Provider-write completion is not proof of cloud upload completion. Report only what can be verified and show available provider/destination information without fabricated cloud paths.
- Display Owner backup limitations if a Billing phone has pending or unknown unsent records. An absent device's unknown state is not zero.

## 5. Readable workbook specification

Include meaningful sheets for the actual data model. At minimum, where supported by the app:

| Sheet/group | Readable contents |
|---|---|
| Summary | Snapshot/date, reporting scope, record counts, stock/account/cash/savings totals and incompleteness warnings |
| Products and Stock | Product/variant/unit identities, prices/costs, quantities, categories, archive state and unknown-cost indicators |
| Stock Movements / Purchases | Receipts, invoices, transport allocations, adjustments, damage/giveaways and supplier references |
| Bills / Bill Items | Original numbers/dates/status, product/service lines, discounts, totals, payments and remaining due |
| Payments / Allocations | Customer collections, supplier settlements, linked bill/purchase/opening-debt allocations and reversals |
| Customers / Suppliers | Contact details where authorized, opening balances, positive dues and separate credits/advances |
| Cash In Out | Effective dates, direction, purpose, category, source/destination, amounts, notes and linked IDs |
| Expenses / Refunds | Classification, category, original expense link, source/destination, status and attachment presence |
| Services | Definitions, job details/rates in their historical records and expense attribution |
| Pigmy | Schemes, confirmed openings, deposits, withdrawals, history and tracked balances |
| Categories | Relevant categories and archive state |

- Where sheet names must be shortened to meet XLSX limits, use clear names and a Summary index.
- Include stable references so records can be traced across sheets. Preserve historical status and distinguish active totals from cancelled/reversed audit rows.
- State each summary's scope: current balance, all-time movement or other explicitly defined measure. Do not label cash balance or unsold stock potential as profit.
- Use real numeric cells, Indian currency grouping, two monetary decimals and unambiguous real spreadsheet dates. Verify formulas/sorting remain usable.
- Preserve Urdu/mixed text, leading-zero identifiers and notes. User text must not become executable formulas.
- Use readable headers, frozen rows, filters, appropriate widths and wrapped notes.
- Show attachment presence/count, not inaccessible local paths. Original photos remain in the complete backup; Excel is not an evidence-image backup.
- For very large datasets, respect workbook limits and split clearly indexed sheets/files or explain a recoverable export limit. Never truncate silently while reporting a complete export.

## 6. Complete backup coverage audit

Do not rely on a fixed list alone: inspect every persistent table, preference store, managed file directory and relevant schema relationship in the current app. Maintain a coverage inventory with storage location, backup method, restore mapping and verification.

Explicitly verify:

- Products, variants, piece/pack mappings, barcodes, categories, photos, costs/prices and archived states.
- All stock receipts/movements/count corrections, purchases, invoices, transport allocation, damage/shop use/giveaways and reversals.
- Completed/cancelled bills, held drafts, lines, discount allocations, original receipt IDs/dates, service snapshots and saved cost snapshots.
- Customer/supplier identities, opening balances, invoices, payments, allocations, credits/advances and audit history.
- Cash/bank accounts or supported sources, confirmed openings, ledger purposes, effective/audit timestamps and linked movements.
- Expenses, linked refunds, Products/Services/Shared classification, categories and actual photos/evidence.
- Service definitions and relevant service reporting data.
- Every Pigmy scheme, opening provenance, deposit, withdrawal, linked transfer, archive status and photo.
- Shop profile, branding/logo assets, relevant preferences, themes, thresholds and supported numbering configuration.
- Applicable sync event identities and acknowledgements needed for safe reconciliation. Do not restore a device role or pairing secret blindly onto another phone.

### What “complete” excludes or cannot promise

- Disposable caches may be regenerated if all source records/assets needed to regenerate them are restored. Document that exclusion.
- Android biometric enrollment, device credentials and keystore-bound secrets are not portable ordinary backup files; re-establish protection/pairing through the supported secure flow.
- Signing keys and developer secrets must never be packaged.
- Launcher widget placement may need to be added again on a new phone; preserve app-owned widget preferences if relevant and portable.
- Unsent records on another device cannot be included until received. Clearly disclose this.
- Do not silently omit a real data store by calling it a cache. Every exclusion must have a reason and restoration impact.

### Manifest and validation

Record true format/schema/app version, creation time with timezone information, snapshot ID, expected entries, file sizes/hashes and relevant record counts. Hashes detect corruption, not authenticity against a maliciously replaced archive; do not overclaim security.

Validate database readability/integrity, referential relationships, referenced attachments and required settings/assets. File/database checks supplement but do not replace restoring and comparing actual records.

## 7. Restore workflow and safety

1. User selects complete backup or outer package through Files.
2. Safely inspect in staging: prevent path traversal, absolute paths, oversized decompression, ambiguous nested backups and unsupported formats.
3. Check integrity, schema compatibility, required content and attachment references before changing current records.
4. Show backup date, source app/schema, available counts and warnings; explain whether restore replaces current data. Do not silently merge unrelated snapshots.
5. Require explicit confirmation for the destructive replacement step and create a consistent pre-restore recovery copy.
6. Restore atomically or with a tested recoverable staged swap; reopen/validate the restored data.
7. Reconnect attachments to valid managed locations, not the original phone's absolute paths.
8. Re-establish device-specific access/sync safely; never replay historic transactions as new postings.
9. Show verified completion and any required setup, such as adding the launcher widget again.

Failures must leave original data recoverable and provide a useful error/retry path. Do not uninstall or clear storage to make restoration appear successful.

## 8. Repair Home Bill History button

The user reports the main-page Bill History button does not work. Treat this as a defect, not a request for a second history implementation.

- Reproduce on the current build and trace tap handler, route, navigation host, role guard and destination initialization.
- Wire the existing Home shortcut to the actual Bill History destination. Preserve its approved placement below Cash In/Out in the former Settings shortcut position.
- Keep Settings accessible through the gear icon.
- Test cold/warm starts, repeated taps, Back, lock/unlock and recreation. No dead tap area, exception swallowed as silence or incorrect route.
- Handle zero bills with a proper empty state; an empty database must not make navigation appear broken.
- Existing bill rows open the selected persisted bill and customer-account links correctly. Viewing/sharing does not duplicate the bill or financial effects.
- If permission denies access, show a clear allowed response; do not bypass role restrictions or fail silently.
- Include a regression test at the actual Home entry point, not only a direct destination test.

## 9. Light-mode readability throughout the app

Audit all screens and dialogs in light mode, including selected/unselected tabs, chips, cards, amount labels, placeholders, disabled states, error text, charts, dropdowns and bottom navigation.

- Use explicit light-theme semantic colors rather than reusing pale dark-theme accents as body text.
- Use dark primary text and sufficiently dark secondary text on light surfaces. Reserve pale green/gold fills for backgrounds; use darker foregrounds.
- Use a darker readable green for positive amounts on pale green cards. For buttons, choose a foreground/background pair with measured contrast; white on a pale green button is not acceptable merely because it matches the brand.
- Target at least 4.5:1 contrast for normal text and 3:1 for large text; meaningful control boundaries/icons should target 3:1 against adjacent colors. Verify actual rendered pairs, including alpha overlays. These are explicit acceptance targets for this task.
- Disabled controls must remain understandable without resembling active controls. Do not rely on color alone for selected/error/negative state.
- Preserve light mode's appearance; do not fix it by turning all surfaces dark or shrinking labels.
- Verify dark mode has not regressed. Theme changes, process restart and system-theme following must behave consistently with existing settings.
- Test on a real phone at normal and enlarged font/display sizes; record screenshots and measured contrast results for problematic combinations.

## 10. Accounts placement fixes from the screenshot

Apply the same responsive structure to Customers and Suppliers, preserving the financial meaning of each side.

### Recommended arrangement

```text
[ We Owe ]             [ Customers Owe ]

Amount customers owe us
₹0.00
No outstanding customer balances

[ + Add Customer                         ]

[ Search customers...                    ]
[ All ] [ Outstanding ]

Customer list, or explanatory empty state
```

- Separate summary text/amount from the action button; do not place a long title underneath or against the button.
- A full-width Add Customer row below the summary is preferred on narrow screens. On wider layouts a separate aligned action may fit, but no overlap or awkward word wrapping is allowed.
- Keep the amount prominent and readable with Indian commas; allow large values without truncation.
- Give search its own full-width row and All/Outstanding their own row beneath it. Keep labels visible at larger fonts.
- Use consistent horizontal padding and vertical spacing. Keep content clear of system bars and bottom navigation.
- Position the empty state naturally beneath the search/filter area with readable line length; remove arbitrary large spacers. Distinguish No customers yet, No outstanding balances and No search matches.
- Supplier view follows the same pattern with Add Supplier and correct payable wording.
- Verify populated lists, long party names, large dues, zero balances, credits, archived records and keyboard-open search.
- Preserve correct counts: distinct parties with positive dues, not number of bills. Layout changes must not change account calculations.

## 11. Synthetic files and end-to-end verification

Create fixtures in a disposable test workspace, never the live shop database. Keep input files, expected-value manifest, generated outputs and test results available for review. Do not use private production records as public sample data.

### Required fixture set

1. Stock XLSX/CSV with leading-zero barcode, Urdu/mixed product name, categories, pack/unit examples, supplier references and exact cost/transport totals; also an XLS fixture if XLS import is advertised.
2. Cash-ledger files with effective dates across month boundaries, category/notes separation, expenses, linked refunds where the import contract supports them, and reviewed opening balance treatment.
3. Deliberately malformed/empty/corrupt/duplicate/overlapping files to verify rejection and retry without partial changes.
4. At least two test photos attached to different records and one branding/product image where supported, so a single surviving image cannot falsely prove all file classes are covered.
5. Expected-value manifest with stable IDs, normalized business fields, quantities, money in paise, relationships, attachment hashes and settings.

Do not silently map unsupported import fields to unrelated columns. Use actual UI transactions to create relationships that the importer does not support.

### Required populated dataset

Include at least one record in every supported persistent entity class: active/archived products and parties, multi-line purchase, partial supplier payment, product/service/mixed bills, held draft, customer debt and later repayment, return, expense and linked refund, each expense classification, cash/bank movement, Pigmy opening/deposit/withdrawal, category, giveaway/damage, photos, branding and nondefault settings. Include cancelled records and links with later activity.

Import fixtures, perform representative transactions and independently reconcile stock, cash/bank, dues, sales, COGS, gross profit, expenses/refunds and savings. Reuse the earlier approved golden financial scenario where applicable; its final checks include cash ₹8,624, product gross profit ₹56, combined recorded operating result -₹114 and customer due ₹50. Those values apply only when its exact preconditions/actions are followed.

### Backup and restore test matrix

| ID | Test | Required result |
|---|---|---|
| B01 | Audit every persistent store against coverage inventory | All required stores covered; exclusions justified |
| B02 | Back Up Everything with populated fixture | Recovery ZIP and workbook share snapshot identity and totals |
| B03 | Committed WAL data and concurrent save during backup | Consistent snapshot; summary/workbook/archive do not disagree |
| B04 | Replace/remove photo during snapshot capture | Referenced contents consistent; no dangling file reference |
| B05 | Open workbook independently | Numeric amounts, dates, IDs, text, sheet counts and summaries correct |
| B06 | Save outer package through actual Files/Drive provider | Valid package retained; actual provider write verified; no false cloud claim |
| B07 | Retrieve saved package and restore elsewhere | App accepts package without manual extraction; full comparison passes |
| B08 | Restore standalone complete ZIP | Same correct outcome as outer package |
| B09 | Compare restored records | IDs/fields/links/amounts/statuses match, not just row counts |
| B10 | Open restored photos/logo on separate installation | Contents valid without access to original phone paths |
| B11 | Restore/restart and inspect linked finances | No replayed sales/payments/stock/savings effects |
| B12 | Change setting/theme before backup and restore | Relevant portable settings retained; device-specific secrets handled safely |
| B13 | Excel generation failure after recovery ZIP success | Recovery file retained; partial result honestly reported and retry available |
| B14 | Provider cancelled/offline/missing/full/revoked | Local outputs intact; failed save does not update successful-save timestamp |
| B15 | Truncated ZIP, missing photo/DB, wrong hash, newer schema | Clear staged rejection; existing database untouched/recoverable |
| B16 | Unsafe paths, excessive nested/compressed archive, multiple backup candidates | Safe rejection without unexpected writes or guessing |
| B17 | Interruption/storage exhaustion during create/restore | No false success; old data and valid prior files recoverable |
| B18 | Two dated backups and later failed attempt | Previous good files/timestamps retained |
| B19 | Owner backup with offline Billing phone | Pending/unknown coverage disclosed; no claim of complete unseen records |
| B20 | Export/share permissions and sensitive content | Authorized user only; no signing secrets/private device paths leaked |

### Navigation and visual tests

| ID | Test | Required result |
|---|---|---|
| U01 | Tap Home Bill History with zero bills and populated data | Destination opens and shows appropriate content |
| U02 | History detail -> customer account -> Back | Correct identity/stack; no new financial record |
| U03 | Locked/warm/cold app and repeat tap | Correct permission-aware route; no crash or dead button |
| U04 | Settings gear after shortcut fix | Settings still accessible |
| U05 | Customer/supplier summary with long text and large amounts | No overlap/clipping; distinct action row |
| U06 | Search with keyboard and enlarged fonts | Search/filters/list remain usable; form not obscured |
| U07 | Light-mode screen/dialog/state audit | Recorded contrast targets met; readable values and controls |
| U08 | Switch light/dark, restart and revisit | Theme persists as expected; dark mode remains readable |
| U09 | Empty/search-empty/zero-due/populated Accounts | Correct different explanations and amounts/counts |
| U10 | Widget, billing, reports, sharing and backup smoke regression | Existing entry points and behavior preserved |

For each test report build/device, exact steps, expected/actual values, evidence and PASS/FAIL/BLOCKED/PARTIAL/NOT RUN. Fix failures and rerun related scenarios, including financial reconciliation after changes to shared data paths. A successful ZIP creation/build or a screenshot alone is not acceptance.

## 12. Build now; update the intended phone when connected

### Build immediately after implementation and available verification

- Inspect the current project/build configuration and installed-version evidence if available. The supplied archive's `.debug` package label is a compatibility clue, not permission to switch package IDs or signing identities.
- Produce a candidate APK with correct app ID, compatible signing identity and an appropriate incremented version code. Do not include synthetic records, developer secrets or test-only destructive actions.
- Run available automated/emulator/device-independent checks now. Clearly list any hardware/provider checks that require the later connection; do not mark them passed.
- Report APK absolute path, filename, version name/code, size, SHA-256, source revision/changes and test status. Keep the exact tested artifact identifiable.
- Do not postpone all work until the phone connects. Finish implementation, build and all possible isolated verification first.

### Update when the user connects the phone for this task

The user has requested this follow-on update. Proceed when the intended phone is connected for it and all prerequisites below pass; do not repeatedly ask for generic permission already given. A random connected device is not enough evidence of intended target identity. If multiple devices or an unexpected app/package appear, ask a targeted clarification.

1. Identify the intended device and currently installed app ID/version/signing compatibility. The phone may need the user to enable/accept normal USB debugging authorization.
2. Verify a fresh complete pre-update recovery backup can be preserved off-device. Do not count an untested empty archive as a safety copy. Preserve existing recovery files.
3. Confirm supported nondestructive migration and review required test results. If critical financial/data-loss/security/recovery failures remain, stop installation and explain the blocker.
4. Install the compatible APK **as an update**, preserving app data. Never uninstall, clear app storage, force destructive migration or install a different package and pretend the original app was updated.
5. If signature/package/version incompatibility blocks update, report it and resolve the legitimate compatible build path. Do not bypass it with data deletion.
6. Run nondestructive post-update checks: launch/unlock, Home Bill History, existing bill/customer/supplier records, light/dark readability, Settings and backup list. Compare existing counts/balances to the pre-update baseline where available.
7. Test transactional writes using disposable data/app isolation, not real transactions on the live shop database. Do not restore test data over the live phone to demonstrate success.
8. Report installed version, checks passed and any remaining unverified device/provider features.

If connection is not available in the current session, deliver the built candidate and exact pending checklist. Do not claim to monitor future connections or auto-update later unless an actual supported monitoring mechanism has been explicitly established. Resume these steps when the user returns with the connected phone.

An older APK is not automatically a safe rollback after schema migration. Preserve a recovery plan that accounts for data created since the backup; never silently replace current data with an older snapshot.

## 13. Required completion handoff

Deliver implementation summary, changed files, before/after Accounts and light-mode screenshots, working Home Bill History evidence, fixture files/expected manifest, sample package/workbook, complete backup coverage inventory, actual restore comparison and test matrix.

State separately: implemented, built, automatically tested, device tested, provider save tested, restore tested and installed. Never collapse these into “done” if some stages remain pending.

Do not call the app final with unexplained record omissions, missing attachments, incorrect financial totals, unreadable critical controls, broken navigation or failed recovery. Fix and retest before release. This handoff authorizes preparing the implementation/build and the conditional in-place phone update described above, not destructive operations on shop data.

## 14. Latest addition: correct the widget to match the approved slim-bar reference

### Reference assets and selected design

- Approved concept board: `output/widget-reference/Ali_Educational_Widget_Reference.png`, supplied alongside this document.
- Use ONLY the **middle concept, labelled 2. Slim bar**. The left and right concepts are not selected.
- Actual current-phone screenshot: `C:/Users/Musid/Downloads/WhatsApp Image 2026-11-01 at 8.46.27 PM.jpeg`.
- The concept is a visual target, not a literal phone wallpaper, launcher layout or source-code asset. Do not implement the surrounding phone frame, wallpaper, app icons, presentation heading or labels outside the widget.

The user reports that the implemented widget does not look like the selected design. The actual screenshot has the four correct actions, but very small thin icons, smaller labels, a small gold title, no reference-style header separator and different icon shapes. Preserve the working actions while correcting these visible differences.

### Required visual changes

1. Retain one compact wide rounded charcoal/deep-green widget and a single horizontal row of four equal action areas.
2. Use a more prominent, readable light/white **Ali Educational** heading aligned to the left with consistent internal padding, matching the middle reference. Do not keep the tiny gold heading as the final design.
3. Add the subtle horizontal divider between the heading and actions, with comfortable spacing. It must not intersect labels or icons.
4. Replace the small diagonal Cash In/Out arrows with clearly visible upright incoming/down and outgoing/up arrows over a short baseline, like the reference. Cash In is green; Cash Out is orange.
5. Use a receipt-with-plus icon for New Bill and a box-with-plus icon for Add Stock. A plain small square plus does not match the Add Stock reference.
6. Enlarge and visually balance all four icons. Approximately 28-32 dp is an initial implementation target; verify at the actual launcher size rather than treating reference-image pixels as Android dp.
7. Increase label readability using medium/bold light text, with consistent icon-to-label spacing. Approximately 13-14 sp is a starting point, subject to actual fit and accessibility testing.
8. Preserve exact labels: Cash In, Cash Out, New Bill, Add Stock. Keep labels on one line at supported dimensions and prevent clipped or split words.
9. Use equal-width columns, aligned icon centers and label baselines. Every whole action area is tappable, not just its small icon. Maintain at least 48 dp touch targets.
10. Use subtle separators between actions if needed to match the reference, without making the bar visually busy. Preserve adequate spacing from the widget border.
11. Keep the outer shape rounded and compact, but do not force an excessively short height that shrinks the heading/actions. Configure sensible minimum/resize dimensions for supported launchers.
12. Use bundled vector/drawable assets, not Unicode/emoji approximations whose appearance varies by phone. Do not ship a flattened screenshot as a noninteractive widget.

The goal is a faithful practical Android rendering, not a promise of identical pixels across different launchers. Compare proportions, icon meaning/weight, text hierarchy, spacing and colors at matched displayed widths. If a platform constraint prevents an element, document the exact constraint and show the closest working result rather than claiming an exact match.

### Preserve behavior and security

- Cash In opens the incoming form; Cash Out opens the outgoing form; New Bill uses the existing draft-safe billing flow; Add Stock opens existing stock receiving.
- No tap automatically saves data. Retain lock/unlock routing and existing role checks.
- Preserve existing unfinished bills/forms; repeated taps must not stack duplicate financial actions.
- Keep multiple widget instances and pending action identities distinct. A visual change must not swap shortcut destinations.
- Do not add balances, extra features or a second selected design.

### Verification and evidence

| ID | Test | Expected |
|---|---|---|
| V01 | Compare actual widget with middle reference at matched widths | Readable light heading, separator, four balanced icons and aligned labels |
| V02 | Check icon semantics | Down/in green, up/out orange, receipt-plus and box-plus; no tiny diagonal/generic-square substitutes |
| V03 | Supported launcher resize/font/display sizes | Compact single row remains readable; minimum sizing prevents clipping |
| V04 | Tap full area of each action from cold/warm/locked state | Correct existing form after required unlock; no automatic posting |
| V05 | Existing draft and rapid taps | Input preserved; no duplicated navigation/save |
| V06 | Update an existing widget instance | New layout appears after supported update/refresh; no need to clear app data |
| V07 | Fresh widget instance and multiple instances | Same correct layout/actions without stale routing |
| V08 | Actual phone screenshot after compatible update | Visual comparison and remaining launcher differences documented |

Provide a before/after widget screenshot and a side-by-side comparison with the **middle reference only**. Verify real tappable controls, not merely a static preview. Build this correction into the same candidate as Sections 1-13; follow the same build-now and later connected-phone update instructions.
