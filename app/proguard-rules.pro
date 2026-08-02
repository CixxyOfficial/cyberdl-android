# Rules for NewPipeExtractor (required for YouTube signature/throttling deobfuscation)
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
