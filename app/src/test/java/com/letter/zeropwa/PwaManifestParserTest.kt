package com.letter.zeropwa

import com.letter.zeropwa.data.parser.PwaManifestParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PwaManifestParserTest {

    @Test
    fun testInspectJourneySite() = runBlocking {
        val result = PwaManifestParser.inspectUrl("https://journey.nevermindzzt.top/")
        assertTrue("探测应成功", result.isSuccess)
        val candidate = result.getOrNull()
        assertNotNull(candidate)
        println("探测结果: name=${candidate?.name}, iconUrl=${candidate?.iconUrl}, themeColor=${candidate?.themeColor}")

        assertEquals("JourneyIn", candidate?.name)
        assertNotNull("应该成功提取到图标链接", candidate?.iconUrl)
        assertTrue("图标应该是 svg 格式", candidate?.iconUrl?.endsWith(".svg") == true)
        assertEquals("https://journey.nevermindzzt.top/favicon.svg", candidate?.iconUrl)
    }
}
