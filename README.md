# WebpageDownloader


[![release][release-badge]][release-link]
[![ci][ci-badge]][ci-link]
![license][license-badge]

An Android and Kotlin library to download webpages for offline viewing.

It will download all assets required to render the chosen webpage and rewrite all links in the
webpage to use the downloaded assets

To use this in your application you will need to add jitpack to the repositories section of your
build.gradle file:

```groovy
allprojects {
    repositories {
        //...
        maven { url 'https://jitpack.io' }
    }
}
```

and then add it as a dependency:

```groovy
dependencies {
    implementation 'com.github.diebietse:webpage-downloader:<latest-release>'
}
```

You can then download a webpage (in a background thread) with something like:

```kotlin
val url = "https://en.wikipedia.org/wiki/Main_Page"
val downloadDir = File(application.filesDir, "downloads")
val pageId = url.hashCode().absoluteValue.toString()
WebpageDownloader().download(url, DefaultFileSaver(File(downloadDir, pageId)))
```

And render it again in a WebView with:

```kotlin
webView.settings.javaScriptEnabled = true
webView.loadUrl("file://${downloadDir}/${pageId}/index.html")
```

[release-badge]: https://jitpack.io/v/diebietse/webpage-downloader.svg
[release-link]: https://jitpack.io/#diebietse/webpage-downloader
[ci-badge]: https://github.com/diebietse/webpage-downloader/actions/workflows/android.yml/badge.svg
[ci-link]: https://github.com/diebietse/webpage-downloader/actions/workflows/android.yml
[license-badge]: https://img.shields.io/github/license/diebietse/webpage-downloader.svg