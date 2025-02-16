package com.example.closets.ui.home


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.closets.R
import androidx.recyclerview.widget.RecyclerView

data class HomeItem(
    val name: String,
    val imageResId: Int,
)

class HomeItemAdapter(private val homeItemList: List<HomeItem>, private val itemClickListener: (HomeItem) -> Unit) :
    RecyclerView.Adapter<HomeItemAdapter.ItemViewHolder>() {


    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)


        init {
            itemView.setOnClickListener {
                itemClickListener(homeItemList[adapterPosition]) // Pass clicked item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_items_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.itemImage.setImageResource(homeItemList[position].imageResId)
    }

    override fun getItemCount(): Int = homeItemList.size
}