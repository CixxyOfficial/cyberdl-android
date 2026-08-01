package com.cyberdl.tiktok

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cyberdl.tiktok.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.optionTikTok.setOnClickListener { openPlatform(Platform.TIKTOK) }
        binding.optionYoutube.setOnClickListener { openPlatform(Platform.YOUTUBE) }
    }

    private fun openPlatform(platform: Platform) {
        val intent = Intent(this, PlatformActivity::class.java)
        intent.putExtra(PlatformActivity.EXTRA_PLATFORM, platform.name)
        startActivity(intent)
    }
}

enum class Platform { TIKTOK, YOUTUBE }
enum class DownloadMode { VIDEO, MP3 }
