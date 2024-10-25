package com.example.test.ui.favorites

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.test.R
import com.example.test.databinding.FragmentFavoritesBinding


class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private var favoriteItems: List<FavoriteItem> = listOf()
    private var sortedFavoriteItems: MutableList<FavoriteItem> = mutableListOf()
    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_down)
        binding.favoritesImage.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

        // Initialize RecyclerView with GridLayoutManager for 3 items per row
        binding.recyclerViewFavorites.layoutManager = GridLayoutManager(requireContext(), 3)

        // Initialize favorite items
        favoriteItems = listOf(
            FavoriteItem(R.drawable.shirt, "Top", true, "Shirt"),
            FavoriteItem(R.drawable.skirt, "Bottom", true, "Skirt"),
            FavoriteItem(R.drawable.cap, "Other", true, "Cap"),
            FavoriteItem(R.drawable.shoes, "Shoes", true, "Shoes")
        )

        // Set sortedFavoriteItems to be a mutable list from favoriteItems
        sortedFavoriteItems = favoriteItems.toMutableList()

        // Initialize the adapter with sorted items and set it to RecyclerView
        adapter = FavoritesAdapter(sortedFavoriteItems) { item ->
            // Add a delay before navigating
            val delayMillis = 150L

            // Use postDelayed to navigate after a delay
            when (item.name) {
                "Cap" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoCapFragment)
                }, delayMillis)
                "Shirt" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoShirtFragment)
                }, delayMillis)
                "Skirt" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoSkirtFragment)
                }, delayMillis)
                "Shoes" -> binding.recyclerViewFavorites.postDelayed({
                    findNavController().navigate(R.id.action_favoritesFragment_to_itemInfoShoesFragment)
                }, delayMillis)
            }
        }
        binding.recyclerViewFavorites.adapter = adapter

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
        sortedFavoriteItems = favoriteItems.toMutableList()
        updateRecyclerView()
    }

    private fun sortByType(selectedType: String) {
        sortedFavoriteItems = favoriteItems.filter { it.type == selectedType }.toMutableList()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        if (sortedFavoriteItems.isEmpty()) {
            showEmptyMessage()
        } else {
            hideEmptyMessage()
            adapter.updateItems(sortedFavoriteItems)
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
        binding.recyclerViewFavorites.visibility = View.GONE
    }

    private fun showAddItemFragment() {
        // Navigate to the Add Item Fragment
        findNavController().navigate(R.id.action_favoritesFragment_to_addItemFragment)
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.visibility = View.GONE
        binding.recyclerViewFavorites.visibility = View.VISIBLE
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_favorites)
        requireActivity().window.decorView.systemUiVisibility = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}