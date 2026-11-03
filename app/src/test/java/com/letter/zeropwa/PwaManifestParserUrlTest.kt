package com.letter.zeropwa

import com.letter.zeropwa.data.parser.PwaManifestParser
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PwaManifestParserUrlTest {
    @Test
    fun resolvesRelativeReferencesLikeBrowser() {
        val base = "https://example.com/proxy/app/page?old=1"
        assertEquals("https://example.com/proxy/app/icon.png", PwaManifestParser.resolveUrl(base, "icon.png"))
        assertEquals("https://example.com/proxy/icon.png", PwaManifestParser.resolveUrl(base, "../icon.png"))
        assertEquals("https://example.com/proxy/app/page?new=1", PwaManifestParser.resolveUrl(base, "?new=1"))
        assertEquals("https://cdn.example.com/i.png", PwaManifestParser.resolveUrl(base, "//cdn.example.com/i.png"))
    }

    @Test
    fun pageRedirectAndBaseElementPreserveProxyContext() = runBlocking {
        val root = "https://example.com"
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            // Model the response after OkHttp has followed the page redirect.
            val finalRequest = chain.request().newBuilder().url("$root/proxy/app/deep/page").build()
            Response.Builder().request(finalRequest).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""<html><head><title>Proxy app</title><base href="../assets/"><link rel="alternate apple-touch-icon" href="icon.png"></head></html>""".toResponseBody())
                .build()
        }.build()
        val candidate = PwaManifestParser.inspectUrl("$root/entry", client).getOrThrow()
        assertEquals("$root/proxy/app/deep/page", candidate.pageUrl)
        assertEquals("$root/proxy/app/deep/page", candidate.startUrl)
        assertEquals("$root/proxy/app/assets/icon.png", candidate.iconUrl)
    }
}
