
package com.example.closets.ui.unused


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.closets.R
import com.example.closets.databinding.FragmentUnusedBinding
import androidx.navigation.fragment.findNavController


class UnusedFragment : Fragment() {

    private var _binding: FragmentUnusedBinding? = null
    val binding get() = _binding!!

    private var unusedItems: List<UnusedItem> = listOf()
    private var sortedItems: MutableList<UnusedItem> = mutableListOf()
    private var isDescImageVisible = false // Track visibility of desc_unused_image
    private var currentSortPosition = 0

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load and apply the slide-down animation to the unusedImage view
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        binding.unusedImage.startAnimation(slideDownAnimation)

        // Apply the slide-down animation to the dynamic title (items count)
        binding.unusedItemsCountText.startAnimation(slideDownAnimation)

        setStatusBarColor()

        val recyclerView = binding.unusedItemsRecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        unusedItems = listOf(
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs."),
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs."),
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs."),
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs.")
        )

        sortedItems = unusedItems.toMutableList()

        // Initialize the adapter with sorted items and set it to RecyclerView
        val adapter = UnusedItemsAdapter(sortedItems) { item ->
            // Add a delay before navigating
            val delayMillis = 150L

            // Use postDelayed to navigate after a delay
            when (item.name) {
                "Shorts" -> binding.unusedItemsRecyclerView.postDelayed({
                    findNavController().navigate(R.id.action_unusedFragment_to_itemInfoShortsFragment)
                }, delayMillis)
                "Dress" -> binding.unusedItemsRecyclerView.postDelayed({
                    findNavController().navigate(R.id.action_unusedFragment_to_itemInfoDressFragment)
                }, delayMillis)
                "Shoes" -> binding.unusedItemsRecyclerView.postDelayed({
                    findNavController().navigate(R.id.action_unusedFragment_to_itemInfoShoesFragment)
                }, delayMillis)
                "Cap" -> binding.unusedItemsRecyclerView.postDelayed({
                    findNavController().navigate(R.id.action_unusedFragment_to_itemInfoCapFragment)
                }, delayMillis)
                // Add more cases for different items as necessary
            }
        }

        recyclerView.adapter = adapter
        setupSortSpinner()

        // Set up toggle for desc_unused_image
        binding.info.setOnClickListener {
            toggleDescImageVisibility()
        }

        // Update the dynamic title initially
        updateDynamicTitle()
    }

    // Method to update the dynamic title with the item count
    private fun updateDynamicTitle() {
        val itemCount = sortedItems.size
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
                    1 -> sortByFavorites() // "Favorites" sorting
                    2 -> sortByDuration(oldestToRecent = false) // "Duration (Longest to Recent)"
                    3 -> sortByDuration(oldestToRecent = true) // "Duration (Recent to Longest)"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                currentSortPosition = 0
                isFiltered = false
            }
        }
    }

    private fun resetToOriginalList() {
        // Reset sortedItems to the original unusedItems
        sortedItems = unusedItems.toMutableList()
        updateRecyclerView()
    }

    fun sortByDuration(oldestToRecent: Boolean) {
        // Sort based on duration
        sortedItems = unusedItems.sortedWith(compareBy {
            val parts = it.duration.split(" ")
            when {
                parts[1].startsWith("yr") -> (parts[0].toIntOrNull() ?: 0) * 12 // Convert years to months
                parts[1].startsWith("mos") -> (parts[0].toIntOrNull() ?: 0) // Keep months as is
                else -> 0 // Default case
            }
        }).toMutableList()

        // If sorting from recent to oldest, reverse the sorted list
        if (!oldestToRecent) {
            sortedItems.reverse()
        }

        updateRecyclerView()
    }

    fun sortByFavorites() {
        // Sort based on favorite status
        sortedItems = unusedItems.sortedByDescending { it.isFavorite }.toMutableList()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        // Update the RecyclerView with sorted items
        (binding.unusedItemsRecyclerView.adapter as UnusedItemsAdapter).updateItems(sortedItems)

        // Update the dynamic title (item count) after updating the list
        updateDynamicTitle()
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_unused)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}