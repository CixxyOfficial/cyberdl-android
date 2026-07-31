# 🟢 CyberDL — TikTok Downloader (APK Android Native)

Aplikasi Android **asli** (Kotlin, native View, bukan WebView) bertema cyberpunk untuk download video TikTok tanpa watermark lewat link. APK-nya di-build otomatis oleh **GitHub Actions**, jadi kamu nggak perlu compile apa-apa di HP.

## ✨ Fitur

- Paste link TikTok → download video tanpa watermark
- Mode Audio: extract MP3 dari link **TikTok maupun YouTube**
- Progress bar native, riwayat download di dalam app
- UI 100% custom (tema HUD futuristik), bukan webview
- Icon aplikasi sendiri, muncul di home screen kayak app biasa
- File hasil download masuk ke folder terpisah: `Download/CyberDL/Video` dan `Download/CyberDL/MP3`

## 🚀 Cara Dapetin APK-nya (setelah kode ini di-push ke GitHub)

1. Push semua file project ini ke repo GitHub kamu (lihat langkah di bawah)
2. Buka repo kamu di GitHub lewat browser HP
3. Tap tab **Actions** di bagian atas repo
4. Tunggu proses build selesai (tanda centang hijau ✓), biasanya 3–6 menit
5. Setelah selesai, tap tab **Releases** di halaman utama repo (di sisi kanan/bawah, tergantung tampilan)
6. Tap release paling atas (misal "CyberDL build #1"), lalu tap file `.apk` di bagian **Assets** untuk download
7. Buka file APK yang sudah kedownload → kalau muncul peringatan "Install blocked", tap **Settings** → aktifkan **Allow from this source** → balik lagi → **Install**

Selesai — aplikasi CyberDL muncul di home screen kayak aplikasi biasa.

## 📤 Cara Push Project Ini ke GitHub (dari Termux)

```bash
cd cyberdl-android
git init
git add .
git commit -m "Initial commit - CyberDL Android native"
git branch -M main
git remote add origin https://github.com/USERNAME/NAMA-REPO.git
git push -u origin main
```

Setiap kali kamu push perubahan baru, GitHub Actions otomatis build ulang APK dan bikin release baru — tinggal ulangi langkah "Cara Dapetin APK" di atas.

## 🛠️ Struktur Project

```
cyberdl-android/
├── .github/workflows/build-apk.yml   # otomatis build APK di GitHub
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/cyberdl/tiktok/
│       │   ├── MainActivity.kt        # logic utama UI + download
│       │   ├── TikTokApi.kt           # resolve link TikTok → video url
│       │   └── HistoryAdapter.kt      # daftar riwayat download
│       └── res/                       # layout, warna, tema cyberpunk
├── build.gradle.kts
└── settings.gradle.kts
```

## ⚠️ Catatan

- Aplikasi ini pakai layanan pihak ketiga publik untuk mengambil link video TikTok tanpa watermark. Kalau sewaktu-waktu layanan itu berubah format responsnya, proses resolve bisa gagal — kabari saya kalau itu terjadi, gampang diperbaiki di `TikTokApi.kt`.
- Fitur **YouTube to MP3** pakai library [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) — library yang sama dipakai aplikasi NewPipe. Karena YouTube aktif mengubah sistem proteksinya, fitur ini **berpotensi berhenti berfungsi sewaktu-waktu** dan mungkin perlu di-update library-nya secara berkala. Kalau tiba-tiba gagal terus, kabari saya.
- NewPipeExtractor berlisensi **GPLv3** — kalau nanti aplikasi ini mau dipublikasikan resmi (misal ke Play Store), perlu diperhatikan implikasi lisensinya (source code project ini sebaiknya juga dibuka).
- Gunakan hanya untuk konten yang boleh kamu simpan untuk keperluan pribadi, dan hormati hak cipta pembuat konten.
