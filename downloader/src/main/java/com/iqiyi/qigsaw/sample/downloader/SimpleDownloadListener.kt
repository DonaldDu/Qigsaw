package com.iqiyi.qigsaw.sample.downloader

import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3

abstract class SimpleDownloadListener : DownloadListener3() {
    override fun retry(task: DownloadTask, cause: ResumeFailedCause) {}

    override fun warn(task: DownloadTask) {}
}