package com.diebietse.htmldownloader

import java.io.File
import java.io.InputStream

class DefaultFileSaver(private val outputDir: File) : HtmlDownloader.FileSaver {
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