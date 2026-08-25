# ## Moshi
#
# The backup models are serialised reflectively, the same way the API models are - see data-remote/consumer-rules.pro for why that makes this keep load-bearing rather than defensive.
# The kotlin.Metadata and kotlin-reflect keeps live in that file; consumer rules from every module merge into the application's configuration, so they are not repeated here.
#
# Moshi's own rules ship in META-INF/proguard/moshi.pro inside its artifact.

-keep class xyz.stignarnia.ui_backup.model.** { *; }
