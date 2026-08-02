package com.cyberdl.tiktok

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cyberdl.tiktok.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener { onStartClicked() }
    }

    private fun onStartClicked() {
        binding.btnStart.visibility = View.GONE
        binding.loadingContainer.visibility = View.VISIBLE
        LoadingDots.start(listOf(binding.dot1, binding.dot2, binding.dot3))

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MenuActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 900)
    }
}
