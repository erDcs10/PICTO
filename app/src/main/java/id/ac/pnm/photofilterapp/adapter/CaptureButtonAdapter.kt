package id.ac.pnm.photofilterapp.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import id.ac.pnm.photofilterapp.databinding.ItemCaptureButtonBinding
import id.ac.pnm.photofilterapp.filter.FilterConfig

class CaptureButtonAdapter(
    private val filters: List<FilterConfig>,
    private val onClick: () -> Unit
) : RecyclerView.Adapter<CaptureButtonAdapter.ButtonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val binding = ItemCaptureButtonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ButtonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(filters[position])
    }

    override fun getItemCount(): Int = filters.size

    inner class ButtonViewHolder(private val binding: ItemCaptureButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(filter: FilterConfig) {
            val context = binding.root.context
            
            binding.captureButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, filter.buttonColorRes)
            )

            binding.captureButton.setOnClickListener {
                onClick()
            }
        }
    }
}
