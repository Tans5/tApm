## Summary

A lightweight library for comprehensive Android application performance monitoring. Monitor Java/Native crashes, ANRs, resource usage, and more with minimal setup.

## Features

### Core Monitors

- Java Crash Monitoring
- Native Crash Monitoring
- ANR (Application Not Responding) Detection

### Advanced Monitors

- Main Thread Lag Detection
- HTTP Request Tracing (`OkHttp` integration)
- Memory Usage Monitoring
- CPU Usage & Power Cost Analysis
- Foreground Screen Power Consumption (Requires `android.permission.WRITE_SETTINGS`)

## Quick Start

### 1. Add Dependencies

In your app-level `build.gradle`:

```groovy
dependencies {
    // Core library  
    implementation("io.github.tans5:tapm-core:1.0.1")

    // Auto-initialization with default monitors  
    implementation("io.github.tans5:tapm-autoinit:1.0.1")

    // Debug-only log recording (optional)  
    debugImplementation("io.github.tans5:tapm-log:1.0.1")
} 
```
### 2. Default Configuration

- No code required for basic monitoring (crashes/ANRs).  
- Trace files are automatically stored in: `<AppExternalStoragePath>/tApm/`

Example Trace Output:  

```text
*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
Build fingerprint: 'Android/sdk_phone64_arm64/emu64a:14/UE1A.230829.036.A1/11228894:userdebug/test-keys'
Revision: '0'
Abi: 'arm64'
Timestamp: 2025-04-25T16:09:43.664+08:00
Process uptime: 3.0s
Cmdline: com.tans.tapm.demo
pid: 2962, tid: 2962, name: .tans.tapm.demo  >>> com.tans.tapm.demo <<<
uid: 10143
signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0000000000000018
    x0  b4000078fdb51fd0  x1  0000007fc80b65f8  x2  0000007fc80b5310  x3  00000078ea0a3ae0
    x4  0000007fc80b5468  x5  0000000000001071  x6  00000078e96fddea  x7  0000007fc80b5484
    x8  000000786907a7e0  x9  0000000000000000  x10 0000000000000020  x11 0000000000000010
    x12 000000000000a8aa  x13 036d5b4dd3ee68c3  x14 ffffffffffffffff  x15 00000000ebad6a89
    x16 0000000000000001  x17 0000007869124c74  x18 0000007bad402000  x19 b400007a3db4d380
    x20 0000000000000000  x21 b400007a3db4d448  x22 0000007b86777228  x23 0000007b86777228
    x24 0000007fc80b6640  x25 b40000797db4a560  x26 000000000000106e  x27 0000007b83416bc0
    x28 0000007fc80b6510  x29 0000007fc80b6600
    lr  00000078e9f46c34  sp  0000007fc80b64e0  pc  0000007869124c90  pst  0000000060001000

64 total frames
backtrace:
      #00 pc 00000000000f5c90  /data/app/~~1x6f0iXwPV38jM7Ukm_K2A==/com.tans.tapm.demo-wSDJRqwwZgNAVmYLZL-4OA==/base.apk!libtapm.so (offset 0x508000) (Java_com_tans_tapm_monitors_NativeCrashMonitor_testNativeCrash+28) (BuildId: 555226fd3f71a1f93588c56edd05ce8aa2ccdc9e)
      #01 pc 0000000000346c30  /apex/com.android.art/lib64/libart.so (art_quick_generic_jni_trampoline+144) (BuildId: d22b3b69a6db691fdd84720465c7a214)
      #02 pc 00000000003301a4  /apex/com.android.art/lib64/libart.so (art_quick_invoke_stub+612) (BuildId: d22b3b69a6db691fdd84720465c7a214)
```

## Advanced Configuration

### Custom Monitor Setup

Initialize in your `Application` class:

```Kotlin
class App : Application() {  

    override fun attachBaseContext(base: Context?) {  
        super.attachBaseContext(base)  
          
        tApmAutoInit.addBuilderInterceptor { builder ->  
            builder  
                .addMonitor(CpuUsageMonitor().apply {  
                    setMonitorInterval(10_000L) // 10 seconds  
                })  
                .addMonitor(CpuPowerCostMonitor())  
                .addMonitor(ForegroundScreenPowerCostMonitor())  
                .addMonitor(MainThreadLagMonitor())  
                .addMonitor(MemoryUsageMonitor().apply {  
                    setMonitorInterval(5_000L) // 5 seconds  
                })
                .addMonitor(HttpRequestMonitor())
        }  
    }  

    // Optional: HTTP monitoring for OkHttp  
    val okHttpClient: OkHttpClient by lazy {  
        OkHttpClient.Builder()  
            .addInterceptor(HttpRequestMonitor)  
            .build()  
    }  
}  
```

### Key Notes

1. Initialization Timing  
    Configure monitors in `attachBaseContext()`, not `onCreate()`.
2. Permission Handling
   ForegroundScreenPowerCostMonitor requires runtime permission:
    ```xml
        <uses-permission android:name="android.permission.WRITE_SETTINGS" />  
    ```

