package com.mapspeople.mapsindoorsgettingstartedkotlin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.textfield.TextInputEditText
import com.mapsindoors.core.*
import com.mapsindoors.core.errors.MIError
import com.mapsindoors.googlemaps.MPMapConfig
import com.mapsindoors.googlemaps.converters.LatLngBoundsConverter


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRouteResultListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mapView: View
    private lateinit var mMapControl: MapControl
    private lateinit var mSearchFragment: SearchFragment
    private lateinit var mNavigationFragment: NavigationFragment
    private lateinit var mBtmnSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mSearchTxtField: TextInputEditText
    private var mCurrentFragment: Fragment? = null
    private val mUserLocation: MPPoint = MPPoint(38.897389429704695, -77.03740973527613, 0.0)

    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mpDirectionsService: MPDirectionsService? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        MapsIndoors.load(applicationContext, "02c329e6777d431a88480a09", null)

        mapFragment.view?.let {
            mapView = it
        }

        mSearchTxtField = findViewById(R.id.search_edit_txt)
        //Listener for when the user searches through the keyboard
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        mSearchTxtField.setOnEditorActionListener { textView, i, _ ->
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.isNotEmpty()) {
                    search(textView.text.toString())
                }
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        //ClickListener to start a search, when the user clicks the search button
        var searchBtn = findViewById<ImageButton>(R.id.search_btn)
        searchBtn.setOnClickListener {
            if (mSearchTxtField.text?.length != 0) {
                //There is text inside the search field. So lets do the search.
                search(mSearchTxtField.text.toString())
            }
            //Making sure keyboard is closed.
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        var bottomSheet = findViewById<FrameLayout>(R.id.standardBottomSheet)
        mBtmnSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBtmnSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (mCurrentFragment != null) {
                        if (mCurrentFragment is NavigationFragment) {
                            //Clears the direction view if the navigation fragment is closed.
                            mpDirectionsRenderer?.clear()
                        }
                        //Clears the map if any searches has been done.
                        mMapControl.clearFilter()
                        //Removes the current fragment from the BottomSheet.
                        removeFragmentFromBottomSheet(mCurrentFragment!!)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mapView?.let { view ->
            initMapControl(view)
        }
    }

    private fun initMapControl(view: View) {
        //Creates a new instance of MapControl
        val config = MPMapConfig.Builder(this, mMap, getString(R.string.google_maps_key), view, true).build()
        MapControl.create(config) { mapControl, miError ->
            if (miError == null) {
                mMapControl = mapControl!!
                //No errors so getting the first venue (in the white house solution the only one)
                val venue = MapsIndoors.getVenues()?.defaultVenue
                venue?.bounds?.let {
                    runOnUiThread {
                        //Animates the camera to fit the new venue
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBoundsConverter.toLatLngBounds(it), 19));
                    }
                }
            }
        }
    }

    private fun search(searchQuery: String) {
        //Query with a string to search on
        val mpQuery = MPQuery.Builder().setQuery(searchQuery).build()
        //Filter for the search query, only taking 30 locations
        val mpFilter = MPFilter.Builder().setTake(30).build()

        //Gets locations
        MapsIndoors.getLocationsAsync(mpQuery, mpFilter) { list: List<MPLocation?>?, miError: MIError? ->
            //Check if there is no error and the list is not empty
            if (miError == null && !list.isNullOrEmpty()) {
                //Create a new instance of the search fragment
                mSearchFragment = SearchFragment.newInstance(list, this)
                //Make a transaction to the bottom sheet
                addFragmentToBottomSheet(mSearchFragment)
                //Clear the search text, since we got a result
                mSearchTxtField.text?.clear()
                //Calling displaySearch results on the ui thread as camera movement is involved
                runOnUiThread { mMapControl.setFilter(list, MPFilterBehavior.DEFAULT) }
            } else {
                val alertDialogTitleTxt: String
                val alertDialogTxt: String
                if (list!!.isEmpty()) {
                    alertDialogTitleTxt = "No results found"
                    alertDialogTxt = "No results could be found for your search text. Try something else"
                } else {
                    if (miError != null) {
                        alertDialogTitleTxt = "Error: " + miError.code
                        alertDialogTxt = miError.message
                    } else {
                        alertDialogTitleTxt = "Unknown error"
                        alertDialogTxt = "Something went wrong, try another search text"
                    }
                }
                AlertDialog.Builder(this)
                        .setTitle(alertDialogTitleTxt)
                        .setMessage(alertDialogTxt)
                        .show()
            }
        }
    }

    fun getMapControl(): MapControl {
        return mMapControl
    }

    fun getMpDirectionsRenderer(): MPDirectionsRenderer? {
        return mpDirectionsRenderer
    }

    fun createRoute(mpLocation: MPLocation) {
        //If MPRoutingProvider has not been instantiated create it here and assign the results call back to the activity.
        if (mpDirectionsService == null) {
            mpDirectionsService = MPDirectionsService()
            mpDirectionsService?.setRouteResultListener(this)
            mpDirectionsService?.setTravelMode(MPTravelMode.WALKING)
        }

        //Use the locations venue to query an origin point for the route. Within the venue bounds.
        if (mpLocation.venue == null) {
            //Open dialog telling user to try another location, as no venue is assigned to the location.
            AlertDialog.Builder(this)
                .setTitle("No venue assigned")
                .setMessage("Please try another location")
                .show()
        } else {
            val venue = MapsIndoors.getVenues()?.getVenueByName(mpLocation.venue!!)
            MapsIndoors.getLocationsAsync(null, MPFilter.Builder().setMapExtend(MPMapExtend(venue!!.bounds!!)).build()) { list: List<MPLocation?>?, miError: MIError? ->
                if (!list.isNullOrEmpty()) {
                    list.first()?.let { location ->
                        //Queries the MPRouting provider for a route with the hardcoded user location and the point from a location.
                        mpDirectionsService?.query(location.point, mpLocation.point)
                    }
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("No locations found within venue of location")
                        .setMessage("Please try another location")
                        .show()
                }
            }
        }
    }

    /**
     * The result callback from the route query. Starts the rendering of the route and opens up a new instance of the navigation fragment on the bottom sheet.
     * @param route the route model used to render a navigation view.
     * @param miError an MIError if anything goes wrong when generating a route
     */
    override fun onRouteResult(route: MPRoute?, miError: MIError?) {
        //Return if either error is not null or the route is null
        if (miError != null || route == null) {
            //TODO: Tell the user about the route not being able to be created etc.
            runOnUiThread { mBtmnSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
            return
        }
        //Create the MPDirectionsRenderer if it has not been instantiated.
        if (mpDirectionsRenderer == null) {
            mpDirectionsRenderer = MPDirectionsRenderer(mMapControl)
        }
        //Set the route on the Directions renderer
        mpDirectionsRenderer?.setRoute(route)
        //Create a new instance of the navigation fragment
        mNavigationFragment = NavigationFragment.newInstance(route, this)
        //Start a transaction and assign it to the BottomSheet
        addFragmentToBottomSheet(mNavigationFragment)
    }

    fun addFragmentToBottomSheet(newFragment: Fragment) {
        if (mCurrentFragment != null) {
            supportFragmentManager.beginTransaction().remove(mCurrentFragment!!).commit()
        }
        supportFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, newFragment).commit()
        mCurrentFragment = newFragment
        //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
        runOnUiThread {
            mMapControl.setMapPadding(0, 0, 0, mBtmnSheetBehavior.peekHeight)
            if (mBtmnSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                mBtmnSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    fun removeFragmentFromBottomSheet(fragment: Fragment) {
        if (mCurrentFragment == fragment) {
            mCurrentFragment = null
        }
        supportFragmentManager.beginTransaction().remove(fragment).commit()
        runOnUiThread { mMapControl.setMapPadding(0, 0, 0, 0) }
    }
}