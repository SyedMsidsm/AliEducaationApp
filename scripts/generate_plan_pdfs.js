const fs = require('fs');
const path = require('path');
const { chromium } = require('C:/Users/Musid/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');

const root = path.resolve(__dirname, '..');
const output = path.join(root, 'output', 'pdf');
const temp = path.join(root, 'tmp', 'pdfs');
fs.mkdirSync(output, { recursive: true });
fs.mkdirSync(temp, { recursive: true });

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function inline(s) {
  let t = escapeHtml(s);
  t = t.replace(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g, '<a href="$2">$1</a>');
  t = t.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  t = t.replace(/`([^`]+)`/g, '<code>$1</code>');
  return t;
}

function markdownToHtml(md) {
  const lines = md.replace(/\r\n/g, '\n').split('\n');
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) { i++; continue; }
    const heading = line.match(/^(#{1,3})\s+(.*)$/);
    if (heading) {
      const level = heading[1].length;
      out.push(`<h${level}>${inline(heading[2])}</h${level}>`);
      i++; continue;
    }
    if (line.startsWith('|')) {
      const rows = [];
      while (i < lines.length && lines[i].startsWith('|')) {
        rows.push(lines[i].split('|').slice(1, -1).map(x => x.trim()));
        i++;
      }
      const head = rows[0];
      const body = rows.slice(2);
      out.push('<table><thead><tr>' + head.map(x => `<th>${inline(x)}</th>`).join('') + '</tr></thead><tbody>' +
        body.map(row => '<tr>' + row.map(x => `<td>${inline(x)}</td>`).join('') + '</tr>').join('') + '</tbody></table>');
      continue;
    }
    if (/^-\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^-\s+/.test(lines[i])) {
        items.push(lines[i].replace(/^-\s+/, ''));
        i++;
      }
      out.push('<ul>' + items.map(x => `<li>${inline(x)}</li>`).join('') + '</ul>');
      continue;
    }
    if (/^\d+\.\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\d+\.\s+/.test(lines[i])) {
        items.push(lines[i].replace(/^\d+\.\s+/, ''));
        i++;
      }
      out.push('<ol>' + items.map(x => `<li>${inline(x)}</li>`).join('') + '</ol>');
      continue;
    }
    const para = [];
    while (i < lines.length && lines[i].trim() && !/^(#{1,3})\s+/.test(lines[i]) && !lines[i].startsWith('|') && !/^-\s+/.test(lines[i]) && !/^\d+\.\s+/.test(lines[i])) {
      para.push(lines[i]);
      i++;
    }
    const text = para.map(x => inline(x.trim())).join('<br>');
    out.push(`<p>${text}</p>`);
  }
  return out.join('\n');
}

function htmlDocument(md, lang, guide = false) {
  const urdu = lang === 'ur';
  const direction = urdu ? 'rtl' : 'ltr';
  const originalLogo = 'data:image/png;base64,' + fs.readFileSync(path.join(root, 'assets', 'brand', 'ali-logo-original.png')).toString('base64');
  const darkLogo = 'data:image/png;base64,' + fs.readFileSync(path.join(root, 'assets', 'brand', 'ali-logo-dark-concept.png')).toString('base64');
  const visual = guide
    ? `<div class="logo-row"><div class="logo-card dark-card"><div class="card-label">DARK DEFAULT</div><img src="${darkLogo}" alt="Ali dark logo concept"><div class="card-name">Ali Educational</div></div><div class="logo-card light-card"><div class="card-label">OPTIONAL LIGHT</div><img src="${originalLogo}" alt="Ali original logo"><div class="card-name">Ali Educational</div></div></div>`
    : `<div class="plan-logo"><img src="${originalLogo}" alt="Ali Educational logo"></div>`;
  const bodyHtml = markdownToHtml(md).replace('</h1>', '</h1>' + visual);
  return `<!doctype html><html lang="${lang}" dir="${direction}"><head><meta charset="utf-8"><style>
    @font-face { font-family: 'Urdu Typesetting Local'; src: url('file:///C:/Windows/Fonts/UrdType.ttf') format('truetype'); }
    @font-face { font-family: 'Urdu Typesetting Local'; src: url('file:///C:/Windows/Fonts/UrdTypeb.ttf') format('truetype'); font-weight: 700; }
    @page { size: A4; margin: ${urdu ? '13mm 15mm 14mm' : '18mm 18mm 19mm'}; }
    html { color: #171A17; }
    body { margin: 0; font-family: Arial, 'Segoe UI', sans-serif; font-size: ${urdu ? '14px' : '11.5px'}; line-height: ${urdu ? '1.38' : '1.5'}; }
    h1, h2, h3 { color: #171A17; page-break-after: avoid; break-after: avoid; }
    h1 { font-size: ${urdu ? '27px' : '22px'}; line-height: 1.35; border-bottom: 3px solid #D9AC46; padding-bottom: 8px; margin: 0 0 13px; }
    h2 { font-size: ${urdu ? '21px' : '15px'}; line-height: 1.35; border-bottom: 1px solid #D9AC46; padding-bottom: 4px; margin: ${urdu ? '13px' : '23px'} 0 7px; }
    h3 { font-size: ${urdu ? '18px' : '13px'}; line-height: 1.35; margin: ${urdu ? '10px' : '18px'} 0 4px; }
    p { margin: 0 0 ${urdu ? '6px' : '10px'}; orphans: 3; widows: 3; }
    ul, ol { margin: ${urdu ? '3px 0 9px' : '5px 0 13px'}; padding-inline-start: 25px; }
    li { margin: 0 0 ${urdu ? '3px' : '6px'}; break-inside: avoid; }
    strong { color: #6E5119; }
    a { color: #8A661C; text-decoration: none; overflow-wrap: anywhere; }
    table { width: 100%; border-collapse: collapse; margin: 11px 0 15px; table-layout: fixed; font-size: 10.4px; }
    th { text-align: left; background: #171A17; color: #fff; padding: 8px 9px; }
    th:first-child, td:first-child { width: 20%; }
    td { padding: 7px 9px; vertical-align: top; border-bottom: 1px solid #E4E1D8; }
    tr:nth-child(even) td { background: #F7F6F1; }
    tr { break-inside: avoid; }
    .plan-logo { margin: 2px 0 13px; height: 38px; }
    .plan-logo img { height: 38px; width: auto; object-fit: contain; mix-blend-mode: multiply; }
    .logo-row { display: flex; gap: 12px; margin: 4px 0 18px; break-inside: avoid; }
    .logo-card { box-sizing: border-box; width: 50%; min-height: 125px; padding: 11px 16px; text-align: center; border-radius: 8px; }
    .dark-card { background: #111411; color: #F6F2E9; }
    .light-card { background: #F7F6F1; color: #171A17; border: 1px solid #DADFD8; }
    .logo-card img { display: block; height: 54px; width: auto; max-width: 100%; margin: 7px auto 3px; object-fit: contain; }
    .card-label { font: bold 9px Arial; letter-spacing: 1px; color: #D9AC46; }
    .light-card .card-label { color: #A47720; }
    .card-name { font: bold 13px Arial; }
    code { font-family: Consolas, monospace; color: #6E5119; }
    .guide { font-size: 10.9px; line-height: 1.38; }
    .guide h2 { margin: 13px 0 6px; }
    .guide p { margin-bottom: 7px; }
    .guide .logo-row { margin: 3px 0 10px; }
    .guide .logo-card { min-height: 98px; padding: 7px 12px; }
    .guide .logo-card img { height: 44px; margin: 3px auto 2px; }
    .guide table { margin: 7px 0 9px; font-size: 10px; }
    .guide th, .guide td { padding: 4px 7px; }
    .guide ul { margin: 3px 0 7px; }
    .guide li { margin-bottom: 3px; }
    @media print { body { print-color-adjust: exact; -webkit-print-color-adjust: exact; } }
  </style></head><body class="${guide ? 'guide' : ''}">${bodyHtml}</body></html>`;
}

async function main() {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  try {
    const jobs = [
      ['en', 'INVENTORY_APP_FEATURE_PLAN.md', 'Stationery_Shop_Inventory_Plan_English.pdf', false],
      ['ur', 'INVENTORY_APP_FEATURE_PLAN_URDU.md', 'Stationery_Shop_Inventory_Plan_Urdu.pdf', false],
      ['en', 'THEME_GUIDE.md', 'Ali_Educational_Theme_Guide.pdf', true],
    ];
    for (const [lang, source, filename, guide] of jobs) {
      const md = fs.readFileSync(path.join(root, source), 'utf8');
      const htmlPath = path.join(temp, guide ? 'theme-guide.html' : `inventory-plan-${lang}.html`);
      fs.writeFileSync(htmlPath, htmlDocument(md, lang, guide), 'utf8');
      const page = await browser.newPage();
      await page.goto('file:///' + htmlPath.replace(/\\/g, '/'), { waitUntil: 'load' });
      await page.evaluate(() => document.fonts.ready);
      await page.pdf({
        path: path.join(output, filename),
        format: 'A4',
        printBackground: true,
        displayHeaderFooter: true,
        headerTemplate: '<div></div>',
        footerTemplate: `<div style="width:100%;font:9px Arial;color:#647785;padding:0 18mm;display:flex;justify-content:space-between"><span>${guide ? 'Ali Educational Theme Guide' : lang === 'ur' ? 'اسٹیشنری دکان کا ایپ پلان' : 'Stationery Shop Inventory App Plan'}</span><span class="pageNumber"></span></div>`,
        margin: lang === 'ur'
          ? { top: '13mm', right: '15mm', bottom: '14mm', left: '15mm' }
          : { top: '18mm', right: '18mm', bottom: '19mm', left: '18mm' },
      });
      await page.close();
      process.stdout.write(path.join(output, filename) + '\n');
    }
  } finally {
    await browser.close();
  }
}

main().catch(e => { console.error(e); process.exit(1); });
