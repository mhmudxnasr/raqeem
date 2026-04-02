# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keep class com.raqeem.app.data.remote.dto.** { *; }
-keep class com.raqeem.app.data.local.entity.** { *; }

# Supabase / Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.raqeem.app.**$$serializer { *; }
