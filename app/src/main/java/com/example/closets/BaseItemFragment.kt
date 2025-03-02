package com.example.closets

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory
import kotlin.math.abs

abstract class BaseItemFragment : Fragment() {
    protected lateinit var itemViewModel: ItemViewModel
    protected var loadingView: View? = null
    var isViewingSearchResults = false
    protected var _hasActiveFilters = false
    protected var appliedTypes: List<String>? = null
    protected var appliedColors: List<String>? = null

    protected val typeOptions = listOf(
        "Top", "Bottom", "Outerwear", "Dress", "Shoes", "Other"
    )

    protected val colorOptions = mapOf(
        "red" to "#ff0000",
        "orange" to "#ffa500",
        "yellow" to "#ffff00",
        "green" to "#00ff00",
        "blue" to "#0000ff",
        "pink" to "#ff6eca",
        "purple" to "#800080",
        "white" to "#ffffff",
        "beige" to "#f5f5dd",
        "gray" to "#808080",
        "brown" to "#5e3e2b",
        "black" to "#000000"
    )

    companion object {
        private var currentToast: Toast? = null

        fun showToast(context: Context, message: String) {
            currentToast?.cancel() // cancel the previous toast
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show() // show the new toast
            }
        }
    }

    // Abstract properties for UI-specific elements.
    protected abstract val adapter: RecyclerView.Adapter<*>
    protected abstract val binding: View

    protected fun initializeViewModel(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]
    }

    // A simpler toast method if needed.
    protected fun showToast(context: Context, message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    open fun hasActiveFilters(): Boolean {
        return appliedTypes != null || appliedColors != null || _hasActiveFilters
    }

    // method to reset all filters and clear the search input.
    open fun clearAllFilters() {
        appliedTypes = null
        appliedColors = null
        _hasActiveFilters = false
        resetToOriginalList()    // Subclass must define how to restore the original list.
        clearSearchInput()       // Subclass must define how to clear its search input.
    }

    // Abstract methods to be implemented by subclasses
    protected abstract fun updateItemsCount(count: Int)
    protected abstract fun clearSearchInput()
    protected abstract fun resetToOriginalList()

    // --- Color Utility Methods ---
    protected fun hexToHSV(hex: String): FloatArray {
        val cleanHex = hex.replace("#", "")
        val r = cleanHex.substring(0, 2).toInt(16)
        val g = cleanHex.substring(2, 4).toInt(16)
        val b = cleanHex.substring(4, 6).toInt(16)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        return hsv
    }

    protected fun colorDistance(color1: FloatArray, color2: FloatArray): Double {
        // Weight factors for Hue, Saturation, and Value
        val hueWeight = 1.0
        val satWeight = 2.0
        val valWeight = 1.0

        // Calculate wrapped hue difference
        var hueDiff = abs(color1[0] - color2[0])
        if (hueDiff > 180) hueDiff = 360 - hueDiff
        hueDiff /= 180 // Normalize to [0,1]

        // Calculate saturation and value differences
        val satDiff = abs(color1[1] - color2[1])
        val valDiff = abs(color1[2] - color2[2])

        // Weighted distance
        return hueDiff * hueWeight +
                satDiff * satWeight +
                valDiff * valWeight
    }

    protected fun findClosestColor(itemHex: String, colorOptions: Map<String, String>): String {
        val itemHSV = hexToHSV(itemHex)
        var closestColor = colorOptions.keys.first()
        var minDistance = Double.MAX_VALUE

        val beigeHex = "#F5F5DD"
        val beigeHSV = hexToHSV(beigeHex)

        // Special case checks based on HSV values
        when {
            // Check for exact color matches
            itemHex.equals(beigeHex, ignoreCase = true) -> return "beige"
            itemHex.equals("#FFFFFF", ignoreCase = true) -> return "white"
            itemHex.equals("#000000", ignoreCase = true) -> return "black"
            itemHex.equals("#808080", ignoreCase = true) -> return "gray"
            itemHex.equals("#FF0000", ignoreCase = true) -> return "red"
            itemHex.equals("#FFA500", ignoreCase = true) -> return "orange"
            itemHex.equals("#FFFF00", ignoreCase = true) -> return "yellow"
            itemHex.equals("#00FF00", ignoreCase = true) -> return "green"
            itemHex.equals("#0000FF", ignoreCase = true) -> return "blue"
            itemHex.equals("#FF69B4", ignoreCase = true) -> return "pink"
            itemHex.equals("#800080", ignoreCase = true) -> return "purple"
            itemHex.equals("#5E3E2B", ignoreCase = true) -> return "brown"
        }

        // Check for beige based on HSV values
        if (colorDistance(itemHSV, beigeHSV) < 0.1) return "beige"

        // For other colors, find the closest match
        colorOptions.forEach { (colorName, colorHex) ->
            // Calculate the HSV for the color option
            val optionHSV = hexToHSV(colorHex)
            val distance = colorDistance(itemHSV, optionHSV)

            // Check if the distance is within a certain threshold
            if (distance < minDistance) {
                minDistance = distance
                closestColor = colorName
            }
        }

        // Set a maximum distance threshold for fallback colors
        if (minDistance > 1.5) {
            // If no good match is found, fallback to gray for very desaturated colors
            if (itemHSV[1] < 0.2) return "gray"
            // For brown-ish colors
            if (itemHSV[0] in 20f..40f && itemHSV[1] > 0.2 && itemHSV[2] < 0.7) return "brown"
        }

        return closestColor
    }
}