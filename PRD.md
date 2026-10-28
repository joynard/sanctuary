# PRD - Sanctuary

## Masalah

Short-video feed dirancang untuk scroll tanpa jeda. Pengguna sering tidak punya
angka konkret tentang seberapa banyak mereka scroll, sehingga sulit membuat
keputusan kecil: berhenti, jeda, atau mengganti aktivitas.

## Tujuan

Memberi cermin yang tenang dan jujur: hitungan gestur scroll harian di tiga
aplikasi, tersimpan lokal, bisa dilihat sebagai pola, dan bisa diekspor.

Bukan tujuan: memblokir aplikasi, memberi diagnosis kesehatan mental, mengawasi
orang lain, atau mengklaim jumlah video yang ditonton.

## Pengguna

Satu pengguna dewasa pada perangkat Android pribadi. Tidak ada multi-user atau
dashboard orang tua. Akun cloud memakai anonymous user Supabase, tanpa email atau
Google, dan bersifat opsional serta terikat ke perangkat.

## Kebutuhan fungsional

1. Menghitung gestur scroll saat TikTok/Instagram/YouTube aktif, termasuk
   transisi YouTube Shorts, dengan izin accessibility yang eksplisit.
2. Menampilkan total hari ini, target harian, sisa ruang, dan rincian per app.
3. Menyimpan sesi (mulai, selesai, jumlah) dan menampilkan riwayat 7/30 hari.
4. Menyimpan profil lokal: nama panggilan, target, niat harian, timezone.
5. Bekerja penuh tanpa internet. Network hanya dipakai setelah cloud sync
   diaktifkan.
6. Mengekspor profil + seluruh sesi ke JSON lewat file picker.
7. Bisa menjeda pencatatan tanpa mencabut izin Android.
8. Sinkronisasi opt-in ke Supabase dengan satu anonymous user per perangkat,
   tanpa email/Google.
9. Menyimpan access/refresh token di Android Keystore dan mengirim data lewat
   RLS per user.

## Non-fungsional

- Offline-first: Room adalah source of truth.
- Privasi: isi layar tidak disimpan. Untuk YouTube Shorts, judul dan nama
  channel dibaca di memori hanya untuk mendeteksi pergantian. Data hanya
  dikirim jika cloud sync diaktifkan; tidak ada judul, isi layar, atau pesan
  yang dikirim.
- Modular: fitur baru bisa ditambah tanpa menyentuh database/tracking.
- Aksesibel: target kontras WCAG AA, target sentuh minimal 48 dp, dukungan dark
  mode, teks tidak mengecil di bawah 12 sp.
- Performa: chart/daftar tanpa animasi runtime berat; event scroll digabung
  supaya tidak menulis database puluhan kali per detik.

## Metrik keberhasilan

- Instalasi dan onboarding selesai tanpa crash.
- Event scroll dari ketiga aplikasi tercatat di perangkat nyata.
- Pergantian YouTube Shorts bertambah saat swipe di perangkat nyata.
- Backup JSON dapat dibuka dan berisi semua sesi.
- Tidak ada network request sebelum cloud sync diaktifkan.
- Anonymous sign-in, upsert profil, dan upsert sesi berhasil dengan RLS aktif.

## Risiko

| Risiko | Mitigasi |
|---|---|
| App target tidak mengirim event scroll | Uji per app; dokumentasikan batas; pertimbangkan mode manual |
| YouTube mengganti view ID Shorts | Detector gagal tertutup ke scroll biasa; uji ulang saat update YouTube |
| Android membatasi aksesibilitas | Onboarding jelas, CTA ke settings, status terlihat |
| Klaim Reels/Shorts terlalu spesifik | Label per app + catatan presisi di UI |
| Kebijakan Play Store | Disclosure, deklarasi accessibility, opsi sideload |
| Anonymous account hilang saat data app dihapus | Backup JSON manual; jelaskan batas ini di UI |
| Ganti perangkat tidak bisa memakai akun yang sama | Diterima untuk versi ini; recovery code bisa ditambahkan nanti |
| Konflik sync | Idempotent `(user_id, client_id)`, RLS, upsert server |
| Token bocor | Android Keystore AES/GCM, tanpa logging token, tidak lewat backup |
