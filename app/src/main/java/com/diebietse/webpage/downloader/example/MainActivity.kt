package com.diebietse.webpage.downloader.example

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.diebietse.webpage.downloader.example.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var downloadsAdapter: DownloadsAdapter
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            MaterialDialog(view.context).show {
                title(R.string.title)
                input(inputType = InputType.TYPE_TEXT_VARIATION_URI) { _, url ->
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
        binding.content.recyclerView.adapter = downloadsAdapter
        updateViews(downloads)
    }

    private fun download(url: String) {
        lifecycleScope.launch {
            binding.content.progressBar.visibility = View.VISIBLE
            viewModel.download(url)
            binding.content.progressBar.visibility = View.GONE
            updateViews(viewModel.listDownloads())
        }
    }

    private fun updateViews(downloads: List<Download>) {
        if (downloads.isEmpty()) {
            binding.content.recyclerView.visibility = View.GONE
            binding.content.textView.visibility = View.VISIBLE
        } else {
            binding.content.recyclerView.visibility = View.VISIBLE
            binding.content.textView.visibility = View.GONE
        }
        downloadsAdapter.updateDownloads(downloads)
    }
}