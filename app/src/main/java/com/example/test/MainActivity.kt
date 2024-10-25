package com.example.test

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.NavController
import com.example.test.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navView = binding.navView

        // Get the NavController from the NavHostFragment
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Set up the ImageView click listeners for navigation
        setupNavigationClickListeners(navController)
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

    private fun updateNavigationImage(imageResId: Int) {
        // Update the ImageView with the new image resource
        findViewById<ImageView>(R.id.navigation_image_view).setImageResource(imageResId)
    }
}