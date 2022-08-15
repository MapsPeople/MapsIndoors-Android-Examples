package com.mapspeople.mapsindoorstemplatemapbox

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.mapsindoors.coresdk.MPLocation
import com.mapsindoors.coresdk.MapsIndoors
import com.mapsindoors.coresdk.OnLocationSelectedListener
import com.mapspeople.mapsindoorstemplatemapbox.databinding.FragmentSearchItemBinding

class MPSearchItemRecyclerViewAdapter : RecyclerView.Adapter<MPSearchItemRecyclerViewAdapter.ViewHolder>() {
    private var mLocations: List<MPLocation> = ArrayList()
    private lateinit var context: Context
    private var mOnLocationSelectedListener: OnLocationSelectedListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(FragmentSearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mLocations[position]
        var iconUrl = getTypeIcon(item)
        iconUrl?.let {
            Glide.with(context).load(iconUrl).into(holder.icon)
        }

        holder.nameView.text = item.name

        if (item.floorName != null && item.buildingName != null) {
            val buildingName = MapsIndoors.getBuildings()?.getBuilding(item.point.latLng)?.name
            if (buildingName != null) {
                holder.subTextView.text = "Floor: " + item.floorName + " - " + buildingName
            }else {
                holder.subTextView.text = "Floor: " + item.floorName + " - " + item.buildingName
            }
        }else {
            holder.subTextView.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (mOnLocationSelectedListener != null) {
                mOnLocationSelectedListener?.onLocationSelected(item)
            }
        }
    }

    private fun getTypeIcon(mpLocation: MPLocation): String? {
        MapsIndoors.getSolution()?.let {
            it.types?.forEach { type ->
                if (mpLocation.type?.equals(type.name, true) == true) {
                    return type.icon
                }
            }
        }
        return null
    }

    fun setOnLocationSelectedListener(onLocationSelectedListener: OnLocationSelectedListener) {
        mOnLocationSelectedListener = onLocationSelectedListener
    }

    override fun getItemCount(): Int = mLocations.size

    fun setLocations(locations: List<MPLocation>) {
        mLocations = locations;
    }

    fun clear() {
        mLocations = ArrayList()
        notifyDataSetChanged()
    }

    inner class ViewHolder(binding: FragmentSearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val icon: ImageView = binding.locationIconView
        val nameView: TextView = binding.locationName
        val subTextView: TextView = binding.locationSubtext

        override fun toString(): String {
            return super.toString() + " '" + subTextView.text + "'"
        }
    }

}