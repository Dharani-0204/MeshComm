package com.meshcomm.ui.broadcast

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meshcomm.R
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.ui.navigation.NavigationActivity
import com.meshcomm.utils.PrefsHelper
import java.util.Date
import java.util.regex.Pattern

class MessageAdapter(private val context: Context) :
    ListAdapter<Message, MessageAdapter.MessageViewHolder>(DiffCallback) {

    private val selfId = PrefsHelper.getUserId(context)

    companion object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(a: Message, b: Message) = a.messageId == b.messageId
        override fun areContentsTheSame(a: Message, b: Message) = a == b

        // Regex patterns for coordinate detection
        private val COORDINATE_PATTERNS = listOf(
            // Pattern 1: "lat, lon" or "latitude, longitude" (decimal degrees)
            Pattern.compile(
                "(?:lat(?:itude)?[:\\s]*)?(-?\\d{1,2}\\.\\d{4,})\\s*,\\s*(?:lon(?:gitude)?[:\\s]*)?(-?\\d{1,3}\\.\\d{4,})",
                Pattern.CASE_INSENSITIVE
            ),
            // Pattern 2: "Location: lat, lon"
            Pattern.compile(
                "location[:\\s]+(-?\\d{1,2}\\.\\d{4,})\\s*,\\s*(-?\\d{1,3}\\.\\d{4,})",
                Pattern.CASE_INSENSITIVE
            ),
            // Pattern 3: Just two decimal numbers that look like coordinates
            Pattern.compile(
                "\\b(-?[1-8]?\\d\\.\\d{4,})\\s*,\\s*(-?1?\\d{2}\\.\\d{4,})\\b"
            )
        )

        fun detectCoordinates(text: String): List<CoordinateMatch> {
            val matches = mutableListOf<CoordinateMatch>()

            for (pattern in COORDINATE_PATTERNS) {
                val matcher = pattern.matcher(text)
                while (matcher.find()) {
                    try {
                        val lat = matcher.group(1)?.toDoubleOrNull()
                        val lon = matcher.group(2)?.toDoubleOrNull()

                        if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                            matches.add(
                                CoordinateMatch(
                                    latitude = lat,
                                    longitude = lon,
                                    startIndex = matcher.start(),
                                    endIndex = matcher.end(),
                                    fullMatch = matcher.group(0)
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip invalid matches
                    }
                }
            }

            return matches.distinctBy { "${it.latitude},${it.longitude}" }
        }

        private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
            return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0
        }
    }

    data class CoordinateMatch(
        val latitude: Double,
        val longitude: Double,
        val startIndex: Int,
        val endIndex: Int,
        val fullMatch: String
    )

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

        fun bind(msg: Message) {
            val isSelf = msg.senderId == selfId
            val isSOS  = msg.type == MessageType.SOS

            tvSender.text = if (isSelf) "You" else "${msg.senderName} [${msg.senderId.take(6)}]"

            // Decrypt content and make coordinates clickable
            val messageContent = try {
                com.meshcomm.crypto.EncryptionUtil.decrypt(msg.content)
            } catch (e: Exception) {
                msg.content
            }

            // Apply coordinate detection and make them clickable
            makeTextWithClickableCoordinates(tvContent, messageContent)

            tvTime.text = DateFormat.format("HH:mm", Date(msg.timestamp))
            tvStatus.text = msg.status.name.lowercase().replaceFirstChar { it.uppercase() }

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

            // Location - make it clickable if available
            if (msg.latitude != 0.0 && msg.longitude != 0.0) {
                tvLocation.visibility = View.VISIBLE
                val locationText = "📍 %.4f, %.4f".format(msg.latitude, msg.longitude)

                // Make location coordinates clickable
                val spannable = SpannableString(locationText)
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        navigateToCoordinate(msg.latitude, msg.longitude, "Message Location")
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = context.getColor(R.color.md_theme_primary)
                        ds.isUnderlineText = true
                    }
                }

                val coordinateStart = locationText.indexOf(msg.latitude.toString().substring(0, 6))
                if (coordinateStart >= 0) {
                    spannable.setSpan(
                        clickableSpan,
                        coordinateStart,
                        locationText.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        ForegroundColorSpan(context.getColor(R.color.md_theme_primary)),
                        coordinateStart,
                        locationText.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                tvLocation.text = spannable
                tvLocation.movementMethod = LinkMovementMethod.getInstance()
            } else {
                tvLocation.visibility = View.GONE
            }

            // Battery info
            tvStatus.text = "🔋${msg.batteryLevel}%  ${msg.status.name.lowercase()}"
        }

        private fun makeTextWithClickableCoordinates(textView: TextView, text: String) {
            val coordinates = detectCoordinates(text)

            if (coordinates.isEmpty()) {
                // No coordinates found, just set the text normally
                textView.text = text
                textView.movementMethod = null
                return
            }

            // Create spannable with clickable coordinates
            val spannable = SpannableString(text)

            for (coord in coordinates.sortedByDescending { it.startIndex }) {
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        navigateToCoordinate(
                            coord.latitude,
                            coord.longitude,
                            "Location from message"
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = context.getColor(R.color.md_theme_primary)
                        ds.isUnderlineText = true
                        ds.isFakeBoldText = true
                    }
                }

                spannable.setSpan(
                    clickableSpan,
                    coord.startIndex,
                    coord.endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Add visual styling
                spannable.setSpan(
                    ForegroundColorSpan(context.getColor(R.color.md_theme_primary)),
                    coord.startIndex,
                    coord.endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    UnderlineSpan(),
                    coord.startIndex,
                    coord.endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            textView.text = spannable
            textView.movementMethod = LinkMovementMethod.getInstance()
        }

        private fun navigateToCoordinate(latitude: Double, longitude: Double, name: String) {
            try {
                // Validate coordinates first
                if (latitude == 0.0 && longitude == 0.0) {
                    Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show()
                    return
                }

                // Ensure we're on UI thread
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    Handler(Looper.getMainLooper()).post {
                        navigateToCoordinate(latitude, longitude, name)
                    }
                    return
                }

                Toast.makeText(
                    context,
                    "Navigating to: $name",
                    Toast.LENGTH_SHORT
                ).show()

                NavigationActivity.start(
                    context,
                    latitude,
                    longitude,
                    name
                )
            } catch (e: Exception) {
                Log.e("MessageAdapter", "Navigation failed", e)
                Toast.makeText(
                    context,
                    "Navigation failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
