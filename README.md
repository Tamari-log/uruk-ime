# Uruk IME (uruk-ime)

Android 用の **楔形文字（キッヌス・クネイフォーム）入力 IME**。  
Unicode の楔形文字ブロック（U+12000 台・U+12400 台など）を、グリッドとラテン転写入力から挿入できます。

> **ベータ** — 仕様・見た目・挙動は変更されることがあります。

**リポジトリ（GitHub）**: https://github.com/Tamari-log/uruk-ime  
（リネーム前は `CuneiformInput` でした。古い URL のリダイレクトは GitHub 設定に依存します。）

## Google Play クローズドテスト

https://play.google.com/store/apps/details?id=com.belleval.enmerkar.type

もしこのリンクからテストに参加する場合はGoogle Playで使用するメールアドレスで私宛（sa.u.20060202@gmail.com）に空メールを送ってください。

一日ほどでクローズドテストユーザーに追加いたします

## 機能（概要）

| | |
|--|--|
| **メイングリッド** | 一覧から符号をタップして挿入 |
| **アルファベットタブ** | ラテン転写（シュメール語向けマッピング）から楔形へ変換 |
| **数字** | アラビア数字バッファを整数として楔形表現に変換 |
| **設定** | テーマ、キーサイズ、既定タブ、ハプティック、枠線など（DataStore で端末内保存） |
| **フォント** | Noto Sans Cuneiform（同梱） |

ネットワーク通信・広告・解析 SDK は **含めていません**（詳細は [プライバシーポリシー](docs/privacy-policy-ja.md)）。

## 要件

- **minSdk** 26 / **targetSdk** 35（`app/build.gradle.kts` 参照）
- Android Studio または JDK 17＋Android SDK

## ビルド

### Google Play 提出（Android App Bundle）

```bash
./gradlew bundleRelease
```

生成 AAB: `app/build/outputs/bundle/release/app-release.aab`  
（本番提出には Android のリリース署名が必要です。手元のキーストアで `app/build.gradle.kts` の設定に合わせてください。未設定の場合、Gradle は debug キーで署名します。）

### ローカル検証（デバッグ APK）

```bash
./gradlew assembleDebug
```

生成 APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**applicationId / パッケージ**: `com.belleval.enmerkar.type`

## インストール後（IME の有効化）

1. **設定 → システム → 言語と入力**（機種により表記差あり）  
2. **オンスクリーンキーボード → キーボードの管理** で本アプリをオン  
3. 入力欄では **キーボード切替**から「Uruk IME」等を選択

## プライバシー

- [プライバシーポリシー（日本語）](docs/privacy-policy-ja.md)

## ライセンス

[MIT License](LICENSE)

クネイフォーム用の文字データ・マッピングには Unicode 等の公開データに依存しています。各データソースのライセンス条項に従ってください。

## 開発メモ

- IME 実装: `app/src/main/java/com/belleval/enmerkar/type/`

---

### English (short)

**Uruk IME** (`uruk-ime`) is an Android IME for entering cuneiform glyphs (Unicode blocks).  
No ads, no analytics, no network calls in the app itself. See [Privacy Policy (JA)](docs/privacy-policy-ja.md).  
Beta quality; APIs and UX may change.
