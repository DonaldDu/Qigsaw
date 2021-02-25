package com.liulishuo.okdownload.core.file

import android.content.Context
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.breakpoint.DownloadStore

class CustomProcessFileStrategy : ProcessFileStrategy() {
    override fun createProcessStream(task: DownloadTask, info: BreakpointInfo, store: DownloadStore): MultiPointOutputStream {
        return CustomMultiPointOutputStream(task, info, store)
    }
}

/**
 * https://github.com/lingochamp/okdownload/issues/415
 * */
fun Context.fixOkDownloadError() {
    val download = OkDownload.Builder(this)
            .processFileStrategy(CustomProcessFileStrategy())
            .build()
    OkDownload.setSingletonInstance(download)
}