package com.meshcomm.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

object OfflineMapUtils {

    private const val TAG = "OfflineMapUtils"
    private const val TILE_EXPIRY_TIME_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days

    /**
     * Initialize OSMDroid configuration for optimal offline usage
     */
    fun initializeOSMDroid(context: Context) {
        try {
            // Load configuration
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

            // Set user agent (required for tile servers)
            Configuration.getInstance().userAgentValue = context.packageName

            // Configure cache settings for better offline experience
            Configuration.getInstance().apply {
                // Increase cache sizes for offline usage
                tileDownloadMaxQueueSize = 20
                tileFileSystemMaxQueueSize = 40

                // Set cache directory
                osmdroidBasePath = File(context.cacheDir, "osmdroid")
                osmdroidTileCache = File(osmdroidBasePath, "tiles")

                // Network settings for better offline behavior
                tileDownloadThreads = 4
                tileFileSystemThreads = 8

                // Cache timeout (use default tileFileSystemCacheMaxBytes if needed)
                tileFileSystemCacheMaxBytes = 100 * 1024 * 1024L // 100MB
            }

            // Ensure cache directory exists
            Configuration.getInstance().osmdroidTileCache.mkdirs()

            Log.d(TAG, "OSMDroid initialized successfully")
            Log.d(TAG, "Cache directory: ${Configuration.getInstance().osmdroidTileCache}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OSMDroid: ${e.message}", e)
        }
    }

    /**
     * Pre-download map tiles for offline usage
     */
    fun downloadTilesForArea(
        mapView: MapView,
        boundingBox: BoundingBox,
        minZoom: Int = 10,
        maxZoom: Int = 18,
        callback: (progress: Int, total: Int, isComplete: Boolean) -> Unit
    ) {
        try {
            val cacheManager = CacheManager(mapView)
            val tileSource = mapView.tileProvider.tileSource

            Log.d(TAG, "Starting tile download for area: $boundingBox, zoom: $minZoom-$maxZoom")

            // Create callback for CacheManager
            val cacheCallback = object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    Handler(Looper.getMainLooper()).post {
                        callback(100, 100, true)
                        Log.d(TAG, "Tile download completed successfully")
                    }
                }

                override fun onTaskFailed(errors: Int) {
                    Handler(Looper.getMainLooper()).post {
                        callback(0, 100, true)
                        Log.w(TAG, "Tile download failed with $errors errors")
                    }
                }

                override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                    Handler(Looper.getMainLooper()).post {
                        val totalZoomLevels = zoomMax - zoomMin + 1
                        val currentZoomProgress = currentZoomLevel - zoomMin
                        val overallProgress = ((currentZoomProgress * 100) + progress) / totalZoomLevels
                        callback(overallProgress.coerceAtMost(100), 100, false)
                    }
                }

                override fun downloadStarted() {
                    Handler(Looper.getMainLooper()).post {
                        callback(0, 100, false)
                        Log.d(TAG, "Tile download started")
                    }
                }

