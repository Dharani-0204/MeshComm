package com.meshcomm.mesh

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Handles sending and receiving ACK / "I'm coming" replies to SOS messages.
 * Also handles CHUNK level ACKs for reliable media transfer.
 */
class AcknowledgementManager(
    private val context: Context,
    private val routerProvider: () -> MessageRouter?
) {
    companion object {
        private const val TAG = "AcknowledgementManager"
    }

    private val _chunkAcks = MutableSharedFlow<Message>()
    val chunkAcks: SharedFlow<Message> = _chunkAcks

    enum class AckType { ACK, IM_COMING, HELP_ON_WAY }

    fun sendAck(originalMessageId: String, targetId: String, ackType: AckType = AckType.ACK) {
        val content = when (ackType) {
            AckType.ACK        -> "✅ Message received [ACK:$originalMessageId]"
            AckType.IM_COMING  -> "🚑 I'm coming! [ACK:$originalMessageId]"
            AckType.HELP_ON_WAY -> "🚒 Help is on the way! [ACK:$originalMessageId]"
        }
        val ackMsg = Message(
            senderId = PrefsHelper.getUserId(context),
            senderName = PrefsHelper.getUserName(context),
            targetId = targetId,
            type = MessageType.DIRECT,
            content = content,
            batteryLevel = BatteryHelper.getLevel(context),
            ttl = 7,
            deviceId = PrefsHelper.getUserId(context)
        )
        routerProvider()?.sendMessage(ackMsg)
    }

    suspend fun onChunkAckReceived(msg: Message) {
        Log.d(TAG, "Chunk ACK received for msg: ${msg.originalMessageId}, index: ${msg.chunkIndex}")
        _chunkAcks.emit(msg)
    }

    fun isAckMessage(message: Message): Boolean =
        message.content.contains("[ACK:")

    fun extractOriginalId(message: Message): String? {
        val regex = Regex("""\[ACK:([^\]]+)\]""")
        return regex.find(message.content)?.groupValues?.getOrNull(1)
    }
}
