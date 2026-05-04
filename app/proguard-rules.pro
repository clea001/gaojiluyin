# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Whisper engine
-keep class com.gaojiluyin.whisper.** { *; }
