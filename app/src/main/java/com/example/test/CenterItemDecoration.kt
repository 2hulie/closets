package com.example.test

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CenterItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)

        // Set the left and right offsets to create spacing
        if (position == 0) {
            // Only add spacing on the right for the first item
            outRect.right = spacing
        } else if (position == state.itemCount - 1) {
            // Only add spacing on the left for the last item
            outRect.left = spacing
        } else {
            // Add spacing on both sides for other items
            outRect.left = spacing / 2
            outRect.right = spacing / 2
        }
    }
}
