from pathlib import Path
import re, hashlib, textwrap, xml.etree.ElementTree as ET
from xml.sax.saxutils import escape
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Preformatted
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
root=Path(r'C:\Users\Musid\.gemini\antigravity\scratch\AliEducational')
workspace=Path.cwd()
out=workspace/'output/pdf/Ali_Educational_Complete_System_Reference_2026-11-07.pdf'
pdfmetrics.registerFont(TTFont('Body',r'C:\Windows\Fonts\arial.ttf'))
pdfmetrics.registerFont(TTFont('Code',r'C:\Windows\Fonts\consola.ttf'))
styles=getSampleStyleSheet()
styles['Normal'].fontName='Body';styles['Normal'].fontSize=9;styles['Normal'].leading=13
styles.add(ParagraphStyle(name='CodeSmall',fontName='Code',fontSize=6.5,leading=8.4))
story=[]
def p(t):story.append(Paragraph(escape(t),styles['Normal']));story.append(Spacer(1,6))
def h(t):story.append(Paragraph(escape(t),styles['Heading1']));story.append(Spacer(1,8))
def code(t):
 lines=[]
 for n,line in enumerate(t.splitlines(),1):
  parts=textwrap.wrap(line.expandtabs(4),width=108,replace_whitespace=False,drop_whitespace=False) or ['']
  lines += [f'{n:5}  '+parts[0]]+['       '+x for x in parts[1:]]
 for i in range(0,len(lines),48):story.append(Preformatted('\n'.join(lines[i:i+48]),styles['CodeSmall']));story.append(Spacer(1,5))
files=sorted([f for f in (root/'app/src').rglob('*') if f.suffix in ('.kt','.xml')])
files += [root/'app/build.gradle.kts',root/'build.gradle.kts',root/'settings.gradle.kts',root/'gradle/libs.versions.toml',root/'app/proguard-rules.pro']
files=[f for f in files if f.exists()]
h('ALI EDUCATIONAL')
p('Complete system reference | Source snapshot: 7 November 2026')
p('Features, architecture, data model, build and test evidence, approved specifications and complete text-source appendix.')
p('This report documents the current project found on disk. Source configuration: version 1.0.12 (code 12), Room schema 8, minimum Android API 26, compile/target API 36, JVM target 17. Installed phone version and current device behavior were not inspected in this task.')
p('Evidence levels: SOURCE PRESENT means code exists; STORED RESULT means an existing test report was found; SPECIFICATION means intended behavior, not proof of implementation. No app tests or build were rerun for this documentation task.')
p('Source location: '+str(root))
p(f'Text source appendix: {len(files)} files. Includes Kotlin implementation/tests, Android XML and selected build configuration. Binary assets, generated build internals, dependency code, signing keys, keystore.properties and local.properties are intentionally not reproduced. This is a reference PDF, not a buildable source checkout.')
h('Reading guide')
p('Part I: architecture and current feature map. Part II: database and code inventory. Part III: existing build/test evidence and limitations. Part IV: detailed approved handoffs. Part V: line-numbered implementation, resource and test code. PDF bookmarks identify major sections and each source file.')
story.append(PageBreak());h('Part I - System architecture')
p('The project uses Kotlin and Jetpack Compose/Material 3 for Android screens, Navigation Compose for routes, ViewModels and coroutine Flow state, Hilt dependency injection, Room/SQLite persistence and DataStore preferences. Apache POI handles spreadsheets; CameraX and ZXing support barcode scanning. Android biometric APIs protect entry. A native RemoteViews-style launcher widget has separate XML resources and a provider.')
code('Android launcher / MainActivity / slim widget\n    -> authentication and route selection\n    -> Compose screens and navigation\n    -> AliViewModel / MainViewModel\n    -> AppRepository and domain calculations\n    -> Room DAOs -> SQLite entities and migrations\n    -> DataStore preferences / managed attachment files\n\nService boundaries:\n  ReceiptService -> customer PDF / sharing\n  ImportExportService + CashTrackerExcelService -> spreadsheets\n  BackupRestoreService -> archive / staging / restore\n  ShopRecordsExcelGenerator -> readable workbook\n  TwoPhoneSyncService -> local network communication')
p('This diagram describes architectural responsibilities inferred from the source organization. Individual screens/services may access data directly; the complete source appendix is authoritative for exact call paths.')
areas=[('Inventory','Products, barcodes, receipt of stock, removals, imports, cost calculations and stock history. Inspect ui/stock, CostCalculator and repository.'),('Billing','Cart, customer selection, payment, held bills, receipt, history and barcode analyzer/screens are present under ui/billing.'),('Accounts','Customer/supplier account UI and payment persistence are present. Shared financial identity and allocation behavior require transaction-level verification.'),('Cash and expenses','CashTrackerScreen, ExpensesScreen, cash categories and shop expense records support financial entry/reporting. Do not equate all inflows with revenue or outflows with expenses.'),('Pigmy','PigmySavingsScreen and scheme/deposit/withdrawal entities are present. Savings are transfers, not sales/profit.'),('Reports','ProfitScreen and CostCalculator contain reporting/calculation code. Exact definitions and shared-expense allocation must be checked against approved requirements.'),('Backup','BackupRestoreService and ShopRecordsExcelGenerator exist alongside backup UI. Their presence does not certify that every setting/file is recoverable.'),('Widget','AliSlimBarWidgetProvider and widget_slim_bar.xml exist with dedicated cash-in/out, bill and stock drawable resources.'),('Appearance and access','Theme/color/type resources, setup/settings screens and BiometricAuthManager are present. Actual light-mode contrast and lock lifecycle need device evidence.'),('Two-phone operation','TwoPhoneSyncService uses ServerSocket/Socket and HttpURLConnection imports. Do not describe it as verified authenticated encrypted synchronization merely from its presence; transport and role enforcement require review.')]
for title,desc in areas:h(title);p(desc)
h('Financial relationships and required invariants')
for t in ['A product purchase receives stock and creates supplier liability only for unpaid consideration. Later supplier settlement changes cash/bank and payable, not stock again.','A completed customer bill records revenue and saved cost; unpaid value becomes customer due. Later repayment settles due and is not new revenue.','Product gross profit = net product sales minus associated saved COGS. Service revenue and recorded operating expenses must remain explicitly classified.','Cash balance, profit, customer dues, supplier dues, Pigmy savings and expected stock profit are distinct measures. Each must reconcile independently.','Refunds/reversals need links to original records, correct effective dates and exactly-once effects. Backup restore must not replay historic postings.']:p(t)
story.append(PageBreak());h('Part II - Data model and source inventory')
ent=root/'app/src/main/java/com/alieducational/app/data/local/entity/Entities.kt'
txt=ent.read_text(encoding='utf-8')
p('Entity declarations found in Entities.kt:')
p(', '.join(re.findall(r'data class (\w+)',txt)))
p('The complete field definitions, nullability, defaults, indexes and Room annotations are reproduced in the source appendix. The database declaration includes 20 entities and schema migration code. SQL relationships must be distinguished from application-only links; do not infer enforced foreign keys from field names alone.')
for f in files:
 rel=str(f.relative_to(root));t=f.read_text(encoding='utf-8',errors='replace')
 p(f'{rel} | {len(t.splitlines())} lines | SHA-256 {hashlib.sha256(f.read_bytes()).hexdigest()}')
