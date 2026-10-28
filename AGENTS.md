# AGENTS.md - Sanctuary

## Tujuan

Android native untuk menghitung gestur scroll TikTok/Instagram/YouTube,
offline-first, modular, dengan backup JSON dan sync Supabase anonymous opt-in.
Satu akun anonim per perangkat, tanpa email/Google.

## Perintah

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleRelease --console=plain
.\gradlew.bat test --console=plain
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

Tidak ada device default. Emulator QA: `Sanctuary_API_36` (x86_64, API 36) di
`C:\Users\alexa\.android\avd\Sanctuary_API_36.avd`.

## Struktur

- `app`: shell, navigation, `AppContainer`, Application, Activity.
- `feature/*`: layar + ViewModel. Jangan taruh akses Room langsung di sini.
- `core:data`: repository, DataStore, backup. Ini composition root data.
- `core:database`: Room entity/DAO. Hanya dipakai `core:data`.
- `core:tracking`: AccessibilityService, gate, status checker.
- `core:sync`: anonymous auth, Keystore token store, PostgREST push sync.
- `core:designsystem`: token warna/tipe/shape dan komponen visual.
- `core:model`: model murni Kotlin + statistik + kontrak JSON.

## Jebakan

- JDK 17 tidak terpasang. Compile pakai JDK 21 dengan target 17; jangan memakai
  `jvmToolchain(17)` karena Gradle akan mencoba download toolchain.
- Module Android harus punya `compileOptions` 17 dan `compilerOptions.jvmTarget`
  17 agar tidak mismatch.
- `SanctuaryDatabase` tidak boleh bocor ke `app`. Tambah repository di
  `core:data` dan ekspos lewat `SanctuaryData`.
- Accessibility config membatasi `packageNames` ke empat package target.
  `canRetrieveWindowContent=true` hanya dipakai untuk membaca judul/channel
  Shorts YouTube di memori. Jangan menambah package, menyimpan, atau melog isi
  node tanpa update privacy disclosure.
- Scroll dihitung per package, bukan per tab. Instagram feed vs Reels tidak bisa
  dipisah. YouTube Shorts dihitung sebagai transisi judul, bukan video ditonton.
  Jangan ubah copy menjadi klaim "video ditonton".
- Detector Shorts bergantung pada view ID YouTube (`reel_watch_player`). Uji ulang
  saat YouTube berubah; kegagalan harus turun ke hitungan scroll biasa, bukan
  crash atau double count.
- Layanan mulai dengan `paused=true` sampai DataStore terbaca (fail closed).
- `INTERNET` sudah ada untuk sync opt-in. Jangan menambah network call di luar
  `core:sync` tanpa review privasi.
- `SUPABASE_URL` dan `SUPABASE_ANON_KEY` dibaca dari `local.properties` ke
  BuildConfig. Jangan commit nilainya; jangan pernah memakai `service_role` di APK.
- Anonymous sign-in harus aktif di Supabase. RLS adalah batas keamanan; uji dua
  user sebelum mengubah policy.
- Token disimpan terenkripsi di Android Keystore. Jangan log token, body auth,
  atau ciphertext. Jangan menaruh token di backup JSON.
- Anonymous account terikat perangkat. Menghapus data app menghilangkan akses
  cloud; backup JSON tetap jalur pemulihan. Jangan klaim sync lintas perangkat.
- Sync memakai upsert idempoten `(user_id, client_id)` dan menandai `SYNCED`
  hanya setelah response 2xx.
- Backup JSON adalah kontrak publik. Naikkan `BackupDocument.CURRENT_SCHEMA_VERSION`
  jika field berubah/bermakna berubah.
- Jangan commit `local.properties`, token, atau key.

## Verifikasi wajib

- `:app:assembleDebug` dan `:app:assembleRelease` sukses.
- `test` sukses (termasuk `core:model` yang JVM-only dan `core:sync`).
- Untuk perubahan tracking, uji di perangkat/emulator dengan app target nyata.
  Emulator tidak membuktikan event dari TikTok/Instagram/YouTube.
- Untuk perubahan sync, uji anonymous sign-in + upsert profil + upsert sesi ke
  project Supabase nyata, bukan hanya MockWebServer.
