package com.meshcomm.ui.home

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MeshStats
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.mesh.MeshService
import com.meshcomm.mesh.PeerRegistry
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MeshViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MessageRepository(AppDatabase.get(application))

    private val _meshStats = MutableLiveData(MeshStats(0, 0, false))
    val meshStats: LiveData<MeshStats> = _meshStats

    private val _newMessage = MutableLiveData<Message?>()
    val newMessage: LiveData<Message?> = _newMessage

    val broadcastMessages = repo.getBroadcastMessages()
    val sosMessages = repo.getSOSMessages()
    val allMessages = repo.getAllMessages()
    
    // Expose connected peers as Flow
    val connectedPeers: Flow<List<PeerDevice>> = PeerRegistry.peerFlow

    var meshService: MeshService? = null
        private set

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            meshService = (service as MeshService.MeshBinder).getService()
            _meshStats.value = MeshStats(
                connectedCount = meshService!!.getConnectedPeerCount(),
                relayedCount = 0,
                isActive = true
            )
            observeRouter()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            meshService = null
            _meshStats.value = MeshStats(0, 0, false)
        }
    }

    private fun observeRouter() {
        viewModelScope.launch {
            meshService?.messageRouter?.incomingMessages?.collect { msg ->
                _newMessage.postValue(msg)
            }
        }
        viewModelScope.launch {
            PeerRegistry.peerFlow.collectLatest { peers ->
                _meshStats.postValue(MeshStats(peers.size, 0, true))
            }
        }
    }

    fun bindService(context: Context) {
        MeshService.start(context)
        val intent = Intent(context, MeshService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        try { context.unbindService(serviceConnection) } catch (e: Exception) {}
    }

    fun sendBroadcast(content: String, includeLocation: Boolean) {
        meshService?.sendBroadcastMessage(content, includeLocation)
    }

    fun sendDirect(targetId: String, targetName: String, content: String, includeLocation: Boolean) {
        meshService?.sendDirectMessage(targetId, targetName, content, includeLocation)
    }

    fun sendSOS(message: String = "EMERGENCY! I need help!") {
        meshService?.sendSOS(message)
    }

    fun sendAudio(filePath: String, duration: Long, targetId: String? = null) {
        val deviceId = PrefsHelper.getUserId(getApplication())
        val msg = Message(
            senderId = deviceId,
            senderName = PrefsHelper.getUserName(getApplication()),
            targetId = targetId,
            type = MessageType.AUDIO,
            content = "Audio message",
            mediaUri = filePath,
            mediaDuration = duration,
            batteryLevel = BatteryHelper.getLevel(getApplication()),
            deviceId = deviceId,
            mediaType = "audio/m4a"
        )
        meshService?.messageRouter?.sendMessage(msg)
    }

    fun sendImage(filePath: String, targetId: String? = null) {
        val deviceId = PrefsHelper.getUserId(getApplication())
        val msg = Message(
            senderId = deviceId,
            senderName = PrefsHelper.getUserName(getApplication()),
            targetId = targetId,
            type = MessageType.IMAGE,
            content = "Image message",
            mediaUri = filePath,
            batteryLevel = BatteryHelper.getLevel(getApplication()),
            deviceId = deviceId,
            mediaType = "image/jpeg"
        )
        meshService?.messageRouter?.sendMessage(msg)
    }
}
