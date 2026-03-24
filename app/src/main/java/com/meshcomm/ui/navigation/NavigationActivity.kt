package com.meshcomm.ui.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.meshcomm.R
import com.meshcomm.databinding.ActivityNavigationBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*

class NavigationActivity : AppCompatActivity(), LocationListener {

    companion object {
        private const val TAG = "NavigationActivity"
        private const val LOCATION_PERMISSION_REQUEST = 1001

        // Location update settings
        private const val MIN_LOCATION_UPDATE_TIME = 3000L // 3 seconds
        private const val MIN_LOCATION_UPDATE_DISTANCE = 5f // 5 meters

        // Distance thresholds
        private const val ARRIVAL_THRESHOLD_METERS = 10f
        private const val CLOSE_DISTANCE_METERS = 100f

        fun start(
            context: Context,
            destinationLat: Double,
            destinationLng: Double,
            destinationName: String
        ) {
            val intent = Intent(context, NavigationActivity::class.java).apply {
                putExtra("destinationLat", destinationLat)
                putExtra("destinationLng", destinationLng)
                putExtra("destinationName", destinationName)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityNavigationBinding
    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())

    // Navigation data
    private var destinationLat = 0.0
    private var destinationLng = 0.0
    private var destinationName = ""
    private var currentLocation: Location? = null

    // Map overlays
    private var startMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routeLine: Polyline? = null

    // UI update tracking
    private var lastDistanceUpdate = 0L
    private val distanceUpdateInterval = 1000L // Update distance every 1 second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure osmdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get destination from intent
        extractDestinationFromIntent()

        setupToolbar()
        setupMap()
        setupLocationManager()
        setupClickListeners()

        // Request location permissions and start navigation
        checkPermissionsAndStartNavigation()
    }

    private fun extractDestinationFromIntent() {
        destinationLat = intent.getDoubleExtra("destinationLat", 0.0)
        destinationLng = intent.getDoubleExtra("destinationLng", 0.0)
        destinationName = intent.getStringExtra("destinationName") ?: "Destination"

        Log.d(TAG, "Navigation to: $destinationName at $destinationLat, $destinationLng")
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Navigate to $destinationName"
    }

    private fun setupMap() {
        mapView = binding.mapView

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false

        // Set initial zoom to destination
        val destinationPoint = GeoPoint(destinationLat, destinationLng)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(destinationPoint)

        // Add destination marker immediately
        addDestinationMarker()
    }

    private fun setupLocationManager() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun setupClickListeners() {
        // Recenter to current location
        binding.fabMyLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }

        // Recenter to destination
        binding.fabDestination.setOnClickListener {
            centerMapOnDestination()
        }

        // Force GPS refresh
        binding.btnRefreshLocation.setOnClickListener {
            requestLocationUpdate()
            Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show()
        }

        // Finish navigation
        binding.btnFinishNavigation.setOnClickListener {
            finish()
        }
    }