                override fun setPossibleTilesInArea(total: Int) {
                    Handler(Looper.getMainLooper()).post {
                        callback(0, total, false)
                        Log.d(TAG, "Total tiles to download: $total")
                    }
                }
            }

            // Start download in background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Download tiles using correct API
                    cacheManager.downloadAreaAsync(
                        mapView.context,
                        boundingBox,
                        minZoom,
                        maxZoom,
                        cacheCallback
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Error during tile download: ${e.message}", e)
                    Handler(Looper.getMainLooper()).post {
                        callback(0, 0, true) // Report completion with error
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tile download: ${e.message}", e)
            callback(0, 0, true)
        }
    }

    /**
     * Download tiles for a specific location with radius
     */
    fun downloadTilesAroundLocation(
        mapView: MapView,
        center: GeoPoint,
        radiusKm: Double,
        minZoom: Int = 10,
        maxZoom: Int = 18,
        callback: (progress: Int, total: Int, isComplete: Boolean) -> Unit
    ) {
        // Convert radius to bounding box
        val boundingBox = createBoundingBoxFromRadius(center, radiusKm)
        downloadTilesForArea(mapView, boundingBox, minZoom, maxZoom, callback)
    }

    /**
     * Create bounding box from center point and radius
     */
    private fun createBoundingBoxFromRadius(center: GeoPoint, radiusKm: Double): BoundingBox {
        // Approximate conversion: 1 degree ≈ 111 km
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(center.latitude)))

        return BoundingBox(
            center.latitude + latDelta,  // North
            center.latitude - latDelta,  // South
            center.longitude + lngDelta, // East
            center.longitude - lngDelta  // West
        )
    }

    /**
     * Get cache size and detailed info
     */
    fun getCacheInfo(context: Context): CacheInfo {
        return try {
            val cacheDir = Configuration.getInstance().osmdroidTileCache
            val cacheSize = calculateDirectorySize(cacheDir)
            val tileCount = countTileFiles(cacheDir)

            CacheInfo(
                sizeBytes = cacheSize,
                sizeMB = cacheSize / (1024 * 1024),
                tileCount = tileCount,
                cachePath = cacheDir.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache info: ${e.message}", e)
            CacheInfo(0, 0, 0, "Unknown")
        }
    }

    /**
     * Clear all cached tiles
     */
    fun clearCache(context: Context): Boolean {
        return try {
            val cacheDir = Configuration.getInstance().osmdroidTileCache
            val deleted = deleteDirectory(cacheDir)
            cacheDir.mkdirs() // Recreate directory

            Log.d(TAG, "Cache cleared: $deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}", e)
            false
        }
    }

    /**
     * Clear cache for specific area (advanced usage)
     */
    fun clearCacheForArea(
        mapView: MapView,
        boundingBox: BoundingBox,
        minZoom: Int = 0,
        maxZoom: Int = 20
    ) {
        try {
            val cacheManager = CacheManager(mapView)

            // Use correct cleanAreaAsync signature (no callback)
            cacheManager.cleanAreaAsync(
                mapView.context,
                boundingBox,
                minZoom,
                maxZoom
            )

            Log.d(TAG, "Started clearing cache for area: $boundingBox")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear area cache: ${e.message}", e)
        }
    }

    /**
     * Check if basic offline availability exists (simplified)
     */
    fun checkOfflineAvailability(
        context: Context,
        boundingBox: BoundingBox
    ): OfflineAvailability {
        return try {
            val cacheDir = Configuration.getInstance().osmdroidTileCache
            val hasCache = cacheDir.exists() && cacheDir.isDirectory && cacheDir.listFiles()?.isNotEmpty() == true
            val cacheSize = calculateDirectorySize(cacheDir)
            val tileCount = countTileFiles(cacheDir)

            // Simple availability check based on cache presence
            val isAvailable = hasCache && cacheSize > 1024 * 1024 // At least 1MB of cache
            val coveragePercent = if (isAvailable) 75 else 0 // Approximate

            OfflineAvailability(
                isAvailable = isAvailable,
                coveragePercent = coveragePercent,
                totalTiles = tileCount,
                availableTiles = if (isAvailable) tileCount else 0
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error checking offline availability: ${e.message}", e)
            OfflineAvailability(false, 0, 0, 0)
        }
    }

    /**
     * Get estimated tile count for area and zoom range
     */
    fun estimateTileCount(boundingBox: BoundingBox, minZoom: Int, maxZoom: Int): Int {
        var totalTiles = 0

        for (zoom in minZoom..maxZoom) {
            val tilesPerSide = 1 shl zoom // 2^zoom
            val latTiles = ((boundingBox.latNorth - boundingBox.latSouth) * tilesPerSide / 360.0).toInt() + 1
            val lonTiles = ((boundingBox.lonEast - boundingBox.lonWest) * tilesPerSide / 360.0).toInt() + 1
            totalTiles += latTiles * lonTiles
        }

        return totalTiles.coerceAtLeast(1)
    }

    // Utility functions
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            if (directory.exists() && directory.isDirectory) {
                directory.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        size += file.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating directory size: ${e.message}")
        }
        return size
    }

    private fun countTileFiles(directory: File): Int {
        var count = 0
        try {
            if (directory.exists() && directory.isDirectory) {
                directory.walkTopDown().forEach { file ->
                    if (file.isFile && (file.extension == "tile" || file.extension == "png" || file.extension == "jpg")) {
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting files: ${e.message}")
        }
        return count
    }

    private fun deleteDirectory(directory: File): Boolean {
        return try {
            if (directory.exists()) {
                directory.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting directory: ${e.message}")
            false
        }
    }

    // Data classes
    data class CacheInfo(
        val sizeBytes: Long,
        val sizeMB: Long,
        val tileCount: Int,
        val cachePath: String
    )

    data class OfflineAvailability(
        val isAvailable: Boolean,
        val coveragePercent: Int,
        val totalTiles: Int,
        val availableTiles: Int
    )
}