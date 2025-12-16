package id.ac.pnm.photofilterapp.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import id.ac.pnm.photofilterapp.adapter.EditAdapter
import id.ac.pnm.photofilterapp.databinding.FragmentPhotoEditorBinding

class PhotoEditFragment : Fragment() {

    private var _binding: FragmentPhotoEditorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoUriString = arguments?.getString("photoUri")
        if (photoUriString != null) {
            binding.ivMainPhoto.setImageURI(Uri.parse(photoUriString))
        }

        val filters = listOf("Original", "Warm", "Cool", "B&W", "Sepia", "Vintage")

        val adapter = EditAdapter(filters)
        binding.rvFilters.adapter = adapter
        binding.rvFilters.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}