package com.liulishuo.okdownload.core.file

import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.Util
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.breakpoint.DownloadStore
import java.io.IOException

class CustomMultiPointOutputStream internal constructor(private val task: DownloadTask,
                                                        info: BreakpointInfo,
                                                        store: DownloadStore,
                                                        syncRunnable: Runnable?) : MultiPointOutputStream(task, info, store, syncRunnable) {
    constructor(task: DownloadTask,
                info: BreakpointInfo,
                store: DownloadStore) : this(task, info, store, null)

    @Synchronized
    @Throws(IOException::class)
    public override fun close(blockIndex: Int) {
        val outputStream = outputStreamMap[blockIndex]
        if (outputStream != null) {
            outputStream.close()
            synchronized(noSyncLengthMap) {
                // make sure the length of noSyncLengthMap is equal to outputStreamMap
                outputStreamMap.remove(blockIndex)
                noSyncLengthMap.remove(blockIndex)
            }
            Util.d(TAG, "OutputStream close task[" + task.id + "] block[" + blockIndex + "]")
        }
    }

    private val TAG = "CustomMultiPointOutputStream"
}