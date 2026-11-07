package com.alieducational.inventory

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var model: FoundationModel
    private var cancellation: android.os.CancellationSignal? = null
    private var authenticating = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        model=androidx.lifecycle.ViewModelProvider(this)[FoundationModel::class.java]
        setContent { FoundationScreen(model, ::authenticate) {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
        } }
    }
    override fun onResume() { super.onResume(); model.resume() }
    override fun onPause() { model.obscure(); super.onPause() }
    override fun onUserInteraction() { super.onUserInteraction(); if(::model.isInitialized) model.touch() }
    override fun onDestroy() { cancellation?.cancel(); super.onDestroy() }
    private fun authenticate(credentialOnly: Boolean) {
        if(authenticating || model.opening || model.unlocked) return
        val authenticators=if(credentialOnly) android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL else
            android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager=getSystemService(android.hardware.biometrics.BiometricManager::class.java)
        if(manager.canAuthenticate(authenticators)!=android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
            model.reportAuthentication(if(model.urdu) "فون کا محفوظ لاک ترتیب دیں، پھر دوبارہ کوشش کریں۔" else "Set up a secure phone screen lock, then retry.")
            return
        }
        authenticating=true
        val signal=android.os.CancellationSignal()
        cancellation=signal
        android.hardware.biometrics.BiometricPrompt.Builder(this)
            .setTitle("Ali Educational")
            .setSubtitle(if(model.urdu) "محفوظ ڈیٹا کھولیں" else "Unlock protected local data")
            .setAllowedAuthenticators(authenticators)
            .build().authenticate(signal, mainExecutor, object: android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult) {
                    authenticating=false
                    if(!signal.isCanceled && !isDestroyed) model.authenticated(
                        if(result.authenticationType==android.hardware.biometrics.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL) "Device credential" else "Biometric")
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    authenticating=false
                    if(!isDestroyed) model.reportAuthentication(if(model.urdu) "تصدیق مکمل نہیں ہوئی۔ ایپ مقفل ہے۔" else "Authentication not completed. App remains locked.")
                }
                override fun onAuthenticationFailed() {
                    model.reportAuthentication(if(model.urdu) "شناخت نہیں ہوئی۔ دوبارہ کوشش کریں۔" else "Not recognised. Try again or use your device credential.")
                }
            })
    }
}

