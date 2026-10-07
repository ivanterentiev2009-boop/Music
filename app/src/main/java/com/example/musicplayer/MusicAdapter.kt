package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Song(val title: String, val artist: String, val uri: String)

class MusicAdapter(
    private val items: List<Song>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val artist: TextView = v.findViewById(R.id.artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.title.text = s.title
        holder.artist.text = s.artist
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount() = items.size
}