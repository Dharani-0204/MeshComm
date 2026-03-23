package com.meshcomm.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.meshcomm.R
import com.meshcomm.databinding.FragmentMapBinding
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.location.LocationProvider
import com.meshcomm.ui.home.MeshViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    companion object {
        private const val TAG = "MapFragment"
        private const val KEY_CAMERA_LAT = "camera_lat"
        private const val KEY_CAMERA_LON = "camera_lon"
        private const val KEY_CAMERA_ZOOM = "camera_zoom"
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()

    private lateinit var mapView: MapView
    private lateinit var offlineTileProvider: OfflineTileProvider
    private lateinit var mapDownloader: TelanganaMapDownloader
    private lateinit var locationProvider: LocationProvider

    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val sosMarkers = mutableListOf<Marker>()

    // Telangana center: approximately 17.5°N, 79.5°E
    private val telanganaCenter = GeoPoint(17.5, 79.5)

    // Saved camera position for state restoration
    private var savedCameraLat: Double? = null
    private var savedCameraLon: Double? = null
    private var savedCameraZoom: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saved: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore saved camera position
        savedInstanceState?.let { bundle ->
            savedCameraLat = bundle.getDouble(KEY_CAMERA_LAT, 0.0).takeIf { it != 0.0 }
            savedCameraLon = bundle.getDouble(KEY_CAMERA_LON, 0.0).takeIf { it != 0.0 }
            savedCameraZoom = bundle.getDouble(KEY_CAMERA_ZOOM, 0.0).takeIf { it != 0.0 }
            Log.d(TAG, "Restored camera position: ($savedCameraLat, $savedCameraLon) zoom=$savedCameraZoom")
        }

        checkPermissionsAndSetup()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current camera position
        if (::mapView.isInitialized) {
            val center = mapView.mapCenter
            outState.putDouble(KEY_CAMERA_LAT, center.latitude)
            outState.putDouble(KEY_CAMERA_LON, center.longitude)
            outState.putDouble(KEY_CAMERA_ZOOM, mapView.zoomLevelDouble)
            Log.d(TAG, "Saved camera position: (${center.latitude}, ${center.longitude}) zoom=${mapView.zoomLevelDouble}")
        }
    }

    private fun checkPermissionsAndSetup() {
        if (hasLocationPermission()) {
            setupMap()
            setupClickListeners()
            observeData()
            setupLocationTracking()
        } else {
            showEmptyState()
            Toast.makeText(
                requireContext(),
                "Location permission required for maps",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showEmptyState() {
        binding.mapView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun setupMap() {
        mapView = binding.mapView

        // Initialize offline tile provider
        offlineTileProvider = OfflineTileProvider()
        mapDownloader = TelanganaMapDownloader()

        // Configure map
        mapView.apply {
            setTileSource(offlineTileProvider.getTileSource())
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0  // Increased to 19 for street-level detail

            // Restore saved camera position or use default
            if (savedCameraLat != null && savedCameraLon != null && savedCameraZoom != null) {
                controller.setZoom(savedCameraZoom!!)
                controller.setCenter(GeoPoint(savedCameraLat!!, savedCameraLon!!))
                Log.d(TAG, "Map restored to saved position: ($savedCameraLat, $savedCameraLon) zoom=$savedCameraZoom")
            } else {
                // Set initial view to Telangana
                controller.setZoom(8.0)
                controller.setCenter(telanganaCenter)
                Log.d(TAG, "Map initialized with default Telangana view")
            }
        }

        // Setup location overlay
        setupMyLocationOverlay()

        // Update download button state
        updateDownloadButtonState()

        binding.mapView.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        Log.d(TAG, "Map initialized and centered on Telangana (zoom 5-19 supported)")
    }

    private fun setupMyLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            mapView
        ).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        mapView.overlays.add(myLocationOverlay)
        Log.d(TAG, "Location overlay enabled")
    }

    private fun setupClickListeners() {
        // Download Telangana map button
        binding.btnDownloadMap.setOnClickListener {
            startMapDownload()
        }

        // Center on location button
        binding.fabCenterLocation.setOnClickListener {
            centerOnCurrentLocation()
        }

        // Floating SOS button - use proper navigation action
        binding.fabSos.setOnClickListener {
            Log.d(TAG, "SOS button clicked - navigating to SOS screen")
            findNavController().navigate(R.id.action_map_to_sos)
        }
    }

    private fun setupLocationTracking() {
        locationProvider = LocationProvider(requireContext())
        locationProvider.startUpdates()

        viewLifecycleOwner.lifecycleScope.launch {
            locationProvider.location.collectLatest { location ->
                location?.let {
                    Log.d(TAG, "Location update: ${it.latitude}, ${it.longitude}")
                }
            }
        }
    }

    private fun observeData() {
        // Observe all messages to display SOS markers
        viewModel.allMessages.asLiveData().observe(viewLifecycleOwner) { allMessages ->
            val messagesWithLocation = allMessages.filter {
                it.latitude != 0.0 && it.longitude != 0.0
            }
            val sosList = messagesWithLocation.filter { it.type == MessageType.SOS }

            // Update SOS markers on map
            updateSosMarkers(sosList)

            // Update statistics in header
            binding.tvMapInfo.text = when {
                sosList.isNotEmpty() -> "🚨 ${sosList.size} SOS alerts • ${messagesWithLocation.size} messages with location"
                messagesWithLocation.isNotEmpty() -> "📍 ${messagesWithLocation.size} messages with location"
                else -> "No location data received yet"
            }

            // Show/hide emergency zone alert
            if (sosList.isNotEmpty()) {
                binding.tvEmergencyZone.visibility = View.VISIBLE
                binding.tvEmergencyZone.text = "⚠️ ${sosList.size} emergency zone(s) detected"
            } else {
                binding.tvEmergencyZone.visibility = View.GONE
            }

            Log.d(TAG, "Updated map with ${sosList.size} SOS markers from ${messagesWithLocation.size} messages")
        }

        // Observe mesh statistics
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvMeshInfo.text = if (stats.isActive) {
                "🟢 Mesh active • ${stats.connectedCount} devices • Location sharing enabled"
            } else {
                "🔴 Mesh offline • Searching for connections..."
            }
        }

        // Observe download progress
        viewLifecycleOwner.lifecycleScope.launch {
            mapDownloader.progress.collectLatest { progress ->
                updateDownloadProgress(progress)
            }
        }
    }

    private fun updateSosMarkers(sosMessages: List<Message>) {
        // Remove old markers
        sosMarkers.forEach { mapView.overlays.remove(it) }
        sosMarkers.clear()

        // Get current device ID to filter out own SOS
        val myDeviceId = com.meshcomm.utils.PrefsHelper.getUserId(requireContext())

        // Filter out self-SOS messages
        val otherSosMessages = sosMessages.filter { it.deviceId != myDeviceId }

        Log.d(TAG, "Updating markers: ${sosMessages.size} total SOS, ${otherSosMessages.size} from other devices")

        // Add new SOS markers
        otherSosMessages.forEach { sos ->
            // Extract address from SOS message if available
            // Format: "SOS|lat,lng|address|timestamp|deviceId|message"
            val address = try {
                val parts = sos.content.split("|")
                if (parts.size >= 3) parts[2] else extractAddressFromCoords(sos)
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse address from SOS content: ${e.message}")
                extractAddressFromCoords(sos)
            }

            val marker = Marker(mapView).apply {
                position = GeoPoint(sos.latitude, sos.longitude)
                title = "🚨 SOS Alert"
                snippet = "From: ${sos.senderName}\n📍 $address\n⏰ ${formatTimestamp(sos.timestamp)}\n🔋 ${sos.batteryLevel}%"

                // Show info window on tap
                setOnMarkerClickListener { clickedMarker, _ ->
                    clickedMarker.showInfoWindow()
                    val fullMessage = extractMessageContent(sos.content)
                    Toast.makeText(
                        requireContext(),
                        "SOS from ${sos.senderName}: $fullMessage",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "SOS marker tapped: ${sos.senderName} from $address")
                    true
                }
            }

            sosMarkers.add(marker)
            mapView.overlays.add(marker)
            Log.d(TAG, "Added SOS marker: ${sos.senderName} at $address (${sos.latitude},${sos.longitude})")
        }

        mapView.invalidate()
        Log.d(TAG, "Updated map with ${sosMarkers.size} SOS markers (filtered ${sosMessages.size - otherSosMessages.size} self-SOS)")
    }

    private fun extractAddressFromCoords(sos: Message): String {
        Log.d(TAG, "Extracting address from coordinates: (${sos.latitude}, ${sos.longitude})")
        return if (sos.latitude != 0.0 && sos.longitude != 0.0) {
            val address = com.meshcomm.utils.GeocoderUtil.getShortAddress(requireContext(), sos.latitude, sos.longitude)
            Log.d(TAG, "Address conversion successful: $address")
            address
        } else {
            Log.w(TAG, "Address conversion failed: Invalid coordinates (${sos.latitude}, ${sos.longitude})")
            "Location unavailable"
        }
    }

    private fun extractMessageContent(sosContent: String): String {
        Log.d(TAG, "Parsing SOS content: ${sosContent.take(50)}...")
        return try {
            val parts = sosContent.split("|")
            val message = if (parts.size >= 6) parts[5] else sosContent
            Log.d(TAG, "Extracted SOS message: $message")
            message
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SOS content: ${e.message}")
            sosContent
        }
    }

    private fun centerOnCurrentLocation() {
        val location = locationProvider.getLastLatLon()
        if (location.first != 0.0 && location.second != 0.0) {
            mapView.controller.animateTo(
                GeoPoint(location.first, location.second),
                14.0,
                1000L
            )
            Log.d(TAG, "Centered on location: ${location.first}, ${location.second}")
        } else {
            Toast.makeText(
                requireContext(),
                "Waiting for GPS location...",
                Toast.LENGTH_SHORT
            ).show()
            Log.w(TAG, "No GPS location available yet")
        }
    }

    private fun startMapDownload() {
        if (!hasLocationPermission()) {
            Toast.makeText(
                requireContext(),
                "Storage permission required to download maps",
                Toast.LENGTH_LONG
            ).show()
            Log.w(TAG, "Cannot download: missing permissions")
            return
        }

        binding.downloadProgressCard.visibility = View.VISIBLE
        binding.btnDownloadMap.isEnabled = false

        mapDownloader.startDownload(viewLifecycleOwner.lifecycleScope)

        Toast.makeText(
            requireContext(),
            "Starting download of Telangana map tiles...",
            Toast.LENGTH_LONG
        ).show()

        Log.i(TAG, "Map download started")
    }

    private fun updateDownloadProgress(progress: TelanganaMapDownloader.DownloadProgress) {
        when {
            progress.isComplete -> {
                binding.downloadProgressCard.visibility = View.GONE
                binding.btnDownloadMap.isEnabled = true
                binding.btnDownloadMap.text = "Map Downloaded ✓"
                Toast.makeText(
                    requireContext(),
                    "Map download complete! ${progress.current} tiles downloaded",
                    Toast.LENGTH_LONG
                ).show()
                Log.i(TAG, "Map download completed: ${progress.current} tiles")
            }
            progress.errorMessage != null -> {
                binding.downloadProgressCard.visibility = View.GONE
                binding.btnDownloadMap.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Download failed: ${progress.errorMessage}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Map download failed: ${progress.errorMessage}")
            }
            else -> {
                binding.tvDownloadStatus.text = "Downloading map tiles..."
                binding.tvDownloadProgress.text = "${progress.percentage}%"
                binding.progressBar.progress = progress.percentage
                Log.d(TAG, "Download progress: ${progress.current}/${progress.total} (${progress.percentage}%)")
            }
        }
    }

    private fun updateDownloadButtonState() {
        if (offlineTileProvider.hasOfflineTiles()) {
            val sizeMB = offlineTileProvider.getOfflineTileSize() / (1024 * 1024)
            binding.btnDownloadMap.text = "Map Downloaded ($sizeMB MB)"
            Log.d(TAG, "Offline tiles exist: $sizeMB MB")
        } else {
            binding.btnDownloadMap.text = "Download Telangana Map"
            Log.d(TAG, "No offline tiles found")
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        locationProvider.startUpdates()
        Log.d(TAG, "MapFragment resumed")
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationProvider.stopUpdates()
        Log.d(TAG, "MapFragment paused")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationProvider.stopUpdates()
        mapDownloader.cancelDownload()
        _binding = null
        Log.d(TAG, "MapFragment destroyed")
    }
}
