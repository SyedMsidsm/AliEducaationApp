"""Read-only source audit. No workbook edits, macro execution or live import."""
import sys, json, hashlib, re
from pathlib import Path
from decimal import Decimal
from datetime import datetime

root = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / "tmp" / "xls-reader"))
import xlrd

source = Path(sys.argv[1])
before = hashlib.sha256(source.read_bytes()).hexdigest()
book = xlrd.open_workbook(str(source))
reports = []
def amount(value):
    text = str(value).strip().replace(",", "")
    return Decimal(text) if text else Decimal(0)

for sheet in book.sheets():
    header = next((r for r in range(sheet.nrows)
                   if [str(v).strip().lower() for v in sheet.row_values(r)[:6]]
                   == ["date", "notes", "description / category", "income", "expense", "balance"]), None)
    if header is None:
        reports.append({"sheet": sheet.name, "classification": "unrecognised", "rows": sheet.nrows})
        continue
    records, summary, opening, discrepancies = [], [], None, []
    running = None
    for r in range(header + 1, sheet.nrows):
        row = sheet.row_values(r)
        if str(row[1]).strip().lower() == "previous balance":
            opening = amount(row[5]); running = opening
        elif re.fullmatch(r"\d{2}-[A-Za-z]{3}-\d{4}", str(row[0]).strip()):
            date = datetime.strptime(str(row[0]).strip(), "%d-%b-%Y").date().isoformat()
            income, expense, balance = map(amount, row[3:6])
            if running is None: raise ValueError("Opening balance missing")
            running += income - expense
            if running != balance: discrepancies.append(r + 1)
            records.append((date, str(row[1]).strip(), income, expense, balance))
        elif any(str(v).strip() for v in row):
            summary.append({"row": r + 1, "label": str(row[2]), "amount": str(row[3])})
    income = sum((r[2] for r in records), Decimal(0))
    expense = sum((r[3] for r in records), Decimal(0))
    sales = [r for r in records if r[1].casefold() == "sales"]
    controls = {"total income": income, "total expense": expense, "balance": running}
    summary_checks = [{"row": s["row"], "matches": amount(s["amount"]) == controls[s["label"].strip().casefold()]}
                      for s in summary if s["label"].strip().casefold() in controls]
    reports.append({
        "sheet": sheet.name, "header_row": header + 1, "classification": "money_ledger_not_product_stock",
        "headers": sheet.row_values(header), "transaction_count": len(records),
        "date_from": min(r[0] for r in records), "date_to": max(r[0] for r in records),
        "distinct_dates": len(set(r[0] for r in records)),
        "opening_rupees": str(opening), "income_rupees": str(income), "expense_rupees": str(expense),
        "closing_rupees": str(running), "running_balance_discrepancy_rows": discrepancies,
        "sales_label_rows": len(sales), "sales_label_income_rupees": str(sum((r[2] for r in sales), Decimal(0))),
        "other_income_rupees": str(income - sum((r[2] for r in sales), Decimal(0))),
        "summary_checks": summary_checks,
        "stock_import_allowed": False,
        "missing_stock_fields": ["product name", "quantity", "pack size", "purchase cost"]
    })
after = hashlib.sha256(source.read_bytes()).hexdigest()
assert before == after, "Source changed during audit"
result = {"source": str(source), "sha256": before, "source_unchanged": True, "sheets": reports}
target = root / "android/docs/evidence/ledger-source-audit.json"
target.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(result, ensure_ascii=False, indent=2))
