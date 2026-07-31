package com.cyberdl.tiktok

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.cyberdl.tiktok.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }

    private lateinit var binding: ActivityMainBinding
    private val historyItems = mutableListOf<HistoryEntry>()
    private lateinit var adapter: HistoryAdapter
    private var mode: DownloadMode = DownloadMode.VIDEO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = try {
            DownloadMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: DownloadMode.VIDEO.name)
        } catch (e: IllegalArgumentException) {
            DownloadMode.VIDEO
        }
        applyModeUi()

        adapter = HistoryAdapter(historyItems)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDownload.setOnClickListener { startDownload() }
        binding.btnClear.setOnClickListener {
            binding.etUrl.setText("")
            binding.resultCard.visibility = View.GONE
            setStatus("menunggu link...", R.color.cyber_text_dim)
        }
    }

    private fun applyModeUi() {
        if (mode == DownloadMode.MP3) {
            binding.tvModeTitle.text = "AUDIO MODE"
            binding.tvTagline.text = "// tiktok atau youtube \u2192 extract audio"
            binding.tvDownloadBtnLabel.text = "EXTRACT AUDIO"
            binding.etUrl.hint = "Tempel link TikTok / YouTube..."
        } else {
            binding.tvModeTitle.text = "VIDEO MODE"
            binding.tvTagline.text = "// tempel target link di bawah"
            binding.tvDownloadBtnLabel.text = "DOWNLOAD VIDEO"
        }
    }

    private fun setStatus(text: String, colorRes: Int) {
        binding.tvStatus.text = "SYS// $text"
        binding.tvStatus.setTextColor(getColor(colorRes))
    }

    private fun startDownload() {
        val url = binding.etUrl.text.toString().trim()

        if (url.isEmpty()) {
            setStatus("link kosong, isi dulu bro.", R.color.cyber_magenta)
            return
        }
        if (!TikTokApi.isTikTokLink(url) && !(mode == DownloadMode.MP3 && YouTubeExtractor.isYouTubeLink(url))) {
            val hint = if (mode == DownloadMode.MP3) "link TikTok atau YouTube" else "link TikTok"
            setStatus("bukan $hint yang valid.", R.color.cyber_magenta)
            return
        }

        val actionWord = if (mode == DownloadMode.MP3) "audio" else "video"
        setStatus("mengambil data $actionWord...", R.color.cyber_cyan)
        binding.progressBar.progress = 20
        binding.btnDownload.isEnabled = false
        binding.resultCard.visibility = View.GONE
        binding.tvSaveChip.visibility = View.GONE

        val isYouTube = mode == DownloadMode.MP3 && YouTubeExtractor.isYouTubeLink(url)

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                if (isYouTube) YouTubeExtractor.resolve(url) else TikTokApi.resolve(url)
            }
            binding.btnDownload.isEnabled = true

            val downloadUrl = if (mode == DownloadMode.MP3) result.musicUrl else result.videoUrl

            if (!result.success || downloadUrl == null) {
                val errMsg = result.error
                    ?: if (mode == DownloadMode.MP3) "audio tidak ditemukan pada link ini." else "video tidak ditemukan pada link ini."
                setStatus("gagal: $errMsg", R.color.cyber_magenta)
                binding.progressBar.progress = 0
                return@launch
            }

            binding.progressBar.progress = 55
            showResultCard(result)

            setStatus("mengunduh $actionWord...", R.color.cyber_green)
            enqueueDownload(downloadUrl, result.title ?: "media_$actionWord")

            binding.progressBar.progress = 100
            setStatus("download dimulai! cek notifikasi.", R.color.cyber_green)
            binding.tvSaveChip.visibility = View.VISIBLE

            val time = DateFormat.format("HH:mm:ss", Date()).toString()
            adapter.addEntry(
                HistoryEntry(
                    title = result.title ?: "media_$actionWord",
                    time = time,
                    coverUrl = result.coverUrl,
                    likeCount = result.likeCount
                )
            )
            binding.etUrl.setText("")
        }
    }

    private fun showResultCard(result: TikTokResult) {
        binding.resultCard.visibility = View.VISIBLE
        binding.tvVidTitle.text = result.title
        binding.tvAuthor.text = result.author
        binding.tvLikes.text = TikTokApi.formatCount(result.likeCount)
        binding.tvComments.text = TikTokApi.formatCount(result.commentCount)
        binding.tvShares.text = TikTokApi.formatCount(result.shareCount)
        binding.tvViews.text = TikTokApi.formatCount(result.playCount)

        if (!result.coverUrl.isNullOrBlank()) {
            binding.ivThumb.load(result.coverUrl)
        }
    }

    private fun enqueueDownload(fileUrl: String, title: String) {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
        val extension = if (mode == DownloadMode.MP3) "mp3" else "mp4"
        val subFolder = if (mode == DownloadMode.MP3) "MP3" else "Video"
        val fileName = "${safeTitle}_${System.currentTimeMillis()}.$extension"

        val request = DownloadManager.Request(Uri.parse(fileUrl))
            .setTitle(title)
            .setDescription("CyberDL - media download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "CyberDL/$subFolder/$fileName"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        try {
            manager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memulai download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
