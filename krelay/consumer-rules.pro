# KRelay - Proguard/R8 Rules
# =============================
#
# These rules ensure KRelay works correctly after code obfuscation.
#
# Why these rules are needed:
# - KRelay uses reflection (KClass) to register/dispatch features
# - R8/Proguard might remove or rename classes that implement RelayFeature
# - This would break the registry system at runtime
#
# Apply these rules automatically by including this library.

# Keep all RelayFeature interface implementations
-keep interface dev.brewkits.krelay.RelayFeature
-keep class * implements dev.brewkits.krelay.RelayFeature { *; }

# Keep KRelay core classes and members
-keep class dev.brewkits.krelay.KRelay {
    public *;
    protected *;
}

-keep class dev.brewkits.krelay.KRelayMetrics {
    public *;
}

# Keep all feature interfaces (they might be defined in app code)
-keep interface * extends dev.brewkits.krelay.RelayFeature { *; }

# Keep WeakRef implementations (platform-specific)
-keep class dev.brewkits.krelay.WeakRef { *; }

# Keep Lock implementations (platform-specific)
-keep class dev.brewkits.krelay.Lock { *; }

# Keep QueuedAction and its members (used in reflection)
-keep class dev.brewkits.krelay.QueuedAction { *; }

# Keep ActionPriority enum
-keep enum dev.brewkits.krelay.ActionPriority { *; }

# Keep MainThreadExecutor implementations
-keep class dev.brewkits.krelay.MainThreadExecutor { *; }

# Preserve annotations (if any are added in the future)
-keepattributes *Annotation*

# Preserve generic signatures (important for type safety)
-keepattributes Signature

# Preserve source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# If using custom feature interfaces in your app, add rules like:
# -keep interface com.yourapp.features.** extends dev.brewkits.krelay.RelayFeature { *; }
# -keep class * implements com.yourapp.features.** { *; }
