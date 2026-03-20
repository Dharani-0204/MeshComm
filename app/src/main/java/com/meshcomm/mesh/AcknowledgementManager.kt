package com.meshcomm.mesh

import android.content.Context
import com.google.gson.Gson
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper

/**
 * Handles sending and receiving ACK / "I'm coming" replies to SOS messages.
 */
class AcknowledgementManager(
    private val context: Context,
    private val router: MessageRouter
) {
    private val gson = Gson()

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
            ttl = 7
        )
        router.sendMessage(ackMsg)
    }

    fun isAckMessage(message: Message): Boolean =
        message.content.contains("[ACK:")

    fun extractOriginalId(message: Message): String? {
        val regex = Regex("""\[ACK:([^\]]+)\]""")
        return regex.find(message.content)?.groupValues?.getOrNull(1)
    }
}
