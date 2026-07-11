# kotlinx.serialization — keep generated serializers and the DTOs they serialize.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Our OpenFoodFacts DTOs (annotated with @Serializable).
-keep,includedescriptorclasses class app.diaita.data.remote.dto.**$$serializer { *; }
-keepclassmembers class app.diaita.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class app.diaita.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit — reflection on generic parameter types of service methods.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**

# OkHttp platform probes.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