@Composable
private fun Prototype() {
    var urdu by rememberSaveable { mutableStateOf(false) }
    var dark by rememberSaveable { mutableStateOf(true) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf("") }
    var notice by rememberSaveable { mutableStateOf("") }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    fun t(en: String, ur: String) = if (urdu) Regex("₹[0-9,.]+|[A-Z]+-[0-9]+").replace(ur) { "\u2066" + it.value + "\u2069" } else en
    val colors = if (dark) darkColorScheme(
        primary=Color(0xFFD9AC46), onPrimary=Color(0xFF171A17),
        secondaryContainer=Color(0xFF354038), onSecondaryContainer=Color(0xFFD9AC46),
        background=Color(0xFF111411), surface=Color(0xFF1C211D),
        onSurface=Color(0xFFF6F2E9), onSurfaceVariant=Color(0xFFB9C0B8),
        onBackground=Color(0xFFF6F2E9)
    ) else lightColorScheme(
        primary=Color(0xFFA47720), onPrimary=Color.White,
        secondaryContainer=Color(0xFFF1E5C8), onSecondaryContainer=Color(0xFF171A17),
        background=Color(0xFFF7F6F1), surface=Color.White,
        onSurface=Color(0xFF171A17), onBackground=Color(0xFF171A17),
        onSurfaceVariant=Color(0xFF566057)
    )
    MaterialTheme(colorScheme=colors) {
        CompositionLocalProvider(LocalLayoutDirection provides if(urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Scaffold(bottomBar={ NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
                listOf(t("Home","ہوم"),t("Stock","اسٹاک"),t("Accounts","کھاتے"),t("More","مزید")).forEachIndexed { i, label ->
                    NavigationBarItem(selected=tab==i, onClick={tab=i;route="";notice=""}, icon={Text(listOf("⌂","▤","₹","•••")[i])}, label={Text(label)})
                }
            } }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Image(painterResource(if(dark) R.drawable.ali_logo else R.drawable.ali_logo_light), t("Ali Educational logo","علی ایجوکیشنل لوگو"), Modifier.size(64.dp))
                        Column { Text("Ali Educational",style=MaterialTheme.typography.headlineSmall); Text(t("TEST • Workflow prototype","ٹیسٹ • ابتدائی نمونہ"),color=MaterialTheme.colorScheme.primary) }
                    }
                    Text(t("Sample data only. Actions are simulated. Nothing is saved. App lock is not implemented yet.","صرف نمونہ ڈیٹا۔ یہ مشق ہے، ریکارڈ محفوظ نہیں ہوتا۔ ایپ لاک ابھی تیار نہیں ہے۔"))
                    Row { TextButton(onClick={urdu=!urdu}) { Text(if(urdu) "English" else "اردو") }; TextButton(onClick={dark=!dark}) {Text(t("Change theme","تھیم بدلیں"))} }
                    if(route.isNotEmpty()) {
                        TextButton(onClick={route="";notice=""}) {Text(t("Back","واپس"))}
                        when(route) {
                            "receive" -> {
                                Text(t("Receive stock","اسٹاک شامل کریں"),style=MaterialTheme.typography.headlineSmall)
                                Info(t("Test Supplier: 20 pens × ₹10 + 10 notebooks × ₹30. Goods ₹500. Delivery ₹50 allocated ₹20 / ₹30. Stock cost ₹550. Paid ₹200; we owe ₹350.","ٹیسٹ سپلائر: ۲۰ قلم × ₹10 اور ۱۰ کاپیاں × ₹30۔ مال ₹500۔ ترسیل ₹50 کی تقسیم ₹20 / ₹30۔ لاگت ₹550۔ ادا ₹200؛ بقایا ₹350۔"))
                                Action(t("Confirm sample receipt","نمونہ وصولی کی تصدیق")) {notice=t("Simulation complete. No stock was posted.","مشق مکمل۔ کوئی اسٹاک محفوظ نہیں ہوا۔")}
                            }
                            "remove" -> {
                                Action(t("Record sale","فروخت درج کریں")) {route="sale"}
                                listOf(t("Shop use","دکان میں استعمال"),t("Giveaway","مفت دیا"),t("Damage/loss","نقصان"),t("Supplier return","سپلائر کو واپسی")).forEach { reason -> Action(reason) {notice=t("Sample reason selected: $reason. Quantity entry comes in Phase 2.","نمونہ وجہ منتخب ہوئی۔ مقدار کا اندراج اگلے مرحلے میں ہوگا۔")} }
                            }
                            "sale" -> {
                                Info(t("Sample bill ₹100 • Received ₹90. Choose what happens to the remaining ₹10.","نمونہ بل ₹100 • وصول ₹90۔ باقی ₹10 کے لیے انتخاب کریں۔"))
                                Action(t("Give discount","رعایت دیں")) {notice=t("Preview: ₹10 discount (10%), net ₹90, paid ₹90, debt ₹0. Simulated only.","پیش نظارہ: رعایت ₹10 (10%)، فروخت ₹90، وصول ₹90، بقایا ₹0۔ صرف مشق۔")}
                                Action(t("Keep as customer credit","گاہک کے کھاتے میں بقایا رکھیں")) {notice=t("Preview: Test Customer owes ₹10; sale ₹100, paid ₹90, discount ₹0. Simulated only.","پیش نظارہ: ٹیسٹ گاہک کا بقایا ₹10، فروخت ₹100، وصول ₹90، رعایت ₹0۔ صرف مشق۔")}
                            }
                            "payment" -> {
                                Info(t("Test Customer owes ₹500. Sample repayment ₹200.","ٹیسٹ گاہک کا بقایا ₹500۔ نمونہ وصولی ₹200۔"))
                                Action(t("Preview repayment","وصولی کا پیش نظارہ")) {notice=t("Remaining debt ₹300. This is not new sales or profit. Simulated only.","بقایا ₹300۔ یہ نئی فروخت یا نیا منافع نہیں ہے۔ صرف مشق۔")}
                            }
                            "daily" -> {
                                Info(t("Already recorded ₹600 • Entered total ₹1,000","پہلے درج ₹600 • نیا کل ₹1,000"))
                                Action(t("This is the full day's total","یہ پورے دن کا کل ہے")) {notice=t("Preview: add ₹400 only. Stock details needed; profit unavailable.","پیش نظارہ: صرف ₹400 شامل ہوں گے۔ اسٹاک کی تفصیل درکار؛ منافع دستیاب نہیں۔")}
                                Action(t("This is additional sales","یہ اضافی فروخت ہے")) {notice=t("Preview: add ₹1,000; day becomes ₹1,600. Stock details needed.","پیش نظارہ: ₹1,000 شامل؛ دن کا کل ₹1,600۔ اسٹاک کی تفصیل درکار۔")}
                            }
                            else -> Info(t("Backup and restore are not implemented. No successful backup exists. Recovery testing is mandatory before release.","بیک اپ اور بحالی ابھی تیار نہیں ہیں۔ کوئی کامیاب بیک اپ موجود نہیں۔ اجرا سے پہلے بحالی کی جانچ ضروری ہے۔"))
                        }
                    } else when(tab) {
                        0 -> {
                            Action(t("Add stock","اسٹاک شامل کریں")){route="receive"}
                            Action(t("Remove stock","اسٹاک نکالیں")){route="remove"}
                            Action(t("Record payment","ادائیگی درج کریں")){route="payment"}
                            Info(t("Sample stock value ₹550 • Low stock: 2 items • We owe ₹350 • Customers owe ₹500","نمونہ اسٹاک کی قیمت ₹550 • کم اسٹاک: ۲ اشیا • ہم نے دینے ہیں ₹350 • گاہکوں نے دینے ہیں ₹500"))
                            Text(t("No successful backup","کوئی کامیاب بیک اپ نہیں"))
                            Action(t("Back up now","ابھی بیک اپ بنائیں")){route="backup"}
                        }
                        1 -> {Text(t("Sample stock","نمونہ اسٹاک"),style=MaterialTheme.typography.headlineSmall); Info(t("Blue pen • PEN-001: 2 boxes + 3 pieces (23 pieces). 10 pieces per box.","نیلا قلم • PEN-001: ۲ ڈبے + ۳ قلم (۲۳ قلم)۔ ہر ڈبے میں ۱۰ قلم۔")); Action(t("Receive stock","اسٹاک شامل کریں")){route="receive"} }
                        2 -> {Text(t("Accounts","کھاتے"),style=MaterialTheme.typography.headlineSmall); Info(t("We owe • Test Supplier ₹350. Customers owe us • Test Customer ₹500.","ہم نے دینے ہیں • ٹیسٹ سپلائر ₹350۔ گاہکوں نے دینے ہیں • ٹیسٹ گاہک ₹500۔"));Action(t("Record payment","ادائیگی درج کریں")){route="payment"} }
                        3 -> {Action(t("Record sale","فروخت درج کریں")){route="sale"};Action(t("Daily total","روزانہ کل")){route="daily"};Action(t("Backup / restore","بیک اپ / بحالی")){route="backup"};Text(t("Planned lock timeout: 1 minute.","مجوزہ لاک وقت: ۱ منٹ۔"))}
                    }
                    if(notice.isNotEmpty()) Info(notice)
                }
            }
        }
    }
}

@Composable private fun Action(label:String, action:()->Unit) { Button(onClick=action,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(label)} }
@Composable private fun Info(text:String) { Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {Text(text,Modifier.padding(16.dp))} }
