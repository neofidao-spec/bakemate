# Keep Hilt generated classes
-keep class com.bakemate.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# Kotlinx coroutines
-dontwarn kotlinx.coroutines.**

# Compose
-dontwarn androidx.compose.**
