package com.example.closets

import android.os.Bundle
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.fragments.ItemInfoCapFragment
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsAdapter
import com.example.closets.ui.items.ItemsFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ClothingItemCardFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var fragment: ItemsFragment

    @Before
    fun setup() {
        // Initialize TestNavHostController
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        // Initialize the fragment
        fragment = ItemsFragment()
        fragment.arguments = Bundle()

        // Set the NavController for the fragment
        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
            viewLifecycleOwner?.let {
                Navigation.setViewNavController(fragment.requireView(), navController)
            }
        }
    }

    @Test
    fun testClickCapItem_NavigatesToCapDetailFragment() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(R.drawable.cap, "Cap", "#FF5733", true, "Cap")

        // Add the "Cap" item to the fragment's list
        fragment.allItems = listOf(capItem)
        fragment.sortedItems = fragment.allItems.toMutableList()

        // Create an adapter with a click listener
        val adapter = ItemsAdapter(fragment.sortedItems, { item ->
            val delayMillis = 150L
            when (item.name) {
                "Cap" -> fragment.view?.postDelayed({
                    navController.navigate(R.id.action_itemsFragment_to_itemInfoCapFragment)
                }, delayMillis)
            }
        }, fragment)

        // Set the adapter on the fragment's RecyclerView
        fragment.adapter = adapter

        // Simulate clicking on the "Cap" item
        adapter.itemClickListener(capItem)
    }

    @Test
    fun testFavoriteClothingItemCard() {
        // Create a clothing item representing "Cap"
        val capItem = ClothingItem(R.drawable.cap, "Other", "#FF5733", false, "Cap")

        // Initialize the ItemInfoCapFragment with the capItem
        val fragment = ItemInfoCapFragment().apply {
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
        val capItem = ClothingItem(R.drawable.cap, "Cap", "#FF5733", false, "Cap")

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
        val capItem = ClothingItem(R.drawable.cap, "Cap", "#FF5733", false, "Cap")

        // Initialize the ItemsFragment and set the items
        ItemsFragment().apply {
            allItems = listOf(capItem)
            sortedItems = allItems.toMutableList()
        }

        // Initialize the ItemInfoCapFragment with the capItem
        ItemInfoCapFragment().apply {
            arguments = Bundle().apply {
                putSerializable("item", capItem)
            }
        }

        // Now we are in the ItemInfoCapFragment, simulate pressing the back button
        navController.popBackStack() // Simulate back navigation
    }
}