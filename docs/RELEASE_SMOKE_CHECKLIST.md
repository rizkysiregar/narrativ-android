# Smoke checklist rilis

Dijalankan pada **build release kandidat** (R8 aktif), bukan debug: R8 hanya berjalan di release, dan build debug
tidak mengirim telemetry. Jalankan sekali di flavor `dev` (backend dev) dan sekali di track internal `prod`.
Centang hanya kalau hasilnya persis seperti yang tertulis; catat versi (`versionName`/`versionCode`), device, dan Android.

**Prasyarat**
- [ ] `./gradlew :app:assembleDevRelease` (signed) terpasang di dua device: Android 8 (minSdk 26) dan Android 14+.
- [ ] Akun uji: satu akun email+password, satu akun Google, satu akun kedua (untuk follow, block, push).
- [ ] Akses ke backend dev untuk mencabut refresh token / memendekkan umur JWT (lihat S-02), dan ke Firebase console
      (kirim push, ubah Remote Config `min_supported_version_code` dan `update_message`).
- [ ] Crashlytics: mapping file build ini sudah ter-upload (artifact `prod-release-mapping` dari job `release-check`).

## A. Instal dan update

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| A-01 | Instal build **v1.0.2 yang sedang live**, login, buka feed. Update ke kandidat tanpa uninstall | Tetap login, feed tampil. Tidak diminta login ulang (file DataStore `app_pref` tidak berubah) | ☐ |
| A-02 | Instal kandidat baru (bersih), buka pertama kali | Onboarding tampil sekali; setelah selesai tidak muncul lagi saat app dibuka ulang | ☐ |
| A-03 | Android 13+: buka pertama kali setelah login | Izin notifikasi diminta; tolak, lalu pastikan app tetap jalan dan upload (C-01) tetap selesai | ☐ |

## B. Sesi dan token (risiko R1)

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| B-01 | Login email+password salah | Pesan error inline di layar login. **Tidak** keluar dialog sesi berakhir | ☐ |
| B-02 | Login benar → feed → tutup app dari recents → buka lagi | Langsung ke feed, tanpa splash login | ☐ |
| B-03 | Login Google (akun yang sudah terdaftar) | Masuk ke feed. Batalkan picker akun: kembali ke login tanpa error mengganggu | ☐ |
| B-04 | **JWT kedaluwarsa, refresh token masih sah** (butuh JWT umur pendek di dev): buka feed, scroll | Feed tetap berfungsi tanpa logout; di Crashlytics/logcat tidak ada lonjakan error. Tidak ada dialog | ☐ |
| B-05 | **Refresh token dicabut** di backend, lalu tarik refresh feed | Kembali ke login dengan pesan sesi berakhir, sekali saja. Login lagi berhasil dan pesan tidak muncul lagi | ☐ |
| B-06 | Mode pesawat → buka feed → matikan mode pesawat | Dialog "tidak ada koneksi" tampil; **sesi tidak berakhir**; setelah online, refresh berhasil | ☐ |
| B-07 | Logout dari Settings (online) | Kembali ke login. Push ke akun itu tidak lagi diterima di device ini | ☐ |
| B-08 | Logout saat **offline** | Tetap bisa logout dalam ≤ 8 detik, tanpa dialog error | ☐ |

## C. Membuat post dan upload (risiko R2)

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| C-01 | Post **hanya caption** | Kembali ke home, banner progres, notifikasi sukses, post muncul di feed | ☐ |
| C-02 | Post 1 gambar JPG + caption | Sama seperti C-01; gambar tampil | ☐ |
| C-03 | Post 5 media campuran (gambar + video) | Urutan media di feed sama dengan urutan pilihan | ☐ |
| C-04 | Pilih media ke-6 | Pesan batas 5 media; lima pertama tetap | ☐ |
| C-05 | Video 51 MB / video 61 detik / GIF 6 MB / file .bmp | Masing-masing ditolak dengan pesan yang sesuai; media lain yang valid tetap dipilih | ☐ |
| C-06 | Caption 600 karakter | Terpotong di 500 | ☐ |
| C-07 | Mulai upload video lalu **matikan jaringan** | Banner menunjukkan "menunggu koneksi/mencoba lagi"; setelah online upload selesai sendiri | ☐ |
| C-08 | Mulai upload lalu **swipe app dari recents** | Upload tetap selesai (foreground service); post muncul | ☐ |
| C-09 | Batalkan upload dari banner | Banner hilang, tidak ada post | ☐ |

