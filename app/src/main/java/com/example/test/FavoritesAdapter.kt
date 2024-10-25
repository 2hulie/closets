package com.example.test.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R

data class FavoriteItem(
    val imageResId: Int,
    val type: String,
    var isFavorite: Boolean = true,
    val name: String
)

class FavoritesAdapter(
    private var items: List<FavoriteItem>,
    private val onItemClick: (FavoriteItem) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)

        init {
            // Item image click listener
            itemView.setOnClickListener { // Use the entire item view for clicks
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }

            // Favorite icon click listener
            favoriteIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Toggle favorite status
                    items[position].isFavorite = !items[position].isFavorite
                    updateFavoriteIcon(favoriteIcon, items[position].isFavorite)
                }
            }
        }

        fun bind(item: FavoriteItem) {
            itemImage.setImageResource(item.imageResId)
            updateFavoriteIcon(favoriteIcon, item.isFavorite)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_layout, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val currentItem = items[position]
        holder.bind(currentItem)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<FavoriteItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun updateFavoriteIcon(favoriteIcon: ImageView, isFavorite: Boolean) {
        favoriteIcon.setImageResource(if (isFavorite) R.drawable.icon_favorite else R.drawable.icon_unfavorite)
    }
}