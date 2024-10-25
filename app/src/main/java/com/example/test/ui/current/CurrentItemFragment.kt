package com.example.test.ui.current

import android.annotation.SuppressLint
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
import androidx.navigation.fragment.findNavController // Import for navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.example.test.R
import com.example.test.databinding.FragmentCurrentItemBinding
import com.example.test.ui.home.TodayOutfitBottomSheet


class CurrentItemFragment : Fragment() {

    private var _binding: FragmentCurrentItemBinding? = null
    private val binding get() = _binding!!

    // Define all current items and sorted list
    private var allCurrentItems: List<CurrentItem> = listOf()
    private var sortedCurrentItems: MutableList<CurrentItem> = mutableListOf()

    // Adapter for RecyclerView
    private lateinit var adapter: CurrentItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentCurrentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_down)
        binding.currentItemsImage.startAnimation(slideDownAnimation)

        // Initialize RecyclerView with GridLayoutManager for 3 items per row
        binding.recyclerViewCurrentItems.layoutManager = GridLayoutManager(requireContext(), 3)

        // Initialize all current items
        allCurrentItems = listOf(
            CurrentItem(R.drawable.cap, "Other", false),
            CurrentItem(R.drawable.dress, "Dress", false),
            CurrentItem(R.drawable.shirt, "Top", true),
            CurrentItem(R.drawable.shorts, "Bottom", false),
            CurrentItem(R.drawable.shoes, "Shoes", false),
            CurrentItem(R.drawable.skirt, "Bottom", true)
        )

        // Set sortedCurrentItems to be a mutable list from allCurrentItems
        sortedCurrentItems = allCurrentItems.toMutableList()

        // Initialize the adapter with sorted items and set it to RecyclerView
        adapter = CurrentItemAdapter(sortedCurrentItems) { item ->
            // Handle item clicks - not needed for now
        }
        binding.recyclerViewCurrentItems.adapter = adapter

        // Set up click listener for the save icon
        binding.saveIcon.setOnClickListener {
            navigateToHomeAndShowBottomSheet()
        }

        // Setup Spinner for sorting
        setupSortSpinner()
    }

    private fun navigateToHomeAndShowBottomSheet() {
        // Navigate to HomeFragment
        findNavController().navigate(R.id.action_currentItemFragment_to_homeFragment)

        // Show the TodayOutfitBottomSheet after a slight delay
        // Use a post delay to allow the HomeFragment to fully appear before showing the bottom sheet
        binding.saveIcon.postDelayed({
            val bottomSheetFragment = TodayOutfitBottomSheet()
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }, 300) // Adjust the delay as needed
    }

    private fun setupSortSpinner() {
        val sortOptions = resources.getStringArray(R.array.filter_options)

        val spinnerAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, sortOptions) {
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

        binding.filterBySpinner.adapter = spinnerAdapter

        binding.filterBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> resetToOriginalList() // "None" - show all items
                    else -> sortByType(sortOptions[position]) // Sorting by selected type
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case when nothing is selected - none for now
            }
        }
    }

    private fun resetToOriginalList() {
        // Reset sortedCurrentItems to the original allCurrentItems list
        sortedCurrentItems = allCurrentItems.toMutableList()
        updateRecyclerView()
    }

    private fun sortByType(selectedType: String) {
        // Filter based on the selected type
        sortedCurrentItems = if (selectedType == "None") {
            allCurrentItems.toMutableList() // No filtering
        } else {
            allCurrentItems.filter { it.type == selectedType }.toMutableList() // Filter by type
        }
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
}