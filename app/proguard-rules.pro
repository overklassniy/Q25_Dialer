# Keep libphonenumber metadata
-keep class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.overklassniy.q25.dialer.**$$serializer { *; }
-keepclassmembers class com.overklassniy.q25.dialer.** {
    *** Companion;
}
-keepclasseswithmembers class com.overklassniy.q25.dialer.** {
    kotlinx.serialization.KSerializer serializer(...);
}