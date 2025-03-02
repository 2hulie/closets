package com.example.closets.ui.current

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.BaseItemFragment
import com.example.closets.R
import com.example.closets.SharedViewModel
import com.example.closets.databinding.FragmentCurrentItemBinding
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.entities.Item
import com.example.closets.ui.home.TodayOutfitBottomSheet
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CurrentItemFragment : BaseItemFragment() {

    private var _binding: FragmentCurrentItemBinding? = null
    override val binding: View
        get() = _binding!!.root

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var previouslySelectedItems: List<ClothingItem> = listOf()
    private val checkedStates = mutableMapOf<Int, Boolean>()
    private val selectedItems = mutableListOf<ClothingItem>()
    private var lastCheckedDate: String? = null
    private val checkedItemsPrefs = "CheckedItemsPrefs"
    private val lastCheckDateKey = "LastCheckDate"
    private val checkedItemsKey = "CheckedItems"
    private var originalCheckedItems: List<ClothingItem> = listOf()
    private var lastDisplayedDate: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var allCurrentItems: List<ClothingItem> = listOf()
    private var sortedCurrentItems: MutableList<ClothingItem> = mutableListOf()
    override lateinit var adapter: CurrentItemAdapter
    private val checkDateRunnable = object : Runnable {
        override fun run() {
            val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            if (lastDisplayedDate != currentDate) {
                Log.d("CurrentItemFragment", "Displayed date has changed from $lastDisplayedDate to $currentDate.")
                lastDisplayedDate = currentDate
                _binding!!.currentDate.text = currentDate
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun checkForDateChange() {
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        if (lastDisplayedDate != currentDate) {
            Log.d("CurrentItemFragment", "Displayed date has changed from $lastDisplayedDate to $currentDate.")
            lastDisplayedDate = currentDate
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        redirectBottomNavigation()
        _binding = FragmentCurrentItemBinding.inflate(inflater, container, false)
        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (binding as ViewGroup).addView(loadingView)
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]
        return binding
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingView?.visibility = View.VISIBLE
        _binding!!.recyclerViewCurrentItems.visibility = View.GONE
        _binding!!.recyclerViewCurrentItems.layoutManager = GridLayoutManager(requireContext(), 3)

        adapter = CurrentItemAdapter(sortedCurrentItems) { item ->
            val currentSelectedItems = sortedCurrentItems.filter { it.isChecked } // Recompute the selected items from the entire list
            sharedViewModel.setCheckedItems(currentSelectedItems) // Update the shared ViewModel with the current selected items.
            saveCheckedState(item.id, item.isChecked) // Save the checked state immediately when toggled.
            adapter.updateItems(sortedCurrentItems) // Refresh the adapter to reflect the change.
        }

        _binding!!.recyclerViewCurrentItems.adapter = adapter
        originalCheckedItems = adapter.getSelectedItems()
        loadSavedState()

        itemViewModel.items.observe(viewLifecycleOwner) { currentItems ->
            lifecycleScope.launch {
                allCurrentItems = currentItems.map { item ->
                    convertToClothingItem(item).apply {
                        isChecked = getSavedCheckedState(id)
                    }
                }

                sortedCurrentItems = allCurrentItems.toMutableList()
                originalCheckedItems = sortedCurrentItems.filter { it.isChecked }.map { it.copy() }
                loadingView?.visibility = View.GONE
                _binding!!.recyclerViewCurrentItems.visibility = View.VISIBLE

                if (sortedCurrentItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                    _binding!!.recyclerViewCurrentItems.visibility = View.VISIBLE
                }

                adapter.updateItems(sortedCurrentItems)
                updateSharedViewModelCheckedItems()
            }
        }

        val searchInput: EditText = _binding!!.searchInput
        val searchButton: ImageView = _binding!!.iconSearch

        _binding!!.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
        searchButton.isEnabled = false
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
                appliedTypes = null
                appliedColors = null
                filterItems(query) // Call the filter method if there is input
            } else {
                resetSearchResults()
            }
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchInput.text.toString()
                if (query.isNotEmpty()) {
                    filterItems(query) // Call the filter method if there is input
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
                } else {
                    resetSearchResults()
                }
                true
            } else {
                false
            }
        }

        setStatusBarColor()
        displayCurrentDate()

        _binding!!.iconResetOutfit.setOnClickListener {
            clearCheckedStates()
            showToast(requireContext(), "Checked items have been reset.")
        }

        _binding!!.saveIcon.setOnClickListener {
            navigateToHomeAndShowBottomSheet()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog()
            }
        })

        // Observe errors
        itemViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(requireContext(), it)
                itemViewModel.clearError()
            }
        }
    }

    private fun loadSavedState() {
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        lastCheckedDate = prefs.getString(lastCheckDateKey, null)

        // Load checked states into the checkedStates map
        val checkedItems = prefs.getStringSet(checkedItemsKey, emptySet()) ?: emptySet()
        checkedStates.clear()
        checkedItems.forEach { itemId ->
            checkedStates[itemId.toInt()] = true
        }

        // Update SharedViewModel with checked items
        updateSharedViewModelCheckedItems()
    }

    private fun updateSharedViewModelCheckedItems() {
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        val checkedItemIds = prefs.getStringSet(checkedItemsKey, emptySet()) ?: emptySet()

        if (checkedItemIds.isNotEmpty() && allCurrentItems.isNotEmpty()) {
            val checkedItems = allCurrentItems.filter { item ->
                checkedItemIds.contains(item.id.toString())
            }
            sharedViewModel.setCheckedItems(checkedItems)
        }
    }

    private fun getSavedCheckedState(itemId: Int): Boolean {
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        val checkedItems = prefs.getStringSet(checkedItemsKey, emptySet()) ?: emptySet()
        return checkedItems.contains(itemId.toString())
    }

    private fun saveCheckedState(itemId: Int, isChecked: Boolean) {
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        val checkedItems = prefs.getStringSet(checkedItemsKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (isChecked) {
            checkedItems.add(itemId.toString())
        } else {
            checkedItems.remove(itemId.toString())
        }

        prefs.edit().putStringSet(checkedItemsKey, checkedItems).apply()
    }

    private fun clearCheckedStates() {
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(checkedItemsKey)
            .apply()

        checkedStates.clear()
        selectedItems.clear()

        // Update all items to be unchecked
        sortedCurrentItems.forEach { item ->
            item.isChecked = false
        }
        allCurrentItems.forEach { item ->
            item.isChecked = false
        }

        // Refresh the adapter
        adapter.updateItems(sortedCurrentItems)
        sharedViewModel.setCheckedItems(emptyList())
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
            fragmentId = R.id.action_currentItemFragment_to_itemInfoFragment
        ).apply {
            isChecked = checkedStates[id] ?: false
        }
    }

    private fun navigateToHomeAndShowBottomSheet() {
        lifecycleScope.launch {
            try {
                showToast(requireContext(), "Changes saved!")
                findNavController().navigate(R.id.action_currentItemFragment_to_homeFragment)

                _binding!!.saveIcon.postDelayed({
                    val selectedItems = adapter.getSelectedItems()
                    val bottomSheetFragment = TodayOutfitBottomSheet(selectedItems)
                    bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
                }, 300)
            } catch (e: Exception) {
                showToast(requireContext(), "Error saving changes: ${e.message}")
            }
        }
    }

    private fun showFilterBottomSheet() {
        // Check if the dialog is already showing
        val existingDialog = parentFragmentManager.findFragmentByTag("FilterBottomSheetDialog") as? FilterBottomSheetDialog
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
        // Store currently selected items before filtering
        previouslySelectedItems = adapter.getSelectedItems()

        println("Color matches for current items:") // Debug logging to see color matches
        allCurrentItems.forEach { item ->
            val closestColor = findClosestColor(item.color, colorOptions)
            println("${item.name} (${item.color}) -> $closestColor")
        }

        val filteredList = allCurrentItems.filter { item ->
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

        val alreadyChecked = allCurrentItems.filter { it.isChecked && !filteredList.contains(it) }
        sortedCurrentItems = (filteredList + alreadyChecked).toMutableList()
        updateRecyclerView()

        // Reset search input
        _binding!!.searchInput.text.clear()
    }

    // override onSaveInstanceState to save filter state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("appliedTypes", ArrayList(appliedTypes ?: emptyList()))
        outState.putStringArrayList("appliedColors", ArrayList(appliedColors ?: emptyList()))
    }

    // override onCreate to restore filter state
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            appliedTypes = it.getStringArrayList("appliedTypes")?.toMutableList()
            appliedColors = it.getStringArrayList("appliedColors")?.toMutableList()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(checkDateRunnable)

        appliedTypes = null
        appliedColors = null
        resetToOriginalList() // Restore the original list of items
    }

    override fun onResume() {
        super.onResume()

        checkForDateChange()
        loadSavedState()
        sortedCurrentItems.forEach { item ->
            item.isChecked = getSavedCheckedState(item.id)
        }
        adapter.updateItems(sortedCurrentItems)
        handler.post(checkDateRunnable)

        // Get the current date
        val currentDate = displayCurrentDate() // Call the method to display and get the current date

        // Check if the displayed date has changed
        if (lastDisplayedDate != currentDate) {
            Log.d("CurrentItemFragment", "Displayed date has changed from $lastDisplayedDate to $currentDate.")
            lastDisplayedDate = currentDate
        }

        // Only reset if there are no applied filters
        if (appliedTypes == null && appliedColors == null) {
            resetToOriginalList()
        } else {
            applyFilters(appliedTypes, appliedColors)
        }
    }

    private fun filterItems(query: String) {
        // Get the items that match the search query.
        val filteredList = allCurrentItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||
                    item.type.contains(query, ignoreCase = true)
        }
        val alreadyChecked = allCurrentItems.filter { it.isChecked && !filteredList.contains(it) }
        sortedCurrentItems = (filteredList + alreadyChecked).toMutableList()
        updateRecyclerView()
        isViewingSearchResults = true
    }

    private fun resetSearchResults() {
        _binding!!.searchInput.text.clear()
        isViewingSearchResults = false
        resetToOriginalList()
    }

    override fun resetToOriginalList() {
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
        _binding!!.emptyMessage.visibility = View.VISIBLE
        _binding!!.emptyMessage.text = "No items found."
        _binding!!.recyclerViewCurrentItems.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        _binding!!.emptyMessage.visibility = View.GONE
        _binding!!.recyclerViewCurrentItems.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun redirectBottomNavigation() {
        val navController = findNavController()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)
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

        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener(null)
        }
    }

    private fun showDiscardChangesDialog() {
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_discard_changes, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        customView.findViewById<ImageView>(R.id.btn_discard).setOnClickListener {
            // Restore the checked states in your adapter:
            sortedCurrentItems.forEach { item ->
                item.isChecked = originalCheckedItems.any { orig -> orig.id == item.id }
            }
            adapter.updateItems(sortedCurrentItems)

            // Restore the shared view model
            sharedViewModel.setCheckedItems(originalCheckedItems)
            val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
            val restoredSet = originalCheckedItems.map { it.id.toString() }.toSet()
            prefs.edit().putStringSet(checkedItemsKey, restoredSet).apply()

            restoreBottomNavigation()
            findNavController().navigateUp()
            dialog.dismiss()
        }

        customView.findViewById<ImageView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun displayCurrentDate(): String {
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        _binding!!.currentDate.text = currentDate
        lastDisplayedDate = currentDate
        return currentDate
    }

    override fun updateItemsCount(count: Int) {
        // none
    }

    override fun clearSearchInput() {
        _binding!!.searchInput.text.clear()
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_items)
    }
}