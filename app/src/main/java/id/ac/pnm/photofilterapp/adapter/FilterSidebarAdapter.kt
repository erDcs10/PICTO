package id.ac.pnm.photofilterapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import id.ac.pnm.photofilterapp.R

class FilterSidebarAdapter(
    private val filterNames: List<String>,
    private val onFilterSelected: (Int) -> Unit
) : RecyclerView.Adapter<FilterSidebarAdapter.FilterViewHolder>() {

    private var selectedIndex = 0

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvFilterName)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivFilterIcon)

        fun bind(name: String, position: Int) {
            tvName.text = name

            // Highlight selected item
            if (position == selectedIndex) {
                ivIcon.alpha = 1.0f
                tvName.alpha = 1.0f
                // You can also change border color here
            } else {
                ivIcon.alpha = 0.5f
                tvName.alpha = 0.5f
            }

            itemView.setOnClickListener {
                val previousIndex = selectedIndex
                selectedIndex = position
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedIndex)
                onFilterSelected(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_sidebar, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filterNames[position], position)
    }

    override fun getItemCount() = filterNames.size
}