package com.example.closets.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.closets.R

class ItemInfoCapFragment : Fragment() {

    // Track whether the item is favorited or not
    private var isFavorited = false

    // Singleton Toast instance
    private var currentToast: Toast? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_info_cap, container, false)

        setStatusBarColor()

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
                showToast("Removed from Favorites!") // Show toast for removing from favorites
            } else {
                favoriteIcon.setImageResource(R.drawable.icon_favorite) // Change to favorite icon
                showToast("Added to Favorites!") // Show toast for adding to favorites
            }
        }

        // Navigate to the EditItemInfoFragment when "Edit Item Info" button is clicked
        val editItemButton = view.findViewById<ImageView>(R.id.icon_edit_item)
        editItemButton.setOnClickListener {
            findNavController().navigate(R.id.action_itemInfoCapFragment_to_editItemInfoFragment)
        }

        // Set up the delete icon click listener
        val deleteIcon = view.findViewById<ImageView>(R.id.icon_delete_item)
        deleteIcon.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle the system back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            handleBackPress()
        }
    }

    fun handleBackPress() {
        findNavController().popBackStack() // Navigate back to the previous fragment
    }

    fun showToast(message: String) {
        currentToast?.cancel() // Cancel the existing toast, if any
        currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).apply {
            show() // Show the new toast immediately
        }
    }

    fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_confirmation, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val deleteButton = dialogView.findViewById<ImageView>(R.id.btn_delete)
        val cancelButton = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        // Remove the default background to avoid unwanted outlines
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        deleteButton.setOnClickListener {
            deleteItem() // Perform delete action
            dialog.dismiss() // Close the dialog
        }

        cancelButton.setOnClickListener {
            dialog.dismiss() // Close the dialog without taking action
        }

        dialog.show()
    }

    fun deleteItem() {
        // Show "Item deleted" toast
        showToast("Item deleted!") // Use showToast to cancel any previous toast and show the new one

        // Perform the deletion logic here
        findNavController().popBackStack() // Navigate back after deleting the item
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.status_item)
    }
}