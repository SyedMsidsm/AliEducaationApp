# Discounts and purchase profit - 28 October 2026

This update adds simple sale discounts and purchase-level profit tracking to the inventory-first scope. Full POS remains deferred; AI remains optional and deferred.

## Confirmed requirements

- Enter a percentage discount, or enter the final agreed amount and calculate the effective percentage automatically. Example: original amount INR 100, final amount INR 90, discount INR 10 / 10%.
- If payment is below the amount due, ask whether the difference is a discount or remaining customer credit. Never silently forgive a balance. Credit requires selecting a customer.
- Preserve original amount, percentage, discount amount, final amount, amount paid, remaining credit, actor and time. Show the discount in the sale record and optional receipt.
- Discounts apply to the whole simple sale. Support decimal percentages from 0 to 100. Round currency to paise; calculate percentages from the undiscounted total, with a safe zero-total case.
- Track delivery separately on goods received and include it in stock cost exactly once.
- Show both expected gross profit at saved selling prices and actual gross profit as sales are recorded.
- Actual gross profit uses net sale value after discounts minus the cost of units sold, including allocated delivery. Unpaid customer credit remains distinct from revenue and profit; repayments must not count as new sales.
- Quantity-only stock-outs cannot produce reliable actual profit. Flag these as missing sales values rather than treating them as zero-value sales or inventing profit.
- Expected and actual gross profit exclude other shop expenses and must not be labelled net business profit.

## Proposed defaults to confirm before implementation

- Allocate a purchase's delivery charge in proportion to each line's purchase value, with exact rounding reconciliation. In the sample, INR 200 of pens receive INR 20 delivery and INR 300 of notebooks receive INR 30 delivery.
- Record who is owed delivery charges: supplier, separate carrier, or already paid. Including delivery in stock cost must not automatically add it to the goods supplier's balance if someone else is owed it.
- Costing across multiple purchases with different costs is still unresolved. The previews use one purchase only and do not settle FIFO versus weighted average.

## Illustrated example

Receive 20 pens at INR 10 and 10 notebooks at INR 30. Goods cost INR 500; delivery INR 50; landed stock cost INR 550. Unit costs become INR 11 per pen and INR 33 per notebook under the proposed allocation.

Saved selling prices are INR 15 per pen and INR 45 per notebook: potential sales INR 750, expected gross profit INR 200.

Sell 10 pens for INR 150 before a 10% discount: net sales INR 135, cost INR 110, actual gross profit INR 25. Expected gross profit on remaining stock is INR 160. The discount reduces combined realised and remaining potential profit by INR 15.

## Interface deliverable

The phone PDF now includes percentage discount entry, a discount-versus-credit choice, stock receipt with delivery, expected profit, and actual recorded profit. All figures are illustrative; this is interface design, not implemented app behaviour.
