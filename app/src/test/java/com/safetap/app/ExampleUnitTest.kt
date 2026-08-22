package com.safetap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun routesStayStable() {
        assertEquals("splash", com.safetap.app.navigation.Routes.Splash)
    }
}
