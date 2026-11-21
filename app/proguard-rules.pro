# Keep Navigation 3 NavKey entries and kotlinx.serialization DTOs used across state restoration and backup I/O.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class com.wtk_apps.workoutbro.** {
    *** Companion;
}
-keepclasseswithmembers class com.wtk_apps.workoutbro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.wtk_apps.workoutbro.**$$serializer { *; }
-keep class com.wtk_apps.workoutbro.** implements androidx.navigation3.runtime.NavKey { *; }
