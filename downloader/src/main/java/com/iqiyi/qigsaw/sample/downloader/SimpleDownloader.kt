package com.iqiyi.qigsaw.sample.downloader

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
class SimpleDownloader(private val threshold: Long = 10 * 1024 * 1024, private val progressInterval: Int = 150) : Downloader {
    private val downloadTasks: MutableMap<Int, Array<DownloadTask>> = mutableMapOf()
    override fun startDownload(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback) {
        val tasks = requests.map { it.toTask(false, false) }.toTypedArray()
        DownloadTask.enqueue(tasks, TaskDownloadCallBack(sessionId, callback))
        downloadTasks[sessionId] = tasks
    }

    override fun deferredDownload(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback, usingMobileDataPermitted: Boolean) {
        val tasks = requests.map { it.toTask(true, !usingMobileDataPermitted) }.toTypedArray()
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
        return DownloadTask.Builder(url, fileDir, fileName)
                .setPriority(priority)
                .setMinIntervalMillisCallbackProcess(progressInterval)
                .setWifiRequired(wifiRequired)
                .build()
    }

    private inner class TaskDownloadCallBack(private val sessionId: Int, private val callback: DownloadCallback) : DownloadListener3() {
        override fun retry(task: DownloadTask, cause: ResumeFailedCause) {

        }

        override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {

        }

        override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
            callback.onProgress(currentOffset)
        }

        override fun started(task: DownloadTask) {
            callback.onStart()
        }

        override fun completed(task: DownloadTask) {
            downloadTasks.remove(sessionId)
            callback.onCompleted()
        }

        override fun canceled(task: DownloadTask) {
            downloadTasks.remove(sessionId)
            callback.onCanceled()
        }

        override fun error(task: DownloadTask, e: Exception) {
            callback.onError(e.hashCode())
        }

        override fun warn(task: DownloadTask) {
        }
    }
}