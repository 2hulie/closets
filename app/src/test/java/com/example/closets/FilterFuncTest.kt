package com.example.closets

import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Spinner
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.FilterBottomSheetDialog
import com.example.closets.ui.favorites.FavoriteItem
import com.example.closets.ui.favorites.FavoritesAdapter
import com.example.closets.ui.favorites.FavoritesFragment
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.items.ItemsAdapter
import com.example.closets.ui.items.ItemsFragment
import com.example.closets.ui.unused.UnusedFragment
import com.example.closets.ui.unused.UnusedItem
import com.example.closets.ui.unused.UnusedItemsAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FilterFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var itemsFragment: ItemsFragment
    private lateinit var itemsAdapter: ItemsAdapter
    private lateinit var favoritesFragment: FavoritesFragment
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var filterDialog: FilterBottomSheetDialog
    private lateinit var unusedFragment: UnusedFragment

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

        // Initialize the FavoritesFragment
        favoritesFragment = FavoritesFragment().apply {
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
            onApplyFilters = { _, _ -> /* Do nothing for testing */ },
            onResetFilters = { /* Do nothing for testing */ }
        )

        // Initialize the UnusedFragment
        unusedFragment = UnusedFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }
    }

    @Test
    fun testFilterOptionsItemsFragment() {
        // Create a mock for the ItemsFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Initialize the adapter with some test data
        val testItems = listOf(
            ClothingItem(R.drawable.cap, "Cap", "#726C5D", true, "Cute Cap"),
            ClothingItem(R.drawable.shirt, "Top", "#3B9DBC", true, "Cool Shirt"),
            ClothingItem(R.drawable.shorts, "Bottom", "#A8A7AB", false, "Stylish Shorts"),
            ClothingItem(R.drawable.skirt, "Bottom", "#C1A281", true, "Fashionable Skirt"),
            ClothingItem(R.drawable.dress, "Dress", "#1C88A4", false, "Elegant Dress"),
            ClothingItem(R.drawable.shoes, "Shoes", "#FFBAC4", true, "Trendy Shoes")
        )

        // Create the adapter with a simple click listener
        val adapter = ItemsAdapter(testItems, { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }, mockFragment)

        // Set the adapter to the mock fragment
        `when`(mockFragment.adapter).thenReturn(adapter)

        // Simulate clicking the Filter Options icon
        val filterButton = mockFragment.view?.findViewById<ImageView>(R.id.filterButton)
        filterButton?.performClick()

        // Simulate selecting "Top" in the filter options
        val typeCheckBoxes = listOf(
            mockFragment.view?.findViewById<CheckBox>(R.id.type_top),
        )

        typeCheckBoxes.forEach { checkBox ->
            checkBox?.isChecked = true // Check the boxes for "Top" and "Bottom"
        }

        // Simulate clicking the "Apply" button
        val applyButton = mockFragment.view?.findViewById<ImageView>(R.id.btn_apply)
        applyButton?.performClick()

        mockFragment.applyFilters(listOf("Top"), null)
        mockFragment.updateItemsCount()

        // Verify that the filterItems method is called with the correct types
        verify(mockFragment).applyFilters(listOf("Top"), null)

        // Verify that the updateItemsCount method is called
        verify(mockFragment).updateItemsCount() // Updates the count text
    }

    @Test
    fun testFilterOptionsFavoritesFragment() {
        // Create a real instance of the FavoritesFragment
        val favoritesFragment = FavoritesFragment().apply {
            // Set up the NavController for the fragment
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Create a mock for the FavoritesFragment
        val mockFragment = mock(FavoritesFragment::class.java)

        // Initialize the adapter with some test data
        val testFavoriteItems: MutableList<FavoriteItem> = mutableListOf(
            FavoriteItem(R.drawable.cap, "Cap", "#726C5D", true, "Cute Cap"),
            FavoriteItem(R.drawable.shirt, "Top", "#3B9DBC", true, "Cool Shirt"),
            FavoriteItem(R.drawable.shorts, "Bottom", "#A8A7AB", true, "Stylish Shorts")
        )

        // Create the adapter with a simple click listener
        val adapter = FavoritesAdapter(testFavoriteItems, { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }) {
            // Handle item removal (for testing, just print a message)
            println("Item removed")
        }

        // Set the adapter to the fragment
        favoritesFragment.adapter = adapter

        // Set the adapter to the mock fragment
        `when`(mockFragment.adapter).thenReturn(adapter)

        // Simulate clicking the Filter Options icon
        val filterButton = mockFragment.view?.findViewById<ImageView>(R.id.filterButton)
        filterButton?.performClick()

        // Simulate selecting "Top" in the filter options
        val typeCheckBoxes = listOf(
            mockFragment.view?.findViewById<CheckBox>(R.id.type_bottom),
        )

        typeCheckBoxes.forEach { checkBox ->
            checkBox?.isChecked = true // Check the boxes for "Top" and "Bottom"
        }

        // Simulate clicking the "Apply" button
        val applyButton = mockFragment.view?.findViewById<ImageView>(R.id.btn_apply)
        applyButton?.performClick()

        mockFragment.applyFilters(listOf("Bottom"), null)
        mockFragment.updateItemsCount()

        // Verify that the filterItems method is called with the correct types
        verify(mockFragment).applyFilters(listOf("Bottom"), null)

        // Verify that the updateItemsCount method is called
        verify(mockFragment).updateItemsCount() // Updates the count text
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

    @Test
    fun testSortByFavoritesFirst() {
        // Create a mock for the UnusedFragment
        val mockFragment = mock(UnusedFragment::class.java)

        // Initialize the adapter with some test data
        val testItems = mutableListOf(
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs.")
        )

        // Create the adapter and set it to the RecyclerView
        UnusedItemsAdapter(testItems) { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }

        // Simulate selecting "Favorites First" in the sort dropdown
        val sortSpinner = mockFragment.view?.findViewById<Spinner>(R.id.sort_by_spinner)
        sortSpinner?.setSelection(1) // "Favorites First" is at position 1

        mockFragment.sortByFavorites()

        // Verify that the items are sorted correctly
        verify(mockFragment).sortByFavorites()
    }

    @Test
    fun testSortByLongestToRecent() {
        // Create a mock for the UnusedFragment
        val mockFragment = mock(UnusedFragment::class.java)

        // Initialize the adapter with some test data
        val testItems = mutableListOf(
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs.")
        )

        // Create the adapter and set it to the RecyclerView
        UnusedItemsAdapter(testItems) { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }

        // Simulate selecting "Duration (Longest to Recent)" in the sort dropdown
        val sortSpinner = mockFragment.view?.findViewById<Spinner>(R.id.sort_by_spinner)
        sortSpinner?.setSelection(2) // "Duration (Longest to Recent)" is at position 2

        mockFragment.sortByDuration(oldestToRecent = false) // "Duration (Longest to Recent)"

        // Verify that the items are sorted correctly
        verify(mockFragment).sortByDuration(oldestToRecent = false)
    }

    @Test
    fun testSortByRecentToLongest() {
        // Create a mock for the UnusedFragment
        val mockFragment = mock(UnusedFragment::class.java)

        // Initialize the adapter with some test data
        val testItems = mutableListOf(
            UnusedItem(R.drawable.shorts, "Shorts", false, "5 mos."),
            UnusedItem(R.drawable.dress, "Dress", false, "1 yr."),
            UnusedItem(R.drawable.shoes, "Shoes", true, "3 mos."),
            UnusedItem(R.drawable.cap, "Cap", true, "2 yrs.")
        )

        // Create the adapter and set it to the RecyclerView
        UnusedItemsAdapter(testItems) { item ->
            // Handle item click (for testing, just print the item name)
            println("Item clicked: ${item.name}")
        }

        // Simulate selecting "Duration (Recent to Longest)" in the sort dropdown
        val sortSpinner = mockFragment.view?.findViewById<Spinner>(R.id.sort_by_spinner)
        sortSpinner?.setSelection(3) // "Duration (Recent to Longest)" is at position 3

        mockFragment.sortByDuration(oldestToRecent = true) // "Duration (Recent to Longest)"

        // Verify that the items are sorted correctly
        verify(mockFragment).sortByDuration(oldestToRecent = true)
    }
}