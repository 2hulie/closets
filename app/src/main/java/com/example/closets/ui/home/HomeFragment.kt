package com.example.closets.ui.home

import com.example.closets.notifications.NotificationWorker
import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.closets.R
import com.example.closets.SharedViewModel
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var isNotifIconOn: Boolean = false

    private val PREFS_NAME = "ClosetsPrefs"
    private val NOTIF_STATE_KEY = "notification_state"

    private lateinit var workManager: WorkManager
    private val notificationWorkName = "closets_reminder_notification"
    private lateinit var requestNotifPermissionLauncher: ActivityResultLauncher<String>
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var loadingView: View? = null
    private lateinit var itemViewModel: ItemViewModel
    private lateinit var homeUIManager: HomeUIManager
    private lateinit var darkOverlay: View

    private lateinit var iconCurrentImageView: ImageView
    private lateinit var iconNotif: ImageView
    private lateinit var iconOutfit: ImageView

    var isIconCurrentExpanded: Boolean = false

    companion object {
        private var currentToast: Toast? = null

        fun showToast(context: Context, message: String) {
            currentToast?.cancel() // cancel the previous toast
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show() // show the new toast
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]

        requestNotifPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                iconNotif.setImageResource(R.drawable.icon_notif_on)
                startNotificationWorker()
                saveNotificationState(true)
                isNotifIconOn = true
                showToast(requireContext(), "Notifications enabled.")
            } else {
                iconNotif.setImageResource(R.drawable.icon_notif_off)
                cancelNotifications()
                saveNotificationState(false)
                isNotifIconOn = false
                showToast(requireContext(), "Please enable notifications for this app in your device settings.")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (root as ViewGroup).addView(loadingView)
        iconNotif = root.findViewById(R.id.icon_notif)
        iconOutfit = root.findViewById(R.id.icon_outfit)

        // Initialize the HomeUIManager
        homeUIManager = HomeUIManager(
            requireContext(),
            viewLifecycleOwner,
            findNavController(),
            itemViewModel
        )

        // Initialize views through the manager
        homeUIManager.initializeViews(root, loadingView)
        homeUIManager.setupObservers(root)

        // Set the initial notification icon state
        homeUIManager.iconNotif.findViewById<ImageView>(R.id.icon_notif).setImageResource(R.drawable.icon_notif_off)
        isNotifIconOn = false

        return root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iconCurrentImageView = homeUIManager.iconCurrentImageView as ImageView
        workManager = WorkManager.getInstance(requireContext()) // initialize WorkManager

        loadNotificationState() // load notification state after initializing workManager

        darkOverlay = View(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#60000000")) // Semi-transparent black
            visibility = View.GONE // Initially hidden
            elevation = 1f // Lower elevation to keep it behind the icons
        }

        val rootLayout = view as ViewGroup
        rootLayout.addView(darkOverlay)
        homeUIManager.darkOverlay = darkOverlay

        homeUIManager.iconNotif.elevation = 2f
        homeUIManager.iconOutfit.elevation = 2f
        homeUIManager.tapToReturn.elevation = 2f

        homeUIManager.setupIconCurrentInteraction()

        darkOverlay.setOnClickListener {
            collapseButtonsAndRestoreIcon()
        }

        homeUIManager.iconNotif.isClickable = true
        homeUIManager.iconOutfit.isClickable = true

        setStatusBarColor()

        homeUIManager.iconNotif.setOnClickListener {
            toggleNotificationIcon()
        }

        iconCurrentImageView.visibility = View.VISIBLE
        iconCurrentImageView.setOnClickListener {
            if (iconNotif.visibility == View.GONE && iconOutfit.visibility == View.GONE) {
                iconNotif.visibility = View.VISIBLE
                iconOutfit.visibility = View.VISIBLE
                homeUIManager.tapToReturn.visibility = View.VISIBLE

                val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                iconNotif.startAnimation(fadeInAnimation)
                iconOutfit.startAnimation(fadeInAnimation)
                homeUIManager.tapToReturn.startAnimation(fadeInAnimation)

                zoomOutIcon(iconCurrentImageView)
            } else {
                val fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                iconNotif.startAnimation(fadeOutAnimation)
                iconOutfit.startAnimation(fadeOutAnimation)
                homeUIManager.tapToReturn.startAnimation(fadeOutAnimation)

                fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        iconNotif.visibility = View.GONE
                        iconOutfit.visibility = View.GONE
                        homeUIManager.tapToReturn.visibility = View.GONE
                        zoomInIcon(iconCurrentImageView)
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
            homeUIManager.toggleIconCurrentState()
        }

        homeUIManager.iconOutfit.setOnClickListener {
            // Load checked items from SharedPreferences
            val prefs = requireContext().getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
            val checkedItemIds = prefs.getStringSet("CheckedItems", emptySet()) ?: emptySet()

            if (checkedItemIds.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        // Get the database instance
                        val database = AppDatabase.getDatabase(requireContext())
                        val items = database.itemDao().getItemsByIds(checkedItemIds.map { it.toInt() })

                        // Convert items to ClothingItems
                        val checkedItems = items.map { item ->
                            ClothingItem(
                                id = item.id,
                                imageUri = item.imageUri,
                                name = item.name,
                                type = item.type,
                                color = item.color,
                                wornTimes = item.wornTimes,
                                lastWornDate = item.lastWornDate,
                                isFavorite = item.isFavorite,
                                fragmentId = R.id.action_homeFragment_to_itemInfoFragment
                            )
                        }

                        // Update SharedViewModel
                        sharedViewModel.setCheckedItems(checkedItems)

                        // Show the bottom sheet with loaded items
                        val bottomSheet = TodayOutfitBottomSheet(checkedItems)
                        bottomSheet.show(parentFragmentManager, "TodayOutfitBottomSheet")
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error loading checked items: ${e.message}")
                        // Show bottom sheet with empty list if there's an error
                        val bottomSheet = TodayOutfitBottomSheet(emptyList())
                        bottomSheet.show(parentFragmentManager, "TodayOutfitBottomSheet")
                    }
                }
            } else {
                // If no checked items in SharedPreferences, show bottom sheet with empty list
                val bottomSheet = TodayOutfitBottomSheet(emptyList())
                bottomSheet.show(parentFragmentManager, "TodayOutfitBottomSheet")
            }
        }

        sharedViewModel.checkedItems.observe(viewLifecycleOwner) {
        }
    }

    private fun loadNotificationState() {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isNotifIconOn = sharedPreferences.getBoolean(NOTIF_STATE_KEY, false) // Default to false if not set

        if (isNotifIconOn) {
            iconNotif.setImageResource(R.drawable.icon_notif_on)
            startNotificationWorker() // Start the worker if notifications are enabled
        } else {
            iconNotif.setImageResource(R.drawable.icon_notif_off)
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun zoomInIcon(imageView: ImageView) {
        homeUIManager.zoomInIcon(imageView)
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun zoomOutIcon(imageView: ImageView) {
        // Animate zoom-out effect
        val animatorX = ObjectAnimator.ofFloat(imageView, "scaleX", 1.0f, 0.0f)
        val animatorY = ObjectAnimator.ofFloat(imageView, "scaleY", 1.0f, 0.0f)

        animatorX.duration = 300 // Duration for animation
        animatorY.duration = 300 // Duration for animation

        animatorX.start()
        animatorY.start()
    }

    fun collapseButtonsAndRestoreIcon() {
       homeUIManager.collapseButtonsAndRestoreIcon()
    }

    private fun toggleNotificationIcon() {
        if (isNotifIconOn) {
            // Turn off notifications
            iconNotif.setImageResource(R.drawable.icon_notif_off)
            cancelNotifications()
            saveNotificationState(false) // Save state as off
            isNotifIconOn = false
            showToast(requireContext(), "Notifications disabled.")
        } else {
            // Check Android version and handle permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13+, check and request permission
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Permission already granted
                        iconNotif.setImageResource(R.drawable.icon_notif_on)
                        startNotificationWorker()
                        saveNotificationState(true)
                        isNotifIconOn = true
                        scheduleNotifications()
                        showToast(requireContext(), "Notifications enabled.")
                    }

                    else -> {
                        // Directly request the permission
                        requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                // for versions below Android 13
                iconNotif.setImageResource(R.drawable.icon_notif_on)
                startNotificationWorker()
                saveNotificationState(true)
                isNotifIconOn = true
                scheduleNotifications()
                showToast(requireContext(), "Notifications enabled.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission() // Check notification permission status when the fragment resumes
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, update the icon to off
                iconNotif.setImageResource(R.drawable.icon_notif_off)
                isNotifIconOn = false
                saveNotificationState(false) // Save state as off
            } else {
                // Permission granted, ensure the icon is on
                iconNotif.setImageResource(R.drawable.icon_notif_on)
                isNotifIconOn = true
                saveNotificationState(true) // Save state as on
            }
        }
    }

    private fun saveNotificationState(isEnabled: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(NOTIF_STATE_KEY, isEnabled)
            apply() // Apply changes asynchronously
        }
    }

    private fun scheduleNotifications() {
        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            startNotificationWorker()
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestNotificationPermission() {
        requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startNotificationWorker() {
        // Calculate initial delay until 9 AM
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = calendar.timeInMillis - now

        // Create work request for daily notifications
        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.DAYS,  // Repeat every day
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        // Enqueue the work
        workManager.enqueueUniquePeriodicWork(
            notificationWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWorkRequest
        )
    }

    private fun cancelNotifications() {
        workManager.cancelUniqueWork(notificationWorkName)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Hide the icon when navigating away from HomeFragment
        val iconCurrentImageView = view?.findViewById<ImageView>(R.id.icon_current)
        iconCurrentImageView?.visibility = View.GONE // Set icon_current visibility to gone

    }

    private fun setStatusBarColor() {
        // Change the status bar color
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_home)
    }
}