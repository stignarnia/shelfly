# Add project specific R8 rules here.
# You can control the set of applied configuration files using the proguardFiles setting in build.gradle.
#
# For more details, see https://developer.android.com/build/shrink-code

-dontwarn okhttp3.internal.platform.ConscryptPlatform

# This is an open source app, so obfuscation buys nothing: anyone can read the source anyway.
# Turning it off keeps crash reports and stack traces readable without shipping a mapping file, and helps reproducible builds.
#
# R8 still shrinks unused code and resources, which is what actually keeps the APK small.
# That is a separate concern from obfuscation.
-dontobfuscate

# Keep line numbers so stack traces point at real source lines.
-keepattributes SourceFile,LineNumberTable
