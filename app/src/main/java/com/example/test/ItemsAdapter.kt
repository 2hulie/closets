package com.example.test.ui.items

import android.annotation.SuppressLint
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R

data class clothingItem(
    val imageResId: Int,
    val type: String,
    var isFavorite: Boolean = false,// Property to track if the item is favorite
    val name: String // Add a name
)


class ItemsAdapter(
    private var items: List<clothingItem>,
    private val onAddItemClicked: () -> Unit,
    private val onItemClick: (clothingItem) -> Unit // Click handler for items
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ADD = 0
    private val VIEW_TYPE_ITEM = 1

    inner class AddItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addItemCard: CardView = itemView.findViewById(R.id.add_item_card)

        init {
            addItemCard.setOnClickListener {
                // Use a Handler to add a delay before executing the action
                Handler().postDelayed({
                    onAddItemClicked() // Handle add item click after the delay
                }, 150) // 150 ms
            }
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)

        init {
            // Set click listener for the entire item view (this allows ripple effect)
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position - 1]) // Pass the clicked item to the lambda
                }
            }

            favoriteIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Toggle the favorite status
                    items[position - 1].isFavorite = !items[position - 1].isFavorite // Adjust index for the item

                    // Update the icon based on the new favorite status
                    updateFavoriteIcon(favoriteIcon, items[position - 1].isFavorite)
                }
            }
        }

        fun bind(item: clothingItem) {
            itemImage.setImageResource(item.imageResId)
            updateFavoriteIcon(favoriteIcon, item.isFavorite)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ADD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.add_item_layout, parent, false)
            AddItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.items_layout, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val currentItem = items[position - 1] // Shift position for items
            holder.bind(currentItem) // Use the bind function to set the data
        }
    }

    override fun getItemCount(): Int {
        return items.size + 1 // Include the "Add Item" button
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<clothingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun updateFavoriteIcon(favoriteIcon: ImageView, isFavorite: Boolean) {
        favoriteIcon.setImageResource(if (isFavorite) R.drawable.icon_favorite else R.drawable.icon_unfavorite)
    }
}