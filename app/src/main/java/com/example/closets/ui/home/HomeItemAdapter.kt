package com.example.closets.ui.home


import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.MainActivity
import com.example.closets.R

data class HomeItem(
    val id: Int,
    val name: String,
    val imageUri: String?
)

class HomeItemAdapter(
    private val homeItemList: List<HomeItem>,
    private val itemClickListener: (HomeItem) -> Unit) :
    RecyclerView.Adapter<HomeItemAdapter.ItemViewHolder>() {


    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)

        init {
            itemView.setOnClickListener {
                itemView.postDelayed({
                    itemClickListener(homeItemList[adapterPosition])
                }, 150)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_items_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val homeItem = homeItemList[position]
        val context = holder.itemView.context

        if (!homeItem.imageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(homeItem.imageUri)

                // Try to open the image to check if it's accessible
                context.contentResolver.openInputStream(uri)?.use {
                    // If successful, set the image
                    holder.itemImage.setImageURI(uri)
                } ?: run {
                    // If the image is not accessible, set a placeholder
                    holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
                }
            } catch (e: SecurityException) {
                // Handle limited access scenario
                if (context is MainActivity) {
                    context.showPermissionDeniedDialog() // Show dialog to ask for full access
                }
                holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
            } catch (e: Exception) {
                // Handle other errors (e.g., file not found)
                holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
            }
        } else {
            holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
        }
    }

    override fun getItemCount(): Int = homeItemList.size
}