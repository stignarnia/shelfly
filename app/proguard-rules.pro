# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn okhttp3.internal.platform.ConscryptPlatform

# This is an open source app, so obfuscation buys nothing: anyone can read the
# source anyway. Turning it off keeps crash reports and stack traces readable
# without shipping a mapping file, and helps reproducible builds.
#
# R8 still shrinks unused code and resources, which is what actually keeps the
# APK small. That is a separate concern from obfuscation.
-dontobfuscate

# Keep line numbers so stack traces point at real source lines.
-keepattributes SourceFile,LineNumberTable
