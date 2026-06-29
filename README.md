# Permission Scanner

A lightweight Android app that scans APK or ZIP files for risky permissions and suspicious code patterns — entirely on-device, no internet required.

Point it at an APK/ZIP (by file picker or path), and it unpacks the archive in memory, scans every manifest/source/config file inside, and reports what it finds, grouped by category, with line numbers and matched tokens.

## Features

- **Pick a file or paste a path** — use the system file picker (`*/*`) or type a raw filesystem path to scan
- **Scans inside ZIP/APK archives** without extracting to disk
- **Category-based detection** across 12 risk categories:

  | Category | Detects |
  |---|---|
  | `permission` | `uses-permission` / `permission` declarations |
  | `network` | `INTERNET`, `CHANGE_NETWORK_STATE`, `ACCESS_WIFI_STATE` |
  | `camera` | `CAMERA` |
  | `storage` | `READ/WRITE/MANAGE_EXTERNAL_STORAGE` |
  | `location` | `ACCESS_FINE/COARSE/BACKGROUND_LOCATION` |
  | `contacts` | `READ/WRITE_CONTACTS` |
  | `phone` | `READ_PHONE_STATE`, `CALL_PHONE`, `READ_CALL_LOG` |
  | `sms` | `SEND/RECEIVE/READ_SMS` |
  | `microphone` | `RECORD_AUDIO` |
  | `execute` | `Runtime.exec`, `ProcessBuilder`, `loadLibrary`, `dlopen` |
  | `crypto` | `Cipher`, `KeyStore`, `SecretKey`, `MessageDigest` |
  | `reflection` | `Class.forName`, `getDeclaredMethod`, `setAccessible` |

- **File types scanned inside the archive:** `.xml`, `.java`, `.kt`, `.smali`, `.json`, `.gradle`, `.properties`
- **Results view** grouped by category, color-coded, with per-finding file name, matched strings, and line numbers
- Built entirely with **Jetpack Compose** and **Material 3**

## How it works

1. The app opens the chosen ZIP/APK as a stream (`ZipInputStream`) — either via a content `Uri` (file picker) or a raw filesystem path.
2. It walks every entry in the archive, skipping directories and any file whose extension isn't in the scan list.
3. Each matching file's text is read and checked line-by-line against a set of regexes (one per category).
4. Matches are deduplicated per file/category and collected into `Finding` objects (`file`, `category`, `matches`, `lineNumbers`).
5. Results are grouped by category and rendered as cards in the UI.

This is a static, regex-based scan — it doesn't decompile or execute any code, so it's fast but can produce false positives/negatives like any pattern-matching approach.

## Project structure

```
app/src/main/java/com/permissionscanner/app/
├── MainActivity.kt              # Entry point, sets up Compose content
├── scanner/
│   └── Scanner.kt               # Core scan logic: scanUri() / scanPath(), regex patterns
└── ui/
    ├── ScannerScreen.kt         # Compose UI: file picker, path input, results list
    └── ScannerViewModel.kt      # State management (Idle / Scanning / Done / Error)
```

## Requirements

- Android Studio (latest stable)
- Android SDK with API 34 (compile/target), min supported: **API 26 (Android 8.0)**
- Kotlin 1.9.23 / AGP 8.3.2 (managed via the Gradle version catalog in `gradle/libs.versions.toml`)

## Building

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`. You can also open the project directly in Android Studio and run it on a device or emulator.

> Building from Termux on-device works the same way via `./gradlew assembleDebug`, as long as the Android SDK/build-tools are installed and `ANDROID_HOME` is set.

## Usage

1. Launch the app.
2. Either:
   - Tap **Pick ZIP / APK** to choose a file through the system picker, or
   - Type a full file path into the text field and tap **Scan** (or hit "Go" on the keyboard).
3. Review findings, grouped and color-coded by category, each showing the file, matched tokens, and line numbers.
4. Tap **Reset** to clear results and scan another file.

## Permissions

The app requests `READ_EXTERNAL_STORAGE` (capped at `maxSdkVersion=32`) to support scanning files via legacy storage paths on older Android versions. On Android 13+, file access goes through the system file picker (Storage Access Framework), which needs no runtime permission.

## Limitations

- Pattern matching is regex-based, not AST/bytecode-aware — it can flag strings inside comments or unrelated text, and won't catch obfuscated or dynamically constructed identifiers.
- `.smali` files are only meaningfully present if the input APK has already been disassembled into that format; raw APKs contain `.dex`, which isn't decompiled by this tool.
- No persistence — results exist only for the current scan session.

## License

No license file is currently included in this repository. Add one (e.g. MIT, Apache-2.0) if you intend to share or open-source this project.
