package com.meshcomm.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.meshcomm.R
import com.meshcomm.databinding.FragmentMapBinding
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.UserRole
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
import java.util.concurrent.ConcurrentHashMap

class MapFragment : Fragment() {

    companion object {
        private const val TAG = "MapFragment"
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeshViewModel by activityViewModels()

    private lateinit var mapView: MapView
    private lateinit var locationProvider: LocationProvider

    private var myLocationOverlay: MyLocationNewOverlay? = null
    
    // Track markers by deviceId to avoid duplication and allow updates
    private val peerMarkers = ConcurrentHashMap<String, Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saved: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupClickListeners()
        observePeers()
    }

    private fun setupMap() {
        mapView = binding.mapView
        mapView.apply {
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 20.0
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(17.3850, 78.4867)) // Default center (e.g., Hyderabad)
        }

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView).apply {
            enableMyLocation()
        }
        mapView.overlays.add(myLocationOverlay)
    }

    private fun setupClickListeners() {
        binding.fabCenterLocation.setOnClickListener {
            val location = myLocationOverlay?.myLocation
            if (location != null) {
                mapView.controller.animateTo(location)
            }
        }
        
        binding.fabSos.setOnClickListener {
            findNavController().navigate(R.id.action_map_to_sos)
        }
    }

    private fun observePeers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectedPeers.collectLatest { peers ->
                updateMarkers(peers)
            }
        }
    }

    private fun updateMarkers(peers: List<PeerDevice>) {
        val currentPeerIds = peers.map { it.deviceId }.toSet()
        
        // 1. Remove markers for peers no longer connected
        val iterator = peerMarkers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentPeerIds.contains(entry.key)) {
                mapView.overlays.remove(entry.value)
                iterator.remove()
            }
        }

        // 2. Add or Update markers
        peers.forEach { peer ->
            if (peer.latitude == 0.0 && peer.longitude == 0.0) return@forEach

            val marker = peerMarkers.getOrPut(peer.deviceId) {
                Marker(mapView).apply {
                    id = peer.deviceId
                    mapView.overlays.add(this)
                }
            }

            marker.position = GeoPoint(peer.latitude, peer.longitude)
            marker.title = "${peer.deviceName} (${peer.role})"
            marker.snippet = "Battery: ${peer.batteryLevel}% | Transport: ${peer.transport}"
            
            // ISSUE 5: Map Marker Differentiation
            val markerColor = when {
                peer.isSosActive -> Color.RED
                peer.role == UserRole.RESCUER -> Color.CYAN
                else -> Color.YELLOW
            }
            
            // Note: In real osmdroid, you'd set an icon. Using default with color filter for brevity
            marker.icon.setTint(markerColor)
            
            if (peer.isSosActive) {
                marker.title = "🚨 SOS: ${peer.deviceName}"
                marker.showInfoWindow()
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
