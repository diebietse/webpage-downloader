package com.diebietse.htmldownloader

import android.util.Log
import java.io.File
import java.io.InputStream

class DefaultFileSaver(private val outputDir: File) : HtmlDownloader.FileSaver {
    init {
        outputDir.mkdirs()
    }

    override fun save(filename: String, content: String) {
        // TODO remove
        Log.d("Saving", filename)
        File(outputDir, filename).writeText(content)
    }

    override fun save(filename: String, content: InputStream) {
        // TODO remove
        Log.d("Saving", filename)
        File(outputDir, filename).outputStream().use { content.copyTo(it) }
    }
}