package com.example.test.ui.data

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.test.R
import com.example.test.databinding.FragmentDataBinding

class DataFragment : Fragment() {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!

    // Declare ImageView variables for the icons
    private lateinit var importClosetIcon: ImageView
    private lateinit var exportClosetIcon: ImageView

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

        // Load the slide-down animation
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.animator.slide_down)

        // Find the ImageView by ID (ensure the ID matches the layout)
        val dataImageView = view.findViewById<ImageView>(R.id.data_image)

        // Apply the animation to the main ImageView
        dataImageView.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

        // Load the scale animation
        val scaleAnimation = AnimationUtils.loadAnimation(context, R.animator.scale_animation)

        // Set click listener for Import Closet icon
        importClosetIcon.setOnClickListener {
            it.startAnimation(scaleAnimation) // Start animation
            // Handle Import Closet action
        }

        // Set click listener for Export Closet icon
        exportClosetIcon.setOnClickListener {
            it.startAnimation(scaleAnimation) // Start animation
            // Handle Export Closet action
        }
    }

    private fun setStatusBarColor() {
        // Change the status bar color
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lbl_data)

        // Get the decor view of the activity's window
        val decorView = requireActivity().window.decorView

        // Make the status bar content (icons/text) white
        // Remove SYSTEM_UI_FLAG_LIGHT_STATUS_BAR to use light icons (white)
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = 0 // Clears any previously set flags
    }
}