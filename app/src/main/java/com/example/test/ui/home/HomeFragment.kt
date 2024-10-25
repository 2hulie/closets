package com.example.test.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.ui.ItemAdapter
import com.example.test.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {


    private lateinit var recyclerView: RecyclerView
    private lateinit var favouritesRecyclerView: RecyclerView
    private lateinit var idleItemsRecyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var favouritesAdapter: ItemAdapter
    private lateinit var idleItemsAdapter: ItemAdapter
    private lateinit var itemsTitle: TextView
    private lateinit var favouritesTitle: TextView
    private lateinit var idleItemsTitle: TextView

    private lateinit var iconCurrentImageView: ImageView
    private lateinit var iconNotif: ImageView
    private lateinit var iconOutfit: ImageView


    private var isNotifIconOn: Boolean = true

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize RecyclerViews and TextViews
        recyclerView = root.findViewById(R.id.recycler_view) // Initialize items RecyclerView
        itemsTitle = root.findViewById(R.id.items_title) // Initialize items title
        favouritesRecyclerView = root.findViewById(R.id.favorites_recycler_view) // Initialize favorites RecyclerView
        favouritesTitle = root.findViewById(R.id.favorites_title) // Initialize favorites title
        idleItemsRecyclerView = root.findViewById(R.id.idle_items_recycler_view) // Initialize idle items RecyclerView
        idleItemsTitle = root.findViewById(R.id.idle_items_title) // Initialize idle items title

        // Initialize icon views
        iconCurrentImageView = root.findViewById(R.id.icon_current)
        iconNotif = root.findViewById(R.id.icon_notif)
        iconOutfit = root.findViewById(R.id.icon_outfit)

        // Set the initial visibility of notification and outfit icons
        iconNotif.visibility = View.GONE
        iconOutfit.visibility = View.GONE

        // List of items to display
        val itemList = listOf(
            Item("Cap", R.drawable.cap),
            Item("Dress", R.drawable.dress),
            Item("Shirt", R.drawable.shirt),
            Item("Shoes", R.drawable.shoes),
            Item("Shorts", R.drawable.shorts),
            Item("Skirt", R.drawable.skirt)
        )

        // List of favorite items
        val favouriteItemList = listOf(
            Item("Shirt", R.drawable.shirt),
            Item("Skirt", R.drawable.skirt),
            Item("Cap", R.drawable.cap),
            Item("Shoes", R.drawable.shoes)
        )

        // List of idle items
        val idleItemList = listOf(
            Item("Shorts", R.drawable.shorts),
            Item("Dress", R.drawable.dress),
            Item("Shoes", R.drawable.shoes),
            Item("Cap", R.drawable.cap)
        )

        // Set the title with the number of items
        this.itemsTitle.text = "Items (${itemList.size})"
        this.favouritesTitle.text = "Favorites (${favouriteItemList.size})" // Set favorites title
        this.idleItemsTitle.text = "Idle Items (${idleItemList.size})" // Set idle items title

        // Set up RecyclerView for items
        itemAdapter = ItemAdapter(itemList) { item ->
            // Add a delay
            val delayMillis = 150L
            when (item.name) {
                "Shirt" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShirtFragment)
                }, delayMillis)
                "Cap" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoCapFragment)
                }, delayMillis)

                "Dress" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoDressFragment)
                }, delayMillis)

                "Shoes" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShoesFragment)
                }, delayMillis)

                "Shorts" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShortsFragment)
                }, delayMillis)

                "Skirt" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoSkirtFragment)
                }, delayMillis)
            }

            // Activate the "Items" icon in the bottom navigation bar
            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
            bottomNavigationView.selectedItemId = R.id.navigation_items
        }

        recyclerView.adapter = itemAdapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Set up RecyclerView for favorites
        favouritesAdapter = ItemAdapter(favouriteItemList) { item ->
            // Add a delay
            val delayMillis = 150L

            when (item.name) {
                "Shirt" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShirtFragment)
                }, delayMillis)
                "Cap" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoCapFragment)
                }, delayMillis)
                "Shoes" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShoesFragment)
                }, delayMillis)
                "Skirt" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoSkirtFragment)
                }, delayMillis)
            }
        }
        favouritesRecyclerView.adapter = favouritesAdapter
        favouritesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Set up RecyclerView for idle items
        idleItemsAdapter = ItemAdapter(idleItemList) { item ->
            // Add a delay
            val delayMillis = 150L

            when (item.name) {
                "Shorts" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShortsFragment)
                }, delayMillis)
                "Dress" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoDressFragment)
                }, delayMillis)
                "Shoes" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoShoesFragment)
                }, delayMillis)
                "Cap" -> recyclerView.postDelayed({
                    findNavController().navigate(R.id.action_homeFragment_to_itemInfoCapFragment)
                }, delayMillis)
            }
        }
        idleItemsRecyclerView.adapter = idleItemsAdapter
        idleItemsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        return root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollView = view.findViewById<ScrollView>(R.id.scroll_view)

        // Load the slide-down animation for the home image
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_down)
        val homeImageView = view.findViewById<ImageView>(R.id.home_image)
        homeImageView.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

        // Scroll to the bottom on fragment creation
        scrollView.post {
            scrollView.scrollTo(0, scrollView.getChildAt(0).height) // Start at the bottom
            smoothScrollTo(scrollView, 0, 800) // Scroll up to the top over 0.8 seconds
        }

        // Set up click listener for icon_notif
        iconNotif.setOnClickListener {
            toggleNotificationIcon()
        }

        // Load the slide-in animation for iconCurrent
        val slideInAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_in)
        iconCurrentImageView.visibility = View.VISIBLE
        iconCurrentImageView.startAnimation(slideInAnimation)

        // Set up click listener for iconCurrent
        iconCurrentImageView.setOnClickListener {
            if (iconNotif.visibility == View.GONE && iconOutfit.visibility == View.GONE) {
                // Show the buttons with slide-up animation
                iconNotif.visibility = View.VISIBLE
                iconOutfit.visibility = View.VISIBLE

                val slideUpAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_in)
                iconNotif.startAnimation(slideUpAnimation)
                iconOutfit.startAnimation(slideUpAnimation)

                // Reduce the size of iconCurrent
                reduceIconSize(iconCurrentImageView)
            } else {
                // Hide the buttons with slide-down animation
                val slideDownAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_out)
                iconNotif.startAnimation(slideDownAnimation)
                iconOutfit.startAnimation(slideDownAnimation)

                slideDownAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        iconNotif.visibility = View.GONE
                        iconOutfit.visibility = View.GONE
                        resetIconSize(iconCurrentImageView)
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }
        }

        // Add a click listener for iconOutfit to show "Today's Outfit" bottom sheet
        iconOutfit.setOnClickListener {
            val bottomSheet = TodayOutfitBottomSheet()
            bottomSheet.show(parentFragmentManager, "TodayOutfitBottomSheet")
        }
    }

    private fun smoothScrollTo(scrollView: ScrollView, targetY: Int, duration: Int) {
        // Calculate the current scroll position
        val startY = scrollView.scrollY
        val distance = targetY - startY

        // Create a ValueAnimator
        val animator = ValueAnimator.ofInt(startY, targetY)
        animator.duration = duration.toLong()
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            scrollView.scrollTo(0, animatedValue)
        }

        // Start the animation
        animator.start()
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun reduceIconSize(imageView: ImageView) {
        // Reduce size with animation
        val animatorWidth = ObjectAnimator.ofInt(imageView, "width", imageView.width, (imageView.width * 0.95).toInt())
        val animatorHeight = ObjectAnimator.ofInt(imageView, "height", imageView.height, (imageView.height * 0.95).toInt())

        animatorWidth.addUpdateListener { animation ->
            val layoutParams = imageView.layoutParams
            layoutParams.width = animation.animatedValue as Int
            imageView.layoutParams = layoutParams
        }

        animatorHeight.addUpdateListener { animation ->
            val layoutParams = imageView.layoutParams
            layoutParams.height = animation.animatedValue as Int
            imageView.layoutParams = layoutParams
        }

        animatorWidth.duration = 300 // Set duration for animation
        animatorHeight.duration = 300 // Set duration for animation

        // Start animations
        animatorWidth.start()
        animatorHeight.start()
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun resetIconSize(imageView: ImageView) {
        // Reset size with animation
        val originalSize = resources.getDimension(R.dimen.icon_current_size).toInt()

        val animatorWidth = ObjectAnimator.ofInt(imageView, "width", imageView.width, originalSize)
        val animatorHeight = ObjectAnimator.ofInt(imageView, "height", imageView.height, originalSize)

        animatorWidth.addUpdateListener { animation ->
            val layoutParams = imageView.layoutParams
            layoutParams.width = animation.animatedValue as Int
            imageView.layoutParams = layoutParams
        }

        animatorHeight.addUpdateListener { animation ->
            val layoutParams = imageView.layoutParams
            layoutParams.height = animation.animatedValue as Int
            imageView.layoutParams = layoutParams
        }

        animatorWidth.duration = 300 // Set duration for animation
        animatorHeight.duration = 300 // Set duration for animation

        // Start animations
        animatorWidth.start()
        animatorHeight.start()
    }

    private fun toggleNotificationIcon() {
        // Toggle the icon state
        if (isNotifIconOn) {
            iconNotif.setImageResource(R.drawable.icon_notif_off)
        } else {
            iconNotif.setImageResource(R.drawable.icon_notif_on)
        }
        isNotifIconOn = !isNotifIconOn // Toggle the state variable
    }

    override fun onResume() {
        super.onResume()
        // Reset icon to icon_notif_on if the user navigates back to this fragment
        if (!isNotifIconOn) {
            iconNotif.setImageResource(R.drawable.icon_notif_on)
            isNotifIconOn = true // Reset the state
        }
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

        // Get the decor view of the activity's window
        val decorView = requireActivity().window.decorView

        // Make the status bar content (icons/text) white
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = 0 // Clears any previously set flags to force light icon
    }
}