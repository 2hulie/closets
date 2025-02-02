package com.example.closets.ui.favorites

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.R
import com.example.closets.databinding.FragmentFavoritesBinding
import com.example.closets.ui.FilterBottomSheetDialog

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private var favoriteItems: List<FavoriteItem> = listOf()
    private var sortedFavoriteItems: MutableList<FavoriteItem> = mutableListOf()

    // Tracks whether the fragment is showing search results
    var isViewingSearchResults = false

    private var _hasActiveFilters = false

    // Variable to hold the currently applied filters for type
    private var appliedTypes: List<String>? = null
    private var appliedColors: List<String>? = null

    lateinit var adapter: FavoritesAdapter

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
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        val searchInput: EditText = binding.searchInput
        val searchButton: ImageView = binding.iconSearch

        binding.favoritesImage.startAnimation(slideDownAnimation)
        binding.favoriteItemsCountText.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

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

        // Initialize RecyclerView with GridLayoutManager for 3 items per row
        binding.recyclerViewFavorites.layoutManager = GridLayoutManager(requireContext(), 3)

        // Initialize favorite items
        favoriteItems = listOf(
            FavoriteItem(R.drawable.shirt, "Top", "#3B9DBC",  true, "Shirt"),
            FavoriteItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt"),
            FavoriteItem(R.drawable.cap, "Other", "#726C5D", true, "Cap"),
            FavoriteItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
            FavoriteItem(R.drawable.shirt, "Top", "#3B9DBC",  true, "Shirt"),
            FavoriteItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt"),
            FavoriteItem(R.drawable.cap, "Other", "#726C5D", true, "Cap"),
            FavoriteItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
            FavoriteItem(R.drawable.shirt, "Top", "#3B9DBC",  true, "Shirt"),
            FavoriteItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt"),
            FavoriteItem(R.drawable.cap, "Other", "#726C5D", true, "Cap"),
            FavoriteItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
            FavoriteItem(R.drawable.shirt, "Top", "#3B9DBC",  true, "Shirt"),
            FavoriteItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Skirt"),
            FavoriteItem(R.drawable.cap, "Other", "#726C5D", true, "Cap"),
            FavoriteItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Shoes"),
        )

        // Set sortedFavoriteItems to be a mutable list from favoriteItems
        sortedFavoriteItems = favoriteItems.toMutableList()

        // Initialize the adapter with sorted items and set it to RecyclerView
        adapter = FavoritesAdapter(sortedFavoriteItems, { item ->
            // Handle item click (navigate to specific item page)
            val delayMillis = 150L
            when (item.name) {
                "Cap" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoCapFragment)
                }, delayMillis)
                "Shirt" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoShirtFragment)
                }, delayMillis)
                "Skirt" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoSkirtFragment)
                }, delayMillis)
                "Shoes" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoShoesFragment)
                }, delayMillis)
            }
        }, {
            // Notify when an item is removed and update the title count
            updateItemsCount()

            // Check if the list is empty and show the empty message if it is
            if (sortedFavoriteItems.isEmpty()) {
                showEmptyMessage()
            } else {
                hideEmptyMessage()
            }
        })

        binding.recyclerViewFavorites.adapter = adapter

        // Update the dynamic title initially
        updateItemsCount()
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

    fun applyFilters(types: List<String>?, colors: List<String>?) {
        _hasActiveFilters = !(types.isNullOrEmpty() && colors.isNullOrEmpty())

        // Debug logging to see color matches
        println("Color matches for current items:")
        favoriteItems.forEach { item ->
            val closestColor = findClosestColor(item.color, colorOptions)
            println("${item.name} (${item.color}) -> $closestColor")
        }

        sortedFavoriteItems = favoriteItems.filter { item ->
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
        val filteredList = favoriteItems.filter { item ->
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
        sortedFavoriteItems = favoriteItems.toMutableList()
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

    private fun showEmptyMessage() {
        val fullMessage = getString(R.string.no_items_available)

        // Find the index of the "Add a new item?" part
        val start = fullMessage.indexOf("Add a new item?")
        val end = start + "Add a new item?".length

        // Create a SpannableString to apply different styles
        val spannableString = SpannableString(fullMessage)

        // Make "Add a new item?" clickable and prevent the cyan highlight
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showAddItemFragment() // Open the Add Item Fragment when clicked
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(requireContext(), R.color.color_items)
                ds.bgColor = ContextCompat.getColor(requireContext(), R.color.faded_pink)
            }
        }

        // Apply clickable span
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the SpannableString to the TextView
        binding.emptyMessage.text = spannableString
        binding.emptyMessage.movementMethod = LinkMovementMethod.getInstance() // Make links clickable

        // Ensure the message is visible
        binding.emptyMessage.visibility = View.VISIBLE
        binding.recyclerViewFavorites.visibility = View.GONE
    }

    private fun showAddItemFragment() {
        // Navigate to the Add Item Fragment
        findNavController().navigate(R.id.action_favoritesFragment_to_addItemFragment)
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
