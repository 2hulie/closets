package com.example.closets.ui.add

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.closets.BaseModifyItemFragment
import com.example.closets.R
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class AddItemFragment : BaseModifyItemFragment() {
    private lateinit var addItemNameEditText: EditText
    private lateinit var wornTimesTextView: TextView
    private lateinit var lastWornTextView: TextView
    private lateinit var addImageView: ImageView
    override val targetImageView: ImageView
        get() = addImageView

    private var currentView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentView = inflater.inflate(R.layout.fragment_add_item, container, false) ?: return requireView()

        setStatusBarColor()

        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]
        initializeViews(currentView!!)

        setupInteractions()
        populateExistingData()
        redirectBottomNavigation()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog()
            }
        })

        return currentView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        restoreBottomNavigation()
        currentView = null
    }

    private fun initializeViews(view: View) {
        addItemNameEditText = view.findViewById(R.id.add_name_text)
        wornTimesTextView = view.findViewById(R.id.worn_text)
        typeSpinner = view.findViewById(R.id.sort_by_spinner)
        colorCircle = view.findViewById(R.id.color_circle)
        lastWornTextView = view.findViewById(R.id.last_worn_value)
        addImageView = view.findViewById(R.id.add_image)
    }

    private fun setupInteractions() {
        setupSortSpinner()

        addImageView.setOnClickListener {
            pickImageFromGalleryOrCamera()
        }

        colorCircle.setOnClickListener {
            val drawable = addImageView.drawable
            val defaultDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.add_item_image)

            if (drawable == null || drawable.constantState == defaultDrawable?.constantState) {
                showToast(requireContext(), "Please upload an image first.")
            } else {
                showImageColorPickerDialog()
            }
        }

        val changeColorText = currentView?.findViewById<TextView>(R.id.change_color_text)
        changeColorText?.setOnClickListener {
            val drawable = addImageView.drawable
            val defaultDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.add_item_image)

            if (drawable == null || drawable.constantState == defaultDrawable?.constantState) {
                showToast(requireContext(), "Please upload an image first.")
            } else {
                showImageColorPickerDialog()
            }
        }

        // Button Interactions
        currentView?.post {
            try {
                currentView?.findViewById<ImageView>(R.id.icon_add_item)?.setOnClickListener {
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
            val itemName = addItemNameEditText.text.toString()
            itemViewModel.items.observe(viewLifecycleOwner) { existingItems ->
                val isDuplicate = existingItems.any { it.name.equals(itemName, ignoreCase = true) }
                if (isDuplicate) {
                    showToast(requireContext(), "Name already exists.")
                    return@observe // exit the function if duplicate found
                }

                try {
                    val itemType = typeSpinner.selectedItem.toString()
                    val wornTimes = wornTimesTextView.text.toString().substringAfter("worn ").substringBefore(" times").toInt()
                    val lastWornDate = lastWornTextView.text.toString()

                    val formattedColor = String.format("#%06X", (0xFFFFFF and selectedColor))

                    val imageUriString = imageUri?.toString() ?: run {
                        showToast(requireContext(), "Error: No image selected")
                        return@observe
                    }

                    val newItem = Item(
                        name = itemName,
                        type = itemType,
                        color = formattedColor,
                        wornTimes = wornTimes,
                        lastWornDate = lastWornDate,
                        imageUri = imageUriString,
                        isFavorite = false
                    )

                    itemViewModel.insert(newItem)

                    Log.d(
                        "SaveItemChanges",
                        "Item Name: $itemName, Type: $itemType, Worn Times: $wornTimes, Last Worn: $lastWornDate, Color: $formattedColor"
                    )
                    showToast(requireContext(), "Item added to closet!")

                    restoreBottomNavigation()

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e("SaveItemChanges", "Error saving item changes", e)
                    showToast(requireContext(), "An error occurred while saving changes")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun populateExistingData() {
        addItemNameEditText.hint = "Tap to add name"
        typeSpinner.setSelection(0)
        colorCircle.background = ColorDrawable(Color.parseColor("#FFFFFF"))
        wornTimesTextView.text = "worn 0 times"
        lastWornTextView.text = "N/A"
    }

    // validate inputs and show Toast messages
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun validateInputs(): Boolean {
        // check if item name is empty
        if (addItemNameEditText.text.isBlank()) {
            showToast(
                requireContext(),
                "Item name cannot be empty"
            )
            return false
        }

        if (typeSpinner.selectedItemPosition == 0) {
            showToast(
                requireContext(),
                "Please select an item type"
            )
            return false
        }

        // check if an image is selected and if it's not the default image
        val drawable = addImageView.drawable
        if (drawable == null || drawable.constantState == ContextCompat.getDrawable(requireContext(), R.drawable.add_item_image)?.constantState) {
            showToast(
                requireContext(),
                "Please upload a valid image"
            )
            return false
        }

        // if all validations pass
        return true
    }

    private fun redirectBottomNavigation() {
        // Access the NavController and redirect based on current destination
        val navController = findNavController()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Disable default navigation for this fragment
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == R.id.addItemFragment) {
                    showDiscardChangesDialog() // Show dialog when navigation is attempted
                    return@setOnMenuItemClickListener true // Block navigation
                }
                return@setOnMenuItemClickListener false // Allow normal behavior for other fragments
            }
        }
    }
}
