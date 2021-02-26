package com.iqiyi.qigsaw.sample.downloader

import com.iqiyi.android.qigsaw.core.splitdownload.DownloadCallback
import com.iqiyi.android.qigsaw.core.splitdownload.DownloadRequest
import com.iqiyi.android.qigsaw.core.splitdownload.Downloader
import com.liulishuo.okdownload.DownloadTask

/**
 * @param threshold default is 10MB
 * @param progressInterval minIntervalMillisCallbackProgress
 * */
class SimpleDownloader(private val threshold: Long = 10 * 1024 * 1024, private val progressInterval: Int = 150, internal val log: Boolean = false) : Downloader {
    internal val downloadTasks: MutableMap<Int, Array<DownloadTask>> = mutableMapOf()
    override fun startDownload(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback) {
        download(sessionId, requests, callback, false, false)
    }

    override fun deferredDownload(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback, usingMobileDataPermitted: Boolean) {
        download(sessionId, requests, callback, true, !usingMobileDataPermitted)
    }

    private fun download(sessionId: Int, requests: MutableList<DownloadRequest>, callback: DownloadCallback, deferred: Boolean, wifiRequired: Boolean) {
        val tasks = requests.map { it.toTask(deferred, wifiRequired) }.toTypedArray()
        DownloadTask.enqueue(tasks, TaskDownloadCallBack(this, sessionId, callback))
        downloadTasks[sessionId] = tasks
    }

    override fun cancelDownloadSync(sessionId: Int): Boolean {
        val tasks = downloadTasks.remove(sessionId)
        if (tasks != null) DownloadTask.cancel(tasks)
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
                .setConnectionCount(1)//fixError https://github.com/lingochamp/okdownload/issues/415
                .setPriority(priority)
                .setMinIntervalMillisCallbackProcess(progressInterval)
                .setWifiRequired(wifiRequired)
                .build()
        task.progress = DownloadProgress()
        return task
    }
}