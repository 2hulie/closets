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
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataFragment : Fragment() {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var importClosetIcon: ImageView
    private lateinit var exportClosetIcon: ImageView
    private var exportedJson: String? = null
    private var isReplaceAction = false

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { writeToUri(it, exportedJson ?: "") }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
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
        val dataViewModel = ViewModelProvider(this).get(DataViewModel::class.java)

        _binding = FragmentDataBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView = binding.textData
        dataViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Initialize the clickable ImageViews
        importClosetIcon = binding.iconImport
        exportClosetIcon = binding.iconExport

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            openDocumentLauncher.launch(arrayOf("application/json"))
            dialog.dismiss()
        }

        btnReplace.setOnClickListener {
            isReplaceAction = true
            openDocumentLauncher.launch(arrayOf("application/json"))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportData() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = ItemRepository(database.itemDao())
            val items: List<Item> = repository.getAllItemsDirectly()
            exportedJson = Gson().toJson(items)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = sdf.format(Date())
            val defaultFileName = "closets_data_${currentDate}"

            createDocumentLauncher.launch(defaultFileName)
        }
    }

    private fun writeToUri(uri: Uri, json: String) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            showToast(requireContext(), "Export successful.")
        } catch (e: Exception) {
            showToast(requireContext(), "Export failed: ${e.message}")
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        lifecycleScope.launch {
            val json = readTextFromUri(uri)
            json?.let {
                // Parse JSON into a list of Items
                val importedItems: List<Item> = Gson().fromJson(it, Array<Item>::class.java).toList()
                // Create a repository instance
                val database = AppDatabase.getDatabase(requireContext())
                val repository = ItemRepository(database.itemDao())

                if (!isReplaceAction) {
                    val currentCount = repository.getItemCount()
                    if (currentCount >= 50) {
                        showToast(requireContext(), "Maximum items reached. No new items imported.")
                        return@launch
                    }
                    val allowedCount = 50 - currentCount
                    val itemsToInsert = importedItems.take(allowedCount).map { it.copy(id = 0) }
                    itemsToInsert.forEach { repository.insertItem(it) }
                    if (itemsToInsert.size < importedItems.size) {
                        showToast(requireContext(), "Imported only ${itemsToInsert.size} items. Maximum limit reached.")
                    } else {
                        showToast(requireContext(), "Data updated successfully.")
                    }
                } else {
                    // For replace action, clear all items first
                    repository.clearAllItems()
                    importedItems.forEach { repository.insertItem(it.copy(id = 0)) }
                    showToast(requireContext(), "Data replaced successfully.")
                }
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            showToast(requireContext(), "Failed to read file: ${e.message}")
            null
        }
    }

    private fun setStatusBarColor() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_data)
    }
}