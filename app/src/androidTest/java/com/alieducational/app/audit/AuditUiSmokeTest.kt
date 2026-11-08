package com.alieducational.app.audit

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.alieducational.app.MainActivity
import com.alieducational.app.data.preferences.AppPreferences
import com.alieducational.app.widget.AliSlimBarWidgetProvider
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File

class AuditUiSmokeTest {
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun prepare() = runBlocking {
        val prefs = AppPreferences(ctx)
        prefs.setFirstLaunch(false); prefs.setLockEnabled(false)
        prefs.setThemeMode("light"); prefs.setDeviceRole("OWNER"); prefs.setAppTextSize("Default")
    }
    @After fun close() { scenario?.close() }
    private fun start(action: String? = null) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (action != null) putExtra(AliSlimBarWidgetProvider.EXTRA_WIDGET_ACTION, action)
        }
        scenario = ActivityScenario.launch(intent)
    }
    private fun find(text: String): android.view.accessibility.AccessibilityNodeInfo? {
        fun walk(n: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.text?.toString() == text || n.contentDescription?.toString() == text) return n
            for (i in 0 until n.childCount) walk(n.getChild(i))?.let { return it }
            return null
        }
        return walk(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
    }
    private fun waitFor(text: String): android.view.accessibility.AccessibilityNodeInfo {
        val until = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < until) { find(text)?.let { return it }; android.os.SystemClock.sleep(150) }
        throw AssertionError("Screen text not found: $text")
    }
    private fun click(text: String) {
        var node: android.view.accessibility.AccessibilityNodeInfo? = waitFor(text)
        while (node != null && !node.isClickable) node = node.parent
        Assert.assertTrue("Clickable target: $text", node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true)
    }
    private fun scroll() {
        fun walk(n: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
            if (n == null) return false
            if (n.isScrollable && n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
            for (i in 0 until n.childCount) if (walk(n.getChild(i))) return true
            return false
        }
        walk(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
        android.os.SystemClock.sleep(350)
    }
    private fun screenshot(name: String) {
        android.os.SystemClock.sleep(900) // Wait for navigation transitions before visual evidence.
        val file = File(ctx.getExternalFilesDir(null), "audit-$name.png")
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
    @Test fun homeBillHistoryOpens() {
        start(); waitFor("Home")
        repeat(5) { if (find("Bill History") == null) scroll() }
        click("Bill History"); waitFor("Remaining Dues")
        screenshot("bill-history-light")
    }
    @Test fun widgetCashInOpensCorrectDialog() {
        start("CASH_IN"); waitFor("Add Income (Cash In)")
        screenshot("cash-in-light")
    }
    @Test fun widgetCashOutOpensCorrectDialog() {
        start("CASH_OUT"); waitFor("Add Expense (Cash Out)")
    }
    @Test fun widgetAddStockOpensProductForm() {
        start("ADD_STOCK"); waitFor("Add Stock & Product")
        screenshot("stock-light")
    }
    @Test fun widgetNewBillOpensBilling() {
        start("NEW_BILL"); waitFor("Scan with camera"); screenshot("billing-light")
    }
    @Test fun accountsOpensInExtraLargeLightMode() {
        runBlocking { AppPreferences(ctx).setAppTextSize("Extra Large") }
        start(); waitFor("Accounts"); click("Accounts")
        waitFor("Add Supplier"); screenshot("accounts-large-light")
    }
}
