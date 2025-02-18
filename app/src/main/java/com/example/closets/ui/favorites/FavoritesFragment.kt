package com.example.closets.ui.favorites

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.R
import com.example.closets.databinding.FragmentFavoritesBinding
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    val binding get() = _binding!!

    private lateinit var itemViewModel: ItemViewModel

    private var allFavoriteItems: List<ClothingItem> = listOf()
    private var sortedFavoriteItems: MutableList<ClothingItem> = mutableListOf()

    // Tracks whether the fragment is showing search results
    var isViewingSearchResults = false
    private var _hasActiveFilters = false
    private var loadingView: View? = null

    // Variable to hold the currently applied filters for type
    private var appliedTypes: List<String>? = null
    private var appliedColors: List<String>? = null

    private lateinit var adapter: FavoritesAdapter

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

    fun hasActiveFilters(): Boolean {
        return appliedTypes != null || appliedColors != null || _hasActiveFilters
    }

    fun clearAllFilters() {
        appliedTypes = null
        appliedColors = null
        _hasActiveFilters = false
        resetToOriginalList()
        binding.searchInput.text.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        // Inflate the loading view
        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (binding.root as ViewGroup).addView(loadingView) // Add loading view to the fragment's view

        // Initialize the ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]

        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show loading view initially
        loadingView?.visibility = View.VISIBLE
        binding.recyclerViewFavorites.visibility = View.GONE

        // Initialize RecyclerView
        binding.recyclerViewFavorites.layoutManager = GridLayoutManager(requireContext(), 3)

        // Observe favorite items
        itemViewModel.favoriteItems.observe(viewLifecycleOwner) { favoriteItems ->
            lifecycleScope.launch {
                delay(100) // Optional delay for loading effect

                allFavoriteItems = favoriteItems.map { convertToClothingItem(it) }
                sortedFavoriteItems = allFavoriteItems.toMutableList()

                // Hide loading view and show RecyclerView
                loadingView?.visibility = View.GONE
                binding.recyclerViewFavorites.visibility = View.VISIBLE

                if (sortedFavoriteItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                }

                adapter.updateItems(sortedFavoriteItems) // Update the adapter
                updateItemsCount() // Update the item count
            }
        }

        // Initialize the adapter
        adapter = FavoritesAdapter(
            sortedFavoriteItems,
            { item ->
                // Create a Bundle to pass the item ID
                val bundle = Bundle().apply {
                    putInt("item_id", item.id) // Pass the item ID
                }
                // Navigate to ItemInfoFragment with the Bundle
                findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoFragment, bundle)
            },
            {
                // Notify when an item is removed and update the title count
                updateItemsCount()
                if (sortedFavoriteItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                }
            }
        )

        binding.recyclerViewFavorites.adapter = adapter

        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }

        val searchInput: EditText = binding.searchInput
        val searchButton: ImageView = binding.iconSearch

        // Initially disable the search button
        searchButton.isEnabled = false

        // TextWatcher to the search input
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Always enable the search button
                searchButton.isEnabled = true
            }

            override fun afterTextChanged(s: Editable?) {}

        })

        // Set up click listener for the search button
        searchButton.setOnClickListener {
            val query = searchInput.text.toString()
            if (query.isNotEmpty()) {
                filterItems(query) // Call the filter method if there is input
            } else {
                resetSearchResults()
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
                } else {
                    resetSearchResults()
                }
                true // Indicating the action was handled
            } else {
                false // Otherwise, let the system handle it
            }
        }

        // Change status bar color for this fragment
        setStatusBarColor()

        // Observe errors
        itemViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error message
                showToast(requireContext(), it)
                // Clear the error after showing it
                itemViewModel.clearError()
            }
        }
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
            isFavorite = item.isFavorite,
            fragmentId = R.id.action_favoritesFragment_to_itemInfoFragment
        )
    }


    // Method to update the dynamic title with the item count
    fun updateItemsCount() {
        val itemCount = sortedFavoriteItems.size
        val dynamicTitle = resources.getQuantityString(R.plurals.favorite_items_count, itemCount, itemCount)
        binding.favoriteItemsCountText.text = dynamicTitle
    }

    private fun showFilterBottomSheet() {
        val bottomSheetDialog = FilterBottomSheetDialog(
            typeOptions = typeOptions,
            colorOptions = colorOptions,
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

        val beigeHex = "#F5F5DD"
        val beigeHSV = hexToHSV(beigeHex)

        // Special case checks based on HSV values
        when {
            // Check for exact color matches
            itemHex.equals(beigeHex, ignoreCase = true) -> return "beige"
            itemHex.equals("#FFFFFF", ignoreCase = true) -> return "white"
            itemHex.equals("#000000", ignoreCase = true) -> return "black"
            itemHex.equals("#808080", ignoreCase = true) -> return "gray"
            itemHex.equals("#FF0000", ignoreCase = true) -> return "red"
            itemHex.equals("#FFA500", ignoreCase = true) -> return "orange"
            itemHex.equals("#FFFF00", ignoreCase = true) -> return "yellow"
            itemHex.equals("#00FF00", ignoreCase = true) -> return "green"
            itemHex.equals("#0000FF", ignoreCase = true) -> return "blue"
            itemHex.equals("#FF69B4", ignoreCase = true) -> return "pink"
            itemHex.equals("#800080", ignoreCase = true) -> return "purple"
            itemHex.equals("#5E3E2B", ignoreCase = true) -> return "brown"
        }

        // Check for beige based on HSV values
        if (colorDistance(itemHSV, beigeHSV) < 0.1) return "beige"

        // For other colors, find the closest match
        colorOptions.forEach { (colorName, colorHex) ->
            // Calculate the HSV for the color option
            val optionHSV = hexToHSV(colorHex)
            val distance = colorDistance(itemHSV, optionHSV)

            // Check if the distance is within a certain threshold
            if (distance < minDistance) {
                minDistance = distance
                closestColor = colorName
            }
        }

        // Set a maximum distance threshold for fallback colors
        if (minDistance > 1.5) {
            // If no good match is found, fallback to gray for very desaturated colors
            if (itemHSV[1] < 0.2) return "gray"
            // For brown-ish colors
            if (itemHSV[0] in 20f..40f && itemHSV[1] > 0.2 && itemHSV[2] < 0.7) return "brown"
        }

        return closestColor
    }

    fun applyFilters(types: List<String>?, colors: List<String>?) {
        _hasActiveFilters = !(types.isNullOrEmpty() && colors.isNullOrEmpty())

        // Debug logging to see color matches
        println("Color matches for current items:")
        allFavoriteItems.forEach { item ->
            val closestColor = findClosestColor(item.color, colorOptions)
            println("${item.name} (${item.color}) -> $closestColor")
        }

        sortedFavoriteItems = allFavoriteItems.filter { item ->
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
        val filteredList = allFavoriteItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||  // Search by name
                    item.type.contains(query, ignoreCase = true)    // Search by type
        }

        // Update the RecyclerView with the filtered list
        sortedFavoriteItems = filteredList.toMutableList()
        updateRecyclerView()
        isViewingSearchResults = true  // Set the state to indicate search results are being viewed
    }

    // Method to reset search results and revert to the original list
    fun resetSearchResults() {
        binding.searchInput.text.clear()
        isViewingSearchResults = false
        resetToOriginalList()
    }

    private fun resetToOriginalList() {
        sortedFavoriteItems = allFavoriteItems.toMutableList()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        if (sortedFavoriteItems.isEmpty()) {
            showEmptyMessage()
        } else {
            hideEmptyMessage()
            adapter.updateItems(sortedFavoriteItems)
        }

        // Update the dynamic title (item count) after updating the list
        updateItemsCount()
    }

    @SuppressLint("SetTextI18n")
    private fun showEmptyMessage() {
        binding.emptyMessage.visibility = View.VISIBLE
        binding.emptyMessage.text = "No items found."
        binding.recyclerViewFavorites.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.visibility = View.GONE
        binding.recyclerViewFavorites.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_favorites)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
