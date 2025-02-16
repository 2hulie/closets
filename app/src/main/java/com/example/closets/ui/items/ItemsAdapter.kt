package com.example.closets.ui.items

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.ui.entities.Item
import com.example.closets.ui.viewmodels.ItemViewModel
import java.io.Serializable

data class ClothingItem(
    val id: Int,
    val imageUri: String?,
    val type: String,
    val color: String,
    var isFavorite: Boolean,
    val name: String,
    var wornTimes: Int = 0,
    var lastWornDate: String? = null,
    var favoriteIconVisibility: Int = View.VISIBLE,
    var checkedIconVisibility: Int = View.GONE,
    var checkedIconResId: Int = R.drawable.icon_unchecked,
    var isChecked: Boolean = false,
    val fragmentId: Int
) : Serializable {
    fun getImageUri(): Uri? {
        return imageUri?.let { Uri.parse(it) }
    }

}

class ItemsAdapter(
    private var _items: MutableList<ClothingItem> = mutableListOf(),
    val itemClickListener: (ClothingItem) -> Unit,
    private val selectionCallback: SelectionCallback? = null,
    private val itemViewModel: ItemViewModel
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    // Backing property to ensure thread safety
    var items: List<ClothingItem>
        get() = _items
        private set(value) {
            _items.clear()
            _items.addAll(value)
        }

    private var isSelectAllActive = false
    private val selectedItems = HashSet<ClothingItem>()

    companion object {
        private var currentToast: Toast? = null

        private fun showToast(context: View, message: String) {
            currentToast?.cancel()
            currentToast = Toast.makeText(context.context, message, Toast.LENGTH_SHORT).apply {
                show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val favoriteIconFrame: FrameLayout = itemView.findViewById(R.id.favorite_icon_frame)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)
        private val checkedIcon: ImageView = itemView.findViewById(R.id.checked_icon)

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(item: ClothingItem) {
            // Load image using setImageURI for the image URI
            if (!item.imageUri.isNullOrEmpty()) {
                itemImage.setImageURI(Uri.parse(item.imageUri)) // Set the image URI directly
            } else {
                itemImage.setImageResource(R.drawable.add_item_image) // Default image
            }

            favoriteIconFrame.visibility = if (item.checkedIconVisibility == View.VISIBLE) View.GONE else item.favoriteIconVisibility
            favoriteIcon.visibility = item.favoriteIconVisibility
            checkedIcon.visibility = item.checkedIconVisibility

            checkedIcon.setImageResource(item.checkedIconResId)

            checkedIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    toggleCheckedState(item, position)
                    itemView.isSelected = item.isChecked

                    if (item.isChecked) {
                        selectedItems.add(item)
                    } else {
                        selectedItems.remove(item)
                    }

                    selectionCallback?.onItemSelectionChanged()
                }
            }

            updateFavoriteIcon(favoriteIcon, item.isFavorite)

            val cardView = itemView.findViewById<CardView>(R.id.card_view)
            if (checkedIcon.visibility == View.VISIBLE) {
                cardView.foreground = null
            } else {
                cardView.foreground = cardView.context.getDrawable(R.drawable.ripple_effect)
            }
        }

        private fun toggleCheckedState(item: ClothingItem, position: Int) {
            item.isChecked = !item.isChecked

            if (item.isChecked) {
                item.checkedIconResId = R.drawable.icon_checked
                selectedItems.add(item)
            } else {
                item.checkedIconResId = R.drawable.icon_unchecked
                selectedItems.remove(item)
            }

            notifyItemChanged(position, "check_state")
            selectionCallback?.onItemSelectionChanged()
        }

        private fun updateFavoriteIcon(favoriteIcon: ImageView, isFavorite: Boolean) {
            favoriteIcon.setImageResource(
                if (isFavorite) R.drawable.icon_favorite else R.drawable.icon_unfavorite
            )
        }

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    if (checkedIcon.visibility == View.VISIBLE) {
                        toggleCheckedState(item, position)
                    } else {
                        Handler().postDelayed({
                            itemClickListener(item) // Trigger the item click listener after the delay
                        }, 150)
                    }
                }
            }

            favoriteIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && !isSelectAllActive) {
                    val currentItem = items[position]
                    currentItem.isFavorite = !currentItem.isFavorite // Toggle the favorite state
                    updateFavoriteIcon(favoriteIcon, currentItem.isFavorite) // Update the icon

                    val message = if (currentItem.isFavorite) {
                        "Added to Favorites!"
                    } else {
                        "Removed from Favorites!"
                    }
                    showToast(favoriteIcon, message) // Show toast message

                    // Update the item in the database
                    val itemToUpdate = convertToItem(currentItem) // Convert currentItem to Item
                    itemViewModel.update(itemToUpdate) // Call the ViewModel to update the item
                }
            }
        }
    }

    private fun convertToItem(clothingItem: ClothingItem): Item {
        return Item(
            id = clothingItem.id,
            name = clothingItem.name,
            type = clothingItem.type,
            color = clothingItem.color,
            wornTimes = clothingItem.wornTimes,
            lastWornDate = clothingItem.lastWornDate ?: "", // Handle null case if necessary
            imageUri = clothingItem.imageUri,
            isFavorite = clothingItem.isFavorite // Ensure this matches your Item entity
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedItems(newSelectedItems: Set<ClothingItem>) {
        selectedItems.clear()
        selectedItems.addAll(newSelectedItems)
        notifyDataSetChanged()
    }

    fun getSelectedItems(): Set<ClothingItem> {
        return items.filter { it.isChecked }.toSet()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllItems() {
        items.forEach { item ->
            item.isChecked = true
            item.checkedIconResId = R.drawable.icon_checked
        }

        selectedItems.clear()
        selectedItems.addAll(items)

        notifyDataSetChanged()
        selectionCallback?.onItemSelectionChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelections() {
        items.forEach { item ->
            item.isChecked = false
            item.checkedIconResId = R.drawable.icon_unchecked
        }
        selectedItems.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun initializeSelectMode() {
        items.forEach { item ->
            item.checkedIconVisibility = View.VISIBLE
            item.favoriteIconVisibility = View.GONE
            item.isChecked = false
            item.checkedIconResId = R.drawable.icon_unchecked
        }
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun hasSelectedItems(): Boolean {
        val checkedCount = items.count { it.isChecked }
        println("Debug: Checked items count: $checkedCount")
        return checkedCount > 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        println("Debug: Creating ViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.items_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        println("Debug: Getting item count: ${items.size}")
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<ClothingItem>) {
        _items.clear()
        _items.addAll(newItems)
        notifyDataSetChanged()
    }

    interface SelectionCallback {
        fun onItemSelectionChanged()
    }
}