package com.alieducational.inventory

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun FoundationScreen(model: FoundationModel, authenticate: (Boolean)->Unit, securitySettings: ()->Unit) {
    val urdu=model.urdu
    val dark=model.dark
    fun t(en:String,ur:String)=if(urdu) ur else en
    val colors=if(dark) darkColorScheme(primary=Color(0xFFD9AC46),onPrimary=Color(0xFF171A17),
        background=Color(0xFF111411),surface=Color(0xFF1C211D),onSurface=Color(0xFFF6F2E9),onBackground=Color(0xFFF6F2E9),
        secondaryContainer=Color(0xFF354038),onSecondaryContainer=Color(0xFFD9AC46),onSurfaceVariant=Color(0xFFB9C0B8))
    else lightColorScheme(primary=Color(0xFFA47720),onPrimary=Color.White,background=Color(0xFFF7F6F1),surface=Color.White,
        onSurface=Color(0xFF171A17),onBackground=Color(0xFF171A17),secondaryContainer=Color(0xFFF1E5C8),onSecondaryContainer=Color(0xFF171A17),onSurfaceVariant=Color(0xFF566057))
    val view=LocalView.current
    SideEffect {
        WindowCompat.getInsetsController((view.context as Activity).window,view).apply {
            isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark
        }
    }
    MaterialTheme(colorScheme=colors) {
        CompositionLocalProvider(LocalLayoutDirection provides if(urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            if(!model.unlocked || !model.foreground) {
                Surface(Modifier.fillMaxSize(),color=colors.background) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Image(painterResource(if(dark) R.drawable.ali_logo else R.drawable.ali_logo_light),null,Modifier.size(120.dp))
                        Text("Ali Educational",style=MaterialTheme.typography.headlineMedium)
                        Text(t("TEST • Protected local storage","ٹیسٹ • محفوظ مقامی ڈیٹا"))
                        Text(t("Locked. Use your fingerprint or the phone's PIN, pattern or password.","ایپ مقفل ہے۔ فنگر پرنٹ یا فون کا پن، پیٹرن یا پاس ورڈ استعمال کریں۔"))
                        TextButton(onClick={model.selectLockedLanguage(!urdu)}) {Text(if(urdu) "English" else "اردو")}
                        Button(onClick={authenticate(false)},enabled=!model.opening,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(t("Unlock","لاک کھولیں"))}
                        OutlinedButton(onClick={authenticate(true)},enabled=!model.opening,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(t("Use device PIN / pattern / password","فون کا پن / پیٹرن / پاس ورڈ استعمال کریں"))}
                        TextButton(onClick=securitySettings) {Text(t("Phone security settings","فون کی سکیورٹی ترتیبات"))}
                        if(model.opening) CircularProgressIndicator()
                        if(model.message.isNotBlank()) Text(model.message)
                        Text(t("No account or app password. Backup/restore is not ready yet; use test data only.","کوئی اکاؤنٹ یا الگ پاس ورڈ نہیں۔ بیک اپ اور بحالی ابھی تیار نہیں؛ صرف ٹیسٹ ڈیٹا استعمال کریں۔"))
                    }
                }
            } else {
                UnlockedFoundation(model)
            }
        }
    }
}