story.append(PageBreak());h('Part III - Existing build and test evidence')
for f in sorted((root/'app/build/outputs').rglob('*.apk')):
 p(f'{f.relative_to(root)} | {f.stat().st_size:,} bytes | SHA-256 {hashlib.sha256(f.read_bytes()).hexdigest()}')
p('APK presence and hash establish artifact identity only. This report does not establish that either APK matches every current source file or is installed on the shop phone.')
reports=list((root/'app/build/test-results').rglob('TEST-*.xml'))
for f in reports:
 try:
  e=ET.parse(f).getroot();p(f'{f.relative_to(root)}: tests={e.get("tests")}, failures={e.get("failures")}, errors={e.get("errors")}, skipped={e.get("skipped")}, recorded timestamp={e.get("timestamp","not supplied")}')
 except Exception as e:p(str(f)+': could not parse report '+str(e))
p('Stored reports may be stale or from a different build. Unit tests do not prove actual phone rendering, Files/Drive behavior, printing, restore completeness, biometric behavior or two-device lifecycle. Test source is included below for examining what was actually asserted.')
h('Known evidence limitations and release checks')
for t in ['No production device was connected or updated during this report. No claim is made that all approved requirements have been implemented correctly.','The earlier supplied backup had a database, manifest and one photo; ZIP integrity passed, but database integrity/complete restoration was not established.','The user reported broken Home Bill History navigation, weak light-mode contrast, Accounts placement problems and widget mismatch on 1 November. Current source may include fixes; fresh device evidence is needed to close those reports.','Before release, reconcile the approved synthetic golden scenario, restore an externally saved backup on an isolated installation, verify signing/migration compatibility, and compare exact candidate APK identity.']:p(t)
story.append(PageBreak());h('Part IV - Detailed approved specifications')
p('The following handoffs preserve detailed decisions, workflows, examples and acceptance tests. Their imperative language describes requested work. It does not establish implementation or test completion. Later explicit decisions override earlier proposals.')
for name in ['ANTIGRAVITY_LAYOUT_GRAPHS_SUPPLIERS_AND_SHOP_EXPENSES.md','ANTIGRAVITY_SLIM_WIDGET_AND_FINAL_RELEASE_TESTING.md','ANTIGRAVITY_BACKUP_PACKAGE_HISTORY_LIGHT_MODE_AND_BUILD.md']:
 h(name)
 for line in (workspace/name).read_text(encoding='utf-8').splitlines():
  if not line.strip():continue
  if line.startswith('#'):story.append(Paragraph(escape(line.lstrip('# ')),styles['Heading2']))
  else:p(line)
story.append(PageBreak());h('Part V - Complete text-source appendix')
p('Paths are relative to the source project. Line numbers refer to the original file; continuation lines are indented. Text is reproduced for inspection, including source comments whose claims are not independently certified.')
for f in files:
 story.append(PageBreak());h(str(f.relative_to(root)))
 code(f.read_text(encoding='utf-8',errors='replace'))
class Doc(SimpleDocTemplate):
 def afterFlowable(self,flow):
  if isinstance(flow,Paragraph) and flow.style.name=='Heading1':
   key='section'+str(self.seq.nextf('section'));self.canv.bookmarkPage(key);self.canv.addOutlineEntry(flow.getPlainText(),key,0)
def footer(c,d):
 c.setFont('Helvetica',8);c.setFillColor(colors.HexColor('#60666b'));c.drawString(36,20,'Ali Educational | Source reference | 7 November 2026');c.drawRightString(559,20,str(d.page))
doc=Doc(str(out),pagesize=(595,842),rightMargin=36,leftMargin=36,topMargin=36,bottomMargin=38,title='Ali Educational - Complete System Reference',author='Ali Educational project documentation')
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print(out)
import fitz
d=fitz.open(out);print('PAGES',len(d))
for i in [0,1,10,len(d)//2,len(d)-1]:d[i].get_pixmap(matrix=fitz.Matrix(1,1)).save(str(workspace/f'tmp/pdfs/review-{i+1}.png'))
