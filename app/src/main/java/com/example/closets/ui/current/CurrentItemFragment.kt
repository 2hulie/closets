package com.example.closets.ui.current

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Import for navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.R
import com.example.closets.databinding.FragmentCurrentItemBinding
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.home.TodayOutfitBottomSheet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CurrentItemFragment : Fragment() {

    private var _binding: FragmentCurrentItemBinding? = null
    private val binding get() = _binding!!

    // Define all current items and sorted list
    private var allCurrentItems: List<CurrentItem> = listOf()
    private var sortedCurrentItems: MutableList<CurrentItem> = mutableListOf()

    // Variables to hold the currently applied filters
    private var appliedTypes: List<String>? = null
    private var appliedColors: List<String>? = null

    // Adapter for RecyclerView
    private lateinit var adapter: CurrentItemAdapter

    private val typeOptions = listOf(
        "Top", "Bottom", "Outerwear", "Dress", "Shoes", "Other"
    )

    private val colorOptions = mapOf(
        "red" to "#ff0000",
        "orange" to "#ffa500",
        "yellow" to "#ffff00",
        "green" to "#00ff00",
        "blue" to "#0000ff",
        "pink" to "#ff6eca",
        "purple" to "#800080",
        "white" to "#ffffff",
        "beige" to "#f5f5dd",
        "gray" to "#808080",
        "brown" to "#5e3e2b",
        "black" to "#000000"
    )

    // Singleton Toast inside the companion object
    companion object {
        private var currentToast: Toast? = null

        // This method shows the Toast and cancels any previous one
        fun showToast(context: Context, message: String) {
            currentToast?.cancel() // Cancel the previous toast
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show() // Show the new toast
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Disable and redirect bottom navigation
        redirectBottomNavigation()

        // Inflate the layout using view binding
        _binding = FragmentCurrentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput: EditText = binding.searchInput
        val searchButton: ImageView = binding.iconSearch

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        binding.currentItemsImage.startAnimation(slideDownAnimation)
        binding.currentDate.startAnimation(slideDownAnimation)
        binding.wearingTodayText.startAnimation(slideDownAnimation)


        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }

        // Initially disable the search button
        searchButton.isEnabled = false

        // TextWatcher to the search input
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Enable the search button if there is input, otherwise disable it
                searchButton.isEnabled = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set up click listener for the search button
        searchButton.setOnClickListener {
            val query = searchInput.text.toString()
            if (query.isNotEmpty()) {
                filterItems(query) // Call the filter method if there is input
            }
        }

        // Trigger search when "Enter" key is pressed on the keyboard
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchInput.text.toString()
                if (query.isNotEmpty()) {
                    filterItems(query) // Call the filter method if there is input
                    // Optionally close the keyboard
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
                }
                true // Indicating the action was handled
            } else {
                false // Otherwise, let the system handle it
            }
        }

        setStatusBarColor()

        // Set current date dynamically
        displayCurrentDate()

        // Initialize RecyclerView with GridLayoutManager for 3 items per row
        binding.recyclerViewCurrentItems.layoutManager = GridLayoutManager(requireContext(), 3)

        // Initialize all current items
        allCurrentItems = listOf(
            CurrentItem(R.drawable.cap, "Other", "#726C5D",false, "Cap"),
            CurrentItem(R.drawable.dress, "Dress", "#1C88A4", false, "Dress"),
            CurrentItem(R.drawable.shirt, "Top", "#3B9DBC", true, "Shirt"),
            CurrentItem(R.drawable.shorts, "Bottom", "#A8A7AB", false, "Shorts"),
            CurrentItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
            CurrentItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt"),
            CurrentItem(R.drawable.cap, "Other", "#726C5D",false, "Cap"),
            CurrentItem(R.drawable.dress, "Dress", "#1C88A4", false, "Dress"),
            CurrentItem(R.drawable.shirt, "Top", "#3B9DBC", true, "Shirt"),
            CurrentItem(R.drawable.shorts, "Bottom", "#A8A7AB", false, "Shorts"),
            CurrentItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
            CurrentItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt"),
            CurrentItem(R.drawable.cap, "Other", "#726C5D",false, "Cap"),
            CurrentItem(R.drawable.dress, "Dress", "#1C88A4", false, "Dress"),
            CurrentItem(R.drawable.shirt, "Top", "#3B9DBC", true, "Shirt"),
            CurrentItem(R.drawable.shorts, "Bottom", "#A8A7AB", false, "Shorts"),
            CurrentItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
            CurrentItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt")
        )

        // Set sortedCurrentItems to be a mutable list from allCurrentItems
        sortedCurrentItems = allCurrentItems.toMutableList()

        // Initialize the adapter with sorted items and set it to RecyclerView
        adapter = CurrentItemAdapter(sortedCurrentItems) { item ->
            // Handle item clicks - not needed for now
        }
        binding.recyclerViewCurrentItems.adapter = adapter

        // Set up click listener for the save icon
        binding.saveIcon.setOnClickListener {
            navigateToHomeAndShowBottomSheet()
        }

        // Setup the back pressed callback to show the discard dialog
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog()
            }
        })
    }

    private fun navigateToHomeAndShowBottomSheet() {
        // Show a toast message
        showToast(requireContext(), "Changes saved!")

        // Navigate to HomeFragment
        findNavController().navigate(R.id.action_currentItemFragment_to_homeFragment)

        // Show the TodayOutfitBottomSheet after a slight delay
        // Use a post delay to allow the HomeFragment to fully appear before showing the bottom sheet
        binding.saveIcon.postDelayed({
            val bottomSheetFragment = TodayOutfitBottomSheet()
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }, 300) // Adjust the delay as needed
    }

    private fun showFilterBottomSheet() {
        val bottomSheetDialog = FilterBottomSheetDialog(
            typeOptions = typeOptions,
            preselectedTypes = appliedTypes?.toList(), // Convert to immutable list
            preselectedColors = appliedColors?.toList(), // Convert to immutable list
            onApplyFilters = { types, colors ->
                // Update the stored filters
                appliedTypes = types?.toMutableList()
                appliedColors = colors?.toMutableList()

                // Print debug information
                println("Debug - Applied Types: $appliedTypes")
                println("Debug - Applied Colors: $appliedColors")

                applyFilters(types, colors)
            },
            onResetFilters = {
                appliedTypes = null
                appliedColors = null
                resetToOriginalList()
            }
        )

        bottomSheetDialog.show(parentFragmentManager, "FilterBottomSheetDialog")
    }

    // Convert hex to HSV for better color matching
    private fun hexToHSV(hex: String): FloatArray {
        val cleanHex = hex.replace("#", "")
        val r = cleanHex.substring(0, 2).toInt(16)
        val g = cleanHex.substring(2, 4).toInt(16)
        val b = cleanHex.substring(4, 6).toInt(16)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        return hsv
    }

    // Calculate color distance using HSV values
    private fun colorDistance(color1: FloatArray, color2: FloatArray): Double {
        // Weight factors for Hue, Saturation, and Value
        val hueWeight = 1.0
        val satWeight = 2.0
        val valWeight = 1.0

        // Calculate wrapped hue difference
        var hueDiff = Math.abs(color1[0] - color2[0])
        if (hueDiff > 180) hueDiff = 360 - hueDiff
        hueDiff /= 180 // Normalize to [0,1]

        // Calculate saturation and value differences
        val satDiff = Math.abs(color1[1] - color2[1])
        val valDiff = Math.abs(color1[2] - color2[2])

        // Weighted distance
        return hueDiff * hueWeight +
                satDiff * satWeight +
                valDiff * valWeight
    }

    private fun findClosestColor(itemHex: String, colorOptions: Map<String, String>): String {
        val itemHSV = hexToHSV(itemHex)
        var closestColor = colorOptions.keys.first()
        var minDistance = Double.MAX_VALUE

        // Define thresholds for specific colors
        val grayThreshold = 0.15 // Threshold for saturation to be considered gray
        val whiteThreshold = 0.85 // Threshold for value to be considered white
        val blackThreshold = 0.15 // Threshold for value to be considered black
        val beigeHueRange = Pair(20f, 40f) // Hue range for beige

        // Special case checks based on HSV values
        when {
            // Check for white (high value, low saturation)
            itemHSV[2] > whiteThreshold && itemHSV[1] < grayThreshold -> return "white"

            // Check for black (low value)
            itemHSV[2] < blackThreshold -> return "black"

            // Check for gray (low saturation, medium value)
            itemHSV[1] < grayThreshold && itemHSV[2] in 0.2f..0.8f -> return "gray"

            // Check for beige
            itemHSV[0] in beigeHueRange.first..beigeHueRange.second &&
                    itemHSV[1] < 0.35f &&
                    itemHSV[2] > 0.8f -> return "beige"
        }

        // For other colors, find the closest match
        colorOptions.forEach { (colorName, colorHex) ->
            // Skip special cases in normal comparison
            if (colorName !in listOf("white", "black", "gray", "beige")) {
                val optionHSV = hexToHSV(colorHex)
                val distance = colorDistance(itemHSV, optionHSV)
                if (distance < minDistance) {
                    minDistance = distance
                    closestColor = colorName
                }
            }
        }

        // Set a maximum distance threshold
        if (minDistance > 1.5) {
            // If no good match is found, fallback to gray for very desaturated colors
            if (itemHSV[1] < 0.2) return "gray"
            // For brown-ish colors
            if (itemHSV[0] in 20f..40f && itemHSV[1] > 0.2 && itemHSV[2] < 0.7) return "brown"
        }

        return closestColor
    }

    private fun applyFilters(types: List<String>?, colors: List<String>?) {
        // Debug logging to see color matches
        println("Color matches for current items:")
        allCurrentItems.forEach { item ->
            val closestColor = findClosestColor(item.color, colorOptions)
            println("${item.name} (${item.color}) -> $closestColor")
        }

        sortedCurrentItems = allCurrentItems.filter { item ->
            // Check if item matches type filter
            val matchesType = types.isNullOrEmpty() || types.contains("None") || types.contains(item.type)

            // Check if item matches color filter using closest color matching
            val matchesColor = if (colors.isNullOrEmpty() || colors.contains("None")) {
                true
            } else {
                val closestColor = findClosestColor(item.color, colorOptions)
                colors.contains(closestColor)
            }

            // Item must match both type AND color filters
            matchesType && matchesColor
        }.toMutableList()

        updateRecyclerView()

        // Reset search input
        binding.searchInput.text.clear()
    }

    // Override onSaveInstanceState to save filter state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("appliedTypes", ArrayList(appliedTypes ?: emptyList()))
        outState.putStringArrayList("appliedColors", ArrayList(appliedColors ?: emptyList()))
    }

    // Override onCreate to restore filter state
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            appliedTypes = it.getStringArrayList("appliedTypes")?.toMutableList()
            appliedColors = it.getStringArrayList("appliedColors")?.toMutableList()
        }
    }

    override fun onPause() {
        super.onPause()
        // Reset applied filters when navigating away from this fragment
        appliedTypes = null
        appliedColors = null
        resetToOriginalList() // Restore the original list of items
    }

    // Modify your onResume to preserve filters
    override fun onResume() {
        super.onResume()
        // Only reset if there are no applied filters
        if (appliedTypes == null && appliedColors == null) {
            resetToOriginalList()
        } else {
            // Reapply existing filters
            applyFilters(appliedTypes, appliedColors)
        }
    }

    private fun filterItems(query: String) {
        // Filter items based on the search query
        val filteredList = allCurrentItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||  // Search by name
                    item.type.contains(query, ignoreCase = true)    // Search by type
        }

        // Update the RecyclerView with the filtered list
        sortedCurrentItems = filteredList.toMutableList()
        updateRecyclerView()
    }

    private fun resetToOriginalList() {
        // Reset sortedCurrentItems to the original allCurrentItems list
        sortedCurrentItems = allCurrentItems.toMutableList()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        if (sortedCurrentItems.isEmpty()) {
            showEmptyMessage()
        } else {
            hideEmptyMessage()
            adapter.updateItems(sortedCurrentItems)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showEmptyMessage() {
        binding.emptyMessage.visibility = View.VISIBLE
        binding.emptyMessage.text = "No items found."
        binding.recyclerViewCurrentItems.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.visibility = View.GONE
        binding.recyclerViewCurrentItems.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun redirectBottomNavigation() {
        // Access the NavController and redirect based on current destination
        val navController = findNavController()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Disable default navigation for this fragment
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == R.id.currentItemFragment) {
                    showDiscardChangesDialog() // Show dialog when navigation is attempted
                    return@setOnMenuItemClickListener true // Block navigation
                }
                return@setOnMenuItemClickListener false // Allow normal behavior for other fragments
            }
        }
    }

    private fun restoreBottomNavigation() {
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Reset all menu item click listeners to null (restore default behavior)
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener(null)
        }
    }

    private fun showDiscardChangesDialog() {
        // Inflate the custom layout
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_discard_changes, null)

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView) // Set the custom layout
            .create()

        // Remove the default background to avoid unwanted outlines
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set up button actions
        customView.findViewById<ImageView>(R.id.btn_discard).setOnClickListener {
            // Restore bottom navigation behavior
            restoreBottomNavigation()

            // Go back to the previous fragment in the navigation stack
            findNavController().navigateUp()

            dialog.dismiss() // Close the dialog
        }
        customView.findViewById<ImageView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss() // Close the dialog without taking any action
        }

        dialog.show()
    }

    // Helper function to get and display the current date
    private fun displayCurrentDate() {
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        binding.currentDate.text = currentDate
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_items)
    }
}