package com.meshcomm.ui.map

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meshcomm.R
import com.meshcomm.data.model.Message
import java.util.Date

class SosListAdapter : ListAdapter<Message, SosListAdapter.VH>(DiffCB) {

    companion object DiffCB : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(a: Message, b: Message) = a.messageId == b.messageId
        override fun areContentsTheSame(a: Message, b: Message) = a == b
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sos_pin, parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvName: TextView = v.findViewById(R.id.tvSosName)
        private val tvCoords: TextView = v.findViewById(R.id.tvSosCoords)
        private val tvMsg: TextView = v.findViewById(R.id.tvSosMsg)
        private val tvTime: TextView = v.findViewById(R.id.tvSosTime)
        private val tvBattery: TextView = v.findViewById(R.id.tvSosBattery)

        fun bind(m: Message) {
            tvName.text = "🚨 ${m.senderName}"
            tvCoords.text = "📍 ${"%.5f".format(m.latitude)}, ${"%.5f".format(m.longitude)}"
            tvMsg.text = m.content
            tvTime.text = DateFormat.format("dd/MM HH:mm", Date(m.timestamp))
            tvBattery.text = "🔋 ${m.batteryLevel}%  |  ${m.nearbyDevicesCount} nearby"
        }
    }
}
