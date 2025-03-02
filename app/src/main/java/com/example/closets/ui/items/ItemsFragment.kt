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
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.BaseItemFragment
import com.example.closets.R
import com.example.closets.databinding.FragmentItemsBinding
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.entities.Item
import kotlinx.coroutines.launch

class ItemsFragment : BaseItemFragment(), ItemsAdapter.SelectionCallback {

    private var _binding: FragmentItemsBinding? = null
    override val binding: View
        get() = _binding!!.root

    var allItems: List<ClothingItem> = listOf()
    var sortedItems: MutableList<ClothingItem> = mutableListOf()

    private lateinit var selectAllCheckbox: CheckBox
    var isSelectingMultiple = false
    private var isSelectionMode = false

    override lateinit var adapter: ItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemsBinding.inflate(inflater, container, false)

        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (binding as ViewGroup).addView(loadingView)

        initializeViewModel(requireContext())

        return binding
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingView?.visibility = View.VISIBLE
        _binding!!.recyclerViewItems.visibility = View.GONE
        _binding!!.recyclerViewItems.layoutManager = GridLayoutManager(requireContext(), 3)

        itemViewModel.items.observe(viewLifecycleOwner) { items ->
            lifecycleScope.launch {
                allItems = items.map { convertToClothingItem(it) }
                sortedItems = allItems.toMutableList()

                loadingView?.visibility = View.GONE
                _binding!!.recyclerViewItems.visibility = View.VISIBLE

                Log.d("ItemsFragment", "Items fetched: ${sortedItems.size}")
                Log.d("ItemsFragment", "Items: $sortedItems")

                adapter.updateItems(sortedItems) // Update the adapter
                updateItemsCount() // Update the item count
            }
        }

        initializeViews(view)

        _binding!!.iconAdd.setOnClickListener {
            val currentCount = itemViewModel.items.value?.size ?: 0
            if (currentCount >= 50) {
                showToast(requireContext(), "Items full, please delete some items.")
            } else {
                showAddItemFragment()
            }
        }

        adapter = ItemsAdapter(
            sortedItems,
            { item ->
                // bundle to pass the item ID
                val bundle = Bundle().apply {
                    putInt("item_id", item.id) // Pass the item ID
                }
                findNavController().navigate(R.id.action_itemsFragment_to_itemInfoFragment, bundle)
            },
            this,
            itemViewModel
        )

        _binding!!.recyclerViewItems.adapter = adapter

