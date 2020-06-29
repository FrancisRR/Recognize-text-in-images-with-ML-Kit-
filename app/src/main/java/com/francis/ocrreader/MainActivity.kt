package com.francis.ocrreader

import TextRecognitionProcessor
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Pair
import com.francis.ocrreader.textdetector.BitmapUtils
import com.francis.ocrreader.textdetector.GraphicOverlay
import com.francis.ocrreader.textdetector.VisionImageProcessor
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private val TAG by lazy { MainActivity::class.java.simpleName }
    private var imageUri: Uri? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var resizedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        selectedSize = SIZE_SCREEN
        imageProcessor = TextRecognitionProcessor(this)

        btGallery.setOnClickListener {
            callGallery()
        }
        btCamera.setOnClickListener {
            callCamera()
        }

        UiUtils.showErrorLog(TAG, "onCreate Called")

    }


    private fun callGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "select gallery"),
            AppConstant.GALLERY_REQUEST_CODE
        )
    }

    private fun callCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Pictures")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(cameraIntent, AppConstant.CAMERA_REQUEST_CODE)
        } else {
            UiUtils.showErrorLog(TAG, "Camera not available")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppConstant.GALLERY_REQUEST_CODE
            && resultCode == Activity.RESULT_OK
        ) {
            imageUri = data?.data
            tryReloadAndDetectInImage()
        } else if (requestCode == AppConstant.CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            tryReloadAndDetectInImage()
        }
    }

    private fun tryReloadAndDetectInImage() {
        Log.d(
            TAG,
            "Try reload and detect image"
        )
        try {

            graphicOverlay.clear()
            val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, imageUri)
                ?: return
            // Get the dimensions of the image view
            val targetedSize = targetedWidthHeight
            // Determine how much to scale down the image
            val scaleFactor = max(
                imageBitmap.width.toFloat() / targetedSize.first.toFloat(),
                imageBitmap.height.toFloat() / targetedSize.second.toFloat()
            )
            resizedBitmap = Bitmap.createScaledBitmap(
                imageBitmap,
                (imageBitmap.width / scaleFactor).toInt(),
                (imageBitmap.height / scaleFactor).toInt(),
                true
            )
            imageView.setImageBitmap(resizedBitmap)
            if (imageProcessor != null) {
                graphicOverlay!!.setImageSourceInfo(
                    resizedBitmap!!.width, resizedBitmap!!.height, false
                )
                imageProcessor!!.processBitmap(resizedBitmap, graphicOverlay)
            } else {
                Log.e(
                    TAG,
                    "Null imageProcessor, please check adb logs for imageProcessor creation error"
                )
            }
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Error retrieving saved image"
            )
            imageUri = null
        }
    }

    private var selectedSize: String? =
        SIZE_SCREEN

    // Max width (portrait mode)
    private var imageMaxWidth = 0

    // Max height (portrait mode)
    private var imageMaxHeight = 0
    private var isLandScape = false

    private val targetedWidthHeight: Pair<Int, Int>
        get() {
            val targetWidth: Int
            val targetHeight: Int
            when (selectedSize) {
                SIZE_SCREEN -> {
                    targetWidth = imageMaxWidth
                    targetHeight = imageMaxHeight
                }
                SIZE_640_480 -> {
                    targetWidth = if (isLandScape) 640 else 480
                    targetHeight = if (isLandScape) 480 else 640
                }
                SIZE_1024_768 -> {
                    targetWidth = if (isLandScape) 1024 else 768
                    targetHeight = if (isLandScape) 768 else 1024
                }
                else -> throw IllegalStateException("Unknown size")
            }
            return Pair(targetWidth, targetHeight)
        }

    companion object {
        private const val SIZE_SCREEN = "w:screen" // Match screen width
        private const val SIZE_1024_768 = "w:1024" // ~1024*768 in a normal ratio
        private const val SIZE_640_480 = "w:640" // ~640*480 in a normal ratio
    }

    /* override fun onSaveInstanceState(outState: Bundle) {
         outState.putParcelable("data", resizedBitmap)
         super.onSaveInstanceState(outState)
     }

     override fun onRestoreInstanceState(savedInstanceState: Bundle) {
         super.onRestoreInstanceState(savedInstanceState)
         val dd: Bitmap? = savedInstanceState.getParcelable("data")
         resizedBitmap=dd
         if (dd != null) {
             imageView.setImageBitmap(dd)
         }
     }*/

}