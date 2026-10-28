# ARCHITECTURE - Sanctuary 0.1.0

## Prinsip

1. Offline-first: database lokal adalah source of truth. Network hanya dipakai
   sync opt-in dan kegagalan jaringan tidak menghapus data.
2. Modular: tracking, data, sync, desain, dan fitur tidak saling mengetahui detail
   implementasi masing-masing.
3. Gagal tertutup: layanan tidak mencatat sampai preferensi jeda terbaca.
4. Jujur: yang dihitung adalah event scroll per package, bukan video yang ditonton.

## Graph module

```
app
 ├── feature:onboarding ──┐
 ├── feature:dashboard ───┤
 ├── feature:history ─────┼── core:data ── core:database ── core:model
 ├── feature:profile ─────┤        └────── core:model
 ├── core:tracking ───────┤
 ├── core:sync ───────────┘
 └── core:designsystem ─────── core:model
```

Aturan dependensi:

- `core:model`: Kotlin/JVM murni. Tidak tahu Android.
- `core:database`: Room. Hanya `core:data` yang boleh mengimpornya.
- `core:data`: repository, DataStore, SAF backup, composition root `SanctuaryData`.
- `core:tracking`: AccessibilityService + gate. Menerima repository lewat
  `TrackingDependenciesProvider` yang diimplementasikan `SanctuaryApp`.
- `core:sync`: anonymous Supabase auth, token Keystore, push sync. Tidak tahu UI.
- `feature:*`: Compose + ViewModel. Tidak membuat database atau mengakses DAO.
- `app`: shell, navigation, `AppContainer`, tidak tahu Room.

## Alur mencatat scroll

```
AccessibilityEvent
        │
        ├── TYPE_VIEW_SCROLLED (TikTok/Instagram/YouTube)
        │       │
        │       ▼
        │   ScrollEventGate.accept(elapsedRealtime)
        │       │ burst fling? ── ya ──▶ abaikan
        │       ▼
        │   record
        │
        └── TYPE_WINDOW_CONTENT_CHANGED (YouTube)
                │
                ▼
        ShortsTransitionDetector
          ├── root bukan permukaan Shorts? ──▶ reset
          ├── ambil judul + channel dari node (maks 160 description)
          ├── signature sama? ──▶ abaikan
          └── signature berubah ──▶ record
        │
        ▼
ScrollRepository.recordScroll(platform, Instant.now())
        │
        ▼
Room transaction
  ├── cari sesi platform yang sama di localDay yang sama
  │   dengan endedAt >= now - 30 menit
  ├── ada  : scrollCount + 1, endedAt = max(now, endedAt)
  └── tidak: insert sesi baru
        │
        ▼
Flow Room ──▶ Dashboard / History
```

Gate scroll memakai **jeda tenang 350 ms**. Event pertama dalam burst dihitung;
event berikutnya selama kurang dari 350 ms sejak event terakhir diabaikan. Setelah
jeda, gestur baru dihitung. Ini mencegah satu fling panjang menjadi beberapa
scroll.

YouTube Shorts tidak mengirim `TYPE_VIEW_SCROLLED`. Detector membaca judul dan
nama channel dari node YouTube dengan throttle 300 ms, lalu menghitung satu
transisi saat signature berubah. Detector hanya berjalan untuk YouTube; judul
tidak disimpan, tidak dilog, dan tidak dikirim. Saat berada di Shorts, event
scroll YouTube diabaikan agar tidak double count. Jika view ID Shorts berubah,
detector gagal tertutup: `isInShorts` tetap false dan hitungan kembali ke scroll
biasa.

Layanan dimulai dengan `paused=true`; setelah DataStore mengirim nilai tersimpan,
status berubah. Jika pengguna menjeda, event tetap diterima Android tetapi tidak
ditulis.

## Database

`SanctuaryDatabase` versi 1, dua tabel:

- `profiles`: satu baris lokal (`local-profile`).
- `scroll_sessions`: satu baris per segmen sesi, index `localDay`,
  `(platform, localDay)`, `endedAtMillis`, `syncState`.

`syncState` (`PENDING`, `SYNCED`, `FAILED`) dipakai outbox sync. `PENDING` berarti
belum terkirim; `SYNCED` hanya ditulis setelah response PostgREST 2xx. Kegagalan
jaringan tidak mengubah state, jadi baris tetap dicoba pada sync berikutnya.
`device_id` berasal dari DataStore untuk atribusi perangkat.

Waktu disimpan sebagai epoch millis UTC; `localDay` disimpan sebagai tanggal lokal
ISO pada saat sesi dimulai. Zona waktu diambil dari profil dan disimpan sebagai
ID IANA.

## Backup

`JsonBackupRepository`:

1. `ensureDefault()` untuk memastikan profil ada.
2. `allSessions()` dari Room.
3. Bentuk `BackupDocument` (`schemaVersion=1`).
4. Serialisasi pretty-print ke `ContentResolver.openOutputStream(uri, "wt")`.

Tidak ada izin storage. File tujuan dipilih lewat `CreateDocument("application/json")`.
Backup tidak melewati Supabase. Import belum diimplementasikan; schema dirancang
flat supaya import bisa divalidasi tanpa membuka Room.

