package com.tans.tapm.model

import com.tans.tapm.convertToStrings
import com.tans.tapm.formatDataTimeMsZoom

sealed class HttpRequest {
    data class TimeCost(
        val startTime: Long,
        val method: String,
        val url: String,

        val queryParams: Map<String, List<String?>>,
        val requestHeader: Map<String, List<String>>,
        val requestBodyContentType: String?,
        val requestBodyContentLength: Long?,
        val requestBodyWriteSize: Long?,
        val requestBodyText: String?,

        val responseHeader: Map<String, List<String>>?,
        val responseCode: Int?,
        val responseBodyContentType: String?,
        val responseBodyContentLength: Long?,
        val responseBodyReadSize: Long?,
        val responseBodyText: String?,
        val isHttpSuccess: Boolean?,


        val httpRequestCostInMillis: Long,
        val error: Throwable?
    ) : HttpRequest() {

        override fun toString(): String {
            val s = StringBuilder()
            s.appendLine("Http Start: $method $url($httpRequestCostInMillis ms, ${startTime.formatDataTimeMsZoom()})")
            s.appendLine("--> Request Start")
            if (requestHeader.isNotEmpty()) {
                s.appendLine("Header:")
                for ((k, vs) in requestHeader) {
                    for (v in vs) {
                        s.appendLine("  $k: $v")
                    }
                }
            }
            if (queryParams.isNotEmpty()) {
                s.appendLine("Query:")
                for ((k, vs) in queryParams) {
                    for (v in vs) {
                        s.appendLine("  $k=${v ?: ""}")
                    }
                }
            }
            if (requestBodyContentLength != null && requestBodyContentLength > 0) {
                s.append("Body")
                s.append("(")
                s.append("$requestBodyContentLength bytes")
                if (requestBodyContentType != null) {
                    s.append(", $requestBodyContentType")
                }
                if (requestBodyWriteSize != null && requestBodyWriteSize > 0) {
                    s.append(", write $requestBodyWriteSize bytes")
                }
                s.appendLine("):")
                if (requestBodyText != null) {
                    s.appendLine(requestBodyText)
                }
            }
            s.appendLine("--> Request End")

            if (responseHeader != null) {
                s.appendLine("\n<-- Response Start")
                if (responseHeader.isNotEmpty()) {
                    s.appendLine("Header:")
                    for ((k, vs) in responseHeader) {
                        for (v in vs) {
                            s.appendLine("  $k: $v")
                        }
                    }
                }
                if (responseBodyContentLength != null && responseBodyContentLength > 0) {
                    s.append("Body")
                    s.append("(")
                    s.append("$responseBodyContentLength bytes")
                    if (responseBodyContentType != null) {
                        s.append(", $responseBodyContentType")
                    }
                    if (responseBodyReadSize != null && responseBodyReadSize > 0) {
                        s.append(", read $responseBodyReadSize bytes")
                    }
                    s.appendLine("):")
                    if (responseBodyText != null) {
                        s.appendLine(responseBodyText)
                    }
                }
                s.append("<-- Response End")
                if (responseCode != null) {
                    s.appendLine("($responseCode)")
                } else {
                    s.appendLine()
                }
            }

            if (error != null) {
                for (e in error.convertToStrings()) {
                    s.appendLine(e)
                }
            }
            s.append("Http End")
            return s.toString()
        }
    }
}