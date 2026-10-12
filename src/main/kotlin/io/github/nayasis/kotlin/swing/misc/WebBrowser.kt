package io.github.nayasis.kotlin.swing.misc

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

class WebBrowser(
    timeout: Int = 30_000,
    userAgent: String? = null,
): Closeable {

    private val agent = userAgent ?: DEFAULT_USER_AGENTS.pick()

    // Accept-Encoding is left to OkHttp: it only decompresses (gzip) when it sets the header itself.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .callTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .build()

    /**
     * Downloads content from a URL with custom headers.
     *
     * @param url the URL to download from
     * @param headers additional headers to include
     * @return byte array of the downloaded content
     * @throws IOException if the download fails
     */
    fun downloadContent(url: URL, headers: Map<String,String>? = null): ByteArray {
        val request = Request.Builder().url(url).apply {
            header("User-Agent"     , agent)
            header("Accept"         , "image/webp,image/apng,image/*,*/*;q=0.8")
            header("Accept-Language", "ko-KR,ko;q=0.9,ja-JP,ja;q=0.8,en-US;q=0.7,en;q=0.6")
            header("Cache-Control"  , "no-cache")
            header("Pragma"         , "no-cache")
            header("Sec-Fetch-Dest" , "image")
            header("Sec-Fetch-Mode" , "no-cors")
            header("Sec-Fetch-Site" , "cross-site")
            headers?.forEach { (name, value) -> header(name, value) }
        }.build()
        return httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body.bytes()
            } else {
                throw IOException("HTTP error: ${response.code} ${response.message}")
            }
        }
    }

    override fun close() {
        runCatching {
            httpClient.dispatcher.executorService.shutdown()
            httpClient.connectionPool.evictAll()
        }
    }
}

private val DEFAULT_USER_AGENTS = UserAgents()
