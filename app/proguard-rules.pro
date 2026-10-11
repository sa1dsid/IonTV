# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit берёт тип ответа из generic-сигнатуры suspend-метода. Если результат
# вызова нигде не читается, R8 вырезает DTO, сигнатура вырождается в
# Continuation<Object>, и запрос падает ещё до отправки. Сериализаторы оставляют
# правила самой kotlinx.serialization, здесь нужно только не дать удалить класс.
-keep,allowobfuscation @kotlinx.serialization.Serializable class com.ionica.iontv.**
