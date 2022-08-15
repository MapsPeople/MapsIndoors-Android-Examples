package com.mapspeople.mapsindoorstemplate

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapsindoors.coresdk.*
import com.mapsindoors.coresdk.errors.MIError
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngBoundsConverter
import com.mapspeople.mapsindoorstemplate.databinding.FragmentMapBinding
import kotlin.collections.ArrayList


class MapsFragment : Fragment(), OnMapReadyCallback, OnRouteResultListener {
    private var _binding: FragmentMapBinding? = null

    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private var mMapView: View? = null
    private lateinit var mMapControl: MapControl
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private var currentLocation: MPLocation? = null
    private var directionsRendered: Boolean = false

    private lateinit var mapFragment: SupportMapFragment

    private var mDirectionsRenderer: MPDirectionsRenderer? = null
    private var mDirectionsService: MPDirectionsService? = null
    private var lastSearchString: String? = null
    private var lastSearchList: ArrayList<MPLocation>? = null

    private val SEARCH_FRAGMENT = "search"
    private val ROUTING_FRAGMENT = "routing"
    private val DIRECTION_FRAGMENT = "direction"
    private val DESCRIPTION_FRAGMENT = "description"

    private var mOnLegSelectedListener: OnLegSelectedListener? = null

    private var currentFragment = "search"

    private var locationId: String? = null
    private var showDialog: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapView = mapFragment.view
        mapFragment.getMapAsync(this)
        setupBottomSheet()

        val userRoleStrings = arguments?.getStringArrayList("userroles")
        val userRoles = ArrayList<MPUserRole>()
        MapsIndoors.getUserRoles().forEach {
            if (userRoleStrings?.contains(it.value) == true) {
                userRoles.add(it)
            }
        }
        if (MapsIndoors.isReady()) {
            MapsIndoors.applyUserRoles(userRoles)
        }else {
            MapsIndoors.addOnMapsIndoorsReadyListener {
                MapsIndoors.applyUserRoles(userRoles)
            }
        }

        if (savedInstanceState == null) {
            binding.loadView.visibility = View.VISIBLE
        }else {
            locationId = savedInstanceState.getString("locationID")
        }

