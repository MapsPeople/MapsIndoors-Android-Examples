package com.mapspeople.mapsindoorssamples.ui.positioning

import android.content.Context
import android.os.Bundle
import com.indooratlas.android.sdk.*
import com.mapsindoors.core.*

class IndoorAtlasPositionProvider(private val context: Context, private val config: MPIndoorAtlasConfig): MPPositionProvider {

    private var mLatestPosition: MPPositionResultInterface? = null
    private var mLatestBearing: Float? = null
    private var mLastHeadingUpdateTime: Long = 0

    private val MIN_TIME_BETWEEN_UPDATES_IN_MS: Long = 100

    private val positionUpdateListeners = ArrayList<OnPositionUpdateListener>()

    private var indoorAtlasClient: IALocationManager? = null

    private val orientationListener: IAOrientationListener = object : IAOrientationListener {
        override fun onHeadingChanged(timestamp: Long, heading: Double) {
            mLatestPosition?.let {
                val dt: Long = timestamp - mLastHeadingUpdateTime

                if (dt < MIN_TIME_BETWEEN_UPDATES_IN_MS) {
                    return
                }

                mLastHeadingUpdateTime = timestamp

                it.bearing = heading.toFloat()
                mLatestBearing = it.bearing

                notifyPositionUpdate()
            }
        }

        override fun onOrientationChange(p0: Long, p1: DoubleArray?) {
            //Empty as we only use heading here
        }

    }

    private val locationListener = object : IALocationListener {
        override fun onLocationChanged(location: IALocation?) {
            location?.let {
                val lat = it.latitude
                val lng = it.longitude
                val floorLevel = it.floorLevel
                val accuracy = it.accuracy

                val hasFloorLevel = it.hasFloorLevel()

                val positionResult = MPPositionResult(MPPoint(lat, lng), accuracy)
                positionResult.androidLocation = it.toLocation()
                if (mLatestBearing != null) {
                    positionResult.bearing = mLatestBearing!!
                }

                if (hasFloorLevel) {
                    if (config.getMappedFloorIndex(floorLevel) != null) {
                        positionResult.floorIndex = config.getMappedFloorIndex(floorLevel)!!
                    }else {
                        positionResult.floorIndex = MPFloor.DEFAULT_GROUND_FLOOR_INDEX
                    }
                }else {
                    positionResult.floorIndex = MPFloor.DEFAULT_GROUND_FLOOR_INDEX
                }

                positionResult.provider = this@IndoorAtlasPositionProvider

                mLatestPosition = positionResult
                notifyPositionUpdate()
            }
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            //Blank for this, can be used to report if indoor atlas is unavailable etc.
        }

    }

    fun notifyPositionUpdate() {
        for (positionUpdateListener in positionUpdateListeners) {
            mLatestPosition?.let {
                positionUpdateListener.onPositionUpdate(it)
            }
        }
    }

    fun initClient() {
        val extras = Bundle(2)
        extras.putString(IALocationManager.EXTRA_API_KEY, config.key)
        extras.putString(IALocationManager.EXTRA_API_SECRET, config.secret)

        indoorAtlasClient = IALocationManager.create(context, extras)
    }

    fun startPositioning() {
        if (indoorAtlasClient == null) {
            initClient()
        }
        indoorAtlasClient?.registerOrientationListener(IAOrientationRequest(1.0, 0.0), orientationListener)
        indoorAtlasClient?.lockIndoors(false)
        indoorAtlasClient?.requestLocationUpdates(IALocationRequest.create(), locationListener)
    }

    fun stopPositioning() {
        indoorAtlasClient?.let {
            it.unregisterOrientationListener(orientationListener)
            it.removeLocationUpdates(locationListener)
            it.lockIndoors(true)
        }
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