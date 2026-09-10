# kotlinx.serialization houdt de gegenereerde serializers nodig
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class nl.woolacast.** {
    *** Companion;
}
-keepclasseswithmembers class nl.woolacast.** {
    kotlinx.serialization.KSerializer serializer(...);
}
