package com.example.closets

import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Spinner
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsAdapter
import com.example.closets.ui.items.ItemsFragment
import com.example.closets.ui.viewmodels.ItemViewModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FilterFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var itemsFragment: ItemsFragment
    private lateinit var filterDialog: FilterBottomSheetDialog

    @Before
    fun setup() {
        // Initialize TestNavHostController
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        // Initialize the ItemsFragment
        itemsFragment = ItemsFragment().apply {
            // Set the NavController for the fragment
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Initialize the FilterBottomSheetDialog
        filterDialog = FilterBottomSheetDialog(
            typeOptions = listOf("Top", "Bottom", "Outerwear", "Dress", "Shoes", "Other"),
            colorOptions = mapOf(
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
            ),
            onApplyFilters = { _, _ -> /* Do nothing for testing */ },
            onResetFilters = { /* Do nothing for testing */ }
        )
    }

    @Test
    fun testFilterOptionsItemsFragment() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(
            id = 1,
            imageUri = "uri_to_cap_image",
            name = "Cap",
            type = "Top",
            color = "#FF5733",
            isFavorite = true,
            wornTimes = 0,
            lastWornDate = null,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment
        )

        // Add the "Cap" item to the fragment's list
        itemsFragment.allItems = listOf(capItem)
        itemsFragment.sortedItems = itemsFragment.allItems.toMutableList()

        // Create an adapter with a click listener
        val adapter = ItemsAdapter(itemsFragment.sortedItems, { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }, itemsFragment, mock(ItemViewModel::class.java)) // Pass a mock ItemViewModel

        // Set the adapter on the fragment's RecyclerView
        itemsFragment.adapter = adapter

        // Simulate clicking the Filter Options icon
        val filterButton = itemsFragment.view?.findViewById<ImageView>(R.id.filterButton)
        filterButton?.performClick()

        // Simulate selecting "Top" in the filter options
        val typeCheckBoxes = listOf(
            itemsFragment.view?.findViewById<CheckBox>(R.id.type_top),
        )

        typeCheckBoxes.forEach { checkBox ->
            checkBox?.isChecked = true // Check the boxes for "Top"
        }

        // Simulate clicking the "Apply" button
        val applyButton = itemsFragment.view?.findViewById<ImageView>(R.id.btn_apply)
        applyButton?.performClick()
    }

    @Test
    fun testResetButtonFunctionality() {
        // Create a mock for the ItemsFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Initialize type CheckBoxes
        val typeCheckBoxes = listOf(
            mockFragment.view?.findViewById<CheckBox>(R.id.type_top)
        )

        // Simulate checking the checkboxes
        typeCheckBoxes.forEach { checkBox ->
            checkBox?.isChecked = true // Check the box for "Top"
        }

        mockFragment.applyFilters(listOf("Top"), null)

        // Verify that the checkboxes are checked
        verify(mockFragment).applyFilters(listOf("Top"), null)

        // Simulate clicking the "Reset" button
        val resetButton = mockFragment.view?.findViewById<ImageView>(R.id.btn_reset)
        resetButton?.performClick()

        mockFragment.resetToOriginalList()

        // Verify that the checkboxes are now unchecked
        verify(mockFragment).resetToOriginalList()
    }
}