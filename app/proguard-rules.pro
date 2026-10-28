# Room, Compose and kotlinx.serialization publish their own consumer rules.
# Keep the accessibility service entry point because Android binds it by name.
-keep class com.sanctuary.core.tracking.SanctuaryAccessibilityService { *; }
