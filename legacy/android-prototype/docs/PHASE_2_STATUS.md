# Phase 2 stock increment

Build 0.2.2-stock, database schema 3. Development only; no release readiness claim.

Latest verification: six JVM lock-policy tests and 15 database instrumentation tests passed.
Phone UI checks passed for manual opening stock, pack deductions, damaged separation,
counted totals, pack-size edits, archive/unarchive and independent piece/pack prices.
The installed update retained the test product's 23 sellable and 3 damaged pieces.

User confirmed manual entry first on 30 October 2026. Excel product import remains planned;
the supplied XLS is a money ledger and cannot supply product opening quantities/costs.

Implemented in this increment:
- Separate product/variant records, optional unique normalized SKU, category, brand.
- Whole pieces and per-product pack sizes; piece selling price and low-stock threshold.
- Product and opening movement committed atomically, with nullable total opening cost.
- Stock quantities derived from movement history, not independently editable totals.
- Shop use, giveaway, damage transfer, stock-only supplier return and add/remove corrections.
- Shortage warning with explicit proceed checkbox; negative quantity stays visible.
- Operation IDs prevent a retried transaction from posting twice.
- Encrypted unfinished form storage, separate from posted inventory.
- Explicit migration 1 → 2 retaining foundation settings and drafts; no destructive fallback.
- Counted-total correction records the difference with the prior quantity and reason.
- Product edits preserve total pieces and historical pack snapshots; before/after metadata is retained.
- Archive/unarchive keeps stock and history and rejects movements while archived.
- Back-to-list keeps the unfinished form and offers Resume without overwriting it.
- Separate optional pack selling price; blank is unknown/unset, not a computed multiple of piece price.
- Migration 2 → 3 adds the nullable price without changing existing stock or historic costs.

Not yet implemented: CSV/XLSX import/mapping and XLS conversion,
photos, favourites, purchase receipt/account links,
valuation/moving-average costing and complete Urdu errors/history labels.
No supplier debt is changed by stock-only returns. No stock removal is represented as a sale.
Opening cost is retained for later valuation but no reliable profit is claimed yet.
Phase 2 is not complete until these remaining requirements and target-device UI checks pass.

Next checks: encrypted migration fixture, movement calculations, duplicate SKU rollback,
repeated submission, shortage gate, retained unknown cost, installed app upgrade, preserved
offline draft, editable product flow, pack movement and damaged history on target phone.
