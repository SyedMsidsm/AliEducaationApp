from pathlib import Path

p = Path('ALI_EDUCATIONAL_APP_SPECIFICATION.md')
s = p.read_text(encoding='utf-8')
s = s.replace('Status: consolidated plan for implementation and review. The application is not yet built.', 'Final consolidated specification | 29 October 2026\n\nStatus: agreed product direction with explicitly listed implementation decisions still open. The application is not yet built. Stock is the primary focus; daily money records support it.')
s = s.replace('See [APP_TEST_PLAN.md](APP_TEST_PLAN.md) for test IDs, expected results and pass criteria.', 'See Appendix A in this document for test IDs, expected results and pass criteria.')
s = s.replace('Show recent/favourite products and remember common supplier choices, allowing review before saving.', 'Show recent/favourite products and remember common supplier choices, allowing review before saving.\n- Display stock in familiar units, for example **2 boxes + 3 pieces** alongside the base-unit total. A box of ten makes this 23 pieces. Keep one underlying quantity so the two displays cannot diverge.')
s = s.replace('Choose whether to set up backup now or later.', 'Offer backup setup during onboarding, explain the destination, and let the brother connect Google Drive or choose a manual export destination. He may skip setup, but Home/Settings should truthfully show that no successful backup exists yet.')
s = s.replace('No device-sync status: there is only one phone. Show save errors and backup status when relevant.', 'No device-sync status: there is only one phone. Show save errors and a compact last-successful-backup status with an easy **Back up now** action.')
s = s.replace('### Two ways to record daily sales', '### Record item sale or record daily total')
s = s.replace('A money-only total such as “₹5,000 sold today” does not identify which products left the shop. It cannot automatically update per-product stock or reliable product profit. Quantity-only stock-outs remain available, with missing sales information clearly marked.', '''Also allow **Record daily total** for busy days when only the monetary total is known. Ask whether it is the full day's net sales or additional unrecorded sales. For a full-day total, show already recorded net sales and calculate only the unrecorded remainder; if the entered total is less than recorded sales, require review rather than creating a negative balancing sale. Show the date and scope clearly before saving.

Money-only entries do not identify which products left the shop. Label them **Stock details needed** and **Profit unavailable** where appropriate. Keep them in monetary sales totals without inventing inventory movements or costs. Adding item details later must replace/link the existing summary allocation, not add a second sale. Quantity-only stock-outs similarly need selling amounts for actual profit.

Because stock is the priority, the brother should normally use Record item sale or enter sold quantities by product at day-end. The app should list unresolved stock details for follow-up without blocking unrelated work. Do not claim inventory is fully current while sales quantities are missing.''')
s = s.replace('### Daily view', '''### Separate types of money movement

Keep **Sales**, **Other money received**, and **Expenses/payments** distinct. A deposit or money introduced into the shop is not automatically sales income. Supplier payments and old customer repayments retain their existing account links and must not be duplicated in a separate ledger.

Provide simple optional date, amount, category and note entries for other money received and shop expenses in More, with clear classification and no invented stock movements. This basic separation is included in Phase 4; detailed expense analytics remain later. Shop expenses stay separate from product gross profit, and cash-flow balance must not be labelled profit. Opening balances are separate from current-period income.

### Daily view''')
s = s.replace('- Separate shop expenses and services such as printing/lamination with editable default prices and no stock quantities.', '- Detailed expense analytics beyond the basic money-category separation; services such as printing/lamination with editable default prices and no stock quantities.')
s = s.replace('- Sale and lines, price snapshots, discounts, payment, remaining credit and linked stock movement.', '- Sale and lines, price snapshots, discounts, payment, remaining credit and linked stock movement.\n- Daily monetary summaries with date/scope, allocation to item sales and unresolved stock-detail status; other-money and expense entries linked to any existing payment to avoid duplication.')
s = s.replace('Pilot with a small set of real products and balances. Compare the app against manual records before importing everything and relying on it fully.', 'Pilot with a small set of real products and balances alongside the brother\'s existing records. Reconcile daily stock and amounts, note confusing steps and improve the interface before expanding to all products. Use an isolated pilot database and explicitly reviewed live opening balances; never merge pilot activity into live records by accident.')
s = s.replace('Historical expense support can be implemented in its own phase if selected.', 'Basic new expense/other-money entry is included in Phase 4. Bulk historical expense import still requires a reviewed migration mapping and explicit scope decision.')
s += '''

## 19. Final scope priorities

1. Keep stock accurate with quick item/quantity entry and familiar box/piece displays.
2. Keep supplier and customer balances correct with simple payment records.
3. Offer daily monetary totals as a fallback, with visible missing-stock and missing-profit information.
4. Keep sales, other money received and expenses clearly separate without turning Home into an accounting dashboard.
5. Make backup setup, last-success status, manual backup and tested recovery part of the first release.
6. Prove the workflows in a small parallel pilot with the brother before full adoption.

This final document consolidates the feature plan, phased delivery, distribution approach and detailed test plan. Open technical choices remain listed in section 15; examples and suggested defaults are not silently treated as final decisions.
'''
tests = Path('APP_TEST_PLAN.md').read_text(encoding='utf-8')
tests = tests.replace('# Ali Educational: dedicated testing and acceptance plan', '## Appendix A. Detailed testing and acceptance plan', 1)
tests = '\n'.join('###' + line[2:] if line.startswith('## ') else '####' + line[3:] if line.startswith('### ') else line for line in tests.splitlines())
tests += '''

### Additional acceptance cases for the final workflow

| ID | Action | Expected result |
| --- | --- | --- |
| FINAL-01 | Stock is 23 pieces, 10 per box | Display 2 boxes + 3 pieces and base total 23; pack and piece actions reconcile. |
| FINAL-02 | Already recorded net sales ₹600; enter full-day net total ₹1,000 | Add only ₹400 unallocated monetary sales; daily total ₹1,000, not ₹1,600. |
| FINAL-03 | Enter additional unrecorded sales ₹400 after ₹600 | Daily total ₹1,000, with the extra entry scope preserved. |
| FINAL-04 | Later supply item details for that ₹400 | Allocate/replace the summary; no second sales total and no duplicate stock deduction. |
| FINAL-05 | Enter full-day total below recorded sales | Request review; do not invent negative sales or silently rewrite transactions. |
| FINAL-06 | Record other money received and rent expense | Neither changes product stock nor gets misreported as sales or product gross profit. |
| FINAL-07 | Record a supplier payment or old debt repayment | Appears once in money history and linked account; no duplicate ledger event. |
| FINAL-08 | Skip backup setup, then complete a manual backup | Initially show no successful backup; afterwards display actual success time/destination. |

All application cases remain NOT RUN until the application is implemented. The supplied spreadsheet's arithmetic checks are the only completed data checks described here.
'''
s += '\n\n' + tests + '\n'
p.write_text(s, encoding='utf-8')
Path('ALI_EDUCATIONAL_FINAL_APP_PLAN.md').write_text(s, encoding='utf-8')
print('Final specification saved; detailed testing included.')
