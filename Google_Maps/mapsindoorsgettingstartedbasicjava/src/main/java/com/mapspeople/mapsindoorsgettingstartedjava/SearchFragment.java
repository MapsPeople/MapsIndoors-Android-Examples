package com.mapspeople.mapsindoorsgettingstartedjava;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mapsindoors.core.MPLocation;

import java.util.List;

public class SearchFragment extends Fragment {

    private List<MPLocation> mLocations = null;
    private MapsActivity mMapActivity = null;

    public static SearchFragment newInstance(List<MPLocation> locations, MapsActivity mapsActivity) {
        final SearchFragment fragment = new SearchFragment();
        fragment.mLocations = locations;
        fragment.mMapActivity = mapsActivity;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new SearchItemAdapter(mLocations, mMapActivity));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}