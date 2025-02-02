package com.example.closets.ui.data

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.closets.R
import com.example.closets.databinding.FragmentDataBinding
import org.w3c.dom.Text

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
        val slideDownAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_down)

        // Find by ID
        val dataImageView = view.findViewById<ImageView>(R.id.data_image)
        val dataText = view.findViewById<TextView>(R.id.data_text)

        // Apply the animation to the label
        dataImageView.startAnimation(slideDownAnimation)
        dataText.startAnimation(slideDownAnimation)

        // Change status bar color for this fragment
        setStatusBarColor()

        // Load the scale animation
        val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_animation)

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
    }
}