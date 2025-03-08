package com.example.closets.ui.favorites

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.BaseItemFragment
import com.example.closets.R
import com.example.closets.databinding.FragmentFavoritesBinding
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.launch

class FavoritesFragment : BaseItemFragment() {

    private var _binding: FragmentFavoritesBinding? = null
    override val binding: View
        get() = _binding!!.root

    private var allFavoriteItems: List<ClothingItem> = listOf()
    private var sortedFavoriteItems: MutableList<ClothingItem> = mutableListOf()

    override lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val trace = FirebasePerformance.getInstance().newTrace("favoritesFragment_onCreateView")
        trace.start()

        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (binding as ViewGroup).addView(loadingView)

        initializeViewModel(requireContext())

        trace.stop()
        return binding
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trace = FirebasePerformance.getInstance().newTrace("favoritesFragment_onViewCreated")
        trace.start()

        loadingView?.visibility = View.VISIBLE // show loading view initially
        _binding!!.recyclerViewFavorites.visibility = View.GONE
        _binding!!.recyclerViewFavorites.layoutManager = GridLayoutManager(requireContext(), 3)

        itemViewModel.favoriteItems.observe(viewLifecycleOwner) { favoriteItems ->
            lifecycleScope.launch {
                allFavoriteItems = favoriteItems.map { convertToClothingItem(it) }
                sortedFavoriteItems = allFavoriteItems.toMutableList()

                // Hide loading view and show RecyclerView
                loadingView?.visibility = View.GONE
                _binding!!.recyclerViewFavorites.visibility = View.VISIBLE

                if (sortedFavoriteItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                }

                adapter.updateItems(sortedFavoriteItems)
                updateItemsCount(sortedFavoriteItems.size)
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
                updateItemsCount(sortedFavoriteItems.size)
                if (sortedFavoriteItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                }
            }
        )

        _binding!!.recyclerViewFavorites.adapter = adapter

        _binding!!.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }

        val searchInput: EditText = _binding!!.searchInput
        val searchButton: ImageView = _binding!!.iconSearch

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
        trace.stop()
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
            fragmentId = R.id.action_favoritesFragment_to_itemInfoFragment,
        )
    }

    override fun updateItemsCount(count: Int) {
        val dynamicTitle = resources.getQuantityString(R.plurals.favorite_items_count, count, count)
        _binding!!.favoriteItemsCountText.text = dynamicTitle
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
        _binding!!.searchInput.text.clear()
    }

    override fun clearSearchInput() {
        _binding!!.searchInput.text.clear()
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
        _binding!!.searchInput.text.clear()
        isViewingSearchResults = false
        resetToOriginalList()
    }

    override fun resetToOriginalList() {
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
        updateItemsCount(sortedFavoriteItems.size)
    }

    @SuppressLint("SetTextI18n")
    private fun showEmptyMessage() {
        _binding!!.emptyMessage.visibility = View.VISIBLE
        _binding!!.emptyMessage.text = "No items found."
        _binding!!.recyclerViewFavorites.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        _binding!!.emptyMessage.visibility = View.GONE
        _binding!!.recyclerViewFavorites.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_favorites)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
