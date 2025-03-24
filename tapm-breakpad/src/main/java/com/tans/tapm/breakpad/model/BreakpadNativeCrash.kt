package com.tans.tapm.breakpad.model

data class BreakpadNativeCrash(
    val time: Long,
    val crashFilePath: String
)