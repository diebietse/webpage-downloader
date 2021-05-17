package com.diebietse.webpage.downloader

import com.diebietse.webpage.downloader.WebpageDownloader.FileSaver
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.io.InputStream

/**
 * The WebpageDownloader implementation, it contains a [download] method used to download a
 * webpage and a [FileSaver] interface used to save the downloaded files.
 *
 */
class WebpageDownloader {
    private companion object {
        private val HEADERS = Headers.Builder().add(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Mobile Safari/537.36"
        ).build()
        private val CLIENT: OkHttpClient = OkHttpClient()
    }

    /**
     * A interface used to save downloaded files in different ways
     *
     */
    interface FileSaver {
        /**
         * Saves text files like html and css
         *
         * @param filename name to save the file as
         * @param content a [String] containing the file's content
         */
        fun save(filename: String, content: String)

        /**
         * Saves downloaded binary data such as fonts and images
         *
         * @param filename name to save the file as
         * @param content an [InputStream] containing the file's content
         */
        fun save(filename: String, content: InputStream)
    }

    private data class ParsedHtml(
        val title: String,
        val newHtml: String,
        val filesToDownload: Set<HtmlUtil.DownloadInfo>,
        val cssToDownload: Set<HtmlUtil.DownloadInfo>
    )

    internal data class ToDownload(
        val filesToDownload: Set<HtmlUtil.DownloadInfo>,
        val cssToDownload: Set<HtmlUtil.DownloadInfo>
    )

    /**
     * Download a webpage and all it's linked assets, then override all links to reference the local
     * assets
     *
     * @param url the url of the webpage to download
     * @param fileSaver an implementation of [FileSaver] to save all downloaded files
     *
     * @throws IllegalArgumentException If the url is not a well-formed HTTP or HTTPS URL.
     * @throws IOException If there is a problem downloading the webpage
     */
    fun download(url: String, fileSaver: FileSaver) {
        download(url.toHttpUrl(), fileSaver)
    }

    /**
     * Download a webpage and all it's linked assets, then override all links to reference the local
     * assets
     *
     * @param url the [HttpUrl] of the webpage to download
     * @param fileSaver an implementation of [FileSaver] to save all downloaded files
     *
     * @throws IOException If there is a problem downloading the webpage
     */
    fun download(url: HttpUrl, fileSaver: FileSaver) {
        val mainPage = downloadPage(url)
        val parsedHtml = parseHtml(mainPage, url)
        fileSaver.save("index.html", parsedHtml.newHtml)

        val filesToDownload = parsedHtml.filesToDownload.toMutableSet()

        val cssList = parsedHtml.cssToDownload.toMutableList()
        while (cssList.isNotEmpty()) {
            val cssToDownload = cssList.removeAt(0)
            val cssPage = downloadPage(cssToDownload.url.toHttpUrl())
            val parsedCss = parseCss(cssPage, url)
            fileSaver.save(cssToDownload.filename, parsedCss.newCss)
            parsedCss.cssToDownload.forEach { cssList.add(it) }
            filesToDownload.addAll(parsedCss.filesToDownload)
        }

        filesToDownload.forEach { fileToDownload ->
            downloadFile(fileToDownload, fileSaver)
        }
    }

    private fun downloadPage(url: HttpUrl): String {
        val request = Request.Builder().url(url).headers(HEADERS).build()
        val response = CLIENT.newCall(request).execute()
        val text = response.body!!.string()
        response.close()
        return text
    }

    private fun downloadFile(fileToDownload: HtmlUtil.DownloadInfo, fileSaver: FileSaver) {
        val request = Request.Builder().url(fileToDownload.url).headers(HEADERS).build()
        val response = CLIENT.newCall(request).execute()
        fileSaver.save(fileToDownload.filename, response.body!!.byteStream())
        response.close()
    }

    private fun parseHtml(htmlToParse: String, baseUrl: HttpUrl): ParsedHtml {
        val document = Jsoup.parse(htmlToParse, baseUrl.toString())

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

    private fun parseCss(cssToParse: String, baseUrl: HttpUrl): HtmlUtil.ParsedCss {
        return HtmlUtil.parseCssForUrlAndImport(cssToParse, baseUrl.toString())
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
            link.removeAttr("crossorigin")
            link.removeAttr("integrity")
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