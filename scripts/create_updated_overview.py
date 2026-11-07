from pathlib import Path
import re, html
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, KeepTogether
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

root=Path(__file__).resolve().parents[1]
out=root/'output/pdf'; out.mkdir(parents=True,exist_ok=True)
pdfmetrics.registerFont(TTFont('AppArial','C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('AppArialBold','C:/Windows/Fonts/arialbd.ttf'))
pdfmetrics.registerFontFamily('AppArial',normal='AppArial',bold='AppArialBold',italic='AppArial',boldItalic='AppArialBold')
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='BodyApp',fontName='AppArial',fontSize=10,leading=15,spaceAfter=7))
styles.add(ParagraphStyle(name='HeadApp',fontName='AppArialBold',fontSize=18,leading=23,spaceBefore=15,spaceAfter=10,textColor=colors.HexColor('#8B651D'),keepWithNext=True))
styles.add(ParagraphStyle(name='SubApp',fontName='AppArialBold',fontSize=12,leading=17,spaceBefore=10,spaceAfter=6,keepWithNext=True))
styles.add(ParagraphStyle(name='TitleApp',fontName='AppArialBold',fontSize=30,leading=36,spaceAfter=20))
story=[]
def para(t,style='BodyApp'):
    t=html.escape(t)
    t=re.sub(r'\*\*(.*?)\*\*',r'<b>\1</b>',t)
    story.append(Paragraph(t,styles[style]))
def section(title,text):
    story.append(PageBreak());para(title,'HeadApp')
    for p in text.strip().split('\n\n'):
        if p.startswith('### '):para(p[4:],'SubApp')
        else:para(p)

para('ALI EDUCATIONAL','TitleApp')
para('App features, updates and release review','HeadApp')
para('Consolidated overview • 1 November 2026')
para('Prepared for Musid and the shop owner. Covers the existing app, agreed changes, implementation instructions and required acceptance testing.')
para('Important status distinction','SubApp')
para('You reported that the app has been built and many updates implemented. This report consolidates what we have specified and reviewed; it is not a certificate that every feature is implemented or that the current APK passed all tests. Screenshots show selected screens, not complete behavior. The latest app source, APK and end-to-end test results have not been independently reverified for this report.')
para('Latest decisions take precedence','SubApp')
para('English interface and receipt headings; Urdu supported in product names. Receipt Preferences removed. Three current Pigmy schemes with future additions allowed. Deposits and interest-free withdrawals linked to Cash In/Out. Manual Google Drive / Files backup saving. Category creation and optional transaction photos. Preserve existing records through all upgrades.')
para('Report contents','SubApp')
para('1. Product scope and design\n2. Inventory and imports\n3. Billing and receipts\n4. Accounts and cash\n5. Pigmy savings\n6. Services\n7. Reports and profit\n8. Backup and recovery\n9. Two-phone operation\n10. Testing and distribution\n11. Evidence, limitations and open decisions\nAppendix: detailed latest implementation addendum')

