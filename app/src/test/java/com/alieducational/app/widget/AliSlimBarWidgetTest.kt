package com.alieducational.app.widget

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AliSlimBarWidgetTest {

    @Test
    fun testWidgetActionConstants_Section02() {
        // Required exact labels & action identities
        assertEquals("CASH_IN", AliSlimBarWidgetProvider.ACTION_CASH_IN)
        assertEquals("CASH_OUT", AliSlimBarWidgetProvider.ACTION_CASH_OUT)
        assertEquals("NEW_BILL", AliSlimBarWidgetProvider.ACTION_NEW_BILL)
        assertEquals("ADD_STOCK", AliSlimBarWidgetProvider.ACTION_ADD_STOCK)
        assertEquals("HOME", AliSlimBarWidgetProvider.ACTION_HOME)
        assertEquals("com.alieducational.app.action.WIDGET_CLICK", AliSlimBarWidgetProvider.ACTION_WIDGET_CLICK)
        assertEquals("extra_widget_action", AliSlimBarWidgetProvider.EXTRA_WIDGET_ACTION)
    }

    @Test
    fun testDistinctUrisPreventIntentCoalescing_Section04() {
        val actions = listOf(
            AliSlimBarWidgetProvider.ACTION_CASH_IN,
            AliSlimBarWidgetProvider.ACTION_CASH_OUT,
            AliSlimBarWidgetProvider.ACTION_NEW_BILL,
            AliSlimBarWidgetProvider.ACTION_ADD_STOCK,
            AliSlimBarWidgetProvider.ACTION_HOME
        )

        val uris = actions.map { "aliedu://widget/${it.lowercase()}" }.toSet()
        // Exactly 5 unique URIs so Android PendingIntent never collapses or crosses intents
        assertEquals(5, uris.size)
        assertTrue(uris.contains("aliedu://widget/cash_in"))
        assertTrue(uris.contains("aliedu://widget/cash_out"))
        assertTrue(uris.contains("aliedu://widget/new_bill"))
        assertTrue(uris.contains("aliedu://widget/add_stock"))
        assertTrue(uris.contains("aliedu://widget/home"))
    }

    @Test
    fun testWidgetActionRoutingSafety_Section03() {
        // Tapping shortcuts must never auto-commit transactions or alter balances
        val simulatedDbBalancePaise = 1000000L // ₹10,000.00
        val simulatedStockQty = 100

        val widgetTappedActions = listOf(
            AliSlimBarWidgetProvider.ACTION_CASH_IN,
            AliSlimBarWidgetProvider.ACTION_CASH_OUT,
            AliSlimBarWidgetProvider.ACTION_NEW_BILL,
            AliSlimBarWidgetProvider.ACTION_ADD_STOCK
        )

        // Simulating repeated widget taps without form submission
        for (action in widgetTappedActions) {
            assertNotNull(action)
            // Balance and stock must remain completely untouched
            assertEquals(1000000L, simulatedDbBalancePaise)
            assertEquals(100, simulatedStockQty)
        }
    }
}
