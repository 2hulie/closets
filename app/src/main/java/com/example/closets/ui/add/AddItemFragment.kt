package com.example.closets.ui.add

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
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.closets.R
import com.example.closets.ui.edit.EditItemInfoFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.eren.removebg.RemoveBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import androidx.lifecycle.ViewModelProvider
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.example.closets.ui.viewmodels.ItemViewModel
import com.example.closets.ui.viewmodels.ItemViewModelFactory

class AddItemFragment : Fragment() {
    // View declarations
    private lateinit var addItemNameEditText: EditText
    private lateinit var wornTimesTextView: TextView
    private lateinit var typeSpinner: Spinner
    private lateinit var colorCircle: View
    private lateinit var lastWornTextView: TextView
    private lateinit var addImageView: ImageView

    private var currentView: View? = null

    private var selectedItemType: String = ""
    private var selectedColor: Int = Color.GRAY // Default color
    private lateinit var itemViewModel: ItemViewModel

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var imageUri: Uri? = null // This will store the URI of the image captured by the camera
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    // Singleton Toast inside the companion object
    companion object {
        var currentToast: Toast? = null

        // This method shows the Toast and cancels any previous one
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
        // Create the TextView for the title
        val titleTextView = TextView(requireContext())
        titleTextView.text = "Choose Image Source"
        titleTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_items))  // Set pink color
        titleTextView.textSize = 18f  // Optionally, adjust the text size

        // Add padding (left, top, right, bottom)
        titleTextView.setPadding(50, 30, 30, 30)

        // Create a line (divider) below the title
        val divider = View(requireContext())

        // Set the divider's layout parameters
        divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)  // Set height of the divider

        // Set the divider color with 30% opacity (alpha = 77 in hexadecimal, 255 is full opacity)
        val dividerColor = ContextCompat.getColor(requireContext(), R.color.color_favorites) // Get the color from resources
        val transparentColor = Color.argb(77, Color.red(dividerColor), Color.green(dividerColor), Color.blue(dividerColor)) // Apply alpha to the color

        divider.setBackgroundColor(transparentColor)  // Set the divider color with transparency

        // Create a container layout to hold both the title and the divider
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.addView(titleTextView)  // Add the title
        container.addView(divider)  // Add the divider below the title

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

    private fun pickImageFromGalleryOrCamera() {
        val options = arrayOf("Pick from Gallery", "Take a Photo")

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(createCustomTitle()) // Use custom title
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
            showToast(requireContext(), "Please check camera permissions in settings.")
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

        val targetWidth = addImageView.width
        val targetHeight = addImageView.height

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
                                    EditItemInfoFragment.showToast(
                                        requireContext(),
                                        "Error: Background removal failed"
                                    )
                                }
                            }
                        }
                    } else {
                        EditItemInfoFragment.showToast(
                            requireContext(),
                            "Error: Failed to crop image"
                        )
                    }
                } catch (e: Exception) {
                    EditItemInfoFragment.showToast(requireContext(), "Error: ${e.message}")
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

    // Function to check if the bitmap has transparent pixels
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
            addImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            addImageView.setImageBitmap(scaledBitmap)

        } catch (e: Exception) {
            Log.e("HandleImage", "Error saving image", e)
            showToast(requireContext(), "Error saving image")
        }

        dialog.dismiss()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentView = inflater.inflate(R.layout.fragment_add_item, container, false) ?: return requireView()

        setStatusBarColor()

        // Initialize ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ItemRepository(database.itemDao())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(repository))[ItemViewModel::class.java]

        // Initialize all views
        initializeViews(currentView!!)

        // Setup all interactions
        setupInteractions()

        // Populate existing data
        populateExistingData()

        // Disable and redirect bottom navigation
        redirectBottomNavigation()

        // Setup the back pressed callback to show the discard dialog
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDiscardChangesDialog()
            }
        })

        return currentView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Restore default navigation behavior when leaving this fragment
        restoreBottomNavigation()

        currentView = null
    }

    private fun redirectBottomNavigation() {
        // Access the NavController and redirect based on current destination
        val navController = findNavController()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Disable default navigation for this fragment
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == R.id.addItemFragment) {
                    showDiscardChangesDialog() // Show dialog when navigation is attempted
                    return@setOnMenuItemClickListener true // Block navigation
                }
                return@setOnMenuItemClickListener false // Allow normal behavior for other fragments
            }
        }
    }

    private fun restoreBottomNavigation() {
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)

        // Reset all menu item click listeners to null (restore default behavior)
        bottomNavigationView?.menu?.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener(null)
        }
    }

    private fun initializeViews(view: View) {
        addItemNameEditText = view.findViewById(R.id.add_name_text)
        wornTimesTextView = view.findViewById(R.id.worn_text)
        typeSpinner = view.findViewById(R.id.sort_by_spinner)
        colorCircle = view.findViewById(R.id.color_circle)
        lastWornTextView = view.findViewById(R.id.last_worn_value)
        addImageView = view.findViewById(R.id.add_image)
    }

    private fun setupInteractions() {
        setupSortSpinner()

        // Add image click listener for photo upload
        addImageView.setOnClickListener {
            pickImageFromGalleryOrCamera()
        }

        // Image-Based Color Picker Setup
        colorCircle.setOnClickListener {
            showImageColorPickerDialog()
        }

        // Edit Color Text Click Interaction
        val changeColorText = currentView?.findViewById<TextView>(R.id.change_color_text)
        changeColorText?.setOnClickListener {
            showImageColorPickerDialog() // Open the same color picker dialog
        }

        // Button Interactions
        currentView?.post {
            try {
                currentView?.findViewById<ImageView>(R.id.icon_add_item)?.setOnClickListener {
                    saveItemChanges()
                }
                currentView?.findViewById<ImageView>(R.id.icon_cancel)?.setOnClickListener {
                    showDiscardChangesDialog()
                }
                currentView?.findViewById<ImageView>(R.id.icon_back)?.setOnClickListener {
                    showDiscardChangesDialog()
                }
            } catch (e: Exception) {
                Log.e("SetupInteractions", "Error setting up buttons", e)
            }
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = resources.getStringArray(R.array.filter_options)

        // Custom adapter for spinner with custom layout
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
    private fun showImageColorPickerDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_image_color_picker, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.imageView)
        val selectedColorView = dialogView.findViewById<View>(R.id.selectedColorView)
        val magnifierView = dialogView.findViewById<View>(R.id.magnifierView)
        val selectButton = dialogView.findViewById<ImageView>(R.id.btn_select)
        val cancelButton = dialogView.findViewById<ImageView>(R.id.btn_cancel)

        val defaultImage = ContextCompat.getDrawable(requireContext(), R.drawable.add_item_image)
        if (addImageView.drawable.constantState == defaultImage?.constantState) {
            showToast(requireContext(), "Please upload an image first")
            return
        }

        val drawable = addImageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            imageView.setImageBitmap(bitmap)

            val currentColor = (colorCircle.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
            selectedColorView.setBackgroundColor(currentColor)

            // Define touch offset (adjust these values based on testing)
            val touchOffsetX = -20 // Negative moves the sampling point left of the touch
            val touchOffsetY = -50 // Negative moves the sampling point above the touch

            imageView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // Calculate the actual dimensions of the displayed image
                        val imageRect = RectF()
                        val drawable = imageView.drawable
                        if (drawable != null) {
                            imageView.imageMatrix.mapRect(imageRect, RectF(0f, 0f,
                                drawable.intrinsicWidth.toFloat(),
                                drawable.intrinsicHeight.toFloat()))
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        selectButton.setOnClickListener {
            val selectedColor = (selectedColorView.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
            this.selectedColor = selectedColor // Store the selected color
            colorCircle.setBackgroundColor(selectedColor)
            selectedColorView.setBackgroundColor(selectedColor)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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

        // Draw crosshair
        canvas.drawLine(centerX - 10, centerY, centerX + 10, centerY, crosshairPaint)
        canvas.drawLine(centerX, centerY - 10, centerX, centerY + 10, crosshairPaint)

        // Add a circle around the crosshair
        canvas.drawCircle(centerX, centerY, 10f, crosshairPaint)

        (magnifierView as? ImageView)?.setImageBitmap(magnifiedBitmap)
    }

    fun saveItemChanges() {
        if (validateInputs()) {
            // Check for duplicate item name
            val itemName = addItemNameEditText.text.toString()
            itemViewModel.items.observe(viewLifecycleOwner) { existingItems ->
                val isDuplicate = existingItems.any { it.name.equals(itemName, ignoreCase = true) }
                if (isDuplicate) {
                    showToast(requireContext(), "Name already exists.")
                    return@observe // Exit the function if duplicate found
                }

                // Proceed to save the item if no duplicates
                try {
                    val itemType = typeSpinner.selectedItem.toString()
                    val wornTimes = wornTimesTextView.text.toString().substringAfter("worn ").substringBefore(" times").toInt()
                    val lastWornDate = lastWornTextView.text.toString()

                    val formattedColor = String.format("#%06X", (0xFFFFFF and selectedColor))

                    // Get the image URI from the ImageView
                    val imageUriString = imageUri?.toString() ?: run {
                        showToast(requireContext(), "Error: No image selected")
                        return@observe
                    }

                    // Create a new Item object
                    val newItem = Item(
                        name = itemName,
                        type = itemType,
                        color = formattedColor,
                        wornTimes = wornTimes,
                        lastWornDate = lastWornDate,
                        imageUri = imageUriString,
                        isFavorite = false
                    )

                    // Save the item using the ViewModel
                    itemViewModel.insert(newItem)

                    Log.d(
                        "SaveItemChanges",
                        "Item Name: $itemName, Type: $itemType, Worn Times: $wornTimes, Last Worn: $lastWornDate, Color: $formattedColor"
                    )
                    showToast(requireContext(), "Item added to closet!")

                    restoreBottomNavigation()

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e("SaveItemChanges", "Error saving item changes", e)
                    showToast(requireContext(), "An error occurred while saving changes")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun populateExistingData() {
        addItemNameEditText.hint = "Tap to add name"
        typeSpinner.setSelection(0)
        colorCircle.background = ColorDrawable(Color.parseColor("#FFFFFF"))
        wornTimesTextView.text = "worn 0 times"
        lastWornTextView.text = "N/A"
    }

    // Validate inputs and show Toast messages
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun validateInputs(): Boolean {
        // Check if item name is empty
        if (addItemNameEditText.text.isBlank()) {
            showToast(
                requireContext(),
                "Item name cannot be empty"
            )
            return false
        }

        // Check if an item type is selected (assuming position 0 is "Please select")
        if (typeSpinner.selectedItemPosition == 0) {
            showToast(
                requireContext(),
                "Please select an item type"
            )
            return false
        }

        // Check if an image is selected and if it's not the default image
        val drawable = addImageView.drawable
        if (drawable == null || drawable.constantState == ContextCompat.getDrawable(requireContext(), R.drawable.add_item_image)?.constantState) {
            showToast(
                requireContext(),
                "Please upload a valid image"
            )
            return false
        }

        // If all validations pass
        return true
    }

    fun showDiscardChangesDialog() {
        // Create the AlertDialog builder
        val dialogBuilder = AlertDialog.Builder(requireContext())

        // Inflate the custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_discard_changes, null)

        // Set the custom layout as the dialog content
        dialogBuilder.setView(dialogView)

        // Create the dialog
        val dialog = dialogBuilder.create()

        // Remove the default background to avoid unwanted outlines
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set up button actions
        dialogView.findViewById<ImageView>(R.id.btn_discard).setOnClickListener {
            // Restore bottom navigation behavior
            restoreBottomNavigation()

            // Go back to the previous fragment in the navigation stack
            findNavController().navigateUp()

            dialog.dismiss() // Close the dialog
        }
        dialogView.findViewById<ImageView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss() // Close the dialog without taking any action
        }

        dialog.show()
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.status_item)
    }
}
