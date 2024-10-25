package com.example.test.ui.items


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
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.method.LinkMovementMethod
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.test.R
import com.example.test.databinding.FragmentItemsBinding


class ItemsFragment : Fragment() {

    private var _binding: FragmentItemsBinding? = null
    private val binding get() = _binding!!

    // Define all items and sorted list
    private var allItems: List<clothingItem> = listOf()
    private var sortedItems: MutableList<clothingItem> = mutableListOf()

    // Adapter for RecyclerView
    private lateinit var adapter: ItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_down)
        val itemsImageView = binding.itemsImage // Ensure this is accessed via binding
        itemsImageView.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

        // Initialize RecyclerView with GridLayoutManager for 3 items per row
        binding.recyclerViewItems.layoutManager = GridLayoutManager(requireContext(), 3)

        // Initialize all items (display all available items)
        allItems = listOf(
            clothingItem(R.drawable.cap, "Other", true, "Cap"),
            clothingItem(R.drawable.dress, "Dress", false, "Dress"),
            clothingItem(R.drawable.shirt, "Top", true, "Shirt"),
            clothingItem(R.drawable.shorts, "Bottom", false, "Shorts"),
            clothingItem(R.drawable.shoes, "Shoes", true, "Shoes"),
            clothingItem(R.drawable.skirt, "Bottom", true, "Skirt")
        )

        // Set sortedItems to be a mutable list from allItems
        sortedItems = allItems.toMutableList()

        // Initialize the adapter with sorted items and set it to RecyclerView
        adapter = ItemsAdapter(sortedItems, {
            // Handle Add Item click
            showAddItemFragment()
        }, { item ->
            // Add a delay before navigating
            val delayMillis = 150L

            // Use postDelayed to navigate after a delay
            when (item.name) { // Use 'name' for correct navigation
                "Cap" -> binding.recyclerViewItems.postDelayed({
                    findNavController().navigate(R.id.action_itemsFragment_to_itemInfoCapFragment)
                }, delayMillis)
                "Dress" -> binding.recyclerViewItems.postDelayed({
                    findNavController().navigate(R.id.action_itemsFragment_to_itemInfoDressFragment)
                }, delayMillis)
                "Shirt" -> binding.recyclerViewItems.postDelayed({
                    findNavController().navigate(R.id.action_itemsFragment_to_itemInfoShirtFragment)
                }, delayMillis)
                "Shorts" -> binding.recyclerViewItems.postDelayed({
                    findNavController().navigate(R.id.action_itemsFragment_to_itemInfoShortsFragment)
                }, delayMillis)
                "Skirt" -> binding.recyclerViewItems.postDelayed({
                    findNavController().navigate(R.id.action_itemsFragment_to_itemInfoSkirtFragment)
                }, delayMillis)
                "Shoes" -> binding.recyclerViewItems.postDelayed({
                    findNavController().navigate(R.id.action_itemsFragment_to_itemInfoShoesFragment)
                }, delayMillis)
                // Add more cases for different items as necessary
            }
        })
        binding.recyclerViewItems.adapter = adapter

        // Setup Spinner for sorting
        setupSortSpinner()
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
        // Reset sortedItems to the original allItems list
        sortedItems = allItems.toMutableList()
        updateRecyclerView()
    }

    private fun sortByType(selectedType: String) {
        sortedItems = allItems.filter { it.type == selectedType }.toMutableList()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        if (sortedItems.isEmpty()) {
            showEmptyMessage()
        } else {
            hideEmptyMessage()
            adapter.updateItems(sortedItems)
        }
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
        binding.recyclerViewItems.visibility = View.GONE
    }

    private fun showAddItemFragment() {
        // Navigate to the Add Item Fragment or show a dialog
        findNavController().navigate(R.id.action_itemsFragment_to_addItemFragment)
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.visibility = View.GONE
        binding.recyclerViewItems.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_items)
        requireActivity().window.decorView.systemUiVisibility = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
