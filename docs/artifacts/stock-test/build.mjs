import fs from 'node:fs/promises';
import {createRequire} from 'node:module';
const require=createRequire('C:/Users/Musid/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/package.json');
const {Workbook,SpreadsheetFile}=await import((await import('node:url')).pathToFileURL(require.resolve('@oai/artifact-tool')).href);
const wb=Workbook.create(); const s=wb.worksheets.add('Products'); const n=wb.worksheets.add('Instructions');
const heads=['Name','Category','Brand','SKU','Barcode','Base Unit','Pack Size','Opening Qty','Purchase Cost','Selling Price Piece','Selling Price Pack','Low Stock'];
const data=[
['TEST Notebook A4','Notebooks','Test Brand','TEST-001','0012345678905','piece',1,20,50,80,80,5],
['TEST Blue Ball Pen','Pens','Test Brand','TEST-002','','piece',10,100,5,8,75,20],
['TEST Black Ball Pen','Pens','Test Brand','TEST-003','','piece',10,50,5,8,75,10],
['TEST HB Pencil','Pencils','Test Brand','TEST-004','','piece',12,60,3,5,55,12],
['TEST Eraser','Accessories','Test Brand','TEST-005','','piece',20,40,2.5,5,90,10],
['TEST Sharpener','Accessories','Test Brand','TEST-006','','piece',10,30,4,7,65,10],
['TEST Ruler 30 cm','Accessories','Test Brand','TEST-007','','piece',1,15,10,15,15,5],
['TEST Glue Stick','Adhesives','Test Brand','TEST-008','','piece',1,10,18.5,30,30,3],
['TEST Colour Pencils Box','Art','Test Brand','TEST-009','','piece',1,8,60,95,95,3],
['TEST File Folder','Files','Test Brand','TEST-010','','piece',1,25,12,20,20,5],
['TEST قلم','Pens','Test Brand','TEST-011','','piece',1,12,10.25,15.5,15.5,5],
['TEST Sketchbook','Notebooks','Test Brand','TEST-012','','piece',1,2,35,55,55,5]
];
const headers=['Product Name','Barcode','Category','Brand','Pcs','Low Stock Alert','Base Cost (INR/pc)','Selling (INR/pc)','Margin (%)','Wholesale (INR/pc)','Supplier Name','Bill No.','Transport Charges (INR)'];
const freight=[50,25,0,12,0,0,0,10,0,20,0,0];
const rows=data.map((r,i)=>[r[0],r[4],r[1],r[2],r[7],r[11],r[8],r[9],null,Math.round((r[8]+r[9])/2*100)/100,'TEST Supplier '+(i%2+1),'TEST-BILL-'+String(i+1).padStart(3,'0'),freight[i]]);
s.getRange('A1:M13').values=[headers,...rows];
s.getRange('B2:B13').setNumberFormat('@'); s.getRange('G2:H13').setNumberFormat('0.00'); s.getRange('J2:J13').setNumberFormat('0.00'); s.getRange('M2:M13').setNumberFormat('0.00');
s.getRange('A1:M1').format={fill:'#A67C20',font:{bold:true,color:'#FFFFFF'}}; s.getRange('A1:M13').format.autofitColumns();
const check=wb.worksheets.add('Expected Results');
check.getRange('A1:G1').values=[['Product','Pieces','Base stock cost','Transport','Landed stock cost','Retail value','Expected gross profit']];
for(let i=0;i<rows.length;i++){let r=i+2;check.getRange(`A${r}:G${r}`).formulas=[[`=Products!A${r}`,`=Products!E${r}`,`=Products!E${r}*Products!G${r}`,`=Products!M${r}`,`=C${r}+D${r}`,`=Products!E${r}*Products!H${r}`,`=F${r}-E${r}`]];}
check.getRange('A14').values=[['TOTAL']]; for(const c of ['B','C','D','E','F','G'])check.getRange(`${c}14`).formulas=[[`=SUM(${c}2:${c}13)`]];
check.getRange('C2:G14').setNumberFormat('#,##0.00');check.getRange('A1:G1').format={fill:'#A67C20',font:{bold:true,color:'#FFFFFF'}};check.getRange('A14:G14').format.font={bold:true};check.getRange('A1:G14').format.autofitColumns();
const notes=[['TEST DATA ONLY','Matches the supplied Add Stock & Product form.'],['Import sheet','Select Products only. Do not import Instructions or Expected Results.'],['Quantity','Pcs means individual pieces; no pack quantities in this sample.'],['Prices','All amounts are INR. Base Cost and Selling are per piece.'],['Margin (%)','Intentionally blank: the screenshot does not establish whether this field means margin or markup. Use the explicit selling price.'],['Transport','Total freight for that row/bill, not per piece. Each row has a distinct synthetic bill number.'],['Supplier','Synthetic names only; use a separate test database to avoid changing real dues.'],['Wholesale','An example separate per-piece price; not a pack price.'],['Expected profit','Retail value minus base stock cost and transport; projected, not realized profit.'],['Costs','No taxes, discounts or further expenses included in expected calculations.'],['Barcode','Synthetic leading-zero text barcode on the first product; others intentionally blank.'],['Urdu','TEST قلم checks product-name preservation.'],['Import compatibility','Map supported fields in preview. Supplier, wholesale, margin and freight may not be supported by the current importer. Do not map these to unrelated fields.'],['Unsupported fields','If needed, enter those fields manually using these rows as reference; do not assume freight imported successfully.'],['Safety','Import once in an isolated test app. Check duplicates before repeating.'],['First-row check','Notebook: 20 pieces, base cost 1000, freight 50, landed cost 1050, retail 1600, expected profit 550.'],['Source','Synthetic sample data authored for this test, not real shop records.']];
n.getRange('A1:B17').values=notes;n.getRange('A1:B1').format={fill:'#A67C20',font:{bold:true,color:'#FFFFFF'}};n.getRange('A1:B17').format.autofitColumns();
wb.recalculate();
console.log((await wb.inspect({kind:'table',range:"'Expected Results'!A14:G14",include:'values',tableMaxRows:2,tableMaxCols:7})).ndjson);
await (await SpreadsheetFile.exportXlsx(wb)).save('Ali_Educational_Test_Products_Stock.xlsx');
for(const [sheetName,range,file] of [['Products','A1:H8','products-preview.png'],['Expected Results','A1:G14','checks-preview.png'],['Instructions','A1:B6','instructions-preview.png']]){const pic=await wb.render({sheetName,range,scale:1});await fs.writeFile(file,new Uint8Array(await pic.arrayBuffer()));}
console.log(JSON.stringify({products:rows.length,pieces:rows.reduce((a,r)=>a+r[4],0),baseCost:rows.reduce((a,r)=>a+r[4]*r[6],0),freight:freight.reduce((a,b)=>a+b,0),retail:rows.reduce((a,r)=>a+r[4]*r[7],0)}));