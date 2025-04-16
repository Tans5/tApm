package com.tans.tapm.model

data class NativeCrash(
    val sig: Int,
    val sigCode: Int,
    val crashPid: Int,
    val crashTid: Int,
    val crashUid: Int,
    val startTime: Long,
    val crashTime: Long,
    val crashSummary: String?,
    val crashTraceFilePath: String?
)