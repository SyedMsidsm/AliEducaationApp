param([switch]$LibraryOnly)
$ErrorActionPreference='Stop'
. "$PSScriptRoot/foundation-phone.ps1" -Mode Inspect | Out-Null
function Find-Visible([string]$label) {
    for($attempt=0;$attempt -lt 8;$attempt++){
        $xml=Read-Ui
        $node=$xml.SelectNodes('//node') | Where-Object {$_.text -ceq $label} | Select-Object -First 1
        if($node){return $node}
        & $adb -d shell input swipe 720 2400 720 950 350
    }
    throw "Label not found: $label"
}
function Fill([string]$label,[string]$value) {
    $node=Find-Visible $label
    while($node -and $node.class -ne 'android.widget.EditText'){$node=$node.ParentNode}
    if(-not $node){throw "Editable field not found: $label"}
    Tap-Node $node
    & $adb -d shell input keyevent 123
    $deletes=@(1..35 | ForEach-Object {'67'})
    & $adb -d shell input keyevent @deletes
    & $adb -d shell input text $value
    & $adb -d shell input keyevent 4
}
if($LibraryOnly){return}
Tap-Text 'Stock'
Tap-Text 'Add product'
Fill 'Product name' 'TEST-STOCK-PEN'
Fill 'SKU (optional; unique)' 'TEST-P2-PEN-001'
Fill 'Pieces per pack' '10'
Fill 'Opening pieces' '30'
Fill 'Total opening cost ₹ (blank = unknown)' '300'
Fill 'Piece selling price ₹' '15'
Tap-Node (Find-Visible 'Save product and opening stock')
Check 'P2-product-created' 'TEST-STOCK-PEN'
Check 'P2-opening-packs' '3 packs + 0 pieces (30 pieces)'
