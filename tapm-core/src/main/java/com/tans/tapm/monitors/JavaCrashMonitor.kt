package com.tans.tapm.monitors

import android.os.Build
import android.os.Process
import com.tans.tapm.convertToStrings
import com.tans.tapm.formatDataTimeMsZoom
import com.tans.tapm.internal.tApmLog
import com.tans.tapm.model.JavaCrash
import com.tans.tapm.tApm
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class JavaCrashMonitor : AbsMonitor<JavaCrash>(Long.MAX_VALUE) {

    override val isSupport: Boolean
        get() = apm.get() != null

    @Volatile
    private var originUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private val hasDispatchedError: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }

    private val uncaughtExceptionHandler: Thread.UncaughtExceptionHandler by lazy {
        object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {
                val time = System.currentTimeMillis()
                if (hasDispatchedError.compareAndSet(false, true)) {
                    tApmLog.e(TAG, "Error msg=${e.message}, threadName=${t.name}", e)
                    val otherThreadsStacks = Thread.getAllStackTraces().filter { it.key !== t }
                    val traceFilePath = writeCrashToFile(time = time, crashThread = t, error = e, otherThreadsStacks = otherThreadsStacks)
                    tApmLog.d(TAG, "TraceFilePath: $traceFilePath")
                    dispatchMonitorData(
                        t = JavaCrash(
                            time = time,
                            thread = t,
                            error = e,
                            otherThreadsStacks = otherThreadsStacks,
                            crashTraceFilePath = traceFilePath
                        ),
                        dispatchOnBackgroundThread = false
                    )
                    originUncaughtExceptionHandler?.uncaughtException(t, e)
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                } else {
                    tApmLog.e(TAG, "Skip handle error: ${e.message}.")
                    originUncaughtExceptionHandler?.uncaughtException(t, e)
                }
            }
        }
    }

    override fun onInit(apm: tApm) {
        tApmLog.d(TAG, "Init JavaCrashMonitor success.")
    }

    override fun onStart(apm: tApm) {
        originUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        tApmLog.d(TAG, "OriginUncaughtExceptionHandler: $originUncaughtExceptionHandler")
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        tApmLog.d(TAG, "JavaCrashMonitor started.")
    }

    override fun onStop(apm: tApm) {
        Thread.setDefaultUncaughtExceptionHandler(originUncaughtExceptionHandler)
        tApmLog.e(TAG, "JavaCrashMonitor stopped.")
    }

    fun testJavaCrash() {
        error("Test java crash.")
    }

    private fun writeCrashToFile(
        time: Long,
        crashThread: Thread,
        error: Throwable,
        otherThreadsStacks: Map<Thread, Array<StackTraceElement>?>
    ): String? {

        return try {
            val dir = File(cacheBaseDir, "JavaCrash")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val dateTimeFormatted = time.formatDataTimeMsZoom()
            val outputFile = File(dir, dateTimeFormatted.replace(':', '_'))
            outputFile.createNewFile()
            outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.appendLine(CRASH_FILE_START_LINE)
                writer.appendLine("Build fingerprint: '${Build.FINGERPRINT}'")
                writer.appendLine("Timestamp: $dateTimeFormatted")
                writer.appendLine("name: ${crashThread.name}  >>> ${application.packageName} <<<")
                error.convertToStrings().take(128).let {
                    for (s in it) {
                        writer.appendLine(s)
                    }
                }
                for ((t, stacks) in otherThreadsStacks) {
                    if (stacks != null && stacks.isNotEmpty()) {
                        writer.appendLine(CRASH_FILE_THREAD_SPLIT_LINE)
                        writer.appendLine("name: ${t.name}")
                        for (s in stacks) {
                            writer.appendLine("\tat $s")
                        }
                    }
                }
            }
            outputFile.canonicalPath
        } catch (e: Throwable) {
            tApmLog.e(TAG, "Write crash file fail: ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "JavaCrashMonitor"

        private const val CRASH_FILE_START_LINE = "*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***"
        private const val CRASH_FILE_THREAD_SPLIT_LINE = "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---"
    }

}