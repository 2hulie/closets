package com.example.closets.ui.favorites

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.MainActivity
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
    private val onItemRemoved: () -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val removeIcon: ImageView = itemView.findViewById(R.id.remove_icon)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    Handler().postDelayed({
                        itemClickListener(items[position])
                    }, 150)
                }
            }

            removeIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showConfirmationDialog(itemView.context, position)
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: ClothingItem) {
            val context = itemView.context
            if (!item.imageUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(item.imageUri)
                    context.contentResolver.openInputStream(uri)?.use {
                        itemImage.setImageURI(uri)
                    } ?: run {
                        itemImage.setImageResource(R.drawable.closets_logo_transparent)
                    }
                } catch (e: SecurityException) {
                    if (context is MainActivity) {
                        context.showPermissionDeniedDialog()
                    }
                    itemImage.setImageResource(R.drawable.closets_logo_transparent)
                } catch (e: Exception) {
                    itemImage.setImageResource(R.drawable.closets_logo_transparent)
                }
            } else {
                itemImage.setImageResource(R.drawable.closets_logo_transparent)
            }
        }
    }

    private fun showConfirmationDialog(context: Context, position: Int) {
        val dialogBuilder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_remove_from_favorites, null)

        dialogBuilder.setView(dialogView)

        val dialog = dialogBuilder.create()

        val removeButton: ImageView = dialogView.findViewById(R.id.btn_remove)
        val cancelButton: ImageView = dialogView.findViewById(R.id.btn_cancel)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        removeButton.setOnClickListener {
            removeItem(position, context)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

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
        showToast(context, "Removed from Favorites")
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

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: MutableList<ClothingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        private var currentToast: Toast? = null

        private fun showToast(context: Context, message: String) {
            currentToast?.cancel()
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show()
            }
        }
    }
}
