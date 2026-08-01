package com.cyberdl.tiktok

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.cyberdl.tiktok.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMulai.setOnClickListener { revealOptions() }
        binding.optionTikTok.setOnClickListener { openPlatform(Platform.TIKTOK) }
        binding.optionYoutube.setOnClickListener { openPlatform(Platform.YOUTUBE) }
    }

    /** Sembunyikan tombol "Mulai" lalu tampilkan opsi platform dengan animasi reveal. */
    private fun revealOptions() {
        val start = binding.startSection
        val options = binding.optionsSection

        // Cegah dobel tap saat animasi jalan.
        start.isEnabled = false

        start.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                start.visibility = View.GONE

                binding.tvSubtitle.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        binding.tvSubtitle.text = getString(R.string.subtitle_pick_platform)
                        binding.tvSubtitle.animate().alpha(1f).setDuration(200).start()
                    }
                    .start()

                options.visibility = View.VISIBLE
                options.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(360)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()
            }
            .start()
    }

    private fun openPlatform(platform: Platform) {
        val intent = Intent(this, PlatformActivity::class.java)
        intent.putExtra(PlatformActivity.EXTRA_PLATFORM, platform.name)
        startActivity(intent)
    }
}

enum class Platform { TIKTOK, YOUTUBE }
enum class DownloadMode { VIDEO, MP3 }
