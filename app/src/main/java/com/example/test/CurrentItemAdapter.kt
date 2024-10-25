package com.example.test.ui.current

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R
import com.example.test.databinding.CurrentItemLayoutBinding

data class CurrentItem(
    val imageResId: Int, // Resource ID for the item image
    val type: String, // String of the item
    var isChecked: Boolean // Indicates if the item is checked
)


class CurrentItemAdapter(
    private var items: List<CurrentItem>,
    private val itemClickListener: (CurrentItem) -> Unit
) : RecyclerView.Adapter<CurrentItemAdapter.CurrentItemViewHolder>() {

    inner class CurrentItemViewHolder(private val binding: CurrentItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CurrentItem) {
            binding.itemImage.setImageResource(item.imageResId)

            // Set the icon based on the checked status
            binding.checkedIcon.setImageResource(if (item.isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked)

            // Set click listener for the check icon
            binding.checkedIcon.setOnClickListener {
                // Toggle the checked status
                item.isChecked = !item.isChecked
                // Update the icon
                binding.checkedIcon.setImageResource(if (item.isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked)
                // Notify the listener of the item click
                itemClickListener(item)
            }

            // Set click listener for the item
            itemView.setOnClickListener {
                itemClickListener(item)
            }
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
    fun updateItems(newItems: List<CurrentItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}