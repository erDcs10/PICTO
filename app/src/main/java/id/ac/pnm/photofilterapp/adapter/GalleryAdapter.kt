package id.ac.pnm.photofilterapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import id.ac.pnm.photofilterapp.databinding.ItemGalleryImageBinding

class GalleryAdapter(private val onClick: (Uri) -> Unit) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    private var images = listOf<Uri>()

    fun submitList(newImages: List<Uri>) {
        images = newImages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount() = images.size

    inner class ViewHolder(private val binding: ItemGalleryImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            binding.imageView.load(uri) {
                crossfade(true)
            }
            binding.root.setOnClickListener { onClick(uri) }
        }
    }
}
