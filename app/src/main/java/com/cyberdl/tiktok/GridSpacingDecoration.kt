package com.cyberdl.tiktok

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Item decoration sederhana buat kasih jarak rata di grid RecyclerView, tanpa dependency tambahan. */
class GridSpacingDecoration(
    private val spanCount: Int,
    private val spacingPx: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        outRect.left = spacingPx - column * spacingPx / spanCount
        outRect.right = (column + 1) * spacingPx / spanCount
        if (position < spanCount) {
            outRect.top = spacingPx
        }
        outRect.bottom = spacingPx
    }
}
