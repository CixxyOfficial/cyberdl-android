package com.cyberdl.tiktok

data class HistoryEntry(
    val title: String,
    val time: String,
    val coverUrl: String? = null,
    val likeCount: Long = 0
)
