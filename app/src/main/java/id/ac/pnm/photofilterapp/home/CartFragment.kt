package id.ac.pnm.photofilterapp.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import id.ac.pnm.photofilterapp.R
class CartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_cart, container, false)
        val button = view.findViewById<Button>(R.id.button)
        button.setOnClickListener {
            Toast.makeText(activity, "Item added to cart", Toast.LENGTH_SHORT).show()
        }
        return view
    }

}