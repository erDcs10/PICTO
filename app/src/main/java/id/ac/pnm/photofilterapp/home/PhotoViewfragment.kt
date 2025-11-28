package id.ac.pnm.photofilterapp.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.yalantis.ucrop.UCrop
import id.ac.pnm.photofilterapp.databinding.FragmentPhotoViewBinding
import java.io.File
import java.util.UUID

class PhotoViewFragment : Fragment() {
    private var _binding: FragmentPhotoViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentUri: Uri

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                currentUri = it
                binding.fullImageView.load(it)
                Toast.makeText(context, "Image Cropped", Toast.LENGTH_SHORT).show()
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
        val uriString = arguments?.getString("imageUri") ?: return
        currentUri = Uri.parse(uriString)

        binding.fullImageView.load(currentUri)

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, currentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        }

        binding.btnDelete.setOnClickListener {
            try {
                requireContext().contentResolver.delete(currentUri, null, null)
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCrop.setOnClickListener {
            val destFileName = "Cropped_${UUID.randomUUID()}.jpg"
            val destFile = File(requireContext().cacheDir, destFileName)
            val destUri = Uri.fromFile(destFile)
            
            val uCrop = UCrop.of(currentUri, destUri)
            val intent = uCrop.getIntent(requireContext())
            cropLauncher.launch(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
