package com.mapspeople.mapsindoorssamples.ui.livedata

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.mapsindoors.coresdk.MapControl
import com.mapsindoors.coresdk.MapsIndoors
import com.mapsindoors.coresdk.errors.MIError
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngBoundsConverter
import com.mapsindoors.livesdk.LiveDataDomainTypes
import com.mapsindoors.livesdk.LiveDataManager
import com.mapsindoors.livesdk.LiveTopicCriteria
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentLivedataBinding

class LivedataFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentLivedataBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    private var mBuildingLiveTopicCriteria: LiveTopicCriteria? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLivedataBinding.inflate(inflater, container, false)
        MapsIndoors.load(requireActivity().applicationContext, "d876ff0e60bb430b8fabb145", null)

        val root: View = binding.root
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapView = supportMapFragment.view
        supportMapFragment.getMapAsync(this)

        binding.positioningButton.setOnClickListener {
            if (!binding.positioningButton.isSelected) {
                binding.positioningButton.isSelected = true
                mMapControl?.enableLiveData(LiveDataDomainTypes.POSITION_DOMAIN)
            }else {
                binding.positioningButton.isSelected = false
                mMapControl?.disableLiveData(LiveDataDomainTypes.POSITION_DOMAIN)
            }
        }

        binding.occupancyButton.setOnClickListener {
            if (!binding.occupancyButton.isSelected) {
                binding.occupancyButton.isSelected = true
                mMapControl?.enableLiveData(LiveDataDomainTypes.OCCUPANCY_DOMAIN)
            }else {
                binding.occupancyButton.isSelected = false
                mMapControl?.disableLiveData(LiveDataDomainTypes.OCCUPANCY_DOMAIN)
            }
        }

        binding.occupancyButton2.setOnClickListener {
            getOccupancyForLocations()
        }

        binding.singlePositioningButton.setOnClickListener {
            getPositionUpdatesForSpecificLocation()
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
                //No errors so getting the first venue (in the white house solution the only one)
                val venue = MapsIndoors.getVenues()!!.currentVenue
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

    private fun getOccupancyForLocations() {
        val liveDataManager = LiveDataManager.getInstance()
        var id = MapsIndoors.getDataSet()?.id
        id?.let { datasetId ->
            mMapControl?.setOnCurrentBuildingChangedListener {
                if (it != null) {
                    if (mBuildingLiveTopicCriteria != null) {
                        liveDataManager.unsubscribeTopic(mBuildingLiveTopicCriteria)
                    }

                    mBuildingLiveTopicCriteria = LiveTopicCriteria.BuilderImpl(datasetId)
                        .setBuildingId(it.id)
                        .setDomainType(LiveDataDomainTypes.OCCUPANCY_DOMAIN)
                        .build()
                    liveDataManager.subscribeTopic(mBuildingLiveTopicCriteria)
                }
            }
            if (mBuildingLiveTopicCriteria == null) {
                MapsIndoors.getBuildings()?.buildings?.get(1)?.let {
                    mBuildingLiveTopicCriteria = LiveTopicCriteria.BuilderImpl(datasetId)
                        .setBuildingId(it.id)
                        .setDomainType(LiveDataDomainTypes.OCCUPANCY_DOMAIN)
                        .build()
                    liveDataManager.subscribeTopic(mBuildingLiveTopicCriteria)
                }
            }
        }

        liveDataManager.setOnReceivedLiveUpdateListener { mpLiveTopic, liveUpdate ->
            if (liveUpdate.domainType.equals(LiveDataDomainTypes.OCCUPANCY_DOMAIN)) {
                var location = MapsIndoors.getLocationById(liveUpdate.id)
                if (location != null) {
                    val nrOfPeople = liveUpdate.occupancyProperties.noOfPeople
                    MapsIndoors.getDisplayRule(location)?.isLabelVisible = true
                    MapsIndoors.getDisplayRule(location)?.label = "People: $nrOfPeople"
                }
            }
        }
    }

    private fun getPositionUpdatesForSpecificLocation() {
        val liveDataManager = LiveDataManager.getInstance()
        var id = MapsIndoors.getDataSet()?.id
        id?.let {
            val liveTopicCriteria = LiveTopicCriteria.BuilderImpl(it)
                .setLocationId("1e43c533c5c0403ba99cecae")
                .setDomainType(LiveDataDomainTypes.POSITION_DOMAIN)
                .build()
            liveDataManager.subscribeTopic(liveTopicCriteria)
        }

        liveDataManager.setOnReceivedLiveUpdateListener { mpLiveTopic, liveUpdate ->
            val point = MapsIndoors.getLocationById("1e43c533c5c0403ba99cecae")?.point
            point?.let {
                activity?.runOnUiThread {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.lng), 19f))
                }
            }
        }
    }
}