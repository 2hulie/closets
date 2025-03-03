package com.example.closets

import android.content.Context
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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

    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 101
    }

    private var hasStoragePermission = false
    private var permissionDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        showTipsDialog()

        navView = binding.navView

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

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        updateNavigationImage(R.drawable.nav_home)

        setupNavigationClickListeners(navController)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (permissionDialog?.isShowing != true) {
            val previousPermissionState = hasStoragePermission
            checkPermissions() 
            // if the permission was previously false but is now true, refresh the activity
            if (!previousPermissionState && hasStoragePermission) {
                recreate() // refresh the entire activity
            }
        }
    }

    private fun checkPermissions() {
        val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        hasStoragePermission = ContextCompat.checkSelfPermission(this, permissionToCheck) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasStoragePermission) {
            showPrePermissionDialog()
        }
    }

    private fun showPrePermissionDialog() {
        if (isFinishing || permissionDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_denied, null)
        val btnSettings = dialogView.findViewById<ImageView>(R.id.btn_settings)
        val btnCancel = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        btnSettings.setOnClickListener {
            permissionDialog?.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        btnCancel.setOnClickListener {
            permissionDialog?.dismiss()
            finish()
        }

        permissionDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        permissionDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        permissionDialog?.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            hasStoragePermission = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (hasStoragePermission) {
                showTipsDialog()
            } else {
                val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (!shouldShowRequestPermissionRationale(permissionToCheck)) {
                    showPermissionDeniedDialog()
                } else {
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun showPermissionDeniedDialog() {
        if (isFinishing || permissionDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_denied, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val btnSettings = dialogView.findViewById<ImageView>(R.id.btn_settings)
        val btnCancel = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        titleTextView.text = "Full Permission Required"
        messageTextView.text = "This app requires full access to your photos to function properly. Please go to Settings and enable full access."

        btnSettings.setOnClickListener {
            permissionDialog?.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        btnCancel.setOnClickListener {
            permissionDialog?.dismiss()
            finish()
        }

        permissionDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        permissionDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        permissionDialog?.show()
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
            // handle main navigation destinations
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

    private fun showTipsDialog(): AlertDialog? {
        val sharedPreferences = getSharedPreferences("YourPrefs", Context.MODE_PRIVATE)
        val dontShowAgain = sharedPreferences.getBoolean("dont_show_tips", false)

        if (!dontShowAgain) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_tips, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val checkBoxDontShowAgain: CheckBox = dialogView.findViewById(R.id.checkbox_dont_show_again)
            val btnClose: TextView = dialogView.findViewById(R.id.btn_close)

            btnClose.setOnClickListener {
                if (checkBoxDontShowAgain.isChecked) {
                    sharedPreferences.edit().putBoolean("dont_show_tips", true).apply()
                }
                sharedPreferences.edit().putBoolean("tips_shown", true).apply()
                dialog.dismiss()
            }

            dialog.setOnCancelListener {
                if (checkBoxDontShowAgain.isChecked) {
                    sharedPreferences.edit().putBoolean("dont_show_tips", true).apply()
                }
                sharedPreferences.edit().putBoolean("tips_shown", true).apply()
            }

            dialog.show()
            return dialog
        }
        return null
    }

    private fun setupNavigationClickListeners(navController: NavController) {
        val navigationImageView = findViewById<ImageView>(R.id.navigation_image_view)

        navigationImageView.setOnClickListener {
            if (!hasStoragePermission) {
                showPermissionDeniedDialog()
                return@setOnClickListener
            }
            navController.navigate(R.id.navigation_home)
        }

        @Suppress("DEPRECATION")
        navView.setOnNavigationItemSelectedListener { item ->
            if (!hasStoragePermission) {
                showPermissionDeniedDialog()
                return@setOnNavigationItemSelectedListener false
            }
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
        updateNavigationImage(R.drawable.nav_home) // set to default PNG resource
    }

    fun updateNavigationImage(imageResId: Int) {
        // update the ImageView with the new image resource
        findViewById<ImageView>(R.id.navigation_image_view).setImageResource(imageResId)
    }

    override fun onDestroy() {
        // dismiss any dialog to prevent window leaks
        permissionDialog?.dismiss()
        super.onDestroy()
    }
}
