package com.example.closets.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.closets.R
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory

class ItemInfoFragment : Fragment() {

    private var isFavorite = false
    private lateinit var itemViewModel: ItemViewModel
    private var itemId: Int? = null // To hold the passed item ID
    private var item: ClothingItem? = null // To hold the item data

    private var currentToast: Toast? = null
    private lateinit var loadingView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_item_info, container, false)

        // Initialize the ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository)).get(ItemViewModel::class.java)

        setStatusBarColor()

        // Set up the back button click listener
        val backButton = view.findViewById<ImageView>(R.id.icon_back)
        backButton.setOnClickListener {
            findNavController().popBackStack() // Navigate back to the previous fragment
        }

        // Set up the edit button click listener
        val editItemButton = view.findViewById<ImageView>(R.id.icon_edit_item)
        editItemButton.setOnClickListener {
            item?.let {
                // Create a Bundle to pass the item ID
                val bundle = Bundle().apply {
                    putInt("item_id", it.id) // Pass the item ID to the edit fragment
                }
                // Navigate to EditItemInfoFragment with the Bundle
                findNavController().navigate(R.id.action_itemInfoFragment_to_editItemInfoFragment, bundle)
            }
        }

        // Set up the delete button click listener
        val deleteButton = view.findViewById<ImageView>(R.id.icon_delete_item)
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog() // Show the delete confirmation dialog
        }

        // Inflate the loading view
        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (view as ViewGroup).addView(loadingView) // Add loading view to the fragment's view

        // Get the item ID from the arguments
        itemId = arguments?.getInt("item_id")

        // Load the item data
        itemId?.let { id ->
            itemViewModel.getItem(id).observe(viewLifecycleOwner) { item: Item? ->
                item?.let {
                    this.item = convertToClothingItem(it) // Convert Item to ClothingItem
                    // Delay the transition to item info
                    view.postDelayed({
                        updateItemInfo(this.item!!)
                        loadingView.visibility = View.GONE // Hide loading view
                    }, 150)
                }
            }
        }

        // Find the favorite icon ImageView
        val favoriteIcon = view.findViewById<ImageView>(R.id.icon_favorite)

        // Set up a click listener for the favorite icon
        favoriteIcon.setOnClickListener {
            // Toggle the favorite state
            isFavorite = !isFavorite // Toggle the favorited state

            // Change the icon based on whether it's favorited or not
            if (isFavorite) {
                favoriteIcon.setImageResource(R.drawable.icon_favorite) // Change to favorite icon
                showToast("Added to Favorites!") // Show toast for adding to favorites
            } else {
                favoriteIcon.setImageResource(R.drawable.icon_unfavorite) // Change to unfavorite icon
                showToast("Removed from Favorites!"); // Show toast for removing from favorites
            }

            // Update the item in the database
            item?.let {
                val itemToUpdate = convertToItem(it)
                itemToUpdate.isFavorite = isFavorite // Update the favorite state in the itemToUpdate
                itemViewModel.update(itemToUpdate) // Call the ViewModel to update the item
            }
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

    private fun handleBackPress() {
        findNavController().popBackStack() // Navigate back to the previous fragment
    }

    private fun showToast(message: String) {
        currentToast?.cancel() // Cancel the existing toast, if any
        currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).apply {
            show() // Show the new toast immediately
        }
    }

    private fun showDeleteConfirmationDialog() {
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
            dialog .dismiss() // Close the dialog
        }

        cancelButton.setOnClickListener {
            dialog.dismiss() // Close the dialog without taking action
        }

        dialog.show()
    }

    private fun deleteItem() {
        // Show "Item deleted" toast
        showToast("Item deleted!") // Use showToast to cancel any previous toast and show the new one

        // Perform the deletion logic here
        itemId?.let { id ->
            item?.let {
                val itemToDelete = convertToItem(it) // Convert ClothingItem back to Item
                itemViewModel.delete(itemToDelete) // Call the ViewModel to delete the item
            }
        }
        findNavController().popBackStack() // Navigate back after deleting the item
    }

    private fun updateItemInfo(item: ClothingItem) {
        // Update the UI with the item information
        val itemNameTextView = view?.findViewById<TextView>(R.id.item_name_text)
        val itemTypeTextView = view?.findViewById<TextView>(R.id.type_value)
        val itemImageView = view?.findViewById<ImageView>(R.id.item_image)

        itemNameTextView?.text = item.name
        itemTypeTextView?.text = item.type
        itemImageView?.setImageURI(item.getImageUri()) // Set the image URI

        // Update the color circle based on the item's color
        val colorCircle = view?.findViewById<View>(R.id.color_circle)
        val color = Color.parseColor(item.color) // Parse the color string to an actual color
        colorCircle?.setBackgroundColor(color) // Set the background color of the color circle

        isFavorite = item.isFavorite
        updateFavoriteIcon()

        // Update the worn text
        val wornTextView = view?.findViewById<TextView>(R.id.worn_text)
        wornTextView?.text = getString(R.string.worn_times, item.wornTimes) // Display the worn count

    }

    private fun updateFavoriteIcon() {
        val favoriteIcon = view?.findViewById<ImageView>(R.id.icon_favorite)
        if (isFavorite) {
            favoriteIcon?.setImageResource(R.drawable.icon_favorite)
        } else {
            favoriteIcon?.setImageResource(R.drawable.icon_unfavorite)
        }
    }

    private fun convertToClothingItem(item: Item): ClothingItem {
        // Convert the Item object to a ClothingItem object
        return ClothingItem(
            id = item.id, // Ensure you have an id to pass
            imageUri = item.imageUri,
            name = item.name,
            type = item.type,
            color = item.color,
            wornTimes = item.wornTimes, // Use wornTimes instead of wornCount
            lastWornDate = item.lastWornDate, // Pass lastWornDate from Item
            fragmentId = R.id.action_itemInfoFragment_to_editItemInfoFragment,
            isFavorite = item.isFavorite
        )
    }

    private fun convertToItem(clothingItem: ClothingItem): Item {
        // Convert the ClothingItem object back to an Item object
        return Item(
            id = clothingItem.id, // Ensure you have an id to pass
            name = clothingItem.name,
            type = clothingItem.type,
            color = clothingItem.color,
            wornTimes = clothingItem.wornTimes, // Use wornTimes instead of wornCount
            lastWornDate = clothingItem.lastWornDate ?: "", // Handle null case if necessary
            imageUri = clothingItem.imageUri,
            isFavorite = clothingItem.isFavorite
        )
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.status_item)
    }
}