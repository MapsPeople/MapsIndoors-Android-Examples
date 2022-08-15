package com.mapspeople.mapsindoorssamples.ui.wayfinding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.mapsindoors.coresdk.*
import com.mapsindoors.coresdk.errors.MIError
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngBoundsConverter
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentWayfindingBinding

class WayfindingFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentWayfindingBinding? = null

    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    private var mRoute: MPRoute? = null
    private var mLocation: MPLocation? = null

    private var mDirectionsRenderer: MPDirectionsRenderer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWayfindingBinding.inflate(inflater, container, false)

        MapsIndoors.load(requireActivity().applicationContext, "gettingstarted", null)

        val routeLegAdapter = RouteCollectionAdapter(this)
        val viewPager = binding.stepViewPager
        viewPager.adapter = routeLegAdapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mDirectionsRenderer?.selectLegIndex(position)
                mDirectionsRenderer?.selectedLegFloorLevel
            }
        })

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

                getRoute()

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

    fun getRoute() {
        val directionsService = MPDirectionsService(requireContext())
        if (mDirectionsRenderer == null) {
            mDirectionsRenderer = MPDirectionsRenderer(mMapControl!!)
        }

        directionsService.setRouteResultListener { mpRoute, miError ->
            if (miError == null && mpRoute != null) {
                mRoute = mpRoute
                mDirectionsRenderer?.setRoute(mpRoute)
                requireActivity().runOnUiThread {
                    binding.stepViewPager.adapter?.notifyDataSetChanged()
                }
            }
        }
        val location = MapsIndoors.getLocationById("5a07435a4e074edc9396b2ff")
        mLocation = MapsIndoors.getLocationById("24ede0c9a5004a148bd01d96")
        if (location != null && mLocation != null) {
            directionsService.query(location.point, mLocation!!.point)
        }
    }

    fun getStepName(startStep: MPRouteStep, endStep: MPRouteStep): String {
        val startStepZindex: Double = startStep.startLocation!!.zIndex
        val startStepFloorName: String = startStep.startLocation.floorName!!
        var highway: String? = null
        for (actionName in getActionNames()) {
            if (startStep.highway == actionName) {
                highway = if (actionName == MPHighway.STEPS) {
                    "stairs"
                } else {
                    actionName
                }
            }
        }
        if (highway != null) {
            return java.lang.String.format(
                "Take %s to %s %s",
                highway,
                "level",
                if (endStep.endLocation.floorName!!.isEmpty()) endStep.endLocation.zIndex else endStep.endLocation.floorName
            )
        }
        if (startStepFloorName == endStep.endLocation.floorName) {
            return "Walk to next step"
        }
        val endStepFloorName: String = endStep.endLocation.floorName!!
        return if (endStepFloorName.isEmpty()) java.lang.String.format(
            "Level %s to %s",
            startStepFloorName.ifEmpty { startStepZindex },
            endStep.endPoint.floorIndex
        ) else String.format(
            "Level %s to %s",
            startStepFloorName.ifEmpty { startStepZindex },
            endStepFloorName
        )
    }

    private fun getActionNames(): ArrayList<String> {
        val actionNames: ArrayList<String> = ArrayList()
        actionNames.add(MPHighway.ELEVATOR)
        actionNames.add(MPHighway.ESCALATOR)
        actionNames.add(MPHighway.STEPS)
        actionNames.add(MPHighway.TRAVELATOR)
        actionNames.add(MPHighway.RAMP)
        actionNames.add(MPHighway.WHEELCHAIRLIFT)
        actionNames.add(MPHighway.WHEELCHAIRRAMP)
        actionNames.add(MPHighway.LADDER)
        return actionNames
    }

    inner class RouteCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            mRoute?.legs?.let { legs->
                return legs.size
            }
            return 0
        }

        override fun createFragment(position: Int): Fragment {
            if (position == mRoute?.legs?.size!! - 1) {
                return RouteLegFragment.newInstance("Walk to " + mLocation?.name, mRoute?.legs!![position]?.distance?.toInt(), mRoute?.legs!![position]?.duration?.toInt())
            } else {
                var leg = mRoute?.legs!![position]
                var firstStep = leg.steps.first()
                var lastFirstStep = mRoute?.legs!![position + 1].steps.first()
                var lastStep = mRoute?.legs!![position + 1].steps.last()

                var firstBuilding = MapsIndoors.getBuildings()?.getBuilding(firstStep.startPoint.latLng)
                var lastBuilding  = MapsIndoors.getBuildings()?.getBuilding(lastStep.startPoint.latLng)
                return if (firstBuilding != null && lastBuilding != null) {
                    RouteLegFragment.newInstance(getStepName(lastFirstStep, lastStep), leg.distance.toInt(), leg.duration.toInt())
                }else if (firstBuilding != null) {
                    RouteLegFragment.newInstance("Exit: " + firstBuilding.name,  leg.distance.toInt(), leg.duration.toInt())
                }else {
                    RouteLegFragment.newInstance("Enter: " + lastBuilding?.name,  leg.distance.toInt(), leg.duration.toInt())
                }
            }
        }
    }
}