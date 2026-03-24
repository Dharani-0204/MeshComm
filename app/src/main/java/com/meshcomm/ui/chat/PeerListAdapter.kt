package com.meshcomm.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.UserRole
import com.meshcomm.databinding.ItemPeerBinding

class PeerListAdapter(
    private val onPeerClick: (PeerDevice) -> Unit
) : ListAdapter<PeerDevice, PeerListAdapter.PeerViewHolder>(PeerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val binding = ItemPeerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PeerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PeerViewHolder(
        private val binding: ItemPeerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(peer: PeerDevice) {
            binding.root.setOnClickListener {
                onPeerClick(peer)
            }

            binding.apply {
                tvPeerName.text = peer.deviceName
                tvPeerId.text = "ID: ${peer.deviceId.take(8)}"
                
                // Show role with appropriate icon
                val roleText = when (peer.role) {
                    UserRole.CIVILIAN -> "👤 Civilian"
                    UserRole.RESCUER -> "🚑 Rescuer"
                    UserRole.AUTHORITY -> "👮 Authority"
                }
                tvPeerRole.text = roleText
                
                // Connection info
                tvConnectionInfo.text = "${peer.transport.name} • ${peer.rssi} dBm"
                
                // Battery level
                chipBattery.text = "🔋 ${peer.batteryLevel}%"
                
                // Connection status
                chipStatus.text = if (peer.isConnected) "🟢 Connected" else "🔴 Disconnected"
            }
        }
    }

    private class PeerDiffCallback : DiffUtil.ItemCallback<PeerDevice>() {
        override fun areItemsTheSame(oldItem: PeerDevice, newItem: PeerDevice): Boolean {
            return oldItem.deviceId == newItem.deviceId
        }

        override fun areContentsTheSame(oldItem: PeerDevice, newItem: PeerDevice): Boolean {
            return oldItem == newItem
        }
    }
}