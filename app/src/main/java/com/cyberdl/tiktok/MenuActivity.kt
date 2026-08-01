package com.cyberdl.tiktok

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.cyberdl.tiktok.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    // Daftar tile menu - tinggal tambah entry baru di sini kalau mau nambah fitur ke depannya
    private val tiles = listOf(
        MenuTile(
            id = "tiktok",
            title = "TikTok",
            subtitle = "Video & audio",
            letter = "T",
            backgroundRes = R.drawable.bg_badge_tiktok
        ),
        MenuTile(
            id = "youtube",
            title = "YouTube",
            subtitle = "Video & audio",
            letter = "Y",
            backgroundRes = R.drawable.bg_badge_youtube,
            outlineRes = R.drawable.bg_menu_option_teal
        ),
        MenuTile(
            id = "coming_soon",
            title = "Segera Hadir",
            subtitle = "Fitur baru nyusul",
            iconRes = R.drawable.ic_plus,
            backgroundRes = R.drawable.bg_icon_badge_muted,
            enabled = false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val spanCount = 2
        binding.rvMenuGrid.layoutManager = GridLayoutManager(this, spanCount)
        val spacingPx = (12 * resources.displayMetrics.density).toInt()
        binding.rvMenuGrid.addItemDecoration(GridSpacingDecoration(spanCount, spacingPx))
        binding.rvMenuGrid.adapter = MenuTileAdapter(tiles) { tile -> onTileClicked(tile) }

        LoadingDots.start(listOf(binding.dot1, binding.dot2, binding.dot3))
        Handler(Looper.getMainLooper()).postDelayed({
            binding.loadingContainer.visibility = View.GONE
            binding.rvMenuGrid.animate().alpha(1f).setDuration(350).start()
        }, 700)
    }

    private fun onTileClicked(tile: MenuTile) {
        val platform = when (tile.id) {
            "tiktok" -> Platform.TIKTOK
            "youtube" -> Platform.YOUTUBE
            else -> return
        }
        val intent = Intent(this, PlatformActivity::class.java)
        intent.putExtra(PlatformActivity.EXTRA_PLATFORM, platform.name)
        startActivity(intent)
    }
}

enum class Platform { TIKTOK, YOUTUBE }
enum class DownloadMode { VIDEO, MP3 }
