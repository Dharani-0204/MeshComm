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

        // Location update settings - enhanced for Swiggy-like experience
        private const val MIN_LOCATION_UPDATE_TIME = 3000L // 3 seconds for real-time feel
        private const val MIN_LOCATION_UPDATE_DISTANCE = 3f // 3 meters for precise tracking

        // Distance thresholds
        private const val ARRIVAL_THRESHOLD_METERS = 10f
        private const val CLOSE_DISTANCE_METERS = 100f
        private const val VERY_CLOSE_DISTANCE_METERS = 50f

        // UI update intervals - Swiggy-like frequent updates
        private const val LIVE_UPDATE_INTERVAL = 3000L // Update UI every 3 seconds
        private const val DISTANCE_CALCULATION_INTERVAL = 1000L // Calculate distance every 1 second
        private const val ETA_CALCULATION_INTERVAL = 5000L // Update ETA every 5 seconds

        // Speed and movement tracking
        private const val MIN_SPEED_FOR_ETA = 0.5f // m/s - minimum speed to calculate ETA
        private const val SPEED_SMOOTHING_FACTOR = 0.3f // For smoothing speed calculations

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

    // Enhanced tracking for Swiggy-like experience
    private var previousLocation: Location? = null
    private var currentSpeed: Float = 0f // m/s
    private var averageSpeed: Float = 0f // m/s for ETA calculation
    private var totalDistanceTraveled: Float = 0f
    private var initialDistance: Float = 0f
    private var estimatedTimeArrival: String = "Calculating..."
    private var navigationStartTime: Long = 0L

    // Movement tracking
    private val recentSpeeds = mutableListOf<Float>()
    private val locationHistory = mutableListOf<Location>()
    private var lastMovementTime: Long = 0L

    // Map overlays
    private var startMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routeLine: Polyline? = null
    private var pathTrail: Polyline? = null // Track the path taken

    // UI update tracking
    private var lastDistanceUpdate = 0L
    private var lastETAUpdate = 0L
    private var lastLiveUpdate = 0L

    // Runnable for live updates
    private val liveUpdateRunnable = object : Runnable {
        override fun run() {
            updateLiveTracking()
            handler.postDelayed(this, LIVE_UPDATE_INTERVAL)
        }
    }

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

        // Initialize navigation tracking
        navigationStartTime = System.currentTimeMillis()
        initialDistance = calculateDistance(0.0, 0.0, destinationLat, destinationLng) // Will be updated with real location

        // Request location permissions and start navigation
        checkPermissionsAndStartNavigation()

        // Start live tracking updates (Swiggy-like)
        handler.post(liveUpdateRunnable)
    }

    private fun extractDestinationFromIntent() {
        destinationLat = intent.getDoubleExtra("destinationLat", 0.0)
        destinationLng = intent.getDoubleExtra("destinationLng", 0.0)
        destinationName = intent.getStringExtra("destinationName") ?: "Destination"

        // Validate coordinates
        if (!isValidCoordinate(destinationLat, destinationLng)) {
            Log.e(TAG, "Invalid coordinates received: $destinationLat, $destinationLng")
            Toast.makeText(this, "Invalid destination coordinates", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d(TAG, "Navigation to: $destinationName at $destinationLat, $destinationLng")
    }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
        return lat != 0.0 || lng != 0.0 &&
               lat >= -90.0 && lat <= 90.0 &&
               lng >= -180.0 && lng <= 180.0
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
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m, speed: ${location.speed}m/s)")

        val previousLoc = currentLocation
        currentLocation = location

        // Update location UI immediately
        updateLocationUI(location)

        // Enhanced movement tracking
        if (previousLoc != null) {
            updateMovementTracking(previousLoc, location)
        } else {
            // First location - calculate initial distance
            initialDistance = calculateDistance(
                location.latitude, location.longitude,
                destinationLat, destinationLng
            )
            Log.d(TAG, "Initial distance to destination: ${initialDistance}m")
        }

        // Add to location history for path tracking
        locationHistory.add(location)
        if (locationHistory.size > 50) { // Keep only recent 50 locations
            locationHistory.removeAt(0)
        }

        // Update distance and route (throttled)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDistanceUpdate >= DISTANCE_CALCULATION_INTERVAL) {
            lastDistanceUpdate = currentTime
            updateDistanceAndRoute(location)
        }

        updateLocationStatus("GPS active - ${location.accuracy.roundToInt()}m accuracy")
    }

    private fun updateMovementTracking(previousLoc: Location, currentLoc: Location) {
        // Calculate distance moved
        val distanceMoved = previousLoc.distanceTo(currentLoc)
        totalDistanceTraveled += distanceMoved

        // Calculate current speed
        val timeDiff = (currentLoc.time - previousLoc.time) / 1000f // seconds
        if (timeDiff > 0) {
            currentSpeed = if (currentLoc.hasSpeed() && currentLoc.speed > 0) {
                currentLoc.speed // Use GPS speed if available
            } else {
                distanceMoved / timeDiff // Calculate from distance/time
            }

            // Add to recent speeds for averaging
            recentSpeeds.add(currentSpeed)
            if (recentSpeeds.size > 10) { // Keep only recent 10 speed readings
                recentSpeeds.removeAt(0)
            }

            // Calculate smoothed average speed
            averageSpeed = if (recentSpeeds.isNotEmpty()) {
                recentSpeeds.average().toFloat()
            } else {
                currentSpeed
            }

            lastMovementTime = currentLoc.time

            Log.d(TAG, "Movement: ${distanceMoved}m in ${timeDiff}s, speed: ${currentSpeed}m/s, avg: ${averageSpeed}m/s")
        }
    }

    private fun updateLiveTracking() {
        currentLocation?.let { location ->
            val currentTime = System.currentTimeMillis()

            // Calculate current distance to destination
            val currentDistance = calculateDistance(
                location.latitude, location.longitude,
                destinationLat, destinationLng
            )

            // Update ETA calculation
            if (currentTime - lastETAUpdate >= ETA_CALCULATION_INTERVAL) {
                lastETAUpdate = currentTime
                updateETA(currentDistance)
            }

            // Update progress and live stats
            updateLiveProgress(currentDistance)

            // Update path trail visualization
            updatePathTrail()

            Log.d(TAG, "Live tracking: ${currentDistance}m to go, ETA: $estimatedTimeArrival, speed: ${averageSpeed}m/s")
        }
    }

    private fun updateETA(currentDistance: Float) {
        estimatedTimeArrival = when {
            averageSpeed < MIN_SPEED_FOR_ETA -> "Calculating..."
            currentDistance <= ARRIVAL_THRESHOLD_METERS -> "Arriving now!"
            else -> {
                val etaSeconds = (currentDistance / averageSpeed).toInt()
                val etaMinutes = etaSeconds / 60
                val etaSecondsRemainder = etaSeconds % 60

                when {
                    etaMinutes > 60 -> "${etaMinutes / 60}h ${etaMinutes % 60}m"
                    etaMinutes > 0 -> "${etaMinutes}m ${etaSecondsRemainder}s"
                    else -> "${etaSecondsRemainder}s"
                }
            }
        }
    }

    private fun updateLiveProgress(currentDistance: Float) {
        // Calculate progress percentage
        val progressPercentage = if (initialDistance > 0) {
            ((initialDistance - currentDistance) / initialDistance * 100).coerceIn(0f, 100f)
        } else {
            0f
        }

        // Update UI with progress
        runOnUiThread {
            // You could add a progress bar to the layout and update it here
            val progressText = when {
                progressPercentage > 90 -> "Almost there!"
                progressPercentage > 75 -> "Nearly arrived"
                progressPercentage > 50 -> "Halfway there"
                progressPercentage > 25 -> "Making progress"
                else -> "Just started"
            }

            // Update direction text with more context
            val direction = currentLocation?.let { loc ->
                val bearing = calculateBearing(
                    loc.latitude, loc.longitude,
                    destinationLat, destinationLng
                )
                bearingToDirection(bearing)
            } ?: "Unknown"

            val speedText = if (averageSpeed > MIN_SPEED_FOR_ETA) {
                " • ${(averageSpeed * 3.6).roundToInt()} km/h"
            } else ""

            binding.tvDirection.text = "Head $direction$speedText • $progressText"
        }
    }

    private fun updatePathTrail() {
        if (locationHistory.size < 2) return

        // Remove existing path trail
        pathTrail?.let { mapView.overlays.remove(it) }

        // Create path trail from location history
        pathTrail = Polyline().apply {
            val points = locationHistory.map { GeoPoint(it.latitude, it.longitude) }
            setPoints(points)

            outlinePaint.apply {
                color = ContextCompat.getColor(this@NavigationActivity, R.color.mesh_active)
                strokeWidth = 4f
                alpha = 180 // Semi-transparent for trail effect
            }
        }

        mapView.overlays.add(1, pathTrail) // Add above heatmap but below markers
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
        // Calculate distance to destination
        val distance = calculateDistance(
            location.latitude, location.longitude,
            destinationLat, destinationLng
        )

        // Update distance display with enhanced info
        updateEnhancedDistanceDisplay(distance)

        // Update route line
        updateRouteLine(location)

        // Check arrival with enhanced feedback
        checkArrivalWithFeedback(distance)

        // Auto-zoom based on distance for better UX
        when {
            distance < VERY_CLOSE_DISTANCE_METERS -> {
                // Very close - zoom in for precision
                mapView.controller.setZoom(19.0)
            }
            distance < CLOSE_DISTANCE_METERS -> {
                autoZoomToShowBothPoints(location)
            }
        }

        mapView.invalidate()
    }

    private fun updateEnhancedDistanceDisplay(distanceMeters: Float) {
        val distanceText = when {
            distanceMeters < 1000 -> "${distanceMeters.roundToInt()} m away"
            else -> "${(distanceMeters / 1000).round(1)} km away"
        }

        // Enhanced distance display with context
        val contextText = when {
            distanceMeters <= ARRIVAL_THRESHOLD_METERS -> "🎯 You've arrived!"
            distanceMeters <= VERY_CLOSE_DISTANCE_METERS -> "🚶 Almost there"
            distanceMeters <= CLOSE_DISTANCE_METERS -> "📍 Very close"
            distanceMeters <= 500 -> "🔄 Approaching"
            else -> "🗺️ En route"
        }

        val etaText = if (estimatedTimeArrival != "Calculating..." && distanceMeters > ARRIVAL_THRESHOLD_METERS) {
            " • ETA $estimatedTimeArrival"
        } else ""

        binding.tvDistance.text = "$contextText • $distanceText$etaText"

        Log.d(TAG, "Enhanced distance updated: $distanceText, ETA: $estimatedTimeArrival")
    }

    private fun checkArrivalWithFeedback(distance: Float) {
        when {
            distance <= ARRIVAL_THRESHOLD_METERS -> {
                if (binding.layoutArrival.visibility != View.VISIBLE) {
                    binding.layoutArrival.visibility = View.VISIBLE
                    binding.tvDistance.text = "🎯 Arrived!"
                    binding.tvDirection.text = "You have reached your destination"

                    // Enhanced arrival feedback
                    Toast.makeText(
                        this,
                        "🎉 Navigation completed to $destinationName",
                        Toast.LENGTH_LONG
                    ).show()

                    // Stop live updates on arrival
                    handler.removeCallbacks(liveUpdateRunnable)

                    Log.i(TAG, "Navigation completed - destination reached")
                }
            }
            distance <= VERY_CLOSE_DISTANCE_METERS -> {
                // Very close feedback
                if (binding.layoutArrival.visibility == View.GONE) {
                    binding.tvDirection.text = "${binding.tvDirection.text} • Almost there!"
                }
                binding.layoutArrival.visibility = View.GONE
            }
            else -> {
                binding.layoutArrival.visibility = View.GONE
            }
        }
    }

    private fun calculateDistance(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0] // Distance in meters
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

        // Stop live updates to prevent memory leaks
        handler.removeCallbacks(liveUpdateRunnable)

        Log.d(TAG, "Navigation session ended. Total distance traveled: ${totalDistanceTraveled.roundToInt()}m")
    }

    // Utility extensions
    private fun Float.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }
}