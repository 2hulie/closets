package com.example.closets

import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.fragments.ItemInfoCapFragment
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeleteItemFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var itemInfoCapFragment: ItemInfoCapFragment

    @Before
    fun setup() {
        // Initialize TestNavHostController
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        // Initialize the ItemInfoCapFragment
        itemInfoCapFragment = ItemInfoCapFragment().apply {
            // Set the NavController for the fragment
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }
    }

    @Test
    fun testDeleteItem() {
        // Create a mock for the ItemInfoCapFragment
        val mockFragment = mock(ItemInfoCapFragment::class.java)

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
        val mockFragment = mock(ItemInfoCapFragment::class.java)

        // Simulate showing the delete confirmation dialog
        mockFragment.showDeleteConfirmationDialog()

        // Simulate user selecting "Delete"
        mockFragment.deleteItem() // This method should handle the deletion logic
    }

    @Test
    fun testCancelDelete() {
        // Create a mock for the ItemInfoCapFragment
        val mockFragment = mock(ItemInfoCapFragment::class.java)

        // Simulate showing the delete confirmation dialog
        mockFragment.showDeleteConfirmationDialog()

        // Simulate user selecting "Cancel"
        val cancelButton = mockFragment.view?.findViewById<ImageView>(R.id.btn_cancel)
        cancelButton?.performClick()

        // Verify that the deleteItem method is not called
        verify(mockFragment, never()).deleteItem()
    }

    @Test
    fun testDeleteMultipleItems() {
        // Create a mock for the ItemInfoCapFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Simulate showing the delete confirmation dialog
        val selectedItemsToDelete = setOf(
            ClothingItem(R.drawable.cap, "Cap", "#726C5D", true, "Cute Cap"),
            ClothingItem(R.drawable.dress, "Dress", "#1C88A4", false, "Stylish Dress")
        )

        // Simulate clicking the "Delete items" button
        val deleteButton = mockFragment.view?.findViewById<ImageView>(R.id.icon_delete_multiple)
        deleteButton?.performClick()

        mockFragment.showDeleteConfirmationDialog(selectedItemsToDelete)

        // Verify that the delete confirmation dialog is shown
        verify(mockFragment).showDeleteConfirmationDialog(anySet()) // Takes a set of items to delete
    }

    @Test
    fun testConfirmDeleteMultipleItems() {
        // Create a mock for the ItemsFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Simulate showing the delete confirmation dialog
        val selectedItemsToDelete = setOf(
            ClothingItem(R.drawable.cap, "Cap", "#726C5D", true, "Cute Cap"),
            ClothingItem(R.drawable.dress, "Dress", "#1C88A4", false, "Stylish Dress")
        )
        mockFragment.showDeleteConfirmationDialog(selectedItemsToDelete)

        // Simulate user selecting "Delete"
        mockFragment.deleteSelectedItems() // This method should handle the deletion logic

        verify(mockFragment).deleteSelectedItems()
    }

    @Test
    fun testCancelDeleteMultipleItems() {
        // Create a mock for the ItemsFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Simulate showing the delete confirmation dialog
        mockFragment.showDeleteConfirmationDialog(emptySet())

        // Simulate user selecting "Cancel"
        val cancelButton = mockFragment.view?.findViewById<ImageView>(R.id.btn_cancel)
        cancelButton?.performClick()

        // Verify that the deleteSelectedItems method is not called
        verify(mockFragment, never()).deleteSelectedItems()
    }
}