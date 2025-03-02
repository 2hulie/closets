package com.example.closets.ui.edit

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.closets.BaseModifyItemFragment
import com.example.closets.R
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class EditItemFragment : BaseModifyItemFragment() {
    // View declarations
    private lateinit var nameEditText: EditText
    private lateinit var wornTimesTextView: TextView
    private lateinit var lastWornTextView: TextView
    private lateinit var editImageView: ImageView
    override val targetImageView: ImageView
        get() = editImageView

    private var currentView: View? = null // current view reference for safe interaction
    private var itemId: Int? = null // To hold the passed item ID
    private var item: ClothingItem? = null // To hold the item data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentView = inflater.inflate(R.layout.fragment_item_info_edit, container, false)

        // Initialize the ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(repository)
        )[ItemViewModel::class.java]

        // Get the item ID from the arguments
        itemId = arguments?.getInt("item_id")

        // Load the item data
        itemId?.let { id ->
            itemViewModel.getItem(id).observe(viewLifecycleOwner) { item: Item? ->
                item?.let {
                    this.item = convertToClothingItem(it) // Convert Item to ClothingItem
                    populateExistingData() // Populate the UI with the item data
                }
            }
        }

        setStatusBarColor()
        initializeViews(currentView!!)
        setupInteractions()
        populateExistingData()
        redirectBottomNavigation()

        // Setup the back pressed callback to show the discard dialog
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog()
            }
        })

        return currentView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Restore default navigation behavior when leaving this fragment
        restoreBottomNavigation()

        currentView = null
    }

    private fun redirectBottomNavigation() {
        // Access the NavController and redirect based on current destination
        val navController = findNavController()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Disable default navigation for this fragment
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == R.id.editItemInfoFragment) {
                    showDiscardChangesDialog() // Show dialog when navigation is attempted
                    return@setOnMenuItemClickListener true // Block navigation
                }
                return@setOnMenuItemClickListener false // Allow normal behavior for other fragments
            }
        }
    }

    private fun initializeViews(view: View) {
        nameEditText = view.findViewById(R.id.edit_name_text)
        wornTimesTextView = view.findViewById(R.id.worn_text)
        typeSpinner = view.findViewById(R.id.sort_by_spinner)
        colorCircle = view.findViewById(R.id.color_circle)
        lastWornTextView = view.findViewById(R.id.last_worn_value)
        editImageView = view.findViewById(R.id.item_image)
    }

    private fun setupInteractions() {
        setupSortSpinner()

        // Add image click listener for photo upload
        editImageView.setOnClickListener {
            pickImageFromGalleryOrCamera()
        }

        // Image-Based Color Picker Setup
        colorCircle.setOnClickListener {
            showImageColorPickerDialog()
        }

        // Edit Color Text Click Interaction
        val changeColorText = currentView?.findViewById<TextView>(R.id.change_color_text)
        changeColorText?.setOnClickListener {
            showImageColorPickerDialog() // Open the same color picker dialog
        }

        // Button Interactions
        currentView?.post {
            try {
                currentView?.findViewById<ImageView>(R.id.icon_save_changes)?.setOnClickListener {
                    saveItemChanges()
                }
                currentView?.findViewById<ImageView>(R.id.icon_cancel)?.setOnClickListener {
                    showDiscardChangesDialog()
                }
                currentView?.findViewById<ImageView>(R.id.icon_back)?.setOnClickListener {
                    showDiscardChangesDialog()
                }
            } catch (e: Exception) {
                Log.e("SetupInteractions", "Error setting up buttons", e)
            }
        }
    }

    fun saveItemChanges() {
        if (validateInputs()) {
            // Check for duplicate item name
            val itemName = nameEditText.text.toString()
            itemViewModel.items.observe(viewLifecycleOwner) { existingItems ->
                val isDuplicate = existingItems.any { it.name.equals(itemName, ignoreCase = true) && it.id != (item?.id ?: 0) }
                if (isDuplicate) {
                    showToast(requireContext(), "Name already exists.")
                    return@observe // Exit the function if duplicate found
                }

                // Proceed to save the item if no duplicates
                try {
                    val itemType = typeSpinner.selectedItem.toString()
                    val wornTimes = wornTimesTextView.text.toString().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    val lastWornDate = lastWornTextView.text.toString()

                    val itemColor = (colorCircle.background as? ColorDrawable)?.color ?: Color.GRAY
                    val formattedColor = String.format("#%06X", (0xFFFFFF and itemColor))

                    // Get the image URI from the ImageView
                    val imageUriString = imageUri?.toString() ?: run {
                        showToast(requireContext(), "Error: No image selected")
                        return@observe
                    }

                    // Create a new Item object
                    val updatedItem = Item(
                        id = item?.id ?: 0, // Use existing ID for the item being edited
                        name = itemName,
                        type = itemType,
                        color = formattedColor,
                        wornTimes = wornTimes,
                        lastWornDate = lastWornDate,
                        imageUri = imageUriString,
                        isFavorite = item?.isFavorite ?: false // Keep the favorite status
                    )

                    // Update the item using the ViewModel
                    itemViewModel.update(updatedItem)

                    Log.d(
                        "SaveItemChanges",
                        "Item Name: $itemName, Type: $itemType, Worn Times: $wornTimes, Last Worn: $lastWornDate, Color: $formattedColor"
                    )
                    showToast(requireContext(), "Changes saved!")

                    restoreBottomNavigation()

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e("SaveItemChanges", "Error saving item changes", e)
                    showToast(requireContext(), "An error occurred while saving changes")
                }
            }
        }
    }

    private fun populateExistingData() {
        item?.let {
            nameEditText.setText(it.name) // Update the name field
            // Set the spinner selection based on the item type
            val typePosition = resources.getStringArray(R.array.filter_options).indexOf(it.type)
            typeSpinner.setSelection(typePosition)

            // Set the color circle based on the item's color
            colorCircle.setBackgroundColor(Color.parseColor(it.color))

            // Update the worn times text
            wornTimesTextView.text = if (it.wornTimes == 1) {
                getString(R.string.worn_time, it.wornTimes) // Use singular form
            } else {
                getString(R.string.worn_times, it.wornTimes) // Use plural form
            }

            lastWornTextView.text = it.lastWornDate ?: ""

            // Set the image URI
            imageUri = Uri.parse(it.imageUri) // Store the existing image URI
            editImageView.setImageURI(imageUri)
        }
    }

    // Validate inputs and show Toast messages
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun validateInputs(): Boolean {
        // Check if item name is empty
        if (nameEditText.text.isBlank()) {
            showToast(requireContext(), "Item name cannot be empty")
            return false
        }

        // Check if an item type is selected
        if (typeSpinner.selectedItemPosition == 0) {
            showToast(requireContext(), "Please select an item type")
            return false
        }

        // Check if an image is selected
        val drawable = editImageView.drawable
        if (drawable == null || drawable.constantState == resources.getDrawable(R.drawable.add_item_image).constantState) {
            showToast(requireContext(), "Please upload a valid image")
            return false
        }

        return true
    }

    private fun convertToClothingItem(item: Item): ClothingItem {
        return ClothingItem(
            id = item.id,
            imageUri = item.imageUri,
            name = item.name,
            type = item.type,
            color = item.color,
            wornTimes = item.wornTimes,
            lastWornDate = item.lastWornDate,
            fragmentId = R.id.action_itemInfoFragment_to_editItemInfoFragment,
            isFavorite = item.isFavorite
        )
    }
}