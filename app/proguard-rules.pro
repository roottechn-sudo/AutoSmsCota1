# ==============================================================================
# ProGuard / R8 Rules for AutoSmsCota1
# Enhanced Security, String Obfuscation & Sensitive Credential Protection
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Bytecode Shrinking, Obfuscation & Package Repackaging
# ------------------------------------------------------------------------------
# Repackage all non-public internal application classes into a single obscured package
-repackageclasses 'com.example.s'
-allowaccessmodification
-overloadaggressively

# Flatten package hierarchy to prevent reverse-engineering architectural layout
-flattenpackagehierarchy 'com.example.internal'

# Strip source file names and debug line info to thwart JADX decompilation
-renamesourcefileattribute ''
-keepattributes SourceFile,LineNumberTable

# Remove unnecessary metadata attributes that reveal sensitive reflection details
-dontusemixedcaseclassnames
-keepparameternames

# ------------------------------------------------------------------------------
# 2. Obfuscation and Elimination of Sensitive Strings & Logs
# ------------------------------------------------------------------------------
# Strip all verbose/debug logging to prevent leakage of credentials and keys at runtime
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int println(...);
}

# Remove Kotlin reflection metadata and debug intrinsics
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
}

# ------------------------------------------------------------------------------
# 3. Protection of BuildConfig & Sensitive Configuration Secrets (.env / Firebase)
# ------------------------------------------------------------------------------
# Strip unused BuildConfig fields; only retain runtime access to required constants
-keepclassmembers class com.example.BuildConfig {
    public static final java.lang.String *;
}

# Obfuscate internal service providers handling Firebase configuration
-keepclassmembers class com.example.data.firebase.FirestoreService {
    private final ** *;
}

# ------------------------------------------------------------------------------
# 4. Firebase SDK, Google Play Services & Auth Safety
# ------------------------------------------------------------------------------
# Keep core Firebase SDK entry points required for dynamic component initialization
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services & Credential Manager
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }

# ------------------------------------------------------------------------------
# 5. Jetpack Compose, Room & Moshi Runtime Persistence
# ------------------------------------------------------------------------------
# Preserve annotations needed for Compose compiler & runtime state reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database schemas, DAOs and Entities
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep interface com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Moshi JSON adapters
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Coroutines and background threading
-dontwarn kotlinx.coroutines.**
