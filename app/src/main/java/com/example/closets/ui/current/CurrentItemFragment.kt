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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.closets.DateCheckWorker
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
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class CurrentItemFragment : Fragment() {

    private var _binding: FragmentCurrentItemBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var loadingView: View? = null
    private var previouslySelectedItems: List<ClothingItem> = listOf()
    private val checkedStates = mutableMapOf<Int, Boolean>()
    private val selectedItems = mutableListOf<ClothingItem>()
    private var lastCheckedDate: String? = null
    private val checkedItemsPrefs = "CheckedItemsPrefs"
    private val lastCheckDateKey = "LastCheckDate"
    private val checkedItemsKey = "CheckedItems"
    private var lastDisplayedDate: String? = null
    private val handler = Handler(Looper.getMainLooper())

    // Define all current items and sorted list
    private var allCurrentItems: List<ClothingItem> = listOf()
    private var sortedCurrentItems: MutableList<ClothingItem> = mutableListOf()

    // Tracks whether the fragment is showing search results
    private var isViewingSearchResults = false

    // Variables to hold the currently applied filters
    private var appliedTypes: List<String>? = null
    private var appliedColors: List<String>? = null

    // Adapter for RecyclerView
    private lateinit var adapter: CurrentItemAdapter
    private lateinit var itemViewModel: ItemViewModel

    private val checkDateRunnable = object : Runnable {
        override fun run() {
            checkForDateChange()
            handler.postDelayed(this, 60000) // Check every minute
        }
    }

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

    private fun checkForDateChange() {
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        if (lastDisplayedDate != currentDate) {
            Log.d("CurrentItemFragment", "Displayed date has changed from $lastDisplayedDate to $currentDate.")
            updateWornTimesAndLastWornDate()
            lastDisplayedDate = currentDate
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        scheduleDateCheckWorker()

        // Disable and redirect bottom navigation
        redirectBottomNavigation()

        // Inflate the layout using view binding
        _binding = FragmentCurrentItemBinding.inflate(inflater, container, false)

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
        binding.recyclerViewCurrentItems.visibility = View.GONE

        binding.recyclerViewCurrentItems.layoutManager = GridLayoutManager(requireContext(), 3)

        // Update adapter click listener
        adapter = CurrentItemAdapter(sortedCurrentItems) { item ->
            if (item.isChecked) {
                selectedItems.add(item)
            } else {
                selectedItems.remove(item)
            }

            // Update the shared ViewModel with the current selected items
            sharedViewModel.setCheckedItems(selectedItems) // Update the checked items in the ViewModel

            // Save checked state immediately when toggled
            saveCheckedState(item.id, item.isChecked)

            // Update the adapter to reflect the change
            adapter.updateItems(sortedCurrentItems)
        }

        binding.recyclerViewCurrentItems.adapter = adapter

        // Load saved checked states and last check date
        loadSavedState()

        // Check if day has changed and update stats if needed
        checkAndUpdateDailyStats()

        itemViewModel.items.observe(viewLifecycleOwner) { currentItems ->
            lifecycleScope.launch {
                allCurrentItems = currentItems.map { item ->
                    convertToClothingItem(item).apply {
                        isChecked = getSavedCheckedState(id)
                    }
                }

                sortedCurrentItems = allCurrentItems.toMutableList()

                // Hide loading view and show RecyclerView
                loadingView?.visibility = View.GONE
                binding.recyclerViewCurrentItems.visibility = View.VISIBLE

                if (sortedCurrentItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                    binding.recyclerViewCurrentItems.visibility = View.VISIBLE
                }

                adapter.updateItems(sortedCurrentItems)
            }
        }

        val searchInput: EditText = binding.searchInput
        val searchButton: ImageView = binding.iconSearch

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

        setStatusBarColor()

        // Set current date dynamically
        displayCurrentDate()

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

    private fun loadSavedState() {
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        lastCheckedDate = prefs.getString(lastCheckDateKey, null)

        // Load checked states into the checkedStates map
        val checkedItems = prefs.getStringSet(checkedItemsKey, emptySet()) ?: emptySet()
        checkedStates.clear()
        checkedItems.forEach { itemId ->
            checkedStates[itemId.toInt()] = true
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

    private fun checkAndUpdateDailyStats() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
        val lastCheckDate = prefs.getString(lastCheckDateKey, null)

        if (lastCheckDate != null && lastCheckDate != currentDate) {
            // Clear checked items for the new day
            sharedViewModel.clearCheckedItems()

            // Get the checked items before updating
            val checkedItemIds = prefs.getStringSet(checkedItemsKey, emptySet()) ?: emptySet()

            // Update worn times and last worn date for previously checked items
            lifecycleScope.launch {
                val itemsToUpdate = allCurrentItems.filter { item ->
                    checkedItemIds.contains(item.id.toString())
                }

                if (itemsToUpdate.isNotEmpty()) {
                    val formattedLastCheckDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastCheckDate)!!)

                    itemsToUpdate.forEach { item ->
                        item.wornTimes += 1
                        item.lastWornDate = formattedLastCheckDate
                    }

                    // Update items in the database
                    itemViewModel.updateItems(itemsToUpdate.map { it.toItem() })
                }

                // Clear checked states after updating
                clearCheckedStates()

                // Update last check date
                prefs.edit()
                    .putString(lastCheckDateKey, currentDate)
                    .apply()
            }
        } else if (lastCheckDate == null) {
            // First time running, just save current date
            prefs.edit()
                .putString(lastCheckDateKey, currentDate)
                .apply()
        }
    }

    private fun updateWornTimesAndLastWornDate() {
        lifecycleScope.launch {
            try {
                // Get the checked items from preferences
                val prefs = requireContext().getSharedPreferences(checkedItemsPrefs, Context.MODE_PRIVATE)
                val checkedItemIds = prefs.getStringSet(checkedItemsKey, emptySet()) ?: emptySet()

                if (checkedItemIds.isEmpty()) {
                    Log.d("CurrentItemFragment", "No items checked, skipping update")
                    return@launch
                }

                val itemsToUpdate = mutableListOf<ClothingItem>()
                val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())

                // Update only checked items
                allCurrentItems.forEach { item ->
                    if (checkedItemIds.contains(item.id.toString())) {
                        Log.d("CurrentItemFragment", "Updating item: ${item.name}, Previous Worn Times: ${item.wornTimes}")
                        item.wornTimes += 1
                        item.lastWornDate = currentDate // Use current date instead of yesterday
                        itemsToUpdate.add(item)
                        Log.d("CurrentItemFragment", "Updated item: ${item.name}, New Worn Times: ${item.wornTimes}, Last Worn Date: ${item.lastWornDate}")
                    }
                }

                // Save updated items to the database
                if (itemsToUpdate.isNotEmpty()) {
                    Log.d("CurrentItemFragment", "Saving ${itemsToUpdate.size} updated items to the database")
                    itemViewModel.updateItems(itemsToUpdate.map { it.toItem() })
                    showToast(requireContext(), "Successfully updated ${itemsToUpdate.size} items")
                }
            } catch (e: Exception) {
                Log.e("CurrentItemFragment", "Error updating items: ${e.message}", e)
                showToast(requireContext(), "Error updating items: ${e.message}")
            }
        }
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
    }

    // Add a new function to update single item
    private fun ItemViewModel.updateItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
            } catch (e: Exception) {
                _error.value = "Error updating item: ${e.message}"
            }
        }
    }

    private fun scheduleDateCheckWorker() {
        val workRequest = PeriodicWorkRequestBuilder<DateCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.MINUTES) // Set initial delay for quick testing
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "date_check_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
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
            // Apply saved checked state when converting
            isChecked = checkedStates[id] ?: false
        }
    }

    // Update navigateToHomeAndShowBottomSheet to not clear checked states immediately
    private fun navigateToHomeAndShowBottomSheet() {
        lifecycleScope.launch {
            try {
                showToast(requireContext(), "Changes saved!")
                findNavController().navigate(R.id.action_currentItemFragment_to_homeFragment)

                binding.saveIcon.postDelayed({
                    val selectedItems = adapter.getSelectedItems()
                    val bottomSheetFragment = TodayOutfitBottomSheet(selectedItems)
                    bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
                }, 300)
            } catch (e: Exception) {
                showToast(requireContext(), "Error saving changes: ${e.message}")
            }
        }
    }

    fun ClothingItem.toItem(): Item {
        return Item(
            id = this.id,
            imageUri = this.imageUri,
            name = this.name,
            type = this.type,
            color = this.color,
            wornTimes = this.wornTimes,
            lastWornDate = this.lastWornDate, // Ensure this is a non-nullable string
            isFavorite = this.isFavorite
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
        var hueDiff = abs(color1[0] - color2[0])
        if (hueDiff > 180) hueDiff = 360 - hueDiff
        hueDiff /= 180 // Normalize to [0,1]

        // Calculate saturation and value differences
        val satDiff = abs(color1[1] - color2[1])
        val valDiff = abs(color1[2] - color2[2])

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

    private fun applyFilters(types: List<String>?, colors: List<String>?) {
        // Store currently selected items before filtering
        previouslySelectedItems = adapter.getSelectedItems()

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
        handler.removeCallbacks(checkDateRunnable)

        // Reset applied filters when navigating away from this fragment
        appliedTypes = null
        appliedColors = null
        resetToOriginalList() // Restore the original list of items
    }

    override fun onResume() {
        super.onResume()

        checkForDateChange()
        checkAndUpdateDailyStats()

        handler.post(checkDateRunnable)

        // Get the current date
        val currentDate = displayCurrentDate() // Call the method to display and get the current date

        // Check if the displayed date has changed
        if (lastDisplayedDate != currentDate) {
            Log.d("CurrentItemFragment", "Displayed date has changed from $lastDisplayedDate to $currentDate.")

            // Update the last worn date and worn times for selected items
            updateWornTimesAndLastWornDate()

            // Update the last displayed date
            lastDisplayedDate = currentDate
        }

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
        isViewingSearchResults = true
    }

    // Method to reset search results and revert to the original list
    fun resetSearchResults() {
        binding.searchInput.text.clear()
        isViewingSearchResults = false
        resetToOriginalList()
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
    private fun displayCurrentDate(): String {
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        binding.currentDate.text = currentDate
        return currentDate // Return the current date for comparison
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_items)
    }
}