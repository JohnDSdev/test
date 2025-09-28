package com.example.aichat.llm.tools

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Tool that returns the device's last known location.  If available it can
 * optionally reverse geocode coordinates into a human readable place name.
 */
object LocationTool {
    const val NAME = "location"

    @SuppressLint("MissingPermission")
    suspend fun getLocation(context: Context): Map<String, Any> {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation!!.accuracy) {
                bestLocation = loc
            }
        }
        bestLocation?.let { loc ->
            val result = mutableMapOf<String, Any>(
                "lat" to loc.latitude,
                "lon" to loc.longitude,
                "accuracy_m" to loc.accuracy
            )
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    result["place"] = listOfNotNull(addr.locality, addr.adminArea, addr.countryName)
                        .joinToString(", ")
                }
            } catch (e: Exception) {
                // ignore geocoder failures
            }
            return result
        }
        return mapOf("error" to "location unavailable")
    }
}