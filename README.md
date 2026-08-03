# 🟣 MOKA.DL — Universal Downloader (APK Android Native)

Aplikasi Android **asli** (Kotlin, native View, bukan WebView) untuk download video/audio dari TikTok dan YouTube lewat link. Satu layar, tab pilih platform, chip pilih format. APK-nya di-build otomatis oleh **GitHub Actions**, jadi kamu nggak perlu compile apa-apa di HP.

## ✨ Fitur

- Tab pilih platform: **TikTok** atau **YouTube**, tema warna beda tiap platform
- Chip pilih format: **Video** (tanpa watermark) atau **Audio MP3**
- YouTube Video: muncul pilihan resolusi kalau tersedia lebih dari satu
- Popup **"Download Selesai"** muncul otomatis begitu file benar-benar selesai terdownload
- File tersimpan rapi per jenis: `Download/MOKA.DL/Video` dan `Download/MOKA.DL/MP3`
- UI 100% custom, bukan webview

## 🚀 Cara Dapetin APK-nya (setelah kode ini di-push ke GitHub)

1. Push semua file project ini ke repo GitHub kamu (lihat langkah di bawah)
2. Buka repo kamu di GitHub lewat browser HP
3. Tap tab **Actions** di bagian atas repo
4. Tunggu proses build selesai (tanda centang hijau ✓), biasanya 3–6 menit
5. Setelah selesai, tap tab **Releases** di halaman utama repo
6. Tap release paling atas, lalu tap file `.apk` di bagian **Assets** untuk download
7. Buka file APK yang sudah kedownload → kalau muncul peringatan "Install blocked", tap **Settings** → aktifkan **Allow from this source** → balik lagi → **Install**

Selesai — aplikasi **MOKA.DL** muncul di home screen kayak aplikasi biasa.

## 📤 Cara Push Project Ini ke GitHub (dari Termux)

```bash
cd cyberdl-android
git init
git add .
git commit -m "Initial commit - MOKA.DL Android native"
git branch -M main
git remote add origin https://github.com/CixxyOfficial/NAMA-REPO.git
git push -u origin main
```

Setiap kali kamu push perubahan baru, GitHub Actions otomatis build ulang APK dan bikin release baru.

## 🛠️ Struktur Project

```
cyberdl-android/
├── .github/workflows/build-apk.yml   # otomatis build APK di GitHub
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/cyberdl/tiktok/
│       │   ├── MainActivity.kt        # satu-satunya layar: tab, chip, resolve, download, popup
│       │   ├── TikTokApi.kt           # resolve link TikTok → data video/audio
│       │   └── YouTubeExtractor.kt    # resolve link YouTube → data video/audio (NewPipeExtractor)
│       └── res/                       # layout, warna, drawable
├── build.gradle.kts
└── settings.gradle.kts
```

## ⚠️ Catatan

- Fitur TikTok pakai layanan pihak ketiga publik untuk mengambil link tanpa watermark. Kalau formatnya berubah, resolve bisa gagal — kabari saya, gampang diperbaiki di `TikTokApi.kt`.
- Fitur YouTube pakai [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (GPLv3) — bisa berhenti berfungsi sewaktu-waktu kalau YouTube mengubah sistem proteksinya.
- Gunakan hanya untuk konten yang boleh kamu simpan untuk keperluan pribadi, dan hormati hak cipta pembuat konten.
