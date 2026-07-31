package com.cyberdl.tiktok

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class HistoryAdapter(private val items: MutableList<HistoryEntry>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.ivItemThumb)
        val title: TextView = view.findViewById(R.id.tvItemTitle)
        val likes: TextView = view.findViewById(R.id.tvItemLikes)
        val time: TextView = view.findViewById(R.id.tvItemTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.likes.text = TikTokApi.formatCount(item.likeCount)
        holder.time.text = item.time
        if (!item.coverUrl.isNullOrBlank()) {
            holder.thumb.load(item.coverUrl)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addEntry(entry: HistoryEntry) {
        items.add(0, entry)
        notifyItemInserted(0)
    }
}
