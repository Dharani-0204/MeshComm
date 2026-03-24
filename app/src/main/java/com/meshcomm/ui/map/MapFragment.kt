package com.meshcomm.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * MapFragment: Displays offline OSMDroid maps with SOS markers.
 * Fixed: MapView lifecycle handling and lateinit property initialization.
 */
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

    // FIX: Using nullable instead of lateinit to prevent UninitializedPropertyAccessException
    private var mapView: MapView? = null
    
    private var offlineTileProvider: OfflineTileProvider? = null
    private var mapDownloader: TelanganaMapDownloader? = null
    private var locationProvider: LocationProvider? = null

    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val sosMarkers = mutableListOf<Marker>()

    private val telanganaCenter = GeoPoint(17.5, 79.5)

    private var savedCameraLat: Double? = null
    private var savedCameraLon: Double? = null
    private var savedCameraZoom: Double? = null

    // Permission launcher for required map permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            setupMap()
            setupLocationTracking()
        } else {
            showEmptyState()
            Toast.makeText(requireContext(), "Permissions required for maps", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        }

        checkPermissionsAndSetup()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // FIX: Safe check for mapView
        mapView?.let {
            val center = it.mapCenter
            outState.putDouble(KEY_CAMERA_LAT, center.latitude)
            outState.putDouble(KEY_CAMERA_LON, center.longitude)
            outState.putDouble(KEY_CAMERA_ZOOM, it.zoomLevelDouble)
        }
    }

    private fun checkPermissionsAndSetup() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            setupMap()
            setupClickListeners()
            observeData()
            setupLocationTracking()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun showEmptyState() {
        binding.mapView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun setupMap() {
        // FIX: Ensure mapView is assigned from binding before use
        mapView = binding.mapView
        
        offlineTileProvider = OfflineTileProvider()
        mapDownloader = TelanganaMapDownloader()

        mapView?.apply {
            setTileSource(offlineTileProvider?.getTileSource())
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0

            if (savedCameraLat != null && savedCameraLon != null && savedCameraZoom != null) {
                controller.setZoom(savedCameraZoom!!)
                controller.setCenter(GeoPoint(savedCameraLat!!, savedCameraLon!!))
            } else {
                controller.setZoom(8.0)
                controller.setCenter(telanganaCenter)
            }
        }

        setupMyLocationOverlay()
        updateDownloadButtonState()

        binding.mapView.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    private fun setupMyLocationOverlay() {
        val map = mapView ?: return
        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            map
        ).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        map.overlays.add(myLocationOverlay)
    }

    private fun setupClickListeners() {
        binding.btnDownloadMap.setOnClickListener {
            startMapDownload()
        }

        binding.fabCenterLocation.setOnClickListener {
            centerOnCurrentLocation()
        }

        binding.fabSos.setOnClickListener {
            findNavController().navigate(R.id.action_map_to_sos)
        }
    }

    private fun setupLocationTracking() {
        locationProvider = LocationProvider(requireContext())
        locationProvider?.startUpdates()

        viewLifecycleOwner.lifecycleScope.launch {
            locationProvider?.location?.collectLatest { location ->
                // Handle location
            }
        }
    }

    private fun observeData() {
        viewModel.allMessages.asLiveData().observe(viewLifecycleOwner) { allMessages ->
            val messagesWithLocation = allMessages.filter {
                it.latitude != 0.0 && it.longitude != 0.0
            }
            val sosList = messagesWithLocation.filter { it.type == MessageType.SOS }
            updateSosMarkers(sosList)
        }

        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            binding.tvMeshInfo.text = if (stats.isActive) {
                "🟢 Mesh active • ${stats.connectedCount} devices"
            } else {
                "🔴 Mesh offline"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            mapDownloader?.progress?.collectLatest { progress ->
                updateDownloadProgress(progress)
            }
        }
    }

    private fun updateSosMarkers(sosMessages: List<Message>) {
        val map = mapView ?: return
        sosMarkers.forEach { map.overlays.remove(it) }
        sosMarkers.clear()

        val myDeviceId = PrefsHelper.getUserId(requireContext())
        val otherSosMessages = sosMessages.filter { it.deviceId != myDeviceId }

        otherSosMessages.forEach { sos ->
            val marker = Marker(map).apply {
                position = GeoPoint(sos.latitude, sos.longitude)
                title = "🚨 SOS Alert"
                snippet = "From: ${sos.senderName}"
                setOnMarkerClickListener { clickedMarker, _ ->
                    clickedMarker.showInfoWindow()
                    true
                }
            }
            sosMarkers.add(marker)
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun centerOnCurrentLocation() {
        val location = locationProvider?.getLastLatLon() ?: Pair(0.0, 0.0)
        if (location.first != 0.0 && location.second != 0.0) {
            mapView?.controller?.animateTo(
                GeoPoint(location.first, location.second),
                14.0,
                1000L
            )
        } else {
            Toast.makeText(requireContext(), "Waiting for GPS...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMapDownload() {
        binding.downloadProgressCard.visibility = View.VISIBLE
        binding.btnDownloadMap.isEnabled = false
        mapDownloader?.startDownload(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateDownloadProgress(progress: TelanganaMapDownloader.DownloadProgress) {
        if (progress.isComplete) {
            binding.downloadProgressCard.visibility = View.GONE
            binding.btnDownloadMap.isEnabled = true
        } else if (progress.errorMessage != null) {
            binding.downloadProgressCard.visibility = View.GONE
            binding.btnDownloadMap.isEnabled = true
        } else {
            binding.progressBar.progress = progress.percentage
        }
    }

    private fun updateDownloadButtonState() {
        if (offlineTileProvider?.hasOfflineTiles() == true) {
            binding.btnDownloadMap.text = "Map Downloaded"
        } else {
            binding.btnDownloadMap.text = "Download Telangana Map"
        }
    }

    override fun onResume() {
        super.onResume()
        // FIX: Safe property access
        mapView?.onResume()
        locationProvider?.startUpdates()
    }

    override fun onPause() {
        super.onPause()
        // FIX: Safe property access
        mapView?.onPause()
        locationProvider?.stopUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationProvider?.stopUpdates()
        mapDownloader?.cancelDownload()
        // FIX: MapView cleanup
        mapView?.onDetach()
        mapView = null
        _binding = null
    }
}
