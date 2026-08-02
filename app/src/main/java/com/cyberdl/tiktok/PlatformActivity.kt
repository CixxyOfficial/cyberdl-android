package com.cyberdl.tiktok

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cyberdl.tiktok.databinding.ActivityPlatformBinding

class PlatformActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLATFORM = "extra_platform"
    }

    private lateinit var binding: ActivityPlatformBinding
    private var platform: Platform = Platform.TIKTOK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlatformBinding.inflate(layoutInflater)
        setContentView(binding.root)

        platform = try {
            Platform.valueOf(intent.getStringExtra(EXTRA_PLATFORM) ?: Platform.TIKTOK.name)
        } catch (e: IllegalArgumentException) {
            Platform.TIKTOK
        }

        binding.tvPlatformTitle.text = if (platform == Platform.TIKTOK) "TikTok" else "YouTube"
        binding.tvVideoSub.text = if (platform == Platform.TIKTOK) {
            "Tanpa watermark, langsung simpan"
        } else {
            "Pilih resolusi sebelum download"
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.optionVideo.setOnClickListener { openDownloader(DownloadMode.VIDEO) }
        binding.optionMp3.setOnClickListener { openDownloader(DownloadMode.MP3) }
    }

    private fun openDownloader(mode: DownloadMode) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_MODE, mode.name)
        intent.putExtra(MainActivity.EXTRA_PLATFORM, platform.name)
        startActivity(intent)
    }
}
