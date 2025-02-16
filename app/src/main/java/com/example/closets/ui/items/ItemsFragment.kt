package com.example.closets.ui.items

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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.R
import com.example.closets.databinding.FragmentItemsBinding
import com.example.closets.ui.FilterBottomSheetDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import kotlinx.coroutines.launch

class ItemsFragment : Fragment(), ItemsAdapter.SelectionCallback {

    private var _binding: FragmentItemsBinding? = null
    val binding get() = _binding!!

    private lateinit var itemViewModel: ItemViewModel

    var allItems: List<ClothingItem> = listOf()
    var sortedItems: MutableList<ClothingItem> = mutableListOf()

    private lateinit var selectAllCheckbox: CheckBox
    var isSelectingMultiple = false
    private var isSelectionMode = false

    // Tracks whether the fragment is showing search results
    var isViewingSearchResults = false

    private var _hasActiveFilters = false

    // Variables to hold the currently applied filters
    var appliedTypes: List<String>? = null
    var appliedColors: List<String>? = null

    private lateinit var adapter: ItemsAdapter

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
        _binding = FragmentItemsBinding.inflate(inflater, container, false)

        // Initialize the ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]

        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        binding.recyclerViewItems.layoutManager = GridLayoutManager(requireContext(), 3)

        itemViewModel.items.observe(viewLifecycleOwner) { items ->
            allItems = items.map { convertToClothingItem(it) }
            sortedItems = allItems.toMutableList()

            Log.d("ItemsFragment", "Items fetched: ${sortedItems.size}") // Log the number of items
            Log.d("ItemsFragment", "Items: $sortedItems") // Log the items

            adapter.updateItems(sortedItems) // Update the adapter
            updateItemsCount() // Update the item count
        }

        // Initialize the adapter
        initializeViews(view)

        // Set up icon_add click listener
        binding.iconAdd.setOnClickListener {
            showAddItemFragment()
        }
        // Initialize the adapter
        adapter = ItemsAdapter(
            sortedItems,
            { item ->
                // Create a Bundle to pass the item ID
                val bundle = Bundle().apply {
                    putInt("item_id", item.id) // Pass the item ID
                }
                // Navigate to ItemInfoFragment with the Bundle
                findNavController().navigate(R.id.action_itemsFragment_to_itemInfoFragment, bundle)
            },
            this,
            itemViewModel
        )

