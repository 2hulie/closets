package com.example.closets.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.ui.items.ClothingItem

class TodayOutfitItemAdapter(
    private var clothingItems: List<ClothingItem>, // Change to ClothingItem
    private val clickListener: (ClothingItem) -> Unit // Change to ClothingItem
) : RecyclerView.Adapter<TodayOutfitItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)

        fun bind(clothingItem: ClothingItem) { // Change to ClothingItem
            itemImage.setImageURI(clothingItem.getImageUri()) // Assuming ClothingItem has a method to get the image URI
            itemView.setOnClickListener { clickListener(clothingItem) } // Change to ClothingItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.todays_outfit_item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(clothingItems[position]) // Change to clothingItems
    }

    override fun getItemCount(): Int {
        return clothingItems.size // Change to clothingItems
    }

    fun updateItems(newItems: List<ClothingItem>) {
        clothingItems = newItems
        notifyDataSetChanged() // Notify the adapter to refresh the view
    }
}