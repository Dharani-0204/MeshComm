package com.meshcomm.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.meshcomm.R
import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.model.SOSAlert
import com.meshcomm.data.model.UserRole
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.data.repository.SOSAlertRepository
import com.meshcomm.databinding.ActivityRescueDashboardBinding
import com.meshcomm.ui.navigation.NavigationActivity
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class RescueDashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RescueDashboard"
        private const val DEFAULT_ZOOM = 12.0
        private const val CLUSTER_DISTANCE_KM = 1.0 // 1km clustering radius

        fun start(context: Context) {
            context.startActivity(Intent(context, RescueDashboardActivity::class.java))
        }
    }

    private lateinit var binding: ActivityRescueDashboardBinding
    private lateinit var sosAlertRepository: SOSAlertRepository
    private lateinit var messageRepository: MessageRepository
    private lateinit var mapView: MapView

    private val sosMarkers = mutableMapOf<String, Marker>()
    private val deviceMarkers = mutableMapOf<String, Marker>()
    private val movementPaths = mutableMapOf<String, Polyline>()
    private val heatmapCircles = mutableListOf<Polygon>()

    private var isHeatmapVisible = true
    private var isClusteringEnabled = true
    private var showActiveOnly = true
    private var showDevices = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify rescuer access
        if (!PrefsHelper.isRescuer(this)) {
            Toast.makeText(this, "Access denied. Rescuer role required.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Configure osmdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityRescueDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sosAlertRepository = SOSAlertRepository(this)
        messageRepository = MessageRepository(AppDatabase.get(this))

        setupToolbar()
        setupMap()
        setupControls()
        setupStats()
        startDataListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Rescue Dashboard"
    }

    private fun setupMap() {
        mapView = binding.mapView

        // Configure map view
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false

        // Set default location (Hyderabad)
        val defaultLocation = GeoPoint(17.385044, 78.486671)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.controller.setCenter(defaultLocation)

        Log.d(TAG, "Offline map ready with osmdroid")
    }

    private fun setupControls() {
        // Heatmap toggle
        binding.chipHeatmap.setOnCheckedChangeListener { _, isChecked ->
            isHeatmapVisible = isChecked
            updateHeatmapVisibility()
            Log.d(TAG, "Heatmap visibility: $isChecked")
        }

        // Clustering toggle
        binding.chipClusters.setOnCheckedChangeListener { _, isChecked ->
            isClusteringEnabled = isChecked
            refreshMarkers()
            Log.d(TAG, "Clustering enabled: $isChecked")
        }

        // Active only filter
        binding.chipActiveOnly.setOnCheckedChangeListener { _, isChecked ->
            showActiveOnly = isChecked
            refreshMarkers()
            Log.d(TAG, "Active only filter: $isChecked")
        }

        // Device visibility toggle
        binding.chipDevices.setOnCheckedChangeListener { _, isChecked ->
            showDevices = isChecked
            updateDeviceVisibility()
            Log.d(TAG, "Show devices: $isChecked")
        }

        // Refresh button
        binding.fabRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            refreshMarkers()
            fetchActiveDevices()
        }
    }

    private fun setupStats() {
        binding.tvTotalAlerts.text = "0"
        binding.tvActiveDevices.text = "0"
        binding.tvLastUpdate.text = "Never"
    }

    private fun startDataListeners() {
        // Listen for SOS alerts from local database
        lifecycleScope.launch {
            val alertsFlow = if (showActiveOnly) {
                sosAlertRepository.getActiveAlerts()
            } else {
                sosAlertRepository.getAllAlerts()
            }

            alertsFlow.collectLatest { alerts ->
                Log.d(TAG, "Received ${alerts.size} SOS alerts from local database")
                updateMap(alerts)
                updateStats(alerts)
            }
        }

        // Listen for mesh messages (for device tracking)
        lifecycleScope.launch {
            messageRepository.getAllMessages().collectLatest { messages ->
                val recentMessages = messages.filter {
                    System.currentTimeMillis() - it.timestamp < 3600000 // Last hour
                }
                updateActiveDevices(recentMessages)
                Log.d(TAG, "Updated active devices from ${recentMessages.size} recent messages")
            }
        }
    }

    private fun updateMap(alerts: List<SOSAlert>) {
        clearMarkers()

        if (alerts.isEmpty()) {
            clearHeatmap()
            return
        }

        val filteredAlerts = if (showActiveOnly) alerts.filter { it.isActive } else alerts

        if (isClusteringEnabled) {
            displayClustered(filteredAlerts)
        } else {
            displayIndividual(filteredAlerts)
        }

        if (isHeatmapVisible) {
            updateHeatmap(filteredAlerts)
        }

        // Auto-zoom to show all alerts
        adjustMapBounds(filteredAlerts)
    }

    private fun displayClustered(alerts: List<SOSAlert>) {
        val clusters = clusterAlerts(alerts, CLUSTER_DISTANCE_KM)

        clusters.forEach { cluster ->
            if (cluster.size == 1) {
                // Single alert
                addSOSMarker(cluster.first(), false)
            } else {
                // Cluster marker
                addClusterMarker(cluster)
            }
        }
    }

    private fun displayIndividual(alerts: List<SOSAlert>) {
        alerts.forEach { alert ->
            addSOSMarker(alert, false)
        }
    }

    private fun addSOSMarker(alert: SOSAlert, isCluster: Boolean = false) {
        val marker = Marker(mapView)
        val position = GeoPoint(alert.latitude, alert.longitude)

        marker.position = position
        marker.title = if (isCluster) "SOS Cluster" else "SOS: ${alert.userName}"
        marker.snippet = formatAlertSnippet(alert)

        // Set marker icon based on type
        val iconRes = if (alert.isActive) R.drawable.ic_sos_active else R.drawable.ic_sos_resolved
        try {
            val drawable = ContextCompat.getDrawable(this, iconRes)
            drawable?.let { marker.icon = it }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load marker icon: ${e.message}")
        }

        marker.setOnMarkerClickListener { clickedMarker, _ ->
            showAlertDetails(alert)
            true
        }

        mapView.overlays.add(marker)
        sosMarkers[alert.alertId] = marker
    }

    private fun addClusterMarker(clusterAlerts: List<SOSAlert>) {
        if (clusterAlerts.isEmpty()) return

        // Calculate cluster center
        val centerLat = clusterAlerts.map { it.latitude }.average()
        val centerLng = clusterAlerts.map { it.longitude }.average()

        val marker = Marker(mapView)
        marker.position = GeoPoint(centerLat, centerLng)
        marker.title = "SOS Cluster (${clusterAlerts.size})"
        marker.snippet = "Tap to view individual alerts"

        // Cluster icon (could be a custom drawable)
        try {
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_cluster_marker)
            drawable?.let { marker.icon = it }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load cluster icon: ${e.message}")
        }

        marker.setOnMarkerClickListener { _, _ ->
            showClusterDetails(clusterAlerts)
            true
        }

        mapView.overlays.add(marker)
    }

    private fun updateActiveDevices(messages: List<com.meshcomm.data.model.Message>) {
        deviceMarkers.values.forEach { mapView.overlays.remove(it) }
        deviceMarkers.clear()

        if (!showDevices) return

        // Group messages by sender to get latest position
        val latestPositions = messages
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .groupBy { it.senderId }
            .mapValues { (_, msgs) -> msgs.maxByOrNull { it.timestamp } }
            .values
            .filterNotNull()

        latestPositions.forEach { message ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(message.latitude, message.longitude)
            marker.title = message.senderName
            marker.snippet = "Last seen: ${formatTimestamp(message.timestamp)}\nBattery: ${message.batteryLevel}%"

            // Different colors for different roles (if available)
            try {
                val iconRes = when {
                    message.senderName.contains("Rescuer", ignoreCase = true) -> R.drawable.ic_rescuer_device
                    else -> R.drawable.ic_civilian_device
                }
                val drawable = ContextCompat.getDrawable(this, iconRes)
                drawable?.let { marker.icon = it }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load device icon: ${e.message}")
            }

            mapView.overlays.add(marker)
            deviceMarkers[message.senderId] = marker
        }

        mapView.invalidate()
    }

    private fun updateHeatmap(alerts: List<SOSAlert>) {
        clearHeatmap()

        if (!isHeatmapVisible || alerts.isEmpty()) return

        alerts.forEach { alert ->
            // Create heatmap circle overlay
            val circle = Polygon(mapView)
            circle.points = createCircle(
                GeoPoint(alert.latitude, alert.longitude),
                500.0 // 500m radius
            )

            // Color based on alert age (red = recent, yellow = old)
            val ageHours = (System.currentTimeMillis() - alert.timestamp) / (1000 * 60 * 60.0)
            val intensity = maxOf(0.1, 1.0 - (ageHours / 24.0)) // Fade over 24 hours

            val red = (255 * intensity).toInt()
            val green = (255 * (1.0 - intensity)).toInt()
            val color = Color.argb(80, red, green, 0)

            circle.fillPaint.color = color
            circle.outlinePaint.color = Color.TRANSPARENT

            mapView.overlays.add(0, circle) // Add at bottom
            heatmapCircles.add(circle)
        }

        mapView.invalidate()
    }

    private fun fetchActiveDevices() {
        // This would fetch from mesh network in a real implementation
        // For now, we get data from local messages
        lifecycleScope.launch {
            try {
                messageRepository.getAllMessages().collectLatest { messages ->
                    val recentCount = messages.count {
                        System.currentTimeMillis() - it.timestamp < 3600000
                    }
                    binding.tvActiveDevices.text = recentCount.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch active devices: ${e.message}")
            }
        }
    }

    private fun updateStats(alerts: List<SOSAlert>) {
        val activeAlerts = alerts.count { it.isActive }
        binding.tvTotalAlerts.text = activeAlerts.toString()
        binding.tvLastUpdate.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date())
    }

    private fun showAlertDetails(alert: SOSAlert) {
        val profile = alert.profile
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        val message = buildString {
            appendLine("User: ${alert.userName}")
            appendLine("Time: ${dateFormat.format(Date(alert.timestamp))}")
            appendLine("Location: ${alert.latitude}, ${alert.longitude}")
            appendLine("Battery: ${alert.batteryLevel}%")
            appendLine("Message: ${alert.message}")

            if (profile != null) {
                appendLine()
                appendLine("─── Medical Profile ───")
                appendLine("Blood Group: ${profile.bloodGroup.ifEmpty { "Unknown" }}")

                if (profile.medicalConditions.isNotEmpty()) {
                    appendLine("Conditions: ${profile.medicalConditions.joinToString(", ")}")
                }

                if (profile.allergies.isNotEmpty()) {
                    appendLine("Allergies: ${profile.allergies.joinToString(", ")}")
                }

                if (profile.emergencyContacts.isNotEmpty()) {
                    appendLine()
                    appendLine("Emergency Contacts:")
                    profile.emergencyContacts.forEach { contact ->
                        appendLine("  • ${contact.name}: ${contact.phone}")
                    }
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("SOS Alert Details")
            .setMessage(message)
            .setPositiveButton("Navigate") { _, _ ->
                openNavigation(alert.latitude, alert.longitude, "SOS: ${alert.userName}")
            }
            .setNeutralButton("Mark Resolved") { _, _ ->
                resolveAlert(alert)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showClusterDetails(alerts: List<SOSAlert>) {
        val items = alerts.map { "${it.userName} - ${formatTimestamp(it.timestamp)}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("SOS Cluster (${alerts.size} alerts)")
            .setItems(items) { _, which ->
                showAlertDetails(alerts[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun resolveAlert(alert: SOSAlert) {
        lifecycleScope.launch {
            try {
                sosAlertRepository.deactivateAlert(alert.alertId)
                Toast.makeText(this@RescueDashboardActivity, "Alert marked as resolved", Toast.LENGTH_SHORT).show()
                refreshMarkers()
            } catch (e: Exception) {
                Toast.makeText(this@RescueDashboardActivity, "Failed to resolve alert", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to resolve alert: ${e.message}")
            }
        }
    }

    private fun openNavigation(lat: Double, lon: Double, locationName: String = "Emergency Location") {
        try {
            NavigationActivity.start(
                context = this,
                destinationLat = lat,
                destinationLng = lon,
                destinationName = locationName
            )
            Log.d(TAG, "Starting navigation to $locationName at $lat, $lon")
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Navigation failed: ${e.message}")
        }
    }

    // Utility Functions

    private fun clusterAlerts(alerts: List<SOSAlert>, radiusKm: Double): List<List<SOSAlert>> {
        val clusters = mutableListOf<MutableList<SOSAlert>>()
        val processed = mutableSetOf<String>()

        for (alert in alerts) {
            if (processed.contains(alert.alertId)) continue

            val cluster = mutableListOf(alert)
            processed.add(alert.alertId)

            for (other in alerts) {
                if (processed.contains(other.alertId)) continue

                val distance = calculateDistance(
                    alert.latitude, alert.longitude,
                    other.latitude, other.longitude
                )

                if (distance <= radiusKm) {
                    cluster.add(other)
                    processed.add(other.alertId)
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun createCircle(center: GeoPoint, radiusMeters: Double): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val segments = 32

        for (i in 0..segments) {
            val angle = 2 * PI * i / segments
            val lat = center.latitude + (radiusMeters / 111320.0) * cos(angle)
            val lon = center.longitude + (radiusMeters / (111320.0 * cos(Math.toRadians(center.latitude)))) * sin(angle)
            points.add(GeoPoint(lat, lon))
        }

        return points
    }

    private fun adjustMapBounds(alerts: List<SOSAlert>) {
        if (alerts.isEmpty()) return

        val latitudes = alerts.map { it.latitude }
        val longitudes = alerts.map { it.longitude }

        val minLat = latitudes.minOrNull() ?: return
        val maxLat = latitudes.maxOrNull() ?: return
        val minLon = longitudes.minOrNull() ?: return
        val maxLon = longitudes.maxOrNull() ?: return

        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        mapView.controller.setCenter(GeoPoint(centerLat, centerLon))

        // Adjust zoom to fit all points
        val latSpan = maxLat - minLat
        val lonSpan = maxLon - minLon
        val maxSpan = maxOf(latSpan, lonSpan)

        val zoom = when {
            maxSpan > 10 -> 8.0
            maxSpan > 5 -> 10.0
            maxSpan > 1 -> 12.0
            maxSpan > 0.5 -> 14.0
            else -> 16.0
        }

        mapView.controller.setZoom(zoom)
    }

    private fun formatAlertSnippet(alert: SOSAlert): String {
        return "Time: ${formatTimestamp(alert.timestamp)}\nBattery: ${alert.batteryLevel}%"
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun refreshMarkers() {
        // Trigger a refresh of all markers
        lifecycleScope.launch {
            val alertsFlow = if (showActiveOnly) {
                sosAlertRepository.getActiveAlerts()
            } else {
                sosAlertRepository.getAllAlerts()
            }

            alertsFlow.collectLatest { alerts ->
                updateMap(alerts)
            }
        }
    }

    private fun clearMarkers() {
        sosMarkers.values.forEach { mapView.overlays.remove(it) }
        sosMarkers.clear()
        mapView.invalidate()
    }

    private fun clearHeatmap() {
        heatmapCircles.forEach { mapView.overlays.remove(it) }
        heatmapCircles.clear()
        mapView.invalidate()
    }

    private fun updateHeatmapVisibility() {
        heatmapCircles.forEach { circle ->
            circle.isVisible = isHeatmapVisible
        }
        mapView.invalidate()
    }

    private fun updateDeviceVisibility() {
        deviceMarkers.values.forEach { marker ->
            marker.isEnabled = showDevices
        }
        mapView.invalidate()
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
    }
}
