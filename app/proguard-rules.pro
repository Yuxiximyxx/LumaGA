# Protobuf generated code is reflected upon by the runtime.
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# 腾讯 Bugly 崩溃监控
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

# Rust JNI: native symbols are looked up on the LogicKt class by name.
# Obfuscating or removing these breaks liblogic.so calls.
-keep class com.bugenzhao.mnga.LogicKt {
    native <methods>;
    *;
}
-keep interface com.bugenzhao.mnga.LogicCallback { *; }
-keep class com.bugenzhao.mnga.LogicException { *; }