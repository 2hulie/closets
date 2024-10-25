package com.example.test.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.test.R


class AddItemFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_item, container, false)

        // Set up the back button click listener
        val backButton = view.findViewById<ImageView>(R.id.icon_back)
        backButton.setOnClickListener {
            findNavController().popBackStack() // Navigate back to the previous fragment
        }

        return view
    }
}
