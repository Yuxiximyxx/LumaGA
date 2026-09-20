# Protobuf generated code is reflected upon by the runtime.
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# 腾讯 Bugly 崩溃监控
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

# JNI bridge + generated protos / app models touched from Rust or reflection.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.bugenzhao.mnga.LogicKt { *; }
-keep class com.bugenzhao.mnga.protos.** { *; }

# Coil (okhttp / coroutines internals occasionally warned under R8)
-dontwarn okhttp3.**
-dontwarn okio.**
