package com.example.closets.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.closets.MainActivity
import com.example.closets.R
import com.example.closets.ui.items.ClothingItem

class TodayOutfitItemAdapter(
    private var clothingItems: List<ClothingItem>,
    // private val clickListener: (ClothingItem) -> Unit
) : RecyclerView.Adapter<TodayOutfitItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.todays_outfit_item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val clothingItem = clothingItems[position]
        val context = holder.itemView.context
        val uri = clothingItem.getImageAsUri()

        if (uri != null) {
            try {
                Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.closets_logo_transparent)
                    .error(R.drawable.closets_logo_transparent)
                    .into(holder.itemImage)
            } catch (e: SecurityException) {
                if (context is MainActivity) {
                    context.showPermissionDeniedDialog()
                }
                holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
            } catch (e: Exception) {
                holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
            }
        } else {
            holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
        }

        // holder.itemView.setOnClickListener { clickListener(clothingItem) }
    }

    override fun getItemCount(): Int {
        return clothingItems.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<ClothingItem>) {
        clothingItems = newItems
        notifyDataSetChanged()
    }
}