package com.letter.zeropwa.data.parser

import android.test.AndroidTestCase
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Uses Android's real org.json implementation; no external server is required. */
class PwaManifestRedirectTest : AndroidTestCase() {
    fun testRedirectedManifestResolvesAgainstFinalManifestUrl() = runBlocking {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val url = chain.request().url.toString()
            requests.add(url)
            val finalUrl: String
            val body: String
            if (url.endsWith("/entry")) {
                finalUrl = "https://example.com/proxy/app/deep/page"
                body = """<link rel="alternate manifest" href="../manifest.webmanifest">"""
            } else {
                finalUrl = "https://example.com/proxy/assets/v2/site.webmanifest"
                body = """{"name":"App", "start_url":"./home", "scope":"./", "icons":[{"src":"../icons/app.png", "sizes":"512x512"}]}"""
            }
            Response.Builder().request(chain.request().newBuilder().url(finalUrl).build())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody()).build()
        }.build()
        val candidate = PwaManifestParser.inspectUrl("https://example.com/entry", client).getOrThrow()
        assertEquals("https://example.com/proxy/app/manifest.webmanifest", requests[1])
        assertEquals("https://example.com/proxy/app/deep/page", candidate.startUrl)
        assertEquals("https://example.com/proxy/assets/v2/", candidate.scope)
        assertEquals("https://example.com/proxy/assets/icons/app.png", candidate.iconUrl)
    }
}
