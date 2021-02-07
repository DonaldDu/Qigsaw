package com.iqiyi.android.qigsaw.core.splitrequest.splitinfo

import android.content.Context
import com.iqiyi.android.qigsaw.core.common.SplitLog
import com.iqiyi.android.qigsaw.core.common.splitCacheDir
import net.dongliu.apk.parser.ApkFile
import org.apache.commons.io.FileUtils
import java.io.File

internal fun Context.updateDefaultSplitInfo(defaultSplitInfo: File, newSplitInfo: File) {
    if (defaultSplitInfo.exists()) {
        val old = defaultSplitInfo.readText().toSplitDetails()
        val new = newSplitInfo.readText().toSplitDetails()
        val caches = new.getSplitCaches(this, old)
        if (caches.isNotEmpty()) copyInstalledToCache(this, caches)
    }
    FileUtils.copyFile(newSplitInfo, defaultSplitInfo)
}

private fun String.toSplitDetails(): SplitDetails {
    return SplitInfoManagerImpl.parseSplitsContent(this)
}

/**
 * get apk if apk_md5 is diffrent
 * */
private fun SplitDetails.getSplitCaches(context: Context, old: SplitDetails): List<SplitCache> {
    val caches: MutableList<SplitCache> = mutableListOf()
    old.splitInfoListing.splitInfoMap.values.forEach { oldSplit ->
        try {
            val newSplit = splitInfoListing.splitInfoMap[oldSplit.splitName]
            if (newSplit != null && newSplit.appVersion != oldSplit.appVersion) {
                val newApks = newSplit.getApkDataList(context)
                val oldApks = oldSplit.getApkDataList(context)
                newApks.forEach { new ->
                    val oldApk = oldApks.find { apk -> apk.md5 == new.md5 }
                    if (oldApk != null) caches.add(SplitCache(oldSplit, oldSplit.splitName, oldApk.abi, oldApk.md5))
                }
            }

        } catch (e: Exception) {
            SplitLog.w(SplitUpdateService.TAG, "failed to update Split cache: %s", e)
        }
    }
    return caches
}

private fun copyInstalledToCache(context: Context, caches: List<SplitCache>) {
    val splitCacheDir = context.splitCacheDir()
    caches.forEach {
        val root = SplitPathManager.require().getSplitRootDir(it.info)
        val apk = it.findApk(root)
        if (apk != null) {
            val cache = File(splitCacheDir, "${it.md5}.apk")
            if (!cache.exists()) FileUtils.copyFile(apk, cache)
        }
    }
}

internal data class SplitCache(val info: SplitInfo, val split: String, val abi: String, val md5: String) {
    fun findApk(file: File): File? {
        return if (file.isDirectory) {
            file.listFiles()?.forEach {
                val apk = findApk(it)
                if (apk != null) return apk
            }
            null
        } else {
            if (file.name.endsWith(".apk") && file.matchAbi()) file else null
        }
    }

    private fun File.matchAbi(): Boolean {
        return try {
            val apk = ApkFile(this)
            apk.apkMeta.apply { apk.close() }.abi == abi
        } catch (e: Exception) {
            false
        }
    }
}