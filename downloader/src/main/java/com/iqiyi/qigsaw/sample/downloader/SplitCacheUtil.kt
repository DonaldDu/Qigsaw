package com.iqiyi.qigsaw.sample.downloader

import android.content.Context
import com.iqiyi.android.qigsaw.core.common.splitCacheDir
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.core.connection.DownloadOkHttp3Connection
import okhttp3.*
import okio.buffer
import okio.source
import java.io.File

/**
 * 新安装时，先从缓存中找MD5相同的apk。
 * */
object SplitCacheUtil {
    fun initOkDownload(context: Context) {
        val cf = DownloadOkHttp3Connection.Factory()
        cf.builder().addInterceptor {
            it.request().cacheResponse(context) ?: it.proceed(it.request())
        }
        val builder = OkDownload.Builder(context).connectionFactory(cf)
        OkDownload.setSingletonInstance(builder.build())
    }

    private fun Request.cacheResponse(context: Context): Response? {
        val url = url.toString()
        if (url.startsWith("http")) {
            val cacheReg = "-([a-z0-9]{32}\\.apk$)".toRegex()//'md5.apk'
            val cacheName = cacheReg.find(url)?.groupValues?.get(1)
            if (cacheName != null) {
                val cache = File(context.splitCacheDir(), cacheName)
                return if (cache.exists()) toResponse(cache) else null
            }
        }
        return null
    }

    private fun Request.toResponse(file: File): Response {
        return Response.Builder()
                .code(200)
                .message("file response")
                .protocol(Protocol.HTTP_1_0)
                .request(this)
                .body(file.asResponseBody())
                .build()
    }

    private fun File.asResponseBody(contentType: MediaType? = null): ResponseBody {
        val fileSource = source().buffer()
        return object : ResponseBody() {
            override fun contentType() = contentType
            override fun contentLength() = length()
            override fun source() = fileSource
        }
    }
}