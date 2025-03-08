package com.example.closets.ui.home

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.R
import com.example.closets.SharedViewModel
import com.example.closets.repository.AppDatabase
import com.example.closets.ui.entities.Item
import com.example.closets.ui.items.ClothingItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayOutfitBottomSheet(private var checkedItems: List<ClothingItem>) : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitItemAdapter: TodayOutfitItemAdapter
    private lateinit var backButton: ImageView
    private lateinit var pencilIcon: ImageView
    private lateinit var saveOutfitButton: ImageView
    private lateinit var emptyStateText: TextView
    private lateinit var currentTimeDate: TextView
    private var currentDay = ""
    private val timeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTimeAndDate()
            checkForDayChange()
            timeHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private var currentToast: Toast? = null

        fun showToast(context: Context, message: String) {
            currentToast?.cancel() // cancel the previous toast
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show() // show the new toast
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_today_outfit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trace = FirebasePerformance.getInstance().newTrace("todayOutfitBottomSheet_onViewCreated")
        trace.start()

        initializeUI(view)
        updateTimeAndDate()
        resetForNewDayIfNeeded()

        // always observe changes in checked items
        val sharedViewModel: SharedViewModel by activityViewModels()
        sharedViewModel.checkedItems.observe(viewLifecycleOwner) { items ->
            // Only update if items actually changed to prevent unnecessary resets
            if (items.size != checkedItems.size || !items.containsAll(checkedItems)) {
                checkedItems = items
                outfitItemAdapter.updateItems(items)
                updateUIState()
                updateCheckedItemsInPrefs(items) // update the SharedPreferences when items change
            }
        }

        setUpRecyclerView()
        setClickListeners()

        trace.stop()
    }

    private fun updateCheckedItemsInPrefs(items: List<ClothingItem>) {
        val checkedPrefs = requireContext().getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
        val checkedItemsSet = items.map { it.id.toString() }.toSet()

        checkedPrefs.edit()
            .putStringSet("CheckedItems", checkedItemsSet)
            .apply()
    }

    private fun initializeUI(view: View) {
        recyclerView = view.findViewById(R.id.todays_outfit_recycler_view)
        backButton = view.findViewById(R.id.icon_back)
        pencilIcon = view.findViewById(R.id.icon_pencil)
        emptyStateText = view.findViewById(R.id.empty_state_text)
        currentTimeDate = view.findViewById(R.id.current_time_date)
        saveOutfitButton = view.findViewById(R.id.icon_save_outfit)
    }

    private fun updateTimeAndDate() {
        val dateFormat = SimpleDateFormat("hh:mm:ss a | MMMM dd, yyyy", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        currentTimeDate.text = currentTime
        if (currentDay.isEmpty()) {
            currentDay = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        }
    }

    private fun checkForDayChange() {
        val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        if (currentDay != today) {
            // Day has changed
            currentDay = today
            resetForNewDay()
        }
    }

    private fun resetForNewDay() {
        checkedItems = emptyList()
        val checkedPrefs = requireContext().getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
        checkedPrefs.edit().clear().apply()
        outfitItemAdapter.updateItems(emptyList())
        updateUIState()
        showToast(requireContext(), "A new day has begun! Your outfit has been reset.")
    }

    private fun resetForNewDayIfNeeded() {
        val outfitPrefs = requireContext().getSharedPreferences("OutfitPrefs", Context.MODE_PRIVATE)
        val savedDate = outfitPrefs.getString("outfit_saved_date", null)  // null if not saved
        val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        currentDay = today // Set the current day

        // only clear if an outfit was saved and that saved date is not today.
        if (savedDate != null && savedDate != today) {
            checkedItems = emptyList()
            val checkedPrefs = requireContext().getSharedPreferences("CheckedItemsPrefs", Context.MODE_PRIVATE)
            checkedPrefs.edit().clear().apply()

            // Also clear the outfit saved date to ensure consistency
            outfitPrefs.edit().remove("outfit_saved_date").apply()

            // Update the adapter
            if (::outfitItemAdapter.isInitialized) {
                outfitItemAdapter.updateItems(emptyList())
            }
        }
    }
    private fun setUpRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with ${checkedItems.size} items")
        outfitItemAdapter = TodayOutfitItemAdapter(checkedItems)

        recyclerView.apply {
            adapter = outfitItemAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }

        updateUIState()
    }

    private fun updateUIState() {
        if (checkedItems.isEmpty()) {
            Log.d(TAG, "No items to display")
            recyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
            saveOutfitButton.visibility = View.GONE // Hide save button when empty
        } else {
            Log.d(TAG, "Displaying ${checkedItems.size} items")
            recyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE

            // only show save button if outfit isn't already saved for today
            saveOutfitButton.visibility = if (isOutfitSavedForToday()) View.GONE else View.VISIBLE
        }

        // always hide pencil icon if outfit saved for today
        pencilIcon.visibility = if (isOutfitSavedForToday()) View.GONE else View.VISIBLE
        if (isOutfitSavedForToday()) {
            adjustBackgroundHeightForSavedOutfit()
        }
    }

    private fun adjustBackgroundHeightForSavedOutfit() {
        val backgroundImage: ImageView? = view?.findViewById(R.id.todays_outfit_background)
        backgroundImage?.let {
            val newHeight = dpToPx(350)
            val params = it.layoutParams
            params.height = newHeight
            it.layoutParams = params
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun setClickListeners() {
        backButton.setOnClickListener { dismiss() }

        pencilIcon.setOnClickListener {
            dismiss()
            parentFragment?.findNavController()
                ?.navigate(R.id.action_navigation_home_to_currentItemFragment)
        }

        saveOutfitButton.setOnClickListener {
            showSaveOutfitConfirmationDialog()
        }
    }

    private fun showSaveOutfitConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_save_outfit_confirmation, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnYes = dialogView.findViewById<ImageView>(R.id.btn_yes)
        val btnNo = dialogView.findViewById<ImageView>(R.id.btn_no)

        btnYes.setOnClickListener {
            lifecycleScope.launch {
                saveOutfit()
            }
            dialog.dismiss()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun saveOutfit() {
        withContext(Dispatchers.IO) {
            val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            val database = AppDatabase.getDatabase(requireContext())
            val itemDao = database.itemDao()

            checkedItems.forEach { item ->
                item.wornTimes += 1
                item.lastWornDate = currentDate
                itemDao.updateItem(convertToItem(item))
            }
            markOutfitSavedForToday(currentDate)
        }

        withContext(Dispatchers.Main) {
            showToast(requireContext(), "Today's Outfit saved!")
            saveOutfitButton.visibility = View.GONE
            pencilIcon.visibility = View.GONE
            dismiss()
        }
    }

    private fun markOutfitSavedForToday(date: String) {
        val prefs = requireContext().getSharedPreferences("OutfitPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("outfit_saved_date", date).apply()
    }

    private fun isOutfitSavedForToday(): Boolean {
        val prefs = requireContext().getSharedPreferences("OutfitPrefs", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("outfit_saved_date", "")
        val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        return savedDate == today
    }

    private fun convertToItem(clothingItem: ClothingItem): Item {
        return Item(
            id = clothingItem.id,
            name = clothingItem.name,
            type = clothingItem.type,
            color = clothingItem.color,
            wornTimes = clothingItem.wornTimes,
            lastWornDate = clothingItem.lastWornDate ?: "",
            imageUri = clothingItem.imageUri,
            isFavorite = clothingItem.isFavorite
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()
        timeHandler.post(timeRunnable)
        dialog?.window?.apply {
            setDimAmount(0.3f)
            setBackgroundDrawableResource(android.R.color.transparent)
            val todaysOutfitColor = resources.getColor(R.color.todays_outfit, null)
            this.navigationBarColor = todaysOutfitColor
            this.insetsController?.apply {
                setSystemBarsAppearance(
                    0, // Clear any light navigation bar settings
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
                setSystemBarsAppearance(
                    0, // Clear any light status bar settings
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        timeHandler.removeCallbacks(timeRunnable)
    }
}