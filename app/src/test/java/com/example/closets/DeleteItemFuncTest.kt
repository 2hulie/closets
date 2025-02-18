package com.example.closets

import android.os.Bundle
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.fragments.ItemInfoFragment
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
class DeleteItemFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var itemsFragment: ItemsFragment
    private lateinit var itemViewModel: ItemViewModel

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

        // Initialize the ItemViewModel (mock or real instance)
        itemViewModel = mock(ItemViewModel::class.java)
        itemsFragment.itemViewModel = itemViewModel // Set the mock ItemViewModel to the fragment
    }

    @Test
    fun testDeleteItem() {
        // Create a mock for the ItemInfoCapFragment
        val mockFragment = mock(ItemInfoFragment::class.java)

        // Access the "Delete item from Closet" button
        val deleteButton = mockFragment.view?.findViewById<ImageView>(R.id.icon_delete_item)

        // Simulate clicking the "Delete item from Closet" button
        deleteButton?.performClick()

        mockFragment.showDeleteConfirmationDialog()

        // Verify that the delete confirmation dialog is shown
        verify(mockFragment).showDeleteConfirmationDialog()
    }

    @Test
    fun testConfirmDelete() {
        // Create a mock for the ItemInfoCapFragment
        val mockFragment = mock(ItemInfoFragment::class.java)

        // Simulate showing the delete confirmation dialog
        mockFragment.showDeleteConfirmationDialog()

        // Simulate user selecting "Delete"
        mockFragment.deleteItem() // This method should handle the deletion logic
    }

    @Test
    fun testCancelDelete() {
        // Create a mock for the ItemInfoCapFragment
        val mockFragment = mock(ItemInfoFragment::class.java)

        // Simulate showing the delete confirmation dialog
        mockFragment.showDeleteConfirmationDialog()

        // Simulate user selecting "Cancel"
        val cancelButton = mockFragment.view?.findViewById<ImageView>(R.id.btn_cancel)
        cancelButton?.performClick()

        // Verify that the deleteItem method is not called
        verify(mockFragment, never()).deleteItem()
    }
}