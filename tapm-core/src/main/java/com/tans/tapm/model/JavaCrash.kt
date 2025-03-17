package com.tans.tapm.model

import com.tans.tapm.internal.formatDataTime

data class JavaCrash(
    val time: Long,
    val thread: Thread,
    val error: Throwable,
    val otherThreadsStacks: Map<Thread, Array<StackTraceElement>>
) {
    override fun toString(): String {
        return "ErrorTime=${time.formatDataTime()}, ThreadName=${error.message}"
    }
}
