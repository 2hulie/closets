package com.example.closets.ui.home


import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.closets.R
import androidx.recyclerview.widget.RecyclerView

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
        if (!homeItem.imageUri.isNullOrEmpty()) {
            holder.itemImage.setImageURI(Uri.parse(homeItem.imageUri))
        } else {
            // Use the same fallback image as in your ItemsAdapter
            holder.itemImage.setImageResource(R.drawable.add_item_image)
        }
    }

    override fun getItemCount(): Int = homeItemList.size
}