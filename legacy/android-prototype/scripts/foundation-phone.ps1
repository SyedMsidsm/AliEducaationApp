param([ValidateSet('SaveFixture','Inspect','Lock','VerifyDraft','VerifyLocked','CredentialPrompt','OfflineWrite')][string]$Mode='Inspect')
$ErrorActionPreference='Stop'
$adb='C:/Users/Musid/AppData/Local/Android/Sdk/platform-tools/adb.exe'
$evidence=Join-Path $PSScriptRoot '../docs/evidence'
function Read-Ui {
    & $adb -d shell uiautomator dump /sdcard/ali-foundation.xml | Out-Null
    if($LASTEXITCODE -ne 0){throw 'UI dump failed'}
    & $adb -d pull /sdcard/ali-foundation.xml "$evidence/foundation-current.xml" 2>$null | Out-Null
    [xml]$xml=Get-Content "$evidence/foundation-current.xml"
    if(-not $xml.SelectSingleNode('//node[@package="com.alieducational.inventory.dev"]')){throw 'TEST app not foreground; no taps performed'}
    return $xml
}
function Tap-Node($node) {
    if(-not $node){throw 'Control missing; no tap performed'}
    $m=[regex]::Match($node.bounds,'\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    $x=[int](([int]$m.Groups[1].Value+[int]$m.Groups[3].Value)/2)
    $y=[int](([int]$m.Groups[2].Value+[int]$m.Groups[4].Value)/2)
    & $adb -d shell input tap $x $y
}
function Tap-Text([string]$label) {
    $xml=Read-Ui
    $node=$xml.SelectNodes('//node') | Where-Object {$_.text -ceq $label} | Select-Object -First 1
    Tap-Node $node
}
function Check([string]$id,[string]$expected) {
    $xml=Read-Ui
    $text=($xml.SelectNodes('//node') | ForEach-Object {$_.text}) -join ' '
    if(-not $text.Contains($expected)){throw "$id FAILED: missing $expected"}
    $xml.Save("$evidence/$id.xml")
    "$id PASS: $expected" | Tee-Object -FilePath "$evidence/foundation-phone-results.txt" -Append
}
switch($Mode) {
    OfflineWrite {
        $xml=Read-Ui
        $field=$xml.SelectNodes('//node') | Where-Object {$_.class -eq 'android.widget.EditText'} | Select-Object -First 1
        if(-not $field -or $field.text -cne 'TEST-PHASE1-20261030'){throw 'Expected disposable fixture missing; no edit performed'}
        Tap-Node $field
        & $adb -d shell input keyevent 123
        & $adb -d shell input text '-OFFLINE'
        Check 'P1-offline-write' 'TEST-PHASE1-20261030-OFFLINE'
        Check 'P1-offline-write-saved' 'Saved'
        & $adb -d shell input keyevent 4
        Tap-Text 'Home'
        Tap-Text 'Open offline test draft'
        Check 'P1-offline-navigation-retained' 'TEST-PHASE1-20261030-OFFLINE'
    }
    CredentialPrompt {Tap-Text 'Use device PIN / pattern / password'}
    SaveFixture {
        Tap-Text 'Save settings'
        Check 'P1-settings-saved' 'Saved'
        Tap-Text 'Home'
        Tap-Text 'Open offline test draft'
        $xml=Read-Ui
        $field=$xml.SelectNodes('//node') | Where-Object {$_.class -eq 'android.widget.EditText'} | Select-Object -First 1
        if($field.text -ne ''){throw 'Draft is not empty; refusing to overwrite it'}
        Tap-Node $field
        & $adb -d shell input text 'TEST-PHASE1-20261030'
        Check 'P1-draft-saved' 'Saved'
        Check 'P1-draft-fixture' 'TEST-PHASE1-20261030'
        & $adb -d shell input keyevent 4
    }
    VerifyDraft {
        Tap-Text 'Home'
        Tap-Text 'Open offline test draft'
        Check 'P1-draft-reopened' 'TEST-PHASE1-20261030'
        Check 'P1-draft-reopened-saved' 'Saved'
    }
    Lock {Tap-Text 'Home';Tap-Text 'Lock now';Check 'P1-manual-lock' 'Locked.'}
    VerifyLocked {Check 'P1-timeout-locked' 'Locked.'}
    Inspect {$xml=Read-Ui;$xml.SelectNodes('//node') | Where-Object {$_.text -ne ''} | Select-Object text,bounds}
}
