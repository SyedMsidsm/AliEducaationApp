param([switch]$VisualOnly)
$ErrorActionPreference = 'Stop'
$adb = 'C:/Users/Musid/AppData/Local/Android/Sdk/platform-tools/adb.exe'
$evidence = Join-Path $PSScriptRoot '../docs/evidence'
function Read-Ui {
    & $adb -d shell uiautomator dump /sdcard/ali-prototype.xml | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'UI dump failed' }
    & $adb -d pull /sdcard/ali-prototype.xml "$evidence/current.xml" 2>$null | Out-Null
    [xml]$xml = Get-Content -LiteralPath "$evidence/current.xml"
    if (-not $xml.SelectSingleNode('//node[@package="com.alieducational.inventory.dev"]')) { throw 'TEST app is not foreground; stop without tapping other apps' }
    return $xml
}
function Tap-Text([string]$label) {
    $xml = Read-Ui
    $node = $xml.SelectNodes('//node') | Where-Object { $_.text -ceq $label } | Select-Object -First 1
    if (-not $node) { throw "Cannot find visible label: $label" }
    $m = [regex]::Match($node.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    $x = [int]( ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2 )
    $y = [int]( ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2 )
    & $adb -d shell input tap $x $y
}
function Check([string]$id, [string]$expected) {
    $xml = Read-Ui
    $texts = ($xml.SelectNodes('//node') | ForEach-Object { $_.text }) -join ' '
    if (-not $texts.Contains($expected)) { throw "$id FAILED: missing $expected" }
    $xml.Save("$evidence/$id.xml")
    "$id PASS: $expected" | Tee-Object -FilePath "$evidence/prototype-smoke-results.txt" -Append
}
function Capture([string]$id) {
    & $adb -d shell screencap -p /sdcard/ali-prototype.png
    & $adb -d pull /sdcard/ali-prototype.png "$evidence/$id.png" 2>$null | Out-Null
}
'Prototype UI checks only: fixed examples, no financial engine or persistence verified.' | Set-Content "$evidence/prototype-smoke-results.txt"
if (-not $VisualOnly) {
Tap-Text 'Add stock'
Check 'P0-receive' 'Stock cost ₹550. Paid ₹200; we owe ₹350.'
Tap-Text 'Confirm sample receipt'
Check 'P0-receive-confirm' 'Simulation complete. No stock was posted.'
Tap-Text 'Back'
Tap-Text 'Remove stock'
Tap-Text 'Record sale'
Tap-Text 'Give discount'
Check 'P0-discount' 'net ₹90, paid ₹90, debt ₹0'
Tap-Text 'Keep as customer credit'
Check 'P0-credit' 'Test Customer owes ₹10'
Capture 'phase0-credit'
Tap-Text 'Back'
Tap-Text 'Record payment'
Tap-Text 'Preview repayment'
Check 'P0-repayment' 'Remaining debt ₹300'
Tap-Text 'More'
Tap-Text 'Daily total'
Tap-Text "This is the full day's total"
Check 'P0-full-day' 'add ₹400 only'
Tap-Text 'This is additional sales'
Check 'P0-additional' 'day becomes ₹1,600'
Tap-Text 'Stock'
Check 'P0-stock' '2 boxes + 3 pieces (23 pieces)'
Tap-Text 'Accounts'
Check 'P0-accounts' 'Test Customer ₹500'
Tap-Text 'Home'
}
Capture 'phase0-english-dark'
Tap-Text 'اردو'
Check 'P0-urdu' 'اسٹاک شامل کریں'
Capture 'phase0-urdu-dark'
Tap-Text 'تھیم بدلیں'
Check 'P0-urdu-light' 'ادائیگی درج کریں'
Capture 'phase0-urdu-light'
Tap-Text 'English'
Check 'P0-english-light' 'Add stock'
Capture 'phase0-english-light'
Tap-Text 'Change theme'
Check 'P0-home-restored' 'TEST • Workflow prototype'
