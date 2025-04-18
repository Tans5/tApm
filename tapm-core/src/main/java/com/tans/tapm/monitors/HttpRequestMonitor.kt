package com.tans.tapm.monitors

import android.os.SystemClock
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.HttpRequest
import com.tans.tapm.tApm
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http.promisesBody
import okhttp3.internal.toHostHeader
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.mutableMapOf

class HttpRequestMonitor : AbsMonitor<HttpRequest>(2000L) {

    override val isSupport: Boolean
        get() = this.apm.get() != null

    override fun onInit(apm: tApm) {

    }

    override fun onStart(apm: tApm) {
        attachMonitor(this)
        tApmLog.d(TAG, "HttpRequestMonitor started.")
    }

    override fun onStop(apm: tApm) {
        detachMonitor(this)
        tApmLog.d(TAG, "HttpRequestMonitor stopped.")
    }

    private fun httpRequestIntercept(chain: Interceptor.Chain): Response {
        val requesting = Requesting(
            startTime = System.currentTimeMillis(),
        )
        val requestStartTime = SystemClock.uptimeMillis()
        var requestHeaderEndTime: Long = -1L
        var requestBodyEndTime: Long = -1L
        return try {
            val realRequest = chain.request()
            requesting.method = realRequest.method

            // Url
            val url = realRequest.url.let {
                it.toHostHeader()
                "${it.scheme}://${it.host}:${it.port}${it.encodedPath}"
            }
            requesting.url = url

            // Query params
            val queryParams = mutableMapOf<String, List<String?>>()
            realRequest.url.let {
                for (name in it.queryParameterNames) {
                    queryParams[name] = it.queryParameterValues(name)
                }
            }
            requesting.queryParams = queryParams

            // Request Header
            requesting.requestHeader = realRequest.headers.toMultimap()

            // Request body
            val realRequestBody = realRequest.body
            var wrapperRequest: Request
            requesting.requestBodyContentType = realRequestBody?.contentType()?.toString()
            requesting.requestBodyContentLength = realRequestBody?.contentLength()
            if (realRequestBody != null && realRequestBody.contentLength() > 0) {
                val wrapperRequestBody = WrapperRequestBody(
                    realRequestBody = realRequestBody,
                    bodyWriteStart = { requestBodyText ->
                        requesting.requestBodyText = requestBodyText
                        requestHeaderEndTime = SystemClock.uptimeMillis()
                        requesting.requestHeaderCostInMillis = requestHeaderEndTime - requestStartTime
                    },
                    bodyWriteEnd = {
                        requestBodyEndTime = SystemClock.uptimeMillis()
                        requesting.requestBodyCostInMillis = requestBodyEndTime - requestHeaderEndTime
                    }
                )
                wrapperRequest = realRequest.newBuilder()
                    .method(realRequest.method, wrapperRequestBody)
                    .build()
            } else {
                wrapperRequest = realRequest
            }
            val realResponse = chain.proceed(chain.request())

            // Response
            requesting.isHttpSuccess = realResponse.isSuccessful
            requesting.responseHeader = realResponse.headers.toMultimap()
            requesting.responseCode = realResponse.code
            requesting.responseBodyContentType = realResponse.body?.contentType()?.toString()
            requesting.responseBodyContentLength = realResponse.body?.contentLength()
            val realResponseBody = realResponse.body
            val wrapperResponse: Response
            if (realResponseBody != null && realResponseBody.contentLength() > 0) {
                val isGzipEncoding = "gzip".equals(realResponse.header("Content-Encoding"), ignoreCase = true) && realResponse.promisesBody()
                wrapperResponse = realResponse.newBuilder()
                    .body(
                        WrapperResponseBody(
                            realResponseBody = realResponseBody,
                            requesting = requesting,
                            requestStartTime = requestStartTime,
                            requestHeaderEndTime = requestHeaderEndTime,
                            requestBodyEndTime = requestBodyEndTime,
                            isGzipEncoding = isGzipEncoding
                        )
                    )
                    .build()
            } else {
                dispatchHttpTimeCost(requesting)
                wrapperResponse = realResponse
            }
            wrapperResponse
        } catch (e: Throwable) {
            requesting.error = e
            dispatchHttpTimeCost(requesting)
            throw e
        }
    }

    private data class Requesting(
        val startTime: Long,
        var method: String = "",
        var url: String = "",

        var queryParams: Map<String, List<String?>> = emptyMap(),
        var requestHeader: Map<String, List<String>> = emptyMap(),
        var requestBodyContentType: String? = null,
        var requestBodyContentLength: Long? = null,
        var requestBodyText: String? = null,
        var requestHeaderCostInMillis: Long? = null,
        var requestBodyCostInMillis: Long? = null,

        var responseHeader: Map<String, List<String>>? = null,
        var responseCode: Int? = null,
        var responseBodyContentType: String? = null,
        var responseBodyContentLength: Long? = null,
        var responseBodyText: String? = null,
        var isHttpSuccess: Boolean? = null,
        var responseHeaderCostInMillis: Long? = null,
        var responseBodyCostInMillis: Long? = null,

        var httpRequestCostInMillis: Long = 0,
        var error: Throwable? = null
    )

