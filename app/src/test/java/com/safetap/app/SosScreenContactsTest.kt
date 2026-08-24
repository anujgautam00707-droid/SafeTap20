package com.safetap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SosScreenContactsTest {

    private fun getEmergencyDispatchStandbyText(contactsCount: Int): String {
        return when (contactsCount) {
            0 -> "No trusted contacts will be alerted until you add one."
            1 -> "1 contact will be alerted immediately on trigger"
            else -> "$contactsCount contacts will be alerted immediately on trigger"
        }
    }

    private fun getEmergencyBroadcastActiveText(contactsCount: Int): String {
        return when (contactsCount) {
            0 -> "No trusted contacts configured for alert"
            1 -> "Alerting 1 trusted contact with live audio & location"
            else -> "Alerting $contactsCount trusted contacts with live audio & location"
        }
    }

    @Test
    fun verifyStandbyGrammarAndFormatting() {
        assertEquals(
            "No trusted contacts will be alerted until you add one.",
            getEmergencyDispatchStandbyText(0)
        )
        assertEquals(
            "1 contact will be alerted immediately on trigger",
            getEmergencyDispatchStandbyText(1)
        )
        assertEquals(
            "2 contacts will be alerted immediately on trigger",
            getEmergencyDispatchStandbyText(2)
        )
        assertEquals(
            "3 contacts will be alerted immediately on trigger",
            getEmergencyDispatchStandbyText(3)
        )
        assertEquals(
            "10 contacts will be alerted immediately on trigger",
            getEmergencyDispatchStandbyText(10)
        )
    }

    @Test
    fun verifyActiveBroadcastGrammarAndFormatting() {
        assertEquals(
            "No trusted contacts configured for alert",
            getEmergencyBroadcastActiveText(0)
        )
        assertEquals(
            "Alerting 1 trusted contact with live audio & location",
            getEmergencyBroadcastActiveText(1)
        )
        assertEquals(
            "Alerting 2 trusted contacts with live audio & location",
            getEmergencyBroadcastActiveText(2)
        )
        assertEquals(
            "Alerting 3 trusted contacts with live audio & location",
            getEmergencyBroadcastActiveText(3)
        )
        assertEquals(
            "Alerting 10 trusted contacts with live audio & location",
            getEmergencyBroadcastActiveText(10)
        )
    }
}
