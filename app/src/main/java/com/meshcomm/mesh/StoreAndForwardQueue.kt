package com.meshcomm.mesh

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meshcomm.data.model.Message
import kotlinx.coroutines.*

/**
 * Stores outbound messages when no peers are connected.
 * Flushes them automatically when a peer connects.
 */
class StoreAndForwardQueue(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("saf_queue", Context.MODE_PRIVATE)
    private val QUEUE_KEY = "pending_messages"

    /** Call this when no peers available — queues the message for later delivery */
    fun enqueue(message: Message) {
        val queue = loadQueue().toMutableList()
        queue.add(message)
        // Cap at 50 messages to avoid unbounded growth
        val trimmed = if (queue.size > 50) queue.takeLast(50) else queue
        saveQueue(trimmed)
    }

    /** Call this when a new peer connects — flushes all queued messages */
    fun flushToAllPeers() {
        scope.launch {
            val queue = loadQueue()
            if (queue.isEmpty()) return@launch
            queue.forEach { msg ->
                val json = gson.toJson(msg) + "\n"
                transportLayer.sendToAll(json)
                // Small delay to avoid flooding
                delay(100)
            }
            clearQueue()
        }
    }

    /** Call this when a specific peer connects */
    fun flushToPeer(peerId: String) {
        scope.launch {
            val queue = loadQueue()
            if (queue.isEmpty()) return@launch
            queue.forEach { msg ->
                val json = gson.toJson(msg) + "\n"
                transportLayer.sendToPeer(peerId, json)
                delay(100)
            }
            clearQueue()
        }
    }

    fun getPendingCount(): Int = loadQueue().size

    fun clearQueue() {
        prefs.edit().remove(QUEUE_KEY).apply()
    }

    private fun loadQueue(): List<Message> {
        val json = prefs.getString(QUEUE_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Message>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveQueue(queue: List<Message>) {
        prefs.edit().putString(QUEUE_KEY, gson.toJson(queue)).apply()
    }
}
