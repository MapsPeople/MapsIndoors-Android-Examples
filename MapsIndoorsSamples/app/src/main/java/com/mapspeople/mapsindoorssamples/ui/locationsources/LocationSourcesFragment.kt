package com.mapspeople.mapsindoorssamples.ui.locationsources

import android.graphics.Color
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
import com.mapsindoors.coresdk.models.MPLatLng
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngBoundsConverter
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentLocationSourcesBinding
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random.Default.nextInt


class LocationSourcesFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationSourcesBinding? = null
    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    private var BASE_POSITION = MPLatLng(57.0582502, 9.9504788)
    private var baseDisplayRule: WeakReference<MPDisplayRule?>? = null
    private var robotDisplayRule: MPDisplayRule? = null
    private var mLocations: ArrayList<MPLocation>? = null
    private var mRobotVacuumLocationSource: RobotVacuumLocationSource? = null

    private var mUpdateTimer: Timer? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationSourcesBinding.inflate(inflater, container, false)
        MapsIndoors.load(requireActivity().applicationContext, "mapspeople") { error ->
            if (error == null) {
                baseDisplayRule = WeakReference(MapsIndoors.getMainDisplayRule())
                setupLocationSource()
            }
        }

        val root: View = binding.root
        val supportMapFragment= childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
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
        }
    }

    private fun setupLocationSource() {
        if (mLocations == null) {
            generateLocations()
        }

        val locationSource = RobotVacuumLocationSource(mLocations!!)
        MapsIndoors.addLocationSources(Collections.singletonList(locationSource) as List<MPLocationSource>) {
        }
        locationSource.setup()

        startUpdatingPositions()
    }

    private fun generateLocations() {
        mLocations = ArrayList()
        for (i in 0..15) {
            val robotName = "vacuum$i"
            val startPosition = getRandomPosition()
            val charge = nextInt(1, 100)
            val floorIndex = nextInt(4) * 10

            var mpLocation = MPLocation.Builder(robotName)
                .setPosition(startPosition.lat, startPosition.lng)
                .setFloorIndex(floorIndex)
                .setName(robotName)
                .setBuilding("Stigsborgvej")
                .build()

            robotDisplayRule = MPDisplayRule(robotName, baseDisplayRule!!)
            robotDisplayRule?.isVisible = true

            if (charge >= 60) {
                robotDisplayRule?.setIcon(R.drawable.ic_baseline_robo_vacuum, Color.GREEN)
            }else if (charge >= 30) {
                robotDisplayRule?.setIcon(R.drawable.ic_baseline_robo_vacuum, Color.YELLOW)
            }else {
                robotDisplayRule?.setIcon(R.drawable.ic_baseline_robo_vacuum, Color.RED)
            }

            MapsIndoors.addDisplayRuleForLocation(mpLocation, robotDisplayRule!!)
            mLocations?.add(mpLocation)
        }
    }

    private fun getRandomPosition(): MPLatLng {
        val lat: Double = BASE_POSITION.lat + (-4 + nextInt(20)) * 0.000005
        val lng: Double = BASE_POSITION.lng + (-4 + nextInt(20)) * 0.000010
        return MPLatLng(lat, lng)
    }

    fun updateLocations() {
        var updatedLocations = ArrayList<MPLocation>()
        mLocations?.forEach {
            var newPosition = getRandomPosition()
            var newLocation = MPLocation.Builder(it).setPosition(MPPoint(newPosition.lat, newPosition.lng))
            var charge = nextInt(1, 100)
            updatedLocations.add(newLocation.build())

            robotDisplayRule = MPDisplayRule("robot", baseDisplayRule!!)
            robotDisplayRule?.isVisible = true

            if (charge >= 60) {
                robotDisplayRule?.setIcon(R.drawable.ic_baseline_robo_vacuum, Color.GREEN)
            }else if (charge >= 30) {
                robotDisplayRule?.setIcon(R.drawable.ic_baseline_robo_vacuum, Color.YELLOW)
            }else {
                robotDisplayRule?.setIcon(R.drawable.ic_baseline_robo_vacuum, Color.RED)
            }

            MapsIndoors.addDisplayRuleForLocation(it, robotDisplayRule!!)
        }
        mRobotVacuumLocationSource?.updateLocations(updatedLocations)
    }

    private fun startUpdatingPositions() {
        mUpdateTimer?.cancel()
        mUpdateTimer = Timer()

        mUpdateTimer?.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                updateLocations();
            }
        }, 2000, 500)
    }

    fun stopUpdatingPositions() {
        mUpdateTimer?.cancel()
        mUpdateTimer?.purge()
    }
}