## D. Interaksi

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| D-01 | Like lalu unlike sebuah post; ulangi dengan jaringan mati | Hitungan langsung berubah; saat gagal kembali ke nilai semula | ☐ |
| D-02 | Buka post detail dari feed (post video), **tekan Home, buka app lagi** | **Tidak crash** (verifikasi R3). Ulangi dengan "Don't keep activities" aktif | ☐ |
| D-03 | Komentar: kirim, balas, hapus balasan, hapus komentar | Baris pending tampil dulu; hitungan balasan benar; komentar gagal mengembalikan teks ke input | ☐ |
| D-04 | Follow / unfollow, lihat daftar follower | Hitungan dan daftar sesuai | ☐ |
| D-05 | Report post, block user, lalu buka Blocked users dan unblock | Post pengguna yang diblok hilang dari feed; setelah unblock kembali | ☐ |
| D-06 | Post yang di-takedown (minta backend menandai satu) | Placeholder takedown, bukan media | ☐ |

## E. Push dan deep link

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| E-01 | Push `POST_LIKED` (data `postId`) saat app **ditutup**, tap | App terbuka di post detail | ☐ |
| E-02 | Push `POST_COMMENTED` / `COMMENT_REPLIED` saat app **di background**, tap | Post detail dengan sheet komentar terbuka | ☐ |
| E-03 | Push `NEW_POST` tanpa `postId` (hanya `authorId`, `authorUsername`) | Membuka profil penulis | ☐ |
| E-04 | Push saat app di foreground | Notifikasi muncul (heads-up) dan tap bekerja | ☐ |
| E-05 | Push `POST_LIKED` untuk post yang sudah dihapus | Pesan "tidak tersedia", bukan crash atau layar kosong | ☐ |

## F. Konfigurasi, bahasa, akun

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| F-01 | Remote Config `min_supported_version_code` > versionCode build ini, buka app | Dialog update memblokir, tidak bisa ditutup. Kembalikan ke 0: dialog hilang pada pembukaan berikutnya | ☐ |
| F-02 | Ganti bahasa Indonesia ↔ Inggris di Settings | Seluruh layar berganti, termasuk pesan error toast dan notifikasi; header `Accept-Language` mengikuti (pesan server berganti bahasa) | ☐ |
| F-03 | Buka halaman legal (kebijakan privasi, syarat), tap tautan `mailto:` dan satu tautan ke domain lain | Halaman tampil di dalam app; `mailto:` membuka aplikasi email; tautan ke domain lain tidak melakukan apa-apa (sengaja diabaikan) | ☐ |
| F-04 | Hapus akun (password; lalu akun Google) | Kembali ke login; akun tidak bisa login lagi | ☐ |
| F-05 | Matikan telemetry di Settings | Pengaturan tersimpan setelah app dibuka ulang | ☐ |

## G. Khusus build release (R8, risiko R5)

| ID | Langkah | Hasil yang diharapkan | ✔ |
|---|---|---|---|
| G-01 | Jalankan B-04 dan B-05 di build **release** | Sama seperti di atas (TypeToken `TokenRefresher` dan Gson tidak rusak oleh R8) | ☐ |
| G-02 | Buka post detail dari feed lalu kembali (D-02) di release | Post dibangun dari JSON saved state dengan benar | ☐ |
| G-03 | Mulai upload (C-02), **update app ke build lain** sebelum selesai | Upload yang antre tetap berjalan (nama worker tidak berubah) | ☐ |
| G-04 | Picu satu crash uji di build release internal | Muncul di Crashlytics **dengan stack trace ter-deobfuscate** (nama kelas dan nomor baris terbaca) | ☐ |
| G-05 | Tunggu 5 menit setelah G-04 | Tidak ada `ClassNotFoundException`, `NoSuchMethodError`, atau `JsonSyntaxException` baru di Crashlytics | ☐ |

## Kriteria lulus
- Semua baris A–G tercentang, **kecuali** baris yang dicatat sebagai known issue dan disetujui.
- Tidak ada crash baru di Crashlytics selama sesi smoke.
- Hasil (device, versi, siapa, tanggal) ditulis di bawah ini atau di PR rilis.

| Tanggal | Penguji | Build | Device / Android | Hasil | Catatan |
|---|---|---|---|---|---|
|  |  |  |  |  |  |
