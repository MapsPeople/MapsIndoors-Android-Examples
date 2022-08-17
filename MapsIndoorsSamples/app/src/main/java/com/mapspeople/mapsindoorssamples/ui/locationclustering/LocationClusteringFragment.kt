package com.mapspeople.mapsindoorssamples.ui.locationclustering

import android.graphics.*
import android.os.Bundle
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.mapsindoors.coresdk.*
import com.mapsindoors.coresdk.errors.MIError
import com.mapsindoors.coresdk.models.MPLatLng
import com.mapsindoors.googlemapssdk.MPMapConfig
import com.mapsindoors.googlemapssdk.converters.LatLngBoundsConverter
import com.mapspeople.mapsindoorssamples.R
import com.mapspeople.mapsindoorssamples.databinding.FragmentLocationClusteringBinding


class LocationClusteringFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationClusteringBinding? = null

    private val binding get() = _binding!!
    private var mMap: GoogleMap? = null
    private var mMapView: View? = null
    private var mMapControl: MapControl? = null

    private var clusteringEnabled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationClusteringBinding.inflate(inflater, container, false)
        MapsIndoors.load(requireActivity().applicationContext, "mapspeople") {
            if (it == null) {
                //clusteringEnabled = MapsIndoors.getSolution()?.config?.enableClustering!!
            }
        }

        var location = MapsIndoors.getLocationById("blabla")!!
        val geometry: MPGeometry = location.geometry
        when (geometry.iType) {
            MPGeometry.TYPE_POINT -> {
                val point = geometry
            }
            MPGeometry.TYPE_POLYGON -> {
                val polygon: MPPolygonGeometry = geometry as MPPolygonGeometry

                // Using GMS helper classes
                // Get all the paths in the polygon
                val paths: List<List<MPLatLng>> = polygon.gmsPath
                val pathCount = paths.size

                // Outer ring (first)
                val path = paths[0]
                for (coordinate in path) {
                    val lat = coordinate.lat
                    val lng = coordinate.lng
                }

                // Optional: Inner rings (holes)
                var i = 1
                while (i < pathCount) {
                    val hole = paths[i]
                    for (coordinate in hole) {
                        val lat = coordinate.lat
                        val lng = coordinate.lng
                    }
                    i++
                }
            }
        }

        val root: View = binding.root
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mMapView = supportMapFragment.view
        supportMapFragment.getMapAsync(this)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
        if (mMapView != null) {
            initMapControl(mMapView!!)
        }
    }

    fun getCircularImageWithText(text: String, textSize: Int, width: Int, height: Int): Bitmap {
        val background = Paint()
        background.color = Color.WHITE
        // Now add the icon on the left side of the background rect
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val radius = width shr 1
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), background)
        background.color = Color.BLACK
        background.style = Paint.Style.STROKE
        background.strokeWidth = 3f
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), (radius - 2).toFloat(), background)
        val tp = TextPaint()
        tp.textSize = textSize.toFloat()
        tp.color = Color.BLACK
        val bounds = Rect()
        tp.getTextBounds(text, 0, text.length, bounds)
        val textHeight: Int = bounds.height()
        val textWidth: Int = bounds.width()
        val textPosX = width - textWidth shr 1
        val textPosY = height + textHeight shr 1
        canvas.drawText(text, textPosX.toFloat(), textPosY.toFloat(), tp)
        return result
    }

    private fun initMapControl(view: View) {
        val mapConfig: MPMapConfig = MPMapConfig.Builder(requireActivity(), mMap!!, getString(R.string.google_maps_key), view, true).setClusterIconAdapter { return@setClusterIconAdapter getCircularImageWithText(it.size.toString(), 15, 30, 30) }.build()
        //Creates a new instance of MapControl
        MapControl.create(mapConfig) { mapControl: MapControl?, miError: MIError? ->
            mMapControl = mapControl
            //Enable Live Data on the map
            if (miError == null) {
                //No errors so getting the first venue (in the white house solution the only one)
                val venue = MapsIndoors.getVenues()?.defaultVenue
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

                mMapControl?.setOnLocationClusterClickListener { mpLatLng, mutableList ->
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mpLatLng.lat, mpLatLng.lng), 22f))
                    return@setOnLocationClusterClickListener true
                }

                binding.clusteringBtn.setOnClickListener {
                    MapsIndoors.getSolution()?.config?.setEnableClustering(!clusteringEnabled)
                    clusteringEnabled = !clusteringEnabled
                }

            }
        }
    }
}