        binding.backBtn.setOnClickListener {
            //TODO: Change this to your app specific back navigation.
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment, null,  NavOptions.Builder().setLaunchSingleTop(true).build())
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (activity?.currentFocus != null && activity?.currentFocus is EditText) {
                        activity?.currentFocus?.clearFocus()
                    }else {
                        when (currentFragment) {
                            SEARCH_FRAGMENT -> {
                                if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                    setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
                                }else {
                                    isEnabled = false
                                    requireActivity().onBackPressed()
                                }
                            }
                            ROUTING_FRAGMENT -> {
                                if (currentLocation != null) {
                                    setDescriptionView(currentLocation!!)
                                }else {
                                    setSearchView()
                                }
                            }
                            DIRECTION_FRAGMENT -> {
                                mDirectionsRenderer?.clear()
                                if (currentLocation != null) {
                                    setDescriptionView(currentLocation!!)
                                }else {
                                    setSearchView()
                                }
                            }
                            DESCRIPTION_FRAGMENT -> {
                                mMapControl.deSelectLocation()
                                setSearchView()
                            }
                            else -> {
                                isEnabled = false
                                requireActivity().onBackPressed()
                            }
                        }
                    }
                }
            })
    }

    private fun initMapControl() {
        context?.let { _ ->
            MapsIndoors.getSolution()?.config?.setEnableClustering(false)
            val config = MPMapConfig.Builder(requireActivity(), mMap, getString(R.string.google_maps_key), mMapView!!, true).build()
            MapControl.create(config) { mapControl, miError ->
                if (mapControl != null && miError == null) {
                    mMapControl = mapControl
                    mMapControl.setMapPadding(0, 0, 0, mBottomSheetBehavior.peekHeight)

                    mMapControl.setOnLocationSelectedListener {
                        it?.let {
                            if (currentFragment != DIRECTION_FRAGMENT) {
                                if (directionsRendered) {
                                    mDirectionsRenderer?.clear()
                                }
                                if (currentLocation != it) {
                                    setDescriptionView(it)
                                }
                            }
                        }
                        return@setOnLocationSelectedListener false
                    }

                    mMapControl.setOnMapClickListener { _, _ ->
                        if (activity?.currentFocus is EditText) {
                            activity?.runOnUiThread {
                                activity?.currentFocus?.clearFocus()
                                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                                imm?.hideSoftInputFromWindow(view?.windowToken, 0)
                            }
                        }
                        return@setOnMapClickListener false
                    }

                    val building = MapsIndoors.getVenues()?.defaultVenue
                    building?.let {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBoundsConverter.toLatLngBounds(it.bounds!!), 40))
                    }

                    if (binding.loadView.visibility == View.VISIBLE) {
                        binding.loadIndicator.animate().alpha(0.0f).duration = 200
                        binding.loadView.animate()
                            .setStartDelay(300)
                            .translationY(binding.loadView.height.toFloat()-mBottomSheetBehavior.peekHeight)
                            .setDuration(1000)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    super.onAnimationEnd(animation)
                                    binding.standardBottomSheet.visibility = View.VISIBLE
                                    binding.standardBottomSheet.animate().setDuration(500).alpha(1f).setListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator?) {
                                            super.onAnimationEnd(animation)
                                            binding.loadView.visibility = View.GONE
                                        }
                                    })
                                }
                            })
                    }else {
                        binding.standardBottomSheet.alpha = 1f
                        binding.standardBottomSheet.visibility = View.VISIBLE
                        if (locationId != null) {
                            MapsIndoors.getLocationById(locationId)?.let { setDescriptionView(it) }
                        }
                    }
                }
            }
        }
    }

    fun setOnLegSelectedListener(onLegSelectedListener: OnLegSelectedListener) {
        mOnLegSelectedListener = onLegSelectedListener
    }

    fun getDirectionsRenderer(): MPDirectionsRenderer {
        if (mDirectionsRenderer == null) {
            mDirectionsRenderer = MPDirectionsRenderer(mMapControl)
        }

        return mDirectionsRenderer!!
    }

    fun getDirectionsService(): MPDirectionsService {
        if (mDirectionsService == null) {
            mDirectionsService = MPDirectionsService(requireContext())
            mDirectionsService!!.setRouteResultListener(this)
        }
        showDialog = true
        return mDirectionsService!!
    }

    fun setAccessibility(accessibility: Boolean) {
        val directionsService = getDirectionsService()
        directionsService.clearWayType()
        if (accessibility) {
            directionsService.addAvoidWayType(MPHighway.STEPS)
            directionsService.addAvoidWayType(MPHighway.LADDER)
            directionsService.addAvoidWayType(MPHighway.ESCALATOR)
        }else {
            directionsService.addAvoidWayType(MPHighway.ELEVATOR)
        }
    }

    fun setCurrentLocation(location: MPLocation?) {
        currentLocation = location
    }

    private fun setupBottomSheet() {
        val bottomSheet: FrameLayout = binding.root.findViewById(R.id.standardBottomSheet)
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    mMapControl.hideFloorSelector(false)
                    mMapControl.floorSelector?.setSelectedFloorByZIndex(mMapControl.currentFloorIndex)
                    if (currentFragment == SEARCH_FRAGMENT) {
                        val view = view?.findViewById<EditText>(R.id.searchEditText)
                        view?.clearFocus()
                        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
                    }else if (currentFragment == ROUTING_FRAGMENT) {
                        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
                    }
                }else {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED && currentFragment == SEARCH_FRAGMENT) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            val view = view?.findViewById<EditText>(R.id.searchEditText)
                            view?.requestFocus()
                            view?.let {
                                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }, 100)
                    }
                    mMapControl.hideFloorSelector(true)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        parentFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, SearchFragment.newInstance(this)).addToBackStack(null).commit()
        //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
        setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
    }

    fun setSearchView() {
        setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED)
        currentFragment = SEARCH_FRAGMENT
        parentFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, SearchFragment.newInstance(this)).commit()
    }

    fun setDescriptionView(location: MPLocation) {
        mMapControl.clearFilter()
        setCurrentLocation(location)
        mMapControl.selectLocation(location, MPSelectionBehavior.DEFAULT)
        currentFragment = DESCRIPTION_FRAGMENT
        parentFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, DescriptionFragment.newInstance(location, this)).commit()
    }

    fun setRoutingView(location: MPLocation) {
        mMapControl.clearFilter()
        currentFragment = ROUTING_FRAGMENT
        parentFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, RoutingFragment.newInstance(location, this)).commit()
    }

    fun setSearchString(text: String?) {
        lastSearchString = text
    }

    fun setLastSearchResults(locations: ArrayList<MPLocation>?) {
        val filterBehavior = MPFilterBehavior.Builder().setAllowFloorChange(false).setAnimationDuration(0).setMoveCamera(false).setShowInfoWindow(false).setZoomToFit(false).build()
        if (locations != null) {
            activity?.runOnUiThread {
                mMapControl.setFilter(locations, filterBehavior)
            }
        }else {
            mMapControl.clearFilter()
        }
        lastSearchList = locations
    }

    fun getLastSearchResults(): ArrayList<MPLocation>? = lastSearchList

    fun getSearchString(): String? {
        return lastSearchString
    }

    fun getMapControl(): MapControl {
        return mMapControl
    }

    fun getGoogleMap(): GoogleMap {
        return mMap
    }

    private fun setDirectionsView(route: MPRoute, location: MPLocation?) {
        location?.let {
            parentFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, DirectionsFragment.newInstance(route, it, this)).commit()
            directionsRendered = true
            currentFragment = DIRECTION_FRAGMENT
            activity?.runOnUiThread {
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    fun setBottomSheetState(state: Int) {
        activity?.runOnUiThread {
            mBottomSheetBehavior.state = state
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (currentFragment == DESCRIPTION_FRAGMENT && currentLocation != null) {
            outState.putString("locationID", currentLocation!!.locationId)
        }
        outState.putBoolean("restarted", true)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDirectionsRenderer?.clear()
        if (mMapControl != null) {
            mMapControl.onDestroy()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        initMapControl()

        setupCompass()
    }

    override fun onStart() {
        super.onStart()
        if (this::mMapControl.isInitialized) {
            mMapControl.onStart()
        }
    }

    override fun onStop() {
        super.onStop()
        if (this::mMapControl.isInitialized) {
            mMapControl.onStop()
        }
    }

    fun setupCompass() {
        mapFragment.view?.let { mapView ->
            mapView.findViewWithTag<View>("GoogleMapMyLocationButton").parent?.let { parent ->
                val vg: ViewGroup = parent as ViewGroup
                vg.post {
                    val mapCompass: View = parent.getChildAt(4)
                    val rlp = RelativeLayout.LayoutParams(mapCompass.height, mapCompass.height)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)

                    val topMargin = (10 * Resources.getSystem().displayMetrics.density).toInt()
                    rlp.setMargins(0, topMargin, topMargin, 0)
                    mapCompass.layoutParams = rlp
                }
            }
        }
    }

    fun selectLocation(location: MPLocation) {
        mMapControl.selectLocation(location, MPSelectionBehavior.DEFAULT)
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onRouteResult(route: MPRoute?, error: MIError?) {
        if (error != null || route == null) {
            activity?.runOnUiThread {
                context?.let {
                    if (showDialog) {
                        MaterialAlertDialogBuilder(it, R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                            .setTitle(getString(R.string.no_route))
                            .setMessage(getString(R.string.no_route_message))
                            .setPositiveButton(getString(R.string.ok)) {dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        showDialog = false
                    }
                }
            }
        }else{
            getDirectionsRenderer().setRoute(route)
            activity?.runOnUiThread {
                setDirectionsView(route, currentLocation)
            }
        }
    }
}