package com.example.closets.ui.data

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.closets.R
import com.example.closets.databinding.FragmentDataBinding
import com.example.closets.repository.AppDatabase
import com.example.closets.repository.ItemRepository
import com.example.closets.ui.entities.Item
import com.google.firebase.perf.FirebasePerformance
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExportedItem(
    val color: String,
    val id: Int,
    val imageFile: String,
    val isChecked: Boolean,
    val isFavorite: Boolean,
    val lastWornDate: String,
    val name: String,
    val type: String,
    val wornTimes: Int
)

class DataFragment : Fragment() {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var importClosetIcon: ImageView
    private lateinit var exportClosetIcon: ImageView
    private var exportedZipFile: File? = null
    private var isReplaceAction = false

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        uri?.let { writeZipToUri(it, exportedZipFile) }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleSelectedZip(it) }
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val trace = FirebasePerformance.getInstance().newTrace("dataFragment_onCreateView")
        trace.start()

        val dataViewModel = ViewModelProvider(this)[DataViewModel::class.java]

        _binding = FragmentDataBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView = binding.textData
        dataViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Initialize the clickable ImageViews
        importClosetIcon = binding.iconImport
        exportClosetIcon = binding.iconExport

        trace.stop()
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trace = FirebasePerformance.getInstance().newTrace("dataFragment_onViewCreated")
        trace.start()

        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        val dataImageView = view.findViewById<ImageView>(R.id.data_image)
        val dataText = view.findViewById<TextView>(R.id.data_text)
        dataImageView.startAnimation(slideDownAnimation)
        dataText.startAnimation(slideDownAnimation)

        setStatusBarColor()

        val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_animation)
        importClosetIcon.setOnClickListener {
            it.startAnimation(scaleAnimation) // Start animation
            showImportDialog()
        }

        exportClosetIcon.setOnClickListener {
            it.startAnimation(scaleAnimation) // Start animation
            exportData()
        }

        trace.stop()
    }

    private fun showImportDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_import, null)

        val btnClose = dialogView.findViewById<TextView>(R.id.btn_close)
        val btnUpdate = dialogView.findViewById<ImageView>(R.id.btn_update)
        val btnReplace = dialogView.findViewById<ImageView>(R.id.btn_replace)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
        val dialog = builder.create()

        // avoid unwanted outlines
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnUpdate.setOnClickListener {
            isReplaceAction = false
            openDocumentLauncher.launch(arrayOf("application/zip"))
            dialog.dismiss()
        }

        btnReplace.setOnClickListener {
            isReplaceAction = true
            openDocumentLauncher.launch(arrayOf("application/zip"))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportData() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = ItemRepository(database.itemDao())
            val items: List<Item> = repository.getAllItemsDirectly()

            if (items.isEmpty()) {
                showToast(requireContext(), "No items available to export.")
                return@launch
            }

            // Create a list of ExportedItem objects.
            // Instead of saving the original imageUri, we generate a file name for the image.
            val exportedItems = items.map { item ->
                ExportedItem(
                    color = item.color,
                    id = item.id,
                    imageFile = "image_${item.id}.jpg", // relative file name for this image
                    isChecked = false,
                    isFavorite = item.isFavorite,
                    lastWornDate = item.lastWornDate,
                    name = item.name,
                    type = item.type,
                    wornTimes = item.wornTimes
                )
            }

            // Create the JSON manifest.
            val jsonManifest = Gson().toJson(exportedItems)

            // Create a temporary ZIP file in your cache directory.
            val zipFile = File(requireContext().cacheDir, "closets_export_${System.currentTimeMillis()}.zip")

            withContext(Dispatchers.IO) {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    // 1. Write the manifest.json entry.
                    val manifestEntry = ZipEntry("manifest.json")
                    zos.putNextEntry(manifestEntry)
                    val manifestBytes = jsonManifest.toByteArray(Charsets.UTF_8)
                    zos.write(manifestBytes)
                    zos.closeEntry()

                    // 2. For each item, read the image and write it to the zip.
                    for (item in items) {
                        try {
                            val uri = Uri.parse(item.imageUri)
                            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                                val imageBytes = inputStream.readBytes()
                                val imageEntry = ZipEntry("image_${item.id}.jpg")
                                zos.putNextEntry(imageEntry)
                                zos.write(imageBytes)
                                zos.closeEntry()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    zos.finish()
                }
            }

            // Save the zip file reference so it can be written in writeZipToUri().
            exportedZipFile = zipFile

            // Create a default file name for the zip.
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = sdf.format(Date())
            val defaultFileName = "closets_export_${currentDate}.zip"

            // Launch the document picker to let the user save the ZIP file.
            createDocumentLauncher.launch(defaultFileName)
        }
    }

    private fun writeZipToUri(uri: Uri, zipFile: File?) {
        if (zipFile == null) {
            showToast(requireContext(), "Export failed: zip file not found.")
            return
        }
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                zipFile.inputStream().use { it.copyTo(outputStream) }
            }
            showToast(requireContext(), "Export successful.")
        } catch (e: Exception) {
            showToast(requireContext(), "Export failed: ${e.message}")
        }
    }

    private fun handleSelectedZip(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // create a temporary directory to extract the zip contents
                val tempDir = File(requireContext().cacheDir, "importedZip")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                } else {
                    tempDir.deleteRecursively()
                    tempDir.mkdirs()
                }

                // open the ZIP file
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry: ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            val file = File(tempDir, entry.name)
                            if (!entry.isDirectory) {
                                // Write the file to tempDir
                                FileOutputStream(file).use { fos ->
                                    BufferedOutputStream(fos).use { bos ->
                                        zis.copyTo(bos)
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }

                // read and parse the manifest.json
                val manifestFile = File(tempDir, "manifest.json")
                if (!manifestFile.exists()) {
                    withContext(Dispatchers.Main) {
                        showToast(requireContext(), "Manifest not found in ZIP.")
                    }
                    return@launch
                }
                val manifestJson = manifestFile.readText(Charsets.UTF_8)
                val importedExportedItems: List<ExportedItem> =
                    Gson().fromJson(manifestJson, Array<ExportedItem>::class.java).toList()

                // For each exported item, prepare to update the item with the corresponding image.
                val database = AppDatabase.getDatabase(requireContext())
                val repository = ItemRepository(database.itemDao())
                val importedItems = importedExportedItems.map { exportedItem ->
                    // Locate the image file in tempDir
                    val imageFile = File(tempDir, exportedItem.imageFile)
                    val newImageUri = if (imageFile.exists()) {
                        // Copy the image file to a location where app stores images.
                        // Then, create a content Uri or file Uri accordingly.
                        val destDir = requireContext().filesDir // or another directory
                        val destFile = File(destDir, exportedItem.imageFile)
                        imageFile.copyTo(destFile, overwrite = true)
                        Uri.fromFile(destFile).toString()
                    } else {
                        ""
                    }

                    // Create your Item object (set id = 0 if you're inserting new items)
                    Item(
                        id = 0,
                        color = exportedItem.color,
                        imageUri = newImageUri,
                        isFavorite = exportedItem.isFavorite,
                        lastWornDate = exportedItem.lastWornDate,
                        name = exportedItem.name,
                        type = exportedItem.type,
                        wornTimes = exportedItem.wornTimes
                    )
                }

                //  update repository based on isReplaceAction
                withContext(Dispatchers.Main) {
                    if (!isReplaceAction) {
                        val currentCount = repository.getItemCount()
                        if (currentCount >= 50) {
                            showToast(requireContext(), "Maximum items reached. No new items imported.")
                            return@withContext
                        }
                        val allowedCount = 50 - currentCount
                        val itemsToInsert = importedItems.take(allowedCount)
                        itemsToInsert.forEach { repository.insertItem(it) }
                        if (itemsToInsert.size < importedItems.size) {
                            showToast(requireContext(), "Imported only ${itemsToInsert.size} items. Maximum limit reached.")
                        } else {
                            showToast(requireContext(), "Data updated successfully.")
                        }
                    } else {
                        repository.clearAllItems()
                        importedItems.forEach { repository.insertItem(it) }
                        showToast(requireContext(), "Data replaced successfully.")
                    }
                }

                tempDir.deleteRecursively()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast(requireContext(), "Import failed: ${e.message}")
                }
            }
        }
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_data)
    }
}