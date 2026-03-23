package com.meshcomm.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import java.util.Locale

/**
 * Utility for converting lat/lng coordinates to human-readable addresses
 * Uses Android Geocoder with offline fallback and local caching
 */
object GeocoderUtil {
    private const val TAG = "GeocoderUtil"
    private const val MAX_CACHE_SIZE = 100

    // Simple LRU cache (lat,lng) -> address using anonymous object
    private val addressCache: LinkedHashMap<String, String> = object : LinkedHashMap<String, String>(
        MAX_CACHE_SIZE + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    /**
     * Convert latitude/longitude to readable address
     * Returns formatted address or fallback coordinate string
     * Uses cache to avoid repeated geocoding calls
     */
    fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String {
        // Check for invalid coordinates
        if (latitude == 0.0 && longitude == 0.0) {
            Log.d(TAG, "Invalid coordinates (0.0, 0.0)")
            return "Location unavailable"
        }

        // Check cache first
        val cacheKey = String.format("%.4f,%.4f", latitude, longitude)
        addressCache[cacheKey]?.let {
            Log.d(TAG, "Cache hit for ($latitude,$longitude) -> $it")
            return it
        }

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val formattedAddress = formatAddress(address)
                Log.d(TAG, "Geocoded: ($latitude,$longitude) -> $formattedAddress")
                addressCache[cacheKey] = formattedAddress  // Cache result
                formattedAddress
            } else {
                Log.w(TAG, "No address found for ($latitude,$longitude)")
                formatCoordinates(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed for ($latitude,$longitude): ${e.message}")
            formatCoordinates(latitude, longitude)
        }
    }

    /**
     * Format Address object into readable string
     * Priority: subLocality > locality > subAdminArea > adminArea
     */
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // Landmark or feature name (optional)
        address.featureName?.let { if (it.isNotBlank() && !it.matches(Regex("\\d+"))) parts.add(it) }

        // Street address
        address.thoroughfare?.let { parts.add(it) }

        // Neighborhood/Area
        address.subLocality?.let { parts.add(it) }

        // City/Town
        address.locality?.let { parts.add(it) }

        // District
        address.subAdminArea?.let {
            if (!parts.contains(it)) parts.add(it)
        }

        // State
        address.adminArea?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.take(3).joinToString(", ") // Limit to 3 components for brevity
        } else {
            // Fallback to postal code if available
            address.postalCode?.let { return "Near $it" }
            formatCoordinates(address.latitude, address.longitude)
        }
    }

    /**
     * Format coordinates as fallback (4 decimal places = ~11m accuracy)
     */
    private fun formatCoordinates(lat: Double, lon: Double): String {
        return "${String.format("%.4f", lat)}, ${String.format("%.4f", lon)}"
    }

    /**
     * Get short address (city/district only) for compact display
     * Uses cache to avoid repeated API calls
     */
    fun getShortAddress(context: Context, latitude: Double, longitude: Double): String {
        if (latitude == 0.0 && longitude == 0.0) return "Unknown location"

        // Check cache for short address
        val cacheKey = "short_${String.format("%.4f,%.4f", latitude, longitude)}"
        addressCache[cacheKey]?.let { return it }

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val parts = mutableListOf<String>()

                address.locality?.let { parts.add(it) }
                address.subAdminArea?.let { if (!parts.contains(it)) parts.add(it) }

                if (parts.isNotEmpty()) {
                    val result = parts.joinToString(", ")
                    addressCache[cacheKey] = result
                    result
                } else {
                    formatCoordinates(latitude, longitude)
                }
            } else {
                formatCoordinates(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed: ${e.message}")
            formatCoordinates(latitude, longitude)
        }
    }

    /**
     * Check if Geocoder is available on device
     */
    fun isGeocoderAvailable(context: Context): Boolean {
        return Geocoder.isPresent().also {
            Log.d(TAG, "Geocoder available: $it")
        }
    }

    /**
     * Clear address cache
     */
    fun clearCache() {
        addressCache.clear()
        Log.d(TAG, "Address cache cleared")
    }
}

