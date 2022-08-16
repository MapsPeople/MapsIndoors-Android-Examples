package com.mapspeople.mapsindoorssamples.ui.positioning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.mapsindoors.coresdk.*
import com.mapsindoors.coresdk.errors.MIError
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngConverter
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentPositioningBinding

class PositioningFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentPositioningBinding? = null

    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    private var mPositionProvider: PositionProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPositioningBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapView = supportMapFragment.view
        supportMapFragment.getMapAsync(this)

        // Create a position provider
        mPositionProvider = PositionProvider()

        MapsIndoors.load(requireActivity().applicationContext, "gettingstarted") {
            // Attach the position provider to the SDK
            MapsIndoors.setPositionProvider(mPositionProvider)
        }

        binding.startPositioning.setOnClickListener {
            mPositionProvider?.start()
        }

        binding.stopPositioning.setOnClickListener {
            mPositionProvider?.stop()
        }

        mPositionProvider?.addOnPositionUpdateListener(object: OnPositionUpdateListener {
            override fun onPositioningStarted(provider: MPPositionProvider) {}

            override fun onPositionFailed(provider: MPPositionProvider) {}

            override fun onPositionUpdate(position: MPPositionResultInterface) {
                val bluedot = MapsIndoors.getDisplayRule(MPSolutionDisplayRule.POSITION_INDICATOR)
                bluedot.let {
                    if(position.hasBearing())
                        it?.setIcon(R.drawable.ic_bluedot_bearing)
                    else
                        it?.setIcon(R.drawable.ic_bluedot)
                }
                activity?.runOnUiThread {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLngConverter.toLatLng(position.point!!.latLng), 20f))
                }
            }

        })

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

            // Enable showing the position indicator (aka. the blue dot)
            mMapControl?.showUserPosition(true)

            if (miError == null) {
                val venue = MapsIndoors.getVenues()!!.currentVenue
                activity?.runOnUiThread {
                    venue?.let { mMapControl?.goTo(venue) }
                }
            }
        }
    }

}