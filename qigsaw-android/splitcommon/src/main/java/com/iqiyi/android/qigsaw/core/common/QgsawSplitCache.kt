package com.iqiyi.android.qigsaw.core.common

import android.content.Context
import java.io.File

/**
 * 执行升级时，先把所有apk复制到cache中，然后会执行卸载流程。
 * 新安装时，先从缓存中找MD5相同的apk。
 * 这样可以解决新版本仅变动了一个模块，但其它模块也要升级而重新下载的问题。
 * */
object QgsawSplitCache {
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, "QgsawSplitCache")
    }
}

fun Context.splitCacheDir(): File {
    return File(cacheDir, "QgsawSplitCache")
}