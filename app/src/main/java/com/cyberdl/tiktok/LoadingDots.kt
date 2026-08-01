package com.cyberdl.tiktok

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View

object LoadingDots {

    /** Mulai animasi pulsing bergantian pada tiap dot. Dot yang sudah hilang dari layar otomatis berhenti. */
    fun start(dots: List<View>) {
        dots.forEachIndexed { index, dot ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.35f, 1f)

            ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha).apply {
                duration = 420
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                startDelay = index * 140L
                start()
            }
        }
    }
}
