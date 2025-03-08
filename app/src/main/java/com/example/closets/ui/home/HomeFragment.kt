package com.example.closets.ui.home

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
import androidx.work.WorkManager
import com.example.closets.R
import com.example.closets.SharedViewModel
import com.example.closets.notifications.NotificationReceiver
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var isNotifIconOn: Boolean = false
    private val PREFS_NAME = "ClosetsPrefs"
    private val NOTIF_STATE_KEY = "notification_state"

    private lateinit var workManager: WorkManager
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
                NotificationReceiver.scheduleExactDailyNotification(requireContext())
                saveNotificationState(true)
                isNotifIconOn = true
                showToast(requireContext(), "Notifications enabled.")
            } else {
                iconNotif.setImageResource(R.drawable.icon_notif_off)
                NotificationReceiver.cancelDailyNotification(requireContext())
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
        val trace = FirebasePerformance.getInstance().newTrace("homeFragment_onCreateView")
        trace.start()

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

        trace.stop()
        return root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trace = FirebasePerformance.getInstance().newTrace("homeFragment_onViewCreated")
        trace.start()

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

        trace.stop()
    }

    private fun loadNotificationState() {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isNotifIconOn = sharedPreferences.getBoolean(NOTIF_STATE_KEY, false) // Default to false if not set

        if (isNotifIconOn) {
            iconNotif.setImageResource(R.drawable.icon_notif_on)
            NotificationReceiver.scheduleExactDailyNotification(requireContext()) // Use the new method
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
            NotificationReceiver.cancelDailyNotification(requireContext())
            saveNotificationState(false)
            isNotifIconOn = false
            showToast(requireContext(), "Notifications disabled.")
        } else {
            // Check Android version and handle permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        enableNotifications()
                    }
                    else -> {
                        // Directly request the permission
                        requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                enableNotifications()
            }
        }
    }

    private fun enableNotifications() {
        iconNotif.setImageResource(R.drawable.icon_notif_on)
        NotificationReceiver.scheduleExactDailyNotification(requireContext())
        saveNotificationState(true)
        isNotifIconOn = true
        showToast(requireContext(), "Notifications enabled.")
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission() // Check notification permission status when the fragment resumes
    }


    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, turn off notifications
                iconNotif.setImageResource(R.drawable.icon_notif_off)
                NotificationReceiver.cancelDailyNotification(requireContext())
                isNotifIconOn = false
                saveNotificationState(false)
            } else {
                // Permission granted, ensure notifications can be scheduled if previously enabled
                if (isNotifIconOn) {
                    iconNotif.setImageResource(R.drawable.icon_notif_on)
                    NotificationReceiver.scheduleExactDailyNotification(requireContext())
                    saveNotificationState(true)
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        // hide the icon when navigating away from HomeFragment
        val iconCurrentImageView = view?.findViewById<ImageView>(R.id.icon_current)
        iconCurrentImageView?.visibility = View.GONE

    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_home)
    }
}