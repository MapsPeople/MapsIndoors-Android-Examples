package com.mapspeople.mapsindoorsgettingstartedkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapsindoors.core.MPLocation


class SearchFragment : Fragment() {
    private var mLocations: List<MPLocation?>? = null
    private var mMapActivity: MapsActivity? = null

    @Nullable
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mLocations?.let { locations -> SearchItemAdapter(locations, mMapActivity) }
    }

    companion object {
        fun newInstance(locations: List<MPLocation?>?, mapsActivity: MapsActivity?): SearchFragment {
            val fragment = SearchFragment()
            fragment.mLocations = locations
            fragment.mMapActivity = mapsActivity
            return fragment
        }
    }
}
