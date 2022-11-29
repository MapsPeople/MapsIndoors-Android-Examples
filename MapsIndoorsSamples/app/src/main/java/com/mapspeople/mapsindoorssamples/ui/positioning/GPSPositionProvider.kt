package com.mapspeople.mapsindoorssamples.ui.positioning

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.*
import com.mapsindoors.core.*

class GPSPositionProvider(context: Context): MPPositionProvider {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var mLatestPosition: MPPositionResultInterface? = null
    private val positionUpdateListeners = ArrayList<OnPositionUpdateListener>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let {
                mLatestPosition = MPPositionResult(MPPoint(it.latitude, it.longitude), it.accuracy)
                notifyPositionUpdate()
            }
        }
    }

    fun notifyPositionUpdate() {
        for (positionUpdateListener in positionUpdateListeners) {
            mLatestPosition?.let {
                positionUpdateListener.onPositionUpdate(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startPositioning() {
        val locationRequest = LocationRequest.Builder(1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    fun stopPositioning() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun addOnPositionUpdateListener(p0: OnPositionUpdateListener) {
        positionUpdateListeners.add(p0)
    }

    override fun removeOnPositionUpdateListener(p0: OnPositionUpdateListener) {
        positionUpdateListeners.remove(p0)
    }

    override fun getLatestPosition(): MPPositionResultInterface? {
        return mLatestPosition
    }
}