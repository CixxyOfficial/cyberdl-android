package com.cyberdl.tiktok

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TikTokResult(
    val success: Boolean,
    val videoUrl: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val playCount: Long = 0,
    val error: String? = null
)

object TikTokApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val urlRegex = Regex(
        "(https?://)?(www\\.|vt\\.|vm\\.)?tiktok\\.com/\\S+",
        RegexOption.IGNORE_CASE
    )

    fun isTikTokLink(url: String): Boolean = urlRegex.containsMatchIn(url.trim())

    /** Format angka besar jadi ringkas: 128000 -> "128K", 1200000 -> "1.2M" */
    fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    /**
     * Resolve link TikTok jadi data lengkap: url video no-watermark, judul,
     * author, thumbnail, dan statistik (like/comment/share/views).
     * Fungsi ini blocking - wajib dipanggil dari background thread.
     */
    fun resolve(tiktokUrl: String): TikTokResult {
        val encoded = URLEncoder.encode(tiktokUrl.trim(), "UTF-8")
        val apiUrl = "https://www.tikwm.com/api/?url=$encoded&hd=1"

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "Mozilla/5.0 (Android) CyberDL/1.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return TikTokResult(success = false, error = "Server API error: ${response.code}")
                }
                val body = response.body?.string() ?: return TikTokResult(
                    success = false, error = "Respons kosong dari server."
                )
                val json = JSONObject(body)
                if (json.optInt("code", -1) != 0) {
                    return TikTokResult(
                        success = false,
                        error = json.optString("msg", "Gagal memproses link.")
                    )
                }
                val data = json.getJSONObject("data")
                val play = data.optString("hdplay").ifBlank { data.optString("play") }
                val title = data.optString("title").ifBlank { "tiktok_video" }
                val cover = data.optString("cover")
                val authorObj = data.optJSONObject("author")
                val authorName = authorObj?.optString("nickname")?.ifBlank { authorObj.optString("unique_id") }
                    ?: "unknown"

                if (play.isBlank()) {
                    TikTokResult(success = false, error = "Tidak menemukan video pada link ini.")
                } else {
                    TikTokResult(
                        success = true,
                        videoUrl = play,
                        title = title,
                        author = "@$authorName",
                        coverUrl = cover,
                        likeCount = data.optLong("digg_count", 0),
                        commentCount = data.optLong("comment_count", 0),
                        shareCount = data.optLong("share_count", 0),
                        playCount = data.optLong("play_count", 0)
                    )
                }
            }
        } catch (e: Exception) {
            TikTokResult(success = false, error = e.message ?: "Terjadi kesalahan jaringan.")
        }
    }
}
