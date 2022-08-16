package com.mapspeople.mapsindoorssamples.ui.positioning

import com.mapsindoors.coresdk.*
import com.mapsindoors.coresdk.models.MPLatLng
import com.mapsindoors.coresdk.models.MPLatLngBounds
import kotlin.random.Random

class PositionProvider : MPPositionProvider {

    private val mUpdateListeners = ArrayList<OnPositionUpdateListener>()
    private var mLatestPosition: MPPositionResultInterface? = null

    private var mRunning = false

    private val UPDATE_RATE_MS = 1000

    private val mPositionProducingThread: Thread = Thread {
        while(true){
            if(mRunning){
                // Produce a random positioning inside The White House bounds
                val bounds = MPLatLngBounds(MPLatLng(38.897545509875954, -77.03687635385639), MPLatLng(38.89779861672662, -77.03623597646553))

                val randomLat = Random.nextDouble(bounds.southWest.lat, bounds.northEast.lat)
                val randomLng = Random.nextDouble(bounds.southWest.lng, bounds.northEast.lng)

                val floorIndex = 10.0
                val accuracy = (3..10).random().toFloat() // In meters
                val bearing = (0..360).random().toFloat() // In degrees

                val position = MPPoint(randomLat, randomLng, floorIndex)

                // Randomize whether or not the position result has a bearing
                if(Random.nextInt() % 2 == 0)
                    mLatestPosition = MPPositionResult(position, accuracy, bearing)
                else
                    mLatestPosition = MPPositionResult(position, accuracy)

                for(listener in mUpdateListeners){
                    listener.onPositionUpdate(mLatestPosition as MPPositionResult)
                }
            }
            Thread.sleep(UPDATE_RATE_MS.toLong())
        }
    }

    init {
        mPositionProducingThread.start()
    }

    override fun addOnPositionUpdateListener(updateListener: OnPositionUpdateListener) {
        mUpdateListeners.add(updateListener)
    }

    override fun removeOnPositionUpdateListener(updateListener: OnPositionUpdateListener) {
        mUpdateListeners.remove(updateListener)
    }

    override fun getLatestPosition(): MPPositionResultInterface? {
        return null
    }

    fun start(){
        mRunning = true
    }

    fun stop(){
        mRunning = false
    }

}