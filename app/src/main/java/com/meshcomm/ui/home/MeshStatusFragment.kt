package com.meshcomm.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.meshcomm.R
import com.meshcomm.mesh.PeerRegistry
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper

class MeshStatusFragment : Fragment() {

    private val viewModel: MeshViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshStats()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View? =
        inflater.inflate(R.layout.fragment_mesh_status, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.meshStats.observe(viewLifecycleOwner) { stats ->
            view.findViewById<TextView>(R.id.tvConnectedCount).text =
                "${stats.connectedCount} device(s) connected"
            view.findViewById<TextView>(R.id.tvMeshActive).text =
                if (stats.isActive) "✅ Mesh ACTIVE" else "⏸ Mesh INACTIVE"
        }
        handler.post(refreshRunnable)
    }

    private fun refreshStats() {
        val peers = PeerRegistry.getConnectedPeers()
        val sb = StringBuilder()
        if (peers.isEmpty()) {
            sb.appendLine("No peers connected.\nScanning via Bluetooth + WiFi Direct…")
        } else {
            peers.forEachIndexed { i, peer ->
                sb.appendLine("${i + 1}. ${peer.deviceName}  [${peer.transport.name}]  🔋${peer.batteryLevel}%")
            }
        }
        view?.findViewById<TextView>(R.id.tvPeerList)?.text = sb.toString()

        val battery = BatteryHelper.getLevel(requireContext())
        view?.findViewById<TextView>(R.id.tvMyBattery)?.text =
            "My battery: $battery%  |  Relay: ${if (battery > 30) "ON" else "OFF (low battery)"}"

        val saf = viewModel.meshService?.storeAndForward
        view?.findViewById<TextView>(R.id.tvPendingQueue)?.text =
            "Queued messages (store-and-forward): ${saf?.getPendingCount() ?: 0}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(refreshRunnable)
    }
}
