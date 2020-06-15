package com.diebietse.htmldownloader.example

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var downloadsAdapter: DownloadsAdapter
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            MaterialDialog(view.context).show {
                title(R.string.title)
                input(
                    hintRes = R.string.example_url,
                    inputType = InputType.TYPE_TEXT_VARIATION_URI
                ) { _, url ->
                    download(url.toString())
                }
                positiveButton(R.string.download)
            }
        }

        val downloads = viewModel.listDownloads()
        downloadsAdapter =
            DownloadsAdapter(downloads, object : DownloadsAdapter.OnClickListener {
                override fun onClick(pageId: String) {
                    val url = viewModel.offlineUrl(pageId)
                    WebActivity.start(this@MainActivity, url)
                }

                override fun onLongClick(pageId: String) {
                    MaterialDialog(this@MainActivity).show {
                        title(R.string.remove_download_title)
                        message(R.string.remove_download_message)
                        positiveButton(R.string.remove_download_yes) {
                            viewModel.removeDownload(pageId)
                            updateViews(viewModel.listDownloads())
                        }
                        negativeButton(R.string.remove_download_no)
                    }
                }
            })
        recyclerView.adapter = downloadsAdapter
        updateViews(downloads)
    }

    private fun download(url: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            viewModel.download(url)
            progressBar.visibility = View.GONE
            updateViews(viewModel.listDownloads())
        }
    }

    private fun updateViews(downloads: List<Download>) {
        if (downloads.isEmpty()) {
            recyclerView.visibility = View.GONE
            textView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            textView.visibility = View.GONE
        }
        downloadsAdapter.updateDownloads(downloads)
    }
}