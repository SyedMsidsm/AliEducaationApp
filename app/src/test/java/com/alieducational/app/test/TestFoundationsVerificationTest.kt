package com.alieducational.app.test

import androidx.fragment.app.FragmentActivity
import com.alieducational.app.fakes.FakeBiometricAuthManager
import com.alieducational.app.fakes.FakeClock
import com.alieducational.app.testing.TestData
import com.alieducational.app.util.StepUpAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestFoundationsVerificationTest {

    private fun createDummyActivity(): FragmentActivity {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstanceMethod.invoke(unsafe, FragmentActivity::class.java) as FragmentActivity
    }

    @Test
    fun testFakeClock_SetAndAdvance() {
        val clock = FakeClock(1000L)
        assertEquals(1000L, clock.nowMillis())

        clock.advance(500L)
        assertEquals(1500L, clock.nowMillis())

        clock.set(5000L)
        assertEquals(5000L, clock.nowMillis())
    }

    @Test
    fun testFakeBiometricAuthManager_SuccessPath() {
        val bio = FakeBiometricAuthManager(canAuth = true, shouldSucceed = true)
        assertTrue(bio.canAuthenticate())

        var successCalled = false
        var errorMsg: String? = null
        val dummyActivity = createDummyActivity()

        bio.promptBiometric(
            activity = dummyActivity,
            title = "Test",
            subtitle = "Test Subtitle",
            onSuccess = { successCalled = true },
            onError = { errorMsg = it }
        )

        assertTrue("Success callback must be called", successCalled)
        assertEquals(null, errorMsg)
    }

    @Test
    fun testFakeBiometricAuthManager_ErrorPath() {
        val bio = FakeBiometricAuthManager(canAuth = true, shouldSucceed = false, errorMessage = "Fingerprint not recognized")
        var successCalled = false
        var errorMsg: String? = null
        val dummyActivity = createDummyActivity()

        bio.promptBiometric(
            activity = dummyActivity,
            title = "Test",
            subtitle = "Test Subtitle",
            onSuccess = { successCalled = true },
            onError = { errorMsg = it }
        )

        assertFalse("Success must not be called", successCalled)
        assertEquals("Fingerprint not recognized", errorMsg)
    }

    @Test
    fun testStepUpAuth_WithFakeClockAndFakeBio() {
        val clock = FakeClock(100_000L)
        val bio = FakeBiometricAuthManager(canAuth = true, shouldSucceed = true)
        val stepUp = StepUpAuth(bio, clock)
        val dummyActivity = createDummyActivity()

        var okCount = 0
        var failMsg: String? = null

        // 1. First invocation prompts biometric
        stepUp.require(
            activity = dummyActivity,
            title = "Step-up Auth",
            windowMs = 60_000L,
            onOk = { okCount++ },
            onFail = { failMsg = it }
        )

        assertEquals(1, okCount)
        assertEquals(null, failMsg)

        // 2. Immediate second invocation within 60s window succeeds without prompting bio
        clock.advance(30_000L) // 30s elapsed
        stepUp.require(
            activity = dummyActivity,
            title = "Step-up Auth",
            windowMs = 60_000L,
            onOk = { okCount++ },
            onFail = { failMsg = it }
        )

        assertEquals(2, okCount)

        // 3. Advancing past 60s window expires privilege
        clock.advance(35_000L) // 65s total since auth
        bio.shouldSucceed = false
        bio.errorMessage = "User cancelled"

        stepUp.require(
            activity = dummyActivity,
            title = "Step-up Auth",
            windowMs = 60_000L,
            onOk = { okCount++ },
            onFail = { failMsg = it }
        )

        assertEquals("Should not succeed on expired window when bio fails", 2, okCount)
        assertEquals("User cancelled", failMsg)
    }

    @Test
    fun testTestDataBuilders_ConstructValidEntities() {
        val product = TestData.product(name = "Notebook 100p", currentSellableStock = 50)
        assertNotNull(product.id)
        assertEquals("Notebook 100p", product.name)
        assertEquals(50, product.currentSellableStock)

        val customer = TestData.customer(name = "Zaid", currentBalancePaise = 15000L)
        assertEquals("Zaid", customer.name)
        assertEquals(15000L, customer.currentBalancePaise)

        val sale = TestData.sale(receiptNumber = "REC-999", netTotalPaise = 15000L)
        assertEquals("REC-999", sale.receiptNumber)
        assertEquals(15000L, sale.netTotalPaise)

        val line = TestData.saleLine(saleId = sale.id, productName = "Notebook 100p", quantityBase = 1)
        assertEquals(sale.id, line.saleId)
        assertEquals("Notebook 100p", line.productName)

        val expense = TestData.expense(category = "TEA_SNACKS", amountPaise = 3000L)
        assertEquals("TEA_SNACKS", expense.category)
        assertEquals(3000L, expense.amountPaise)
    }
}
