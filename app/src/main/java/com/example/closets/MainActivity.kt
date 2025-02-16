package com.example.closets

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.closets.databinding.ActivityMainBinding
import com.example.closets.ui.favorites.FavoritesFragment
import com.example.closets.ui.home.HomeFragment
import com.example.closets.ui.items.ItemsFragment
import com.example.closets.ui.unused.UnusedFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navView: BottomNavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force light mode to prevent dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showTipsDialog() // Show tips dialog

        navView = binding.navView

        // Make the navigation bar dark
        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)
            val decorView = window.decorView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                controller?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            } else {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }

        // Get the NavController from the NavHostFragment
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Set the default image for the navigation
        updateNavigationImage(R.drawable.nav_home) // Set to default PNG resource

        // Set up the ImageView click listeners for navigation
        setupNavigationClickListeners(navController)

        // Set up back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()

        when {
            currentFragment is HomeFragment -> {
                when {
                    currentFragment.isIconCurrentExpanded -> {
                        currentFragment.collapseButtonsAndRestoreIcon()
                        return
                    }
                    else -> {
                        finish()
                    }
                }
            }

            // Handle ItemsFragment special cases
            currentFragment is ItemsFragment -> {
                when {
                    // Case 1: Search results are showing
                    currentFragment.isViewingSearchResults -> {
                        currentFragment.resetSearchResults()
                        return
                    }
                    // Case 2: Multiple selection mode is active
                    currentFragment.isSelectingMultiple -> {
                        currentFragment.exitSelectMultipleMode()
                        return
                    }
                    // Case 3: Filters are applied
                    currentFragment.hasActiveFilters() -> {
                        currentFragment.clearAllFilters()
                        return
                    }
                    // Default state: Exit app if in main navigation
                    else -> {
                        finish()
                    }
                }
            }
            currentFragment is FavoritesFragment -> {
                when {
                    currentFragment.isViewingSearchResults -> {
                        currentFragment.resetSearchResults()
                        return
                    }
                    currentFragment.hasActiveFilters() -> {
                        currentFragment.clearAllFilters()
                        return
                    }
                    else -> {
                        finish()
                    }
                }
            }
            currentFragment is UnusedFragment -> {
                when {
                    // Check if filters are active
                    currentFragment.hasActiveFilters() -> {
                        currentFragment.clearAllFilters()
                        return
                    }
                    // Default state: Exit app if in main navigation
                    else -> {
                        finish()
                    }
                }
            }
            // Handle main navigation destinations
            navController.currentDestination?.id in setOf(
                R.id.navigation_home,
                R.id.navigation_favorites,
                R.id.navigation_items,
                R.id.navigation_unused,
                R.id.navigation_data
            ) -> {
                finish()
            }
            // Default: Navigate up
            else -> {
                navController.navigateUp()
            }
        }
    }

    fun showTipsDialog(): AlertDialog? {
        val sharedPreferences = getSharedPreferences("YourPrefs", Context.MODE_PRIVATE)
        val dontShowAgain = sharedPreferences.getBoolean("dont_show_tips", false)

        // Show the dialog only if the user hasn't opted out
        if (!dontShowAgain) {
            // Inflate the dialog layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_tips, null)

            // Create the dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true) // Allow the dialog to be canceled
                .create()

            // Remove the default background to avoid unwanted outlines
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Find views
            val checkBoxDontShowAgain: CheckBox = dialogView.findViewById(R.id.checkbox_dont_show_again)
            val btnClose: TextView = dialogView.findViewById(R.id.btn_close)

            // Set the close button click listener
            btnClose.setOnClickListener {
                if (checkBoxDontShowAgain.isChecked) {
                    // Save the preference to not show the dialog again
                    sharedPreferences.edit().putBoolean("dont_show_tips", true).apply()
                }
                // Mark that the tips dialog has been shown
                sharedPreferences.edit().putBoolean("tips_shown", true).apply()
                dialog.dismiss()
            }

            // Set a listener for when the dialog is canceled (e.g., tapping outside)
            dialog.setOnCancelListener {
                if (checkBoxDontShowAgain.isChecked) {
                    // Save the preference to not show the dialog again
                    sharedPreferences.edit().putBoolean("dont_show_tips", true).apply()
                }
                // Mark that the tips dialog has been shown
                sharedPreferences.edit().putBoolean("tips_shown", true).apply()
            }

            // Show the dialog
            dialog.show()
            return dialog // Return the dialog instance
        }
        return null // Return null if the dialog is not shown
    }

    private fun setupNavigationClickListeners(navController: NavController) {
        val navigationImageView = findViewById<ImageView>(R.id.navigation_image_view)

        navigationImageView.setOnClickListener {
            // Handle clicks based on the current image
            when (it) {
                findViewById<ImageView>(R.id.navigation_image_view) -> {
                    // Navigate to Home when clicking on nav_home image
                    navController.navigate(R.id.navigation_home)
                }
            }
        }

        // Set a listener for bottom navigation view item selection to update image
        @Suppress("DEPRECATION")
        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    updateNavigationImage(R.drawable.nav_home)
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_favorites -> {
                    updateNavigationImage(R.drawable.nav_favorites)
                    navController.navigate(R.id.navigation_favorites)
                    true
                }
                R.id.navigation_items -> {
                    updateNavigationImage(R.drawable.nav_items)
                    navController.navigate(R.id.navigation_items)
                    true
                }

                R.id.navigation_unused -> {
                    updateNavigationImage(R.drawable.nav_unused)
                    navController.navigate(R.id.navigation_unused)
                    true
                }
                R.id.navigation_data -> {
                    updateNavigationImage(R.drawable.nav_data)
                    navController.navigate(R.id.navigation_data)
                    true
                }
                else -> false
            }
        }

        // Set the default image when the activity starts
        updateNavigationImage(R.drawable.nav_home) // Set to your default PNG resource
    }

    fun updateNavigationImage(imageResId: Int) {
        // Update the ImageView with the new image resource
        findViewById<ImageView>(R.id.navigation_image_view).setImageResource(imageResId)
    }
}