package com.mapspeople.mapsindoorssamples.ui.locationsources

import android.util.Log
import com.mapsindoors.core.MPLocation
import com.mapsindoors.core.MPLocationSource
import com.mapsindoors.core.MPLocationSourceStatus
import com.mapsindoors.core.MPLocationsObserver

class RobotVacuumLocationSource(private val robots: ArrayList<MPLocation>): MPLocationSource {
    private val mObservers = ArrayList<MPLocationsObserver>()
    private var mStatus = MPLocationSourceStatus.NOT_INITIALIZED

    fun setup() {
        status = MPLocationSourceStatus.AVAILABLE
        notifyUpdateLocations()
    }

    fun setStatus(status: MPLocationSourceStatus) {
        mStatus = status
        for (observer in mObservers) {
            observer.onStatusChanged(mStatus, this)
        }
    }

    fun updateLocations(locations: MutableList<MPLocation>) {
        robots.clear()
        robots.addAll(locations)
        notifyUpdateLocations()
    }

    override fun getLocations(): MutableList<MPLocation> {
        return robots
    }

    override fun addLocationsObserver(observer: MPLocationsObserver?) {
        if (observer != null) {
            mObservers.add(observer)
        }
    }

    override fun removeLocationsObserver(observer: MPLocationsObserver?) {
        if (observer != null) {
            mObservers.remove(observer)
        }
    }

    private fun notifyUpdateLocations() {
        for (observer in mObservers) {
            observer.onLocationsUpdated(robots, this)
        }
    }

    override fun getStatus(): MPLocationSourceStatus {
        return mStatus
    }

    override fun getSourceId(): Int {
        return 10101010
    }

    override fun clearCache() {
        robots.clear()
        mObservers.clear()
    }

    override fun terminate() {
        robots.clear()
        mObservers.clear()
    }
}