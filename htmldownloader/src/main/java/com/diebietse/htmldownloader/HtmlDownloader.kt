package com.diebietse.htmldownloader

import android.util.Log
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class HtmlDownloader {
    companion object {
        private val HEADERS = Headers.Builder().add(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"
        ).build()
        private val CLIENT: OkHttpClient = OkHttpClient()
    }

    interface FileSaver {
        fun save(filename: String, content: String)
        fun save(filename: String, content: InputStream)
    }

    data class ParsedHtml(
        val title: String,
        val newHtml: String,
        val filesToDownload: Set<HtmlUtil.DownloadInfo>,
        val cssToDownload: Set<HtmlUtil.DownloadInfo>
    )

    data class ToDownload(
        val filesToDownload: Set<HtmlUtil.DownloadInfo>,
        val cssToDownload: Set<HtmlUtil.DownloadInfo>
    )

    fun download(baseUrl: String, fileSaver: FileSaver) {
        val mainPage = downloadPage(baseUrl)
        val parsedHtml = parseHtml(mainPage, baseUrl)
        fileSaver.save("index.html", parsedHtml.newHtml)

        val filesToDownload = parsedHtml.filesToDownload.toMutableSet()

        val cssList = parsedHtml.cssToDownload.toMutableList()
        while (cssList.isNotEmpty()) {
            val cssToDownload = cssList.removeAt(0)
            val cssPage = downloadPage(cssToDownload.url)
            val parsedCss = parseCss(cssPage, baseUrl)
            fileSaver.save(cssToDownload.filename, parsedCss.newCss)
            parsedCss.cssToDownload.forEach { cssList.add(it) }
            filesToDownload.addAll(parsedCss.filesToDownload)
        }

        filesToDownload.forEach { fileToDownload ->
            val file = downloadFile(fileToDownload.url)
            fileSaver.save(fileToDownload.filename, file)
        }
    }

    private fun downloadPage(url: String): String {
        val request = Request.Builder().url(url).headers(HEADERS).build()

        return try {
            val response = CLIENT.newCall(request).execute()
            val text = response.body!!.string()
            response.body!!.close()
            text
        } catch (e: IOException) {
            Log.e("downloader", e.message, e)
            ""
        }
    }

    private fun downloadFile(url: String): InputStream {
        val request = Request.Builder().url(url).headers(HEADERS).build()

        return try {
            val response = CLIENT.newCall(request).execute()
            val stream = response.body!!.byteStream()
            stream
        } catch (e: IOException) {
            ByteArrayInputStream(ByteArray(0))
        }
    }

    private fun parseHtml(htmlToParse: String, baseUrl: String): ParsedHtml {
        val document = Jsoup.parse(htmlToParse, baseUrl)

        val title = document.title()
        removeBaseElements(document)
        updateAnchorsToAbsolutePath(document)

        val filesAndCss = findAndUpdateStylesheets(document)

        val cssToDownload = mutableSetOf<HtmlUtil.DownloadInfo>()
        cssToDownload.addAll(filesAndCss.cssToDownload)
        cssToDownload.addAll(findAndUpdateStylesheetLinks(document))

        val filesToDownload = mutableSetOf<HtmlUtil.DownloadInfo>()
        filesToDownload.addAll(filesAndCss.filesToDownload)
        filesToDownload.addAll(findAndUpdateScripts(document))
        filesToDownload.addAll(findAndUpdateImages(document))
        filesToDownload.addAll(findAndUpdateInputImages(document))
        filesToDownload.addAll(findAndUpdateInlineStyles(document))

        return ParsedHtml(title, document.outerHtml(), filesToDownload, cssToDownload)
    }

    private fun parseCss(cssToParse: String, baseUrl: String): HtmlUtil.ParsedCss {
        return HtmlUtil.parseCssForUrlAndImport(cssToParse, baseUrl)
    }

    internal fun removeBaseElements(document: Document) {
        val bases = document.select("base[href]")
        for (base in bases) {
            base.remove()
        }
    }

    internal fun updateAnchorsToAbsolutePath(document: Document) {
        val anchors = document.select("a[href]")
        for (anchor in anchors) {
            val absUrl = anchor.attr("abs:href")
            if (!absUrl.startsWith("http")) continue
            anchor.attr("href", absUrl)
        }
    }

    internal fun findAndUpdateStylesheetLinks(document: Document): Set<HtmlUtil.DownloadInfo> {
        val filesToDownload = mutableSetOf<HtmlUtil.DownloadInfo>()

        val links = document.select("link[href][rel=stylesheet]")
        for (link in links) {
            val absUrl = link.attr("abs:href")
            if (!absUrl.startsWith("http")) continue
            var newFileName = HtmlUtil.urlToFileName(absUrl)
            if (!newFileName.endsWith(".css")) newFileName += ".css"
            filesToDownload.add(HtmlUtil.DownloadInfo(absUrl, newFileName))
            link.attr("href", newFileName)
        }
        return filesToDownload
    }

    internal fun findAndUpdateStylesheets(document: Document): ToDownload {
        val styles = document.select("style")
        val filesToDownload = mutableSetOf<HtmlUtil.DownloadInfo>()
        val cssToDownload = mutableSetOf<HtmlUtil.DownloadInfo>()

        for (style in styles) {
            val cssToParse = style.data()

            val toDownloadAndCss = HtmlUtil.parseCssForUrlAndImport(cssToParse, document.baseUri())
            filesToDownload.addAll(toDownloadAndCss.filesToDownload)
            cssToDownload.addAll(toDownloadAndCss.cssToDownload)

            if (style.dataNodes().size != 0) {
                style.dataNodes()[0].wholeData = toDownloadAndCss.newCss
            }
        }
        return ToDownload(filesToDownload, cssToDownload)
    }

    internal fun findAndUpdateInlineStyles(document: Document): Set<HtmlUtil.DownloadInfo> {
        val styles = document.select("[style]")
        val filesToDownload = mutableSetOf<HtmlUtil.DownloadInfo>()

        for (style in styles) {
            val cssToParse = style.attr("style")
            val cssAndLinks = HtmlUtil.parseCssForUrl(cssToParse, document.baseUri())
            style.attr("style", cssAndLinks.css)
            filesToDownload.addAll(cssAndLinks.links)
        }
        return filesToDownload
    }

    internal fun findAndUpdateScripts(document: Document): Set<HtmlUtil.DownloadInfo> {
        return HtmlUtil.findAndUpdateSrc(document, "script[src]")
    }

    internal fun findAndUpdateImages(document: Document): Set<HtmlUtil.DownloadInfo> {
        return HtmlUtil.findAndUpdateSrc(document, "img[src]")
    }

    internal fun findAndUpdateInputImages(document: Document): Set<HtmlUtil.DownloadInfo> {
        return HtmlUtil.findAndUpdateSrc(document, "input[type=image]")
    }
}