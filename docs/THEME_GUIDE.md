# Ali Educational - Theme Guide

**Design direction:** use the poster's black-and-gold identity with a calm, uncluttered inventory interface. Product photos and poster panels are not part of the app layout. The brand name is **Ali Educational**.

## Color and layout

| Token | Dark default | Optional light theme | Use |
| --- | --- | --- | --- |
| Background | `#111411` | `#F7F6F1` | Main app canvas |
| Surface | `#1C211D` | `#FFFFFF` | Cards, navigation, dialogs |
| Main text | `#F6F2E9` | `#171A17` | Headings and values |
| Secondary text | `#B9C0B8` | `#566057` | Labels and help text |
| Gold accent | `#D9AC46` | `#A47720` | Primary action, selection, focus |
| Border | `#354038` | `#DADFD8` | Dividers and input outlines |
| Success | `#75B890` | `#24754C` | Successful saves, backups and payments |
| Warning | `#E4B765` | `#915E13` | Low stock and unpaid balance alerts |

Gold is an accent, not a full-screen fill. Use clear type and large numbers for stock on hand, supplier dues, and customer dues. Use Home, Stock, Accounts and More on the single Android phone; computer support is deferred. The dark theme is the default; Settings offers the light theme. Urdu uses proper right-to-left screen layout and readable Urdu script; numbers, SKUs, and currency remain legible in either language.

## Logo placement

- **Unlock screen:** centered monogram above **Ali Educational**, with comfortable clear space. Place Android biometric/device-credential unlock below it; no app account or sign-in form.
- **Dashboard:** compact monogram next to the shop name in the top bar. The dashboard itself leads with stock, supplier dues, customer dues, and low-stock alerts.
- **Receipts:** small, single-color black monogram at the top beside the shop name. Receipts are plain black on white for reliable printing.
- **Dark screens:** use warm-ivory letters with the gold book/pencil details on a transparent background.
- **Light screens:** use the supplied black-and-gold logo on a light surface.

The supplied small raster logo is the source of truth. The dark transparent image in `assets/brand/ali-logo-dark-concept.png` is a placement concept generated from that source; its edges should be cleaned or replaced by an original vector export before production use. Do not stretch the logo, add shadows, or use the full poster as an app background.

## Receipts and settings

The owner sets one default receipt action in **Settings > Receipts**: none, show, print, save PDF, or share (including WhatsApp). The owner can override it for any receipt. The owner also sets English or Urdu as the default receipt language and can change it for an individual receipt. Both language versions print as black text on white with a compact black logo.

## Application priority

The visual hierarchy follows the revised product plan: **inventory first, then whom we owe and who owes us**. Quick stock-out and goods received are prominent actions. Daily sales, discounts, delivery-aware gross profit and basic recorded-sales graphs are included in the plan. Full POS and optional AI are later options. See UI_UX_SPECIFICATION.md for the current screen inventory.
