package com.example.closets.ui.unused

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.BaseItemFragment
import com.example.closets.R
import com.example.closets.databinding.FragmentUnusedBinding
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class UnusedFragment : BaseItemFragment() {

    private var _binding: FragmentUnusedBinding? = null
    override val binding: View
        get() = _binding!!.root

    private var allUnusedItems: List<ClothingItem> = listOf()
    private var sortedUnusedItems: MutableList<ClothingItem> = mutableListOf()

    override lateinit var adapter: UnusedItemsAdapter
    private var isDescImageVisible = false
    private var currentSortPosition = 0
    private var dataLoaded = false

    // track if the list is currently filtered
    private var isFiltered = false

    // method to check if filters are active
    override fun hasActiveFilters(): Boolean {
        return isFiltered
    }

    // method to clear filters
    override fun clearAllFilters() {
        if (isFiltered) {
            _binding!!.sortBySpinner.setSelection(0)
            currentSortPosition = 0
            isFiltered = false
            resetToOriginalList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val trace = FirebasePerformance.getInstance().newTrace("unusedFragment_onCreateView")
        trace.start()

        _binding = FragmentUnusedBinding.inflate(inflater, container, false)
        initializeViewModel(requireContext())

        trace.stop()
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trace = FirebasePerformance.getInstance().newTrace("unusedFragment_onViewCreated")
        trace.start()

        setStatusBarColor()
        setupSortSpinner()
        updateItemsCount(sortedUnusedItems.size)

        _binding!!.recyclerViewUnused.visibility = View.GONE

        val recyclerView = _binding!!.recyclerViewUnused
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        itemViewModel.items.observe(viewLifecycleOwner) { unusedItems ->
            lifecycleScope.launch {
                // Process items in the background
                allUnusedItems = unusedItems.map { convertToClothingItem(it) }
                    .filter { item -> hasBeenUnusedForAtLeastThreeMonths(item.lastWornDate) } // Filter items

                sortedUnusedItems = allUnusedItems.toMutableList()
                dataLoaded = true
                _binding!!.recyclerViewUnused.visibility = View.VISIBLE

                Log.d("UnusedFragment", "Filtered Items fetched: ${sortedUnusedItems.size}")
                Log.d("UnusedFragment", "Filtered Items: $sortedUnusedItems")

                if (sortedUnusedItems.isEmpty()) {
                    showEmptyMessage()
                } else {
                    hideEmptyMessage()
                }

                updateRecyclerView()
                updateItemsCount(sortedUnusedItems.size)
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

        _binding!!.recyclerViewUnused.adapter = adapter
        startSlideDownAnimation(_binding!!.unusedImage, _binding!!.unusedItemsCountText)

        _binding!!.info.setOnClickListener {
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
            fragmentId = R.id.action_unusedFragment_to_itemInfoFragment,
        )
    }

    override fun updateItemsCount(count: Int) {
        val dynamicTitle = resources.getQuantityString(R.plurals.unused_items_count, count, count)
        _binding!!.unusedItemsCountText.text = dynamicTitle
    }

    // Method to toggle visibility of desc_unused_image
    private fun toggleDescImageVisibility() {
        isDescImageVisible = !isDescImageVisible
        _binding!!.descUnusedImage.visibility = if (isDescImageVisible) View.VISIBLE else View.GONE
    }

    private fun setupSortSpinner() {
        val options = resources.getStringArray(R.array.sort_options)
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
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Set the adapter to the Spinner
        _binding!!.sortBySpinner.adapter = spinnerAdapter

        // Handle Spinner item selections
        _binding!!.sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

    private fun hasBeenUnusedForAtLeastThreeMonths(lastWornDate: String): Boolean {
        if (lastWornDate.isEmpty() || lastWornDate == "N/A") {
            return true // "N/A" as longest unused
        }

        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return try {
            val lastWorn = formatter.parse(lastWornDate) ?: return true
            val today = Date()
            val diffInMillis = today.time - lastWorn.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val diffInMonths = diffInDays / 30

            diffInMonths >= 3
        } catch (e: ParseException) {
            Log.e("UnusedFragment", "Error parsing date: $lastWornDate", e)
            true // default to unused if there's an error
        }
    }

    private fun calculateDuration(lastWornDate: String): String {
        if (lastWornDate.isEmpty() || lastWornDate == "N/A") return "Unused"

        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        return try {
            val lastWorn = formatter.parse(lastWornDate)
            val today = Date()
            val diff = today.time - (lastWorn?.time ?: 0)
            val months = TimeUnit.MILLISECONDS.toDays(diff) / 30

            when {
                months >= 12 -> {
                    val years = months / 12
                    if (years == 1L) "1 yr." else "$years yrs."
                }
                else -> "$months mos."
            }
        } catch (e: ParseException) {
            Log.e("UnusedFragment", "Error parsing lastWornDate: $lastWornDate", e)
            "Never Worn"
        }
    }

    override fun resetToOriginalList() {
        // Reset sortedItems to the original unusedItems
        sortedUnusedItems = allUnusedItems.toMutableList()
        updateRecyclerView()
    }

    private fun sortByDuration(oldestToRecent: Boolean) {
        sortedUnusedItems.sortWith(compareBy {
            val duration = calculateDuration(it.lastWornDate)

            when {
                duration == "Unused" -> Int.MAX_VALUE
                duration.contains("yr") -> duration.split(" ")[0].toInt() * 12
                duration.contains("mos") -> duration.split(" ")[0].toInt()
                else -> 0
            }
        })

        if (!oldestToRecent) {
            sortedUnusedItems.reverse()
        }

        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        val unusedItemsList = sortedUnusedItems.map { clothingItem ->
            val duration = calculateDuration(clothingItem.lastWornDate) // recalculate duration
            val isUnused = duration.contains("Unused") // determine if it's unused

            UnusedItem(
                clothingItem = clothingItem,
                duration = duration,
                isUnused = isUnused
            )
        }

        // Update the adapter with the correct type
        adapter.updateItems(unusedItemsList)
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        _binding!!.recyclerViewUnused.startAnimation(fadeInAnimation)
        updateItemsCount(sortedUnusedItems.size)
    }

    @SuppressLint("SetTextI18n")
    private fun showEmptyMessage() {
        _binding!!.emptyMessage.visibility = View.VISIBLE
        _binding!!.emptyMessage.text = "No items found."
        _binding!!.recyclerViewUnused.visibility = View.GONE
    }

    private fun hideEmptyMessage() {
        _binding!!.emptyMessage.visibility = View.GONE
        _binding!!.recyclerViewUnused.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_unused)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun clearSearchInput() {
        // no search input in UnusedFragment
    }
}