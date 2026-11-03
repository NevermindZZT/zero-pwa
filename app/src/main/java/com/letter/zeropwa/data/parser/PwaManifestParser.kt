package com.letter.zeropwa.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class PwaCandidate(
    val name: String,
    val shortName: String? = null,
    val pageUrl: String,
    val startUrl: String,
    val scope: String? = null,
    val iconUrl: String? = null,
    val iconCandidates: List<String> = emptyList(),
    val themeColor: Int? = null,
    val backgroundColor: Int? = null,
    val displayMode: String = "standalone",
    val orientation: String = "any"
)

object PwaManifestParser {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    suspend fun inspectUrl(inputUrl: String): Result<PwaCandidate> = inspectUrl(inputUrl, httpClient)

    internal suspend fun inspectUrl(inputUrl: String, client: OkHttpClient): Result<PwaCandidate> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(inputUrl)
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", DEFAULT_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val (finalUrl, html) = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP 响应错误: ${response.code}"))
                }
                response.request.url.toString() to response.body?.string().orEmpty()
            }
            val doc = Jsoup.parse(html, finalUrl)

            var name = doc.title().trim()
            val appNameMeta = doc.select("meta[name=application-name], meta[property=og:site_name]").attr("content").trim()
            if (appNameMeta.isNotBlank()) {
                name = appNameMeta
            }

            var themeColor = parseColorSafely(doc.select("meta[name=theme-color]").attr("content"))
            var backgroundColor = parseColorSafely(doc.select("meta[name=background-color]").attr("content"))

            var manifestUrl: String? = null
            val links = doc.select("link[href]")
            fun hasRel(rel: String, token: String): Boolean =
                rel.split(Regex("\\s+")).any { it.equals(token, ignoreCase = true) }
            val manifestElement = links.firstOrNull {
                hasRel(it.attr("rel"), "manifest") ||
                    (hasRel(it.attr("rel"), "alternate") && it.attr("type").equals("application/json", ignoreCase = true))
            }
            if (manifestElement != null) {
                val href = manifestElement.attr("abs:href")
                if (href.isNotBlank()) {
                    manifestUrl = href
                }
            }

            val fallbackIcons = mutableListOf<Pair<String, Int>>() // url, score
            // Apple touch icons (一般是高质量 180x180 png)
            links.filter { hasRel(it.attr("rel"), "apple-touch-icon") || hasRel(it.attr("rel"), "apple-touch-icon-precomposed") }.forEach { elem ->
                val href = elem.attr("abs:href")
                if (href.isNotBlank()) fallbackIcons.add(href to 200)
            }
            // Favicons (包含 svg 或 png)
            links.filter { hasRel(it.attr("rel"), "icon") || hasRel(it.attr("rel"), "mask-icon") }.forEach { elem ->
                val href = elem.attr("abs:href")
                val type = elem.attr("type")
                if (href.isNotBlank()) {
                    val score = if (type.contains("svg") || href.endsWith(".svg", ignoreCase = true)) 350 else 100
                    fallbackIcons.add(href to score)
                }
            }
            // og:image
            val ogImage = doc.select("meta[property='og:image'], meta[name='twitter:image']").attr("content")
            if (ogImage.isNotBlank()) {
                fallbackIcons.add(resolveUrl(doc.baseUri(), ogImage) to 80)
            }

            var shortName: String? = null
            var startUrl = finalUrl
            var scope: String? = null
            val manifestIcons = mutableListOf<Pair<String, Int>>()
            var displayMode = "standalone"
            var orientation = "any"

            if (manifestUrl != null) {
                try {
                    val manifestReq = Request.Builder()
                        .url(manifestUrl)
                        .header("User-Agent", DEFAULT_UA)
                        .header("Accept", "application/json,application/manifest+json,*/*")
                        .build()
                    client.newCall(manifestReq).execute().use { manifestRes ->
                        if (manifestRes.isSuccessful) {
                            val manifestJsonStr = manifestRes.body?.string().orEmpty()
                            if (manifestJsonStr.isNotBlank()) {
                                val json = JSONObject(manifestJsonStr)
                                val mName = json.optString("name").trim()
                                val mShortName = json.optString("short_name").trim()
                                if (mName.isNotBlank()) name = mName
                                if (mShortName.isNotBlank()) shortName = mShortName

                                // 保留用户输入页面的最终地址，不让站点级 manifest 把深层链接改回首页。
                                val mScope = json.optString("scope").trim()
                                if (mScope.isNotBlank()) {
                                    scope = resolveUrl(manifestRes.request.url.toString(), mScope)
                                }

                                val mThemeColor = parseColorSafely(json.optString("theme_color"))
                                if (mThemeColor != null) themeColor = mThemeColor

                                val mBgColor = parseColorSafely(json.optString("background-color"))
                                if (mBgColor != null) backgroundColor = mBgColor

                                val mDisplay = json.optString("display").trim()
                                if (mDisplay.isNotBlank()) displayMode = mDisplay

                                val mOrientation = json.optString("orientation").trim()
                                if (mOrientation.isNotBlank()) orientation = mOrientation

                                val iconsArray = json.optJSONArray("icons")
                                if (iconsArray != null) {
                                    manifestIcons.addAll(extractBestIcons(manifestRes.request.url.toString(), iconsArray))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 根域名默认图标探测备选
            val rootIcons = mutableListOf<Pair<String, Int>>()
            try {
                val hostUri = URI(finalUrl)
                val baseRoot = "${hostUri.scheme}://${hostUri.authority}"
                rootIcons.add("$baseRoot/favicon.ico" to 49)
            } catch (_: Exception) {}

            // 页面声明的 icon 反映当前路径，优先于可能属于整个站点的 manifest。
            val sortedIcons = (fallbackIcons.filter { it.second >= 100 }.sortedByDescending { it.second } +
                manifestIcons.sortedByDescending { it.second } +
                fallbackIcons.filter { it.second < 100 }.sortedByDescending { it.second } + rootIcons)
                .map { it.first }
                .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
                .distinct()
            val chosenIconUrl = sortedIcons.firstOrNull()

            if (name.isBlank()) {
                name = try {
                    URI(finalUrl).host ?: "Web App"
                } catch (_: Exception) {
                    "Web App"
                }
            }

            Result.success(
                PwaCandidate(
                    name = name,
                    shortName = shortName,
                    pageUrl = finalUrl,
                    startUrl = startUrl,
                    scope = scope,
                    iconUrl = chosenIconUrl,
                    iconCandidates = sortedIcons,
                    themeColor = themeColor,
                    backgroundColor = backgroundColor,
                    displayMode = displayMode,
                    orientation = orientation
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        // 优先使用 Coil (支持 SVG 矢量渲染、WebP、PNG 等)
        try {
            val imageLoader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(512, 512)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            val drawable = result.drawable
            if (drawable != null) {
                return@withContext (drawable as? BitmapDrawable)?.bitmap
                    ?: drawable.toBitmap(512, 512, Bitmap.Config.ARGB_8888)
            }
        } catch (_: Exception) {}

        // 回退到纯 OkHttp + BitmapFactory 原生字节解码
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_UA)
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    private fun extractBestIcons(baseUrl: String, iconsArray: JSONArray): List<Pair<String, Int>> {
        val parsed = mutableListOf<Pair<String, Int>>()

        for (i in 0 until iconsArray.length()) {
            val item = iconsArray.optJSONObject(i) ?: continue
            val src = item.optString("src").trim()
            if (src.isBlank()) continue
            val absUrl = resolveUrl(baseUrl, src)
            val sizes = item.optString("sizes")
            val type = item.optString("type")

            var score = 100
            val isSvg = type.contains("svg") || absUrl.endsWith(".svg", ignoreCase = true)
            if (isSvg) {
                score = 450 // SVG 矢量无限清晰，优先级极高
            } else if (sizes.contains("512x512")) {
                score = 512
            } else if (sizes.contains("192x192")) {
                score = 380
            } else if (sizes.contains("256x256")) {
                score = 300
            } else if (sizes.contains("144x144")) {
                score = 180
            } else if (sizes.contains("any")) {
                score = 320
            } else {
                val m = Pattern.compile("(\\d+)x(\\d+)").matcher(sizes)
                if (m.find()) {
                    score = m.group(1)?.toIntOrNull() ?: 100
                }
            }

            parsed.add(absUrl to score)
        }

        return parsed
    }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return "https://$trimmed"
    }

    fun resolveUrl(base: String, relative: String): String {
        return base.toHttpUrlOrNull()?.resolve(relative)?.toString() ?: relative
    }

    fun parseColorSafely(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val clean = raw.trim().lowercase()
        return try {
            when {
                clean.startsWith("#") -> {
                    when (clean.length) {
                        4 -> {
                            val r = clean[1]
                            val g = clean[2]
                            val b = clean[3]
                            Color.parseColor("#$r$r$g$g$b$b")
                        }
                        7, 9 -> Color.parseColor(clean)
                        else -> null
                    }
                }
                clean.startsWith("rgb(") -> {
                    val parts = clean.removePrefix("rgb(").removeSuffix(")").split(",")
                    if (parts.size >= 3) {
                        val r = parts[0].trim().toInt()
                        val g = parts[1].trim().toInt()
                        val b = parts[2].trim().toInt()
                        Color.rgb(r, g, b)
                    } else null
                }
                clean.startsWith("rgba(") -> {
                    val parts = clean.removePrefix("rgba(").removeSuffix(")").split(",")
                    if (parts.size >= 4) {
                        val r = parts[0].trim().toInt()
                        val g = parts[1].trim().toInt()
                        val b = parts[2].trim().toInt()
                        val a = (parts[3].trim().toFloat() * 255).toInt().coerceIn(0, 255)
                        Color.argb(a, r, g, b)
                    } else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
