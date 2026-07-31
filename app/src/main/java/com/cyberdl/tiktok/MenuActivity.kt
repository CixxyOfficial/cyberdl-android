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

        binding.optionVideo.setOnClickListener { openDownloader(DownloadMode.VIDEO) }
        binding.optionMp3.setOnClickListener { openDownloader(DownloadMode.MP3) }
    }

    private fun openDownloader(mode: DownloadMode) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_MODE, mode.name)
        startActivity(intent)
    }
}

enum class DownloadMode { VIDEO, MP3 }
