package com.cyberdl.tiktok

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyberdl.tiktok.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val historyItems = mutableListOf<HistoryEntry>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = HistoryAdapter(historyItems)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        binding.btnDownload.setOnClickListener { startDownload() }
        binding.btnClear.setOnClickListener {
            binding.etUrl.setText("")
            setStatus("menunggu link...", R.color.cyber_text_dim)
        }
    }

    private fun setStatus(text: String, colorRes: Int) {
        binding.tvStatus.text = "STATUS: $text"
        binding.tvStatus.setTextColor(getColor(colorRes))
    }

    private fun startDownload() {
        val url = binding.etUrl.text.toString().trim()

        if (url.isEmpty()) {
            setStatus("link kosong, isi dulu bro.", R.color.cyber_magenta)
            return
        }
        if (!TikTokApi.isTikTokLink(url)) {
            setStatus("bukan link TikTok yang valid.", R.color.cyber_magenta)
            return
        }

        setStatus("mengambil data video...", R.color.cyber_cyan)
        binding.progressBar.progress = 20
        binding.btnDownload.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) { TikTokApi.resolve(url) }
            binding.btnDownload.isEnabled = true

            if (!result.success || result.videoUrl == null) {
                setStatus("gagal: ${result.error}", R.color.cyber_magenta)
                binding.progressBar.progress = 0
                return@launch
            }

            binding.progressBar.progress = 60
            setStatus("mengunduh video...", R.color.cyber_green)
            enqueueDownload(result.videoUrl, result.title ?: "tiktok_video")

            binding.progressBar.progress = 100
            setStatus("download dimulai! cek notifikasi.", R.color.cyber_green)

            val time = DateFormat.format("HH:mm:ss", Date()).toString()
            adapter.addEntry(HistoryEntry(result.title ?: "tiktok_video", time))
            binding.etUrl.setText("")
        }
    }

    private fun enqueueDownload(videoUrl: String, title: String) {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
        val fileName = "${safeTitle}_${System.currentTimeMillis()}.mp4"

        val request = DownloadManager.Request(Uri.parse(videoUrl))
            .setTitle(title)
            .setDescription("CyberDL - TikTok download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "CyberDL/$fileName"
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
