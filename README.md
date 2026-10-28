# Sanctuary

Aplikasi Android native untuk melihat berapa banyak gestur scroll yang kamu
lakukan di TikTok, Instagram, dan YouTube. Offline-first, tanpa email/Google,
dengan akun anonim opsional untuk sinkronisasi dan backup JSON mandiri.

> Status: **MVP 0.1.0 + cloud sync**. Build debug/release dan seluruh unit test
> lulus. Verifikasi perangkat nyata untuk akurasi hitung TikTok/Instagram/YouTube
> masih diperlukan.

| Onboarding | Dashboard |
|---|---|
| ![Onboarding Sanctuary](docs/screenshots/onboarding.png) | ![Dashboard Sanctuary](docs/screenshots/dashboard.png) |

## Kenapa Android dulu

Android memberi aplikasi akses ke event `TYPE_VIEW_SCROLLED` lewat
AccessibilityService. iOS tidak menyediakan cara resmi untuk menghitung scroll di
dalam aplikasi pihak ketiga; Screen Time hanya memberi durasi/pemakaian kasar.
Karena itu Sanctuary dibangun sebagai Android native, bukan janji cross-platform
yang setengah bekerja.

## Fitur

| Fitur | Status |
|---|---|
| Hitung scroll TikTok, Instagram, YouTube | Ada, lewat AccessibilityService |
| Hitung perpindahan YouTube Shorts | Ada, lewat transisi judul di node YouTube |
| Logo resmi platform | Ada, aset merek masing-masing |
| Dashboard harian + target | Ada |
| Riwayat 7/30 hari + detail sesi | Ada |
| Profil, niat harian, batas scroll | Ada |
| Jeda hitung | Ada |
| Offline-first | Ada; network hanya untuk sync opsional |
| Backup JSON via file picker | Ada |
| Import JSON | Belum |
| Supabase anonymous sync | Ada, opt-in; token terenkripsi |
| Email/Google sign-in | Tidak dipakai |
| iOS | Tidak didukung |

![Logo platform resmi](docs/screenshots/platforms.png)

## Batasan yang penting

Android memberi sinyal per **package**, bukan per tab. Instagram feed dan Reels
tetap tidak bisa dipisah. YouTube Shorts memakai pager yang tidak mengirim
`TYPE_VIEW_SCROLLED`; Sanctuary mendeteksi pergantian Shorts dari judul dan nama
channel yang tampil di permukaan player. Yang dihitung adalah **perpindahan
Shorts**, bukan video yang ditonton sampai selesai. Feed YouTube biasa tetap
dihitung lewat scroll event package.

Satu gestur yang selesai dihitung satu. Event fling dari framework digabung
dengan jeda tenang 350 ms per aplikasi: event selama burst tidak menambah angka,
tetapi gestur baru setelah jeda akan dihitung. Sesi baru dimulai setelah 30 menit
tanpa scroll. Angka bisa lebih rendah dari persepsi pengguna bila aplikasi target
tidak mengirim event yang bisa dibaca.

## Arsitektur

```
app
├── feature:onboarding
├── feature:dashboard
├── feature:history
├── feature:profile
├── core:designsystem
├── core:tracking        AccessibilityService + gate
├── core:sync            anonymous auth, encrypted token, push sync
├── core:data            repository, DataStore, backup JSON
├── core:database        Room
└── core:model           model, statistik, kontrak backup
```

- UI: Jetpack Compose + Material 3.
- State: ViewModel + StateFlow + Flow dari Room/DataStore.
- DI: composition root manual (`AppContainer` / `SanctuaryData`), tanpa framework.
- Offline: Room adalah source of truth. Sync hanya berjalan jika diaktifkan;
  kegagalan jaringan tidak menghapus data.
- Backup: `kotlinx.serialization` ke `BackupDocument` versi 1.
- Cloud: satu anonymous user per perangkat, access/refresh token dienkripsi
  Android Keystore, push idempoten lewat PostgREST.

Detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Build

