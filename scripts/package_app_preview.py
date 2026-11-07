from pathlib import Path
import json
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import landscape,A4
from PIL import Image,ImageOps,ImageDraw
root=Path.cwd(); previews=root/'output/app-preview'; pages=json.loads((previews/'manifest.json').read_text())
dest=root/'output/pdf/Ali_Educational_App_Screen_Previews.pdf'
c=canvas.Canvas(str(dest),pagesize=landscape(A4));w,h=landscape(A4)
c.setTitle('Ali Educational | App screen previews')
for i,p in enumerate(pages,1):
    c.setFillColorRGB(.97,.97,.95);c.rect(0,0,w,h,fill=1,stroke=0)
    c.setFillColorRGB(.55,.39,.1);c.setFont('Helvetica-Bold',9);c.drawString(28,h-28,'ALI EDUCATIONAL / DESIGN PREVIEW')
    c.setFillColorRGB(.08,.1,.08);c.setFont('Helvetica-Bold',20);c.drawString(28,h-55,p['title'])
    c.setFont('Helvetica',10);c.drawString(28,h-75,p['caption'])
    ih=h-140; iw=ih*1280/800
    c.drawImage(str(previews/(p['id']+'.png')),(w-iw)/2,38,width=iw,height=ih)
    c.setFillColorRGB(.36,.39,.36);c.setFont('Helvetica',8)
    c.drawString(28,19,'Concept screens only. All names, amounts and stock figures are illustrative. Logo treatment is a draft.')
    c.drawRightString(w-28,19,f'{i} / {len(pages)}');c.showPage()
c.save()
print(dest)
