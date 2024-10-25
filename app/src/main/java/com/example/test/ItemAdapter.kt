package com.example.test.ui


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.test.R
import androidx.recyclerview.widget.RecyclerView
import com.example.test.ui.home.Item


class ItemAdapter(private val itemList: List<Item>, private val itemClickListener: (Item) -> Unit) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {


    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)


        init {
            itemView.setOnClickListener {
                itemClickListener(itemList[adapterPosition]) // Pass clicked item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_items_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.itemImage.setImageResource(itemList[position].imageResId)
    }

    override fun getItemCount(): Int = itemList.size
}