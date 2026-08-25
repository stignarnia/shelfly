# ## Moshi
#
# Adapters here are reflective - RetrofitModule installs KotlinJsonAdapterFactory and nothing uses codegen - so the model classes are only ever reached at runtime.
# R8 sees nothing referencing them statically and shrinks all of them away without the keeps below: the APK builds, installs and launches, then fails on the first response it tries to parse.
# That failure is invisible to every check in this repository, so treat these three lines as load-bearing.
#
# Moshi, Retrofit and OkHttp each ship their own rules in META-INF/proguard/ inside their artifacts, and R8 applies them automatically.
# Only what is specific to this module belongs here; a local copy of a library's rules goes stale silently, as this file's did.

# KotlinJsonAdapterFactory recovers constructor parameter names from @Metadata, and reads them through kotlin-reflect.
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoaderImpl

-keep class xyz.stignarnia.data_remote.tmdb.model.** { *; }
-keep class xyz.stignarnia.data_remote.omdb.model.** { *; }
-keep class xyz.stignarnia.data_remote.catalog.model.** { *; }
