package com.meshcomm.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationProvider(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) { _location.value = loc }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        try {
            // Try GPS first, then network
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener
                )
                // Seed with last known
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?.let { _location.value = it }
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 10000L, 50f, locationListener
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopUpdates() {
        try { locationManager.removeUpdates(locationListener) } catch (e: Exception) {}
    }

    fun getLastLatLon(): Pair<Double, Double> {
        val loc = _location.value
        return if (loc != null) Pair(loc.latitude, loc.longitude) else Pair(0.0, 0.0)
    }
}
