package com.meshcomm.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "LocationProvider"
        private const val FRESH_LOCATION_TIMEOUT_MS = 15000L // 15 seconds
        private const val MIN_ACCURACY_METERS = 100f // Accept locations within 100m accuracy
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location
    private val handler = Handler(Looper.getMainLooper())

    // Fresh location request tracking
    private var freshLocationCallback: LocationCallback? = null
    private var freshLocationListener: LocationListener? = null
    private var timeoutRunnable: Runnable? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) { _location.value = loc }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    interface LocationCallback {
        fun onLocationReceived(location: Location)
        fun onLocationFailed(error: String)
    }

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission for continuous updates")
            return
        }

        try {
            // Try GPS first, then network
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener
                )
                // Seed with last known
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?.let {
                        _location.value = it
                        Log.d(TAG, "Seeded with last known GPS location: ${it.latitude}, ${it.longitude}")
                    }
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 10000L, 50f, locationListener
                )
                // Fallback seed if no GPS
                if (_location.value == null) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?.let {
                            _location.value = it
                            Log.d(TAG, "Seeded with last known Network location: ${it.latitude}, ${it.longitude}")
                        }
                }
            }
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}", e)
        }
    }

    fun stopUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates: ${e.message}")
        }
    }

    fun getLastLatLon(): Pair<Double, Double> {
        val loc = _location.value
        return if (loc != null) {
            Log.d(TAG, "Returning cached location: ${loc.latitude}, ${loc.longitude}")
            Pair(loc.latitude, loc.longitude)
        } else {
            Log.d(TAG, "No cached location available, returning (0.0, 0.0)")
            Pair(0.0, 0.0)
        }
    }

    /**
     * Request a fresh location with callback. This is ideal for SOS scenarios where we need
     * the most current location possible.
     */
    @SuppressLint("MissingPermission")
    fun requestFreshLocation(callback: LocationCallback) {
        Log.d(TAG, "Fresh location requested")

        // Clear any existing fresh location request
        clearFreshLocationRequest()

        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission for fresh location")
            callback.onLocationFailed("Location permission not granted")
            return
        }

        // Check if providers are available
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            Log.w(TAG, "No location providers available")
            // Try to return last known location as fallback
            val lastKnown = getLastKnownLocationFromProvider()
            if (lastKnown != null) {
                Log.d(TAG, "Using last known location as fallback")
                callback.onLocationReceived(lastKnown)
            } else {
                callback.onLocationFailed("GPS and Network location disabled")
            }
            return
        }

        freshLocationCallback = callback

        // Create fresh location listener
        freshLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "Fresh location received: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")

                // Accept location if accuracy is reasonable or if it's the best we have after timeout
                if (location.accuracy <= MIN_ACCURACY_METERS) {
                    Log.d(TAG, "Location accuracy acceptable (${location.accuracy}m), using it")
                    clearFreshLocationRequest()
                    freshLocationCallback?.onLocationReceived(location)
                    // Also update our current location
                    _location.value = location
                }
               // If accuracy is poor, keep waiting for better location until timeout
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Fresh location provider status changed: $provider -> $status")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Fresh location provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.d(TAG, "Fresh location provider disabled: $provider")
            }
        }

        try {
            // Request single update from available providers
            if (gpsEnabled) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, freshLocationListener!!, Looper.getMainLooper())
                Log.d(TAG, "Requested single GPS update")
            }
            if (networkEnabled) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, freshLocationListener!!, Looper.getMainLooper())
                Log.d(TAG, "Requested single Network update")
            }

            // Set timeout fallback
            timeoutRunnable = Runnable {
                Log.w(TAG, "Fresh location request timed out after ${FRESH_LOCATION_TIMEOUT_MS}ms")

                // Try to get the best location we have
                val currentListener = freshLocationListener
                if (currentListener != null) {
                    // Get last location from the listener if any update was received
                    val lastFromProvider = getLastKnownLocationFromProvider()
                    if (lastFromProvider != null) {
                        Log.d(TAG, "Using last known location due to timeout")
                        clearFreshLocationRequest()
                        freshLocationCallback?.onLocationReceived(lastFromProvider)
                        _location.value = lastFromProvider
                    } else {
                        clearFreshLocationRequest()
                        freshLocationCallback?.onLocationFailed("Location request timed out and no location available")
                    }
                }
            }

            handler.postDelayed(timeoutRunnable!!, FRESH_LOCATION_TIMEOUT_MS)

        } catch (e: Exception) {
            Log.e(TAG, "Error requesting fresh location: ${e.message}", e)
            clearFreshLocationRequest()
            callback.onLocationFailed("Error requesting location: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocationFromProvider(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            // Prefer GPS, then Network
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location: ${e.message}")
            null
        }
    }

    private fun clearFreshLocationRequest() {
        freshLocationListener?.let { listener ->
            try {
                locationManager.removeUpdates(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing fresh location listener: ${e.message}")
            }
        }
        freshLocationListener = null
        freshLocationCallback = null

        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * For compatibility with existing code - synchronous access to cached location
     */
    fun getCurrentLocation(): Location? {
        val cachedLocation = _location.value
        Log.d(TAG, "getCurrentLocation() called - cached location: $cachedLocation")
        return cachedLocation
    }
}
