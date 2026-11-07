from pathlib import Path
import zipfile

replacements = {
'**Proposed, pending confirmation:** distribute delivery according to each item\'s purchase value.': '**Confirmed:** distribute delivery proportionally according to each line\'s purchase value (quantity × purchase price after purchase discounts, before delivery). Line delivery share = total delivery × line purchase value / total purchase value.',
'Multiple purchases with different costs need a chosen costing policy before actual profit is enabled; see open decisions.': 'The confirmed costing policy is weighted-average cost per product/variant. Use the moving weighted-average implementation described in the profit section when new stock arrives.',
'Using the proposed purchase-value allocation': 'Using the confirmed proportional purchase-value allocation',
'Select a consistent policy for changing purchase costs, such as FIFO or weighted average, before implementation of profit calculations. This choice is still open.': 'Confirmed policy: weighted-average cost. For this transaction-based app, implement a moving average per product/variant: new average = (existing stock cost value + incoming goods cost including allocated delivery) / (existing quantity + incoming quantity). Use base-unit quantities; never average different products together.',
'Define how purchase-level profit works under that costing policy; the example uses one purchase and does not settle mixed-batch behaviour.': 'Expected profit for a new purchase may use that purchase\'s landed cost. Actual gross profit uses the product\'s weighted-average unit cost at the time of each sale. When purchases are mixed, show actual profit by product/sale/period, not an asserted actual profit attributable to one supplier invoice. Keep purchase summaries clearly labelled as estimates.',
'3. Delivery allocation rule and handling of separately paid delivery.': '3. Zero-value purchase allocation exception and handling of separately paid delivery. Proportional allocation for positive-value purchases is confirmed.',
'4. FIFO versus weighted-average costs, including purchase-level profit and negative stock.': '4. Negative-stock cost resolution and backdated corrections. Weighted-average costing is confirmed; only these exceptional behaviours need final implementation rules.',
'mixed purchases after the costing policy is agreed.': 'mixed purchases under the confirmed weighted-average costing policy.',
}

details='''

### Confirmed cost calculations and controls

Delivery allocation and weighted-average costing are confirmed user decisions, supplied in the attached decision screenshots. They are no longer open choices.

Implementation detail: use a moving weighted average after each receipt. Example: 10 existing pieces with total cost ₹100; receive 10 more with goods cost ₹140 and allocated delivery ₹20. Combined quantity is 20, combined cost ₹260, and average cost ₹13 per piece. Sell 5 pieces for ₹20 each with a 10% whole-sale discount: gross sales ₹100, discount ₹10, net sales ₹90, cost ₹65 and actual gross profit ₹25. Remaining stock is 15 pieces with cost ₹195.

Store each sale's applied cost so later receipts do not retroactively change previously recorded profit. Retain sufficient internal precision for per-unit costs, round displayed currency to paise, and reconcile total allocated delivery exactly. For multiple sale lines, allocate the whole-sale discount consistently by pre-discount line value before reporting product profit.

When stock is zero with no unresolved cost, the next receipt establishes the new average. Negative stock, missing opening cost, returns and backdated edits must not use the ordinary formula blindly: flag unresolved profit and define an auditable correction/recalculation rule before release. Keep sellable-to-damaged transfers at the applicable cost; never create profit from a quantity transfer.
'''

for name in ['ALI_EDUCATIONAL_FINAL_APP_PLAN.md','ALI_EDUCATIONAL_APP_SPECIFICATION.md']:
 p=Path(name);s=p.read_text(encoding='utf-8')
 for old,new in replacements.items(): s=s.replace(old,new)
 s=s.replace('| Delivery | Included once in stock cost, separately visible |','| Delivery | Proportional allocation by purchase value; included once in stock cost |\n| Costing | Weighted-average cost per product/variant |')
 s=s.replace('## 10. Phase 5: receipts and reports', details+'\n## 10. Phase 5: receipts and reports')
 p.write_text(s,encoding='utf-8')

extra='''

### Confirmed costing acceptance cases

| ID | Scenario | Expected result |
| --- | --- | --- |
| COST-01 | Allocate ₹50 delivery across goods values ₹200 and ₹300 | Shares ₹20/₹30; total cost ₹550; no second delivery deduction. |
| COST-02 | 10 pieces cost ₹100; receive 10 with goods ₹140 + delivery ₹20 | 20 pieces; stock value ₹260; moving average ₹13. |
| COST-03 | Sell 5 from COST-02 for ₹100 less 10% discount | Net sale ₹90; cost ₹65; gross profit ₹25; 15 pieces costing ₹195 remain. |
| COST-04 | Receive additional stock at a different cost after COST-03 | Remaining average updates; saved ₹25 historical sale profit does not change. |
| COST-05 | Several products, packs, fractional delivery shares | Calculate each variant separately in base units; shares reconcile exactly to delivery total. |
| COST-06 | Mixed purchase batches | Actual profit uses weighted-average sale cost; purchase screen does not claim batch-specific realised profit. |
| COST-07 | Negative quantity, unknown opening cost, or zero purchase-value allocation base | Explicit exception/review; no divide-by-zero or invented reliable profit. |

These application tests are NOT RUN until implementation. Weighted-average policy and proportional delivery allocation are confirmed; edge-case handling remains subject to implementation review.
'''
p=Path('APP_TEST_PLAN.md');s=p.read_text(encoding='utf-8').replace('mixed purchases after the costing policy is agreed.','mixed purchases under the confirmed weighted-average costing policy.');p.write_text(s+extra,encoding='utf-8')
for name in ['ALI_EDUCATIONAL_FINAL_APP_PLAN.md','ALI_EDUCATIONAL_APP_SPECIFICATION.md']:
 p=Path(name);s=p.read_text(encoding='utf-8').replace('## Appendix B. UI/UX, theme and logo assets',extra+'\n## Appendix B. UI/UX, theme and logo assets');p.write_text(s,encoding='utf-8')
p=Path('UI_UX_SPECIFICATION.md');s=p.read_text(encoding='utf-8');s+='\n\n## Confirmed cost display\n\nShow delivery as proportionally allocated by purchase value. Product detail shows weighted-average unit cost. Actual profit uses the saved average cost at sale time. Purchase summaries show expected profit, not batch-specific realised profit once stock is mixed. These decisions override the proposed labels in the earlier PNG/PDF previews.\n';p.write_text(s,encoding='utf-8')
# Refresh archive members from the verified existing package, preserving its structure.
archive=Path('Ali_Educational_Final_Plan_and_Design_Assets.zip')
with zipfile.ZipFile(archive) as z: names=z.namelist()
for name in names: assert Path(name).is_file(),name
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for name in names:z.write(name,name)
with zipfile.ZipFile(archive) as z: assert z.testzip() is None
print('Confirmed decisions applied; tests and design package refreshed.')
