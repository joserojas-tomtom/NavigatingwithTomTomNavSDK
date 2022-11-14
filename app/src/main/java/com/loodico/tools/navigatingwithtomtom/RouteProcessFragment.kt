package com.loodico.tools.navigatingwithtomtom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_ADDRESS = "address"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RouteProcessFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RouteProcessFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var address: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            address = it.getString(ARG_ADDRESS)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: ViewGroup = inflater.inflate(R.layout.fragment_route_process, container, false) as ViewGroup
        val addressField: TextView = view.findViewById(R.id.address)
        addressField.setText(address)
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RouteProcessFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RouteProcessFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ADDRESS, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}