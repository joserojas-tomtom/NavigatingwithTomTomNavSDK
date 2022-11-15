package com.loodico.tools.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.Button
import com.loodico.tools.NavDemoApp.R
import com.tomtom.sdk.common.location.GeoCoordinate
import com.tomtom.sdk.search.ui.model.Place

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_ADDRESS = "address"

/**
 * A simple [Fragment] subclass.
 * Use the [RouteProcessFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RouteProcessFragment(place: Place, navigationInterface: NavigateOptionsInterface) : Fragment() {
    private var address: String? = null
    private var listener: NavigateOptionsInterface = navigationInterface
    private var destination: Place = place

    interface NavigateOptionsInterface {
        fun onNavigate(destination: GeoCoordinate)
        fun onCancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            address = it.getString(ARG_ADDRESS)
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

        val navigateButton = view.findViewById<Button>(R.id.navigateButton)
        navigateButton.setOnClickListener { listener.onNavigate(destination.position) }

        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener { listener.onCancel() }

        return view
    }

    companion object {

        @JvmStatic
        fun newInstance(place: Place, listener: NavigateOptionsInterface) =
            RouteProcessFragment(place, listener).apply {
                arguments = Bundle().apply {
                    putString(ARG_ADDRESS, place.address)
                }
            }
    }
}