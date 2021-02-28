package com.iqiyi.android.qigsaw.core.splitrequest.splitinfo

import net.dongliu.apk.parser.ApkFile
import java.io.File

class SplitCache(val info: SplitInfo, apk: SplitInfo.ApkData) {
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