section('1. Product scope and simple design', '''Ali Educational is an Android shop-management app for stationery and related shop goods. Stock, customer money owed, supplier dues and fast billing are central functions. Printing services and financial reporting extend the same records rather than creating disconnected systems.

The Owner phone is the main device. Core work is local and should remain usable offline, with no desktop required. A second phone may use the same app in a restricted Billing role. The design avoids separate billing implementations and unnecessary online logins.

The visual identity uses the supplied Ali logo and Ali Educational name. Dark charcoal/black with gold accents is the default direction; an optional light theme is retained. Large readable controls, simple labels and reachable actions matter more than decoration.

Settings uses a gear icon. Recent Activity is removed from Home, but underlying stock movements and audit history remain. The payment-screen quick buttons must no longer break words and amounts into vertical fragments. Forms must work with the keyboard open and larger system fonts.

The latest language decision removes the Urdu interface and Urdu receipt translation. App labels and receipts are English. Product names entered in Urdu or mixed scripts must remain intact in storage, search, bills, PDFs, imports, exports, sync and backups. Do not change the whole interface to RTL to display one product name.

Local fingerprint/device credential protection remains the preferred access model. Cold launch, resume and configured lock timing need testing. Pairing identifies a device role; it is not an online account or an unrestricted role-switch button.''')
section('2. Products, stock and spreadsheet entry', '''### Current Add Stock & Product form

The supplied screenshots show Product Name, optional Barcode, Category, Brand, Pcs, Low Stock Alert, Base Cost per piece, Selling price per piece, Margin (%), Wholesale price per piece, Supplier Name, Bill No. and Transport Charges. The sample workbook was adapted to these fields. The precise Margin calculation must be checked in the app; the test sheet leaves it blank rather than assuming margin equals markup.

Products may have variants, categories/subcategories, optional photos and barcodes. Products without a barcode remain selectable through search, categories and favorites/recent products. Camera scanning uses existing product barcodes. No short-code system was requested.

Pieces and packs share underlying stock. Pack sizes can vary, and independent piece/pack prices are supported by the agreed plan. Wholesale per-piece price is not a pack price. Barcode lookup must respect the mapped unit and exclude stale or inappropriate archived mappings.

Editing a product name or selling price must not add its existing stock again. Receive additional stock, edit product details and correct a physical count are separate operations. Preserve history of quantity changes, damage, shop use and giveaways. Low-stock thresholds support defaults and product overrides.

### Receiving and cost

Record received quantities, supplier, invoice details, purchase cost and delivery charges. Allocate delivery proportionally to purchase values with exact total paise preserved. Weighted-average costing is the agreed policy. For ₹500 stock plus ₹50 transport, total received inventory cost is ₹550. Historical sale costs must be preserved as snapshots.

Supplier balances must reflect the real liability; do not automatically assume every transport charge is payable to the supplier. Partial supplier payments and optional invoice/payment-proof attachments belong to the corresponding transaction history.

### Excel import and validation

Use file selection, sheet choice, mapping, preview, validation and explicit confirmation. Support advertised XLS/XLSX/CSV formats and content providers. Reject corrupt, empty, wrong-type and inaccessible files with useful messages rather than crashes. Preserve leading-zero barcode text, exact currency, optional blanks and Urdu names.

Import must distinguish catalog updates, counted stock and additional receipts. Do not repeatedly add opening stock when the same file is retried. Detect duplicates, preserve batch history and avoid partial silent changes. Unsupported fields must be disclosed in preview, not mapped to unrelated columns.

The two original October XLS references were cash ledgers with Date, Notes, Description / Category, Income, Expense and Balance headings on row 5. They are not stock files. Product import should direct the user to Cash In/Out for those files.

The new synthetic workbook has 12 products and 372 pieces. Base cost totals ₹3,458; transport ₹117; landed stock cost ₹3,575; listed retail value ₹5,591; expected gross profit ₹2,016. Only Products is an import sheet. Instructions and Expected Results are checks, not import data. These figures are sample data, not real shop balances.''')
section('3. Billing, payments and receipts', '''Billing is now a major app function. Select by camera barcode, search, categories or favorites; adjust quantities and current-bill prices. Catalog price edits are separate from bill-only overrides. Hold and resume multiple drafts without deleting their only durable copy. Service-only and mixed bills should use the same checkout.

Whole-bill percentage or amount discounts remain distinct from unpaid customer credit. A ₹100 bill settled at ₹90 by discount is a ₹90 sale with ₹10 discount. If ₹10 is still owed, the sale remains ₹100 and requires a customer-linked balance. Do not create unassigned walk-in debt.

Cash Received must be directly editable for any valid rupee amount, including decimals. Exact sets the payable amount. Increment buttons and fixed-amount buttons must have unambiguous labels. A ₹48 bill with ₹50 cash returns ₹2 change; ₹47.50 requires explicit shortfall handling. Preserve tendered cash, applied payment and change separately.

Support the agreed cash/digital/credit and split-payment workflows with optional digital references. Saving twice must not duplicate a sale. Pack billing must use selected-unit prices correctly: one pack of ten at ₹100 is a ₹100 line and ten pieces out, not ₹1,000.

Returns, exchanges and cancellations need linked reversals, correct refund/credit choices and exactly-once stock effects. Later debt repayments must be considered when reversing a sale. Damage and saleable returns must not subtract sold stock a second time.

### Latest receipt layout

Receipt Preferences is removed from Settings. Receipt actions belong on the saved-bill preview: WhatsApp/share, Save PDF and actual printing. Old automatic sharing/printing preferences must not trigger unexpectedly after upgrade.

Use a clean monochrome Ali logo on white, separate logo/title/subtitle spacing, aligned metadata, item columns and numeric totals. Wrap long descriptions and paginate long bills. Divider lines must not cross text. Show original receipt number and date from the selected persisted bill.

Remove the visible Not GST registered statement and any retain/keep-the-bill instruction. This is a presentation change, not permission to add GST, invent a GSTIN or label the document a GST tax invoice.

The latest reference receipt is AE-M-20261101-8261 dated 01 Nov 2026, 01:22 pm. Photocopying is 1 copy at ₹2; show ₹2 gross line amount, ₹2 subtotal, ₹1 discount (50%), ₹1 payable, ₹1 paid and ₹0 due. The supplied PDF used an ambiguous discounted line amount and had rules crossing the totals. A corrected visual reference and layout notes were created for Antigravity.

WhatsApp must receive a real valid PDF attachment with a readable content URI and permission. Preserve files long enough for the recipient app to read them. The user selects/confirms the recipient; opening WhatsApp does not prove delivery. A missing app or cancelled chooser must not change the saved bill. Do not send supplier costs, profit or private evidence with customer receipts.''')
section('4. Accounts, Cash In/Out and giveaways', '''Maintain customer and supplier names/details, outstanding balances and partial payments. Existing opening balances require explicit user input/import review. Customer repayments settle debt and are not new sales. Supplier payment is not automatically a new operating expense. Archive actions must not hide outstanding liabilities or erase history.

Cash In/Out needs explicit effective date and time on both directions, with calendar/time pickers and preserved values when editing. Store created/updated audit times separately. Backdated transactions must appear in the correct day/month and export. The exact policy for future-dated completed entries should be resolved rather than silently accepting them as current cash.

### Categories and photo evidence

Each manual cash transaction has a dedicated Category selector separate from description/notes. Choose an existing category or create a new one without losing the form. Use stable IDs, duplicate-name validation and rename/archive rather than deleting referenced categories. Category/date filters and totals must reconcile.

Pigmy-linked deposits and withdrawals automatically use the Pigmy category and retain the individual scheme identity. A category is an organizational label, not the transaction accounting type. Selecting Pigmy manually must not fabricate a scheme transaction; guide the user to the linked workflow.

An optional photo can be captured or chosen from the gallery, previewed, replaced or removed. Copy it into durable managed storage; a temporary picker path is not permanent evidence. Failed processing must preserve the form and let the user explicitly retry or save without the photo. Editing evidence must not duplicate or change the financial entry.

Backup must include actual photo contents and relationship IDs. Restored photos must open on a new installation without access to the original phone path. Standalone Excel exports may show category and attachment presence but must not pretend to contain all photos. Owner-only financial evidence must not leak to restricted billing devices.

### Cash-ledger spreadsheets

Preserve Notes when category is blank. Validate dates and currency; do not replace malformed dates with today. Skip titles, repeated headers, totals and closing-balance rows as transactions. Opening balance needs separate reviewed treatment. Warn about repeat/overlapping imports and ambiguous matches. Export the selected period only; an empty filter must not export everything.

### Free products and donations

Products only; cash donations were not requested. Record product, quantity/unit, effective date, Free item or Donation reason, optional recipient/note and cost snapshot. Reduce stock once without sales income or debt. Show known giveaway costs separately from paid-sale COGS, and mark unknown cost. Reversal restores stock once and preserves history.''')
section('5. Pigmy savings: schemes, deposits and withdrawals', '''### Expandable schemes

There are three schemes today, but the app must allow the Owner to add more. Each has a name, stable ID, usual deposit amount, separate history and totals. Show a combined summary and readable scrollable cards/list rather than squeezing all schemes horizontally. Rename/archive/reactivate while preserving historical records and links.

Replace the piggy-bank icon with a Bitcoin-style coin icon as requested. This is visual only: labels remain Pigmy savings and money stays in rupees. It introduces no cryptocurrency wallet, trading or conversions. Accessibility labels should describe Pigmy savings.

### Deposits

Prefill the scheme's usual amount, allow changes and save only after confirming actual payment. Do not auto-post every day or mark all schemes paid when one is paid. Record date/time, funding source and optional note/reference. A cash deposit creates exactly one linked Cash Out savings transfer, atomically with the deposit.

Bank-funded deposits must not reduce physical cash. Edit/cancel updates or reverses the existing linked transfer once. Retry, restart and applicable sync must preserve exactly-once effects. The category is Pigmy, with a separate scheme name/ID. Savings deposits do not reduce operating profit as expenses.

### Withdrawals without interest

The brother receives his deposited money back with no interest. Add Record withdrawal in each scheme, with amount, effective date/time, destination (shop cash or bank) and optional reference/note. A cash withdrawal creates a linked Cash In savings transfer; a bank receipt affects that destination, not physical cash. It is neither sales revenue nor profit.

Show Deposits, Withdrawals and Tracked balance. Balance is confirmed opening savings plus active deposits minus active withdrawals. Separate period movement from closing balance. Historical missing deposits must not be invented; any opening balance needs explicit amount/date confirmation and creates no new cash outflow.

For complete recorded savings of ₹10,000, withdrawing ₹3,000 leaves ₹7,000. Editing that withdrawal to ₹2,500 leaves ₹7,500; cancelling restores ₹10,000 and reverses the cash receipt once. An amount above tracked funds requires review of missing history/opening balance before posting, not fabricated balancing entries.

### Preservation and acceptance

Old single-scheme data must not be copied into each new scheme. Preserve IDs and links; unknown attribution remains visible for Owner review. Backup/restore covers all schemes, deposits, withdrawals, openings, audit history and attachments. Add fourth/fifth schemes in testing and verify individual/combined totals, archive visibility and linked cash effects.

Synthetic test: deposits ₹100/₹200/₹300 yield ₹600 and three outflows. Edit the second to ₹250: ₹650. Cancel the third: ₹350. No change to sales/profit. Test withdrawals separately and confirm neither retry nor recovery replays cash movements.''')
section('6. Printing services and Owner expense reporting', '''Initial services are Printing, Photocopying and Sticker printing, plus Add service for reusable definitions. Printing options are A4 B&W and A4 Colour. Each new job requires its own entered rate; no default or last-used rates. Existing draft/held-line rates remain when editing that specific job.

The service definition stores identity, name and unit, not stock or a selling rate. Switching printing options clears an entered rate to avoid using a B&W price for colour. Quantity units must be explicit; sides, sheets, copies and stickers are not interchangeable. Photocopy/sticker charging units still require confirmation where not already resolved in implementation.

Services may share a bill with products. Only product lines deduct inventory; services do not automatically consume paper, ink or toner. Reversing a service does not restore product stock. Preserve service name, option, quantity, unit, entered rate and discount allocation as historical snapshots.

Example: products ₹160 and services ₹40 with 10% whole-bill discount produce ₹180 payable, ₹144 product revenue and ₹36 service revenue. Round line discount allocations to exactly the bill discount.

Only the Owner manages service expenses and accesses the service financial overview. Use one expense ledger with classification/target fields. Categories include paper, ink/toner, sticker sheets, repair/maintenance and other. Target a service or Shared services, with amount, date, note and optional evidence.

Show net service revenue, recorded service expenses and revenue minus recorded expenses. The difference is not exact service profit or available cash: supplies may span periods and sales can be unpaid. Do not allocate shared costs arbitrarily or count a reclassified expense twice.

Test credit repayments not becoming new service revenue, refunds not reducing revenue twice, void/edit history, no default rates, unauthorized Billing expense access, offline persistence, exports and backup. The earlier handoff includes 30 detailed service reporting/expense scenarios, all still applicable to implemented scope.''')
section('7. Sales & Profit and analysis', '''The recommended report entry point is Sales & Profit rather than an incomplete Gross Profit label. Keep product sales, cash collections, product COGS, product gross profit, operating expenses, services and savings transfers distinct.

Net product sales are itemized revenue after discounts/returns. Gross profit subtracts the corresponding saved cost snapshots. Missing costs must produce incomplete/known-cost labels rather than zero-cost assumptions and apparently exact 100% margins.

Expected stock profit is separate from actual profit realized through sales. It depends on current sellable quantities, selling prices and known landed costs. Negative stock or missing costs must not produce misleading projections. Weighted-average costing and proportional delivery allocation remain consistent across receiving and reports.

Daily summaries need explicit interpretation as additional unbilled sales or a separate summary. Never count a full-day total plus all its underlying bills twice. Later customer repayments are collections, not additional revenue. Supplier settlements, Pigmy transfers and inventory purchases are not all ordinary operating expenses.

Use consistent Today/Week/Month/custom ranges and daily/weekly/monthly graph grouping. Apply identical filters to cards, graphs, drill-downs, exports and incomplete-data warnings. Giveaways show separate known costs. Service comparison remains labelled as revenue minus recorded expenses, not exact net profit.

When the second phone has pending or unknown sync state, clearly indicate report incompleteness. A disconnected device's pending count cannot honestly be displayed as confirmed zero. Test boundary dates, backdated entries, changed catalog prices/costs, reversals, unknown cost and exported reconciliation.''')
section('8. Backup Now, Google Drive and recovery', '''Backup Now previously had a user-reported nonworking action. Verify the full button-to-service-to-file-to-result path. Show progress, avoid concurrent jobs, refresh the backup list and update last successful backup only after real validation. Failures need actionable errors and retry; do not leave a permanent spinner or false success.

Back up a consistent database snapshot including committed WAL data, not a casually copied live DB. Include products, stock history, bills/lines, accounts/payments, expenses/cash, services, Pigmy, categories, held bills, relevant settings and actual attachments/branding. Use a manifest with true format/schema/version details and checks, not decorative Verified labels.

The observed backup screenshot showed ali_educational.db and manifest.json. Those filenames and a small file size are not proof of completeness or failure. Records can be compact, but photos/settings may require extra contents. The current archive contents must be inspected and restored to establish what is actually covered.

### Confirmed Drive policy

Keep manual on-demand Save Copy to Google Drive / Files. Create/validate a local backup, then let the user select an available provider/account/folder using Android's native picker. No unattended OAuth-based background upload work for this release.

Distinguish local backup creation from successful provider write and verified cloud delivery. Drive may queue an upload while offline. A picker/share launch alone proves neither saving nor upload. Show available filename/destination information and separate last-local-backup and last-external-save states. Provide a simple reminder to save an external copy, especially before updates.

Keep dated copies and retain the valid local archive when external saving fails. Handle cancellation, missing Drive, revoked permission, storage limits, offline behavior and interrupted writes. Do not silently delete older backups or invent cloud paths.

### Restore safety

Validate archive structure, schema compatibility, integrity and attachment coverage in staging before touching live data. Reject unsafe paths, missing critical contents and unsupported versions. Require confirmation, preserve a consistent pre-restore recovery copy and reopen the database safely. Failures must leave prior data recoverable.

Sync and backup differ. Owner backup includes accepted bills, not unknown unsent bills on the other phone. Restoring requires reconciliation of event IDs/acknowledgements, not blind replay or overwriting another device role. Sensitive authentication/signing material is not ordinary backup content.''')
section('9. Two phones, one app', '''Use one shared Android app and compatible signed APK with Owner and Billing roles. Both phones are on the same shop Wi-Fi; no desktop server or remote-location sync is part of this phase. Optional capability stays off until explicitly paired and tested. Owner workflows must continue when the second phone is absent.

Pair through an Owner-approved short-lived invitation and authenticated encrypted connection. Send only role-appropriate catalog/data, not supplier costs, service expenses or full Owner backups. Billing must not promote itself through an unprotected selector.

Billing may override current-bill prices and whole-bill discounts but cannot change saved catalog prices. Permission for customer creation, credit, repayments, returns/cancellations, reusable-service creation and visibility of other bills remains to be confirmed where not resolved later.

Offline billing saves completed bills and outbound events atomically, keeps durable pending history and delivers on reconnection. Use stable unique event/transaction IDs; retries may repeat messages but must apply stock/money effects once. Acknowledgements occur after commit. Updated stock must not deduct an already acknowledged local sale again.

Preserve original timestamps, prices, units and receipt numbers. Show last-known stock while offline and retain both real sales if the last item was sold on each device; flag shortage rather than discarding a bill. Handle changed/archived products and revoked devices with visible review states.

Delayed offline cost assignment needs a documented policy, not an unsupported exact historical-profit claim. Invoice series must avoid offline collisions and survive backup/replacement. Test interruption at local save, remote commit and acknowledgement boundaries, plus screen-off, battery restrictions, changing addresses and device restart.

Two real phones are required for final connectivity/lifecycle evidence. If only one is available, mark two-phone hardware behavior unverified instead of claiming full completion.''')
section('10. Final testing and release handoff', '''### Test fixtures and independent checks

Use synthetic spreadsheets in a separate app/database. The latest generated workbook is for adding products/stock. Test valid data, bad files, duplicates, malformed dates/currency, leading-zero identifiers and optional fields. Do not silently import test records into the brother's real catalog.

Perform actual transactions after import: sales, discounts, credit repayments, supplier payments, services, Pigmy deposits/withdrawals, giveaways and photos. Record an expected-value manifest with IDs, quantities, amounts and relations. Create a backup via the real UI, save/retrieve externally and restore into another disposable installation. Compare contents, not just record counts or ZIP size.

Rehearse migration from supported old schemas without destructive fallback. Test storage exhaustion, interruption, failed restore, missing attachments and corrupt archives. Protect the current database and validate recoverability before proposing a live update.

### Evidence and acceptance gate

Test actual release-equivalent/minified Android behavior for PDF fonts/logo, file pickers, content URI sharing, camera, lock and backup. JVM tests are useful but cannot prove platform compatibility. Preserve expected/actual outcomes, build/device details, screenshots/documents and pass/fail/unverified status per scenario.

Do not release with critical crashes, data loss, unexplained stock/money discrepancies or failed recovery/migration. Rerun affected checks after fixes. A completed checklist or successful build is not feature evidence. Review exact artifact identity and any differences from the tested variant.

### Build and share

Produce one signed release APK, compatible with the existing application ID/signing key and incremented version code. Exclude private reference data, test records, debug tools and secrets. Provide filename, version, size, SHA-256, release notes, setup/pairing instructions, test results and limitations.

Ask explicit approval before updating the production device. Never uninstall or clear data to bypass a signature/migration problem. Older APK installation is not a safe automatic rollback after a database upgrade; recovery must account for records created since the backup.

The APK can be downloaded locally and shared as a file using an appropriate channel, including the user's intended WhatsApp document flow where supported. A local workspace link is not a public link for the brother. Do not fabricate a hosted download URL or upload publicly without authorization. Send the APK only, not signing keys/passwords. After an approved update, use nondestructive smoke checks.''')
section('11. Evidence and remaining decisions', '''### What this conversation actually produced

Detailed Markdown handoffs, the combined two-phone/service specification, repair/test plans, the corrected receipt reference image and layout notes, and a synthetic product-stock XLSX workbook. Earlier isolated source review and limited JVM tests identified defects; those historical checks are not evidence that the later Antigravity-built app is fixed.

The latest supplied screenshots show Home/Pigmy and payment/stock-entry layouts. The user confirmed Pigmy Daily opened nothing at that time and reported receipt/backup issues. Later user reports indicate implementations progressed, but this PDF does not independently certify the current build.

The AE-M-20261101-8261 PDF was rendered and inspected. Logo spacing and intersecting rules were observed, and the discounted item amount was ambiguous relative to subtotal. A visual layout reference was generated; it is not a production receipt renderer or proof that WhatsApp output was repaired.

The stock workbook totals were calculated and its expected-results rendering checked. It has not been imported into the latest app by this report-generation task. Current importer support for supplier, wholesale and transport columns still needs preview/device verification.

### Questions that must not be silently invented

Confirm unresolved Billing-role permissions, exact service units, receipt series, delayed offline-sale costing and any future-dated cash policy. Confirm historical opening savings before creating an opening balance. Existing source may resolve some choices; inspect before asking the user again.

Optional local AI was discussed earlier but remains lower priority and outside the current release work: read-only answers/explanations, optional activation and no external shop-data transmission. No current AI implementation or quality is claimed.

### How to use this report

Use Sections 1–10 as a consolidated feature/update review. The appendix retains the detailed latest addendum so Antigravity has exact category/photo, Pigmy withdrawal, manual Drive and receipt/icon requirements. Latest decisions override older deposits-only, fixed-three-schemes or bilingual-interface wording retained in historical specifications. Request build-specific evidence before treating any requirement as completed.''')

