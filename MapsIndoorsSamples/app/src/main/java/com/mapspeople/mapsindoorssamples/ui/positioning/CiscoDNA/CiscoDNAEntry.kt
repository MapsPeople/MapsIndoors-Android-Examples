package com.mapspeople.mapsindoorssamples.ui.positioning.CiscoDNA

import android.location.Location
import com.google.gson.annotations.SerializedName
import com.mapsindoors.core.MPFloor
import com.mapsindoors.core.MPPoint
import com.mapsindoors.core.MPPositionProvider
import com.mapsindoors.core.MPPositionResultInterface
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CiscoDNAEntry : MPPositionResultInterface {
    @SerializedName("tennantId")
    val tennantId: String? = null

    @SerializedName("deviceId")
    val deviceId: String? = null

    @SerializedName("macAddress")
    private val mMacAddress: String? = null

    @SerializedName("latitude")
    private val mLatitude = 0.0

    @SerializedName("longitude")
    private val mLongitude = 0.0

    @SerializedName("datasetId")
    val datasetId: String? = null

    @SerializedName("venueId")
    val venueId: String? = null

    @SerializedName("buildingId")
    val buildingId: String? = null

    @SerializedName("floorIndex")
    private val mFloorIndex: String? = null

    @SerializedName("timestamp")
    val timestamp: String? = null

    @SerializedName("operatingSystem")
    private val mOperatingSystem: String? = null

    @SerializedName("confidenceFactor")
    private val mConfidenceFactor = 0

    @SerializedName("maxDetectedRssi")
    val rssi = 0

    @SerializedName("type")
    private val mType: String? = null
    override fun getPoint(): MPPoint? {
        return if (hasFloor()) {
            MPPoint(mLatitude, mLongitude, floorIndex.toDouble())
        } else {
            MPPoint(mLatitude, mLongitude)
        }
    }

    override fun hasFloor(): Boolean {
        return mFloorIndex != null
    }

    override fun getFloorIndex(): Int {
        return try {
            mFloorIndex!!.toInt()
        } catch (e: NumberFormatException) {
            MPFloor.DEFAULT_GROUND_FLOOR_INDEX
        }
    }

    override fun setFloorIndex(i: Int) {}
    
    override fun hasBearing(): Boolean {
        return false
    }

    override fun getBearing(): Float {
        return 0f
    }

    override fun setBearing(v: Float) {}
    
    override fun hasAccuracy(): Boolean {
        return true
    }

    override fun getAccuracy(): Float {
        /*
        The CiscoDNA "confidence factor" is the width of their "bounding box"
        (much like an accuracy circle, but a box, for some reason...).
        The user's position is centered in this square box, so we compute the distance
        to the square box's corner (using pythagoras theorem).
        Lastly, we need to convert from feet to meters (1 foot = 0.3048 meters)
         */
        val x = (mConfidenceFactor.toFloat() / 2).roundToInt()
        val acc = sqrt(x.toDouble().pow(2.0) + x.toDouble().pow(2.0)).toFloat()
        return acc * 0.3048f
    }

    override fun setAccuracy(v: Float) {}
    
    override fun getProvider(): MPPositionProvider? {
        return null
    }

    override fun setProvider(positionProvider: MPPositionProvider?) {}
    
    override fun getAndroidLocation(): Location {
        val loc = Location("")
        loc.latitude = mLatitude
        loc.longitude = mLongitude
        loc.accuracy = accuracy
        loc.time = System.currentTimeMillis()
        return loc
    }

    override fun setAndroidLocation(location: Location?) {}
    
    override fun toString(): String {
        return """
            Rssi: $rssi
            Confidence: $mConfidenceFactor
            Timestamp: $timestamp
            Building: $buildingId
            Venue: $venueId
            TennantId: $tennantId
            
            """.trimIndent()
    }
}