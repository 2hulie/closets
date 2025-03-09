package com.example.closets

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.closets.ui.viewmodels.ItemViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.eren.removebg.RemoveBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

abstract class BaseModifyItemFragment : Fragment() {
    protected var selectedItemType: String = ""
    protected lateinit var itemViewModel: ItemViewModel
    protected abstract val targetImageView: ImageView
    protected var imageUri: Uri? = null
    protected lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    protected lateinit var pickImageLauncher: ActivityResultLauncher<String>
    protected lateinit var typeSpinner: Spinner
    protected lateinit var colorCircle: View
    protected var selectedColor: Int = Color.GRAY

    companion object {
        private var currentToast: Toast? = null

        fun showToast(context: Context, message: String) {
            currentToast?.cancel() // Cancel the previous toast
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
                show() // Show the new toast
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ActivityResultLauncher for image picking
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                showCropImageDialog(it) // Show custom crop dialog for gallery image
            }
        }

        // Initialize ActivityResultLauncher for taking a picture (camera)
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess && imageUri != null) {
                showCropImageDialog(imageUri!!) // Show custom crop dialog for camera image
            } else {
                showToast(requireContext(), "Failed to capture image")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createCustomTitle(): View {
        val titleTextView = TextView(requireContext())
        titleTextView.text = "Choose Image Source"
        titleTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_items))
        titleTextView.textSize = 18f
        titleTextView.setPadding(50, 30, 30, 30) // Add padding (left, top, right, bottom)

        val divider = View(requireContext()) // Create a line (divider) below the title
        divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)  // Set height of the divider

        // Set the divider color with 30% opacity (alpha = 77 in hexadecimal, 255 is full opacity)
        val dividerColor = ContextCompat.getColor(requireContext(), R.color.color_favorites) // Get the color from resources
        val transparentColor = Color.argb(77, Color.red(dividerColor), Color.green(dividerColor), Color.blue(dividerColor)) // Apply alpha to the color

        divider.setBackgroundColor(transparentColor)  // Set the divider color with transparency

        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.addView(titleTextView)
        container.addView(divider)

        return container  // Return the container with both title and divider
    }

    private fun createCustomAdapter(options: Array<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.setTextColor(ContextCompat.getColor(context, R.color.base_text)) // Match title color
                textView.textSize = 12f // Match title text size
                textView.setPadding(50, 10, 20, 20) // Adjust padding
                return textView
            }
        }
    }

    fun pickImageFromGalleryOrCamera() {
        val options = arrayOf("Pick from Gallery", "Take a Photo")

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(createCustomTitle())
            .setAdapter(createCustomAdapter(options)) { _, which ->
                when (which) {
                    0 -> {
                        pickImageFromGallery() // Launch gallery image picker
                    }
                    1 -> {
                        captureImageWithCamera() // Launch camera to take a photo
                    }
                }
            }

        dialogBuilder.show()
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun captureImageWithCamera() {
        try {
            // Create a temporary file where the captured image will be stored
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, "New Image")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            imageUri = requireActivity().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            )

            // Launch the camera to take a picture
            imageUri?.let { takePictureLauncher.launch(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(
                requireContext(),
                "Please check camera permissions in settings."
            )
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showCropImageDialog(uri: Uri) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_image_crop, null)

        val cropImageView: com.canhub.cropper.CropImageView = dialogView.findViewById(R.id.cropImageView)
        val cropButton: ImageView = dialogView.findViewById(R.id.btn_crop)
        val cancelButton: ImageView = dialogView.findViewById(R.id.btn_cancel)
        val progressBar: ProgressBar = dialogView.findViewById(R.id.loadingProgressBar)
        val loadingText: TextView = dialogView.findViewById(R.id.loadingText)

        // Customize ProgressBar color
        val drawable = progressBar.indeterminateDrawable
        val colorFilter = PorterDuffColorFilter(ContextCompat.getColor(requireContext(), R.color.color_items), PorterDuff.Mode.SRC_IN)
        drawable.colorFilter = colorFilter

        cropImageView.setImageUriAsync(uri)

        val targetWidth = targetImageView.width
        val targetHeight = targetImageView.height

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val rotateButton: ImageView = dialogView.findViewById(R.id.btn_rotate)

        rotateButton.setOnClickListener {
            cropImageView.rotateImage(90) // Rotate the image by 90 degrees
        }

        var isProcessing = false

        // Function to show/hide loading state
        @SuppressLint("SetTextI18n")
        fun setLoadingState(loading: Boolean) {
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            loadingText.visibility = if (loading) View.VISIBLE else View.GONE
            loadingText.text = getString(R.string.loading_cropped_image)
            cropButton.isEnabled = !loading
            isProcessing = loading
        }

        cropButton.setOnClickListener {
            if (isProcessing) return@setOnClickListener

            // Show loading state immediately
            setLoadingState(true)

            // Launch the processing in a coroutine
            lifecycleScope.launch {
                try {
                    // Small delay to ensure UI updates are visible
                    withContext(Dispatchers.Main) {
                        delay(100)  // Brief delay to ensure loading state is visible
                    }

                    val croppedBitmap = cropImageView.getCroppedImage()

                    if (croppedBitmap != null) {
                        if (hasTransparency(croppedBitmap)) {
                            handleImage(croppedBitmap, targetWidth, targetHeight, dialog)
                        } else {
                            val remover = RemoveBg(requireContext())
                            remover.clearBackground(croppedBitmap).collect { output ->
                                output?.let {
                                    handleImage(it, targetWidth, targetHeight, dialog)
                                } ?: run {
                                    showToast(
                                        requireContext(),
                                        "Error: Background removal failed"
                                    )
                                }
                            }
                        }
                    } else {
                        showToast(
                            requireContext(),
                            "Error: Failed to crop image"
                        )
                    }
                } catch (e: Exception) {
                    showToast(requireContext(), "Error: ${e.message}")
                } finally {
                    // Reset loading state
                    setLoadingState(false)
                }
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun handleImage(bitmap: Bitmap, targetWidth: Int, targetHeight: Int, dialog: Dialog) {
        try {
            // Save the processed bitmap to a file and get its URI
            val filename = "item_image_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            }

            val uri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let { imageUri ->
                requireContext().contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                // Save the new URI
                this.imageUri = imageUri
            }

            // Set the scaled bitmap to the ImageView
            val scaledBitmap = scaleBitmapToFit(bitmap, targetWidth, targetHeight)
            targetImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            targetImageView.setImageBitmap(scaledBitmap)

        } catch (e: Exception) {
            Log.e("HandleImage", "Error saving image", e)
            showToast(
                requireContext(),
                "Error saving image"
            )
        }

        dialog.dismiss()
    }

    private fun hasTransparency(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                // If any pixel is fully transparent (alpha == 0), return true
                if (Color.alpha(pixel) == 0) {
                    return true
                }
            }
        }
        return false // No transparent pixels found
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Calculate the scaling factor to fit the image while maintaining the aspect ratio
        val scaleFactor = max(targetWidth.toFloat() / bitmapWidth, targetHeight.toFloat() / bitmapHeight)

        // Calculate the new scaled dimensions
        val scaledWidth = (bitmapWidth * scaleFactor).toInt()
        val scaledHeight = (bitmapHeight * scaleFactor).toInt()

        // Create a new bitmap with the scaled dimensions
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    fun setupSortSpinner() {
        val sortOptions = resources.getStringArray(R.array.filter_options)
        val spinnerAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.spinner_item, sortOptions) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(ContextCompat.getColor(requireContext(), R.color.base_text))
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(ContextCompat.getColor(requireContext(), R.color.base_text))
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        typeSpinner.adapter = spinnerAdapter

        // Listener to capture the selected item type
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedItemType = sortOptions[position] // Capture selected item type
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case when nothing is selected
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showImageColorPickerDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_image_color_picker, null)

        val colorPickerImageView = dialogView.findViewById<ImageView>(R.id.imageView)
        val selectedColorView = dialogView.findViewById<View>(R.id.selectedColorView)
        val magnifierView = dialogView.findViewById<View>(R.id.magnifierView)
        val selectButton = dialogView.findViewById<ImageView>(R.id.btn_select)
        val cancelButton = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        val drawable = targetImageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            colorPickerImageView.setImageBitmap(bitmap) // Set the existing image to the color picker

            val currentColor = (colorCircle.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
            selectedColorView.setBackgroundColor(currentColor)

            val touchOffsetX = -20 // Negative moves the sampling point left of the touch
            val touchOffsetY = -50 // Negative moves the sampling point above the touch

            colorPickerImageView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val imageRect = RectF()
                        val drawable = colorPickerImageView.drawable
                        if (drawable != null) {
                            colorPickerImageView.imageMatrix.mapRect(imageRect, RectF(0f, 0f,
                                drawable.intrinsicWidth.toFloat(),
                                drawable.intrinsicHeight.toFloat())
                            )
                        }

                        // Apply the touch offset
                        val adjustedX = event.x + touchOffsetX
                        val adjustedY = event.y + touchOffsetY

                        // Convert touch coordinates to bitmap coordinates
                        val bitmapX = ((adjustedX - imageRect.left) * bitmap.width / imageRect.width()).toInt()
                        val bitmapY = ((adjustedY - imageRect.top) * bitmap.height / imageRect.height()).toInt()

                        if (bitmapX in 0 until bitmap.width && bitmapY in 0 until bitmap.height) {
                            val pixelColor = bitmap.getPixel(bitmapX, bitmapY)
                            selectedColorView.setBackgroundColor(pixelColor)

                            // Create magnifier effect
                            createColorMagnifier(bitmap, bitmapX, bitmapY, magnifierView)

                            magnifierView.visibility = View.VISIBLE
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        magnifierView.visibility = View.GONE
                    }
                }
                true
            }
        } else {
            showToast(requireContext(), "No image available for color picker")
            return
        }

        setupColorOptions(dialogView, selectedColorView)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        selectButton.setOnClickListener {
            this.selectedColor = (selectedColorView.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
            colorCircle.setBackgroundColor(this.selectedColor)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("DiscouragedApi")
    private fun setupColorOptions(dialogView: View, selectedColorView: View) {
        val colorOptions = mapOf(
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

        colorOptions.forEach { (colorName, colorHex) ->
            val colorViewId = resources.getIdentifier("color_$colorName", "id", requireContext().packageName)
            val checkmarkViewId = resources.getIdentifier("checkmark_$colorName", "id", requireContext().packageName)

            dialogView.findViewById<ImageView>(colorViewId).setOnClickListener {
                val parsedColor = Color.parseColor(colorHex)
                selectedColorView.setBackgroundColor(parsedColor) // Update the UI
                selectedColor = parsedColor // Update the selected color variable

                // Hide all checkmarks
                colorOptions.keys.forEach { name ->
                    val checkmarkId = resources.getIdentifier("checkmark_$name", "id", requireContext().packageName)
                    dialogView.findViewById<ImageView>(checkmarkId).visibility = View.GONE
                }
                // Show the selected checkmark
                dialogView.findViewById<ImageView>(checkmarkViewId).visibility = View.VISIBLE
            }
        }
    }

    private fun createColorMagnifier(
        bitmap: Bitmap,
        bitmapX: Int,
        bitmapY: Int,
        magnifierView: View
    ) {
        val magnifierSize = 100
        val halfSize = magnifierSize / 2

        val startX = max(0, bitmapX - halfSize/2)
        val startY = max(0, bitmapY - halfSize/2)
        val endX = min(bitmap.width, bitmapX + halfSize/2)
        val endY = min(bitmap.height, bitmapY + halfSize/2)

        val magnifiedBitmap = Bitmap.createBitmap(magnifierSize, magnifierSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(magnifiedBitmap)
        val srcRect = Rect(startX, startY, endX, endY)
        val destRect = Rect(0, 0, magnifierSize, magnifierSize)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(bitmap, srcRect, destRect, paint)

        // Draw a crosshair or indicator at the center
        val centerX = magnifierSize / 2f
        val centerY = magnifierSize / 2f
        val crosshairPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // draw crosshair
        canvas.drawLine(centerX - 10, centerY, centerX + 10, centerY, crosshairPaint)
        canvas.drawLine(centerX, centerY - 10, centerX, centerY + 10, crosshairPaint)

        // add a circle around the crosshair
        canvas.drawCircle(centerX, centerY, 10f, crosshairPaint)

        (magnifierView as? ImageView)?.setImageBitmap(magnifiedBitmap)
    }

    fun restoreBottomNavigation() {
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Reset all menu item click listeners to null (restore default behavior)
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener(null)
        }
    }

    fun showDiscardChangesDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_discard_changes, null)

        dialogBuilder.setView(dialogView)

        val dialog = dialogBuilder.create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<ImageView>(R.id.btn_discard).setOnClickListener {
            restoreBottomNavigation()
            findNavController().navigateUp()
            dialog.dismiss() // close the dialog
        }
        dialogView.findViewById<ImageView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss() // close the dialog without taking any action
        }

        dialog.show()
    }

    fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.status_item)
    }
}