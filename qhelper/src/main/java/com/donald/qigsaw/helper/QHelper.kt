package com.donald.qigsaw.helper

import org.apache.commons.codec.binary.Hex
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipFile

object QHelper {
    @JvmStatic
    fun getMD5(file: File?): String? {
        return if (file == null || !file.exists()) {
            null
        } else try {
            val digest = MessageDigest.getInstance("MD5")
            if (file.name.endsWith(".apk")) {
                updateMD5WithApkFileList(digest, file)
            } else {
                updateMD5WithFileInputStream(digest, file)
            }
            Hex.encodeHexString(digest.digest())
        } catch (e: Exception) {
            null
        }
    }

    @Throws(FileNotFoundException::class)
    private fun updateMD5WithFileInputStream(digest: MessageDigest, file: File) {
        updateMD5(digest, FileInputStream(file))
    }

    @Throws(IOException::class)
    private fun updateMD5WithApkFileList(digest: MessageDigest, file: File) {
        val apk = ZipFile(file)
        val entries = apk.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val inputStream = apk.getInputStream(entry)
            updateMD5(digest, inputStream)
        }
    }

    private fun updateMD5(digest: MessageDigest, inputStream: InputStream) {
        val buffer = ByteArray(1024 * 1024 * 5) //5MB
        var read: Int
        try {
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for MD5", e)
        } finally {
            closeQuietly(inputStream)
        }
    }

    private fun closeQuietly(obj: Any?) {
        if (obj == null) return
        when (obj) {
            is Closeable -> {
                try {
                    obj.close()
                } catch (ignored: Throwable) {
                    // Ignored.
                }
            }
            is ZipFile -> {
                try {
                    obj.close()
                } catch (ignored: Throwable) {
                    // Ignored.
                }
            }
            else -> {
                throw IllegalArgumentException("obj: $obj cannot be closed.")
            }
        }
    }
}