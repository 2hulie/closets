package com.example.closets.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R


class TodayOutfitItemAdapter(private val homeItems: List<HomeItem>, private val clickListener: (HomeItem) -> Unit) :
    RecyclerView.Adapter<TodayOutfitItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)

        fun bind(homeItem: HomeItem) {
            itemImage.setImageResource(homeItem.imageResId) // Item class has an imageResId field
            itemView.setOnClickListener { clickListener(homeItem) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.todays_outfit_item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(homeItems[position])
    }

    override fun getItemCount(): Int {
        return homeItems.size
    }
}