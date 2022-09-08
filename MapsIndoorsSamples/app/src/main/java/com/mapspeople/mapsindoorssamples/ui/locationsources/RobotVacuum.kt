package com.mapspeople.mapsindoorssamples.ui.locationsources

import com.mapsindoors.core.models.MPLatLng

class RobotVacuum (private var mBatteryCharge: Int?, private var mPosition: MPLatLng?){
    fun setPosition(position: MPLatLng?) {
        mPosition = position
    }
    fun getPosition(): MPLatLng? {
        return mPosition
    }
    fun setBatteryCharge(charge: Int?) {
        mBatteryCharge = charge
    }
    fun getBatteryCharge(): Int? {
        return mBatteryCharge
    }
}