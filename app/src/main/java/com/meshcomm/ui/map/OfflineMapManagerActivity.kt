package com.meshcomm.ui.map

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.meshcomm.R
import com.meshcomm.databinding.ActivityOfflineMapManagerBinding
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox

class OfflineMapManagerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OfflineMapManager"

        fun start(context: Context) {
            context.startActivity(Intent(context, OfflineMapManagerActivity::class.java))
        }
    }

    private lateinit var binding: ActivityOfflineMapManagerBinding
    private lateinit var tileDownloader: OfflineTileDownloader

    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineMapManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tileDownloader = OfflineTileDownloader(this)

        setupToolbar()
        setupClickListeners()
        refreshStats()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Offline Maps"
    }

    private fun setupClickListeners() {
        // Download Hyderabad tiles
        binding.btnDownloadHyderabad.setOnClickListener {
            if (!isDownloading) {
                startHyderabadDownload()
            }
        }

        // Download custom area
        binding.btnDownloadCustom.setOnClickListener {
            if (!isDownloading) {
                showCustomAreaDialog()
            }
        }

        // Clear all tiles
        binding.btnClearTiles.setOnClickListener {
            showClearTilesDialog()
        }

        // Refresh stats
        binding.btnRefresh.setOnClickListener {
            refreshStats()
        }
    }

    private fun refreshStats() {
        lifecycleScope.launch {
            try {
                val stats = tileDownloader.getDownloadedTileCount()
                val sizeBytes = tileDownloader.getDownloadedTilesSize()
                val sizeFormatted = Formatter.formatFileSize(this@OfflineMapManagerActivity, sizeBytes)

                binding.tvTileStats.text = "${stats.existingTiles} / ${stats.totalTiles} tiles (${stats.completionPercentage}%)"
                binding.tvStorageUsed.text = sizeFormatted

                binding.progressOverall.progress = stats.completionPercentage
                binding.tvProgressPercent.text = "${stats.completionPercentage}%"

                if (stats.isComplete) {
                    binding.chipMapStatus.text = "Maps: Ready"
                    binding.chipMapStatus.setChipBackgroundColorResource(R.color.mesh_active)
                } else if (stats.existingTiles > 0) {
                    binding.chipMapStatus.text = "Maps: Partial"
                    binding.chipMapStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                } else {
                    binding.chipMapStatus.text = "Maps: None"
                    binding.chipMapStatus.setChipBackgroundColorResource(R.color.sos_red)
                }

                // Update last refresh time
                binding.tvLastRefresh.text = "Last updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"

            } catch (e: Exception) {
                Toast.makeText(this@OfflineMapManagerActivity, "Failed to refresh stats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startHyderabadDownload() {
        AlertDialog.Builder(this)
            .setTitle("Download Offline Maps")
            .setMessage("This will download map tiles for the Hyderabad region for offline use. This may take several minutes and use significant data.")
            .setPositiveButton("Download") { _, _ ->
                performDownload {
                    tileDownloader.downloadHyderabadTiles(
                        onProgress = ::updateDownloadProgress,
                        onComplete = ::onDownloadComplete
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomAreaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_area, null)

        // Get input fields from dialog
        val etNorth = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNorth)
        val etSouth = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSouth)
        val etEast = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEast)
        val etWest = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etWest)

        // Set default values (Hyderabad area)
        etNorth.setText("17.6868")
        etSouth.setText("17.2543")
        etEast.setText("78.6677")
        etWest.setText("78.1198")

        AlertDialog.Builder(this)
            .setTitle("Download Custom Area")
            .setMessage("Enter coordinates for the area to download:")
            .setView(dialogView)
            .setPositiveButton("Download") { _, _ ->
                try {
                    val north = etNorth.text.toString().toDouble()
                    val south = etSouth.text.toString().toDouble()
                    val east = etEast.text.toString().toDouble()
                    val west = etWest.text.toString().toDouble()

                    val bounds = BoundingBox(north, east, south, west)

                    performDownload {
                        tileDownloader.downloadTilesForArea(
                            bounds,
                            onProgress = ::updateDownloadProgress,
                            onComplete = ::onDownloadComplete
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid coordinates: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearTilesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Offline Maps")
            .setMessage("This will delete all downloaded map tiles to free up storage space. You'll need to download them again for offline use.")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch {
                    val success = tileDownloader.clearDownloadedTiles()
                    if (success) {
                        Toast.makeText(this@OfflineMapManagerActivity, "Map tiles cleared successfully", Toast.LENGTH_SHORT).show()
                        refreshStats()
                    } else {
                        Toast.makeText(this@OfflineMapManagerActivity, "Failed to clear map tiles", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDownload(downloadAction: () -> Unit) {
        isDownloading = true
        updateDownloadUI(true)
        downloadAction()
    }

    private fun updateDownloadProgress(current: Int, total: Int, zoomLevel: Int) {
        runOnUiThread {
            val progress = if (total > 0) (current * 100) / total else 0
            binding.progressDownload.progress = progress
            binding.tvDownloadProgress.text = "$current / $total tiles (${progress}%)"
            binding.tvCurrentZoom.text = "Zoom level: $zoomLevel"
        }
    }

    private fun onDownloadComplete(success: Boolean, message: String) {
        runOnUiThread {
            isDownloading = false
            updateDownloadUI(false)

            val toastMessage = if (success) {
                "Download completed successfully"
            } else {
                "Download failed: $message"
            }

            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
            refreshStats()
        }
    }

    private fun updateDownloadUI(downloading: Boolean) {
        binding.progressDownload.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.tvDownloadProgress.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.tvCurrentZoom.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.layoutDownloadStatus.visibility = if (downloading) View.VISIBLE else View.GONE

        binding.btnDownloadHyderabad.isEnabled = !downloading
        binding.btnDownloadCustom.isEnabled = !downloading
        binding.btnClearTiles.isEnabled = !downloading

        if (!downloading) {
            binding.tvDownloadProgress.text = ""
            binding.tvCurrentZoom.text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }
}