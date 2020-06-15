package com.diebietse.htmldownloader

import org.jsoup.nodes.Document
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern
import kotlin.math.absoluteValue

object HtmlUtil {
    private val FILE_NAME_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9-_.]")

    // https://regex101.com/ gives a nice explanation of the regex
    // This is to support both: https://developer.mozilla.org/en-US/docs/Web/CSS/url and
    // https://developer.mozilla.org/en-US/docs/Web/CSS/@import
    private val URL_PATTERN = Pattern.compile("""url\s*\(\s*['"]?\s*(.*?)\s*['"]?\s*\)""")
    private val IMPORT_PATTERN = Pattern.compile("""@import\s*['"]\s*(.*)\s*['"]\s*""")

    data class DownloadInfo(val url: String, val filename: String)

    data class ParsedCss(
        val newCss: String,
        val filesToDownload: Set<DownloadInfo>,
        val cssToDownload: Set<DownloadInfo>
    )

    data class CssAndLinks(val css: String, val links: Set<DownloadInfo>)

    fun findAndUpdateSrc(document: Document, cssQuery: String): Set<DownloadInfo> {
        val links = document.select(cssQuery)
        val filesToDownload = mutableSetOf<DownloadInfo>()

        for (link in links) {
            val absUrl = link.attr("abs:src")
            if (!absUrl.startsWith("http")) continue
            val newFileName = urlToFileName(absUrl)
            filesToDownload.add(DownloadInfo(absUrl, newFileName))
            link.attr("src", newFileName)
            link.removeAttr("srcset")
            link.removeAttr("crossorigin")
            link.removeAttr("integrity")
        }
        return filesToDownload
    }

    fun parseCssForUrlAndImport(cssToParse: String, baseUrl: String): ParsedCss {
        val filesToDownload = mutableSetOf<DownloadInfo>()
        val cssToDownload = mutableSetOf<DownloadInfo>()

        val cssAndLinks = parseCssForPattern(cssToParse, baseUrl, URL_PATTERN)
        cssAndLinks.links.forEach { entry ->
            if (entry.filename.endsWith(".css")) {
                cssToDownload.add(entry)
            } else {
                filesToDownload.add(entry)
            }
        }

        val cssAndCssLinks = parseCssForPattern(cssAndLinks.css, baseUrl, IMPORT_PATTERN)
        cssToDownload.addAll(cssAndCssLinks.links)
        return ParsedCss(cssAndCssLinks.css, filesToDownload, cssToDownload)
    }

    fun parseCssForUrl(cssToParse: String, baseUrl: String): CssAndLinks {
        return parseCssForPattern(cssToParse, baseUrl, URL_PATTERN)
    }

    private fun parseCssForPattern(
        cssToParse: String,
        baseUrl: String,
        pattern: Pattern
    ): CssAndLinks {
        var css = cssToParse
        val links = mutableSetOf<DownloadInfo>()

        val matcher = pattern.matcher(css)
        val sb = StringBuffer()
        while (matcher.find()) {
            val match = matcher.group(1) ?: continue
            if (match.startsWith("data:image/")) continue
            val absMatch = resolve(baseUrl, match)
            val filename = urlToFileName(absMatch)
            val replacement = matcher.group().replace(match, filename)
            matcher.appendReplacement(sb, replacement)
            links.add(DownloadInfo(absMatch, filename))
        }
        matcher.appendTail(sb)
        css = sb.toString()

        return CssAndLinks(css, links)
    }

    fun urlToFileName(url: String): String {
        var filename = url.substring(url.lastIndexOf('/') + 1)
        filename = filename.takeLast(90)
        val hash = url.hashCode().absoluteValue

        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf("?"))
        }
        val extension: String
        if (filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."))
            filename = filename.substring(0, filename.lastIndexOf("."))
        } else {
            extension = ".$filename"
            filename = "no-name"
        }
        filename = "${filename}-${hash}${extension}"
        filename = FILE_NAME_SANITIZE_PATTERN.matcher(filename).replaceAll("_")

        return filename
    }

    /**
     * Copied from org.jsoup.internal.StringUtil.resolve()
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param baseUrl the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return an absolute URL if one was able to be generated, or the empty string if not
     */
    private fun resolve(baseUrl: String, relUrl: String): String {
        val base: URL
        return try {
            base = try {
                URL(baseUrl)
            } catch (e: MalformedURLException) {
                // the base is unsuitable, but the attribute/rel may be abs on its own, so try that
                val abs = URL(relUrl)
                return abs.toExternalForm()
            }
            resolve(base, relUrl).toExternalForm()
        } catch (e: MalformedURLException) {
            ""
        }
    }

    /**
     * Copied from org.jsoup.internal.StringUtil.resolve()
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param base the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return the resolved absolute URL
     * @throws MalformedURLException if an error occurred generating the URL
     */
    @Throws(MalformedURLException::class)
    private fun resolve(base: URL, relUrl: String): URL {
        // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
        var baseIn = base
        var relUrlIn = relUrl
        if (relUrlIn.startsWith("?")) relUrlIn = baseIn.path + relUrlIn
        // workaround: //example.com + ./foo = //example.com/./foo, not //example.com/foo
        if (relUrlIn.indexOf('.') == 0 && baseIn.file.indexOf('/') != 0) {
            baseIn = URL(baseIn.protocol, baseIn.host, baseIn.port, "/" + baseIn.file)
        }
        return URL(baseIn, relUrlIn)
    }
}