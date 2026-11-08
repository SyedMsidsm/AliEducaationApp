package com.alieducational.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerOnlyGuardTest {

    private fun checkIsOwnerAllowed(role: String): Boolean {
        return role.uppercase() == "OWNER"
    }

    @Test
    fun testOwnerRole_Allowed() {
        assertTrue("OWNER uppercase must be allowed", checkIsOwnerAllowed("OWNER"))
        assertTrue("owner lowercase must be allowed", checkIsOwnerAllowed("owner"))
        assertTrue("Owner mixed case must be allowed", checkIsOwnerAllowed("Owner"))
    }

    @Test
    fun testNonOwnerRole_FailClosed() {
        assertFalse("STAFF must be denied", checkIsOwnerAllowed("STAFF"))
        assertFalse("staff lowercase must be denied", checkIsOwnerAllowed("staff"))
        assertFalse("UNKNOWN must be denied", checkIsOwnerAllowed("UNKNOWN"))
        assertFalse("Empty string must be denied", checkIsOwnerAllowed(""))
        assertFalse("Guest must be denied", checkIsOwnerAllowed("GUEST"))
    }
}
