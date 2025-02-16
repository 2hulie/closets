package com.example.closets.ui.favorites

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.items.ClothingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesAdapter(
    private var items: MutableList<ClothingItem>,
    private val itemClickListener: (ClothingItem) -> Unit,
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
                    Handler().postDelayed({
                        itemClickListener(items[position])
                    }, 150)
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

        fun bind(item: ClothingItem) {
            // Load image using setImageURI for the image URI
            if (!item.imageUri.isNullOrEmpty()) {
                itemImage.setImageURI(Uri.parse(item.imageUri)) // Set the image URI directly
            } else {
                itemImage.setImageResource(R.drawable.add_item_image) // Default image
            }


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
            removeItem(position, context)
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
    private fun removeItem(position: Int, context: Context) {
        val currentItem = items[position]
        val database = AppDatabase.getDatabase(context)
        val repository = ItemRepository(database.itemDao())

        // Run database operation in a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateItemFavoriteStatus(currentItem.id, false)
        }

        items.removeAt(position)
        notifyItemRemoved(position)
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
    fun updateItems(newItems: MutableList<ClothingItem>) {
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
