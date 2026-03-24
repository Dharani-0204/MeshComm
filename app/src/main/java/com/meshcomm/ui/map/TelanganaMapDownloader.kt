package com.meshcomm.ui.map

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads OpenStreetMap tiles for Telangana region
 * Bounding box: 15.85°N to 19.92°N, 77.23°E to 81.97°E
 * Zoom levels: 5-15
 */
class TelanganaMapDownloader(
    private val offlineTilePath: String = "/storage/emulated/0/tiles"
) {
    companion object {
        private const val TAG = "TelanganaDownloader"

        // Telangana bounding box
        private const val MIN_LAT = 15.85
        private const val MAX_LAT = 19.92
        private const val MIN_LON = 77.23
        private const val MAX_LON = 81.97

        // Zoom range
        private const val MIN_ZOOM = 5
        private const val MAX_ZOOM = 18  // Increased to 18 for street-level detail (buildings, street names)

        // OSM tile servers
        private val TILE_SERVERS = listOf(
            "https://a.tile.openstreetmap.org",
            "https://b.tile.openstreetmap.org",
            "https://c.tile.openstreetmap.org"
        )
    }

    data class DownloadProgress(
        val current: Int = 0,
        val total: Int = 0,
        val percentage: Int = 0,
        val isComplete: Boolean = false,
        val errorMessage: String? = null
    )

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var downloadJob: Job? = null

    /**
     * Calculate tile coordinates from lat/lon at given zoom level
     */
    private fun latLonToTile(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom // 2^zoom
        val xtile = ((lon + 180.0) / 360.0 * n).toInt()
        val ytile = ((1.0 - kotlin.math.ln(
            kotlin.math.tan(lat * Math.PI / 180.0) +
                    1.0 / kotlin.math.cos(lat * Math.PI / 180.0)
        ) / Math.PI) / 2.0 * n).toInt()
        return Pair(xtile, ytile)
    }

    /**
     * Calculate total number of tiles to download
     */
    private fun calculateTotalTiles(): Int {
        var total = 0
        for (zoom in MIN_ZOOM..MAX_ZOOM) {
            val (minX, maxY) = latLonToTile(MAX_LAT, MIN_LON, zoom)
            val (maxX, minY) = latLonToTile(MIN_LAT, MAX_LON, zoom)

            val tilesX = (maxX - minX + 1).coerceAtLeast(1)
            val tilesY = (maxY - minY + 1).coerceAtLeast(1)
            total += tilesX * tilesY
        }
        return total
    }

    /**
     * Download single tile
     */
    private suspend fun downloadTile(zoom: Int, x: Int, y: Int): Boolean = withContext(Dispatchers.IO) {
        val tileFile = File(offlineTilePath, "$zoom/$x/$y.png")

        // Skip if already exists
        if (tileFile.exists() && tileFile.length() > 0) {
            return@withContext true
        }

        // Create parent directories
        tileFile.parentFile?.mkdirs()

        // Select tile server (round-robin)
        val serverIndex = (x + y) % TILE_SERVERS.size
        val url = "${TILE_SERVERS[serverIndex]}/$zoom/$x/$y.png"

        return@withContext try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MeshComm/1.0 (Telangana Offline Maps)")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    tileFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Downloaded tile: $zoom/$x/$y")
                true
            } else {
                Log.w(TAG, "Failed to download $zoom/$x/$y: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading tile $zoom/$x/$y: ${e.message}")
            false
        }
    }

    /**
     * Start downloading Telangana map tiles
     */
    fun startDownload(scope: CoroutineScope) {
        if (downloadJob?.isActive == true) {
            Log.w(TAG, "Download already in progress")
            return
        }

        downloadJob = scope.launch {
            try {
                val totalTiles = calculateTotalTiles()
                var downloadedTiles = 0

                Log.i(TAG, "Starting download of $totalTiles tiles for Telangana")

                _progress.value = DownloadProgress(
                    current = 0,
                    total = totalTiles,
                    percentage = 0
                )

                for (zoom in MIN_ZOOM..MAX_ZOOM) {
                    val (minX, maxY) = latLonToTile(MAX_LAT, MIN_LON, zoom)
                    val (maxX, minY) = latLonToTile(MIN_LAT, MAX_LON, zoom)

                    for (x in minX..maxX) {
                        for (y in minY..maxY) {
                            downloadTile(zoom, x, y)
                            downloadedTiles++

                            val percentage = (downloadedTiles * 100) / totalTiles
                            _progress.value = DownloadProgress(
                                current = downloadedTiles,
                                total = totalTiles,
                                percentage = percentage
                            )

                            // Respect OSM tile usage policy: max 250ms delay
                            delay(250)
                        }
                    }
                }

                _progress.value = DownloadProgress(
                    current = totalTiles,
                    total = totalTiles,
                    percentage = 100,
                    isComplete = true
                )

                Log.i(TAG, "Download complete! $downloadedTiles tiles downloaded")

            } catch (e: CancellationException) {
                Log.i(TAG, "Download cancelled")
                _progress.value = DownloadProgress(errorMessage = "Download cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                _progress.value = DownloadProgress(errorMessage = e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }
}
