package com.iqiyi.android.qigsaw.core.splitrequest.splitinfo

import android.content.Context
import com.iqiyi.android.qigsaw.core.common.SplitLog
import com.iqiyi.android.qigsaw.core.common.splitCacheDir
import net.dongliu.apk.parser.ApkFile
import org.apache.commons.io.FileUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 要求每次打包都升级组件版本（建议用时间秒数）
 * */
internal fun Context.updateDefaultSplitInfo(defaultSplitInfo: File, newSplitInfo: File) {
    if (defaultSplitInfo.exists()) {
        val oldJson = defaultSplitInfo.readText()
        val old = oldJson.toSplitDetails()
        val newJson = newSplitInfo.readText()
        val new = newJson.toSplitDetails()

        val caches = new.getSplitCaches(this, old)
        if (caches.isNotEmpty()) copyInstalledToCache(this, caches)

        val unchangedSplits = new.getUnchangedSplits(this, old)
        if (unchangedSplits.isNotEmpty()) {
            val fixed = keepUnchangedSplitVersion(newJson, oldJson, unchangedSplits)
            return FileUtils.writeByteArrayToFile(defaultSplitInfo, fixed.toByteArray())
        }
    }
    FileUtils.copyFile(newSplitInfo, defaultSplitInfo)
}

/**
 * 修正组件版本错误：如果[所有apk-md5]未变化，却更新了组件版本，则使用旧版本
 * */
private fun keepUnchangedSplitVersion(newSplitDetailsJson: String, oldSplitDetailsJson: String, unchangedSplits: List<String>): String {
    val oldSplits = JSONObject(oldSplitDetailsJson).getJSONArray("splits")
    val splitDetails = JSONObject(newSplitDetailsJson)
    val splits = splitDetails.getJSONArray("splits")
    for (i in 0 until splits.length()) {
        val split = splits.getJSONObject(i)
        val splitName = split.getString("splitName")
        if (unchangedSplits.contains(splitName)) {
            split.put("version", oldSplits.getOldSplitVersion(splitName))
        }
    }
    return splitDetails.toString()
}

/**
 * 获取 所有apk-md5相同，但splitVersion却有更新的组件名，以减少不必要更新
 * */
private fun SplitDetails.getUnchangedSplits(context: Context, old: SplitDetails): List<String> {
    val unchangedSplits: MutableList<String> = mutableListOf()
    splitInfoListing.splitInfoMap.keys.forEach { splitName ->
        val newSplitInfo = splitInfoListing.splitInfoMap[splitName]!!
        if (!newSplitInfo.splitVersion.contains("@")) {//不带‘@’的版本号为自动生成的，可能有未变动的组件。其它的都是有变动才更新。
            val oldSplitInfo = old.splitInfoListing.splitInfoMap[splitName]!!
            if (newSplitInfo.splitVersion != oldSplitInfo.splitVersion) {
                val newApks = newSplitInfo.getApkDataList(context).map { it.abi + it.md5 }
                val oldApks = oldSplitInfo.getApkDataList(context).map { it.abi + it.md5 }
                if (oldApks.containsAll(newApks)) unchangedSplits.add(splitName)
            }
        }
    }
    return unchangedSplits
}

private fun JSONArray.getOldSplitVersion(splitName: String): String {
    val i = (0 until length()).find { getJSONObject(it).getString("splitName") == splitName }!!
    return getJSONObject(i).getString("version")
}

private fun String.toSplitDetails(): SplitDetails {
    return SplitInfoManagerImpl.parseSplitsContent(this)
}

/**
 * 组件版本不同，但apk_md5相同的，可以缓存旧组件
 * */
private fun SplitDetails.getSplitCaches(context: Context, old: SplitDetails): List<SplitCache> {
    val caches: MutableList<SplitCache> = mutableListOf()
    splitInfoListing.splitInfoMap.keys.forEach { splitName ->
        try {
            val newSplit = splitInfoListing.splitInfoMap[splitName]!!
            val oldSplit = old.splitInfoListing.splitInfoMap[splitName]!!
            if (newSplit.splitVersion != oldSplit.splitVersion) {
                val newApks = newSplit.getApkDataList(context)
                val oldApks = oldSplit.getApkDataList(context)
                val unchanges = oldApks.intersect(newApks).map { SplitCache(oldSplit, it) }
                caches.addAll(unchanges)
            }
        } catch (e: Exception) {
            SplitLog.w(SplitUpdateService.TAG, "failed to update Split cache: %s", e)
        }
    }
    return caches
}

/**
 * 缓存已安装的旧组件apk
 * */
private fun copyInstalledToCache(context: Context, caches: List<SplitCache>) {
    val splitCacheDir = context.splitCacheDir()
    caches.forEach {
        val splitDir = SplitPathManager.require().getSplitDir(it.info)
        val apk = it.findApk(splitDir)
        if (apk != null) {
            val cache = File(splitCacheDir, "${it.md5}.apk")
            if (!cache.exists()) FileUtils.copyFile(apk, cache)
        }
    }
}

private class SplitCache(val info: SplitInfo, apk: SplitInfo.ApkData) {
    private val abi: String = apk.abi
    val md5: String = apk.md5
    val split: String = info.splitName
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