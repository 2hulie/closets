package com.example.closets.ui.home

import NotificationWorker
import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.closets.MainActivity
import com.example.closets.R
import com.example.closets.SharedViewModel
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.items.ClothingItem
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var isNotifIconOn: Boolean = false

    private val PREFS_NAME = "ClosetsPrefs"
    private val NOTIF_STATE_KEY = "notification_state"

    private lateinit var workManager: WorkManager
    private val notificationWorkName = "closets_reminder_notification"
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var loadingView: View? = null
    private lateinit var itemViewModel: ItemViewModel
    private var sortedRecentItems: List<HomeItem> = emptyList()
    private var sortedFavoriteItems: List<HomeItem> = emptyList()
    private var sortedUnusedItems: List<HomeItem> = emptyList()
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var idleItemsRecyclerView: RecyclerView
    private lateinit var itemsTitle: TextView
    private lateinit var favoritesTitle: TextView
    private lateinit var idleItemsTitle: TextView
    private lateinit var darkOverlay: View
    private lateinit var tapToReturn: TextView

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

        // Initialize the ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]

        // initialize the ActivityResultLauncher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, turn on notifications
                iconNotif.setImageResource(R.drawable.icon_notif_on)
                startNotificationWorker()
                saveNotificationState(true) // Save state as on
                isNotifIconOn = true // Update the state
                showToast(requireContext(), "Notifications enabled.")
            } else {
                // Permission denied
                iconNotif.setImageResource(R.drawable.icon_notif_off)
                cancelNotifications() // Cancel any existing notifications
                saveNotificationState(false) // Save state as off
                isNotifIconOn = false // Ensure state is off
                showToast(requireContext(), "Please enable notifications for this app in your device settings.")
            }
        }


    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        loadingView = inflater.inflate(R.layout.loading_view, container, false)
        (root as ViewGroup).addView(loadingView)

        // Initialize RecyclerViews and TextViews
        itemsRecyclerView = root.findViewById(R.id.recycler_view) // Initialize items RecyclerView
        favoritesRecyclerView = root.findViewById(R.id.favorites_recycler_view) // Initialize favorites RecyclerView
        idleItemsRecyclerView = root.findViewById(R.id.idle_items_recycler_view) // Initialize idle items RecyclerView
        itemsTitle = root.findViewById(R.id.items_title) // Initialize items title
        favoritesTitle = root.findViewById(R.id.favorites_title) // Initialize favorites title
        idleItemsTitle = root.findViewById(R.id.idle_items_title) // Initialize idle items title
        tapToReturn = root.findViewById(R.id.tap_to_return) // Initialize return text

        // Initialize icon views
        iconCurrentImageView = root.findViewById(R.id.icon_current)
        iconNotif = root.findViewById(R.id.icon_notif)
        iconOutfit = root.findViewById(R.id.icon_outfit)

        // Show loading state initially
        val emptyMessage = root.findViewById<TextView>(R.id.homeEmptyMessage)
        emptyMessage.visibility = View.GONE

        // Initially hide all content until data is loaded
        itemsRecyclerView.visibility = View.GONE
        favoritesRecyclerView.visibility = View.GONE
        idleItemsRecyclerView.visibility = View.GONE
        itemsTitle.visibility = View.GONE
        favoritesTitle.visibility = View.GONE
        idleItemsTitle.visibility = View.GONE

        // Set the initial visibility of notification and outfit icons
        iconNotif.visibility = View.GONE
        iconOutfit.visibility = View.GONE
        tapToReturn.visibility = View.GONE

        iconNotif.setImageResource(R.drawable.icon_notif_off) // Set to off state
        isNotifIconOn = false // Ensure the state reflects that notifications are off

        // Find "See All" TextViews
        val itemsSeeAll = root.findViewById<TextView>(R.id.items_see_all)
        val favoritesSeeAll = root.findViewById<TextView>(R.id.favorites_see_all)
        val idleItemsSeeAll = root.findViewById<TextView>(R.id.idle_items_see_all)

        // Hide "See All" until data is loaded
        itemsSeeAll.visibility = View.GONE
        favoritesSeeAll.visibility = View.GONE
        idleItemsSeeAll.visibility = View.GONE

        // Set click listeners for "See All" with navigation image updates
        itemsSeeAll.setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.nav_view)?.apply {
                selectedItemId = R.id.navigation_items
            }
        }

        favoritesSeeAll.setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.nav_view)?.apply {
                selectedItemId = R.id.navigation_favorites
            }
        }

        idleItemsSeeAll.setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.nav_view)?.apply {
                selectedItemId = R.id.navigation_unused
            }
        }

        // observe LiveData for items from database
        itemViewModel.items.observe(viewLifecycleOwner) { clothingItems ->
            lifecycleScope.launch {
                sortedRecentItems = clothingItems
                    .sortedByDescending { it.id }
                    .take(5)
                    .map { clothingItem ->
                        HomeItem(
                            id = clothingItem.id,
                            name = clothingItem.name,
                            imageUri = clothingItem.imageUri
                        )
                    }

                if (sortedRecentItems.isNotEmpty()) {
                    itemsTitle.visibility = View.VISIBLE
                    itemsSeeAll.visibility = View.VISIBLE
                    itemsRecyclerView.visibility = View.VISIBLE

                    val adapter = HomeItemAdapter(sortedRecentItems) { homeItem ->
                        val bundle = Bundle().apply {
                            putInt("item_id", homeItem.id)
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_itemInfoFragment, bundle)
                    }
                    itemsRecyclerView.adapter = adapter
                    itemsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                } else {
                    itemsTitle.visibility = View.GONE
                    itemsSeeAll.visibility = View.GONE
                    itemsRecyclerView.visibility = View.GONE
                }
                updateEmptyState(root)
                loadingView?.visibility = View.GONE
            }
        }

        // observe favorite items
        itemViewModel.favoriteItems.observe(viewLifecycleOwner) { favoriteItems ->
            lifecycleScope.launch {
                sortedFavoriteItems = favoriteItems
                    .sortedByDescending { it.wornTimes }
                    .take(5)
                    .map { favoriteItem ->
                        HomeItem(
                            id = favoriteItem.id,
                            name = favoriteItem.name,
                            imageUri = favoriteItem.imageUri
                        )
                    }

                if (sortedFavoriteItems.isNotEmpty()) {
                    favoritesTitle.visibility = View.VISIBLE
                    favoritesSeeAll.visibility = View.VISIBLE
                    favoritesRecyclerView.visibility = View.VISIBLE

                    val adapter = HomeItemAdapter(sortedFavoriteItems) { homeItem ->
                        val bundle = Bundle().apply {
                            putInt("item_id", homeItem.id)
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_itemInfoFragment, bundle)
                    }
                    favoritesRecyclerView.adapter = adapter
                    favoritesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                } else {
                    favoritesTitle.visibility = View.GONE
                    favoritesSeeAll.visibility = View.GONE
                    favoritesRecyclerView.visibility = View.GONE
                }
                updateEmptyState(root)
                loadingView?.visibility = View.GONE
            }
        }

        // observe unused items
        itemViewModel.items.observe(viewLifecycleOwner) { items ->
            lifecycleScope.launch {
                // Filter for unused items and sort by longest unused duration first
                sortedUnusedItems = items
                    .filter { item -> hasBeenUnusedForAtLeastThreeMonths(item.lastWornDate) }
                    .sortedByDescending { item ->
                        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        val lastWornDate = formatter.parse(item.lastWornDate) ?: Date()
                        Date().time - lastWornDate.time
                    }
                    .take(5)
                    .map { item ->
                        HomeItem(
                            id = item.id,
                            name = item.name,
                            imageUri = item.imageUri
                        )
                    }

                if (sortedUnusedItems.isNotEmpty()) {
                    idleItemsTitle.visibility = View.VISIBLE
                    idleItemsSeeAll.visibility = View.VISIBLE
                    idleItemsRecyclerView.visibility = View.VISIBLE

                    val adapter = HomeItemAdapter(sortedUnusedItems) { homeItem ->
                        val bundle = Bundle().apply {
                            putInt("item_id", homeItem.id)
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_itemInfoFragment, bundle)
                    }
                    idleItemsRecyclerView.adapter = adapter
                    idleItemsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                } else {
                    idleItemsTitle.visibility = View.GONE
                    idleItemsSeeAll.visibility = View.GONE
                    idleItemsRecyclerView.visibility = View.GONE
                }
                updateEmptyState(root)
                loadingView?.visibility = View.GONE
            }
        }

        return root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize WorkManager
        workManager = WorkManager.getInstance(requireContext())

        // Load notification state after initializing workManager
        loadNotificationState()

        // Create the overlay
        darkOverlay = View(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#60000000")) // Semi-transparent black
            visibility = View.GONE // Initially hidden
            elevation = 1f // Lower elevation to keep it behind the icons
        }

        // Use the root view of the fragment as the parent layout
        val rootLayout = view as ViewGroup

        // Add the overlay BEFORE the existing views
        rootLayout.addView(darkOverlay)

        // Bring notification and outfit icons to the front
        iconNotif.elevation = 2f
        iconOutfit.elevation = 2f
        tapToReturn.elevation = 2f

        // Handle click on the overlay to dismiss the buttons
        darkOverlay.setOnClickListener {
            collapseButtonsAndRestoreIcon()
        }

        iconNotif.isClickable = true
        iconOutfit.isClickable = true

        // Change status bar color for this fragment
        setStatusBarColor()

        // Make icon_current floatable and handle clicks
        setupIconCurrentInteraction()

        // Set up click listener for icon_notif
        iconNotif.setOnClickListener {
            toggleNotificationIcon()
        }

        // Load the slide-in animation for iconCurrent
        val slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in)
        iconCurrentImageView.visibility = View.VISIBLE
        iconCurrentImageView.startAnimation(slideInAnimation)

        // Set up click listener for iconCurrent
        iconCurrentImageView.setOnClickListener {
            if (iconNotif.visibility == View.GONE && iconOutfit.visibility == View.GONE) {
                // Show the buttons with fade-in animation
                iconNotif.visibility = View.VISIBLE
                iconOutfit.visibility = View.VISIBLE
                tapToReturn.visibility = View.VISIBLE

                val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                iconNotif.startAnimation(fadeInAnimation)
                iconOutfit.startAnimation(fadeInAnimation)
                tapToReturn.startAnimation(fadeInAnimation)

                // Reduce the size of iconCurrent
                zoomOutIcon(iconCurrentImageView)
            } else {
                // Hide the buttons with fade-out animation
                val fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                iconNotif.startAnimation(fadeOutAnimation)
                iconOutfit.startAnimation(fadeOutAnimation)
                tapToReturn.startAnimation(fadeOutAnimation)

                fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        iconNotif.visibility = View.GONE
                        iconOutfit.visibility = View.GONE
                        tapToReturn.visibility = View.GONE
                        zoomInIcon(iconCurrentImageView)
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
            toggleIconCurrentState()
        }

        // Add a click listener for iconOutfit to show "Today's Outfit" bottom sheet
        iconOutfit.setOnClickListener {
            // Load checked items from SharedPreferences
            val prefs = requireContext().getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
            val checkedItemIds = prefs.getStringSet("CheckedItems", emptySet()) ?: emptySet()

            // If we have checked items in SharedPreferences, load them from the database
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

        // Initialize the checked items observer separately
        sharedViewModel.checkedItems.observe(viewLifecycleOwner) {
            // This will update whenever the checked items change
            // You can add additional handling here if needed
        }
    }

    private fun hasBeenUnusedForAtLeastThreeMonths(lastWornDate: String): Boolean {
        if (lastWornDate.isEmpty() || lastWornDate == "N/A") return false

        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return try {
            val lastWorn = formatter.parse(lastWornDate) ?: return false
            val diffInMillis = Date().time - lastWorn.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val diffInMonths = diffInDays / 30 // Approximate conversion
            diffInMonths >= 3
        } catch (e: ParseException) {
            false
        }
    }

    private fun showEmptyMessage(root: View) {
        val fullMessage = getString(R.string.no_items_available)

        // Find the index of the "Add a new item?" part
        val start = fullMessage.indexOf("Add a new item?")
        val end = start + "Add a new item?".length

        // Create a SpannableString to apply different styles
        val spannableString = SpannableString(fullMessage)

        // Make "Add a new item?" clickable and prevent the cyan highlight
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showAddItemFragment() // Open the Add Item Fragment when clicked
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(requireContext(), R.color.color_items)
                ds.bgColor = ContextCompat.getColor(requireContext(), R.color.faded_pink)
            }
        }

        // Apply clickable span to "Add a new item?"
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the SpannableString to the TextView
        val emptyMessage: TextView = root.findViewById(R.id.homeEmptyMessage)
        emptyMessage.text = spannableString
        emptyMessage.movementMethod = LinkMovementMethod.getInstance() // Make links clickable
    }

    private fun loadNotificationState() {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isNotifIconOn = sharedPreferences.getBoolean(NOTIF_STATE_KEY, false) // Default to false if not set

        // Set the icon based on the loaded state
        if (isNotifIconOn) {
            iconNotif.setImageResource(R.drawable.icon_notif_on)
            startNotificationWorker() // Start the worker if notifications are enabled
        } else {
            iconNotif.setImageResource(R.drawable.icon_notif_off)
        }
    }

    private fun updateEmptyState(root: View) {
        val emptyMessage = root.findViewById<TextView>(R.id.homeEmptyMessage)
        if (sortedRecentItems.isEmpty() && sortedFavoriteItems.isEmpty() && sortedUnusedItems.isEmpty()) {
            showEmptyMessage(root)
            emptyMessage.visibility = View.VISIBLE
        } else {
            emptyMessage.visibility = View.GONE
        }
    }

    private fun showAddItemFragment() {
        // Navigate to the Add Item Fragment or show a dialog
        findNavController().navigate(R.id.action_navigation_home_to_addItemFragment)
    }


    @SuppressLint("ObjectAnimatorBinding")
    private fun zoomInIcon(imageView: ImageView) {
        // Animate zoom-in effect
        val animatorX = ObjectAnimator.ofFloat(imageView, "scaleX", 0.0f, 1.0f)
        val animatorY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.0f, 1.0f)

        animatorX.duration = 300 // Duration for animation
        animatorY.duration = 300 // Duration for animation

        animatorX.start()
        animatorY.start()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupIconCurrentInteraction() {
        var dX = 0f
        var dY = 0f
        var isMoving = false
        val margin = 3 // Margin in dp to snap to screen edges
        val density = resources.displayMetrics.density
        val snapMargin = (margin * density).toInt()

        iconCurrentImageView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    isMoving = false
                    true // Capture the touch event
                }
                MotionEvent.ACTION_MOVE -> {
                    // Consider it a move only if the finger has moved a significant distance
                    if (Math.abs(event.rawX - (v.x - dX)) > 10 || Math.abs(event.rawY - (v.y - dY)) > 10) {
                        isMoving = true
                        v.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        // This was a click, not a drag
                        v.performClick()
                    } else {
                        // This was a drag, so snap to the nearest edge and consider bottom navigation bar
                        val parent = v.parent as View
                        val parentWidth = parent.width
                        val parentHeight = parent.height

                        // Get height of bottom navigation bar
                        val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)

                        val finalX = if (v.x < parentWidth / 2) {
                            snapMargin.toFloat() // Snap to the left edge
                        } else {
                            (parentWidth - v.width - snapMargin).toFloat() // Snap to the right edge
                        }

                        val maxBottomY = (parentHeight - v.height - bottomNavHeight - snapMargin).toFloat()
                        val finalY = v.y.coerceIn(
                            snapMargin.toFloat(),
                            maxBottomY
                        )

                        v.animate()
                            .x(finalX)
                            .y(finalY)
                            .setDuration(300)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }

        // Set up click listener for iconCurrent
        iconCurrentImageView.setOnClickListener {
            // This will only be called if it's a tap, not a drag
            toggleIconCurrentState()
        }
    }

    private fun toggleIconCurrentState() {
        if (!isIconCurrentExpanded) {
            // Show notif and today's outfit buttons
            iconNotif.visibility = View.VISIBLE
            iconOutfit.visibility = View.VISIBLE

            // Show the dark overlay with animation (fade in)
            darkOverlay.visibility = View.VISIBLE
            val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            darkOverlay.startAnimation(fadeInAnimation)

            // Show the "tap to return" text when the icons are visible
            tapToReturn.visibility = View.VISIBLE

            isIconCurrentExpanded = true
        } else {
            collapseButtonsAndRestoreIcon()
        }
    }

    fun collapseButtonsAndRestoreIcon() {
        // Hide notif and today's outfit buttons
        val fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        iconNotif.startAnimation(fadeOutAnimation)
        iconOutfit.startAnimation(fadeOutAnimation)
        tapToReturn.startAnimation(fadeOutAnimation)

        fadeOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Ensure icon is visible and zoomed in
                iconNotif.visibility = View.GONE
                iconOutfit.visibility = View.GONE
                tapToReturn.visibility = View.GONE

                // Explicitly make iconCurrentImageView visible and zoom in
                iconCurrentImageView.visibility = View.VISIBLE
                zoomInIcon(iconCurrentImageView)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Hide the overlay with animation (fade out)
        val fadeOutOverlayAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        darkOverlay.startAnimation(fadeOutOverlayAnimation)
        darkOverlay.visibility = View.GONE

        isIconCurrentExpanded = false
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
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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