package com.example.closets.ui.home

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.MainActivity
import com.example.closets.R
import com.example.closets.ui.viewmodels.ItemViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class HomeUIManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val navController: NavController,
    private val itemViewModel: ItemViewModel
) {
    // UI elements
    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var idleItemsRecyclerView: RecyclerView
    private lateinit var itemsTitle: TextView
    private lateinit var favoritesTitle: TextView
    private lateinit var idleItemsTitle: TextView
    lateinit var tapToReturn: TextView
    lateinit var iconCurrentImageView: View
    lateinit var iconNotif: View
    lateinit var iconOutfit: View
    lateinit var darkOverlay: View

    // Data
    private var sortedRecentItems: List<HomeItem> = emptyList()
    private var sortedFavoriteItems: List<HomeItem> = emptyList()
    private var sortedUnusedItems: List<HomeItem> = emptyList()
    private var isIconCurrentExpanded = false

    fun initializeViews(root: View) {
        // Initialize RecyclerViews and TextViews
        itemsRecyclerView = root.findViewById(R.id.recycler_view)
        favoritesRecyclerView = root.findViewById(R.id.favorites_recycler_view)
        idleItemsRecyclerView = root.findViewById(R.id.idle_items_recycler_view)
        itemsTitle = root.findViewById(R.id.items_title)
        favoritesTitle = root.findViewById(R.id.favorites_title)
        idleItemsTitle = root.findViewById(R.id.idle_items_title)
        tapToReturn = root.findViewById(R.id.tap_to_return)

        // Initialize icon views
        iconCurrentImageView = root.findViewById(R.id.icon_current)
        iconNotif = root.findViewById(R.id.icon_notif)
        iconOutfit = root.findViewById(R.id.icon_outfit)

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

        setupSeeAllButtons(root)
    }

    private fun setupSeeAllButtons(root: View) {
        // Find "See All" TextViews
        val itemsManage = root.findViewById<TextView>(R.id.items_manage)
        val favoritesManage = root.findViewById<TextView>(R.id.favorites_manage)
        val idleItemsManage = root.findViewById<TextView>(R.id.idle_items_manage)

        // Hide "See All" until data is loaded
        itemsManage.visibility = View.GONE
        favoritesManage.visibility = View.GONE
        idleItemsManage.visibility = View.GONE

        // Set click listeners for "See All" with navigation image updates
        itemsManage.setOnClickListener {
            (context as? MainActivity)?.findViewById<BottomNavigationView>(R.id.nav_view)?.apply {
                selectedItemId = R.id.navigation_items
            }
        }

        favoritesManage.setOnClickListener {
            (context as? MainActivity)?.findViewById<BottomNavigationView>(R.id.nav_view)?.apply {
                selectedItemId = R.id.navigation_favorites
            }
        }

        idleItemsManage.setOnClickListener {
            (context as? MainActivity)?.findViewById<BottomNavigationView>(R.id.nav_view)?.apply {
                selectedItemId = R.id.navigation_unused
            }
        }
    }

    fun setupObservers(root: View) {
        // observe LiveData for items from database
        itemViewModel.items.observe(lifecycleOwner) { clothingItems ->
            lifecycleOwner.lifecycleScope.launch {
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

                updateRecentItemsUI(root)
                updateEmptyState(root)
            }
        }

        // observe favorite items
        itemViewModel.favoriteItems.observe(lifecycleOwner) { favoriteItems ->
            lifecycleOwner.lifecycleScope.launch {
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

                updateFavoriteItemsUI(root)
                updateEmptyState(root)
            }
        }

        // observe unused items
        itemViewModel.items.observe(lifecycleOwner) { items ->
            lifecycleOwner.lifecycleScope.launch {
                // Filter for unused items and sort by longest unused duration first
                sortedUnusedItems = items
                    .filter { item -> hasBeenUnusedForAtLeastThreeMonths(item.lastWornDate) }
                    .sortedByDescending { item ->
                        val lastWornDate = parseLastWornDate(item.lastWornDate)
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

                updateUnusedItemsUI(root)
                updateEmptyState(root)
            }
        }
    }

    private fun updateRecentItemsUI(root: View) {
        val itemsManage = root.findViewById<TextView>(R.id.items_manage)

        if (sortedRecentItems.isNotEmpty()) {
            itemsTitle.visibility = View.VISIBLE
            itemsManage.visibility = View.VISIBLE
            itemsRecyclerView.visibility = View.VISIBLE

            val adapter = HomeItemAdapter(sortedRecentItems) { homeItem ->
                val bundle = Bundle().apply {
                    putInt("item_id", homeItem.id)
                }
                navController.navigate(R.id.action_homeFragment_to_itemInfoFragment, bundle)
            }
            itemsRecyclerView.adapter = adapter
            itemsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            applyFadeInAnimation(itemsRecyclerView)
        } else {
            itemsTitle.visibility = View.GONE
            itemsManage.visibility = View.GONE
            itemsRecyclerView.visibility = View.GONE
        }
    }

    private fun updateFavoriteItemsUI(root: View) {
        val favoritesManage = root.findViewById<TextView>(R.id.favorites_manage)

        if (sortedFavoriteItems.isNotEmpty()) {
            favoritesTitle.visibility = View.VISIBLE
            favoritesManage.visibility = View.VISIBLE
            favoritesRecyclerView.visibility = View.VISIBLE

            val adapter = HomeItemAdapter(sortedFavoriteItems) { homeItem ->
                val bundle = Bundle().apply {
                    putInt("item_id", homeItem.id)
                }
                navController.navigate(R.id.action_homeFragment_to_itemInfoFragment, bundle)
            }
            favoritesRecyclerView.adapter = adapter
            favoritesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            applyFadeInAnimation(favoritesRecyclerView)
        } else {
            favoritesTitle.visibility = View.GONE
            favoritesManage.visibility = View.GONE
            favoritesRecyclerView.visibility = View.GONE
        }
    }

    private fun updateUnusedItemsUI(root: View) {
        val idleItemsManage = root.findViewById<TextView>(R.id.idle_items_manage)

        if (sortedUnusedItems.isNotEmpty()) {
            idleItemsTitle.visibility = View.VISIBLE
            idleItemsManage.visibility = View.VISIBLE
            idleItemsRecyclerView.visibility = View.VISIBLE

            val adapter = HomeItemAdapter(sortedUnusedItems) { homeItem ->
                val bundle = Bundle().apply {
                    putInt("item_id", homeItem.id)
                }
                navController.navigate(R.id.action_homeFragment_to_itemInfoFragment, bundle)
            }
            idleItemsRecyclerView.adapter = adapter
            idleItemsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            applyFadeInAnimation(idleItemsRecyclerView)
        } else {
            idleItemsTitle.visibility = View.GONE
            idleItemsManage.visibility = View.GONE
            idleItemsRecyclerView.visibility = View.GONE
        }
    }

    private fun hasBeenUnusedForAtLeastThreeMonths(lastWornDate: String): Boolean {
        if (lastWornDate.isEmpty() || lastWornDate == "N/A") {
            return true // "N/A" as longest unused
        }

        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return try {
            val lastWorn = formatter.parse(lastWornDate) ?: return true
            val today = Date()
            val diffInMillis = today.time - lastWorn.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val diffInMonths = diffInDays / 30

            diffInMonths >= 3
        } catch (e: ParseException) {
            Log.e("UnusedFragment", "Error parsing date: $lastWornDate", e)
            true // default to unused if there's an error
        }
    }

    private fun parseLastWornDate(dateStr: String): Date {
        return if (dateStr.equals("N/A", ignoreCase = true) || dateStr.isEmpty()) {
            Date(0)
        } else {
            try {
                val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                formatter.parse(dateStr) ?: Date(0)
            } catch (e: ParseException) {
                Date(0)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupIconCurrentInteraction() {
        var dX = 0f
        var dY = 0f
        var isMoving = false
        val margin = 3 // Margin in dp to snap to screen edges
        val density = context.resources.displayMetrics.density
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
                    if (abs(event.rawX - (v.x - dX)) > 10 || abs(event.rawY - (v.y - dY)) > 10) {
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
                        v.performClick()
                    } else {
                        val parent = v.parent as View
                        val parentWidth = parent.width
                        val parentHeight = parent.height

                        val bottomNavHeight = context.resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
                        val finalX = if (v.x < parentWidth / 2) {
                            snapMargin.toFloat()
                        } else {
                            (parentWidth - v.width - snapMargin).toFloat()
                        }
                        val maxBottomY = (parentHeight - v.height - bottomNavHeight - snapMargin).toFloat()
                        val finalY = v.y.coerceIn(snapMargin.toFloat(), maxBottomY)

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

        iconCurrentImageView.setOnClickListener {
            toggleIconCurrentState()
        }
    }

    fun toggleIconCurrentState() {
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
                zoomInIcon(iconCurrentImageView as ImageView)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Hide the overlay with animation (fade out)
        val fadeOutOverlayAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        darkOverlay.startAnimation(fadeOutOverlayAnimation)
        darkOverlay.visibility = View.GONE

        isIconCurrentExpanded = false
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun zoomInIcon(imageView: ImageView) {
        // Animate zoom-in effect
        val animatorX = ObjectAnimator.ofFloat(imageView, "scaleX", 0.0f, 1.0f)
        val animatorY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.0f, 1.0f)

        animatorX.duration = 300 // Duration for animation
        animatorY.duration = 300 // Duration for animation

        animatorX.start()
        animatorY.start()
    }

    private fun showEmptyMessage(root: View) {
        val fullMessage = context.getString(R.string.no_items_available)

        val start = fullMessage.indexOf("Add a new item?")
        val end = start + "Add a new item?".length

        val spannableString = SpannableString(fullMessage)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navController.navigate(R.id.action_navigation_home_to_addItemFragment)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(context, R.color.color_items)
                ds.bgColor = ContextCompat.getColor(context, R.color.faded_pink)
            }
        }

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val emptyMessage: TextView = root.findViewById(R.id.homeEmptyMessage)
        emptyMessage.text = spannableString
        emptyMessage.movementMethod = LinkMovementMethod.getInstance() // Make links clickable
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

    private fun applyFadeInAnimation(view: View) {
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        view.startAnimation(fadeInAnimation)
    }
}