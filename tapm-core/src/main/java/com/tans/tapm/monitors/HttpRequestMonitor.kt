package com.tans.tapm.monitors

import android.os.Handler
import android.os.Message
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
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import okio.sink
import okio.source
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.mutableMapOf

class HttpRequestMonitor : AbsMonitor<HttpRequest>(DEFAULT_HTTP_REQUEST_SUMMARY_CALCULATE_INTERVAL) {

    override val isSupport: Boolean
        get() = this.apm.get() != null


    private val lastHttpSummaryRecordSnapshot: AtomicReference<HttpSummaryRecordSnapshot?> by lazy {
        AtomicReference(null)
    }
    private val calculateRequestSummaryHandler: Handler by lazy {
        object : Handler(executor.getBackgroundThreadLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CALCULATE_HTTP_REQUEST_SUMMARY_MSG -> {
                        val last = lastHttpSummaryRecordSnapshot.get()
                        val new = HttpSummaryRecordSnapshot(
                            time = System.currentTimeMillis(),
                            uptime = SystemClock.uptimeMillis(),
                            uploadDataSize = uploadDataSize.get(),
                            downloadDataSize = downloadDataSize.get()
                        )
                        if (last == null) {
                            lastHttpSummaryRecordSnapshot.set(new)
                        } else {
                            // bytes pre second
                            val downloadSpeed = (new.downloadDataSize - last.downloadDataSize) / (((new.uptime - last.uptime)) / 1000L)
                            val uploadSpeed = (new.uploadDataSize - last.uploadDataSize) / (((new.uptime - last.uptime)) / 1000L)
                            val requests = requestsInfo.map { (k, v) ->
                                HttpRequest.HttpRequestsSummary.Companion.SingleRequestInfo(
                                    key = k,
                                    dataUploadSize = v.uploadDataSize.get(),
                                    dataDownloadSize = v.downloadDataSize.get(),
                                    requestTimes = v.requestTimes.get()
                                )
                            }.sortedByDescending { it.dataUploadSize + it.dataDownloadSize }
                            dispatchMonitorData(HttpRequest.HttpRequestsSummary(
                                dataUploadSize = new.uploadDataSize,
                                dataDownloadSize = new.downloadDataSize,
                                requests = requests,
                                speedCalculateStartTime = last.time,
                                speedCalculateEndTime = new.time,
                                uploadSpeed = uploadSpeed,
                                downloadSpeed = downloadSpeed
                            ))
                            lastHttpSummaryRecordSnapshot.set(new)
                        }
                        sendNextTimeCheckTask()
                    }
                }
            }

