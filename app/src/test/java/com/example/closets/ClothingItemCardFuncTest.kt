package com.example.closets

import android.os.Bundle
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.items.ItemInfoFragment
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsAdapter
import com.example.closets.ui.items.ItemsFragment
import com.example.closets.ui.viewmodels.ItemViewModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ClothingItemCardFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var fragment: ItemsFragment
    private lateinit var itemViewModel: ItemViewModel

    @Before
    fun setup() {
        // Initialize TestNavHostController
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        // Initialize the ItemsFragment
        fragment = ItemsFragment().apply {
            // Set the NavController for the fragment
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Initialize the ItemViewModel (mock or real instance)
        itemViewModel = mock(ItemViewModel::class.java)
        fragment.itemViewModel = itemViewModel // Set the mock ItemViewModel to the fragment
    }

    @Test
    fun testClickCapItem_NavigatesToCapDetailFragment() {
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
        fragment.allItems = listOf(capItem)
        fragment.sortedItems = fragment.allItems.toMutableList()

        // Create an adapter with a click listener
        val adapter = ItemsAdapter(fragment.sortedItems, { item ->
            val delayMillis = 150L
            when (item.name) {
                "Cap" -> fragment.view?.postDelayed({
                    navController.navigate(R.id.action_itemsFragment_to_itemInfoFragment)
                }, delayMillis)
            }
        }, fragment, itemViewModel) // Pass the mock ItemViewModel

        // Set the adapter on the fragment's RecyclerView
        fragment.adapter = adapter

        // Simulate clicking on the "Cap" item
        adapter.itemClickListener(capItem)
    }

    @Test
    fun testFavoriteClothingItemCard() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(
            id = 2,
            imageUri = "uri_to_cap_image",
            name = "Other",
            type = "Top",
            color = "#FF5733",
            isFavorite = false,
            wornTimes = 0,
            lastWornDate = null,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment
        )

        // Initialize the ItemInfoFragment with the capItem
        val fragment = ItemInfoFragment().apply {
            arguments = Bundle().apply {
                putSerializable("item", capItem)
            }
        }

        // Simulate clicking the favorite icon
        val favoriteIcon = fragment.view?.findViewById<ImageView>(R.id.icon_favorite)
        favoriteIcon?.performClick() // Simulate clicking the favorite icon
    }

    @Test
    fun testFavoriteClothingItemInItemsPage() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(
            id = 3,
            imageUri = "uri_to_cap_image",
            name = "Cap",
            type = "Top",
            color = "#FF5733",
            isFavorite = false,
            wornTimes = 0,
            lastWornDate = null,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment
        )

        // Add the "Cap" item to the fragment's list
        fragment.allItems = listOf(capItem)
        fragment.sortedItems = fragment.allItems.toMutableList()

        // Initialize the ItemsFragment with the capItem
        val fragment = ItemsFragment().apply {
            arguments = Bundle().apply {
                putSerializable("item", capItem)
            }
        }

        // Simulate clicking the favorite icon
        val favoriteIcon = fragment.view?.findViewById<ImageView>(R.id.icon_favorite)
        favoriteIcon?.performClick() // Simulate clicking the favorite icon
    }

    @Test
    fun testBackButtonFromItemInfoCapFragment() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(
            id = 4,
            imageUri = "uri_to_cap_image",
            name = "Cap",
            type = "Top",
            color = "#FF5733",
            isFavorite = false,
            wornTimes = 0,
            lastWornDate = null,
            fragmentId = R.id.action_itemsFragment_to_itemInfoFragment
        )

        // Initialize the ItemsFragment and set the items
        fragment.allItems = listOf(capItem)
        fragment.sortedItems = fragment.allItems.toMutableList()

        // Initialize the ItemInfoFragment with the capItem
        val itemInfoFragment = ItemInfoFragment().apply {
            arguments = Bundle().apply {
                putSerializable("item", capItem)
            }
        }

        // Now we are in the ItemInfoFragment, simulate pressing the back button
        navController.popBackStack() // Simulate back navigation
    }
}