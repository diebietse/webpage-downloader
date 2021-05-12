package com.diebietse.webpage.downloader.example

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.diebietse.webpage.downloader.DefaultFileSaver
import com.diebietse.webpage.downloader.WebpageDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)
    private val downloadDir = File(application.filesDir, "downloads")

    suspend fun download(url: String) = withContext(Dispatchers.IO) {
        val pageId = url.hashCode().absoluteValue.toString()
        WebpageDownloader().download(url, DefaultFileSaver(File(downloadDir, pageId)))
        prefs.edit { putString(pageId, url) }
    }

    fun listDownloads(): List<Download> {
        val downloads = downloadDir.listFiles() ?: emptyArray()
        return downloads.mapNotNull { download ->
            val pageId = download.name
            prefs.getString(pageId, null)?.let { url ->
                Download(url, pageId)
            }
        }
    }

    fun offlineUrl(pageId: String): String {
        return "file://${downloadDir}/${pageId}/index.html"
    }

    fun removeDownload(pageId: String) {
        File(downloadDir, pageId).deleteRecursively()
    }
}