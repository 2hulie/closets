package com.example.closets

import android.widget.EditText
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
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
class SearchBarFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var itemsFragment: ItemsFragment
    private lateinit var adapter: ItemsAdapter

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
    }

    @Test
    fun testSearchForSpecificItem() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(
            id = 1,
            imageUri = "uri_to_cap_image", // Replace with a valid URI if needed
            name = "Cap",
            type = "Top",
            color = "#726C5D",
            isFavorite = true,
            wornTimes = 0,
            lastWornDate = null,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment
        )

        // Create another clothing item representing "Dress"
        val dressItem = ClothingItem(
            id = 2,
            imageUri = "uri_to_dress_image",
            name = "Dress",
            type = "Bottom",
            color = "#1C88A4",
            isFavorite = false,
            wornTimes = 0,
            lastWornDate = null,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment
        )

        // Add the items to the fragment's list
        itemsFragment.allItems = listOf(capItem, dressItem)
        itemsFragment.sortedItems = itemsFragment.allItems.toMutableList()

        // Create the adapter with a simple click listener
        adapter = ItemsAdapter(itemsFragment.sortedItems, { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }, itemsFragment, mock(ItemViewModel::class.java)) // Pass a mock ItemViewModel

        // Set the adapter on the fragment's RecyclerView
        itemsFragment.adapter = adapter

        // Simulate typing "cap" in the search bar
        val searchInput = itemsFragment.view?.findViewById<EditText>(R.id.search_input)
        searchInput?.setText("cap")

        // Simulate clicking the "Search" button (magnifying glass icon)
        val searchButton = itemsFragment.view?.findViewById<ImageView>(R.id.icon_search)
        searchButton?.performClick()
    }
}