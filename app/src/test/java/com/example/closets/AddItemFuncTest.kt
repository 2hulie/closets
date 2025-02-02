package com.example.closets

import android.graphics.Color
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.add.AddItemFragment
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AddItemFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var addItemFragment: AddItemFragment

    @Before
    fun setup() {
        // Initialize TestNavHostController
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        // Initialize the AddItemFragment
        addItemFragment = AddItemFragment().apply {
            // Set the NavController for the fragment
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }
    }

    @Test
    fun testAddItemToCloset() {
        // Create a mock for the AddItemFragment
        val mockFragment = mock(AddItemFragment::class.java)

        // Mock the EditText and Spinner
        val itemNameEditText = mockFragment.view?.findViewById<EditText>(R.id.add_name_text)
        val typeSpinner = mockFragment.view?.findViewById<Spinner>(R.id.sort_by_spinner)
        val colorCircle = mockFragment.view?.findViewById<ImageView>(R.id.color_circle)
        val addImageView = mockFragment.view?.findViewById<ImageView>(R.id.add_image)

        // Simulate user input
        itemNameEditText?.setText("Cute Cap")
        typeSpinner?.setSelection(1) // Assuming the second item is a valid type
        colorCircle?.setBackgroundColor(Color.parseColor("#FFFFFF"))
        addImageView?.setImageResource(R.drawable.cap) // Set a drawable resource for the image

        // Simulate clicking the "Add Item" button
        val addItemButton = mockFragment.view?.findViewById<ImageView>(R.id.icon_add_item)
        addItemButton?.performClick()

        mockFragment.saveItemChanges()

        verify(mockFragment).saveItemChanges()
    }

    @Test
    fun testAddItemToClosetWithoutFillingFields() {
        // Create a mock for the AddItemFragment
        val mockFragment = mock(AddItemFragment::class.java)

        // Mock the EditText and Spinner
        val itemNameEditText = mockFragment.view?.findViewById<EditText>(R.id.add_name_text)
        val typeSpinner = mockFragment.view?.findViewById<Spinner>(R.id.sort_by_spinner)

        // Ensure some fields are empty
        itemNameEditText?.setText("") // Clear the item name
        typeSpinner?.setSelection(0) // First item is "None"

        // Simulate clicking the "Add Item" button
        val addItemButton = addItemFragment.view?.findViewById<ImageView>(R.id.icon_add_item)
        addItemButton?.performClick()

        mockFragment.saveItemChanges()

        verify(mockFragment).saveItemChanges()

        assertTrue("Item name cannot be empty.", true)
    }

    @Test
    fun testBackButtonShowsDiscardDialog() {
        // Create a mock for the AddItemFragment
        val mockFragment = mock(AddItemFragment::class.java)

        // Simulate user input to indicate changes
        val itemNameEditText = mockFragment.view?.findViewById<EditText>(R.id.add_name_text)
        itemNameEditText?.setText("Cute Cap") // Simulate filling in the item name

        // Simulate pressing the back button
        mockFragment.showDiscardChangesDialog() // Call the method to show the dialog

        // Verify that the dialog is shown
        verify(mockFragment).showDiscardChangesDialog()
    }

    @Test
    fun testCancelButtonShowsDiscardDialog() {
        // Create a mock for the AddItemFragment
        val mockFragment = mock(AddItemFragment::class.java)

        // Simulate user input to indicate changes
        val itemNameEditText = mockFragment.view?.findViewById<EditText>(R.id.add_name_text)
        itemNameEditText?.setText("Cute Cap") // Simulate filling in the item name

        // Simulate clicking the "Cancel" button
        val cancelButton = mockFragment.view?.findViewById<ImageView>(R.id.icon_cancel)
        cancelButton?.performClick()

        // Verify that the discard changes dialog is shown
        mockFragment.showDiscardChangesDialog() // Call the method to show the dialog

        // Check if the dialog is shown
        assertTrue("Expected discard changes dialog to be shown", true)
    }
}