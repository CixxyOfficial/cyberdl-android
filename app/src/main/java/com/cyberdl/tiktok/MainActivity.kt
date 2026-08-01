package com.cyberdl.tiktok

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
        const val EXTRA_PLATFORM = "extra_platform"
    }

    private lateinit var binding: ActivityMainBinding
    private val historyItems = mutableListOf<HistoryEntry>()
    private lateinit var adapter: HistoryAdapter
    private var mode: DownloadMode = DownloadMode.VIDEO
    private var platform: Platform = Platform.TIKTOK

    // State setelah resolve sukses, menunggu user pilih resolusi (kalau ada)
    private var lastResult: TikTokResult? = null
    private var selectedVideoOption: VideoOption? = null
    private var awaitingResolutionChoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = try {
            DownloadMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: DownloadMode.VIDEO.name)
        } catch (e: IllegalArgumentException) {
            DownloadMode.VIDEO
        }
        platform = try {
            Platform.valueOf(intent.getStringExtra(EXTRA_PLATFORM) ?: Platform.TIKTOK.name)
        } catch (e: IllegalArgumentException) {
            Platform.TIKTOK
        }
        applyModeUi()

        adapter = HistoryAdapter(historyItems)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDownload.setOnClickListener { onPrimaryButtonClicked() }
        binding.btnClear.setOnClickListener { resetForm() }
    }

    private fun applyModeUi() {
        val platformName = if (platform == Platform.TIKTOK) "TikTok" else "YouTube"
        if (mode == DownloadMode.MP3) {
            binding.tvModeTitle.text = "$platformName · Audio"
            binding.tvTagline.text = "Tempel link $platformName"
            binding.etUrl.hint = "Tempel link $platformName..."
        } else {
            binding.tvModeTitle.text = "$platformName · Video"
            binding.tvTagline.text = "Tempel link $platformName"
            binding.etUrl.hint = "Tempel link $platformName..."
        }
        binding.tvDownloadBtnLabel.text = "Proses Link"
    }

    private fun resetForm() {
        binding.etUrl.setText("")
        binding.resultCard.visibility = View.GONE
        binding.resolutionSection.visibility = View.GONE
        binding.resolutionChipContainer.removeAllViews()
        binding.tvDownloadBtnLabel.text = "Proses Link"
        awaitingResolutionChoice = false
        lastResult = null
        selectedVideoOption = null
        setStatus("menunggu link...", R.color.text_muted)
    }

    private fun setStatus(text: String, colorRes: Int) {
        binding.tvStatus.text = text
        binding.tvStatus.setTextColor(getColor(colorRes))
    }

    private fun onPrimaryButtonClicked() {
        if (awaitingResolutionChoice) {
            confirmVideoDownload()
        } else {
            resolveLink()
        }
    }

    private fun resolveLink() {
        val url = binding.etUrl.text.toString().trim()

        if (url.isEmpty()) {
            setStatus("link kosong, isi dulu ya.", R.color.danger)
            return
        }

        val platformName = if (platform == Platform.TIKTOK) "TikTok" else "YouTube"
        val linkMatchesPlatform = if (platform == Platform.TIKTOK) {
            TikTokApi.isTikTokLink(url)
        } else {
            YouTubeExtractor.isYouTubeLink(url)
        }

        if (!linkMatchesPlatform) {
            setStatus("bukan link $platformName yang valid.", R.color.danger)
            return
        }

        val actionWord = if (mode == DownloadMode.MP3) "audio" else "video"
        setStatus("mengambil data $actionWord...", R.color.accent)
        binding.progressBar.progress = 20
        binding.btnDownload.isEnabled = false
        binding.resultCard.visibility = View.GONE
        binding.tvSaveChip.visibility = View.GONE
        binding.resolutionSection.visibility = View.GONE
        binding.resolutionChipContainer.removeAllViews()

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                when {
                    platform == Platform.YOUTUBE && mode == DownloadMode.MP3 -> YouTubeExtractor.resolveAudio(url)
                    platform == Platform.YOUTUBE && mode == DownloadMode.VIDEO -> YouTubeExtractor.resolveVideo(url)
                    else -> TikTokApi.resolve(url)
                }
            }
            binding.btnDownload.isEnabled = true

            if (!result.success) {
                setStatus("gagal: ${result.error ?: "coba lagi"}", R.color.danger)
                binding.progressBar.progress = 0
                return@launch
            }

            binding.progressBar.progress = 55
            showResultCard(result)
            lastResult = result

            if (mode == DownloadMode.VIDEO && result.videoOptions.size > 1) {
                // Butuh pilih resolusi dulu sebelum download
                setupResolutionChips(result.videoOptions)
                binding.progressBar.progress = 0
                binding.tvDownloadBtnLabel.text = "Download ${result.videoOptions.first().label}"
                awaitingResolutionChoice = true
                setStatus("pilih resolusi, lalu tap Download.", R.color.text_secondary)
            } else {
                // Langsung download (TikTok, audio, atau video dengan 1 opsi saja)
                val downloadUrl = if (mode == DownloadMode.MP3) result.musicUrl else result.videoUrl
                if (downloadUrl == null) {
                    setStatus("file tidak ditemukan pada link ini.", R.color.danger)
                    binding.progressBar.progress = 0
                    return@launch
                }
                finishDownload(downloadUrl, result, resolutionLabel = null)
            }
        }
    }

    private fun setupResolutionChips(options: List<VideoOption>) {
        binding.resolutionSection.visibility = View.VISIBLE
        binding.resolutionChipContainer.removeAllViews()
        selectedVideoOption = options.first()

        val density = resources.displayMetrics.density
        val hPad = (16 * density).toInt()
        val vPad = (9 * density).toInt()
        val marginEnd = (8 * density).toInt()

        options.forEach { option ->
            val chip = TextView(this).apply {
                text = option.label
                setPadding(hPad, vPad, hPad, vPad)
                textSize = 12.5f
                setTextColor(if (option == selectedVideoOption) getColor(R.color.on_accent) else getColor(R.color.text_secondary))
                setBackgroundResource(if (option == selectedVideoOption) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { this.marginEnd = marginEnd }
                setOnClickListener {
                    selectedVideoOption = option
                    binding.tvDownloadBtnLabel.text = "Download ${option.label}"
                    refreshChipStyles(options)
                }
            }
            binding.resolutionChipContainer.addView(chip)
        }
    }

    private fun refreshChipStyles(options: List<VideoOption>) {
        for (i in 0 until binding.resolutionChipContainer.childCount) {
            val chipView = binding.resolutionChipContainer.getChildAt(i) as? TextView ?: continue
            val option = options.getOrNull(i) ?: continue
            val isSelected = option == selectedVideoOption
            chipView.setTextColor(if (isSelected) getColor(R.color.on_accent) else getColor(R.color.text_secondary))
            chipView.setBackgroundResource(if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        }
    }

    private fun confirmVideoDownload() {
        val result = lastResult ?: return
        val option = selectedVideoOption ?: return
        finishDownload(option.url, result, resolutionLabel = option.label)
    }

    private fun finishDownload(downloadUrl: String, result: TikTokResult, resolutionLabel: String?) {
        val actionWord = if (mode == DownloadMode.MP3) "audio" else "video"
        setStatus("mengunduh $actionWord...", R.color.accent)
        enqueueDownload(downloadUrl, result.title ?: "media_$actionWord", resolutionLabel)

        binding.progressBar.progress = 100
        setStatus("download dimulai, cek notifikasi.", R.color.success)
        binding.tvSaveChip.visibility = View.VISIBLE

        val time = DateFormat.format("HH:mm", Date()).toString()
        adapter.addEntry(
            HistoryEntry(
                title = result.title ?: "media_$actionWord",
                time = time,
                coverUrl = result.coverUrl,
                likeCount = result.likeCount
            )
        )

        binding.etUrl.setText("")
        binding.resolutionSection.visibility = View.GONE
        binding.tvDownloadBtnLabel.text = "Proses Link"
        awaitingResolutionChoice = false
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

    private fun enqueueDownload(fileUrl: String, title: String, resolutionLabel: String?) {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
        val extension = if (mode == DownloadMode.MP3) "mp3" else "mp4"
        val subFolder = if (mode == DownloadMode.MP3) "MP3" else "Video"
        val resSuffix = if (resolutionLabel != null) "_$resolutionLabel" else ""
        val fileName = "${safeTitle}${resSuffix}_${System.currentTimeMillis()}.$extension"

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
