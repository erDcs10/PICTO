package id.ac.pnm.photofilterapp.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import id.ac.pnm.photofilterapp.R
import id.ac.pnm.photofilterapp.databinding.FragmentCameraBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

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

        binding.imageCaptureButton.setOnClickListener { takePhoto() }

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

    private fun takePhoto() {
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
                        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees.toFloat())

                        val filteredBitmap = applyAutumnFilter(rotatedBitmap)

                        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                            .format(System.currentTimeMillis())
                        val photoFile = File(requireContext().filesDir, "$name.jpg")

                        FileOutputStream(photoFile).use { out ->
                            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }

                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Saved Autumn Photo!", Toast.LENGTH_SHORT).show()
                            updateGalleryThumbnail()
                        }

                        if (bitmap != rotatedBitmap) bitmap.recycle()
                        if (rotatedBitmap != filteredBitmap) rotatedBitmap.recycle()

                    } catch (e: Exception) {
                        Log.e(TAG, "Processing failed", e)
                    } finally {
                        image.close()
                    }
                }
            }
        )
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        if (angle == 0f) return source
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun applyAutumnFilter(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)

        val colorMatrix = ColorMatrix()

        val rScale = 1.3f
        val gScale = 1.1f
        val bScale = 0.9f

        colorMatrix.setScale(rScale, gScale, bScale, 1f)

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.2f)
        colorMatrix.postConcat(saturationMatrix)

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(src, 0f, 0f, paint)

        return dest
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder().build()

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
