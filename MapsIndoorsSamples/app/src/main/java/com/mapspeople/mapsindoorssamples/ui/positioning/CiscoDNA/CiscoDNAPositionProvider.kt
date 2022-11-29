package com.mapspeople.mapsindoorssamples.ui.positioning.CiscoDNA

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mapsindoors.core.*
import com.mapsindoors.livedata.CiscoDNAEntry
import com.mapsindoors.livedata.CiscoDNATopic
import com.mapsindoors.livedata.LiveDataManager
import com.mapsindoors.livedata.MPLiveTopic
import okhttp3.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class CiscoDNAPositionProvider(private val context: Context, private val config: MPCiscoDNAConfig): MPPositionProvider {

    private var mLatestPosition: MPPositionResultInterface? = null
    private val positionUpdateListeners = ArrayList<OnPositionUpdateListener>()
    private val tenantId: String? = config.tenantId
    private var ciscoDeviceId: String? = null
    private var lan: String? = null
    private var wan: String? = null
    private var ciscoDNATopic: CiscoDNATopic? = null

    val MAPSINDOORS_CISCO_ENDPOINT = "https://ciscodna.mapsindoors.com/"


    init {
        LiveDataManager.getInstance().setOnTopicUnsubscribedListener {
            //Here you can see if your topic is unsubscribed
        }

        LiveDataManager.getInstance().setOnTopicSubscribedListener {
            //Here you can fetch errors that might happen with your subscription
        }

        LiveDataManager.getInstance().setOnErrorListener {
            //Here you can fetch errors that might happen with the livedata manager
        }

        LiveDataManager.getInstance().setOnReceivedLiveUpdateListener { mpLiveTopic, liveUpdate ->
            if (liveUpdate.id == ciscoDeviceId) {
                mLatestPosition = liveUpdate.positionResult
                notifyPositionUpdate()
            }
        }
    }

    private fun startSubscription() {
        ciscoDNATopic = CiscoDNATopic(tenantId!!, ciscoDeviceId!!)
        LiveDataManager.getInstance().setOnTopicSubscribedListener { topic: MPLiveTopic ->
            if (topic == ciscoDNATopic) {
                Log.i("CiscoDNA", "topic subscribed to succesfully")
            }
        }
        LiveDataManager.getInstance().subscribeTopic(ciscoDNATopic)
    }

    private fun unsubscribe() {
        LiveDataManager.getInstance().unsubscribeTopic(ciscoDNATopic)
    }

    fun update(onreadyListener: MPReadyListener) {
        updateAddressesAndId {
            if (!ciscoDeviceId.isNullOrEmpty()) {
                if (mLatestPosition == null) {
                    obtainInitialPosition(onreadyListener)
                }else {
                    onreadyListener.onResult()
                }
            }
        }
    }

    private fun obtainInitialPosition(listener: MPReadyListener) {
        val url = "$MAPSINDOORS_CISCO_ENDPOINT$tenantId/api/ciscodna/$ciscoDeviceId"
        val request: Request = Request.Builder().url(url).build()
        if (ciscoDeviceId != null && tenantId != null) {
            val httpClient = OkHttpClient()
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    listener.onResult()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
                        listener.onResult()
                        return
                    }
                    val json = response.body!!.string()
                    val positionResult: CiscoDNAEntry = Gson().fromJson(json, CiscoDNAEntry::class.java)
                    mLatestPosition = positionResult
                    notifyPositionUpdate()
                    listener.onResult()
                    response.close()
                }
            })
        }
    }

    /**
     * This method is responsible for gathering the local and external IP addresses
     * as well as acquiring a device ID from the Cisco DNA API.
     */
    private fun updateAddressesAndId(onComplete: MPReadyListener?) {
        lan = getLocalAddress()
        //mCiscoDeviceId = null;
        fetchExternalAddress {
            if (tenantId != null && lan != null && wan != null) {
                val url: String = "$MAPSINDOORS_CISCO_ENDPOINT$tenantId/api/ciscodna/devicelookup?clientIp=$lan&wanIp=$wan"
                val client = OkHttpClient()
                val request: Request = Request.Builder().url(url).build()
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val gson = Gson()
                        val json = response.body!!.string()
                        val jsonObject =
                            gson.fromJson(json, JsonObject::class.java)
                        ciscoDeviceId = jsonObject["deviceId"].asString
                    } else {
                        Log.d(
                            "ciscodnaprovider",
                            "Could not obtain deviceId from backend deviceID request! Code: " + response.code
                        )
                    }
                    response.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            onComplete?.onResult()
        }
    }

    private fun getLocalAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e(this.javaClass.simpleName, "Failed to resolve LAN address")
        }
        return null
    }

    private fun fetchExternalAddress(listener: MPReadyListener) {
        val httpClient = OkHttpClient()
        val request: Request = Request.Builder().url("https://ipinfo.io/ip").build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onResult()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val str = response.body!!.string()
                    wan = str
                    response.close()
                }
                listener.onResult()
            }
        })
    }

    fun notifyPositionUpdate() {
        for (positionUpdateListener in positionUpdateListeners) {
            mLatestPosition?.let {
                positionUpdateListener.onPositionUpdate(it)
            }
        }
    }

    fun startPositioning() {
        update { startSubscription() }
    }

    fun stopPositioning() {
        unsubscribe()
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