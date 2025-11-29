package id.ac.pnm.photofilterapp.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import id.ac.pnm.photofilterapp.R
import id.ac.pnm.photofilterapp.adapter.GalleryAdapter
import id.ac.pnm.photofilterapp.databinding.FragmentGalleryBinding
import java.io.File
import java.io.FileOutputStream

class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GalleryAdapter

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importImageToInternalStorage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = GalleryAdapter { uri ->
            val bundle = Bundle().apply { putString("imageUri", uri.toString()) }
            findNavController().navigate(R.id.action_gallery_to_photoView, bundle)
        }
        
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(context, 3)

        binding.importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            importLauncher.launch(intent)
        }

        loadInternalImages()
    }

    private fun loadInternalImages() {
        val imageList = mutableListOf<Uri>()
        val files = requireContext().filesDir.listFiles { _, name ->
            name.endsWith(".jpg") || name.endsWith(".jpeg")
        }

        files?.sortByDescending { it.lastModified() }
        files?.forEach { file ->
            imageList.add(Uri.fromFile(file))
        }
        
        adapter.submitList(imageList)
    }

    private fun importImageToInternalStorage(sourceUri: Uri) {
        val resolver = requireContext().contentResolver
        val name = "Imported_${System.currentTimeMillis()}.jpg"
        val destFile = File(requireContext().filesDir, name)

        try {
            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            loadInternalImages() // Refresh grid
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
