from pathlib import Path
import json
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
root=Path.cwd(); folder=root/'output/billing-preview'
items=[{'id':i,'title':t} for i,t in [('billing','Add items to a bill'),('scan','Scan with the phone camera'),('review','Review items and discounts'),('payment','Take payment and calculate change')]]
c=canvas.Canvas(str(root/'output/pdf/Ali_Educational_Billing_Phone_Previews.pdf'),pagesize=A4)
c.setTitle('Ali Educational - Phone screen previews')
w,h=A4
for i,item in enumerate(items,1):
 c.setFillColorRGB(.97,.97,.95);c.rect(0,0,w,h,fill=1,stroke=0)
 c.setFillColorRGB(.48,.34,.08);c.setFont('Helvetica-Bold',10);c.drawString(28,h-30,'ALI EDUCATIONAL / ANDROID PHONE PREVIEW')
 c.setFillColorRGB(.08,.1,.08);c.setFont('Helvetica-Bold',21);c.drawString(28,h-62,item['title'])
 ph=665;pw=ph*390/844;x=(w-pw)/2;y=62
 c.setFillColorRGB(.02,.025,.02);c.roundRect(x-7,y-7,pw+14,ph+14,24,fill=1,stroke=0)
 c.drawImage(str(folder/(item['id']+'.png')),x,y,pw,ph)
 c.setFont('Helvetica',9);c.setFillColorRGB(.35,.38,.35);c.drawString(28,28,'Design mockups with sample data. Actual fit adapts to your phone.');c.drawRightString(w-28,28,f'{i} / {len(items)}');c.showPage()
c.save()
