package com.mapspeople.mapsindoorssamples.ui.locationdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.mapsindoors.coresdk.MPLocation
import com.mapsindoors.coresdk.MapControl
import com.mapsindoors.coresdk.MapsIndoors
import com.mapsindoors.coresdk.errors.MIError
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngBoundsConverter.toLatLngBounds
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentLocationDetailsBinding

class LocationDetailsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationDetailsBinding? = null

    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationDetailsBinding.inflate(inflater, container, false)

        MapsIndoors.load(requireActivity().applicationContext, "gettingstarted", null)

        val root: View = binding.root
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapView = supportMapFragment.view
        supportMapFragment.getMapAsync(this)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                //No errors so getting the first venue (in the white house solution the only one)
                val venue = MapsIndoors.getVenues()!!.currentVenue
                activity?.runOnUiThread {
                    if (venue != null) {
                        //Animates the camera to fit the new venue
                        mMap!!.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                toLatLngBounds(venue.bounds!!),
                                19
                            )
                        )
                    }
                }
            }

            setupListener()
        }
    }

    private fun setupListener() {
        mMapControl?.let { mapControl ->
            mapControl.setOnLocationSelectedListener {
                if (it != null) {
                    showLocationDetails(it)
                }
                return@setOnLocationSelectedListener false
            }

            mapControl.setOnMarkerInfoWindowCloseListener {
                binding.detailsTextView.visibility = View.GONE
                mMapControl?.setMapPadding(0, 0, 0, 0)
            }
        }
    }

    private fun showLocationDetails(location: MPLocation) {
        binding.detailsTextView.text =  "Name: " + location.name + "\nDescription: " + location.description
        binding.detailsTextView.visibility = View.VISIBLE
        mMapControl?.setMapPadding(0, 0, 0, binding.detailsTextView.height)
    }

}