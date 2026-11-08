package com.alieducational.app.ui

import com.alieducational.app.R
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AliLogoUnitTest {

    @Test
    fun testLogoResourcesExist() {
        val darkRes = R.drawable.ali_logo_dark
        val lightRes = R.drawable.ali_logo_light
        assertTrue(darkRes != 0)
        assertTrue(lightRes != 0)
        assertNotEquals(darkRes, lightRes)
    }
}
