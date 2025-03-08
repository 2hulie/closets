package com.example.closets.ui.unused

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.closets.MainActivity
import com.example.closets.R
import com.example.closets.ui.items.ClothingItem

data class UnusedItem(
    val clothingItem: ClothingItem,
    val duration: String,
    var isUnused: Boolean = false
)

class UnusedItemsAdapter(
    private var items: List<UnusedItem>,
    private val itemClickListener: (ClothingItem) -> Unit
) : RecyclerView.Adapter<UnusedItemsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        val itemDuration: TextView = itemView.findViewById(R.id.item_duration)

        init {
            // set click listener for the entire item view to trigger the ripple effect
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemView.postDelayed({
                        itemClickListener(items[position].clothingItem) // trigger the click callback after delay
                    }, 150) // selay for 150 milliseconds
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.unused_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        val context = holder.itemView.context

        val imageUri = currentItem.clothingItem.getImageAsUri() // if type is Uri?
        if (imageUri != null) {
            try {
                Glide.with(context)
                    .load(imageUri)
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

        holder.itemDuration.text = currentItem.duration
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<UnusedItem>) {
        items = newItems
        notifyDataSetChanged()
    }

}