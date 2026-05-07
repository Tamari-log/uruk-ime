# --- スタックトレース用（マッピングと突き合わせやすくする） ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Compose / runtime（依存ライブラリ付帯のルールに加え、よくある警告抑止）
-dontwarn androidx.compose.**
-dontwarn androidx.graphics.**

# Coroutines（ライブラリ側ルールで足りない場合向け）
-dontwarn kotlinx.coroutines.**

# DataStore Proto 以外の preferences 実装
-keep class androidx.datastore.*.** { *; }
