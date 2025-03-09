package com.example.closets.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.example.closets.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.perf.FirebasePerformance

class FilterBottomSheetDialog(
    private val typeOptions: List<String>,
    private val colorOptions: Map<String, String>,
    private val onApplyFilters: (List<String>?, List<String>?) -> Unit,
    private val onResetFilters: () -> Unit,
    private val preselectedTypes: List<String>? = null,
    private val preselectedColors: List<String>? = null
) : BottomSheetDialogFragment() {

    private lateinit var typeCheckBoxes: List<CheckBox>
    private lateinit var backButton: ImageView
    private lateinit var colorViews: Map<ImageView, ImageView>
    private val selectedColors = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.filter_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trace = FirebasePerformance.getInstance().newTrace("filterBottomSheetDialog_onViewCreated")
        trace.start()

        backButton = view.findViewById(R.id.icon_back)
        backButton.setOnClickListener { dismiss() }

        val infoIcon = view.findViewById<ImageView>(R.id.icon_info)
        infoIcon.setOnClickListener {
            showInfoDialog()
        }

        typeCheckBoxes = listOf(
            view.findViewById(R.id.type_top),
            view.findViewById(R.id.type_bottom),
            view.findViewById(R.id.type_outerwear),
            view.findViewById(R.id.type_dress),
            view.findViewById(R.id.type_shoes),
            view.findViewById(R.id.type_other)
        )

        preselectedTypes?.let { selected ->
            typeCheckBoxes.forEach { checkBox ->
                checkBox.isChecked = selected.contains(checkBox.text.toString())
            }
        }

        colorViews = mapOf(
            view.findViewById<ImageView>(R.id.color_red) to view.findViewById<ImageView>(R.id.checkmark_red),
            view.findViewById<ImageView>(R.id.color_orange) to view.findViewById<ImageView>(R.id.checkmark_orange),
            view.findViewById<ImageView>(R.id.color_yellow) to view.findViewById<ImageView>(R.id.checkmark_yellow),
            view.findViewById<ImageView>(R.id.color_green) to view.findViewById<ImageView>(R.id.checkmark_green),
            view.findViewById<ImageView>(R.id.color_blue) to view.findViewById<ImageView>(R.id.checkmark_blue),
            view.findViewById<ImageView>(R.id.color_pink) to view.findViewById<ImageView>(R.id.checkmark_pink),
            view.findViewById<ImageView>(R.id.color_purple) to view.findViewById<ImageView>(R.id.checkmark_purple),
            view.findViewById<ImageView>(R.id.color_white) to view.findViewById<ImageView>(R.id.checkmark_white),
            view.findViewById<ImageView>(R.id.color_beige) to view.findViewById<ImageView>(R.id.checkmark_beige),
            view.findViewById<ImageView>(R.id.color_gray) to view.findViewById<ImageView>(R.id.checkmark_gray),
            view.findViewById<ImageView>(R.id.color_brown) to view.findViewById<ImageView>(R.id.checkmark_brown),
            view.findViewById<ImageView>(R.id.color_black) to view.findViewById<ImageView>(R.id.checkmark_black)
        )

        preselectedColors?.let { selected ->
            selectedColors.addAll(selected)
            colorViews.forEach { (circle, checkmark) ->
                val colorName = circle.contentDescription.toString()
                if (selected.contains(colorName)) {
                    toggleCheckmark(checkmark, true)
                }
            }
        }

        colorViews.forEach { (circle, checkmark) ->
            circle.setOnClickListener {
                val colorName = circle.contentDescription.toString()
                val isCurrentlySelected = selectedColors.contains(colorName)
                toggleCheckmark(checkmark, !isCurrentlySelected)

                if (isCurrentlySelected) {
                    selectedColors.remove(colorName)
                } else {
                    selectedColors.add(colorName)
                }

                // Debug print
                println("Debug - Current selected colors: $selectedColors")
            }
        }

        view.findViewById<ImageView>(R.id.btn_apply).setOnClickListener {
            val selectedTypes = typeCheckBoxes.filter { it.isChecked }
                .map { it.text.toString() }

            println("Debug - Applying colors: $selectedColors")

            onApplyFilters(
                selectedTypes.ifEmpty { null },
                selectedColors.ifEmpty { null }?.toList()
            )
            dismiss()
        }

        view.findViewById<ImageView>(R.id.btn_reset).setOnClickListener {
            resetFilters()
        }
        trace.stop()
    }


    private fun showInfoDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val infoDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_color_info, null)
        dialogBuilder.setView(infoDialogView)
        val infoDialog = dialogBuilder.create()
        infoDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        infoDialog.window?.setDimAmount(0.6f)
        val colorInfoTextView: TextView = infoDialogView.findViewById(R.id.dialog_message)
        val colorInfoText = getString(R.string.color_info)

        // For Android N (API level 24) and above
        colorInfoTextView.text = Html.fromHtml(colorInfoText, Html.FROM_HTML_MODE_LEGACY)

        val closeButton: ImageView = infoDialogView.findViewById(R.id.btn_close)
        closeButton.setOnClickListener {
            infoDialog.dismiss() // Close the dialog when the button is clicked
        }

        infoDialog.show()
    }


    private fun toggleCheckmark(checkmark: ImageView, show: Boolean) {
        checkmark.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun resetFilters() {
        // Reset all type CheckBoxes
        typeCheckBoxes.forEach { it.isChecked = false }
        // Reset all color checkmarks
        colorViews.values.forEach { it.visibility = View.GONE }
        selectedColors.clear()
        // Call the reset filters callback
        onResetFilters()
    }
}