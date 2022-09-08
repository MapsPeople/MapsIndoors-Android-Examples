package com.mapspeople.mapsindoorsgettingstartedjava;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.textfield.TextInputEditText;
import com.mapsindoors.core.MPDirectionsRenderer;
import com.mapsindoors.core.MPDirectionsService;
import com.mapsindoors.core.MPFilter;
import com.mapsindoors.core.MPFilterBehavior;
import com.mapsindoors.core.MPLocation;
import com.mapsindoors.core.MPPoint;
import com.mapsindoors.core.MPQuery;
import com.mapsindoors.core.MPRoute;
import com.mapsindoors.core.MPTravelMode;
import com.mapsindoors.core.MPVenue;
import com.mapsindoors.core.MapControl;
import com.mapsindoors.core.MapsIndoors;
import com.mapsindoors.core.OnRouteResultListener;
import com.mapsindoors.core.errors.MIError;
import com.mapsindoors.googlemaps.MPMapConfig;
import com.mapsindoors.googlemaps.converters.LatLngBoundsConverter;
import com.mapsindoors.livedata.LiveDataDomainTypes;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, OnRouteResultListener {

    private GoogleMap mMap;
    private MapControl mMapControl;
    private View mMapView;
    private TextInputEditText mSearchTxtField;
    private MPDirectionsService mpDirectionsService;
    private MPDirectionsRenderer mpDirectionsRenderer;
    private MPPoint mUserLocation = new MPPoint(38.897389429704695, -77.03740973527613,0);
    private NavigationFragment mNavigationFragment;
    private SearchFragment mSearchFragment;
    private Fragment mCurrentFragment;
    private BottomSheetBehavior<FrameLayout> mBtmnSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //The local variable for the MapFragments view.
        mMapView = mapFragment.getView();

        //Initialize MapsIndoors and set the google api Key
        MapsIndoors.load(getApplicationContext(), "d876ff0e60bb430b8fabb145", null);

        ImageButton searchBtn = findViewById(R.id.search_btn);
        mSearchTxtField = findViewById(R.id.search_edit_txt);
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //ClickListener to start a search, when the user clicks the search button
        searchBtn.setOnClickListener(view -> {
            if (mSearchTxtField.getText().length() != 0) {
                //There is text inside the search field. So lets do the search.
                search(mSearchTxtField.getText().toString());
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        //Listener for when the user searches through the keyboard
        mSearchTxtField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.getText().length() != 0) {
                    //There is text inside the search field. So lets do the search.
                    search(textView.getText().toString());
                }
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        FrameLayout bottomSheet = findViewById(R.id.standardBottomSheet);
        mBtmnSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBtmnSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (mCurrentFragment != null) {
                        if (mCurrentFragment instanceof NavigationFragment) {
                            //Clears the direction view if the navigation fragment is closed.
                            mpDirectionsRenderer.clear();
                        }
                        //Clears the map if any searches has been done.
                        mMapControl.clearFilter();
                        //Removes the current fragment from the BottomSheet.
                        removeFragmentFromBottomSheet(mCurrentFragment);
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    /**
     * Public getter for the MapControl object
     * @return MapControl object for this activity
     */
    public MapControl getMapControl() {
        return mMapControl;
    }

    /**
     * Public getter for the
     * @return MPDirectionRenderer object for this activity
     */
    public MPDirectionsRenderer getMpDirectionsRenderer() {
        return mpDirectionsRenderer;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMapView != null) {
            initMapControl(mMapView);
        }
    }

    /**
     * Inits mapControl and sets the camera to the venue.
     * @param view the view assigned to the google map.
     */
    void initMapControl(View view) {
        MPMapConfig mapConfig = new MPMapConfig.Builder(this, mMap, getString(R.string.google_maps_key), view, true).build();
        //Creates a new instance of MapControl
        MapControl.create(mapConfig, (mapControl, miError) -> {
            mMapControl = mapControl;
            //Enable Live Data on the map
            enableLiveData();
            if (miError == null) {
                //No errors so getting the first venue (in the white house solution the only one)
                MPVenue venue = MapsIndoors.getVenues().getDefaultVenue();
                runOnUiThread( ()-> {
                    if (venue != null) {
                        //Animates the camera to fit the new venue
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBoundsConverter.toLatLngBounds(venue.getBounds()), 19));
                    }
                });
            }
        });
    }

    /**
     * Performs a search for locations with mapsindoors and opens up a list of search results
     * @param searchQuery String to search for
     */
    void search(String searchQuery) {
        //Query with a string to search on
        MPQuery mpQuery = new MPQuery.Builder().setQuery(searchQuery).build();
        //Filter for the search query, only taking 30 locations
        MPFilter mpFilter = new MPFilter.Builder().setTake(30).build();

        //Gets locations
        MapsIndoors.getLocationsAsync(mpQuery, mpFilter, (list, miError) -> {
            //Check if there is no error and the list is not empty
            if (miError == null && !list.isEmpty()) {
                //Create a new instance of the search fragment
                mSearchFragment = SearchFragment.newInstance(list, this);
                //Make a transaction to the bottomsheet
                addFragmentToBottomSheet(mSearchFragment);
                //Clear the search text, since we got a result
                mSearchTxtField.getText().clear();
                //Calling displaySearch results on the ui thread as camera movement is involved
                runOnUiThread(()-> {
                    mMapControl.setFilter(list, MPFilterBehavior.DEFAULT);
                });
            }else {
                String alertDialogTitleTxt;
                String alertDialogTxt;
                if (list.isEmpty()) {
                    alertDialogTitleTxt = "No results found";
                    alertDialogTxt = "No results could be found for your search text. Try something else";
                }else {
                    if (miError != null) {
                        alertDialogTitleTxt = "Error: " + miError.code;
                        alertDialogTxt = miError.message;
                    }else {
                        alertDialogTitleTxt = "Unknown error";
                        alertDialogTxt = "Something went wrong, try another search text";
                    }
                }

                new AlertDialog.Builder(this)
                        .setTitle(alertDialogTitleTxt)
                        .setMessage(alertDialogTxt)
                        .show();
            }
        });
    }

    /**
     * Queries the MPRouting provider with a hardcoded user location and the location the user should be routed to
     * @param mpLocation A MPLocation to navigate to
     */
    void createRoute(MPLocation mpLocation) {
        //If MPRoutingProvider has not been instantiated create it here and assign the results call back to the activity.
        if (mpDirectionsService == null) {
            mpDirectionsService = new MPDirectionsService(this);
            mpDirectionsService.setRouteResultListener(this);
        }
        mpDirectionsService.setTravelMode(MPTravelMode.WALKING);
        //Queries the MPRouting provider for a route with the hardcoded user location and the point from a location.
        mpDirectionsService.query(mUserLocation, mpLocation.getPoint());
    }

    @Override
    public void onRouteResult(@Nullable MPRoute mpRoute, @Nullable MIError miError) {
        //Return if either error is not null or the route is null
        if (miError != null || mpRoute == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Something went wrong")
                    .setMessage("Something went wrong when generating the route. Try again or change your destination/origin")
                    .show();
            return;
        }
        //Create the MPDirectionsRenderer if it has not been instantiated.
        if (mpDirectionsRenderer == null) {
            mpDirectionsRenderer = new MPDirectionsRenderer(mMapControl);
        }
        //Set the route on the Directions renderer
        mpDirectionsRenderer.setRoute(mpRoute);
        //Create a new instance of the navigation fragment
        mNavigationFragment = NavigationFragment.newInstance(mpRoute, this);
        //Add the fragment to the BottomSheet
        addFragmentToBottomSheet(mNavigationFragment);
    }

    /**
     * Enables live data for the map.
     */
    void enableLiveData() {
        //Enabling Live Data for the three known Live Data Domains enabled for this Solution.
        mMapControl.enableLiveData(LiveDataDomainTypes.AVAILABILITY_DOMAIN);
        mMapControl.enableLiveData(LiveDataDomainTypes.OCCUPANCY_DOMAIN);
        mMapControl.enableLiveData(LiveDataDomainTypes.POSITION_DOMAIN);
    }

    void addFragmentToBottomSheet(Fragment newFragment) {
        if (mCurrentFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(mCurrentFragment).commit();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.standardBottomSheet, newFragment).commit();
        mCurrentFragment = newFragment;
        //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
        runOnUiThread(()-> {
            mMapControl.setMapPadding(0, 0,0, mBtmnSheetBehavior.getPeekHeight());
            if (mBtmnSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                mBtmnSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    void removeFragmentFromBottomSheet(Fragment fragment) {
        if (mCurrentFragment.equals(fragment)) {
            mCurrentFragment = null;
        }
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        runOnUiThread(()-> {
            mMapControl.setMapPadding(0,0,0,0);
        });
    }
}