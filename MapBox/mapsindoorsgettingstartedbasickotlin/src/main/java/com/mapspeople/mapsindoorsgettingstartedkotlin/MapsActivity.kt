package com.mapspeople.mapsindoorsgettingstartedkotlin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.textfield.TextInputEditText
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapsindoors.core.MPDirectionsRenderer
import com.mapsindoors.core.MPDirectionsService
import com.mapsindoors.core.MPPoint
import com.mapsindoors.core.MapControl

class MapsActivity : AppCompatActivity() {

    private lateinit var mMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var mMapControl: MapControl
    private lateinit var mSearchFragment: SearchFragment
    private lateinit var mNavigationFragment: NavigationFragment
    private lateinit var mBtmnSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mSearchTxtField: TextInputEditText
    private var mCurrentFragment: Fragment? = null
    private val mUserLocation: MPPoint = MPPoint(38.897389429704695, -77.03740973527613, 0.0)

    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mpRoutingProvider: MPDirectionsService? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapView = findViewById(R.id.mapView)
        mMap = mapView.mapboxMap
        //TODO: Instantiate MapsIndoors

        mSearchTxtField = findViewById(R.id.search_edit_txt)
        //Listener for when the user searches through the keyboard
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        mSearchTxtField.setOnEditorActionListener { textView, i, _ ->
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.isNotEmpty()) {
                    //TODO: Call the search method when you have created it following the tutorial
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
                //TODO: Call the search method when you have created it following the tutorial
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
        //TODO: Init MapControl here
    }

    //TODO: Implement methods when described in the tutorial.

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