@Composable
private fun UnlockedFoundation(model:FoundationModel) {
    fun t(en:String,ur:String)=if(model.urdu) ur else en
    var tab by remember { mutableIntStateOf(0) }
    var settingsOpen by remember { mutableStateOf(!model.settings.setupComplete) }
    var noteOpen by remember { mutableStateOf(false) }
    Scaffold(bottomBar={
        NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
            listOf(t("Home","ہوم"),t("Stock","اسٹاک"),t("Accounts","کھاتے"),t("More","مزید")).forEachIndexed { i,label ->
                NavigationBarItem(selected=tab==i,onClick={tab=i;settingsOpen=false;noteOpen=false},icon={Text(listOf("⌂","▤","₹","•••")[i])},label={Text(label)})
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Image(painterResource(if(model.dark) R.drawable.ali_logo else R.drawable.ali_logo_light),null,Modifier.size(64.dp))
                Column {Text(model.settings.shopName,style=MaterialTheme.typography.headlineSmall);Text(t("TEST • Stock development","ٹیسٹ • اسٹاک کی تیاری"),color=MaterialTheme.colorScheme.primary)}
            }
            Text(t("Stock development build. Payments and backups are not ready. Use test data only.","اسٹاک اور ادائیگی کا اندراج ابھی تیار نہیں۔ دکان کا اصل ڈیٹا درج نہ کریں۔"))
            if(settingsOpen) SettingsEditor(model)
            else if(noteOpen) {
                Text(t("Offline test draft","آف لائن ٹیسٹ مسودہ"),style=MaterialTheme.typography.titleLarge)
                Text(t("This draft never changes stock or balances. Each edit is saved in the encrypted database.","یہ مسودہ اسٹاک یا بقایا تبدیل نہیں کرتا۔ ہر ترمیم محفوظ ڈیٹابیس میں رکھی جاتی ہے۔"))
                OutlinedTextField(value=model.draft,onValueChange=model::editDraft,label={Text(t("Test note","ٹیسٹ نوٹ"))},modifier=Modifier.fillMaxWidth(),minLines=3)
                Text(when(model.draftStatus) {"Saved"->t("Saved","محفوظ ہو گیا");"Saving"->t("Saving…","محفوظ ہو رہا ہے…");"Not saved"->t("Not saved — retry your edit","محفوظ نہیں ہوا — دوبارہ ترمیم کریں");else->""})
                TextButton(onClick={noteOpen=false}) {Text(t("Back","واپس"))}
            } else when(tab) {
                0 -> {
                    Text(t("Local storage is ready","مقامی ڈیٹا تیار ہے"),style=MaterialTheme.typography.titleLarge)
                    Text(t("Products and opening stock are available in Stock. Accounts and purchases come next.","فہرست خالی ہے۔ اشیا، اسٹاک اور کھاتے اگلے مراحل میں آئیں گے۔"))
                    FoundationButton(t("Settings","ترتیبات")) {settingsOpen=true}
                    FoundationButton(t("Open offline test draft","آف لائن ٹیسٹ مسودہ کھولیں")) {noteOpen=true}
                    Text(t("No successful backup. Manual encrypted backup and restore are not available yet.","کوئی کامیاب بیک اپ نہیں۔ محفوظ بیک اپ اور بحالی ابھی دستیاب نہیں۔"))
                }
                1 -> InventoryScreen(model)
                2 -> Text(t("No supplier or customer accounts yet. Accounts arrive in Phase 3.","ابھی سپلائر یا گاہک کے کھاتے نہیں۔ یہ تیسرے مرحلے میں آئیں گے۔"))
                3 -> {FoundationButton(t("Settings","ترتیبات")) {settingsOpen=true};FoundationButton(t("Open offline test draft","آف لائن ٹیسٹ مسودہ کھولیں")) {noteOpen=true}}
            }
            if(model.message.isNotBlank()) Text(model.message)
            Text(t("Last unlock: "+model.authMethod,"آخری تصدیق: "+if(model.authMethod=="Biometric") "بایومیٹرک" else "فون کا لاک"))
            TextButton(onClick=model::lock) {Text(t("Lock now","ابھی لاک کریں"))}
        }
    }
}

@Composable private fun SettingsEditor(model:FoundationModel) {
    fun t(en:String,ur:String)=if(model.urdu) ur else en
    var shop by remember(model.settings) {mutableStateOf(model.settings.shopName)}
    var urdu by remember(model.settings) {mutableStateOf(model.urdu)}
    var dark by remember(model.settings) {mutableStateOf(model.dark)}
    var seconds by remember(model.settings) {mutableIntStateOf(model.settings.lockSeconds)}
    Text(t("Shop settings","دکان کی ترتیبات"),style=MaterialTheme.typography.titleLarge)
    OutlinedTextField(shop,{shop=it},label={Text(t("Shop name","دکان کا نام"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
    Row {TextButton(onClick={urdu=false}) {Text(if(!urdu) "✓ English" else "English")};TextButton(onClick={urdu=true}) {Text(if(urdu) "✓ اردو" else "اردو")}}
    Row {Text(t("Dark theme","گہری تھیم"),Modifier.weight(1f));Switch(dark,{dark=it})}
    Text(t("Lock after inactivity or time away","غیرفعال رہنے یا باہر جانے کے بعد لاک"))
    Row {listOf(30,60,300).forEach { value -> TextButton(onClick={seconds=value}) {Text((if(seconds==value) "✓ " else "")+t("$value sec","$value سیکنڈ"))}}}
    Text(t("Backup is not ready. Start with an empty test catalog; setup can be revisited here.","بیک اپ تیار نہیں۔ خالی ٹیسٹ فہرست سے شروع کریں؛ ترتیبات بعد میں بدل سکتے ہیں۔"))
    Button(onClick={model.saveSettings(shop,urdu,dark,seconds)},enabled=!model.savingSettings,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(t("Save settings","ترتیبات محفوظ کریں"))}
}

@Composable private fun FoundationButton(label:String,action:()->Unit) {
    Button(onClick=action,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(label)}
}
