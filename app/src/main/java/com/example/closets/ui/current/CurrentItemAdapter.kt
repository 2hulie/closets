package com.example.closets.ui.current

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.databinding.CurrentItemLayoutBinding
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel


class CurrentItemAdapter(
    private var items: List<ClothingItem>,
    private val itemClickListener: (ClothingItem) -> Unit,
) : RecyclerView.Adapter<CurrentItemAdapter.CurrentItemViewHolder>() {

    inner class CurrentItemViewHolder(private val binding: CurrentItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClothingItem) {
            // Load image using setImageURI for the image URI
            if (!item.imageUri.isNullOrEmpty()) {
                binding.itemImage.setImageURI(Uri.parse(item.imageUri)) // Set the image URI directly
            } else {
                binding.itemImage.setImageResource(R.drawable.add_item_image) // Default image
            }

            // Set the icon based on the checked status
            binding.checkedIcon.setImageResource(
                if (item.isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked
            )

            // Set click listener for the check icon
            binding.checkedIcon.setOnClickListener {
                toggleCheckedStatus(item)
            }

            // Set the click listener for the entire CardView (root view)
            binding.root.setOnClickListener {
                toggleCheckedStatus(item)
            }
        }

        private fun toggleCheckedStatus(item: ClothingItem) {
            // Toggle the checked status
            item.isChecked = !item.isChecked
            // Update the icon based on the new checked status
            binding.checkedIcon.setImageResource(
                if (item.isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked
            )
            // Notify the listener of the item click (optional, if needed)
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
        notifyDataSetChanged()
    }
}