package com.diebietse.webpage.downloader

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HtmlUtilTest {

    @Test
    fun findAndUpdateSrc() {
    }

    @Test
    fun urlToFileName_always_unique() {
        val fileNames = mutableSetOf<String>()
        fileNames.add(HtmlUtil.urlToFileName("https://example.org/style.min.css?ver=1.0.0"))
        fileNames.add(HtmlUtil.urlToFileName("https://example.org/style.min.css?ver=1.0.1"))
        fileNames.add(HtmlUtil.urlToFileName("https://example.org/style.min.css"))
        fileNames.add(HtmlUtil.urlToFileName("http://example.org/style.min.css"))
        fileNames.add(HtmlUtil.urlToFileName("https://example.com/style.min.css"))

        assertThat(fileNames).hasSize(5)
    }

    @Test
    fun urlToFileName_long_names_keep_hash_and_extension() {
        val longCssFile = "${"a".repeat(200)}.css"
        val longCssFileName = HtmlUtil.urlToFileName(longCssFile)

        assertThat(longCssFileName).endsWith(".css")
        assertThat("""-\d*\.""".toRegex().containsMatchIn(longCssFileName)).isTrue()

        val longExtension = "a".repeat(210)
        val longExtensionFileName = HtmlUtil.urlToFileName(longExtension)

        assertThat("""-\d*\.""".toRegex().containsMatchIn(longExtensionFileName)).isTrue()
    }

    @Test
    fun urlToFileName_strips_leading_url_and_params() {
        val fileName = HtmlUtil.urlToFileName("https://example.org/style.min.css?ver=1.0.0")

        assertThat(fileName).doesNotContain("https://example.org")
        assertThat(fileName).doesNotContain("ver=1.0.0")
    }

    @Test
    fun urlToFileName_removes_special_chars() {
        val fileName = HtmlUtil.urlToFileName("$#%^&*.css")

        assertThat("""[$#%^&*]""".toRegex().containsMatchIn(fileName)).isFalse()
    }

    @Test
    fun parseCssForUrlAndImport() {
        val css = """
            @import url("test1.css");
            @import url(test2.css);
            @import url(http://example.com/test3/css);
            @import url('test4.css?v1.0.0');
            @import "test5.css";
            @import 'test6.css?v1.0.0';
        """.trimIndent()
        val cssAndLinks =
            HtmlUtil.parseCssForUrlAndImport(css, "https://www.example.org/index.html")
        assertThat(cssAndLinks.cssToDownload).hasSize(6)
        assertThat(cssAndLinks.cssToDownload.map { it.url }).containsExactly(
            "https://www.example.org/test1.css",
            "https://www.example.org/test2.css",
            "http://example.com/test3/css",
            "https://www.example.org/test4.css?v1.0.0",
            "https://www.example.org/test5.css",
            "https://www.example.org/test6.css?v1.0.0"
        )
    }

    @Test
    fun parseCssForLinks_same_name() {
        val css = """
            @import url("test.css");
            @import url('https://www.example.org/test.css');
        """.trimIndent()
        val cssAndLinks =
            HtmlUtil.parseCssForUrlAndImport(css, "https://www.example.org/index.html")
        assertThat(cssAndLinks.cssToDownload).hasSize(1)
        assertThat(cssAndLinks.cssToDownload.map { it.url }).containsExactly("https://www.example.org/test.css")
    }

    @Test
    fun parseCssForLinks_url() {
        val css = """
            .class1 { background: url("test1.jpg") #00D no-repeat fixed;}
            .class2 { background: url(data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==) no-repeat fixed; }
            .class3 { background: url('test3.jpg') fixed;}
            .class4 { background: url(test4.jpg) no-repeat;}
            ul { list-style: square url(http://example.com/test5.jpg); }
        """.trimIndent()
        val cssAndLinks =
            HtmlUtil.parseCssForUrlAndImport(css, "https://www.example.org/index.html")
        assertThat(cssAndLinks.filesToDownload).hasSize(4)
        assertThat(cssAndLinks.filesToDownload.map { it.url }).containsExactly(
            "https://www.example.org/test1.jpg",
            "https://www.example.org/test3.jpg",
            "https://www.example.org/test4.jpg",
            "http://example.com/test5.jpg"
        )
    }
}