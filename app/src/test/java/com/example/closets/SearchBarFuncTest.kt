package com.example.closets

import android.widget.EditText
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsAdapter
import com.example.closets.ui.items.ItemsFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
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
        // Create a real instance of the ItemsFragment
        val itemsFragment = ItemsFragment().apply {
            // Set up the NavController for the fragment
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Create a mock for the ItemsFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Initialize the adapter with some test data
        val testItems = listOf(
            ClothingItem(R.drawable.cap, "Cap", "#726C5D", true, "Cute Cap"),
            ClothingItem(R.drawable.dress, "Dress", "#1C88A4", false, "Stylish Dress"),
            ClothingItem(R.drawable.shirt, "Shirt", "#3B9DBC", true, "Cool Shirt"),
            ClothingItem(R.drawable.cap, "Cap", "#726C5D", true, "Another Cap")
        )

        // Create the adapter with a simple click listener
        val adapter = ItemsAdapter(testItems, { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }, itemsFragment)

        // Set the adapter to the fragment
        itemsFragment.adapter = adapter

        // Simulate typing "cap" in the search bar
        val searchInput = mockFragment.view?.findViewById<EditText>(R.id.search_input)
        searchInput?.setText("cap")

        // Simulate clicking the "Search" button (magnifying glass icon)
        val searchButton = mockFragment.view?.findViewById<ImageView>(R.id.icon_search)
        searchButton?.performClick()

        mockFragment.filterItems("cap")
        mockFragment.updateItemsCount()

        // Verify that the filterItems method is called with the correct query
        verify(mockFragment).filterItems("cap")

        // Verify that the items count text is updated
        verify(mockFragment).updateItemsCount() // Updates the count text
    }
}