        _binding!!.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }

        val ellipsisIcon: ImageView = view.findViewById(R.id.ellipsis_icon)

        ellipsisIcon.setOnClickListener {
            showDropdown(it)  // Pass the view to showDropdown to anchor the menu
        }

        selectAllCheckbox = view.findViewById(R.id.select_all_checkbox)

        _binding!!.selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Select all items
                adapter.selectAllItems()
            } else {
                // Clear all selections
                adapter.clearSelections()
            }

            _binding!!.iconDeleteMultiple.visibility =
                View.VISIBLE // delete button is still visible in clear selection

            updateDeleteButtonVisibility() // Refresh delete button (if needed)
            updateItemsCount() // Update the count dynamically
        }

        val deleteIcon: ImageView = _binding!!.iconDeleteMultiple
        val cancelIcon: ImageView = _binding!!.iconCancelMultiple

        // Hide these icons by default
        deleteIcon.visibility = View.GONE
        cancelIcon.visibility = View.GONE

        // Handle cancel action
        cancelIcon.setOnClickListener {
            // Exit select multiple mode
            exitSelectMultipleMode()
            _binding!!.iconAdd.visibility = View.VISIBLE
            // Reset search input and restore the original list
            _binding!!.searchInput.text.clear()
            appliedTypes = null // Reset applied types
            appliedColors = null // Reset applied colors
            resetToOriginalList()
        }

        deleteIcon.setOnClickListener {
            // Delete selected items
            deleteSelectedItems()
        }

        setStatusBarColor()

        val searchInput: EditText = _binding!!.searchInput
        val searchButton: ImageView = _binding!!.iconSearch
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Enable the search button if there is input, otherwise disable it
                searchButton.isEnabled = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

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
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            ) {
                val query = searchInput.text.toString()
                if (query.isNotEmpty()) {
                    filterItems(query) // Call the filter method if there is input
                    // Optionally close the keyboard
                    val imm =
                        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
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
        return ClothingItem(
            id = item.id,
            imageUri = item.imageUri,
            name = item.name,
            type = item.type,
            color = item.color,
            wornTimes = item.wornTimes,
            lastWornDate = item.lastWornDate,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment,
            isFavorite = item.isFavorite
        )
    }

    @SuppressLint("ResourceType", "InflateParams")
    private fun showDropdown(view: View) {
        val ellipsisItems = resources.getStringArray(R.array.ellipsis_items)
        val linearLayout = LinearLayout(requireContext())
        linearLayout.orientation = LinearLayout.VERTICAL
        val popupWindow = PopupWindow(
            linearLayout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.isFocusable = true

        val selectMultipleTextView = LayoutInflater.from(requireContext())
            .inflate(R.layout.menu_item, null) as TextView

        // Set initial text based on current state
        selectMultipleTextView.text = if (isSelectingMultiple) {
            getString(R.string.close_select_multiple)
        } else {
            ellipsisItems[0] // "Select Multiple"
        }

        selectMultipleTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.color_items
            )
        )
        selectMultipleTextView.setPadding(50, 30, 50, 30)

        selectMultipleTextView.setOnClickListener {
            popupWindow.dismiss()

            // Toggle the select multiple mode
            toggleSelectMultipleMode(selectMultipleTextView)

            // Check if the text is "close_select_multiple" and make btnFilter visible
            if (selectMultipleTextView.text == getString(R.string.close_select_multiple)) {
                _binding!!.btnFilter.visibility = View.VISIBLE
            }
        }

        linearLayout.addView(selectMultipleTextView)

        // Show the PopupWindow
        popupWindow.showAsDropDown(view)
    }

    override fun updateItemsCount(count: Int) {
        _binding!!.itemsCountText.text =
            resources.getQuantityString(R.plurals.items_count, count, count)
    }

    override fun clearSearchInput() {
        _binding!!.searchInput.text.clear()
    }

    override fun onItemSelectionChanged() {
        val selectedCount = adapter.getSelectedItems().size
        println("Debug: Selection changed. Selected count: $selectedCount")

        // Always show the delete button when in selection mode
        if (isSelectionMode) {
            _binding!!.iconDeleteMultiple.visibility = View.VISIBLE
        }

        // Enter or exit selection mode based on selected items
        if (selectedCount > 0) {
            if (!isSelectionMode) {
                // Enter selection mode
                isSelectionMode = true
                _binding!!.iconCancelMultiple.visibility = View.VISIBLE
                _binding!!.iconDeleteMultiple.visibility = View.VISIBLE
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
            _binding!!.searchInput.text.clear()
            // Reset filters before initializing selection mode
            resetToOriginalList()
            appliedTypes = null // Clear any saved filters
            appliedColors = null // Clear any saved filters

            // Initialize selection mode
            adapter.initializeSelectMode()
            _binding!!.iconDeleteMultiple.visibility = View.VISIBLE
            _binding!!.iconCancelMultiple.visibility = View.VISIBLE
            _binding!!.iconAdd.visibility = View.GONE
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

    private fun deleteSelectedItems() {
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
        _binding!!.iconDeleteMultiple.visibility = View.GONE
        _binding!!.iconCancelMultiple.visibility = View.GONE
        _binding!!.iconAdd.visibility = View.VISIBLE

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
            item.checkedIconResId =
                if (isChecked) R.drawable.icon_checked else R.drawable.icon_unchecked
        }

        if (isChecked) {
            adapter.setSelectedItems(sortedItems.toSet())
        } else {
            adapter.setSelectedItems(emptySet())
        }

        adapter.notifyDataSetChanged()
        updateDeleteButtonVisibility()
    }

    private fun updateDeleteButtonVisibility() {
        val hasSelectedItems = adapter.hasSelectedItems()
        println("Debug: Delete button visibility check - Has selected items: $hasSelectedItems")
        _binding!!.iconDeleteMultiple.visibility = View.VISIBLE
    }

    // method to update item count
    private fun updateItemsCount() {
        val itemCount = sortedItems.size
        val dynamicTitle = resources.getQuantityString(R.plurals.items_count, itemCount, itemCount)
        _binding!!.itemsCountText.text = dynamicTitle  // Update the TextView with the dynamic count
    }

    private fun showDeleteConfirmationDialog(selectedItemsToDelete: Set<ClothingItem>) {
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
        _binding!!.searchInput.text.clear()
        resetToOriginalList()

        _binding!!.iconAdd.visibility = View.VISIBLE

        exitSelectMultipleMode()
        updateRecyclerView()
        showToast(
            requireContext(),
            "${selectedItemsToDelete.size} item(s) deleted"
        )
    }

    private fun showFilterBottomSheet() {
        // Check if the dialog is already showing
        val existingDialog =
            parentFragmentManager.findFragmentByTag("FilterBottomSheetDialog") as? FilterBottomSheetDialog
        if (existingDialog != null && existingDialog.isVisible) {
            return // Do nothing if already visible
        }

        val bottomSheetDialog = FilterBottomSheetDialog(
            typeOptions = typeOptions,
            colorOptions = colorOptions,
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

    private fun applyFilters(types: List<String>?, colors: List<String>?) {
        // Debug logging to see color matches
        println("Color matches for current items:")
        allItems.forEach { item ->
            val closestColor = findClosestColor(item.color, colorOptions)
            println("${item.name} (${item.color}) -> $closestColor")
        }

        val filteredList = allItems.filter { item ->
            // Check if item matches type filter
            val matchesType =
                types.isNullOrEmpty() || types.contains("None") || types.contains(item.type)

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

        val alreadyChecked = allItems.filter { it.isChecked && !filteredList.contains(it) }
        sortedItems = (filteredList + alreadyChecked).toMutableList()
        updateRecyclerView()

        // Reset search input
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
        _binding!!.searchInput.text.clear() // Clear the search input
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

    private fun filterItems(query: String) {
        // Filter items based on the search query
        val filteredList = allItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||  // Search by name
                    item.type.contains(query, ignoreCase = true)    // Search by type
        }

        val alreadyChecked = allItems.filter { it.isChecked && !filteredList.contains(it) }
        sortedItems = (filteredList + alreadyChecked).toMutableList()
        updateRecyclerView()
        isViewingSearchResults = true
    }

    // Method to reset search results and revert to the original list
    fun resetSearchResults() {
        _binding!!.searchInput.text.clear()
        isViewingSearchResults = false
        resetToOriginalList()
    }

    override fun resetToOriginalList() {
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
        _binding!!.emptyMessage.text = spannableString
        _binding!!.emptyMessage.movementMethod = LinkMovementMethod.getInstance()
        _binding!!.emptyMessage.visibility = View.VISIBLE
        _binding!!.recyclerViewItems.visibility = View.GONE
    }

    private fun showAddItemFragment() {
        findNavController().navigate(R.id.action_itemsFragment_to_addItemFragment)
    }

    private fun hideEmptyMessage() {
        _binding!!.emptyMessage.visibility = View.GONE
        _binding!!.recyclerViewItems.visibility = View.VISIBLE
    }

    @Suppress("DEPRECATION")
    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.lbl_items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}