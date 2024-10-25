package com.example.test.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.test.R

class ItemInfoShoesFragment : Fragment() {
    // Track whether the item is favorited or not
    private var isFavorited = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_info_shoes, container, false)

        // Set up the back button click listener
        val backButton = view.findViewById<ImageView>(R.id.icon_back)
        backButton.setOnClickListener {
            findNavController().popBackStack() // Navigate back to the previous fragment
        }

        // Find the favorite icon ImageView
        val favoriteIcon = view.findViewById<ImageView>(R.id.icon_favorite)

        // Set up a click listener for the favorite icon
        favoriteIcon.setOnClickListener {
            isFavorited = !isFavorited // Toggle the favorited state

            // Change the icon based on whether it's favorited or not
            if (isFavorited) {
                favoriteIcon.setImageResource(R.drawable.icon_unfavorite) // Change to unfavorite icon
            } else {
                favoriteIcon.setImageResource(R.drawable.icon_favorite) // Change to favorite icon
            }
        }

        return view
    }
}
