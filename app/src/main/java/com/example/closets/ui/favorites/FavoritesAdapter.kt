package com.example.closets.ui.favorites

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R

data class FavoriteItem(
    val imageResId: Int,
    val type: String,
    val color: String,
    var isFavorite: Boolean = true,
    val name: String,
)

class FavoritesAdapter(
    private var items: MutableList<FavoriteItem>,
    private val onItemClick: (FavoriteItem) -> Unit,
    private val onItemRemoved: () -> Unit // Callback when item is removed
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val removeIcon: ImageView = itemView.findViewById(R.id.remove_icon)

        init {
            // Handle item click (optional action when clicked)
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }

            // Handle remove icon click with confirmation dialog
            removeIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Show the confirmation dialog
                    showConfirmationDialog(itemView.context, position)
                }
            }
        }

        fun bind(item: FavoriteItem) {
            itemImage.setImageResource(item.imageResId)
        }
    }

    // Show a confirmation dialog before removing the item
    private fun showConfirmationDialog(context: Context, position: Int) {
        // Create the AlertDialog builder
        val dialogBuilder = AlertDialog.Builder(context)

        // Inflate your custom dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_remove_from_favorites, null)

        // Set the custom layout as the dialog content
        dialogBuilder.setView(dialogView)

        // Create the dialog
        val dialog = dialogBuilder.create()

        // Find the buttons in the custom dialog layout
        val removeButton: ImageView = dialogView.findViewById(R.id.btn_remove)
        val cancelButton: ImageView = dialogView.findViewById(R.id.btn_cancel)

        // Remove the default background to avoid unwanted outlines
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Handle Remove button click
        removeButton.setOnClickListener {
            // If Remove is clicked, remove the item
            removeItem(position)
            dialog.dismiss() // Dismiss the dialog
        }

        // Handle Cancel button click
        cancelButton.setOnClickListener {
            // If Cancel is clicked, just dismiss the dialog
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    // Remove the item from the list and notify the adapter
    private fun removeItem(position: Int) {
        items.removeAt(position) // Remove the item
        notifyItemRemoved(position) // Notify the adapter about the removal
        // Trigger the callback to update the count in the fragment
        onItemRemoved()
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

    // Updates the list of items and refreshes the RecyclerView
    fun updateItems(newItems: MutableList<FavoriteItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // Singleton Quick Toast implementation (same as previous)
    companion object {
        private var currentToast: Toast? = null

        private fun showQuickToast(context: android.content.Context, message: String) {
            currentToast?.cancel() // Cancel the existing toast, if any
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show() // Show the new toast immediately
            }
        }
    }
}
