package com.diebietse.webpage.downloader.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.diebietse.webpage.downloader.example.databinding.ActivityWebBinding

class WebActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebBinding

    companion object {
        fun start(activity: Activity, url: String) {
            val intent = Intent(activity, WebActivity::class.java)
            intent.putExtra("url", url)
            activity.startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra("url")
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.allowFileAccess = true
        binding.webView.loadUrl(url!!)
    }
}
