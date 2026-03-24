package com.meshcomm.ui.broadcast

import android.content.Context
import android.graphics.BitmapFactory
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meshcomm.R
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.utils.MediaHelper
import com.meshcomm.utils.PrefsHelper
import java.io.File
import java.util.Date

class MessageAdapter(private val context: Context) :
    ListAdapter<Message, MessageAdapter.MessageViewHolder>(DiffCallback) {

    private val selfId = PrefsHelper.getUserId(context)

    companion object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(a: Message, b: Message) = a.messageId == b.messageId
        override fun areContentsTheSame(a: Message, b: Message) = a == b
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardMessage)
        private val tvSender: TextView = itemView.findViewById(R.id.tvSender)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvSosLabel: TextView = itemView.findViewById(R.id.tvSosLabel)
        
        // Media views
        private val layoutAudio: LinearLayout = itemView.findViewById(R.id.layoutAudio)
        private val btnPlayAudio: ImageButton = itemView.findViewById(R.id.btnPlayAudio)
        private val tvAudioDuration: TextView = itemView.findViewById(R.id.tvAudioDuration)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)

        fun bind(msg: Message) {
            val isSelf = msg.senderId == selfId
            val isSOS  = msg.type == MessageType.SOS

            tvSender.text = if (isSelf) "You" else "${msg.senderName} [${msg.senderId.take(6)}]"
            
            // Text content
            if (msg.type == MessageType.AUDIO || msg.type == MessageType.IMAGE) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = try {
                    com.meshcomm.crypto.EncryptionUtil.decrypt(msg.content)
                } catch (e: Exception) {
                    msg.content
                }
            }

            tvTime.text = DateFormat.format("HH:mm", Date(msg.timestamp))

            // SOS styling
            tvSosLabel.visibility = if (isSOS) View.VISIBLE else View.GONE
            card.setCardBackgroundColor(
                context.getColor(
                    when {
                        isSOS   -> R.color.sos_bg
                        isSelf  -> R.color.self_msg_bg
                        else    -> R.color.other_msg_bg
                    }
                )
            )

            // Audio handling
            if (msg.type == MessageType.AUDIO && msg.mediaUri != null) {
                layoutAudio.visibility = View.VISIBLE
                tvAudioDuration.text = formatDuration(msg.mediaDuration)
                btnPlayAudio.setOnClickListener {
                    MediaHelper.playAudio(msg.mediaUri) {
                        btnPlayAudio.setImageResource(android.R.drawable.ic_media_play)
                    }
                    btnPlayAudio.setImageResource(android.R.drawable.ic_media_pause)
                }
            } else {
                layoutAudio.visibility = View.GONE
            }

            // Image handling
            if (msg.type == MessageType.IMAGE && msg.mediaUri != null) {
                ivImage.visibility = View.VISIBLE
                val file = File(msg.mediaUri)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ivImage.setImageBitmap(bitmap)
                }
            } else {
                ivImage.visibility = View.GONE
            }

            // Location
            if (msg.latitude != 0.0 && msg.longitude != 0.0) {
                tvLocation.visibility = View.VISIBLE
                tvLocation.text = "📍 %.4f, %.4f".format(msg.latitude, msg.longitude)
            } else {
                tvLocation.visibility = View.GONE
            }

            // Battery info
            tvStatus.text = "🔋${msg.batteryLevel}%  ${msg.status.name.lowercase()}"
        }
        
        private fun formatDuration(durationMs: Long): String {
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
