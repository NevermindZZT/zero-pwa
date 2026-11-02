package com.letter.zeropwa

import com.letter.zeropwa.data.model.PwaItem
import org.junit.Assert.assertTrue
import org.junit.Test

class PwaSplashScreenTest {

    @Test
    fun defaultPwaItemHasSplashScreenEnabled() {
        val pwa = PwaItem(
            name = "Test PWA",
            url = "https://example.com",
            startUrl = "https://example.com"
        )
        assertTrue("新建 PWA 默认应开启品牌过渡屏以防止白屏", pwa.enableSplashScreen)
        assertTrue("新建 PWA 默认应开启悬浮手柄", pwa.enableFloatingToolHandle)
    }
}
