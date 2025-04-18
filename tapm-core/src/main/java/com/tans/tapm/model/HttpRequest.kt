package com.tans.tapm.model

sealed class HttpRequest {
    data class TimeCost(
        val startTime: Long,
        val method: String,
        val url: String,

        val queryParams: Map<String, List<String?>>,
        val requestHeader: Map<String, List<String>>,
        val requestBodyContentType: String?,
        val requestBodyContentLength: Long?,
        val requestBodyText: String?,
        val requestHeaderCostInMillis: Long?,
        val requestBodyCostInMillis: Long?,

        val responseHeader: Map<String, List<String>>?,
        val responseCode: Int?,
        val responseBodyContentType: String?,
        val responseBodyContentLength: Long?,
        val responseBodyText: String?,
        val isHttpSuccess: Boolean?,
        val responseHeaderCostInMillis: Long?,
        val responseBodyCostInMillis: Long?,

        val httpRequestCostInMillis: Long,
        val error: Throwable?
    ) : HttpRequest()
}