section('Appendix — Detailed latest implementation addendum','The following text is retained from ANTIGRAVITY_LATEST_CHANGES_AND_BACKUP_DECISION.md. It is an implementation/test specification, not a verified completion report. Later override sections take precedence over older scope wording.')
text=(root/'ANTIGRAVITY_LATEST_CHANGES_AND_BACKUP_DECISION.md').read_text(encoding='utf-8-sig')
for line in text.splitlines():
    line=line.strip()
    if not line:continue
    if line.startswith('#'):para(line.lstrip('# ').strip(),'SubApp')
    elif re.match(r'^[A-Z]:/',line) or line.startswith('`C:'):continue
    else:para(line.replace('`',''))

def footer(c,d):
    c.setStrokeColor(colors.HexColor('#D4C6A7'));c.line(42,37,553,37)
    c.setFont('AppArial',8);c.setFillColor(colors.HexColor('#666666'))
    c.drawString(42,24,'Ali Educational | Consolidated review | 01 Nov 2026')
    c.drawRightString(553,24,str(d.page))
path=out/'Ali_Educational_Detailed_Features_and_Updates.pdf'
SimpleDocTemplate(str(path),pagesize=(595.28,841.89),rightMargin=42,leftMargin=42,topMargin=42,bottomMargin=52,title='Ali Educational - Detailed Features and Updates',author='Ali Educational').build(story,onFirstPage=footer,onLaterPages=footer)
print(path)