            fun sendNextTimeCheckTask() {
                removeMessages(CALCULATE_HTTP_REQUEST_SUMMARY_MSG)
                sendEmptyMessageDelayed(CALCULATE_HTTP_REQUEST_SUMMARY_MSG, monitorIntervalInMillis.get())
            }
        }
    }

    override fun onInit(apm: tApm) {

    }

    override fun onStart(apm: tApm) {
        attachMonitor(this)
        lastHttpSummaryRecordSnapshot.set(null)
        calculateRequestSummaryHandler.sendEmptyMessage(CALCULATE_HTTP_REQUEST_SUMMARY_MSG)
        tApmLog.d(TAG, "HttpRequestMonitor started.")
    }

    override fun onStop(apm: tApm) {
        detachMonitor(this)
        calculateRequestSummaryHandler.removeMessages(CALCULATE_HTTP_REQUEST_SUMMARY_MSG)
        lastHttpSummaryRecordSnapshot.set(null)
        tApmLog.d(TAG, "HttpRequestMonitor stopped.")
    }

    private fun httpRequestIntercept(chain: Interceptor.Chain): Response {
        val requesting = Requesting(
            startTime = System.currentTimeMillis(),
        )
        val requestStartTime = SystemClock.uptimeMillis()
        return try {
            val realRequest = chain.request()
            requesting.method = realRequest.method

            // Url
            val url = realRequest.url.let {
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
            requesting.requestBodyContentType = realRequest.contentType()
            requesting.requestBodyContentLength = realRequest.contentLength()
            val wrapperRequestBody: WrapperRequestBody?
            if (realRequestBody != null) {
                wrapperRequestBody = WrapperRequestBody(
                    realRequestBody = realRequestBody,
                    requesting = requesting,
                    isGzipEncoding = "gzip".equals(realRequest.header("Content-Encoding"), ignoreCase = true)
                )
                wrapperRequest = realRequest.newBuilder()
                    .method(realRequest.method, wrapperRequestBody)
                    .build()
            } else {
                wrapperRequestBody = null
                wrapperRequest = realRequest
            }
            val realResponse = chain.proceed(wrapperRequest)
            wrapperRequestBody?.dispatchEnd()

            // Response
            requesting.isHttpSuccess = realResponse.isSuccessful
            requesting.responseHeader = realResponse.headers.toMultimap()
            requesting.responseCode = realResponse.code
            requesting.responseBodyContentType = realResponse.contentType()
            requesting.responseBodyContentLength = realResponse.contentLength()
            val realResponseBody = realResponse.body
            val wrapperResponse: Response
            val isGzipEncoding = "gzip".equals(realResponse.header("Content-Encoding"), ignoreCase = true) && realResponse.promisesBody()
            wrapperResponse = realResponse.newBuilder()
                .body(
                    WrapperResponseBody(
                        realResponseBody = realResponseBody,
                        requesting = requesting,
                        requestStartTime = requestStartTime,
                        isGzipEncoding = isGzipEncoding
                    )
                )
                .build()
            wrapperResponse
        } catch (e: Throwable) {
            requesting.error = e
            requesting.httpRequestCostInMillis = SystemClock.uptimeMillis() - requestStartTime
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
        var requestBodyWriteSize: Long? = null,
        var requestBodyText: String? = null,

        var responseHeader: Map<String, List<String>>? = null,
        var responseCode: Int? = null,
        var responseBodyContentType: String? = null,
        var responseBodyContentLength: Long? = null,
        var responseBodyReadSize: Long? = null,
        var responseBodyText: String? = null,
        var isHttpSuccess: Boolean? = null,

        var httpRequestCostInMillis: Long = 0,
        var error: Throwable? = null,

        var isDispatched: Boolean = false
    )

    private inner class WrapperRequestBody(
        private val realRequestBody: RequestBody,
        private val requesting: Requesting,
        private val isGzipEncoding: Boolean
    ) : RequestBody() {

        private val bodyBuffer: ArrayList<Byte>? = if (contentType().isTextType() && contentLength() <= MAX_HANDLE_BODY_SIZE) {
            ArrayList<Byte>()
        } else {
            null
        }

        private var writeSize: Long = 0L

        override fun contentType(): MediaType? = realRequestBody.contentType()

        override fun contentLength(): Long = realRequestBody.contentLength()

        override fun isDuplex(): Boolean = realRequestBody.isDuplex()

        override fun isOneShot(): Boolean = realRequestBody.isOneShot()

        override fun writeTo(sink: BufferedSink) {
            val realOutputStream = sink.outputStream()
            val wrapperSink = WrapperOutputStream(realOutputStream).sink().buffer()
            realRequestBody.writeTo(wrapperSink)
            if (wrapperSink.isOpen) {
                wrapperSink.flush()
            }
        }

        fun dispatchEnd() {
            requesting.requestBodyWriteSize = writeSize
            if (bodyBuffer != null && bodyBuffer.isNotEmpty() && writeSize <= MAX_HANDLE_BODY_SIZE) {
                val s = if (isGzipEncoding) {
                    GzipSource(Buffer().write(bodyBuffer.toByteArray())).use { it.buffer().readUtf8() }
                } else {
                    bodyBuffer.toByteArray().toString(Charsets.UTF_8)
                }
                if (contentType().isJsonType()) {
                    requesting.requestBodyText = s.beautifyJsonString()
                } else {
                    requesting.requestBodyText = s
                }
            }
        }

        private inner class WrapperOutputStream(private val realOutputStream: OutputStream) : OutputStream() {
            override fun write(b: Int) {
                if (writeSize < MAX_HANDLE_BODY_SIZE) {
                    bodyBuffer?.add(b.toByte())
                }
                realOutputStream.write(b)
                writeSize ++
            }
        }
    }

    private inner class WrapperResponseBody(
        val realResponseBody: ResponseBody,
        val requesting: Requesting,
        val requestStartTime: Long,
        val isGzipEncoding: Boolean
    ) : ResponseBody() {

        private val bodyBuffer: ArrayList<Byte>? = if (contentType().isTextType() && contentLength() <= MAX_HANDLE_BODY_SIZE) {
            ArrayList<Byte>()
        } else {
            null
        }

        private var isRecordBodyEnd = false

        private var readSize: Long = 0L

        override fun contentLength(): Long = realResponseBody.contentLength()

        override fun contentType(): MediaType? = realResponseBody.contentType()

        override fun source(): BufferedSource {
            return WrapperInputStream(realResponseBody.source().inputStream()).source().buffer()
        }

        private fun dispatchEnd(e: Throwable?) {
            if (!isRecordBodyEnd) {
                isRecordBodyEnd = true
                requesting.httpRequestCostInMillis = SystemClock.uptimeMillis() - requestStartTime
                requesting.responseBodyReadSize = readSize
                if (bodyBuffer != null && bodyBuffer.isNotEmpty() && readSize <= MAX_HANDLE_BODY_SIZE) {
                    if (isGzipEncoding) {
                        requesting.responseBodyText = try {
                            GzipSource(ByteArrayInputStream(bodyBuffer.toByteArray()).source()).use {
                                it.buffer().readUtf8()
                            }.let {
                                if (contentType().isJsonType()) {
                                    it.beautifyJsonString()
                                } else {
                                    it
                                }
                            }
                        } catch (e: Throwable) {
                            tApmLog.e(TAG, "Read gzip fail: ${e.message}", e)
                            null
                        }
                    } else {
                        val s = bodyBuffer.toByteArray().toString(Charsets.UTF_8)
                        requesting.responseBodyText = if (contentType().isJsonType()) {
                            s.beautifyJsonString()
                        } else {
                            s
                        }
                    }
                }
                requesting.error = e
                dispatchHttpTimeCost(requesting)
            }
        }

        private inner class WrapperInputStream(val realInputStream: InputStream) : InputStream() {
            override fun read(): Int {
                val readResult = try {
                    realInputStream.read()
                } catch (e: Throwable) {
                    dispatchEnd(e)
                    throw e
                }
                if (readResult == -1) {
                    dispatchEnd(null)
                } else {
                    if (bodyBuffer != null && readSize < MAX_HANDLE_BODY_SIZE) {
                        bodyBuffer.add(readResult.toByte())
                    }
                    readSize ++
                }
                return readResult
            }
        }
    }

    private fun dispatchHttpTimeCost(requesting: Requesting) {
        if (!requesting.isDispatched) {
            requesting.isDispatched = true
            requestTimesUpdate(url = requesting.url, method = requesting.method)
            requesting.requestBodyWriteSize?.let {
                if (it > 0) {
                    uploadDataSizeUpdate(url = requesting.url, method = requesting.method, writeSize = it)
                }
            }
            requesting.responseBodyReadSize?.let {
                if (it > 0) {
                    downloadDataSizeUpdate(url = requesting.url, method = requesting.method, readSize = it)
                }
            }
            dispatchMonitorData(
                HttpRequest.SingleRequestRecord(
                    startTime = requesting.startTime,
                    method = requesting.method,
                    url = requesting.url,
                    queryParams = requesting.queryParams,
                    requestHeader = requesting.requestHeader,
                    requestBodyContentType = requesting.requestBodyContentType,
                    requestBodyContentLength = requesting.requestBodyContentLength,
                    requestBodyWriteSize = requesting.requestBodyWriteSize,
                    requestBodyText = requesting.requestBodyText,

                    responseHeader = requesting.responseHeader,
                    responseCode = requesting.responseCode,
                    responseBodyContentType = requesting.responseBodyContentType,
                    responseBodyContentLength = requesting.responseBodyContentLength,
                    responseBodyReadSize = requesting.responseBodyReadSize,
                    responseBodyText = requesting.responseBodyText,

                    isHttpSuccess = requesting.isHttpSuccess,
                    httpRequestCostInMillis = requesting.httpRequestCostInMillis,
                    error = requesting.error
                )
            )
        }
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

    private fun MediaType?.isJsonType(): Boolean {
        return if (this != null) {
            val type = toString().lowercase()
            when {
                type.startsWith("application/json") -> true
                else -> false
            }
        } else {
            false
        }
    }

    private fun String.beautifyJsonString(): String {
        return try {
            JSONObject(this).toString(2)
        } catch (_: Throwable) {
            this
        }
    }

    private fun Request.contentType(): String? {
        val body = body
        return if (body == null) {
            null
        } else {
            body.contentType()?.toString() ?: headers["content-type"]
        }
    }

    private fun Request.contentLength(): Long? {
        val body = body
        return body?.contentLength()?.let {
            if (it > 0) {
                it
            } else {
                headers["content-length"]?.toLongOrNull()
            }
        }
    }

    private fun Response.contentType(): String? {
        val body = body
        return body.contentType()?.toString() ?: headers["content-type"]
    }

    private fun Response.contentLength(): Long? {
        val body = body
        return body.contentLength().let {
            if (it > 0) {
                it
            } else {
                headers["content-length"]?.toLongOrNull()
            }
        }
    }

    companion object : Interceptor {

        private data class HttpRequestInfo(
            val requestTimes: AtomicInteger = AtomicInteger(0),
            val uploadDataSize: AtomicLong = AtomicLong(0),
            val downloadDataSize: AtomicLong = AtomicLong(0)
        )

        private data class HttpSummaryRecordSnapshot(
            val time: Long,
            val uptime: Long,
            val uploadDataSize: Long,
            val downloadDataSize: Long
        )

        private const val TAG = "HttpRequestMonitor"

        private const val MAX_HANDLE_BODY_SIZE = 1024 * 1024 * 64 // 64MB

        private const val MAX_RECORD_HTTP_SIZE = 128

        private const val CALCULATE_HTTP_REQUEST_SUMMARY_MSG = 0

        private const val DEFAULT_HTTP_REQUEST_SUMMARY_CALCULATE_INTERVAL = 60L * 1000L * 3L // 3 min

        private val uploadDataSize: AtomicLong by lazy {
            AtomicLong(0)
        }

        private val downloadDataSize: AtomicLong by lazy {
            AtomicLong(0)
        }

        private val requestsInfo: ConcurrentHashMap<String, HttpRequestInfo> by lazy {
            ConcurrentHashMap()
        }

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

        private fun requestTimesUpdate(url: String, method: String) {
            val key = "$method->$url"
            val info = requestsInfo[key].let {
                it ?: if (requestsInfo.size < MAX_RECORD_HTTP_SIZE) {
                        val new = HttpRequestInfo()
                        val last = requestsInfo.putIfAbsent(key, new)
                        last ?: new
                    } else {
                        null
                    }
            }
            info?.requestTimes?.addAndGet(1)
        }

        private fun uploadDataSizeUpdate(url: String, method: String, writeSize: Long) {
            uploadDataSize.addAndGet(writeSize)
            val key = "$method->$url"
            requestsInfo[key]?.uploadDataSize?.addAndGet(writeSize)
        }

        private fun downloadDataSizeUpdate(url: String, method: String, readSize: Long) {
            downloadDataSize.addAndGet(readSize)
            val key = "$method->$url"
            requestsInfo[key]?.downloadDataSize?.addAndGet(readSize)
        }
    }
}