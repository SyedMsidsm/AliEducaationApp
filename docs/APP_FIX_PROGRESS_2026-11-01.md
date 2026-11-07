# Ali Educational — repair work and confirmed decisions

This is a progress record, not a claim that the app has been fully repaired or tested.

## Confirmed additions

### Pigmy savings deposits

- One account/scheme.
- A configurable usual daily deposit, editable for an individual entry.
- Prefill the amount but save only after the user confirms payment. Do not create automatic daily payments.
- Deposits only in this version. Withdrawals, maturity payouts and interest tracking are deferred.
- A confirmed deposit paid from shop cash creates a linked Cash In/Out savings-transfer entry.
- Save both records atomically and prevent repeat-tap duplicates. Editing or cancelling the deposit must update or reverse its linked cash entry atomically.
- Transfers must be excluded from operating expenses and profit calculations.
- Show deposit date, amount, optional note/reference, history and period totals. Label the headline “Total recorded deposits”, not an independently verified bank balance.
- Historical cash-file rows must not be silently converted into deposits based only on the word “Pigmy”. Preview classification and prevent duplication with already recorded deposits.

### Free products and donations

- Products only; cash donations are outside this addition.
- Record item, quantity, date, reason (free item / donation) and optional recipient/note.
- Reduce stock once, without creating sales revenue or customer debt.
- Preserve the cost at the time of the stock-out. Show unknown cost explicitly.
- Keep these entries separate from cost of goods sold on paid sales. Show giveaway costs separately in analysis.
- Correct or reverse mistakes with history rather than silently deleting stock movements.

## Reference files inspected

Both supplied files are cash ledgers, not product-stock tables:

- `C:/Users/Musid/Downloads/Ali Educational 29-Oct-2026.xls`: Transactions sheet, 180 physical row positions including headings and other rows.
- `C:/Users/Musid/Downloads/Ali Educational 31-Oct-2026.xls`: Transactions sheet, 953 row positions including headings and other rows.
- Both have a heading on row 5: Date, Notes, Description / Category, Income, Expense, Balance.
- Notes carry meaningful transaction descriptions when Description / Category is blank.
- These files are test references. Their values must not be hardcoded as permanent app records.
- A representative product-stock file is still needed for format-specific stock import acceptance testing.

## Development location

Original matching project: `C:/Users/Musid/.gemini/antigravity/scratch/AliEducational`.

Isolated repair copy: `C:/Users/Musid/.codex/visualizations/2026/10/25/01a17358-b9dd-74e2-9bf4-9050fa382bac/ali-app-fixes`.

The original source and the production phone database have not been replaced by this work. Signing keys were excluded from the repair copy.

## Changes started in the repair copy

- Removed Recent Activity from Home, preserving underlying movement records.
- Replaced cash Excel auto-sized columns, which depend on desktop graphics, with fixed readable widths.
- Staged export files in app cache for sharing rather than relying on direct writes to public Documents.
- Added visible export failure handling and preserved coroutine cancellation.
- Empty filtered periods no longer fall back to exporting every transaction.
- Cash parser rejects unrecognized headings rather than guessing column positions.
- Cash parser preserves Notes as the description when the category cell is blank.
- Cash parser excludes explicit totals and balance rows from transaction import.
- Export now writes numeric amounts and date values.
- Extracted the actual export workbook builder for serialization/re-import regression tests.
- Product import detects the supplied cash-ledger format and explains which import screen to use, before writing products.

## Verification status

- Both original reference XLS files opened successfully using the app's Apache POI dependency in an isolated JVM test.
- Regression checks added for cash export/re-import, totals not becoming transactions, wrong-workbook rejection, Notes fallback and amount precision.
- Full build/test result and Android checks remain to be recorded after execution. JVM success alone does not establish Android export compatibility.

## Outstanding work

Continue the fixes and checks in `APP_REVIEW_2026-11-01.md`: transactional imports, duplicate prevention, invalid-date/amount reporting, stock editing, pack billing, payment/discount/reversal correctness, backups and restoration, report accuracy, layout/accessibility, receipts, lock behavior and remaining screens.

Pigmy and giveaway features above are confirmed requirements, not implemented features yet. Do not ship this development copy as a fully tested replacement.
