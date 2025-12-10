package id.ac.pnm.photofilterapp.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import id.ac.pnm.photofilterapp.databinding.ItemFilterSidebarBinding
import id.ac.pnm.photofilterapp.filter.FilterConfig

class FilterSidebarAdapter(
    private val filters: List<FilterConfig>,
    private val onFilterSelected: (FilterConfig) -> Unit
) : RecyclerView.Adapter<FilterSidebarAdapter.FilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemFilterSidebarBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position])
    }

    override fun getItemCount(): Int = filters.size

    inner class FilterViewHolder(private val binding: ItemFilterSidebarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(filter: FilterConfig) {
            val context = binding.root.context
            
            // Set the color of the filter indicator
            binding.filterIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, filter.buttonColorRes)
            )

            binding.root.setOnClickListener {
                onFilterSelected(filter)
            }
        }
    }
}
