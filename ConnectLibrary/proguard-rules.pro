# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class in.eko.connectlib.WebAppInterface {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

#### For AdvancedWebView
-keep class * extends android.webkit.WebChromeClient { *; }
# -dontwarn im.delight.android.webview.**

# -keep public class android.support.annotation.NonNull
# -keep public class android.webkit.JavascriptInterface


#### For Digio .................
-keep class in.eko.connectlib.BaseConnectActivity {
  public void onSigning*(...);
}

#-keep class com.digio.** {*;}
#-keepclassmembers class com.digio.** {*;}


#### For RazorPay .....................

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
-keepattributes *Annotation*

-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}

-optimizations !method/inlining/*

-keepclasseswithmembers class * {
  public void onPayment*(...);
}


#### For Branch.io.................

# To collect the Google Advertising ID, you must ensure that proguard doesn't remove the necessary Google Ads class
-keep class com.google.android.gms.** { *; }

#### Firebase Crashlytics Deobfuscated reports.....................
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# Uncomment for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
#### Remove the logging in the production environment..............
-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** d(...);
  public static *** i(...);
  public static *** w(...);
  public static *** e(...);
}
-keepclassmembers class * implements in.eko.connectlib.ConfigProvider {
    <methods>;
}