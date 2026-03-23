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

        checkPermissionsAndSetup()
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
            maxZoomLevel = 15.0

            // Set initial view to Telangana
            controller.setZoom(8.0)
            controller.setCenter(telanganaCenter)
        }

        // Setup location overlay
        setupMyLocationOverlay()

        // Update download button state
        updateDownloadButtonState()

        binding.mapView.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        Log.d(TAG, "Map initialized and centered on Telangana")
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

        // Floating SOS button
        binding.fabSos.setOnClickListener {
            findNavController().navigate(R.id.nav_sos)
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

        // Add new SOS markers
        sosMessages.forEach { sos ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(sos.latitude, sos.longitude)
                title = "🚨 ${sos.senderName}"
                snippet = "${sos.content}\n🔋 ${sos.batteryLevel}% • ${formatTimestamp(sos.timestamp)}"

                // Show info window on tap
                setOnMarkerClickListener { clickedMarker, _ ->
                    clickedMarker.showInfoWindow()
                    Toast.makeText(
                        requireContext(),
                        "SOS from ${sos.senderName}: ${sos.content}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "SOS marker tapped: ${sos.senderName}")
                    true
                }
            }

            sosMarkers.add(marker)
            mapView.overlays.add(marker)
        }

        mapView.invalidate()
        Log.d(TAG, "Added ${sosMarkers.size} SOS markers to map")
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
