package com.mapspeople.mapsindoorssamples.ui.reverseGeoCode

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.mapsindoors.core.MPPoint
import com.mapsindoors.core.MapControl
import com.mapsindoors.core.MapsIndoors
import com.mapsindoors.core.errors.MIError
import com.mapsindoors.googlemaps.MPMapConfig
import com.mapsindoors.googlemaps.converters.LatLngBoundsConverter
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentReverseGeoCodeBinding

class ReverseGeoCodeFragment : Fragment(), OnMapReadyCallback{
    private var _binding: FragmentReverseGeoCodeBinding? = null

    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReverseGeoCodeBinding.inflate(inflater, container, false)

        MapsIndoors.load(requireActivity().applicationContext, "57e4e4992e74800ef8b69718", null)

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

            setupListener()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupListener() {
        mMapControl?.let { mapControl ->
            //Use a MapClick Listener to get a coordinate to perform the reverse geocode with.
            mapControl.setOnMapClickListener { mpLatLng, mpLocations ->
                //In this case we just ignore the locations send back from onMapClickListener and perform a reverse geocode with the LatLng along with our current floorIndex to filter for only the visible geometries.
                MapsIndoors.reverseGeoCode(MPPoint(mpLatLng.lat, mpLatLng.lng, mapControl.currentFloorIndex.toDouble())) { mpGeoCodeResult ->
                    //Here we just create a text view that states the amount of geometries within the coordinate by each location type.
                    activity?.runOnUiThread {
                        binding.detailsTextView.text =
                            "Locations at click: ${System.lineSeparator()}Venues: ${mpGeoCodeResult.venues.size}${System.lineSeparator()}Buildings: ${mpGeoCodeResult.buildings.size}${System.lineSeparator()}Floors: ${mpGeoCodeResult.floors.size}${System.lineSeparator()}Rooms: ${mpGeoCodeResult.rooms.size}${System.lineSeparator()}Areas: ${mpGeoCodeResult.areas.size}"
                        binding.detailsTextView.visibility = View.VISIBLE
                        mMapControl?.setMapPadding(0, 0, 0, binding.detailsTextView.height)
                    }
                }
                //Return true for the listener to not delegate it back to mapcontrol
                return@setOnMapClickListener true
            }
        }
    }
}