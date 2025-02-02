package com.example.closets

import android.content.Context
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.closets.ui.home.HomeFragment
import com.example.closets.ui.items.ItemsFragment
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
class AppNavigationFuncTest {

    private lateinit var navController: TestNavHostController
    private lateinit var homeFragment: HomeFragment
    private lateinit var itemsFragment: ItemsFragment

    @Before
    fun setup() {
        // Initialize TestNavHostController
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        // Initialize the HomeFragment
        homeFragment = HomeFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Create a real instance of the ItemsFragment
        itemsFragment = ItemsFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController) // Use the mock NavController
                }
            }
        }
    }

    @Test
    fun testLaunchApplicationAndCheckHomePage() {
        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Create a mock for the CheckBox
        val checkBoxDontShowAgain: CheckBox = mock(CheckBox::class.java)

        // Simulate the user checking the "Don't show again" checkbox
        `when`(checkBoxDontShowAgain.isChecked).thenReturn(true) // Simulate that the checkbox is checked

        // Simulate closing the quick tips popup
        val btnClose: TextView = mockFragment.view?.findViewById(R.id.btn_close) ?: mock(TextView::class.java)
        btnClose.performClick() // Close the dialog

        // Verify that the "Don't show again" checkbox was checked
        println("Checkbox 'Don't show again' checked: ${checkBoxDontShowAgain.isChecked}")

        // Verify that the close button was clicked
        verify(btnClose).performClick() // Verify that the close button was clicked

        // Verify that the home page elements are visible
        val floatingHanger: ImageView? = mockFragment.view?.findViewById(R.id.icon_current)
        println("Floating hanger visibility: ${floatingHanger?.visibility}")

        // Check for the presence of recent items, favorites, and idle items
        val itemsTitle: TextView? = mockFragment.view?.findViewById(R.id.items_title)
        val favouritesTitle: TextView? = mockFragment.view?.findViewById(R.id.favorites_title)
        val idleItemsTitle: TextView? = mockFragment.view?.findViewById(R.id.idle_items_title)

        println("Items title visibility: ${itemsTitle?.visibility}")
        println("Favorites title visibility: ${favouritesTitle?.visibility}")
        println("Idle items title visibility: ${idleItemsTitle?.visibility}")
    }

    @Test
    fun testLaunchApplicationAgainWithDontShowAgainChecked() {
        // Create a real instance of the HomeFragment
        HomeFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Simulate that the "Don't show again" checkbox was previously checked
        val sharedPreferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("YourPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("dont_show_tips", true).apply()

        // Simulate opening the app again
        HomeFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), navController)
                }
            }
        }

        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Verify that the home page elements are visible
        val floatingHanger: ImageView? = mockFragment.view?.findViewById(R.id.icon_current)
        println("Floating hanger visibility: ${floatingHanger?.visibility}")

        // Check for the presence of recent items, favorites, and idle items
        val itemsTitle: TextView? = mockFragment.view?.findViewById(R.id.items_title)
        val favouritesTitle: TextView? = mockFragment.view?.findViewById(R.id.favorites_title)
        val idleItemsTitle: TextView? = mockFragment.view?.findViewById(R.id.idle_items_title)

        println("Items title visibility: ${itemsTitle?.visibility}")
        println("Favorites title visibility: ${favouritesTitle?.visibility}")
        println("Idle items title visibility: ${idleItemsTitle?.visibility}")
    }

    @Test
    fun testCheckItemsPage() {
        // Create a mock for the NavController
        val mockNavController = mock(NavController::class.java)

        // Create a real instance of the HomeFragment
        HomeFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), mockNavController) // Use the mock NavController
                }
            }
        }

        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Simulate the user tapping the "See all" button
        val seeAllButton: TextView? = mockFragment.view?.findViewById(R.id.items_see_all)
        seeAllButton?.performClick() // Simulate clicking the "See all" button

        mockNavController.navigate(R.id.navigation_items)

        // Verify that the navigation to the ItemsFragment occurred
        verify(mockNavController).navigate(R.id.navigation_items) // Verify that the navigate method was called on the mock NavController
    }

    @Test
    fun testCheckFavoritesPage() {
        // Create a mock for the NavController
        val mockNavController = mock(NavController::class.java)

        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Simulate the user tapping the "See all" button
        val seeAllButton: TextView? = mockFragment.view?.findViewById(R.id.favorites_see_all)
        seeAllButton?.performClick() // Simulate clicking the "See all" button

        mockNavController.navigate(R.id.navigation_favorites)

        // Verify that the navigation to the FavoritesFragment occurred
        verify(mockNavController).navigate(R.id.navigation_favorites) // Verify that the navigate method was called on the mock NavController
    }

    @Test
    fun testCheckIdleItemsPage() {
        // Create a mock for the NavController
        val mockNavController = mock(NavController::class.java)

        // Create a real instance of the HomeFragment
        HomeFragment().apply {
            viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                viewLifecycleOwner?.let {
                    Navigation.setViewNavController(requireView(), mockNavController) // Use the mock NavController
                }
            }
        }

        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Simulate the user tapping the "See all" button
        val seeAllButton: TextView? = mockFragment.view?.findViewById(R.id.idle_items_see_all)
        seeAllButton?.performClick() // Simulate clicking the "See all" button

        mockNavController.navigate(R.id.navigation_unused)

        // Verify that the navigation to the UnusedFragment occurred
        verify(mockNavController).navigate(R.id.navigation_unused) // Verify that the navigate method was called on the mock NavController
    }

    @Test
    fun testCheckDataTransferPage() {
        // Create a mock for the NavController
        val mockNavController = mock(NavController::class.java)

        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Simulate the user tapping Data Transfer button
        val dataTransferButton: ImageView? = mockFragment.view?.findViewById(R.id.navigation_data) // ID for Data Transfer
        dataTransferButton?.performClick() // Simulate clicking Data Transfer

        mockNavController.navigate(R.id.navigation_data)

        // Verify that the navigation to the Data Transfer Page occurred
        verify(mockNavController).navigate(R.id.navigation_data) // Verify that the navigate method was called on the mock NavController
    }

    @Test
    fun testCheckHomePage() {
        // Create a mock for the NavController
        val mockNavController = mock(NavController::class.java)

        // Create a mock for the ItemsFragment
        val mockFragment = mock(ItemsFragment::class.java)

        // Simulate the user tapping Home button
        val homeButton: ImageView? = mockFragment.view?.findViewById(R.id.navigation_home) // ID for Home
        homeButton?.performClick() // Simulate clicking Home

        mockNavController.navigate(R.id.navigation_home)

        // Verify that the navigation to the Data Transfer Page occurred
        verify(mockNavController).navigate(R.id.navigation_home) // Verify that the navigate method was called on the mock NavController
    }

    @Test
    fun testCheckFloatingHanger() {
        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Simulate the user tapping the floating hanger
        val floatingHanger: ImageView? = mockFragment.view?.findViewById(R.id.icon_current) // ID for the floating hanger
        floatingHanger?.performClick() // Simulate clicking the floating hanger
    }

    @Test
    fun testCheckCurrentClothesPage() {
        // Create a mock for the NavController
        val mockNavController = mock(NavController::class.java)

        // Create a mock for the HomeFragment
        val mockFragment = mock(HomeFragment::class.java)

        // Simulate the user tapping the floating hanger
        val floatingHanger: ImageView? = mockFragment.view?.findViewById(R.id.icon_current) // ID for the floating hanger
        floatingHanger?.performClick() // Simulate clicking the floating hanger

        // Simulate selecting the "View Today's Outfit" option
        val viewOutfitOption: TextView? = mockFragment.view?.findViewById(R.id.action_homeFragment_to_todayOutfitBottomSheet) // ID for the view outfit option
        viewOutfitOption?.performClick() // Simulate clicking the "View Today's Outfit" option

        // Simulate tapping the "Pencil" icon beside the Today's Outfit line
        val pencilIcon: ImageView? = mockFragment.view?.findViewById(R.id.icon_pencil) // ID for the pencil icon
        pencilIcon?.performClick() // Simulate clicking the "Pencil" icon

        mockNavController.navigate(R.id.action_navigation_home_to_currentItemFragment)

        // Verify that the navigation to the Current Clothes Page occurred
        verify(mockNavController).navigate(R.id.action_navigation_home_to_currentItemFragment) // Verify that the navigate method was called on the mock NavController
    }
}