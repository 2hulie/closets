package com.example.closets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent

// Define the CropImageListener interface
interface CropImageListener {
    fun onImageCropped(croppedBitmap: Bitmap)
}

@SuppressLint("ClickableViewAccessibility")
class CropImageView(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private val cropRectPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    val cropRect: RectF = RectF()
    private var isCropping = false
    private var cropImageListener: CropImageListener? = null

    init {
        // Set up touch listener to allow user to crop interactively
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start cropping area
                    cropRect.set(event.x, event.y, event.x, event.y)
                    isCropping = true
                    invalidate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isCropping) {
                        // Update the cropping rectangle while moving
                        cropRect.right = event.x
                        cropRect.bottom = event.y
                        invalidate()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Crop the image once the user lifts the finger
                    isCropping = false
                    cropImage()
                    true
                }
                else -> false
            }
        }
    }

    // Set the listener for cropping events
    fun setCropImageListener(listener: CropImageListener) {
        this.cropImageListener = listener
    }

    fun getCroppedBitmap(): Bitmap {
        // Ensure that the bitmap is valid before proceeding
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
            ?: throw IllegalStateException("No bitmap available for cropping")

        val width = cropRect.width().toInt()
        val height = cropRect.height().toInt()

        return Bitmap.createBitmap(bitmap, cropRect.left.toInt(), cropRect.top.toInt(), width, height)
    }

    // Crop the image based on the selected area
    fun cropImage() {
        val bitmap = (drawable as BitmapDrawable).bitmap
        val width = cropRect.width().toInt()
        val height = cropRect.height().toInt()
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            cropRect.left.toInt(),
            cropRect.top.toInt(),
            width,
            height
        )
        // Notify the listener about the cropped image
        cropImageListener?.onImageCropped(croppedBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isCropping) {
            // Draw the crop rectangle
            canvas.drawRect(cropRect, cropRectPaint)
        }
    }
}