    private inner class WrapperRequestBody(
        private val realRequestBody: RequestBody,
        private val bodyWriteStart: (textBody: String?) -> Unit,
        private val bodyWriteEnd: () -> Unit
    ) : RequestBody() {

        override fun contentType(): MediaType? = realRequestBody.contentType()

        override fun writeTo(sink: BufferedSink) {
            if (realRequestBody.contentType().isTextType()) {
                val buffer = Buffer()
                realRequestBody.writeTo(buffer)
                val s = buffer.peek().readUtf8()
                bodyWriteStart(s)
                sink.writeAll(buffer)
                bodyWriteEnd()
            } else {
                bodyWriteStart(null)
                realRequestBody.writeTo(sink)
                bodyWriteEnd()
            }
        }
    }

    private inner class WrapperResponseBody(
        val realResponseBody: ResponseBody,
        val requesting: Requesting,
        val requestStartTime: Long,
        val requestHeaderEndTime: Long,
        val requestBodyEndTime: Long,
        val isGzipEncoding: Boolean
    ) : ResponseBody() {

        private val bodyBuffer: ByteArray? = if (realResponseBody.contentType().isTextType()) {
            ByteArray(contentLength().toInt())
        } else {
            null
        }

        private var isRecordBodyStart = false
        private var isRecordBodyEnd = false

        private var responseBodyStartTime = -1L

        private var readSize = 0

        override fun contentLength(): Long = realResponseBody.contentLength()

        override fun contentType(): MediaType? = realResponseBody.contentType()

        override fun source(): BufferedSource {
            return WrapperInputStream(realResponseBody.source().inputStream()).source().buffer()
        }

        private fun dispatchStart() {
            if (!isRecordBodyStart) {
                isRecordBodyStart = true
                responseBodyStartTime = SystemClock.uptimeMillis()
                if (requestBodyEndTime != -1L) {
                    requesting.responseHeaderCostInMillis = responseBodyStartTime - requestBodyEndTime
                }
            }
        }

        private fun dispatchEnd(e: Throwable?) {
            if (!isRecordBodyEnd) {
                isRecordBodyEnd = true
                val now = SystemClock.uptimeMillis()
                requesting.responseBodyCostInMillis = now - responseBodyStartTime
                requesting.httpRequestCostInMillis = now - requestStartTime
                if (bodyBuffer != null && readSize.toLong() == contentLength()) {
                    if (isGzipEncoding) {
                        requesting.responseBodyText = try {
                            GzipSource(ByteArrayInputStream(bodyBuffer).source()).use {
                                it.buffer().readUtf8()
                            }
                        } catch (e: Throwable) {
                            tApmLog.e(TAG, "Read gzip fail: ${e.message}", e)
                            null
                        }
                    } else {
                        requesting.responseBodyText = bodyBuffer.toString(Charsets.UTF_8)
                    }
                }
                requesting.error = e
                dispatchHttpTimeCost(requesting)
            }
        }

        private inner class WrapperInputStream(val realInputStream: InputStream) : InputStream() {
            override fun read(): Int {
                if (!isRecordBodyStart) {
                    dispatchStart()
                }
                val readResult = try {
                    realInputStream.read()
                } catch (e: Throwable) {
                    dispatchEnd(e)
                    throw e
                }
                if (readResult == -1 || readSize >= contentLength()) {
                    dispatchEnd(null)
                } else {
                    if (bodyBuffer != null) {
                        bodyBuffer[readSize] = readResult.toByte()
                    }
                    readSize ++
                    // TODO: Record read size.
                }
                return readResult
            }
        }
    }

    private fun dispatchHttpTimeCost(requesting: Requesting) {
        dispatchMonitorData(
            HttpRequest.TimeCost(
                startTime = requesting.startTime,
                method = requesting.method,
                url = requesting.url,
                queryParams = requesting.queryParams,
                requestHeader = requesting.requestHeader,
                requestBodyContentType = requesting.requestBodyText,
                requestBodyContentLength = requesting.requestBodyContentLength,
                requestBodyText = requesting.requestBodyText,
                requestHeaderCostInMillis = requesting.requestHeaderCostInMillis,
                requestBodyCostInMillis = requesting.requestBodyCostInMillis,
                responseHeader = requesting.responseHeader,
                responseCode = requesting.responseCode,
                responseBodyContentType = requesting.responseBodyContentType,
                responseBodyContentLength = requesting.responseBodyContentLength,
                responseBodyText = requesting.responseBodyText,
                isHttpSuccess = requesting.isHttpSuccess,
                responseHeaderCostInMillis = requesting.responseHeaderCostInMillis,
                responseBodyCostInMillis = requesting.responseBodyCostInMillis,
                httpRequestCostInMillis = requesting.httpRequestCostInMillis,
                error = requesting.error
            )
        )
    }

    private fun MediaType?.isTextType(): Boolean {
        return if (this != null) {
            val type = toString().lowercase()
            when {
                type.startsWith("application/json") -> true
                type.startsWith("application/yaml") -> true
                type.startsWith("text") -> true
                else -> false
            }
        } else {
            false
        }
    }

    companion object : Interceptor {

        private const val TAG = "HttpRequestMonitor"

        private val monitor: AtomicReference<HttpRequestMonitor?> by lazy {
            AtomicReference(null)
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val m = monitor.get()
            return m?.httpRequestIntercept(chain) ?: chain.proceed(chain.request())
        }

        private fun attachMonitor(m: HttpRequestMonitor) {
            monitor.compareAndSet(null, m)
        }

        private fun detachMonitor(m: HttpRequestMonitor) {
            monitor.compareAndSet(m, null)
        }
    }
}