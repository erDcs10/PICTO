package id.ac.pnm.photofilterapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import id.ac.pnm.photofilterapp.R
import id.ac.pnm.photofilterapp.databinding.ItemCaptureButtonBinding

class CaptureButtonAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<CaptureButtonAdapter.ButtonViewHolder>() {

    private val modes = listOf("NORMAL", "FILTER")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val binding = ItemCaptureButtonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ButtonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(modes[position])
    }

    override fun getItemCount(): Int = modes.size

    inner class ButtonViewHolder(private val binding: ItemCaptureButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mode: String) {
            val context = binding.root.context
            
            if (mode == "NORMAL") {
                binding.captureButton.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
                binding.captureButton.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.corn)
                )
            }

            binding.captureButton.setOnClickListener {
                onClick()
            }
        }
    }
}