        binding.recyclerViewItems.adapter = adapter


        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }

        // Get reference to the ellipsis icon
        val ellipsisIcon: ImageView = view.findViewById(R.id.ellipsis_icon)

        // Handle the click event for the ellipsis icon
        ellipsisIcon.setOnClickListener {
            // Call the showDropdown function to display the dropdown
            showDropdown(it)  // Pass the view to showDropdown to anchor the menu
        }

        // Initialize the selectAllCheckbox inside onViewCreated
        selectAllCheckbox = view.findViewById(R.id.select_all_checkbox)

        binding.selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Select all items
                adapter.selectAllItems()
            } else {
                // Clear all selections
                adapter.clearSelections()
            }

            // Make sure the Delete button is still visible in clear selection
            binding.iconDeleteMultiple.visibility = View.VISIBLE

            updateDeleteButtonVisibility() // Refresh delete button (if needed)
            updateItemsCount() // Update the count dynamically
        }

        val deleteIcon: ImageView = binding.iconDeleteMultiple
        val cancelIcon: ImageView = binding.iconCancelMultiple

        // Hide these icons by default
        deleteIcon.visibility = View.GONE
        cancelIcon.visibility = View.GONE

        // Handle cancel action
        cancelIcon.setOnClickListener {
            // Exit select multiple mode
            exitSelectMultipleMode()
            binding.iconAdd.visibility = View.VISIBLE
            // Reset search input and restore the original list
            binding.searchInput.text.clear()
            appliedTypes = null // Reset applied types
            appliedColors = null // Reset applied colors
            resetToOriginalList()
        }

        // Handle delete action
        deleteIcon.setOnClickListener {
            // Delete selected items
            deleteSelectedItems()
        }

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        val itemsImageView = binding.itemsImage
        val itemsCountText = binding.itemsCountText
        val searchInput: EditText = binding.searchInput
        val searchButton: ImageView = binding.iconSearch

        itemsCountText.startAnimation(slideDownAnimation)
        itemsImageView.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

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
                // Clear applied filters before filtering
                appliedTypes = null
                appliedColors = null
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
        // Convert the Item object to a ClothingItem object
        return ClothingItem(
            id = item.id, // Ensure you have an id to pass
            imageUri = item.imageUri,
            name = item.name,
            type = item.type,
            color = item.color,
            wornTimes = item.wornTimes, // Use wornTimes instead of wornCount
            lastWornDate = item.lastWornDate, // Pass lastWornDate from Item
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment, // Provide the appropriate fragment ID
            isFavorite = item.isFavorite
        )
    }

    // Function to display the dropdown menu
    @SuppressLint("ResourceType", "InflateParams")
    private fun showDropdown(view: View) {
        // Get the string array from resources
        val ellipsisItems = resources.getStringArray(R.array.ellipsis_items)

        // Create a linear layout to hold the items
        val linearLayout = LinearLayout(requireContext())
        linearLayout.orientation = LinearLayout.VERTICAL

        // Create a PopupWindow to display the menu
        val popupWindow = PopupWindow(linearLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.isFocusable = true

        // Create and add menu items
        val selectMultipleTextView = LayoutInflater.from(requireContext())
            .inflate(R.layout.menu_item, null) as TextView

        // Set initial text based on current state
        selectMultipleTextView.text = if (isSelectingMultiple) {
            getString(R.string.close_select_multiple)
        } else {
            ellipsisItems[0] // "Select Multiple"
        }

        selectMultipleTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_items))
        selectMultipleTextView.setPadding(50, 30, 50, 30)

        selectMultipleTextView.setOnClickListener {
            popupWindow.dismiss()

            // Toggle the select multiple mode
            toggleSelectMultipleMode(selectMultipleTextView)

            // Check if the text is "close_select_multiple" and make btnFilter visible
            if (selectMultipleTextView.text == getString(R.string.close_select_multiple)) {
                binding.btnFilter.visibility = View.VISIBLE
            }
        }

        linearLayout.addView(selectMultipleTextView)

        // Show the PopupWindow
        popupWindow.showAsDropDown(view)
    }

    // Method to properly handle selection changes
    override fun onItemSelectionChanged() {
        val selectedCount = adapter.getSelectedItems().size
        println("Debug: Selection changed. Selected count: $selectedCount")

        // Always show the delete button when in selection mode
        if (isSelectionMode) {
            binding.iconDeleteMultiple.visibility = View.VISIBLE
        }

        // Enter or exit selection mode based on selected items
        if (selectedCount > 0) {
            if (!isSelectionMode) {
                // Enter selection mode
                isSelectionMode = true
                binding.iconCancelMultiple.visibility = View.VISIBLE
                binding.iconDeleteMultiple.visibility = View.VISIBLE
                selectAllCheckbox.visibility = View.VISIBLE
            }
        }

        updateDeleteButtonVisibility() // Refresh delete button visibility
        updateItemsCount() // Optional, if item count needs updating
    }

    private fun toggleSelectMultipleMode(textView: TextView) {
        isSelectingMultiple = !isSelectingMultiple

        if (isSelectingMultiple) {
            // Reset search input and restore the original list
            binding.searchInput.text.clear()
            // Reset filters before initializing selection mode
            resetToOriginalList()
            appliedTypes = null // Clear any saved filters
            appliedColors = null // Clear any saved filters

            // Initialize selection mode
            adapter.initializeSelectMode()
            binding.iconDeleteMultiple.visibility = View.VISIBLE
            binding.iconCancelMultiple.visibility = View.VISIBLE
            binding.iconAdd.visibility = View.GONE
            selectAllCheckbox.visibility = View.VISIBLE
            selectAllCheckbox.isChecked = false
        } else {
            // Exit selection mode
            exitSelectMultipleMode()
        }

        textView.text = if (isSelectingMultiple) {
            getString(R.string.close_select_multiple)
        } else {
            resources.getStringArray(R.array.ellipsis_items)[0]
        }
    }

    fun deleteSelectedItems() {
        val selectedItemsToDelete = adapter.getSelectedItems()
        println("Debug: Attempting to delete ${selectedItemsToDelete.size} items")

        if (selectedItemsToDelete.isEmpty()) {
            showToast(requireContext(), "No items selected")
            return
        }

        // Show the confirmation dialog before proceeding with deletion
        showDeleteConfirmationDialog(selectedItemsToDelete)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun exitSelectMultipleMode() {
        isSelectionMode = false
        isSelectingMultiple = false
        selectAllCheckbox.visibility = View.GONE
        selectAllCheckbox.isChecked = false
        binding.iconDeleteMultiple.visibility = View.GONE
        binding.iconCancelMultiple.visibility = View.GONE
        binding.iconAdd.visibility = View.VISIBLE

        // Reset the state for ALL items, not just the sorted/filtered ones
        allItems.forEach { item ->
            item.isChecked = false
            item.checkedIconVisibility = View.GONE
            item.favoriteIconVisibility = View.VISIBLE
        }

        // Also apply the reset to the currently visible items
        sortedItems = allItems.toMutableList()
        adapter.updateItems(sortedItems)

        adapter.clearSelections()
    }

    private fun initializeViews(view: View) {
        selectAllCheckbox = view.findViewById(R.id.select_all_checkbox)

        selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            handleSelectAll(isChecked)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleSelectAll(isChecked: Boolean) {
        sortedItems.forEach { item ->
            item.isChecked = isChecked
            item.checkedIconResId = if (isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked
        }

        // Update the adapter's selected items
        if (isChecked) {
            adapter.setSelectedItems(sortedItems.toSet())
        } else {
            adapter.setSelectedItems(emptySet())
        }

        // Notify the adapter of changes
        adapter.notifyDataSetChanged()

        // Update delete icon visibility
        updateDeleteButtonVisibility()
    }

    private fun updateDeleteButtonVisibility() {
        // Use the adapter's method to check for selected items
        val hasSelectedItems = adapter.hasSelectedItems()

        println("Debug: Delete button visibility check - Has selected items: $hasSelectedItems")

        binding.iconDeleteMultiple.visibility = View.VISIBLE
    }

    // Method to update item count
    fun updateItemsCount() {
        val itemCount = sortedItems.size
        val dynamicTitle = resources.getQuantityString(R.plurals.items_count, itemCount, itemCount)
        binding.itemsCountText.text = dynamicTitle  // Update the TextView with the dynamic count
    }

    fun showDeleteConfirmationDialog(selectedItemsToDelete: Set<ClothingItem>) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_confirmation, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Get the TextView for the dialog message
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)

        // Change the text dynamically based on the selected items
        val itemCount = selectedItemsToDelete.size
        val confirmationMessage = if (itemCount == 1) {
            "Are you sure you want to delete this item?"
        } else {
            "Are you sure you want to delete these $itemCount items?"
        }

        // Update the message text
        messageTextView.text = confirmationMessage

        val deleteButton = dialogView.findViewById<ImageView>(R.id.btn_delete)
        val cancelButton = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        // Remove the default background to avoid unwanted outlines
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        deleteButton.setOnClickListener {
            // Perform delete action if user confirms
            confirmDeleteItems(selectedItemsToDelete)
            dialog.dismiss() // Close the dialog
        }

        cancelButton.setOnClickListener {
            dialog.dismiss() // Close the dialog without taking action
        }

        dialog.show()
    }

    private fun confirmDeleteItems(selectedItemsToDelete: Set<ClothingItem>) {
        // Remove items from the fragment's lists
        allItems = allItems.filterNot { item -> selectedItemsToDelete.contains(item) }
        sortedItems.removeAll(selectedItemsToDelete)

        // Perform the deletion in the database
        lifecycleScope.launch {
            // Assuming you have a method in your repository to delete items
            itemViewModel.deleteItems(selectedItemsToDelete.map { it.id }) // Pass the IDs of the items to delete
        }

        // Reset search input and restore the original list
        appliedTypes = null // Reset applied types
        appliedColors = null // Reset applied colors
        binding.searchInput.text.clear()
        resetToOriginalList()

        binding.iconAdd.visibility = View.VISIBLE

        // Exit selection mode after deletion
        exitSelectMultipleMode()

        // Update the RecyclerView to reflect the changes
        updateRecyclerView()

        // Show a Toast with the number of items deleted
        showToast(
            requireContext(),
            "${selectedItemsToDelete.size} item(s) deleted"
        )
    }

    private fun showFilterBottomSheet() {
        // Check if the dialog is already showing
        val existingDialog = parentFragmentManager.findFragmentByTag("FilterBottomSheetDialog") as? FilterBottomSheetDialog
        if (existingDialog != null && existingDialog.isVisible) {
            return // Do nothing if already visible
        }

        val bottomSheetDialog = FilterBottomSheetDialog(
            typeOptions = typeOptions,
            preselectedTypes = appliedTypes?.toList(),
            preselectedColors = appliedColors?.toList(),
            onApplyFilters = { types, colors ->
                appliedTypes = types?.toMutableList()
                appliedColors = colors?.toMutableList()
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
        allItems.forEach { item ->
            val closestColor = findClosestColor(item.color, colorOptions)
            println("${item.name} (${item.color}) -> $closestColor")
        }

        sortedItems = allItems.filter { item ->
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
        // Exit selection mode when navigating away from the fragment
        if (isSelectionMode) {
            exitSelectMultipleMode()
        }
        // Reset applied filters when navigating away from this fragment
        appliedTypes = null
        appliedColors = null
        resetToOriginalList() // Restore the original list of items
    }

    // onResume to preserve filters
    override fun onResume() {
        super.onResume()
        // Reset selection mode when resuming the fragment
        isSelectionMode = false
        isSelectingMultiple = false

        // Reset search input and filters
        binding.searchInput.text.clear() // Clear the search input
        appliedTypes = null // Reset applied types
        appliedColors = null // Reset applied colors
        resetToOriginalList() // Restore the original list of items

        // Observe the items from ViewModel if not already observing
        itemViewModel.items.observe(viewLifecycleOwner) { items ->
            allItems = items.map { convertToClothingItem(it) }

            // If we have active filters, apply them
            if (appliedTypes != null || appliedColors != null) {
                applyFilters(appliedTypes, appliedColors)
            } else {
                // Otherwise just show all items
                sortedItems = allItems.toMutableList()
                updateRecyclerView()
            }
        }
    }

    fun filterItems(query: String) {
        // Filter items based on the search query
        val filteredList = allItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||  // Search by name
                    item.type.contains(query, ignoreCase = true)    // Search by type
        }

        // Update the RecyclerView with the filtered list
        sortedItems = filteredList.toMutableList()
        updateRecyclerView()
        isViewingSearchResults = true  // Set the state to indicate search results are being viewed
    }

    // Method to reset search results and revert to the original list
    fun resetSearchResults() {
        binding.searchInput.text.clear()
        isViewingSearchResults = false
        resetToOriginalList()
    }

    fun resetToOriginalList() {
        sortedItems = allItems.toMutableList()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        if (sortedItems.isEmpty()) {
            showEmptyMessage()
        } else {
            hideEmptyMessage()
            adapter.updateItems(sortedItems)
        }
        updateItemsCount()  // Update the item count when the list is updated
    }

    private fun showEmptyMessage() {
        val fullMessage = getString(R.string.no_items_available)
        val start = fullMessage.indexOf("Add a new item?")
        val end = start + "Add a new item?".length

        val spannableString = SpannableString(fullMessage)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showAddItemFragment()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(requireContext(), R.color.color_items)
                ds.bgColor = ContextCompat.getColor(requireContext(), R.color.faded_pink)
            }
        }

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.emptyMessage.text = spannableString
        binding.emptyMessage.movementMethod = LinkMovementMethod.getInstance()
        binding.emptyMessage.visibility = View.VISIBLE
        binding.recyclerViewItems.visibility = View.GONE
    }

    private fun showAddItemFragment() {
        findNavController().navigate(R.id.action_itemsFragment_to_addItemFragment)
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.visibility = View.GONE
        binding.recyclerViewItems.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}