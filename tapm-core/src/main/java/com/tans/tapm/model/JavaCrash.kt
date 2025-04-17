package com.tans.tapm.model

import com.tans.tapm.formatDataTimeMs

data class JavaCrash(
    val time: Long,
    val thread: Thread,
    val error: Throwable,
    val otherThreadsStacks: Map<Thread, Array<StackTraceElement>?>,
    val crashTraceFilePath: String?
) {
    override fun toString(): String {
        return "ErrorTime=${time.formatDataTimeMs()}, ThreadName=${error.message}"
    }
}