Prasyarat: JDK 21, Android SDK 36, Gradle wrapper 8.13.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat test
```

`test` mencakup unit test JVM (`core:model`) dan unit test Android
(`core:data`, `core:tracking`, `core:sync`). Varian debug dan release menjalankan
test yang sama.

Salin [local.properties.example](local.properties.example) menjadi `local.properties`
lalu sesuaikan `sdk.dir`. Untuk cloud sync, isi file yang sama (tidak ikut di-commit):

```properties
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=sb_publishable_...
```

Gunakan publishable/anon key. Jangan pernah memakai `service_role` di APK.
Aktifkan **Anonymous Sign-Ins** di Supabase Auth dan jalankan migrasi.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Instal:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

Setelah onboarding, aktifkan **Pelacakan Sanctuary** di
`Settings > Accessibility`. Layanan hanya dikonfigurasi untuk empat package:
TikTok (`com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`), Instagram,
dan YouTube. `canRetrieveWindowContent=true` diperlukan untuk membaca judul
Shorts di YouTube; isi layar tidak disimpan, tidak dilog, dan tidak dikirim.

## Backup JSON

File dibuat lewat Storage Access Framework, jadi tidak perlu izin storage. Backup
ini tidak melewati Supabase; kamu memilih sendiri lokasi filenya.

```json
{
  "format": "sanctuary.backup",
  "schemaVersion": 1,
  "exportedAt": "2026-10-27T08:00:00Z",
  "appVersion": "0.1.0",
  "profile": {
    "local_id": "local-profile",
    "display_name": "Sahabat",
    "daily_goal": 100,
    "timezone_id": "Asia/Jakarta"
  },
  "sessions": [
    {
      "id": "…",
      "platform": "tiktok",
      "local_day": "2026-10-27",
      "started_at": "…",
      "ended_at": "…",
      "scroll_count": 14,
      "source": "accessibility"
    }
  ]
}
```

Backup tidak berisi isi layar, pesan, akun, atau token.

## Supabase

Satu akun anonim per perangkat, tanpa email/Google. Aktifkan sekali dari kartu
**Cloud sync** di Profil; sesi disimpan terenkripsi dan dipakai ulang otomatis.
Profil dan sesi yang menunggu dikirim lewat PostgREST. Kegagalan jaringan hanya
menunda sync.

![Cloud sync](docs/screenshots/cloud-sync.png)

Batas penting: anonymous user tidak dapat dipulihkan setelah data aplikasi
dihapus atau di perangkat lain. Backup JSON adalah salinan mandiri. Skema dan RLS
ada di [`supabase/migrations/0001_init.sql`](supabase/migrations/0001_init.sql);
langkah aktivasi ada di [`supabase/README.md`](supabase/README.md).

## Verifikasi

| Cek | Hasil |
|---|---|
| `:app:assembleDebug` | Sukses, APK debug 19.4 MB |
| `:app:assembleRelease` (R8) | Sukses, APK unsigned 1.6 MB |
| `:app:lintDebug` | 0 error, 20 warning (versi dependency dan ikon monokrom) |
| Unit test | 33 unik, 58 eksekusi, 0 gagal |
| Emulator API 36 | Onboarding, dashboard, riwayat, profil, jeda, backup JSON |
| Tracking pipeline | Event `TYPE_VIEW_SCROLLED` nyata tercatat, sesi dan UI terupdate |
| Shorts detector | Unit test signature lulus; belum diuji di YouTube asli |
| Backup JSON | Profil + sesi tersimpan dan terbaca |
| Supabase auth | Anonymous sign-in nyata berhasil |
| Supabase sync | Profil dan 1 sesi berhasil dikirim, status "Tersinkron" |
| RLS | Dua anonymous user terisolasi: user B melihat `[]` |
| TikTok/IG/YouTube asli | Belum diuji dengan akun/feed nyata |

Detail bukti dan batasannya ada di [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
Klaim akurasi hitung harus diuji di perangkat dengan ketiga aplikasi target
sebelum dipakai sehari-hari.

## Privasi dan Play Store

Penggunaan AccessibilityService untuk memantau aplikasi lain memerlukan
disclosure menonjol dan deklarasi kebijakan Play. Untuk penggunaan pribadi,
sideload aman. Jangan mengubah konfigurasi layanan untuk membaca konten tanpa
memperbarui disclosure dan kebijakan privasi.

Cloud sync mengirim profil dan hitungan scroll ke project Supabase milikmu.
Tidak ada isi layar, pesan, atau token pihak ketiga. Untuk Shorts, judul dan
nama channel dibaca di memori perangkat saja, tidak masuk database.

Logo TikTok, Instagram, dan YouTube adalah merek milik pemegang hak masing-masing.
Sumber dan catatan atribusi ada di [docs/ASSETS.md](docs/ASSETS.md). Jangan pernah
menaruh `service_role` key di aplikasi; RLS adalah batas keamanan utama.