## Supabase anonymous sync

`supabase/migrations/0001_init.sql` menyiapkan:

- `profiles` dengan primary key `auth.users.id`.
- `scroll_sessions` dengan `user_id`, `client_id`, `platform`, `local_day`,
  `scroll_count`, `device_id`.
- Unique `(user_id, client_id)` untuk upsert idempoten.
- RLS `auth.uid() = id` / `auth.uid() = user_id`.
- Trigger `updated_at`.

Alur aktivasi:

```
Profil > Aktifkan sinkronisasi
        │
        ▼
POST /auth/v1/signup (anonymous, tanpa email/Google)
        │  access_token + refresh_token
        ▼
EncryptedSessionStore (AES/GCM, Android Keystore)
        │
        ▼
POST /rest/v1/profiles?on_conflict=id
POST /rest/v1/scroll_sessions?on_conflict=user_id,client_id
        │
        ▼
mark SYNCED + last_sync_at
```

- Access token di-refresh 60 detik sebelum kedaluwarsa.
- `SanctuaryApp` menjalankan `syncIfLinked()` saat proses start. Sync tidak
  membuat akun baru kalau belum pernah diaktifkan.
- Sync juga bisa dipicu manual dari kartu Profil. Tidak ada WorkManager; sesi baru
  menunggu sampai app dibuka lagi atau tombol ditekan.
- Hanya push, belum ada pull. Reinstall/perangkat baru tidak bisa memulihkan
  anonymous account, dan backup JSON adalah jalur pemulihan.
- `service_role` tidak pernah ada di aplikasi. Publishable key aman untuk client;
  RLS yang membatasi baris.
- Error jaringan dipetakan ke pesan UI; token dan body auth tidak pernah dilog.

## Keputusan dan trade-off

| Keputusan | Alasan | Konsekuensi |
|---|---|---|
| Android native saja | Satu-satunya jalur scroll-event yang layak | Tidak ada iOS |
| AccessibilityService | API resmi untuk event scroll lintas app | Izin manual, kebijakan Play |
| Per-package count | Android tidak memberi tab context | Instagram feed vs Reels tetap tidak bisa dipisah |
| Shorts title transition | Shorts tidak mengirim scroll event | Perlu `canRetrieveWindowContent` untuk YouTube, berisiko berubah saat YouTube update |
| Room + Flow | Offline, reaktif, transaksional | Perlu migrasi saat skema berubah |
| Backup JSON flat | Portabel, mudah diverifikasi | Import belum ada |
| Tanpa DI framework | Sedikit dependensi, build cepat | Wiring manual di `AppContainer` |
| Anonymous auth | Tanpa email/Google dan friction rendah | Terikat perangkat, tidak bisa lintas HP |
| `INTERNET` permission | Dibutuhkan sync opt-in | Bukan lagi app tanpa network permission |
| Token di Keystore | Tidak exportable, tidak ikut backup | Bisa hilang jika Keystore di-reset |
| Serif display + sage palette | Kesan tenang, bukan dashboard kompetitif | Bukan gaya aplikasi produktivitas umum |

## Verifikasi

Dilakukan di emulator Android 16 (API 36), build debug:

- Onboarding, penyimpanan profil, dashboard kosong, riwayat kosong, profil.
- Izin aksesibilitas diaktifkan lewat Settings; status "Hitung aktif" muncul.
- Event `TYPE_VIEW_SCROLLED` dari permukaan scroll lokal menghasilkan count,
  sesi, riwayat, dan detail sesi (debug config sementara; Chrome bukan target
  produksi).
- Jeda tracking menghentikan penambahan count.
- Backup JSON dengan sesi berhasil diunduh dan berisi profil + sesi.
- Anonymous sign-in nyata ke Supabase berhasil.
- Upsert profil dan 1 sesi berhasil; UI menampilkan "Tersinkron · 1 sesi dikirim".
- RLS dua anonymous user: A melihat barisnya, B mendapat `[]` untuk profil dan
  sesi.
- Unit test: 33 unik, 58 eksekusi, 0 gagal; termasuk MockWebServer untuk signup,
  refresh, upsert profil, upsert sesi, reuse sesi, dan kegagalan offline.
- Shorts signature test: channel/title, label Indonesia, chrome, progress timer,
  dan transisi judul; 6 kasus lulus. Belum diuji pada YouTube asli.
- Logo platform resmi tampil di riwayat dan dashboard; aset dan atribusi di
  `docs/ASSETS.md`.
- Lint debug: 0 error, 20 warning (mayoritas versi dependency dan ikon monokrom).
- Release R8 berhasil di-build dan dijalankan setelah signing dengan debug key
  (hanya untuk smoke test; APK release yang disimpan tetap unsigned).

Belum diverifikasi:

- TikTok/Instagram/YouTube nyata dengan akun dan feed aktif. Emulator hanya punya
  YouTube yang meminta update.
- View ID dan node Shorts pada versi YouTube di HP pengguna.
- Akurasi Reels vs feed, serta transisi Shorts vs feed YouTube biasa.
- Pull/restore dari cloud, rotasi refresh token setelah berhari-hari, dan
  recovery lintas perangkat.
- Release signing produksi dan Play Store policy review.
