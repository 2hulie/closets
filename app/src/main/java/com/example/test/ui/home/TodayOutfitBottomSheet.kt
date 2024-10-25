package com.example.test.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R
import com.example.test.ui.OutfitItemAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TodayOutfitBottomSheet : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitItemAdapter: OutfitItemAdapter
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
            Item("Shirt", R.drawable.shirt),
            Item("Skirt", R.drawable.skirt)
        )

        outfitItemAdapter = OutfitItemAdapter(todaysOutfitList) { item ->
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
            parentFragment?.findNavController()?.navigate(R.id.action_navigation_home_to_currentItemFragment)
        }
    }

    private fun dismissAndNavigateToItemInfo(item: Item) {
        dismiss()
        val actionId = when (item.name) {
            "Shirt" -> R.id.action_todayOutfitBottomSheet_to_itemInfoShirtFragment
            "Skirt" -> R.id.action_todayOutfitBottomSheet_to_itemInfoSkirtFragment
            else -> null
        }
        actionId?.let { findNavController().navigate(it) }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setDimAmount(0.3f)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}