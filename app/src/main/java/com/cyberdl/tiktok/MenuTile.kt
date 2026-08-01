package com.cyberdl.tiktok

data class MenuTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val letter: String? = null,
    val iconRes: Int? = null,
    val backgroundRes: Int,
    val outlineRes: Int = R.drawable.bg_menu_option,
    val enabled: Boolean = true
)
