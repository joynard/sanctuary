# STYLEGUIDE - Sanctuary

## Prinsip

Tenang, jujur, tidak menghakimi. UI harus terasa seperti ruang, bukan dashboard
kompetisi. Angka adalah cermin, bukan skor.

- Banyak ruang kosong, sedikit garis, tanpa bayangan tajam.
- Warna desaturasi: sage, oat, tanah liat, mist blue.
- Tidak ada merah untuk melewati target; gunakan clay/tertiary.
- Tidak ada streak, ranking, notifikasi menyalahkan, atau copy "gagal".
- Animasi hanya transisi state singkat; tidak ada animasi dekoratif terus-menerus.

## Palet

### Light

| Token | Hex | Pemakaian |
|---|---|---|
| background | `#F4F1E8` | Oat, latar utama |
| surface | `#FBF8F1` | Kartu |
| primary | `#5A7A67` | Sage, aksi/progress |
| primaryContainer | `#DCE9DF` | Kartu hero, pill |
| secondary | `#6F8174` | Aksen sekunder |
| tertiary | `#A3836A` | Clay, batas terlampaui |
| onSurface | `#1E2A24` | Teks utama |
| outlineVariant | `#CFD2C7` | Border halus |
| error | `#A15C56` | Error fungsional saja |

### Dark

| Token | Hex | Pemakaian |
|---|---|---|
| background | `#121815` | Night |
| surface | `#19211D` | Kartu |
| primary | `#A6C4B1` | Sage terang |
| primaryContainer | `#2C4738` | Kartu hero |
| tertiary | `#D8B79D` | Clay |
| onSurface | `#E4EAE2` | Teks |
| outlineVariant | `#3E4B43` | Border |

Warna platform (bukan warna brand asli, sengaja didesaturasi):

| Platform | Light | Dark |
|---|---|---|
| TikTok | `#4F736E` | `#8FB7AF` |
| Instagram | `#A47A72` | `#C9A39A` |
| YouTube | `#9B7652` | `#D0AE87` |

## Tipografi

- Display/headline: `FontFamily.Serif`, normal weight, tracking negatif tipis.
- Body/label: `FontFamily.SansSerif`, ukuran 12-16 sp.
- Angka besar memakai `displayMedium` (44 sp); jangan lebih dari satu angka besar
  per layar.
- Label seksi: uppercase, 11 sp, tracking 1 sp.

## Shape dan spacing

- Skala shape: 10/14/20/28/36 dp. Kartu memakai 28 dp.
- Padding layar horizontal 24 dp. Jarak antar kartu 16 dp.
- Target sentuh minimal 48 dp; tombol utama 52-56 dp.
- Border kartu 1 dp dengan alpha 0.45.

## Komponen

- `SanctuaryBackground`: gradient vertikal + glow radial halus.
- `SoftCard`: surface 80% opacity, border tipis, shape 28 dp.
- `QuietProgressRing`: track dan progress round cap; jangan pakai warna merah.
- `RhythmBarChart`: bar tipis, bar tertinggi di-highlight, garis target putus.
- `SanctuaryMark`: tiga busur, bukan grafik progress.

## Copy

- Bahasa Indonesia, sapaan "kamu".
- Hindari "gagal", "kecanduan", "harus", "seharusnya".
- Contoh baik: "Masih ada 24 scroll sebelum batas harian."
- Contoh baik: "Batas harian sudah terlewati. Tidak apa-apa; pilih jeda dan mulai lagi."
- Jangan mengklaim "Reels" atau "Shorts" sebagai video yang ditonton. Instagram
  tetap per package; YouTube Shorts hanya transisi judul.

## Logo platform

- Gunakan aset merek resmi dari `docs/ASSETS.md`; jangan menggambar ulang.
- Tampilkan dalam tile putih bersudut membulat agar terbaca di tema terang/gelap.
- Jangan memotong, memutar, atau memberi efek yang merusak bentuk merek.

## Aksesibilitas

- Kontras teks minimal 4.5:1.
- Jangan hanya mengandalkan warna untuk status; sediakan teks/pill.
- Chart punya `contentDescription` berisi total dan target.
- Switch punya label teks di dekatnya.
- Dukungan dark mode mengikuti sistem.
- Hormati `adjustResize` dan keyboard saat input.
