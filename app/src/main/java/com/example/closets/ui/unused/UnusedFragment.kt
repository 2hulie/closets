package com.example.closets.ui.unused

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.R
import com.example.closets.databinding.FragmentUnusedBinding
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsFragment.Companion.showToast
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class UnusedFragment : Fragment() {

    private var _binding: FragmentUnusedBinding? = null
    val binding get() = _binding!!

    private lateinit var itemViewModel: ItemViewModel
    private var allUnusedItems: List<ClothingItem> = listOf()
    private var sortedUnusedItems: MutableList<ClothingItem> = mutableListOf()
    private lateinit var adapter: UnusedItemsAdapter
    private var isDescImageVisible = false // Track visibility of desc_unused_image
    private var currentSortPosition = 0
    private var loadingView: View? = null

    // track if the list is currently filtered
    private var isFiltered = false

    // method to check if filters are active
    fun hasActiveFilters(): Boolean {
        return isFiltered
    }

    // method to clear filters
    fun clearAllFilters() {
        if (isFiltered) {
            binding.sortBySpinner.setSelection(0)
            currentSortPosition = 0
            isFiltered = false
            resetToOriginalList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnusedBinding.inflate(inflater, container, false)

        // Inflate the loading view
        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (binding.root as ViewGroup).addView(loadingView) // Add loading view to the fragment's view

        // Initialize the ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setStatusBarColor()
        setupSortSpinner()
        updateItemsCount()

        // Show loading view initially
        loadingView?.visibility = View.VISIBLE
        binding.recyclerViewUnused.visibility = View.GONE

        val recyclerView = binding.recyclerViewUnused
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        // Observe the unused items from the ViewModel
        itemViewModel.items.observe(viewLifecycleOwner) { unusedItems ->
            lifecycleScope.launch {
                // Process items in the background
                allUnusedItems = unusedItems.map { convertToClothingItem(it) }
                    .filter { item -> hasBeenUnusedForAtLeastThreeMonths(item.lastWornDate) } // Filter items

                sortedUnusedItems = allUnusedItems.toMutableList()

                // Hide loading view and show RecyclerView
                loadingView?.visibility = View.GONE
                binding.recyclerViewUnused.visibility = View.VISIBLE

                Log.d("UnusedFragment", "Filtered Items fetched: ${sortedUnusedItems.size}")
                Log.d("UnusedFragment", "Filtered Items: $sortedUnusedItems")

                if (sortedUnusedItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                }

                updateRecyclerView()
                updateItemsCount()
            }
        }

        // Initialize the adapter
        adapter = UnusedItemsAdapter(
            sortedUnusedItems.map { clothingItem ->
                UnusedItem(
                    clothingItem = clothingItem,
                    duration = calculateDuration(clothingItem.lastWornDate),
                    isUnused = calculateDuration(clothingItem.lastWornDate).contains("Unused")
                )
            }
        ) { item ->
            val bundle = Bundle().apply {
                putInt("item_id", item.id)
            }
            findNavController().navigate(R.id.action_unusedFragment_to_itemInfoFragment, bundle)
        }

        binding.recyclerViewUnused.adapter = adapter

        // Set up toggle for desc_unused_image
        binding.info.setOnClickListener {
            toggleDescImageVisibility()
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
            isFavorite = item.isFavorite,
            fragmentId = R.id.action_unusedFragment_to_itemInfoFragment,
        )
    }

    // Method to update the dynamic title with the item count
    private fun updateItemsCount() {
        val itemCount = sortedUnusedItems.size
        val dynamicTitle = resources.getQuantityString(R.plurals.unused_items_count, itemCount, itemCount)
        binding.unusedItemsCountText.text = dynamicTitle
    }

    // Method to toggle visibility of desc_unused_image
    private fun toggleDescImageVisibility() {
        isDescImageVisible = !isDescImageVisible
        binding.descUnusedImage.visibility = if (isDescImageVisible) View.VISIBLE else View.GONE
    }

    private fun setupSortSpinner() {
        // Array of sort options from resources
        val options = resources.getStringArray(R.array.sort_options)

        // Create a custom ArrayAdapter using the default spinner layout
        val spinnerAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, options) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(ContextCompat.getColor(context, R.color.base_text))
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(ContextCompat.getColor(context, R.color.base_text))
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Use default dropdown layout
        }

        // Set the adapter to the Spinner
        binding.sortBySpinner.adapter = spinnerAdapter

        // Handle Spinner item selections
        binding.sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSortPosition = position
                isFiltered = position != 0 // update filter state

                when (position) {
                    0 -> resetToOriginalList() // "None" selection
                    1 -> sortByDuration(oldestToRecent = false) // "Duration (Longest to Recent)"
                    2 -> sortByDuration(oldestToRecent = true) // "Duration (Recent to Longest)"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                currentSortPosition = 0
                isFiltered = false
            }
        }
    }


    // Function to check if an item has been unused for at least three months
    private fun hasBeenUnusedForAtLeastThreeMonths(lastWornDate: String): Boolean {
        if (lastWornDate.isEmpty() || lastWornDate == "N/A") {
            Log.d("UnusedFragment", "Skipping item - No valid last worn date.")
            return false
        }

        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return try {
            val lastWorn = formatter.parse(lastWornDate) ?: return false
            val today = Date()
            val diffInMillis = today.time - lastWorn.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val diffInMonths = diffInDays / 30 // Approximate months

            Log.d("UnusedFragment", "Item Last Worn: $lastWornDate -> Days Since Last Worn: $diffInDays, Months Since Last Worn: $diffInMonths")

            diffInMonths >= 3 // Only return true if it's been at least 3 months
        } catch (e: ParseException) {
            Log.e("UnusedFragment", "Error parsing date: $lastWornDate", e)
            false // Ignore invalid dates
        }
    }


    private fun calculateDuration(lastWornDate: String): String {
        if (lastWornDate.isEmpty()) return "N/A" // Handle empty case

        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        return try {
            val lastWorn = formatter.parse(lastWornDate)
            val today = Date()
            val diff = today.time - (lastWorn?.time ?: 0)
            val months = TimeUnit.MILLISECONDS.toDays(diff) / 30

            when {
                months >= 12 -> "${months / 12} yrs."
                else -> "$months mos."
            }
        } catch (e: ParseException) {
            Log.e("UnusedFragment", "Error parsing lastWornDate: $lastWornDate", e)
            "Unknown"
        }
    }

    private fun resetToOriginalList() {
        // Reset sortedItems to the original unusedItems
        sortedUnusedItems = allUnusedItems.toMutableList() // Assuming you have a way to get the original list
        updateRecyclerView()
    }

    private fun sortByDuration(oldestToRecent: Boolean) {
        sortedUnusedItems.sortWith(compareBy {
            val duration = calculateDuration(it.lastWornDate) // Calculate duration dynamically
            val parts = duration.split(" ")
            when {
                parts[1].startsWith("yr") -> (parts[0].toIntOrNull() ?: 0) * 12 // Convert years to months
                parts[1].startsWith("mos") -> (parts[0].toIntOrNull() ?: 0) // Keep months as is
                else -> 0 // Default case
            }
        })

        if (!oldestToRecent) {
            sortedUnusedItems.reverse()
        }

        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        val unusedItemsList = sortedUnusedItems.map { clothingItem ->
            val duration = calculateDuration(clothingItem.lastWornDate) // Recalculate duration
            val isUnused = duration.contains("Unused") // Determine if it's unused

            UnusedItem(
                clothingItem = clothingItem,
                duration = duration,
                isUnused = isUnused
            )
        }

        // Update the adapter with the correct type
        adapter.updateItems(unusedItemsList)

        updateItemsCount()
    }

    @SuppressLint("SetTextI18n")
    private fun showEmptyMessage() {
        binding.emptyMessage.visibility = View.VISIBLE
        binding.emptyMessage.text = "No items found."
        binding.recyclerViewUnused.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.visibility = View.GONE
        binding.recyclerViewUnused.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_unused)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}