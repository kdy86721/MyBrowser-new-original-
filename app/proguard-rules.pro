# R8 / ProGuard optimization rules for MyBrowser(BM) (Pure AOSP / Zero-GMS)

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# --- WebView @JavascriptInterface bridges ---
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.webview.BlobDownloadBridge {
    public *;
}

# --- WebView / WebChromeClient / WebViewClient callbacks ---
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
-keepclassmembers class * implements android.webkit.DownloadListener {
    public *;
}

# --- kotlinx.serialization (all @Serializable models, serializers & companions) ---
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.example.** { *; }

# --- Rust FFI-ready core boundary models & bridge ---
-keep class com.example.core.BrowserCoreBridge { *; }
-keep class com.example.core.** {
    public *;
}

# --- AndroidX Webkit & Window (Foldable Layout) reflection-backed classes ---
-keep class androidx.webkit.** { *; }
-keep class androidx.window.** { *; }
-dontwarn androidx.window.**
-dontwarn androidx.webkit.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
