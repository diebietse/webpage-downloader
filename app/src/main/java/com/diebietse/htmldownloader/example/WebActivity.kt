package com.diebietse.htmldownloader.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_web.*

class WebActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_web)

        val url = intent.getStringExtra("url")
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
    }
}
