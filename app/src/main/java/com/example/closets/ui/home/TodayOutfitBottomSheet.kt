package com.example.closets.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TodayOutfitBottomSheet : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitItemAdapter: TodayOutfitItemAdapter
    private lateinit var backButton: ImageView
    private lateinit var pencilIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_today_outfit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI(view)
        setUpRecyclerView()
        setClickListeners()
    }

    private fun initializeUI(view: View) {
        recyclerView = view.findViewById(R.id.todays_outfit_recycler_view)
        backButton = view.findViewById(R.id.icon_back)
        pencilIcon = view.findViewById(R.id.icon_pencil)
    }

    private fun setUpRecyclerView() {
        val todaysOutfitList = listOf(
            HomeItem("Cap", R.drawable.cap),
            HomeItem("Skirt", R.drawable.skirt)
        )

        outfitItemAdapter = TodayOutfitItemAdapter(todaysOutfitList) { item ->
            dismissAndNavigateToItemInfo(item)
        }

        recyclerView.apply {
            adapter = outfitItemAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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

    private fun dismissAndNavigateToItemInfo(homeItem: HomeItem) {
        dismiss()
        val actionId = when (homeItem.name) {
            "Cap" -> R.id.action_todayOutfitBottomSheet_to_itemInfoCapFragment
            "Skirt" -> R.id.action_todayOutfitBottomSheet_to_itemInfoSkirtFragment
            else -> null
        }
        actionId?.let { findNavController().navigate(it) }
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