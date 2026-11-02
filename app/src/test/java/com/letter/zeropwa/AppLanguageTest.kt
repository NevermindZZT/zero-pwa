package com.letter.zeropwa

import com.letter.zeropwa.data.AppLanguageOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun verifyLanguageTagResolution() {
        assertEquals(AppLanguageOption.SYSTEM, AppLanguageOption.forTag(""))
        assertEquals(AppLanguageOption.SYSTEM, AppLanguageOption.forTag("   "))
        assertEquals(AppLanguageOption.ENGLISH, AppLanguageOption.forTag("en"))
        assertEquals(AppLanguageOption.ENGLISH, AppLanguageOption.forTag("en-US"))
        assertEquals(AppLanguageOption.SIMPLIFIED_CHINESE, AppLanguageOption.forTag("zh-Hans"))
        assertEquals(AppLanguageOption.SIMPLIFIED_CHINESE, AppLanguageOption.forTag("zh-CN"))
    }

    @Test
    fun verifyLanguageStringResources() {
        for (option in AppLanguageOption.entries) {
            assertNotEquals(0, option.labelRes)
        }
    }
}
