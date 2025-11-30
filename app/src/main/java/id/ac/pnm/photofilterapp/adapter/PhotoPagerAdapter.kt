package id.ac.pnm.photofilterapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import id.ac.pnm.photofilterapp.databinding.ItemPhotoPagerBinding

class PhotoPagerAdapter(private var photos: List<Uri>) : RecyclerView.Adapter<PhotoPagerAdapter.ViewHolder>() {

    fun updateList(newPhotos: List<Uri>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Uri {
        return photos[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoPagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount() = photos.size

    class ViewHolder(private val binding: ItemPhotoPagerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            binding.fullImageView.load(uri) {
                crossfade(true)
            }
        }
    }
}
