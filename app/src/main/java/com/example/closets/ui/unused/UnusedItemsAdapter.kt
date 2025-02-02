package com.example.closets.ui.unused

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R

data class UnusedItem(
    val imageResId: Int,
    val name: String,
    var isFavorite: Boolean = false,
    val duration: String
)

class UnusedItemsAdapter(
    var items: MutableList<UnusedItem>,
    private val onItemClick: (UnusedItem) -> Unit
) : RecyclerView.Adapter<UnusedItemsAdapter.ViewHolder>() {

    // Singleton Toast instance
    companion object {
        private var currentToast: Toast? = null

        private fun showToast(context: View, message: String) {
            currentToast?.cancel() // Cancel the existing toast if any
            currentToast = Toast.makeText(context.context, message, Toast.LENGTH_SHORT).apply {
                show() // Show the new toast
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        val itemDuration: TextView = itemView.findViewById(R.id.item_duration)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)

        init {
            // Set click listener for the entire item view to trigger the ripple effect
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position]) // Trigger the click callback
                }
            }

            // Handle favorite icon click
            favoriteIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Toggle favorite status
                    val currentItem = items[position]
                    currentItem.isFavorite = !currentItem.isFavorite

                    // Update the favorite icon
                    updateFavoriteIcon(favoriteIcon, currentItem.isFavorite)

                    // Show Toast based on the favorite status using the singleton method
                    val message = if (currentItem.isFavorite) {
                        "Added to Favorites!"
                    } else {
                        "Removed from Favorites!"
                    }
                    showToast(favoriteIcon, message)
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
        holder.itemImage.setImageResource(currentItem.imageResId)
        holder.itemDuration.text = currentItem.duration
        updateFavoriteIcon(holder.favoriteIcon, currentItem.isFavorite)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<UnusedItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun updateFavoriteIcon(favoriteIcon: ImageView, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.icon_favorite)
        } else {
            favoriteIcon.setImageResource(R.drawable.icon_unfavorite)
        }
    }
}