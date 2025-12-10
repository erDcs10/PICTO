package id.ac.pnm.photofilterapp.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.media.MediaActionSound
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import id.ac.pnm.photofilterapp.R
import id.ac.pnm.photofilterapp.adapter.CaptureButtonAdapter
import id.ac.pnm.photofilterapp.databinding.FragmentCameraBinding
import id.ac.pnm.photofilterapp.filter.FilterManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val shutterSound = MediaActionSound()
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permission request denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        val adapter = CaptureButtonAdapter(FilterManager.filters) {
            val currentItem = binding.captureButtonPager.currentItem
            val selectedFilter = FilterManager.filters.getOrNull(currentItem)

            takePhoto(selectedFilter?.colorMatrix)
        }
        binding.captureButtonPager.adapter = adapter

        binding.galleryButton.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_gallery)
        }

        binding.switchCameraButton.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        binding.captureButtonPager.setOnClickListener {
            triggerShutterEffect()
            // saveImageToGallery()
        }

        updateGalleryThumbnail()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        updateGalleryThumbnail()
    }

    private fun updateGalleryThumbnail() {
        val files = requireContext().filesDir.listFiles { _, name ->
            name.endsWith(".jpg") || name.endsWith(".jpeg")
        }

        if (files != null && files.isNotEmpty()) {
            files.sortByDescending { it.lastModified() }
            val latestFile = files.first()
            
            requireActivity().runOnUiThread {
                binding.galleryButton.load(latestFile) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        } else {
            requireActivity().runOnUiThread {
                binding.galleryButton.load(R.drawable.ic_gallery) {
                    transformations(CircleCropTransformation())
                }
            }
        }
    }

    private fun takePhoto(filterMatrix: ColorMatrix?) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Capture Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = image.toBitmap()
                        val rotationDegrees = image.imageInfo.rotationDegrees

                        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

                        val rotatedBitmap = rotateAndFlipBitmap(bitmap, rotationDegrees.toFloat(), isFrontCamera)

                        val finalBitmap = if (filterMatrix != null) {
                            applyFilter(rotatedBitmap, filterMatrix)
                        } else {
                            rotatedBitmap
                        }

                        val prefix = if (filterMatrix != null) "Filtered" else "Normal"
                        val name = "${prefix}_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                            .format(System.currentTimeMillis())
                        val photoFile = File(requireContext().filesDir, "$name.jpg")

                        FileOutputStream(photoFile).use { out ->
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }

                        requireActivity().runOnUiThread {
                            val toastMsg = if (filterMatrix != null) "Saved Filtered Photo!" else "Saved Photo!"
                            Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
                            updateGalleryThumbnail()
                        }

                        if (bitmap != rotatedBitmap) bitmap.recycle()
                        if (rotatedBitmap != finalBitmap) rotatedBitmap.recycle()
                        if (filterMatrix != null) finalBitmap.recycle()

                    } catch (e: Exception) {
                        Log.e(TAG, "Processing failed", e)
                    } finally {
                        image.close()
                    }
                }
            }
        )
    }

    private fun rotateAndFlipBitmap(source: Bitmap, angle: Float, flipHorizontal: Boolean): Bitmap {
        // If no rotation and no flip is needed, return the original
        if (angle == 0f && !flipHorizontal) return source

        val matrix = Matrix()

        // 1. Rotate the image first
        matrix.postRotate(angle)

        // 2. If it's the front camera, mirror it (Flip Horizontal)
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun triggerShutterEffect() {
        // 4. Access the view directly through binding
        // Assuming the ID in XML is android:id="@+id/shutterFlashView"
        binding.shutterFlashView.apply {
            // Make visible and white
            visibility = View.VISIBLE
            alpha = 1f

            // Animate fade out
            animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction {
                    visibility = View.GONE
                }
                .start()
        }
    }
    private fun applyFilter(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val width = src.width
        val height = src.height
        
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        
        canvas.drawBitmap(src, 0f, 0f, paint)
        
        return dest
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                            )
                            .build()
                    )
                    .build().also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }
                /*imageCapture = ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()*/
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                            )
                            .build()
                    )
                    .build()
                
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "PictoApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).toTypedArray()
    }
}
