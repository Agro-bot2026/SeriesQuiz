# Reglas de ProGuard/R8 para SeriesQuiz
# App simple (WebView + AppCompat + AdMob + Play Games), reglas minimas necesarias.

-keepattributes JavascriptInterface
-keep public class * extends android.webkit.WebViewClient
-keep public class * extends android.webkit.WebChromeClient
-keep class com.google.android.gms.games.** { *; }
-keep class com.google.android.gms.ads.** { *; }
