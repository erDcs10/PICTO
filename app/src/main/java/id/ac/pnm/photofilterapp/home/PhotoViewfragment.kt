package id.ac.pnm.photofilterapp.home

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.yalantis.ucrop.UCrop
import id.ac.pnm.photofilterapp.R
import id.ac.pnm.photofilterapp.adapter.PhotoPagerAdapter
import id.ac.pnm.photofilterapp.databinding.FragmentPhotoViewBinding
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class PhotoViewFragment : Fragment() {
    private var _binding: FragmentPhotoViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var pagerAdapter: PhotoPagerAdapter
    private var imageList = mutableListOf<Uri>()

    private val uri = arguments?.getString("photoUri") ?: ""

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {

                loadInternalImages()
                Toast.makeText(context, "Image Cropped & Saved", Toast.LENGTH_SHORT).show()

                val index = imageList.indexOf(it)
                if (index != -1) {
                    binding.viewPager.setCurrentItem(index, false)
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data!!)
            Toast.makeText(context, "Crop Error: ${error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = PhotoPagerAdapter(imageList)
        binding.viewPager.adapter = pagerAdapter

        loadInternalImages()

        val initialUriString = arguments?.getString("imageUri")
        if (initialUriString != null) {
            val initialUri = Uri.parse(initialUriString)
            val index = imageList.indexOf(initialUri)
            if (index != -1) {
                binding.viewPager.setCurrentItem(index, false)
            }
        }

        binding.btnShare.setOnClickListener {
            // 1. Get the URI from your list
            val currentUri = getCurrentUri()

            if (currentUri == null) {
                Toast.makeText(requireContext(), "Message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val shareUri: Uri = if (currentUri.scheme == "file") {
                    val file = java.io.File(currentUri.path!!)
                    androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider", // Use requireContext().packageName
                        file
                    )
                } else {
                    currentUri
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg" // Or "image/*" to be safe
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share Image"))

            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Message", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDelete.setOnClickListener {
            val currentUri = getCurrentUri() ?: return@setOnClickListener
            try {
                val file = File(currentUri.path!!)
                if (file.exists()) file.delete()

                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                loadInternalImages()

                if (imageList.isEmpty()) {
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCrop.setOnClickListener {
            val currentUri = getCurrentUri() ?: return@setOnClickListener

            val destFileName = "Cropped_${UUID.randomUUID()}.jpg"
            val destFile = File(requireContext().filesDir, destFileName)
            val destUri = Uri.fromFile(destFile)

            val uCrop = UCrop.of(currentUri, destUri)
            val intent = uCrop.getIntent(requireContext())
            cropLauncher.launch(intent)
        }

        binding.btnFilter.setOnClickListener {
            // 1. Get the URI of the image currently visible on screen
            val currentImageUri = getCurrentUri()

            if (currentImageUri != null) {
                val bundle = Bundle().apply {
                    // 2. FIX: Key must be "photoUri" to match your navigation XML
                    // 3. FIX: Pass currentImageUri.toString(), NOT the old 'uri' variable
                    putString("photoUri", currentImageUri.toString())
                }

                // Navigate
                findNavController().navigate(R.id.action_photoViewFragment_to_photoEditFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Error: No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadInternalImages() {
        imageList.clear()
        val files = requireContext().filesDir.listFiles { _, name ->
            name.endsWith(".jpg") || name.endsWith(".jpeg")
        }

        files?.sortByDescending { it.lastModified() }
        files?.forEach { file ->
            imageList.add(Uri.fromFile(file))
        }
        pagerAdapter.updateList(imageList)
    }

    private fun getCurrentUri(): Uri? {
        val position = binding.viewPager.currentItem
        if (position in imageList.indices) {
            return imageList[position]
        }
        return null
    }

    private fun saveToPublicGallery(srcUri: Uri) {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Picto_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Picto")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (destUri != null) {
                val srcFile = File(srcUri.path!!)
                FileInputStream(srcFile).use { input ->
                    resolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(destUri, contentValues, null, null)
                }
                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Save Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
