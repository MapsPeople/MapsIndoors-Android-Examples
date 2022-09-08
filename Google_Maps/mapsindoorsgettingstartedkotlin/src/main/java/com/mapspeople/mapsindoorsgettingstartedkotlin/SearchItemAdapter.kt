package com.mapspeople.mapsindoorsgettingstartedkotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapsindoors.core.MPLocation
import com.mapsindoors.core.MapsIndoors


internal class SearchItemAdapter(private val mLocations: List<MPLocation?>, private val mMapActivity: MapsActivity?) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = mLocations[position]?.name
        holder.itemView.setOnClickListener {
            mLocations[position]?.let { locations -> mMapActivity?.createRoute(locations) }
            //Clearing map to remove the location filter from our search result
            mMapActivity?.getMapControl()?.clearFilter()
        }
        if (mMapActivity != null) {
            mLocations[position]?.let { MapsIndoors.getDisplayRule(it) }?.getIconAsync { bitmap, error ->
                mMapActivity.runOnUiThread {
                    holder.imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mLocations.size
    }

}

internal class ViewHolder(inflater: LayoutInflater, parent: ViewGroup?) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_search_list_item, parent, false)) {
    val text: TextView
    val imageView: ImageView

    init {
        text = itemView.findViewById(R.id.text)
        imageView = itemView.findViewById(R.id.location_image)
    }
}