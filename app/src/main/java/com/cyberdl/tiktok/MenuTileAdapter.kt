package com.cyberdl.tiktok

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuTileAdapter(
    private val items: List<MenuTile>,
    private val onClick: (MenuTile) -> Unit
) : RecyclerView.Adapter<MenuTileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconFrame: FrameLayout = view.findViewById(R.id.tileIconFrame)
        val letter: TextView = view.findViewById(R.id.tvTileLetter)
        val icon: ImageView = view.findViewById(R.id.ivTileIcon)
        val title: TextView = view.findViewById(R.id.tvTileTitle)
        val subtitle: TextView = view.findViewById(R.id.tvTileSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tile = items[position]

        holder.iconFrame.setBackgroundResource(tile.backgroundRes)
        holder.itemView.setBackgroundResource(tile.outlineRes)
        holder.title.text = tile.title
        holder.subtitle.text = tile.subtitle

        if (tile.letter != null) {
            holder.letter.text = tile.letter
            holder.letter.visibility = View.VISIBLE
            holder.icon.visibility = View.GONE
        } else if (tile.iconRes != null) {
            holder.icon.setImageResource(tile.iconRes)
            holder.icon.visibility = View.VISIBLE
            holder.letter.visibility = View.GONE
        }

        holder.itemView.alpha = if (tile.enabled) 1f else 0.4f
        holder.itemView.setOnClickListener {
            if (tile.enabled) onClick(tile)
        }
    }

    override fun getItemCount(): Int = items.size
}
