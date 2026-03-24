package com.meshcomm.ui.map

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

/**
 * Offline tile downloader for OpenStreetMap tiles
 * Downloads map tiles for a given area and zoom levels for offline use
 */
class OfflineTileDownloader(private val context: Context) {

    companion object {
        private const val TAG = "OfflineTileDownloader"
        private const val TILE_SERVER_URL = "https://tile.openstreetmap.org"
        private const val USER_AGENT = "MeshComm-DisasterApp/1.0"

        // Hyderabad area bounds (customize for your region)
        private const val HYDERABAD_NORTH = 17.6868
        private const val HYDERABAD_SOUTH = 17.2543
        private const val HYDERABAD_EAST = 78.6677
        private const val HYDERABAD_WEST = 78.1198

        // Download limits
        private const val MIN_ZOOM = 10
        private const val MAX_ZOOM = 16
        private const val MAX_TILES_PER_BATCH = 100
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Download tiles for the default Hyderabad region
     */
    fun downloadHyderabadTiles(
        minZoom: Int = MIN_ZOOM,
        maxZoom: Int = MAX_ZOOM,
        onProgress: (current: Int, total: Int, zoomLevel: Int) -> Unit = { _, _, _ -> },
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        val bounds = BoundingBox(HYDERABAD_NORTH, HYDERABAD_EAST, HYDERABAD_SOUTH, HYDERABAD_WEST)
        downloadTilesForArea(bounds, minZoom, maxZoom, onProgress, onComplete)
    }

    /**
     * Download tiles for a custom area
     */
    fun downloadTilesForArea(
        boundingBox: BoundingBox,
        minZoom: Int = MIN_ZOOM,
        maxZoom: Int = MAX_ZOOM,
        onProgress: (current: Int, total: Int, zoomLevel: Int) -> Unit = { _, _, _ -> },
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        scope.launch {
            try {
                val totalTiles = calculateTotalTiles(boundingBox, minZoom, maxZoom)
                Log.i(TAG, "Starting download of $totalTiles tiles for zoom levels $minZoom-$maxZoom")

                var downloadedTiles = 0
                var failedTiles = 0

                for (zoom in minZoom..maxZoom) {
                    val tilesForZoom = getTilesForZoom(boundingBox, zoom)
                    Log.d(TAG, "Downloading ${tilesForZoom.size} tiles for zoom level $zoom")

                    val batches = tilesForZoom.chunked(MAX_TILES_PER_BATCH)

                    for ((batchIndex, batch) in batches.withIndex()) {
                        Log.d(TAG, "Processing batch ${batchIndex + 1}/${batches.size} for zoom $zoom")

                        for (tile in batch) {
                            try {
                                val success = downloadTile(tile.x, tile.y, zoom)
                                if (success) {
                                    downloadedTiles++
                                } else {
                                    failedTiles++
                                }

                                withContext(Dispatchers.Main) {
                                    onProgress(downloadedTiles + failedTiles, totalTiles, zoom)
                                }

                                // Rate limiting to be respectful to OSM servers
                                kotlinx.coroutines.delay(50)

                            } catch (e: Exception) {
                                failedTiles++
                                Log.w(TAG, "Failed to download tile ${tile.x},${tile.y} at zoom $zoom: ${e.message}")
                            }
                        }

                        // Longer pause between batches
                        kotlinx.coroutines.delay(1000)
                    }
                }

                val message = "Download complete: $downloadedTiles successful, $failedTiles failed"
                Log.i(TAG, message)

                withContext(Dispatchers.Main) {
                    onComplete(failedTiles < downloadedTiles / 2, message) // Success if less than 50% failed
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "Download failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun downloadTile(x: Int, y: Int, zoom: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val tileFile = getTileFile(x, y, zoom)

            // Skip if file already exists and is not empty
            if (tileFile.exists() && tileFile.length() > 0) {
                return@withContext true
            }

            val url = "$TILE_SERVER_URL/$zoom/$x/$y.png"
            val request = Request.Builder().url(url).build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.let { body ->
                    tileFile.parentFile?.mkdirs()
                    FileOutputStream(tileFile).use { output ->
                        body.byteStream().copyTo(output)
                    }
                    Log.v(TAG, "Downloaded tile: $x/$y/$zoom")
                    return@withContext true
                }
            }

            Log.w(TAG, "Failed to download tile $x/$y/$zoom: ${response.code}")
            false

        } catch (e: Exception) {
            Log.w(TAG, "Error downloading tile $x/$y/$zoom: ${e.message}")
            false
        }
    }

    private fun getTileFile(x: Int, y: Int, zoom: Int): File {
        val osmDir = File(context.getExternalFilesDir(null), "osmdroid/tiles/Mapnik")
        return File(osmDir, "$zoom/$x/$y.png")
    }

    private fun calculateTotalTiles(boundingBox: BoundingBox, minZoom: Int, maxZoom: Int): Int {
        var total = 0
        for (zoom in minZoom..maxZoom) {
            total += getTilesForZoom(boundingBox, zoom).size
        }
        return total
    }

    private fun getTilesForZoom(boundingBox: BoundingBox, zoom: Int): List<TileCoordinate> {
        val tiles = mutableListOf<TileCoordinate>()

        val northWestTile = latLonToTile(boundingBox.latNorth, boundingBox.lonWest, zoom)
        val southEastTile = latLonToTile(boundingBox.latSouth, boundingBox.lonEast, zoom)

        for (x in northWestTile.x..southEastTile.x) {
            for (y in northWestTile.y..southEastTile.y) {
                tiles.add(TileCoordinate(x, y))
            }
        }

        return tiles
    }

    private fun latLonToTile(lat: Double, lon: Double, zoom: Int): TileCoordinate {
        val latRad = Math.toRadians(lat)
        val n = 2.0.pow(zoom)

        val x = floor((lon + 180.0) / 360.0 * n).toInt()
        val y = floor((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt()

        return TileCoordinate(x, y)
    }

    /**
     * Check how many tiles are already downloaded for an area
     */
    fun getDownloadedTileCount(
        boundingBox: BoundingBox = BoundingBox(HYDERABAD_NORTH, HYDERABAD_EAST, HYDERABAD_SOUTH, HYDERABAD_WEST),
        minZoom: Int = MIN_ZOOM,
        maxZoom: Int = MAX_ZOOM
    ): TileDownloadStats {
        var existingTiles = 0
        var totalTiles = 0

        for (zoom in minZoom..maxZoom) {
            val tilesForZoom = getTilesForZoom(boundingBox, zoom)
            totalTiles += tilesForZoom.size

            for (tile in tilesForZoom) {
                val file = getTileFile(tile.x, tile.y, zoom)
                if (file.exists() && file.length() > 0) {
                    existingTiles++
                }
            }
        }

        return TileDownloadStats(existingTiles, totalTiles)
    }

    /**
     * Clear all downloaded tiles to free up space
     */
    fun clearDownloadedTiles(): Boolean {
        return try {
            val osmDir = File(context.getExternalFilesDir(null), "osmdroid")
            val deleted = osmDir.deleteRecursively()
            Log.i(TAG, "Cleared downloaded tiles: success=$deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear tiles: ${e.message}")
            false
        }
    }

    /**
     * Get size of downloaded tiles in bytes
     */
    fun getDownloadedTilesSize(): Long {
        val osmDir = File(context.getExternalFilesDir(null), "osmdroid")
        return calculateDirectorySize(osmDir)
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L

        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}

/**
 * Tile coordinate data class
 */
data class TileCoordinate(val x: Int, val y: Int)

/**
 * Download statistics data class
 */
data class TileDownloadStats(
    val existingTiles: Int,
    val totalTiles: Int
) {
    val completionPercentage: Int = if (totalTiles > 0) (existingTiles * 100) / totalTiles else 0
    val isComplete: Boolean = existingTiles == totalTiles && totalTiles > 0
}