    private fun checkPermissionsAndStartNavigation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            // Start GPS location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_LOCATION_UPDATE_TIME,
                MIN_LOCATION_UPDATE_DISTANCE,
                this
            )

            // Also try network provider as fallback
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_LOCATION_UPDATE_TIME,
                    MIN_LOCATION_UPDATE_DISTANCE,
                    this
                )
            }

            // Get last known location for immediate display
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            lastKnownLocation?.let { onLocationChanged(it) }

            updateLocationStatus("Getting location...")
            Log.d(TAG, "Location updates started")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}")
            updateLocationStatus("Location unavailable")
            showLocationHelp()
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")

        currentLocation = location
        updateLocationUI(location)
        updateDistanceAndRoute(location)
        updateLocationStatus("GPS active")
    }

    private fun updateLocationUI(location: Location) {
        val currentPoint = GeoPoint(location.latitude, location.longitude)

        // Update/add start marker
        if (startMarker == null) {
            startMarker = Marker(mapView).apply {
                position = currentPoint
                title = "Your Location"
                snippet = "Accuracy: ${location.accuracy.roundToInt()}m"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                try {
                    icon = ContextCompat.getDrawable(this@NavigationActivity, R.drawable.ic_my_location)
                } catch (e: Exception) {
                    // Fallback to default marker
                    Log.w(TAG, "Could not load location icon")
                }
            }
            mapView.overlays.add(startMarker)
        } else {
            startMarker?.position = currentPoint
            startMarker?.snippet = "Accuracy: ${location.accuracy.roundToInt()}m"
        }
    }

    private fun updateDistanceAndRoute(location: Location) {
        val currentTime = System.currentTimeMillis()

        // Throttle distance updates to avoid UI spam
        if (currentTime - lastDistanceUpdate < distanceUpdateInterval) {
            return
        }
        lastDistanceUpdate = currentTime

        // Calculate distance to destination
        val distance = calculateDistance(
            location.latitude, location.longitude,
            destinationLat, destinationLng
        )

        // Update distance display
        updateDistanceDisplay(distance)

        // Update route line
        updateRouteLine(location)

        // Check arrival
        checkArrival(distance)

        // Auto-zoom if very close to destination
        if (distance < CLOSE_DISTANCE_METERS) {
            autoZoomToShowBothPoints(location)
        }

        mapView.invalidate()
    }

    private fun calculateDistance(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0] // Distance in meters
    }

    private fun updateDistanceDisplay(distanceMeters: Float) {
        val distanceText = when {
            distanceMeters < 1000 -> "${distanceMeters.roundToInt()} m away"
            else -> "${(distanceMeters / 1000).round(1)} km away"
        }

        binding.tvDistance.text = distanceText

        // Update direction hint
        currentLocation?.let { location ->
            val bearing = calculateBearing(
                location.latitude, location.longitude,
                destinationLat, destinationLng
            )
            val direction = bearingToDirection(bearing)
            binding.tvDirection.text = "Head $direction"
        }

        Log.d(TAG, "Distance updated: $distanceText")
    }

    private fun calculateBearing(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val startLatRad = Math.toRadians(startLat)
        val startLngRad = Math.toRadians(startLng)
        val endLatRad = Math.toRadians(endLat)
        val endLngRad = Math.toRadians(endLng)

        val dLng = endLngRad - startLngRad

        val y = sin(dLng) * cos(endLatRad)
        val x = cos(startLatRad) * sin(endLatRad) - sin(startLatRad) * cos(endLatRad) * cos(dLng)

        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    private fun bearingToDirection(bearing: Float): String {
        return when ((bearing + 22.5) % 360) {
            in 0.0..44.9 -> "North"
            in 45.0..89.9 -> "North-East"
            in 90.0..134.9 -> "East"
            in 135.0..179.9 -> "South-East"
            in 180.0..224.9 -> "South"
            in 225.0..269.9 -> "South-West"
            in 270.0..314.9 -> "West"
            in 315.0..359.9 -> "North-West"
            else -> "North"
        }
    }

    private fun updateRouteLine(location: Location) {
        // Remove previous route line
        routeLine?.let { mapView.overlays.remove(it) }

        // Create new route line
        routeLine = Polyline().apply {
            val points = listOf(
                GeoPoint(location.latitude, location.longitude),
                GeoPoint(destinationLat, destinationLng)
            )
            setPoints(points)

            outlinePaint.apply {
                color = ContextCompat.getColor(this@NavigationActivity, R.color.md_theme_primary)
                strokeWidth = 8f
            }
        }

        mapView.overlays.add(0, routeLine) // Add at bottom so markers are on top
    }

    private fun addDestinationMarker() {
        destinationMarker = Marker(mapView).apply {
            position = GeoPoint(destinationLat, destinationLng)
            title = destinationName
            snippet = "Destination"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            try {
                icon = ContextCompat.getDrawable(this@NavigationActivity, R.drawable.ic_destination_marker)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load destination icon")
            }
        }
        mapView.overlays.add(destinationMarker)
        mapView.invalidate()
    }

    private fun checkArrival(distance: Float) {
        if (distance <= ARRIVAL_THRESHOLD_METERS) {
            binding.tvDistance.text = "Arrived!"
            binding.tvDirection.text = "You have reached your destination"

            // Show arrival notification
            if (binding.layoutArrival.visibility != View.VISIBLE) {
                binding.layoutArrival.visibility = View.VISIBLE
                Toast.makeText(this, "You have arrived at $destinationName", Toast.LENGTH_LONG).show()
            }
        } else {
            binding.layoutArrival.visibility = View.GONE
        }
    }

    private fun centerMapOnCurrentLocation() {
        currentLocation?.let { location ->
            val currentPoint = GeoPoint(location.latitude, location.longitude)
            mapView.controller.animateTo(currentPoint)
            mapView.controller.setZoom(18.0)
        } ?: Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
    }

    private fun centerMapOnDestination() {
        val destinationPoint = GeoPoint(destinationLat, destinationLng)
        mapView.controller.animateTo(destinationPoint)
        mapView.controller.setZoom(18.0)
    }

    private fun autoZoomToShowBothPoints(location: Location) {
        try {
            val currentPoint = GeoPoint(location.latitude, location.longitude)
            val destinationPoint = GeoPoint(destinationLat, destinationLng)

            // Calculate bounding box
            val minLat = minOf(currentPoint.latitude, destinationPoint.latitude)
            val maxLat = maxOf(currentPoint.latitude, destinationPoint.latitude)
            val minLng = minOf(currentPoint.longitude, destinationPoint.longitude)
            val maxLng = maxOf(currentPoint.longitude, destinationPoint.longitude)

            // Add padding
            val latPadding = (maxLat - minLat) * 0.2
            val lngPadding = (maxLng - minLng) * 0.2

            val boundingBox = org.osmdroid.util.BoundingBox(
                maxLat + latPadding, maxLng + lngPadding,
                minLat - latPadding, minLng - lngPadding
            )

            mapView.zoomToBoundingBox(boundingBox, true, 100)
        } catch (e: Exception) {
            Log.w(TAG, "Auto-zoom failed: ${e.message}")
        }
    }

    private fun updateLocationStatus(status: String) {
        binding.tvLocationStatus.text = status
    }

    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null)
            } catch (e: Exception) {
                Log.w(TAG, "Single location request failed: ${e.message}")
            }
        }
    }

    private fun showLocationHelp() {
        binding.layoutLocationHelp.visibility = View.VISIBLE
    }

    // LocationListener methods
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "Location provider status changed: $provider -> $status")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Location provider enabled: $provider")
        updateLocationStatus("GPS enabled")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Location provider disabled: $provider")
        updateLocationStatus("GPS disabled")
        showLocationHelp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission required for navigation", Toast.LENGTH_LONG).show()
                showLocationHelp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }

    // Utility extensions
    private fun Float.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }
}