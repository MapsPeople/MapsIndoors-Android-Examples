package com.mapspeople.mapsindoorssamples.ui.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.*
import com.mapsindoors.core.MPSelectionBehavior
import com.mapsindoors.core.MapControl
import com.mapsindoors.core.MapsIndoors
import com.mapsindoors.core.errors.MIError
import com.mapsindoors.googlemaps.MPMapConfig
import com.mapsindoors.googlemaps.converters.LatLngBoundsConverter
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentSearchBinding

class SearchFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private var mMapView: View? = null
    private var mMap: GoogleMap? = null
    private var mMapControl: MapControl? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        MapsIndoors.load(requireActivity().applicationContext, "d876ff0e60bb430b8fabb145", null)

        val root: View = binding.root
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapView = supportMapFragment.view
        supportMapFragment.getMapAsync(this)

        binding.searchButton.setOnClickListener {
            openSearchFragment()
        }

        return root
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
        if (mMapView != null) {
            initMapControl(mMapView!!)
        }
    }

    private fun initMapControl(view: View) {
        val mapConfig: MPMapConfig = MPMapConfig.Builder(requireActivity(), mMap!!, getString(R.string.google_maps_key), view, true).build()
        //Creates a new instance of MapControl
        MapControl.create(mapConfig) { mapControl: MapControl?, miError: MIError? ->
            mMapControl = mapControl
            //Enable Live Data on the map
            if (miError == null) {
                var locationId = arguments?.get("locationId") as String?
                if (locationId != null) {
                    mMapControl?.selectLocation(locationId, MPSelectionBehavior.DEFAULT)
                }else {
                    //No errors so getting the first venue (in the white house solution the only one)
                    val venue = MapsIndoors.getVenues()?.defaultVenue
                    activity?.runOnUiThread {
                        if (venue != null) {
                            //Animates the camera to fit the new venue
                            mMap!!.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    LatLngBoundsConverter.toLatLngBounds(venue.bounds!!),
                                    19
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openSearchFragment() {
        val navController = findNavController()
        navController.navigate(R.id.action_nav_search_to_nav_search_fullscreen)
    }
}