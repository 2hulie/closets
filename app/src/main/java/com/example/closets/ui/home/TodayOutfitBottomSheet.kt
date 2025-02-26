package com.example.closets.ui.home

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.SharedViewModel
import com.example.closets.ui.items.ClothingItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TodayOutfitBottomSheet(private var checkedItems: List<ClothingItem>) : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitItemAdapter: TodayOutfitItemAdapter
    private lateinit var backButton: ImageView
    private lateinit var pencilIcon: ImageView
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_today_outfit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load checked items from SharedPreferences if none provided
        if (checkedItems.isEmpty()) {
            val prefs = requireContext().getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
            val checkedItemIds = prefs.getStringSet("CheckedItems", emptySet()) ?: emptySet()

            // Get items from SharedViewModel or database
            val sharedViewModel: SharedViewModel by activityViewModels()
            sharedViewModel.checkedItems.observe(viewLifecycleOwner) { items ->
                if (items.isNotEmpty()) {
                    checkedItems = items
                    outfitItemAdapter.updateItems(items)
                    updateUIState()
                }
            }
        }

        initializeUI(view)
        setUpRecyclerView()
        setClickListeners()
    }

    private fun initializeUI(view: View) {
        recyclerView = view.findViewById(R.id.todays_outfit_recycler_view)
        backButton = view.findViewById(R.id.icon_back)
        pencilIcon = view.findViewById(R.id.icon_pencil)
        emptyStateText = view.findViewById(R.id.empty_state_text)
    }

    private fun setUpRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with ${checkedItems.size} items")
        outfitItemAdapter = TodayOutfitItemAdapter(checkedItems) { item ->
            dismissAndNavigateToItemInfo(item)
        }

        recyclerView.apply {
            adapter = outfitItemAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)  // Optimization if items are fixed size
        }

        // Update the UI state based on the checked items
        updateUIState()
    }

    private fun updateUIState() {
        if (checkedItems.isEmpty()) {
            Log.d(TAG, "No items to display")
            recyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "Displaying ${checkedItems.size} items")
            recyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
        }
    }

    private fun setClickListeners() {
        backButton.setOnClickListener { dismiss() }

        pencilIcon.setOnClickListener {
            dismiss()
            parentFragment?.findNavController()
                ?.navigate(R.id.action_navigation_home_to_currentItemFragment)
        }
    }

    private fun dismissAndNavigateToItemInfo(homeItem: ClothingItem) {
        dismiss()
        // Create a Bundle to pass the item ID to the ItemInfoFragment
        val bundle = Bundle().apply {
            putInt("item_id", homeItem.id) // Pass the item ID
        }
        // Navigate to the ItemInfoFragment with the Bundle
        findNavController().navigate(R.id.action_todayOutfitBottomSheet_to_itemInfoFragment, bundle)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Dim the background and make it transparent
            setDimAmount(0.3f)
            setBackgroundDrawableResource(android.R.color.transparent)

            // Resolve the color from resources
            val todaysOutfitColor = resources.getColor(R.color.todays_outfit, null)

            // Set navigation bar background color
            this.navigationBarColor = todaysOutfitColor

            // Set system navigation buttons to dark (default)
            this.insetsController?.apply {
                // Clear the light navigation bar appearance for default (dark icons)
                setSystemBarsAppearance(
                    0, // Clear any light navigation bar settings
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
                // Clear the light status bar appearance for default (dark icons)
                setSystemBarsAppearance(
                    0, // Clear any light status bar settings
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
    }
}