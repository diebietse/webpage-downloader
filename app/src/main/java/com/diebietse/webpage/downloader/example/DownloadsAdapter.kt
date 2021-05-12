package com.diebietse.webpage.downloader.example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class DownloadsAdapter(
    private var downloads: List<Download>,
    private val onClickListener: OnClickListener
) :
    RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView = view.findViewById<TextView>(R.id.folder)!!
    }

    interface OnClickListener {
        fun onClick(pageId: String)
        fun onLongClick(pageId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.view_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = downloads[position].url
        holder.textView.setOnClickListener {
            onClickListener.onClick(downloads[position].pageId)
        }
        holder.textView.setOnLongClickListener {
            onClickListener.onLongClick(downloads[position].pageId)
            true
        }
    }

    override fun getItemCount() = downloads.size

    fun updateDownloads(newDownloads: List<Download>) {
        val diffResults = DiffUtil.calculateDiff(DiffUtilCallback(downloads, newDownloads))
        downloads = newDownloads
        diffResults.dispatchUpdatesTo(this)
    }

    class DiffUtilCallback(
        private val oldResults: List<Download>,
        private val newResults: List<Download>
    ) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldResults[oldItemPosition] == newResults[newItemPosition]
        }

        override fun getOldListSize(): Int {
            return oldResults.size
        }

        override fun getNewListSize(): Int {
            return newResults.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldResults[oldItemPosition] == newResults[newItemPosition]
        }
    }
}