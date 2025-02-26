package com.example.closets.ui.current

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.databinding.CurrentItemLayoutBinding
import com.example.closets.ui.items.ClothingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrentItemAdapter(
    private var items: List<ClothingItem>,
    private val itemClickListener: (ClothingItem) -> Unit
) : RecyclerView.Adapter<CurrentItemAdapter.CurrentItemViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    // Map to store the original state of items when they are first loaded
    private val originalItemStates = mutableMapOf<Int, ItemState>()

    // Data class to store the original state of an item
    data class ItemState(
        val lastWornDate: String,
        val wornTimes: Int
    )

    inner class CurrentItemViewHolder(private val binding: CurrentItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClothingItem) {
            // Load image using setImageURI for the image URI
            if (!item.imageUri.isNullOrEmpty()) {
                binding.itemImage.setImageURI(Uri.parse(item.imageUri)) // Set the image URI directly
            } else {
                binding.itemImage.setImageResource(R.drawable.add_item_image) // Default image
            }

            // Set the icon based on the checked status
            updateCheckedIcon(item.isChecked)

            // Set click listener for the check icon
            binding.checkedIcon.setOnClickListener {
                toggleCheckedStatus(item)
            }

            // Set the click listener for the entire CardView (root view)
            binding.root.setOnClickListener {
                toggleCheckedStatus(item)
            }
        }

        private fun updateCheckedIcon(isChecked: Boolean) {
            binding.checkedIcon.setImageResource(
                if (isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked
            )
        }

        private fun toggleCheckedStatus(item: ClothingItem) {
            // Toggle the checked state
            item.isChecked = !item.isChecked
            updateCheckedIcon(item.isChecked)

            // Notify the listener of the item click
            itemClickListener(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrentItemViewHolder {
        val binding = CurrentItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CurrentItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrentItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    // Method to update the items in the adapter
    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<ClothingItem>) {
        items = newItems
        // Clear original states when updating items
        originalItemStates.clear()
        notifyDataSetChanged()
    }

    // Method to get selected items
    fun getSelectedItems(): List<ClothingItem> {
        return items.filter { it.isChecked }
    }

    // Add this method to your CurrentItemAdapter class
    fun getAllItems(): List<ClothingItem> {
        return items.toList()
    }

    fun getOriginalState(itemId: Int): ItemState? {
        return originalItemStates[itemId]
    }
}