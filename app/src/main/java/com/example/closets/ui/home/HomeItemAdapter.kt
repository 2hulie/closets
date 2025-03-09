package com.example.closets.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
                Glide.with(context)
                    .load(homeItem.imageUri)
                    .placeholder(R.drawable.closets_logo_transparent)
                    .error(R.drawable.closets_logo_transparent)
                    .into(holder.itemImage)
            } catch (e: SecurityException) {
                if (context is MainActivity) {
                    context.showPermissionDeniedDialog()
                }
                holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
            } catch (e: Exception) {
                holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
            }
        } else {
            holder.itemImage.setImageResource(R.drawable.closets_logo_transparent)
        }
    }

    override fun getItemCount(): Int = homeItemList.size
}