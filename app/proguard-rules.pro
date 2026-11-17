# Release builds run R8 (minify + resource shrinking). Libraries such as Retrofit, OkHttp, Hilt,
# WorkManager, Firebase and AppAuth ship their own consumer rules; only app specifics live here.

# Stack traces: keep line numbers, hide the original file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson maps JSON by reflecting on field names and generic signatures, so every request/response
# model must keep its fields, and the generic types (BaseResponse<T> through TypeToken) their signatures.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses
-keep class com.rizkysiregar.narrativ.data.remote.model.** { *; }

# Models are also written into the nav saved state as JSON (Gson().toJson(post)); nothing else to keep.

# WorkManager stores a worker's class name with the pending request. Keep the names stable, or an upload
# that is still queued when the app updates would fail to find its renamed worker.
-keepnames class * extends androidx.work.ListenableWorker

# Debug logging is dropped from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
