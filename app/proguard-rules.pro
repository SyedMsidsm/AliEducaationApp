# ─── Ali Educational ProGuard Rules ───────────────────────────

# ── Room ──────────────────────────────────────────────────────
# Keep Room-generated Impl classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ─────────────────────────────────────────────
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}

# ── Apache POI ────────────────────────────────────────────────
# POI uses reflection heavily; keep all POI classes
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep interface org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.apache.logging.** { *; }
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.logging.**
-dontwarn org.apache.commons.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.batik.**
-dontwarn java.awt.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.graphbuilder.**
-dontwarn org.etsi.uri.**
-dontwarn org.bouncycastle.**

# ── Kotlin Serialization (if used later) ─────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# ── Compose ───────────────────────────────────────────────────
# Compose is handled by default rules, but keep stability annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── General ───────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
