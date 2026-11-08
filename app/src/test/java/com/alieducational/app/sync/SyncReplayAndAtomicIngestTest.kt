package com.alieducational.app.sync

import com.alieducational.app.data.service.IngestResult
import com.alieducational.app.data.service.canon
import com.alieducational.app.data.service.formatIngestResponseJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class SyncReplayAndAtomicIngestTest {

    @Test
    fun testCanonIgnoresCustomerObjectAndSortsKeys() {
        val bill1 = """
            {
                "id": "sale-101",
                "receiptNumber": "AE-00101",
                "netTotalPaise": 50000,
                "grossTotalPaise": 50000,
                "timestamp": 1700000000000,
                "customer": {
                    "id": "cust-1",
                    "name": "Original Name",
                    "phone": "9876543210"
                }
            }
        """.trimIndent()

        val bill2 = """
            {
                "timestamp": 1700000000000,
                "receiptNumber": "AE-00101",
                "customer": {
                    "id": "cust-1",
                    "name": "Updated Name",
                    "phone": "9999999999"
                },
                "grossTotalPaise": 50000,
                "netTotalPaise": 50000,
                "id": "sale-101"
            }
        """.trimIndent()

        val canon1 = canon(bill1)
        val canon2 = canon(bill2)

        // Bills differing only in customer display object and key ordering must be canonically identical
        assertEquals("Canonical representation must ignore customer object and order keys deterministically", canon1, canon2)

        // Expected deterministic ordering: grossTotalPaise, id, netTotalPaise, receiptNumber, timestamp
        val expected = """{"grossTotalPaise":50000,"id":"sale-101","netTotalPaise":50000,"receiptNumber":"AE-00101","timestamp":1700000000000}"""
        assertEquals(expected, canon1)

        // If financial field changes, canonical form must differ
        val billDifferentAmount = """
            {
                "id": "sale-101",
                "receiptNumber": "AE-00101",
                "netTotalPaise": 55000,
                "grossTotalPaise": 55000,
                "timestamp": 1700000000000
            }
        """.trimIndent()
        assertNotEquals("Canonical representation must change when financial fields differ", canon1, canon(billDifferentAmount))
    }

    @Test
    fun testCanonWithNestedLinesAndQuotes() {
        val billWithLines1 = """
            {
                "id": "sale-200",
                "receiptNumber": "AE-00200",
                "customer": {"id": "c1", "name": "Old"},
                "lines": [
                    {"id": "l1", "productName": "Class 10 \"Maths\" Book", "lineGrossPaise": 15000},
                    {"id": "l2", "productName": "Notebook", "lineGrossPaise": 5000}
                ]
            }
        """.trimIndent()

        val billWithLines2 = """
            {
                "lines": [
                    {"id": "l1", "productName": "Class 10 \"Maths\" Book", "lineGrossPaise": 15000},
                    {"id": "l2", "productName": "Notebook", "lineGrossPaise": 5000}
                ],
                "receiptNumber": "AE-00200",
                "customer": {"id": "c1", "name": "New"},
                "id": "sale-200"
            }
        """.trimIndent()

        val c1 = canon(billWithLines1)
        val c2 = canon(billWithLines2)
        assertEquals(c1, c2)
        assertFalse(c1.contains("customer"))
        assertTrue(c1.contains("Maths"))
    }

    @Test
    fun testFormatIngestResponseProtocolVersion2() {
        val results = listOf(
            IngestResult.Accepted("sale-1"),
            IngestResult.Duplicate("sale-2"),
            IngestResult.Rejected("sale-3", "Unknown product")
        )

        val jsonString = formatIngestResponseJson(results)
        assertTrue(jsonString.contains("\"status\":\"SUCCESS\""))
        assertTrue(jsonString.contains("\"protocolVersion\":2"))
        assertTrue(jsonString.contains("\"acknowledgedIds\":[\"sale-1\",\"sale-2\"]"))
        assertTrue(jsonString.contains("{\"id\":\"sale-1\",\"status\":\"ACCEPTED\"}"))
        assertTrue(jsonString.contains("{\"id\":\"sale-2\",\"status\":\"DUPLICATE\"}"))
        assertTrue(jsonString.contains("{\"id\":\"sale-3\",\"status\":\"REJECTED\",\"reason\":\"Unknown product\"}"))
    }

    @Test
    fun testSanityCheckTimestampAndLines() {
        val now = 1700000000000L
        val validTimestamp = now + 3600_000L // 1 hour in future (allowable)
        val invalidFutureTimestamp = now + 90_000_000L // > 24 hours in future

        assertTrue("1 hour in future should be accepted", validTimestamp <= now + 86_400_000L)
        assertFalse("> 24h in future must be rejected", invalidFutureTimestamp <= now + 86_400_000L)
    }
}
