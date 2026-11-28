# The rules from AOSP are located in proguard.flags file, we can just maintain Mellune related rules here.

# Optimization options.
-allowaccessmodification
-dontusemixedcaseclassnames
-allowaccessmodification
-keepattributes InnerClasses, *Annotation*, Signature, SourceFile, LineNumberTable

# Remove some Kotlin overhead
-processkotlinnullchecks remove

# Common rules.
-keep class android.window.** { *; }
-keep class android.view.** { *; }

-keepclassmembers class * implements android.os.Parcelable {
  public static final ** CREATOR;
}

# Mellune specific rules.
-keep,allowshrinking,allowoptimization class app.mellune.MelluneProto$* { *; }
-keep,allowshrinking,allowoptimization class app.mellune.MelluneApp { *; }
-keep,allowshrinking,allowoptimization class app.mellune.MelluneLauncher { *; }
-keep,allowshrinking,allowoptimization class app.mellune.compatlib.** { *; }

-keep,allowshrinking,allowoptimization class com.google.protobuf.Timestamp { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# We intentionally remove it to replace Smartspacer's widget popup with our own Launcher3 popup
-dontwarn com.skydoves.balloon.*

# This shouldn't concern us much
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# Mellune Motion
-keep class app.mellune.util.MelluneMotionHelper { *; }
-keepclassmembers class app.mellune.util.MelluneMotionHelper { *; }


# Mellune Spring Engine
-keep class app.mellune.util.MelluneSpringEngine { *; }
-keepclassmembers class app.mellune.util.MelluneSpringEngine { *; }

# Reflectively loaded overrides and launcher singletons
-keep class app.mellune.MelluneProcessInitializer { *; }
-keep class app.mellune.AccentColorExtractor { *; }
-keep class app.mellune.MelluneActivityCachingLogic { *; }
-keep class app.mellune.MelluneLayoutFactory { *; }
-keep class app.mellune.overview.TaskOverlayFactoryImpl { *; }
-keep class app.mellune.factory.MelluneWidgetHolder** { *; }
-keep class app.mellune.util.MelluneWindowManagerProxy { *; }
-keep class app.mellune.search.** { *; }
-keep class app.mellune.organizer.** { *; }
-keep class app.mellune.homescreenfiles.** { *; }

