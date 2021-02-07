package com.iqiyi.android.qigsaw.core.common

import android.app.Application
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 执行升级时，先把所有apk复制到cache中，然后会执行卸载流程。
 * 新安装时，先从缓存中找MD5相同的apk。
 * 这样可以解决新版本仅变动了一个模块，但其它模块也要升级而重新下载的问题。
 * */
object QgsawSplitCache {
    const val LOCAL_HOST = "http://127.0.0.1/"
    private lateinit var context: Application
    fun init(application: Application) {
        context = application
    }

    private fun getCacheDir(): File {
        return context.splitCacheDir()
    }

    @JvmStatic
    fun cacheUrl(url: String): String {
        if (url.startsWith("http")) {
            val cacheReg = "-([a-z0-9]{32}\\.apk$)".toRegex()//'md5.apk'
            val cacheName = cacheReg.find(url)?.groupValues?.get(1)
            if (cacheName != null) {
                val cache = File(getCacheDir(), cacheName)
                return LOCAL_HOST + cache.absolutePath
            }
        }
        return url
    }
}

fun Context.splitCacheDir(): File {
    return File(cacheDir, "QgsawSplitCache")
}