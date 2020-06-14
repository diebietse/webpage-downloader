package com.diebietse.htmldownloader

import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.jsoup.Jsoup
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.InputStream


class HtmlDownloaderTest {
    companion object {
        private val htmlDownloader = HtmlDownloader()

        val MOCK_FILE_SAVER = object : HtmlDownloader.FileSaver {
            override fun save(filename: String, content: String) {}
            override fun save(filename: String, content: InputStream) {
                content.close()
            }
        }
    }


    @Test
    @Ignore("Used for a manual test")
    fun download() {
        htmlDownloader.download(
            "https://en.wikipedia.org/wiki/HTML",
            DefaultFileSaver(File("build/temp"))
        )
    }

    @Test
    fun download_update_css_list() {
        val server = MockWebServer()
        val url = server.url("index.html").toString()

        server.enqueue(MockResponse().setBody("<link rel='stylesheet' href='${server.url("css")}'/>"))
        server.enqueue(MockResponse().setBody("@import url(${server.url("font.css")});"))
        server.enqueue(
            MockResponse().setBody(
                "@font-face { src: local('Roboto'), local('Roboto-Regular'), url(${server.url(
                    "font.ttf"
                )}) format('truetype'); }"
            )
        )
        server.enqueue(MockResponse().setBody("someBinaryBlob"))

        htmlDownloader.download(url, MOCK_FILE_SAVER)

        assertThat(server.takeRequest().path).isEqualTo("/index.html")
        assertThat(server.takeRequest().path).isEqualTo("/css")
        assertThat(server.takeRequest().path).isEqualTo("/font.css")
        assertThat(server.takeRequest().path).isEqualTo("/font.ttf")
    }

    @Test
    fun removeBaseUrl_removes_all_base_elements() {
        val html = """
            <!DOCTYPE html>
            <html lang="en"><head>
                <base href="/" />
                <base href="/test/" />
                <title>Document</title>
            </head></html>
        """.trimIndent()
        val document = Jsoup.parse(html)
        htmlDownloader.removeBaseElements(document)

        assertThat(document.toString()).doesNotContain("base")
    }

    @Test
    fun updateAnchorsToAbsolutePath() {
        val anchor = """<a href="test">"""
        val document = Jsoup.parse(anchor, "https://example.com")
        htmlDownloader.updateAnchorsToAbsolutePath(document)

        assertThat(
            document.selectFirst("a[href]").attr("href")
        ).isEqualTo("https://example.com/test")
    }

    @Test
    fun findAndUpdateStylesheetLinks() {
        val stylesheetHtml = """
            <link rel='stylesheet' href='https://example.org/style1.min.css?ver=1.0.0' />
            <link rel='stylesheet' href='style2.min.css' />
            """.trimIndent()
        val document = Jsoup.parse(stylesheetHtml, "https://example.com")
        val stylesheets = htmlDownloader.findAndUpdateStylesheetLinks(document)
        val hrefs = document.select("link[href]").eachAttr("href")

        assertThat(stylesheets).hasSize(2)
        assertThat(stylesheets.map { it.url }).contains("https://example.org/style1.min.css?ver=1.0.0")
        assertThat(stylesheets.map { it.url }).contains("https://example.com/style2.min.css")
        assertThat(hrefs).hasSize(2)
        hrefs.forEach { href ->
            assertThat(stylesheets.map { it.filename }.contains(href)).isTrue()
        }
    }

    @Test
    fun findAndUpdateStylesheetLinks_removes_sri() {
        val stylesheetHtml = """
            <link ="anonymous" rel='stylesheet' integrity="sha512-x...w==" href='https://example.org/style1.min.css?ver=1.0.0' />
            """.trimIndent()
        val document = Jsoup.parse(stylesheetHtml, "https://example.com")
        val stylesheets = htmlDownloader.findAndUpdateStylesheetLinks(document)

        assertThat(stylesheets).hasSize(1)

        val newHtml = document.toString()
        assertThat(newHtml).doesNotContain("crossorigin")
        assertThat(newHtml).doesNotContain("integrity")
    }

    @Test
    fun findAndUpdateStylesheets() {
        val htmlEmbeddedCss = """
            <style type="text/css">
                @import url("https://www.example.org/test1.css");
                .class1 { background: url("test2.jpg") #00D no-repeat fixed;}
            </style>
        """.trimIndent()
        val document = Jsoup.parse(htmlEmbeddedCss, "https://example.com")
        val filesAndCss = htmlDownloader.findAndUpdateStylesheets(document)
        val styles = document.selectFirst("style[type=text/css]").data()
        assertThat(styles).doesNotContain("https://www.example.org/")
        assertThat(styles).contains("test1-")
        assertThat(styles).contains("test2-")

        val css = filesAndCss.cssToDownload
        assertThat(css).hasSize(1)
        assertThat(css.map { it.url }).contains("https://www.example.org/test1.css")

        val files = filesAndCss.filesToDownload
        assertThat(files).hasSize(1)
        assertThat(files.map { it.url }).contains("https://example.com/test2.jpg")

        val newHtml = document.toString()
        css.forEach { assertThat(newHtml).contains(it.filename) }
        files.forEach { assertThat(newHtml).contains(it.filename) }
    }

    @Test
    fun findAndUpdateInlineStyles() {
        val htmlInlineCss = """
            <div style="background-image:url('https://www.example.org/test1.jpg')">
                <div style="background-image:url('test2.jpg')" >
                </div>
            </div>
        """.trimIndent()
        val document = Jsoup.parse(htmlInlineCss, "https://example.com")
        val stylesheets = htmlDownloader.findAndUpdateInlineStyles(document)
        val newHtml = document.toString()
        assertThat(newHtml).doesNotContain("https://www.example.org/")
        assertThat(newHtml).contains("test1-")
        assertThat(newHtml).contains("test2-")

        assertThat(stylesheets).hasSize(2)
        assertThat(stylesheets.map { it.url }).containsExactly(
            "https://www.example.org/test1.jpg",
            "https://example.com/test2.jpg"
        )

        stylesheets.forEach { assertThat(newHtml).contains(it.filename) }
    }

    @Test
    fun findAndUpdateScripts() {
        val htmlScripts = """
            <script type='text/javascript' src='https://www.example.org/test1.js?ver=5.2.5'></script>
            <script type='text/javascript' src='test2.js?ver=1.0.0'></script>
            <script>document.getElementById("demo").innerHTML = "Hello JavaScript!";</script>
        """.trimIndent()

        val document = Jsoup.parse(htmlScripts, "https://example.com")
        val scripts = htmlDownloader.findAndUpdateScripts(document)
        val newHtml = document.toString()
        assertThat(newHtml).doesNotContain("https://www.example.org/")
        assertThat(newHtml).contains("test1-")
        assertThat(newHtml).contains("test2-")

        assertThat(scripts).hasSize(2)
        assertThat(scripts.map { it.url }).containsExactly(
            "https://www.example.org/test1.js?ver=5.2.5",
            "https://example.com/test2.js?ver=1.0.0"
        )

        scripts.forEach { assertThat(newHtml).contains(it.filename) }
    }

    @Test
    fun findAndUpdateScripts_removes_sri() {
        val htmlScripts = """
            <script crossorigin="anonymous" integrity="sha512-W...A==" type='text/javascript' src='https://www.example.org/test1.js?ver=5.2.5'></script>
        """.trimIndent()

        val document = Jsoup.parse(htmlScripts, "https://example.com")
        val scripts = htmlDownloader.findAndUpdateScripts(document)

        assertThat(scripts).hasSize(1)

        val newHtml = document.toString()
        assertThat(newHtml).doesNotContain("crossorigin")
        assertThat(newHtml).doesNotContain("integrity")
    }

    @Test
    fun findAndUpdateImages() {
        val htmlScripts = """
            <img src="https://www.example.org/test1.gif">
            <img src="test2.png">
        """.trimIndent()

        val document = Jsoup.parse(htmlScripts, "https://example.com")
        val iamges = htmlDownloader.findAndUpdateImages(document)
        val newHtml = document.toString()
        assertThat(newHtml).doesNotContain("https://www.example.org/")
        assertThat(newHtml).contains("test1-")
        assertThat(newHtml).contains("test2-")

        assertThat(iamges).hasSize(2)
        assertThat(iamges.map { it.url }).containsExactly(
            "https://www.example.org/test1.gif",
            "https://example.com/test2.png"
        )

        iamges.forEach { assertThat(newHtml).contains(it.filename) }
    }

    @Test
    fun findAndUpdateInputImages() {
        val htmlScripts = """
            <input type="image" src="https://www.example.org/test1.gif" alt="Submit">
            <input type="image" src="test2.png" alt="Submit">
        """.trimIndent()

        val document = Jsoup.parse(htmlScripts, "https://example.com")
        val images = htmlDownloader.findAndUpdateInputImages(document)
        val newHtml = document.toString()
        assertThat(newHtml).doesNotContain("https://www.example.org/")
        assertThat(newHtml).contains("test1-")
        assertThat(newHtml).contains("test2-")

        assertThat(images).hasSize(2)
        assertThat(images.map { it.url }).containsExactly(
            "https://www.example.org/test1.gif",
            "https://example.com/test2.png"
        )

        images.forEach { assertThat(newHtml).contains(it.filename) }
    }
}