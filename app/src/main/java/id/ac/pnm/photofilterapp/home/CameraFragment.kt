package id.ac.pnm.photofilterapp.home

import ColdFilter
import RetroSepiaFilter
import VintageFilter
import WarmFilter
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter // Added import
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.recyclerview.widget.LinearLayoutManager
import createDramaticFilter
import id.ac.pnm.photofilterapp.adapter.FilterSidebarAdapter
class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var currentFilterIndex = 0

    private val filterList = listOf("Normal", "Vintage", "Cold", "Warm", "Sepia", "Dramatic")

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Changed to 16:9 to better match phone screens (optional, but recommended)
    private val aspect = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (allPermissionsGranted()) startCamera()
            else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
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
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.rvFilterSidebar.visibility = View.GONE
        binding.rvFilterSidebar.translationX = 100f

        if (allPermissionsGranted()) startCamera()
        else requestPermissions()

        setupUI()
        updateGalleryThumbnail()
    }

    private fun setupUI() {

        val filterAdapter = FilterSidebarAdapter(filterList) { index ->
            currentFilterIndex = index
            Toast.makeText(context, "${filterList[index]} Selected", Toast.LENGTH_SHORT).show()
        }


        binding.rvFilterSidebar.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = filterAdapter
        }

        binding.btnFilterMenu.setOnClickListener {
            toggleSidebar()
        }

        binding.viewFinder.setOnClickListener {
            if (binding.rvFilterSidebar.visibility == View.VISIBLE) {
                toggleSidebar()
            }
        }

        binding.buttonShutter.setOnClickListener {
            takePhoto(currentFilterIndex)
        }


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

    }

    private fun takePhoto(filterMatrix: Int) {
        val imageCapture = imageCapture ?: return

        setLoadingState(true)
        triggerShutterEffect()

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Capture failed: ${exc.message}", exc)
                    setLoadingState(false)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    processCapturedImage(image, filterMatrix)
                }
            }
        )
    }

    private fun processCapturedImage(image: ImageProxy, filterIndex: Int) {
        // Safe context for background thread
        val appContext = context?.applicationContext ?: return

        try {
            // 1. Convert ImageProxy to Bitmap
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val rotationDegrees = image.imageInfo.rotationDegrees
            val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

            image.close() // Close immediately

            // 2. Rotate
            val rotatedBitmap = rotateAndFlipBitmap(originalBitmap, rotationDegrees.toFloat(), isFrontCamera)

            // 3. DECIDE WHICH FILTER TO USE
            // Here we convert the ColorMatrix to a GPUFilter, OR use your custom one.
            val filteredBitmap = if (filterIndex == 0) {
                // OPTIMIZATION: If "Normal" is selected, SKIP the GPU entirely.
                // This guarantees the original colors and saves processing time.
                rotatedBitmap
            } else {
                val filterToApply: GPUImageFilter = when (filterIndex) {
                    0 -> GPUImageColorMatrixFilter(1.0f, ColorMatrix().array)
                    1 -> VintageFilter()
                    2 -> ColdFilter()
                    3 -> WarmFilter()
                    4 -> RetroSepiaFilter()
                    5 -> createDramaticFilter()
                    else -> GPUImageColorMatrixFilter(1.0f, ColorMatrix().array)
                }
                applyGpuImage(appContext, rotatedBitmap, filterToApply)
            }
            
            var frameBitmap: Bitmap? = null
            val latch = java.util.concurrent.CountDownLatch(1)

            requireActivity().runOnUiThread {
                binding.ivFrame.isDrawingCacheEnabled = true
                val cache = binding.ivFrame.drawingCache
                if (cache != null) {
                    frameBitmap = Bitmap.createBitmap(cache)
                }
                binding.ivFrame.isDrawingCacheEnabled = false
                latch.countDown()
            }
            latch.await()

            val finalBitmap = if (frameBitmap != null) {
                mergeBitmaps(filteredBitmap, frameBitmap!!)
            } else {
                filteredBitmap
            }

            // 6. Save
            saveImageToFile(finalBitmap, filterIndex != null)

            // 7. Cleanup
            if (originalBitmap != rotatedBitmap) originalBitmap.recycle()
            if (rotatedBitmap != filteredBitmap && rotatedBitmap != finalBitmap) rotatedBitmap.recycle()
            if (filteredBitmap != finalBitmap) filteredBitmap.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Error saving", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
        }
    }

    /**
     * UPDATED FUNCTION: Now accepts 'GPUImageFilter' instead of 'ColorMatrix'.
     * This makes it compatible with VintageFilter AND Matrix filters.
     */
    private fun applyGpuImage(context: Context, src: Bitmap, filter: GPUImageFilter): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(src)
        gpuImage.setFilter(filter)
        return gpuImage.bitmapWithFilterApplied
    }

    // --- FIX ENDS HERE ---

    private fun rotateAndFlipBitmap(source: Bitmap, angle: Float, flipHorizontal: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        if (flipHorizontal) matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun mergeBitmaps(photo: Bitmap, frame: Bitmap): Bitmap {
        val width = photo.width
        val height = photo.height
        val merged = Bitmap.createBitmap(width, height, photo.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(merged)

        canvas.drawBitmap(photo, 0f, 0f, null)
        val scaledFrame = Bitmap.createScaledBitmap(frame, width, height, true)
        canvas.drawBitmap(scaledFrame, 0f, 0f, null)

        return merged
    }

    private fun saveImageToFile(bitmap: Bitmap, isFiltered: Boolean) {
        val prefix = if (isFiltered) "Filtered" else "Normal"
        val name = "${prefix}_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val safeContext = context ?: return
        val photoFile = File(safeContext.filesDir, "$name.jpg")

        try {
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            if (isAdded && activity != null) {
                requireActivity().runOnUiThread {
                    updateGalleryThumbnail()
                    setLoadingState(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save", e)
            setLoadingState(false)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(aspect)
                            .build()
                    )
                    .build().also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(aspect)
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

    private fun setLoadingState(isLoading: Boolean) {
        activity?.runOnUiThread {
            binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.galleryButton.isEnabled = !isLoading
            binding.galleryButton.alpha = if (isLoading) 0.5f else 1.0f
            binding.buttonShutter.isEnabled = !isLoading
            binding.switchCameraButton.isEnabled = !isLoading
        }
    }

    private fun triggerShutterEffect() {
        binding.shutterFlashView.apply {
            visibility = View.VISIBLE
            alpha = 1f
            animate().alpha(0f).setDuration(100)
                .withEndAction { visibility = View.GONE }
                .start()
        }
    }

    private fun toggleSidebar() {
        val sidebar = binding.rvFilterSidebar
        val isVisible = sidebar.visibility == View.VISIBLE

        if (isVisible) {
            // HIDE: Slide out to the right -> Then set GONE
            sidebar.animate()
                .translationX(sidebar.width.toFloat()) // Move right
                .alpha(0f) // Fade out
                .setDuration(300)
                .withEndAction {
                    sidebar.visibility = View.GONE
                }
                .start()
        } else {
            // SHOW: Set VISIBLE -> Slide in from right
            sidebar.visibility = View.VISIBLE
            sidebar.translationX = sidebar.width.toFloat() // Start off-screen
            sidebar.alpha = 0f

            sidebar.animate()
                .translationX(0f) // Move to position
                .alpha(1f) // Fade in
                .setDuration(300)
                .start()
        }
    }

    private fun updateGalleryThumbnail() {
        val files = requireContext().filesDir.listFiles { _, name ->
            name.endsWith(".jpg") || name.endsWith(".jpeg")
        }
        if (!files.isNullOrEmpty()) {
            files.sortByDescending { it.lastModified() }
            requireActivity().runOnUiThread {
                binding.galleryButton.load(files.first()) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        }
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

    override fun onResume() {
        super.onResume()
        updateGalleryThumbnail()
    }

    companion object {
        private const val TAG = "PictoApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}