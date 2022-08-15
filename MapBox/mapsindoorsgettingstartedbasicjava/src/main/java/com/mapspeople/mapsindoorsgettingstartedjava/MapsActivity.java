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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.textfield.TextInputEditText;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapsindoors.coresdk.MPDirectionsRenderer;
import com.mapsindoors.coresdk.MPDirectionsService;
import com.mapsindoors.coresdk.MPFilter;
import com.mapsindoors.coresdk.MPFilterBehavior;
import com.mapsindoors.coresdk.MPLocation;
import com.mapsindoors.coresdk.MPPoint;
import com.mapsindoors.coresdk.MPQuery;
import com.mapsindoors.coresdk.MPRoute;
import com.mapsindoors.coresdk.MPTravelMode;
import com.mapsindoors.coresdk.MPVenue;
import com.mapsindoors.coresdk.MapControl;
import com.mapsindoors.coresdk.MapsIndoors;
import com.mapsindoors.coresdk.OnRouteResultListener;
import com.mapsindoors.coresdk.errors.MIError;
import com.mapsindoors.livesdk.LiveDataDomainTypes;
import com.mapsindoors.mapboxsdk.MPMapConfig;
import com.mapsindoors.mapboxsdk.converters.CoordinateBoundsConverter;


public class MapsActivity extends AppCompatActivity {

    private MapControl mMapControl;
    private MapView mMapView;
    private MapboxMap mMapboxMap;
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
        mMapView = findViewById(R.id.mapView);
        mMapboxMap = mMapView.getMapboxMap();

        //TODO: Instantiate MapsIndoors

        ImageButton searchBtn = findViewById(R.id.search_btn);
        mSearchTxtField = findViewById(R.id.search_edit_txt);
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //ClickListener to start a search, when the user clicks the search button
        searchBtn.setOnClickListener(view -> {
            if (mSearchTxtField.getText().length() != 0) {
                //There is text inside the search field. So lets do the search.
                //TODO: Call the search method when you have created it following the tutorial
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        //Listener for when the user searches through the keyboard
        mSearchTxtField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.getText().length() != 0) {
                    //There is text inside the search field. So lets do the search.
                    //TODO: Call the search method when you have created it following the tutorial
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

        //TODO: Init MapControl here
    }

    //TODO: Implement methods when described in the tutorial.

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