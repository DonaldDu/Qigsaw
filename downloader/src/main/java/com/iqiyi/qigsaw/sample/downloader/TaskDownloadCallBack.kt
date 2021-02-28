package com.iqiyi.qigsaw.sample.downloader

import android.util.Log
import com.iqiyi.android.qigsaw.core.splitdownload.DownloadCallback
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener2

internal class TaskDownloadCallBack(val downloader: SimpleDownloader, private val sessionId: Int, private val callback: DownloadCallback) : SimpleDownloadListener() {
    private val log = downloader.log
    private val downloadTasks = downloader.downloadTasks
    private val TAG = "DownloadCallback"
    override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
        task.progress.currentOffset = currentOffset
        if (log) Log.d(TAG, "connected: ---------------")
    }

    override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
        task.progress.currentOffset = currentOffset
        callback.onProgress(bunchProgress())
        if (log) Log.d(TAG, "progress: ---------------,totalLength $totalLength:$currentOffset finish " + (currentOffset == totalLength))
    }

    private fun bunchProgress(): Long {
        return downloadTasks.getValue(sessionId).sumOf { it.progress.currentOffset }
    }

    private var started = false
    override fun started(task: DownloadTask) {
        if (!started) {
            started = true
            callback.onStart()
        }
        if (log) Log.d(TAG, "started: --------------- ${task.url}")
    }

    override fun completed(task: DownloadTask) {
        task.progress.completed = true
        if (isCompleted()) {
            downloadTasks.remove(sessionId)
            callback.onCompleted()
        }
        if (log) Log.d(TAG, "onCompleted: ---------------")
    }

    private fun isCompleted(): Boolean {
        return downloadTasks.getValue(sessionId).all { it.progress.completed }
    }

    override fun canceled(task: DownloadTask) {
        downloadTasks.remove(sessionId)
        callback.onCanceled()
        if (log) Log.d(TAG, "canceled: ---------------")
    }

    override fun error(task: DownloadTask, e: Exception) {
        if (log) e.printStackTrace()
        if (log) Log.d(TAG, "error: ---------------")
        if (task.progress.retryCount-- > 0) {
            task.enqueue(this)
        } else {
            downloadTasks.remove(sessionId)?.forEach { it.replaceListener(emptyDownloadListener) }
            callback.onError(e.hashCode())
        }
    }

    companion object {
        private val emptyDownloadListener = object : DownloadListener2() {
            override fun taskStart(task: DownloadTask) {}
            override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?) {}
        }
    }
}

internal var DownloadTask.progress: DownloadProgress
    get() {
        return tag as DownloadProgress
    }
    set(value) {
        tag = value
    }

internal data class DownloadProgress(var currentOffset: Long = 0, var completed: Boolean = false, var retryCount: Int = 3)