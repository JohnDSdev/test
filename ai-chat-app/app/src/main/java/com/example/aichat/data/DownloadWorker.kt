package com.example.aichat.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.io.File

/**
 * Worker responsible for downloading model files using the system
 * [DownloadManager].  The download URL and destination file name are passed
 * via the input data.  Progress is periodically reported via the returned
 * [Result.Success] data.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return Result.failure()

        val destinationDir = File(applicationContext.filesDir, "models")
        if (!destinationDir.exists()) destinationDir.mkdirs()

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading model...")
            setDestinationUri(Uri.fromFile(File(destinationDir, fileName)))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Poll until the download completes.  In a real implementation you
        // would register a BroadcastReceiver for ACTION_DOWNLOAD_COMPLETE to
        // react promptly.  Here we simply wait and check progress every 2s.
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val progress = if (bytesTotal > 0) (bytesDownloaded * 100f / bytesTotal).toInt() else 0
                // Update progress via setProgress.  The WorkManager UI can observe this.
                setProgress(workDataOf(KEY_PROGRESS to progress))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    cursor.close()
                    return Result.success(workDataOf(KEY_FILE_URI to uriString))
                } else if (status == DownloadManager.STATUS_FAILED) {
                    cursor.close()
                    return Result.failure()
                }
            }
            cursor?.close()
            delay(2000)
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_URI = "file_uri"
    }
}