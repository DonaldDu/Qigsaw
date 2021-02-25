package com.iqiyi.qigsaw.sample.downloader

import android.util.Log
import com.iqiyi.android.qigsaw.core.splitdownload.DownloadCallback
import com.iqiyi.android.qigsaw.core.splitdownload.DownloadRequest
import com.iqiyi.android.qigsaw.core.splitdownload.Downloader
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3

/**
 * @param threshold default is 10MB
 * @param progressInterval minIntervalMillisCallbackProgress
 * */
class SimpleDownloader(private val threshold: Long = 10 * 1024 * 1024, private val progressInterval: Int = 150, private val log: Boolean = false) : Downloader {
    private val downloadTasks: MutableMap<Int, Array<DownloadTask>> = mutableMapOf()
    override fun startDownload(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback) {
        download(sessionId, requests, callback, false, false)
    }

    override fun deferredDownload(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback, usingMobileDataPermitted: Boolean) {
        download(sessionId, requests, callback, true, !usingMobileDataPermitted)
    }

    private fun download(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback, deferred: Boolean, wifiRequired: Boolean) {
        val tasks = requests.map { it.toTask(deferred, wifiRequired) }.toTypedArray()
        DownloadTask.enqueue(tasks, TaskDownloadCallBack(sessionId, callback))
        downloadTasks[sessionId] = tasks
    }

    override fun cancelDownloadSync(sessionId: Int): Boolean {
        val tasks = downloadTasks.remove(sessionId)
        tasks?.forEach { it.cancel() }
        return tasks != null
    }

    override fun getDownloadSizeThresholdWhenUsingMobileData(): Long {
        return threshold
    }

    override fun isDeferredDownloadOnlyWhenUsingWifiData(): Boolean {
        return true
    }

    private fun DownloadRequest.toTask(deferred: Boolean, wifiRequired: Boolean): DownloadTask {
        val priority = if (deferred) 0 else 10
        val task = DownloadTask.Builder(url, fileDir, fileName)
                .setPriority(priority)
                .setMinIntervalMillisCallbackProcess(progressInterval)
                .setWifiRequired(wifiRequired)
                .build()
        task.tag = DownloadProgress()
        return task
    }

    private data class DownloadProgress(var currentOffset: Long = 0, var completed: Boolean = false)

    private val DownloadTask.progress: DownloadProgress
        get() {
            return tag as DownloadProgress
        }

    private inner class TaskDownloadCallBack(private val sessionId: Int, private val callback: DownloadCallback) : DownloadListener3() {
        val TAG = "DownloadCallback"
        override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            if (log) Log.d(TAG, "retry: ---------------")
        }

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
            e.printStackTrace()
            callback.onError(e.hashCode())
            if (log) Log.d(TAG, "error: ---------------")
        }

        override fun warn(task: DownloadTask) {
            if (log) Log.d(TAG, "warn: ---------------")
        }
    }
}