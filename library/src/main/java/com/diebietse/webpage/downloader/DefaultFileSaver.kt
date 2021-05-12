package com.diebietse.webpage.downloader

import java.io.File
import java.io.InputStream

/**
 * An [WebpageDownloader.FileSaver] that writes files to the filesystem in the given [outputDir]
 *
 * @property outputDir the location to write the files to
 */
class DefaultFileSaver(private val outputDir: File) : WebpageDownloader.FileSaver {
    init {
        outputDir.mkdirs()
    }

    override fun save(filename: String, content: String) {
        File(outputDir, filename).writeText(content)
    }

    override fun save(filename: String, content: InputStream) {
        File(outputDir, filename).outputStream().use { content.copyTo(it) }
    }
}