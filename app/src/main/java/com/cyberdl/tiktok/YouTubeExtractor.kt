package com.cyberdl.tiktok

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.TimeUnit

/**
 * Downloader jembatan antara NewPipeExtractor dan OkHttp.
 * NewPipeExtractor butuh implementasi Downloader sendiri agar bisa
 * melakukan request HTTP ke YouTube.
 */
private class OkHttpDownloaderImpl(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: NPRequest): NPResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.toRequestBody("application/octet-stream".toMediaTypeOrNull())

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Android) CyberDL/1.0")

        for ((headerName, headerValueList) in headers) {
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (headerValue in headerValueList) {
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val body = response.body
        val responseBodyString = body?.string()
        val latestUrl = response.request.url.toString()

        return NPResponse(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyString,
            latestUrl
        )
    }
}

object YouTubeExtractor {

    private val urlRegex = Regex(
        "(https?://)?(www\\.|m\\.|music\\.)?(youtube\\.com/(watch\\?v=|shorts/)|youtu\\.be/)\\S+",
        RegexOption.IGNORE_CASE
    )

    private var initialized = false

    fun isYouTubeLink(url: String): Boolean = urlRegex.containsMatchIn(url.trim())

    private fun ensureInit() {
        if (!initialized) {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            NewPipe.init(OkHttpDownloaderImpl(client))
            initialized = true
        }
    }

    /**
     * Resolve link YouTube jadi data audio: url stream audio terbaik,
     * judul, uploader, thumbnail, dan statistik yang tersedia.
     * Fungsi ini blocking - wajib dipanggil dari background thread.
     */
    fun resolve(youtubeUrl: String): TikTokResult {
        return try {
            ensureInit()
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, youtubeUrl.trim())

            val bestAudio = streamInfo.audioStreams.maxByOrNull { it.averageBitrate }
                ?: return TikTokResult(success = false, error = "Tidak ada stream audio pada video ini.")

            val thumbnailUrl = streamInfo.thumbnails.maxByOrNull { it.height }?.url

            TikTokResult(
                success = true,
                musicUrl = bestAudio.content,
                title = streamInfo.name ?: "youtube_audio",
                author = streamInfo.uploaderName?.let { "@$it" } ?: "@youtube",
                coverUrl = thumbnailUrl,
                likeCount = if (streamInfo.likeCount > 0) streamInfo.likeCount else 0,
                commentCount = 0,
                shareCount = 0,
                playCount = if (streamInfo.viewCount > 0) streamInfo.viewCount else 0
            )
        } catch (e: Exception) {
            TikTokResult(
                success = false,
                error = "Gagal ambil audio YouTube: ${e.message ?: "coba link lain atau update aplikasi"}"
            )
        }
    }
}
