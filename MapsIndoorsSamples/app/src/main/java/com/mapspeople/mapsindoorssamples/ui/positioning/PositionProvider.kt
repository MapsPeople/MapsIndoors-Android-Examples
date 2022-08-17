package com.mapspeople.mapsindoorssamples.ui.positioning

import com.mapsindoors.coresdk.*
import com.mapsindoors.coresdk.models.MPLatLng
import com.mapsindoors.coresdk.models.MPLatLngBounds
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class PositionProvider : MPPositionProvider {

    private val mUpdateListeners = ArrayList<OnPositionUpdateListener>()
    private var mLatestPosition: MPPositionResultInterface? = null

    private var mPositionProducer : Timer? = null

    // White House bounds
    private val mWhiteHouseBounds = MPLatLngBounds(MPLatLng(38.897545509875954, -77.03687635385639), MPLatLng(38.89779861672662, -77.03623597646553))

    override fun addOnPositionUpdateListener(updateListener: OnPositionUpdateListener) {
        mUpdateListeners.add(updateListener)
    }

    override fun removeOnPositionUpdateListener(updateListener: OnPositionUpdateListener) {
        mUpdateListeners.remove(updateListener)
    }

    override fun getLatestPosition(): MPPositionResultInterface? {
        return mLatestPosition
    }

    fun start(){
        mPositionProducer = Timer(true)
        mPositionProducer?.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                // Produce a random positioning inside The White House bounds
                val randomLat = Random.nextDouble(mWhiteHouseBounds.southWest.lat, mWhiteHouseBounds.northEast.lat)
                val randomLng = Random.nextDouble(mWhiteHouseBounds.southWest.lng, mWhiteHouseBounds.northEast.lng)

                val floorIndex = 10.0
                val accuracy = (3..10).random().toFloat() // In meters
                val bearing = (0..360).random().toFloat() // In degrees

                val position = MPPoint(randomLat, randomLng, floorIndex)

                // Randomize whether or not the position result has a bearing
                if(Random.nextInt() % 2 == 0)
                    mLatestPosition = MPPositionResult(position, accuracy, bearing)
                else
                    mLatestPosition = MPPositionResult(position, accuracy)

                // Report the updated positioning to attached listeners
                for(listener in mUpdateListeners){
                    listener.onPositionUpdate(mLatestPosition as MPPositionResult)
                }
            }
        }, 0, 1000L)
    }

    fun stop(){
        mPositionProducer?.cancel()
    }

}