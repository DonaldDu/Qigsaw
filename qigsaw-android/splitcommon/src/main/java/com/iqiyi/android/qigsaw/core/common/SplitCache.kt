package com.iqiyi.android.qigsaw.core.common

import android.content.Context
import java.io.File

/**
 * 执行升级时，先把所有apk复制到cache中，然后会执行卸载流程。
 * 新安装时，先从缓存中找MD5相同的apk。
 * 这样可以解决新版本仅变动了一个模块，但其它模块也要升级而重新下载的问题。
 *
 * splitCacheDir 中存放的文件都是完整的，能直接用的，不能保存不完整或下载中的。
 * */
fun Context.splitCacheDir(): File {
    return File(cacheDir, "QgsawSplitCache")
}