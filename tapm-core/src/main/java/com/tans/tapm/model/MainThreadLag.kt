package com.tans.tapm.model

data class MainThreadLag(
    val lagTime: Long,
    val lagStackTrace: Array<StackTraceElement>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MainThreadLag

        if (lagTime != other.lagTime) return false
        if (!lagStackTrace.contentEquals(other.lagStackTrace)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lagTime.hashCode()
        result = 31 * result + lagStackTrace.contentHashCode()
        return result
    }

    override fun toString(): String {
        val s = StringBuilder()
        s.append("LagTime: $lagTime ms")
        for (t in lagStackTrace) {
            s.append("\n at $t")
        }
        return s.toString()
    }
}