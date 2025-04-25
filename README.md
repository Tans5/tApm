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
Abi: arm64  
Timestamp: 2025-04-25T16:09:43.664+08:00  
Process: com.tans.tapm.demo (pid: 2962)  
Signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0000000000000018  

Registers:  
    x0  b4000078fdb51fd0  x1  0000007fc80b65f8  
    ...  

Backtrace:  
    #00 pc 00000000000f5c90  /data/app/.../base.apk!libtapm.so  
    #01 pc 0000000000346c30  /apex/com.android.art/lib64/libart.so  
    ...  
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

