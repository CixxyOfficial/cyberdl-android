package com.cyberdl.tiktok

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load
import com.cyberdl.tiktok.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Platform { TIKTOK, YOUTUBE }
enum class DownloadMode { VIDEO, MP3 }

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var platform: Platform = Platform.TIKTOK
    private var format: DownloadMode = DownloadMode.VIDEO

    private var lastResult: TikTokResult? = null
    private var selectedVideoOption: VideoOption? = null
    private var awaitingResolutionChoice = false

    private var pendingDownloadId: Long = -1L
    private var downloadReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tabTikTok.setOnClickListener { switchPlatform(Platform.TIKTOK) }
        binding.tabYoutube.setOnClickListener { switchPlatform(Platform.YOUTUBE) }
        binding.chipVideo.setOnClickListener { switchFormat(DownloadMode.VIDEO) }
        binding.chipMp3.setOnClickListener { switchFormat(DownloadMode.MP3) }
        binding.btnDownload.setOnClickListener { onPrimaryButtonClicked() }

        applyPlatformTheme()
        applyFormatChips()
        registerDownloadReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadReceiver?.let {
            try { unregisterReceiver(it) } catch (e: IllegalArgumentException) { /* sudah lepas */ }
        }
    }

    // ---------- Tema per platform ----------

    private fun switchPlatform(newPlatform: Platform) {
        if (platform == newPlatform) return
        platform = newPlatform
        resetResultState()
        applyPlatformTheme()
        applyFormatChips()
    }

    private fun applyPlatformTheme() {
        val isTikTok = platform == Platform.TIKTOK

        binding.brandDot.setBackgroundResource(if (isTikTok) R.drawable.bg_dot_tiktok else R.drawable.bg_dot_youtube)

        binding.tvEyebrow.text = if (isTikTok) "TIKTOK DOWNLOADER" else "YOUTUBE DOWNLOADER"
        binding.tvEyebrow.setTextColor(getColor(if (isTikTok) R.color.tiktok_accent else R.color.youtube_accent))

        binding.tabTikTok.setBackgroundResource(if (isTikTok) R.drawable.bg_tab_active_tiktok else 0)
        binding.tabYoutube.setBackgroundResource(if (!isTikTok) R.drawable.bg_tab_active_youtube else 0)
        binding.tvTabTikTokLabel.setTextColor(getColor(if (isTikTok) R.color.text_primary else R.color.text_secondary))
        binding.tvTabYoutubeLabel.setTextColor(getColor(if (!isTikTok) R.color.text_primary else R.color.text_secondary))

        binding.etUrl.hint = if (isTikTok) "https://vt.tiktok.com/xxxxxxx/" else "https://youtu.be/xxxxxxx"

        binding.btnDownload.setBackgroundResource(if (isTikTok) R.drawable.bg_btn_outline_tiktok else R.drawable.bg_btn_outline_youtube)
        binding.btnSaveChip.setBackgroundResource(if (isTikTok) R.drawable.bg_save_chip_tiktok else R.drawable.bg_save_chip_youtube)
        binding.btnSaveChip.setTextColor(getColor(if (isTikTok) R.color.tiktok_accent else R.color.youtube_accent))
    }

    // ---------- Format Video / MP3 ----------

    private fun switchFormat(newFormat: DownloadMode) {
        if (format == newFormat) return
        format = newFormat
        resetResultState()
        applyFormatChips()
    }

    private fun applyFormatChips() {
        val isTikTok = platform == Platform.TIKTOK
        val activeChipBg = if (isTikTok) R.drawable.bg_chip_active_tiktok else R.drawable.bg_chip_active_youtube

        val videoActive = format == DownloadMode.VIDEO
        binding.chipVideo.setBackgroundResource(if (videoActive) activeChipBg else R.drawable.bg_chip_unselected)
        binding.tvChipVideoLabel.setTextColor(getColor(if (videoActive) R.color.text_primary else R.color.text_secondary))

        val mp3Active = format == DownloadMode.MP3
        binding.chipMp3.setBackgroundResource(if (mp3Active) activeChipBg else R.drawable.bg_chip_unselected)
        binding.tvChipMp3Label.setTextColor(getColor(if (mp3Active) R.color.text_primary else R.color.text_secondary))
    }

    // ---------- Reset ----------

    private fun resetResultState() {
        binding.previewCard.visibility = View.GONE
        binding.resolutionSection.visibility = View.GONE
        binding.resolutionChipContainer.removeAllViews()
        binding.tvStatus.text = ""
        binding.tvDownloadBtnLabel.text = "Proses Link"
        awaitingResolutionChoice = false
        lastResult = null
        selectedVideoOption = null
    }

    // ---------- Alur utama ----------

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
            binding.tvStatus.text = "Link kosong, isi dulu ya."
            binding.tvStatus.setTextColor(getColor(R.color.tiktok_accent))
            return
        }

        val linkMatchesPlatform = if (platform == Platform.TIKTOK) {
            TikTokApi.isTikTokLink(url)
        } else {
            YouTubeExtractor.isYouTubeLink(url)
        }
        if (!linkMatchesPlatform) {
            val platformName = if (platform == Platform.TIKTOK) "TikTok" else "YouTube"
            binding.tvStatus.text = "Bukan link $platformName yang valid."
            binding.tvStatus.setTextColor(getColor(R.color.tiktok_accent))
            return
        }

        binding.tvStatus.text = "Memproses link..."
        binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
        binding.btnDownload.isEnabled = false
        binding.previewCard.visibility = View.GONE
        binding.resolutionSection.visibility = View.GONE
        binding.resolutionChipContainer.removeAllViews()

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                when {
                    platform == Platform.YOUTUBE && format == DownloadMode.MP3 -> YouTubeExtractor.resolveAudio(url)
                    platform == Platform.YOUTUBE && format == DownloadMode.VIDEO -> YouTubeExtractor.resolveVideo(url)
                    else -> TikTokApi.resolve(url)
                }
            }
            binding.btnDownload.isEnabled = true

            if (!result.success) {
                binding.tvStatus.text = "Gagal: ${result.error ?: "coba lagi"}"
                binding.tvStatus.setTextColor(getColor(R.color.tiktok_accent))
                return@launch
            }

            showPreview(result)
            lastResult = result

            if (format == DownloadMode.VIDEO && result.videoOptions.size > 1) {
                setupResolutionChips(result.videoOptions)
                binding.tvDownloadBtnLabel.text = "Download ${result.videoOptions.first().label}"
                awaitingResolutionChoice = true
                binding.tvStatus.text = "Pilih resolusi, lalu tap tombol di atas."
                binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
            } else {
                val downloadUrl = if (format == DownloadMode.MP3) result.musicUrl else result.videoUrl
                if (downloadUrl == null) {
                    binding.tvStatus.text = "File tidak ditemukan pada link ini."
                    binding.tvStatus.setTextColor(getColor(R.color.tiktok_accent))
                    return@launch
                }
                startDownload(downloadUrl, result, resolutionLabel = null)
            }
        }
    }

    private fun setupResolutionChips(options: List<VideoOption>) {
        binding.resolutionSection.visibility = View.VISIBLE
        binding.resolutionChipContainer.removeAllViews()
        selectedVideoOption = options.first()

        val density = resources.displayMetrics.density
        val hPad = (14 * density).toInt()
        val vPad = (8 * density).toInt()
        val marginEndPx = (8 * density).toInt()
        val activeBg = if (platform == Platform.TIKTOK) R.drawable.bg_chip_active_tiktok else R.drawable.bg_chip_active_youtube

        options.forEach { option ->
            val chip = TextView(this).apply {
                text = option.label
                setPadding(hPad, vPad, hPad, vPad)
                textSize = 12f
                setTextColor(getColor(if (option == selectedVideoOption) R.color.text_primary else R.color.text_secondary))
                setBackgroundResource(if (option == selectedVideoOption) activeBg else R.drawable.bg_chip_unselected)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { this.marginEnd = marginEndPx }
                setOnClickListener {
                    selectedVideoOption = option
                    binding.tvDownloadBtnLabel.text = "Download ${option.label}"
                    refreshChipStyles(options, activeBg)
                }
            }
            binding.resolutionChipContainer.addView(chip)
        }
    }

    private fun refreshChipStyles(options: List<VideoOption>, activeBg: Int) {
        for (i in 0 until binding.resolutionChipContainer.childCount) {
            val chipView = binding.resolutionChipContainer.getChildAt(i) as? TextView ?: continue
            val option = options.getOrNull(i) ?: continue
            val isSelected = option == selectedVideoOption
            chipView.setTextColor(getColor(if (isSelected) R.color.text_primary else R.color.text_secondary))
            chipView.setBackgroundResource(if (isSelected) activeBg else R.drawable.bg_chip_unselected)
        }
    }

    private fun confirmVideoDownload() {
        val result = lastResult ?: return
        val option = selectedVideoOption ?: return
        startDownload(option.url, result, resolutionLabel = option.label)
    }

    private fun showPreview(result: TikTokResult) {
        binding.previewCard.visibility = View.VISIBLE
        binding.tvVidTitle.text = result.title
        binding.tvLikes.text = TikTokApi.formatCount(result.likeCount)
        binding.tvViews.text = TikTokApi.formatCount(result.playCount)
        if (!result.coverUrl.isNullOrBlank()) {
            binding.ivThumb.load(result.coverUrl)
        }
    }

    // ---------- Download & popup selesai ----------

    private fun startDownload(fileUrl: String, result: TikTokResult, resolutionLabel: String?) {
        val actionWord = if (format == DownloadMode.MP3) "audio" else "video"
        binding.tvStatus.text = "Mengunduh $actionWord..."
        binding.tvStatus.setTextColor(getColor(R.color.text_secondary))

        val safeTitle = (result.title ?: "media_$actionWord").replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
        val extension = if (format == DownloadMode.MP3) "mp3" else "mp4"
        val subFolder = if (format == DownloadMode.MP3) "MP3" else "Video"
        val resSuffix = if (resolutionLabel != null) "_$resolutionLabel" else ""
        val fileName = "${safeTitle}${resSuffix}_${System.currentTimeMillis()}.$extension"

        val request = DownloadManager.Request(Uri.parse(fileUrl))
            .setTitle(result.title ?: "media_$actionWord")
            .setDescription("MOKA.DL - media download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MOKA.DL/$subFolder/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        try {
            pendingDownloadId = manager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memulai download: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        binding.resolutionSection.visibility = View.GONE
        binding.tvDownloadBtnLabel.text = "Proses Link"
        awaitingResolutionChoice = false
    }

    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != -1L && finishedId == pendingDownloadId) {
                    showDownloadCompleteDialog()
                }
            }
        }
        downloadReceiver = receiver
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun showDownloadCompleteDialog() {
        val subFolder = if (format == DownloadMode.MP3) "MP3" else "Video"
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_download_complete, null)

        val isTikTok = platform == Platform.TIKTOK
        view.findViewById<View>(R.id.successCircle).setBackgroundResource(R.drawable.bg_success_circle)
        val messageView = view.findViewById<TextView>(R.id.tvDialogMessage)
        messageView.text = "File tersimpan di folder\nDownload/MOKA.DL/$subFolder"

        val okBtn = view.findViewById<TextView>(R.id.btnDialogOk)
        okBtn.setBackgroundResource(if (isTikTok) R.drawable.bg_btn_outline_tiktok else R.drawable.bg_btn_outline_youtube)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        okBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()

        binding.tvStatus.text = "Download selesai."
        binding.tvStatus.setTextColor(getColor(if (isTikTok) R.color.tiktok_accent else R.color.youtube_accent))
    }
}
