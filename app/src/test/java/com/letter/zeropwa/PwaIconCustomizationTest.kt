package com.letter.zeropwa

import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.data.parser.PwaCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PwaIconCustomizationTest {

    @Test
    fun verifyCustomIconUrlOverride() {
        val candidate = PwaCandidate(
            name = "Test Web",
            pageUrl = "https://example.com/deep/page",
            startUrl = "https://example.com/deep/page",
            iconUrl = "https://example.com/default.png"
        )

        val customIcon = "https://www.deepseek.com/harness/favicon.svg"
        val updatedCandidate = candidate.copy(iconUrl = customIcon)

        assertEquals(customIcon, updatedCandidate.iconUrl)
    }

    @Test
    fun verifyPwaItemIconUpdate() {
        val original = PwaItem(
            id = 1L,
            name = "My App",
            url = "https://example.com",
            startUrl = "https://example.com",
            iconUrl = "https://example.com/old.png",
            iconPath = "/data/user/0/com.letter.zeropwa/files/pwa_icons/icon_1.png"
        )

        val newSvgIcon = "https://www.deepseek.com/harness/favicon.svg"
        val updated = original.copy(
            iconUrl = newSvgIcon
        )

        assertEquals(newSvgIcon, updated.iconUrl)
        assertEquals(original.iconPath, updated.iconPath)
    }

    @Test
    fun verifyIconStringResources() {
        assertNotEquals(0, R.string.icon_manage_title)
        assertNotEquals(0, R.string.icon_refetch)
        assertNotEquals(0, R.string.icon_custom_url)
        assertNotEquals(0, R.string.icon_apply)
    }
}
