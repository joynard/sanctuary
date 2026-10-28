# Supabase

Status: **anonymous sync aktif, push-only**. Satu akun anonim per perangkat,
tanpa email/Google. Aplikasi tetap berjalan offline dan hanya menghubungi
Supabase setelah pengguna menekan **Aktifkan sinkronisasi**.

## Konfigurasi project

1. Aktifkan **Anonymous Sign-Ins**:
   `Authentication` → `Providers` → `Anonymous` → enable.
2. Terapkan migrasi `migrations/0001_init.sql` lewat SQL Editor atau
   `supabase db push`.
3. Isi `local.properties` (tidak di-commit):

```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=sb_publishable_...
```

Gunakan publishable/anon key. Jangan pernah memakai `service_role` di APK.

## Alur client

- `POST /auth/v1/signup` dengan body anonymous, tanpa email/password.
- Access token dan refresh token dienkripsi AES/GCM dengan Android Keystore.
- Refresh token dipakai otomatis 60 detik sebelum access token kedaluwarsa.
- `POST /rest/v1/profiles?on_conflict=id` dengan `Prefer: merge-duplicates`.
- `POST /rest/v1/scroll_sessions?on_conflict=user_id,client_id` per batch 200.
- Baris lokal ditandai `SYNCED` hanya setelah response 2xx.
- Sync otomatis saat app start jika sudah pernah diaktifkan; bisa dipicu manual.

## Keamanan

- RLS `auth.uid() = id` untuk profil dan `auth.uid() = user_id` untuk sesi.
- `client_id` unik per user untuk idempotensi.
- Anonymous user memakai role `authenticated`, bukan role `anon`.
- Jangan membuat policy yang mempercayai `user_id` dari body tanpa `auth.uid()`.
- Uji dengan dua anonymous user: user A tidak boleh membaca baris user B.
- Token tidak masuk backup JSON dan tidak pernah dilog.

## Batasan

- Anonymous account terikat perangkat. Clear data/uninstall = kehilangan akses
  cloud; backup JSON tetap salinan mandiri.
- Belum ada pull/restore. Reinstall tidak mengambil data lama dari cloud.
- Belum ada penghapusan akun/data dari UI.
- Tidak ada WorkManager; sesi baru menunggu app dibuka lagi atau tombol sync.
- CAPTCHA anonymous sign-in tidak didukung. Jika CAPTCHA diaktifkan di Supabase,
  aktivasi akan gagal sampai alur token CAPTCHA ditambahkan.

## Bukti uji

- Anonymous sign-in ke project nyata: berhasil.
- Upsert profil + 1 sesi: berhasil, status UI "Tersinkron · 1 sesi dikirim".
- RLS dua anonymous user: user A melihat 1 profil + 1 sesi; user B melihat `[]`
  untuk keduanya.
- MockWebServer tests: signup, refresh, upsert profil, upsert sesi, reuse sesi,
  error offline.
