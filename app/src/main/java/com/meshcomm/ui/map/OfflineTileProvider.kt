package com.meshcomm.ui.map

import android.util.Log
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File

/**
 * Custom tile provider that:
 * 1. Uses OpenStreetMap online tiles as the tile source
 * 2. OSMDroid automatically caches tiles locally
 * 3. Supports checking for offline tiles in custom path
 */
class OfflineTileProvider(
    private val offlineTilePath: String = "/storage/emulated/0/tiles"
) {

    companion object {
        private const val TAG = "OfflineTileProvider"
        const val TILE_SOURCE_NAME = "TelanganaOfflineOSM"
        const val MIN_ZOOM = 5
        const val MAX_ZOOM = 18  // Increased to 18 for street-level detail
    }

    /**
     * Standard OpenStreetMap tile source
     * OSMDroid will automatically handle caching
     */
    class OSMTileSource : OnlineTileSourceBase(
        TILE_SOURCE_NAME,
        MIN_ZOOM,
        MAX_ZOOM,
        256,
        ".png",
        arrayOf(
            "https://a.tile.openstreetmap.org/",
            "https://b.tile.openstreetmap.org/",
            "https://c.tile.openstreetmap.org/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "$zoom/$x/$y.png"
        }
    }

    fun getTileSource(): ITileSource {
        Log.d(TAG, "Using OSM tile source with automatic caching")
        return OSMTileSource()
    }

    /**
     * Check if offline tiles exist for Telangana region
     * Telangana bounding box: approximately 15.85°N to 19.92°N, 77.23°E to 81.97°E
     */
    fun hasOfflineTiles(): Boolean {
        // Check if at least some tiles exist at zoom level 10 (reasonable mid-zoom)
        val telanganaCenter = File(offlineTilePath, "10/630/389.png")
        val exists = telanganaCenter.exists() || File(offlineTilePath).exists()
        Log.d(TAG, "Checking for offline tiles: $exists")
        return exists
    }

    /**
     * Get estimated storage space used by offline tiles
     */
    fun getOfflineTileSize(): Long {
        val tilesDir = File(offlineTilePath)
        return if (tilesDir.exists()) {
            val size = tilesDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            Log.d(TAG, "Offline tile size: $size bytes")
            size
        } else {
            Log.d(TAG, "No offline tiles directory found")
            0L
        }
    }
}
