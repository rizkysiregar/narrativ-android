# TEST PLAN — NARRATIV Android

> Dokumen rencana, belum berisi test. Disusun dari kode di branch `master` commit `3bb032b` (17 Nov 2026).
> Semua path relatif terhadap `app/src/main/java/com/rizkysiregar/narrativ/` kecuali disebut lain.

**Skala estimasi usaha** (1 engineer): **S** ≤ ½ hari · **M** 1–2 hari · **L** 3–5 hari.
**Prioritas**: **P0** = blocker rilis production · **P1** = sebaiknya sebelum rilis, boleh menyusul di minggu pertama · **P2** = setelah rilis.

## Status Implementasi

Branch kerja: `chore/test-foundation-phase-0`.

| Task | Status | Catatan |
|---|---|---|
| 0.1 Pin JDK | ✅ | `gradle/gradle-daemon-jvm.properties` (toolchain 21). Build jalan dengan JDK default 25 |
| 0.2 Dependency test | ✅ | Tanpa Hilt-testing & Compose UI test untuk androidTest (menyusul di Phase 3). Robolectric 4.14.1 belum dicoba di API 36 |
| 0.3 `testOptions` | ✅ | |
| 0.4 Hapus template test | ✅ | |
| 0.5 Helper test | ✅ | `MainDispatcherRule`, `Fixtures.kt` (fake dan `TestDataStore` menyusul bersama kelas yang dites) |
| 0.6 Lint | ✅ | 11 error diperbaiki, 105 warning dibekukan di `app/lint-baseline.xml` |
| 0.7 CI | 🟡 | `.github/workflows/ci.yml` ditulis dan langkahnya diverifikasi lokal; **belum pernah jalan di GitHub**. Branch protection `master` perlu diatur manual di GitHub |
| 0.8 Kover | ✅ | Laporan saja, belum ada gate |
| 0.9 ktlint + detekt | ⏳ | Ditunda: perlu satu commit format massal terpisah |

Branch Phase 1: `chore/testability-seams-phase-1`.

| Task | Status | Catatan |
|---|---|---|
| 1.1 E1 interface telemetry/string/config | ✅ | `AppAnalytics`, `CrashReporter`, `AppStrings`, `RemoteConfigManager` tetap bernama sama tapi jadi interface (call site tidak berubah). Implementasi: `Firebase*` / `ContextAppStrings`, di-bind di `di/TelemetryModule.kt` |
| 1.2 E2 DataStore | ✅ | `DataStoreManager(DataStore<Preferences>)`, file tetap `app_pref`. 5 test, termasuk snapshot nama file dan semua `PrefKeys` |
| 1.3 E3 `@BaseUrl` | ✅ | Ditambah `@RefreshClient` (client refresh token juga di-inject, karena Chucker dan App Check tidak bisa dibuat di JVM) |
| 1.4 E4 scope/dispatcher | ✅ | `@ApplicationScope` (SupervisorJob + Default), `@IoDispatcher`. `SessionManager` dan `PresignedUploader` |
| 1.5 E5 Clock | ✅ | `RegisterViewModel`, `DeviceRegistrar` |
| 1.6 `PerfTracer` | ✅ | Bagian dari 1.1 |
| 1.9 E9 kode mati | 🟡 | Kode dihapus (`AuthModule`, `UserDataRepository`, `OAuth2LoginState`, param `authService`). Dependency `appauth` dan `RedirectUriReceiverActivity` di manifest **belum** disentuh: menunggu keputusan |
| 1.11 E11 `AppInitializer` | ✅ | `initLocal` / `initServices` / `observeForeground` |
| 1.7 E7 `MediaValidator`, 1.8 E6 `PushTokenProvider` | ⏳ | P1, boleh menyusul |
| 1.10 E10 function reference di `SavedStateHandle` | ✅ | Hand-off post dan handler like pindah ke `PostDetailHandoff` (ViewModel di scope `HomeRoot`), tidak lagi di saved state back stack. Branch `fix/post-detail-handoff-crash` |

Branch Phase 2: `test/phase-2-p0-core` (sesi 1 dan 2). Total **299 unit test**, coverage terukur **37,8% line** setelah adapter SDK dikecualikan (baseline awal 3,2%).

| Task | Status | Catatan |
|---|---|---|
| 2.1 `NetworkUtils` | ✅ | 16 test |
| 2.2 `SessionManager` | ✅ | 7 test, termasuk 16 thread paralel |
| 2.3 Stack OkHttp (`AuthInterceptor` + `TokenRefresher` + `SessionManager`) | ✅ | 17 test vs MockWebServer. Uji mutasi pada guard dedup refresh |
| 2.4 Contract test parsing | 🟡 | 12 test. **Fixture ditulis dari model client, bukan dari backend** (`src/test/resources/fixtures/README.md`). Menunggu contoh respons asli atau spec OpenAPI |
| 2.5 `PresignedUploader` | ✅ | 6 test |
| 2.6 `PostUploadWorker` | ✅ | 14 test (Robolectric, sdk 35) |
| 2.7 Auth VM | ✅ | `AuthViewModel` 8, `RegisterViewModel` 10, `ResetPasswordViewModel` 7 |
| 2.8 `BaseFeedViewModel`, `CommentViewModel` | ✅ | 18 dan 16 test, dengan uji mutasi |
| 2.9 `SettingsViewModel`, `PostFeedViewModel` | ✅ | `SettingsViewModel` 11, `PostFeedViewModel` 15, `MediaValidator` 11 (E7 selesai) |
| P1 yang ikut selesai | ✅ | `DeviceRegistrar` 12, `DeviceSyncWorker` 4, `ApiRemoteSessionCleaner` 6, `LanguageInterceptor` 2, `WorkUploadManager` 7, `WorkDeviceSyncScheduler` 3, repository impl 20 (E6 selesai), `FileMediaPreparationUseCase` 15 dan `ProfilePhotoUseCase` 6 (setelah `ImageCompressor` dan `VideoCompressor` jadi interface) |
| 2.10 Verifikasi R3 | ✅ | **Terkonfirmasi di device** (18 Nov 2026): buka post video lalu tekan Home → `BadParcelableException` / `NotSerializableException: FeedViewModel` dari `FeedRoute$9$1$1`. Diperbaiki di 1.10; perlu dicek ulang di device setelah merge (langkah D-02 smoke checklist) |
| 2.11 E2E session expiry | ⏳ | Butuh emulator; belum diizinkan memakai device |
| 2.12 Smoke checklist release | ✅ ditulis | `docs/RELEASE_SMOKE_CHECKLIST.md`. Belum dijalankan pada build kandidat |
| 2.13 Gate coverage per area | ✅ | Task `verifyCoverageFloors` (Kover 0.9 tidak bisa memfilter per rule), jalan di job CI `verify`. Batas: network 90, session 90, repository 85, auth VM 85, settings+post VM 85, media preparation + upload queue 90, profile photo 85, util 75, total 35 |
| 2.14 Job CI `release-check` | ✅ | Sudah jalan di GitHub (Phase 0) |

Yang sengaja dikecualikan dari ukuran coverage (adapter tipis ke SDK yang tidak bisa jalan di JVM): `CredentialManagerGoogleIdTokenProvider`, `FirebasePushTokenProvider`, `AndroidMediaInspector`, `Firebase*` di `telemetry`, dan `FirebaseRemoteConfigManager`. Logika di sekitarnya dites lewat interface-nya. `FirebaseRemoteConfigManager` punya sedikit logika sendiri (fallback `ifBlank`, mutex refresh) yang belum teruji.

Belum tercakup: paket `media` (2,2%: `BitmapImageCompressor`, `Media3VideoCompressor`, `AvatarCropper`, `VideoCacheManager`; P2 di device), `ui.feature.home.feed` (komponen Compose dan `PostDetailViewModel`), dan jalur pilihan bahasa eksplisit di `LanguageInterceptor` (butuh device).

Temuan sesi ini:
- **R6 terkonfirmasi di level interceptor**: setiap panggilan lewat client utama, termasuk `DeviceRegistrar` dan `RemoteSessionCleaner`, melaporkan kegagalannya ke dialog error global (`aFailingBackgroundCallStillReachesTheGlobalErrorChannel`). Belum diperbaiki, menunggu keputusan.
- **R4 terkonfirmasi**: field wajib yang hilang diparse sebagai `null` di properti non-null (`aMissingRequiredFieldCurrentlyParsesAsANullInANonNullProperty`). Test itu harus dibalik saat E8 diputuskan.
- Plugin Firebase Performance meng-instrumentasi `OkHttpClient` dan membuatnya tidak bisa dibuat di test JVM. Instrumentasi dimatikan untuk build `debug` (telemetry di debug memang tidak aktif). Build `release` tetap ter-instrumentasi.
- Coverage terendah dari area P0: `domain.post` 6,6% (`MediaPreparationUseCase`, `UploadManager`), `ui.feature.home.feed` 22,5% (komponen Compose dan `PostDetailViewModel`).

---

## A. Ringkasan Kondisi Project

### A.1 Stack & arsitektur

| Aspek | Temuan (dari kode) |
|---|---|
| Modul | **Single module** `:app` (`settings.gradle.kts`). ±249 file Kotlin, ±19.800 baris di `main`. |
| Arsitektur | MVVM + lapisan "Clean" ringan: `ui/feature/**` (Route → Screen + `*ViewModel`), `domain/**` (use case, kebanyakan *pass-through* 1 baris ke repository), `data/**` (repository interface + `impl/`, Retrofit API, DataStore). State via `StateFlow`, hasil API dibungkus `ResultState` (`data/remote/model/ResultState.kt`). |
| UI | **Jetpack Compose** penuh (Material3 + sedikit Material2 untuk pull-refresh), Navigation Compose 2.8.3, Coil 3, Media3 ExoPlayer, Lottie. Sebagian besar layar sudah dipisah `XxxRoute` (stateful) vs `XxxScreen` (stateless), bagus untuk test UI. |
| DI | **Hilt** 2.57.2 (+ `hilt-work` untuk worker). Modul: `di/AppModule.kt`, `NetworkModule.kt`, `DataStoreModule.kt`, `AuthModule.kt`. |
| Networking | Retrofit 3 + **Gson** converter + OkHttp 5. Interceptor: `AppCheckInterceptor`, `LanguageInterceptor`, `AuthInterceptor` (refresh token lewat `TokenRefresher`), Chucker (debug). Client terpisah `@UploadClient` untuk PUT presigned (`PresignedUploader`). |
| Persistensi | **Tidak ada Room/SQLite.** Hanya DataStore Preferences (`data/cache/DataStoreManager.kt`, key di `constants/PrefKeys.kt`: JWT, refresh token, userId, throttle device sync) dan SharedPreferences (`TelemetrySettings`, `OnboardingPrefs`). |
| Background | WorkManager: `PostUploadWorker` (upload post multi-media, foreground service `dataSync`), `DeviceSyncWorker` (registrasi FCM token). |
| Firebase | Messaging (push), Crashlytics, Analytics, Performance, Remote Config (force update), App Check (Play Integrity / debug provider). |
| Auth | Email+password, Google Sign-In (Credential Manager, `data/session/GoogleIdTokenProvider.kt`), OTP registrasi & reset password, refresh token *single-use*. |
| SDK | `minSdk 26`, `targetSdk 36`, `compileSdk 36`, Java/JVM 17, Kotlin 2.2.20, AGP 8.10.1, Gradle 8.11.1. |
| Build variant | Flavor `local` / `dev` / `prod` (beda `BASE_URL`) × `debug` / `release`. Release: R8 minify + shrink resources (`app/proguard-rules.pro`). Signing dari `keystore.properties` atau env var. |
| CI | **Tidak ada** (tidak ada `.github/workflows` atau konfigurasi CI lain). |
| Static analysis | Hanya Android Lint bawaan. Tidak ada detekt/ktlint/spotless, tidak ada konfigurasi coverage. |

### A.2 Kondisi test saat ini

Saya menjalankan `./gradlew :app:testDevDebugUnitTest --rerun :app:testProdReleaseUnitTest` dengan JDK 21:

| Suite (`app/src/test/...`) | Jumlah | Hasil | Menguji |
|---|---|---|---|
| `util/TimeAgoTest` | 10 | ✅ | format waktu relatif |
| `ui/feature/home/feed/CommentListRowsTest` | 6 | ✅ | penyusunan baris komentar/reply |
| `ui/feature/home/feed/PostDetailUiStateTest` | 6 | ✅ | mapping `ResultState` → UI state (404, takedown) |
| `notification/PushNotificationsTargetTest` | 5 | ✅ | `PushNotifications.targetFor` |
| `ui/feature/common/ErrorDialogCoordinatorTest` | 5 | ✅ | dedup dialog error |
| `config/AppConfigTest` | 4 | ✅ | `requiresUpdate` |
| `ui/components/EmojiPanelTest` | 4 | ✅ | insert emoji ke `TextFieldValue` |
| `ui/feature/legal/LegalDocumentTest` | 4 | ✅ | parsing path dokumen legal |
| `domain/moderation/ReportReasonTest` | 3 | ✅ | enum alasan report |
| `data/remote/upload/PostUploadInputDataTest` | 2 | ✅ | input data WorkManager tetap utuh setelah persist |
| `ExampleUnitTest` | 1 | ✅ | template (`2+2`), tidak bernilai |
| **Total unit** | **50** | **50/50 lulus** | sama di `devDebug` dan `prodRelease` |
| `androidTest/ExampleInstrumentedTest` | 1 | ⚠️ tidak dijalankan (tidak ada device) | **Pasti gagal**: mengharapkan package `com.rizkysiregar.narrativ_android_native`, padahal `applicationId` = `com.rizkysiregar.narrativ`. |

Catatan:
- Test yang ada valid dan berkualitas (nama deskriptif, fokus perilaku, tanpa mock). Semuanya **pure function / logika kecil**. **Tidak ada satu pun test untuk ViewModel, repository, interceptor, session, worker, atau UI.**
- Dependency test hanya `junit` 4.13.2, `androidx.test.ext:junit`, Espresso, `ui-test-junit4`. Belum ada `kotlinx-coroutines-test`, Turbine, MockWebServer, Robolectric, `work-testing`, `hilt-android-testing`.
- ⚠️ **Toolchain**: dengan JDK default mesin (OpenJDK 25), Gradle 8.11.1 langsung gagal (`What went wrong: 25`). Build hanya jalan dengan JDK 17/21. Developer lain atau CI bisa terkena masalah yang sama.
- Lint `lintDevDebug` **gagal** dengan **11 error, 103 warning** (lihat C.1).

### A.3 Temuan utama & risiko

Diurutkan dari risiko bisnis tertinggi. Poin bertanda 🔎 adalah **dugaan bug dari membaca kode** yang harus dibuktikan oleh test di roadmap, bukan fakta yang sudah terverifikasi.

| # | Risiko | Lokasi | Dampak |
|---|---|---|---|
| R1 | **Auth/session tanpa test.** Logika 401 → refresh → retry → expire ada di `AuthInterceptor` dan `TokenRefresher` (`@Synchronized`, refresh token single-use, `runBlocking` di thread OkHttp). Regresi di sini = user ter-logout massal atau loop 401. | `data/remote/AuthInterceptor.kt`, `data/remote/TokenRefresher.kt`, `data/session/SessionManager.kt` | Kritis |
| R2 | **Pipeline upload post tanpa test.** Banyak cabang: jumlah file ≠ content type, file hilang, 403 presigned → minta URL baru sekali, 5xx → retry s.d. 3×, 429 → gagal, IOException → retry, sukses → hapus file + emit completion. | `data/remote/upload/PostUploadWorker.kt`, `PresignedUploader.kt`, `domain/post/UploadManager.kt`, `domain/post/MediaPreparationUseCase.kt` | Kritis (fitur inti, data user bisa hilang) |
| R3 | ✅ **Terkonfirmasi dan diperbaiki: crash saat user menekan Home di post detail.** `FeedRoute.kt:90-93` (dan pola yang sama di `ExploreRoute.kt:51-54`) menyimpan *bound function reference* `viewModel::toggleLike` ke `SavedStateHandle`. Saat Activity menyimpan state, Bundle akan mencoba men-*serialize* referensi itu beserta ViewModel-nya. Bytecode (`FunctionReferenceImpl`) menunjukkan referensi itu `Serializable`, jadi `set()` tidak menolaknya, tetapi ViewModel di dalamnya tidak bisa di-serialize. Karena Android mem-parcel saved state di `onStop`, crash `NotSerializableException` kemungkinan terjadi **saat user menekan Home**, bukan hanya saat process death. Belum direproduksi di device; cek lewat logcat (task 2.10). | `ui/feature/home/feed/FeedRoute.kt`, `ui/feature/home/explore/ExploreRoute.kt`, `ui/feature/home/feed/PostDetailRoute.kt:103` | Tinggi (crash nyata di device RAM kecil) |
| R4 | **Parsing respons rapuh.** Gson + model Kotlin non-null (`FeedContentItem.username`, `PaginationResponseWrapper.content`, `LoginResponse.token`, dst.). Jika backend menghilangkan field, Gson mengisi `null` ke tipe non-null dan NPE muncul jauh dari titik parsing. Tidak ada contract test. | `data/remote/model/response/**` | Tinggi |
| R5 | **Build release (R8) tidak pernah dites otomatis.** Keep rule Gson berbasis package (`data.remote.model.**`), `TypeToken` anonim di `TokenRefresher`, objek post di-JSON-kan ke nav saved state, `-keepnames` worker. Kesalahan rule baru terlihat di build release. | `app/proguard-rules.pro`, `TokenRefresher.kt:72` | Tinggi |
| R6 | 🔎 **Dialog error global dari panggilan latar belakang.** `DeviceRegistrar` dan `RemoteSessionCleaner` memakai `Retrofit` utama, sehingga lewat `AuthInterceptor`, yang memanggil `apiErrorManager.report(...)` untuk IOException dan non-2xx. Akibatnya user bisa melihat dialog "Cannot reach the server" saat logout offline atau saat device sync gagal, padahal keduanya didesain *best effort/silent*. | `data/repository/DeviceRegistrar.kt`, `data/session/RemoteSessionCleaner.kt`, `AuthInterceptor.kt:36-40,63-71` | Sedang (UX) |
| R7 | **Logika bisnis di ViewModel besar tanpa test**: optimistic like + rollback (`BaseFeedViewModel`), komentar pending/retry/delete thread (`CommentViewModel`, 414 baris), OTP register dengan reuse token 9 menit (`RegisterViewModel`), hapus akun (`SettingsViewModel`). | `ui/feature/**` | Sedang–Tinggi |
| R8 | **String hardcoded melewati i18n**: `CommentViewModel.REFRESH_FAILED` (bahasa Indonesia), `AppConfig.DEFAULT_UPDATE_MESSAGE` dan `DEFAULT_TAKEDOWN_MESSAGE` (bahasa Inggris). | `CommentViewModel.kt:412`, `config/AppConfig.kt`, `FeedsPostPaginationResponse.kt` | Rendah |
| R9 | **Tidak ada jaring pengaman build**: tanpa CI, lint gagal, toolchain JDK tidak dipin. JWT dan refresh token disimpan plaintext di DataStore (dimitigasi `allowBackup="false"`). | root project | Sedang |
| R10 | **Kode mati / dependency tidak terpakai**: `AuthorizationService` (AppAuth) di-inject ke `AuthViewModel` tapi tidak dipakai; `data/repository/UserDataRepository.kt` dan `data/model/OAuth2LoginState.kt` tidak direferensikan. Ini menambah beban testing (constructor `AuthViewModel` butuh `Context`). | `ui/feature/auth/AuthViewModel.kt`, `di/AuthModule.kt` | Rendah |

### A.4 Penilaian testability

| Hambatan | Lokasi | Akibat ke test | Refactor minimal (detail di §E) |
|---|---|---|---|
| Kelas telemetry/config memanggil `Firebase*.getInstance()` di constructor/field | `telemetry/AppAnalytics.kt:20`, `CrashReporter.kt:15`, `config/RemoteConfigManager.kt:31`, `telemetry/PerfTrace.kt` (object statis) | Tidak bisa diinstansiasi di JVM. Hampir semua ViewModel penting (`AuthViewModel`, `CommentViewModel`, `RegisterViewModel`, `PostFeedViewModel`, `SettingsViewModel`, `MainViewModel`) dan `PostUploadWorker` bergantung padanya. | E1: ekstrak interface |
| `AppStrings` butuh `Context` | `i18n/AppStrings.kt` | ViewModel yang menampilkan pesan butuh Robolectric | E1: interface `StringProvider` |
| `DataStoreManager` membuat DataStore dari `Context` | `data/cache/DataStoreManager.kt` | Session/Auth/Splash/Comment VM butuh Robolectric | E2: terima `DataStore<Preferences>` |
| Repository membuat API sendiri via `retrofit.create(...)` lazy | `data/repository/impl/*.kt`, `DeviceRegistrar`, `RemoteSessionCleaner` | Masih bisa dites dengan Retrofit + MockWebServer (tidak wajib refactor). | Opsional: provide `XxxApi` dari Hilt |
| `BuildConfig.BASE_URL` hardcoded | `TokenRefresher.kt:63`, `NetworkModule.provideRetrofit` | Refresh token tidak bisa diarahkan ke MockWebServer; E2E hermetic tidak mungkin | E3: inject `@BaseUrl` |
| Dispatcher/scope hardcoded | `SessionManager.kt:34` (`Dispatchers.IO` scope sendiri), `PresignedUploader` (`Dispatchers.IO` + `delay` retry nyata), `MediaPreparationUseCase`, `ProfilePhotoUseCase`, `PostUploadWorker` (progress scope) | Test nondeterministik / lambat (delay retry 1,5 detik nyata) | E4: inject dispatcher/scope |
| Waktu sistem langsung | `RegisterViewModel` (`System.currentTimeMillis`, token 9 menit), `DeviceRegistrar` (throttle 24 jam) | Tidak bisa menguji batas waktu | E5: inject `Clock`/`() -> Long` |
| Firebase Messaging statis | `DeviceRegistrar.kt:59`, `RemoteSessionCleaner.kt:39,51` | Sync device & logout tidak bisa dites | E6: `PushTokenProvider` |
| `WorkManager.getInstance(context)` di constructor | `UploadManager.kt:39`, `DeviceSyncScheduler` | Butuh `WorkManagerTestInitHelper` (Robolectric). Masih bisa diterima. | Opsional |
| Validasi media di ViewModel memakai `ContentResolver` + `MediaMetadataRetriever` | `PostFeedViewModel.validateMedia` | Batas ukuran/durasi/format tidak bisa dites di JVM | E7: ekstrak `MediaValidator` |
| `android.util.Log` di helper jaringan | `util/NetworkUtils.kt` | Test JVM `safeApiCall` crash `Method ... not mocked` | C: `unitTests.isReturnDefaultValues = true` (pragmatis) |
| ✅ Sudah baik | Repository = interface; use case konstruktornya menerima repository; Screen composable sebagian besar stateless; `PushNotifications.targetFor`, `PostDetailUiState.from`, `AppConfig` sudah pure | Fake repository mudah dibuat | — |

---

## B. Strategi Test per Lapisan

Prinsip: **uji perilaku yang berisiko bisnis**, utamakan **fake** (repository sudah berupa interface) dan **MockWebServer** (stack HTTP asli), mock (MockK) hanya untuk kasus terakhir. Use case yang hanya meneruskan ke repository (`domain/feed/*`, `domain/auth/*`) **tidak** dites satu per satu; mereka ikut teruji lewat test ViewModel yang memakai use case asli + fake repository.

### B.1 Unit test (JVM, `src/test`)

**Tools:** JUnit4 (sudah dipakai), `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`, `MainDispatcherRule`), Turbine (assert `Flow`/`StateFlow`/`Channel`), Truth (opsional, assertion lebih jelas). Robolectric hanya untuk kelas yang benar-benar menyentuh Android API.
**Alasan:** cepat (< 1 detik per suite), bisa jalan di setiap PR, menutup mayoritas logika berisiko.

| Prioritas | Target | Kasus yang wajib dicakup |
|---|---|---|
| **P0** | `util/NetworkUtils.kt` (`safeApiCall`, `flowFromApi`, `flowFromMessageApi`, `flowFromApiOrEmptyOn404`) | Loading lalu Success; `data == null` → Failure(`message`) pada `flowFromApi` tapi Success(`message`) pada `flowFromMessageApi`; `HttpException` dengan body `{"message":..}` → pesan server; body rusak → `GENERIC_ERROR`; `IOException` → `NETWORK_ERROR`; `JsonParseException` → `GENERIC_ERROR`; `throwable` asli tetap terbawa; **`CancellationException` dilempar ulang**; 404 → `emptyValue`. |
| **P0** | `data/session/SessionManager.kt` | `expireSession` hanya sekali walau dipanggil paralel (`AtomicBoolean`); token holder di-null-kan sinkron; DataStore dibersihkan lalu `sessionExpiredMessage` terisi; `markAuthenticated` me-reset; `logout` tidak memancarkan pesan expired; telemetry user di-clear. |
| **P0** | `ui/feature/auth/AuthViewModel.kt` | Login sukses → token, refresh token, userId, username, `isLoggedIn` tersimpan, `tokenHolder` di-set, `markAuthenticated`, `identify`, `DeviceSyncScheduler.schedule(force = true)`; login gagal → tidak ada yang tersimpan + event analytics gagal; Google: `GetCredentialException` → Failure dengan pesan; reset state. |
| **P0** | `ui/feature/auth/regiter/RegisterViewModel.kt` | `verificationRequired=false` → langsung register tanpa OTP; OTP terkirim → step OTP + `sentCount` naik saat resend; verifikasi sukses → register dengan `registrationToken`; register gagal (username dipakai) → kembali ke FORM, **submit ulang < 9 menit memakai token lama tanpa OTP baru**, > 9 menit atau email berbeda → OTP baru (butuh Clock, E5). |
| **P0** | `ui/feature/auth/reset/ResetPasswordViewModel.kt` | Transisi EMAIL_ENTRY → OTP_VERIFICATION → NEW_PASSWORD_INPUT → FINISH hanya saat sukses; gagal tetap di step; `savedResetToken` diisi dari respons verifikasi. |
| **P0** | `ui/feature/home/feed/BaseFeedViewModel.kt` (via `FeedViewModel`) | Optimistic like/unlike + rollback saat Failure; tap ganda saat request berjalan diabaikan; like tersinkron antara `uiState` dan `gridState`; `refresh` membatalkan halaman lama (generation guard, halaman basi tidak menimpa); `loadNextPage` tidak jalan sebelum halaman awal / saat refreshing / `hasMore=false`; `hasMore = !last && content.isNotEmpty()`; error state. |
| **P0** | `ui/feature/home/feed/CommentViewModel.kt` | Kirim komentar → baris pending muncul, event `Sent` + `ScrollToBottom`; sukses → pending jadi final + refresh semua halaman; gagal → pending dihapus, `replyTarget` dikembalikan, `SendFailed(text)` (teks tidak hilang); reply ke thread yang tertutup lalu gagal → thread kembali tertutup; hapus reply → `replyCount` root berkurang dan tag `replyTo*` jawaban di bawahnya dibersihkan; hapus komentar root → thread dan `replyTarget` ikut hilang; ganti post di tengah loading → hasil lama dibuang; `toggleReplies` collapse membatalkan job; batas `MAX_REFRESH_PAGES`. |
| **P0** | `ui/feature/home/settings/SettingsViewModel.kt` | Logout: `remoteSessionCleaner.signOut()` **sebelum** `sessionManager.logout()`, lalu `onDone`; guard `isBusy`; hapus akun dengan password kosong → error lokal; Failure karena `HttpException`/`IOException` → `deleteError = null` (sudah tampil di dialog global), Failure lain → pesan; sukses → `dropPushToken` + logout; pembatalan picker Google → diam. |
| **P0** | `ui/feature/home/post/PostFeedViewModel.kt` | Maks 5 media, duplikat di-skip, caption dipotong ke `MAX_CAPTION_LENGTH`; `submitPost` → `prepareAll` + `UploadManager.enqueue` + `navigateHome`; `MediaValidationException` → `validationError`; exception lain → pesan generik; `canSubmit=false` → tidak ada aksi. Validasi format/ukuran/durasi dites di `MediaValidator` (E7). |
| P1 | `ui/feature/home/feed/PostDetailViewModel.kt` | id bukan UUID → `Unavailable`; load sekali per id; `retry`; toggle like. |
| P1 | `ui/main/MainViewModel.kt`, `ui/feature/splash/SplashViewModel.kt` | `forceUpdateMessage` mengikuti config; `sessionExpiredMessage` diteruskan; Splash mengisi `tokenHolder` dari DataStore; `awaitRemoteConfig` timeout 3 detik tidak memblokir. |
| P1 | `ui/feature/home/search/SearchViewModel.kt` | Debounce (virtual time), query baru membatalkan pencarian lama, 404 → list kosong. |
| P1 | `ui/feature/home/profile/ProfileViewModel.kt`, `FollowListViewModel.kt`, `userdetail/UserDetailViewModel.kt` | Follow/unfollow optimistic + rollback, paging follower. |
| P1 | `domain/post/UploadManager.kt` (Robolectric + `WorkManagerTestInitHelper`) | `enqueue` tanpa media & caption → `IllegalArgumentException`; dua post caption-only identik tidak saling menimpa; media yang sama di-KEEP; `pendingUploads` mengambil caption dari tag; work yang selesai tidak muncul. |
| P1 | `data/remote/DeviceSyncWorker.kt`, `data/repository/DeviceRegistrar.kt` | Tidak login → `true` tanpa request; throttle 24 jam (Clock); `force` melewati throttle; gagal ambil token → `false` → retry s.d. 5 (10 jika force) lalu failure. |
| P1 | `config/RemoteConfigManager.kt` (setelah E1) | Gagal fetch → config lama tetap; `awaitFirstRefresh` selesai walau gagal; `updateMessage` kosong → default. |
| P2 | `ModerationViewModel`, `BlockedUsersViewModel`, `ExploreViewModel`, `UploadBannerViewModel`, `OnboardingViewModel` | Alur report/block/unblock, banner progres upload. |
| P2 | `media/ImageCompressor.kt`, `media/AvatarCropper.kt`, `media/VideoCompressor.kt` | Robolectric untuk Bitmap/EXIF (P2: perangkat nyata lebih representatif, lihat B.3). |

### B.2 Integration test (JVM, `src/test`, dengan Robolectric bila perlu)

> **Room + migration: tidak berlaku.** Project tidak memakai Room/SQLite. Padanan "migrasi" di project ini ada dua dan keduanya diuji: (1) **kompatibilitas key DataStore** (`PrefKeys`) antar versi, dan (2) **kompatibilitas input data WorkManager** untuk upload yang masih antre saat app di-update (sudah dimulai di `PostUploadInputDataTest`).

**Tools:** OkHttp **MockWebServer** (`mockwebserver3`), Retrofit + Gson asli, DataStore asli di `TemporaryFolder`, `androidx.work:work-testing` (`TestListenableWorkerBuilder`, `WorkManagerTestInitHelper`), Robolectric.
**Alasan:** bug paling mahal di app ini ada di **sambungan** antar-komponen (interceptor ↔ refresher ↔ session, worker ↔ repository ↔ storage). Mock per-kelas tidak akan menangkapnya.

| Prioritas | Target | Kasus |
|---|---|---|
| **P0** | **Stack OkHttp nyata**: `AuthInterceptor` + `TokenRefresher` + `SessionManager` + `LanguageInterceptor` vs MockWebServer (butuh E3) | Header `Authorization: Bearer` di endpoint terproteksi, **tidak** di `/auth/*`; `Accept-Language` sesuai `AppLocale`; 401 → `POST auth/refresh` → token & refresh token baru tersimpan → request diulang dengan token baru; retry tetap 401 → session expired; refresh 401 → expired dengan pesan dari body; refresh 5xx/timeout → session **dipertahankan**, 401 diteruskan; **5 request paralel kena 401 → tepat 1 panggilan refresh** (cek `requestCount`); 404 tidak dilaporkan ke `ApiErrorManager`, 500 dilaporkan dengan pesan server; request dibatalkan → tidak ada laporan error. 🔎 Verifikasi R6: panggilan `DeviceRegistrar` yang gagal **tidak boleh** memicu `ApiErrorManager`. |
| **P0** | **Contract/parsing test** semua respons (`data/remote/model/response/**`) | Fixture JSON nyata dari backend di `src/test/resources/fixtures/`. Parse lewat Retrofit+Gson asli. Untuk tiap model: payload lengkap, field opsional hilang (`media`, `userId`, `moderationStatus`), dan **field wajib hilang** (`username`, `content`, `token`). Field wajib yang hilang harus menghasilkan Failure, bukan NPE di UI. Ini menguji R4 dan menjadi dasar keputusan validasi (E8). |
| **P0** | `data/remote/upload/PostUploadWorker.kt` (`TestListenableWorkerBuilder` + WorkerFactory custom; `PostRepository` fake; `PresignedUploader` asli ke MockWebServer) | Caption-only: tidak ada PUT, `createPost(caption, [])`; 3 media: urutan object key sama dengan urutan file; PUT 403 → minta URL baru **sekali** lalu sukses; 403 dua kali → `failure` + non-fatal tercatat; `createPost` 5xx dengan `runAttemptCount` < 3 → `retry`, ≥ 3 → `failure`; 429 → `failure` tanpa retry; IOException → `retry`/`failure`; file hilang atau jumlah tidak cocok → `failure` tanpa network; sukses → file lokal terhapus + `UploadManager.completions` emit + analytics sukses; progres tidak pernah > 99 sebelum finalisasi. |
| **P0** | `data/remote/upload/PresignedUploader.kt` vs MockWebServer | Header `Content-Type` persis sesuai input (bagian dari signature); IOException di-retry 2× lalu dilempar; non-2xx → `UploadHttpException` dengan `Retry-After`; callback progres naik monoton sampai 1.0 (butuh E4 agar delay retry pakai virtual time). |
| P1 | Repository impl (`FeedRepositoryImpl`, `UserRepositoryImpl`, `ModerationRepositoryImpl`, `PostRepositoryImpl`, `AuthRepositoryImpl`) vs MockWebServer | Path, method, query, dan body request sesuai `*Api` (misalnya `GetAllFeedRequest` → query page/size/mediaOnly); `searchUsers` dan `getBlockedUsers` 404 → list kosong; error body → pesan server. Satu test "happy + error" per endpoint kritis, bukan per endpoint. |
| P1 | `DataStoreManager` + `PrefKeys` (DataStore asli di temp file) | Nama key tidak berubah (snapshot nama key: `token`, `refresh_token`, `user_id`, `is_logged_in`, …). Mengganti nama key = semua user ter-logout setelah update. `clearAll` menghapus semua kredensial; `SessionManager.logout` + `expireSession` meninggalkan DataStore kosong. |
| P1 | Kompatibilitas WorkManager | Perluas `PostUploadInputDataTest`: data yang di-enqueue versi sekarang bisa dibaca worker (key `file_paths`, `content_types`, `caption` dibekukan). `-keepnames` worker diverifikasi di B.5 (R8). |
| P1 | `RemoteSessionCleaner` (setelah E6) | Urutan: deactivate device dulu, lalu revoke refresh token; tiap langkah gagal tetap lanjut; timeout 8 detik tidak memblokir logout; `dropPushToken` selalu dipanggil. |
| P2 | Hilt graph smoke (`@HiltAndroidTest`, Robolectric) | Graph bisa dibangun dengan modul test (memastikan `@TestInstallIn` untuk E2E berfungsi). |

### B.3 UI / instrumented test

**Tools:** Compose UI Test (`createComposeRule` / `createAndroidComposeRule`). Test layar stateless dijalankan **di JVM via Robolectric** agar cepat dan bisa di CI tiap PR. Sebagian kecil berjalan di emulator (Gradle Managed Device) untuk hal yang butuh device nyata (media, notifikasi, permission). Espresso hanya untuk intent/notifikasi bila perlu.
**Alasan:** layar sudah dipisah `Route`/`Screen`, jadi `Screen` bisa diberi state langsung tanpa Hilt atau network.

| Prioritas | Layar (file) | Yang diuji |
|---|---|---|
| P1 | `auth/login/LoginScreen.kt` | Validasi input, tombol disabled saat Loading, pesan error tampil, navigasi ke register/reset. |
| P1 | `auth/regiter/RegisterScreen.kt` + `ui/components/OtpInputField.kt` | Form → OTP, input OTP (paste, hapus), countdown resend reset saat `sentCount` berubah. |
| P1 | `auth/reset/*View.kt` | Tiap step tampil sesuai `ResetPasswordStep`, error inline. |
| P1 | `home/feed/CommentSectionSheet.kt`, `CommentInputArea.kt`, `SwipeToDeleteComment.kt` | Kirim → input kosong; `SendFailed` → teks kembali; baris pending tampil beda; reply target chip + batal; swipe hapus hanya bila `canDelete`. |
| P1 | `home/post/PostFeedScreen.kt` | Tombol post disabled tanpa caption/media; batas 5 media; error validasi tampil; hapus media dari strip. |
| P1 | `common/ForceUpdateDialog.kt`, `common/ErrorDialog.kt` | Force update tidak bisa di-dismiss; dedup dialog error (logika sudah dites, di sini tampilannya). |
| P2 | `home/feed/FeedScreen.kt`, `FeedItemCard.kt`, `components/TakenDownPlaceholder.kt` | Empty/error/loading-more; post takedown menampilkan placeholder, bukan media. |
| P2 | `home/settings/SettingsScreen.kt`, `moderation/ContentModerationMenu.kt` | Dialog konfirmasi hapus akun, menu report/block. |
| P2 (device) | Notifikasi & permission | `POST_NOTIFICATIONS` ditolak → app tidak crash, upload tetap jalan (lihat lint `MissingPermission` di `notification/UploadNotifications.kt:162`). |

### B.4 E2E untuk flow kritis

**Tools:** instrumented test di emulator dengan **Hilt `@TestInstallIn`** yang mengganti `@BaseUrl` (E3) ke **MockWebServer di device**, `Dispatcher` MockWebServer berbasis path, dan fake Firebase (App Check/Messaging dimatikan di test). Hasilnya hermetic, deterministik, dan tidak bergantung backend dev.
Pelengkapnya adalah **smoke test manual terstruktur** pada build release yang terhubung ke backend `dev`, karena E2E otomatis tidak bisa membuktikan konfigurasi Firebase/Play Integrity production.

| # | Flow | Prioritas | Otomatis/Manual sebelum rilis |
|---|---|---|---|
| E2E-1 | Install baru → onboarding → login email → feed tampil | P0 | Manual (checklist) sebelum rilis; otomatis P1 |
| E2E-2 | **Session expiry**: feed → 401 → refresh ditolak → kembali ke login dengan pesan; login lagi → feed | P0 | Otomatis (hermetic) **sebelum rilis** |
| E2E-3 | Buat post caption-only dan post 1 gambar → worker → banner progres → sukses → feed ter-refresh | P0 | Manual sebelum rilis; otomatis P1 |
| E2E-4 | Like, komentar, reply, hapus reply di post detail | P1 | Manual sebelum rilis |
| E2E-5 | Tap push (`POST_COMMENTED`) saat cold start dan warm → post detail dengan sheet komentar terbuka | P1 | Manual sebelum rilis (kirim dari Firebase console ke build dev) |
| E2E-6 | Logout dan hapus akun → kembali ke login, token terhapus, push tidak diterima lagi | P0 | Manual sebelum rilis |
| E2E-7 | Force update: Remote Config `min_supported_version_code` > versionCode → dialog blokir | P1 | Manual sebelum rilis (di project Firebase dev) |
| E2E-8 | Register dengan OTP dan reset password end-to-end | P1 | Manual sebelum rilis |

### B.5 Test tambahan yang relevan

| Jenis | Kenapa relevan di project ini | Rencana | Prioritas |
|---|---|---|---|
| **Process death & config change** | R3 (function reference di `SavedStateHandle`); `RegisterViewModel`/`ResetPasswordViewModel` menyimpan form dan token di field biasa | (1) Manual: Developer options → *Don't keep activities*, buka post detail dari feed lalu tekan Home dan kembali; atau `adb shell am kill com.rizkysiregar.narrativ` saat app di background. (2) Otomatis: `ActivityScenario.recreate()` + `StateRestorationTester` (Compose) untuk Login/Register/PostFeed. | **P0** (verifikasi R3) / P1 (otomatis) |
| **Build release / R8** | R5. Hanya build release yang menjalankan R8 | (1) CI: `assembleProdRelease` + `lintVitalProdRelease` di setiap merge ke `master`. (2) Smoke manual di build release flavor `dev`: login, **paksa refresh token** (tunggu JWT expired atau revoke di backend), buka post detail dari feed (Gson nav JSON), upload, lalu matikan jaringan saat upload dan update app (worker rename). (3) P2: jalankan suite E2E terhadap build `release` (`testBuildType = "release"` + `proguard-rules` test). | **P0** (smoke manual) |
| **Screenshot test** | UI Compose banyak, tema dark-only, dua bahasa (EN/ID) | Roborazzi (JVM/Robolectric) untuk `FeedItemCard`, `CommentRow`, `PostTile`, `ErrorDialog`, `ForceUpdateDialog` × locale `en`/`in` × font scale 1.0/1.5. | P2 |
| **Accessibility** | App sosial publik; banyak ikon tanpa label | `composeTestRule.enableAccessibilityChecks()` (Compose 1.8+) di test UI P1; audit manual TalkBack pada login, feed, komentar. Cek `contentDescription` ikon like/comment/more. | P2 (audit manual P1) |
| **Performance** | Feed video (ExoPlayer + cache), cold start dengan Remote Config wait 3 detik di splash | Modul `:benchmark` (Macrobenchmark): cold startup, scroll feed; **Baseline Profile** untuk startup. Firebase Performance sudah terpasang untuk data lapangan. | P2 |
| **Lokalisasi** | `values` + `values-in`; R8 string hardcoded | Unit test: semua key di `values/strings.xml` ada di `values-in` (lint `MissingTranslation` sudah menemukan `app_name`). | P2 |

---

## C. Quality Gate Non-Test

### C.1 Lint
- Status sekarang: `lintDevDebug` **gagal** dengan 11 error:
  - `MissingPermission`: `notification/UploadNotifications.kt:162`
  - `UnsafeOptInUsageError` (Media3 `@UnstableApi`): `home/feed/PostDetailRoute.kt:266`, `home/feed/PostMediaPager.kt:63`
  - `LocalContextGetResourceValueCall` ×7: `moderation/ContentModerationMenu.kt:127,148`, `home/profile/ProfileRoute.kt:80`, `auth/regiter/RegisterRoute.kt:42`, `auth/regiter/RegisterScreen.kt:99`, `auth/reset/ResetPasswordRoute.kt:50`. Ini juga menyebabkan teks tidak ikut berubah saat bahasa diganti.
  - `MissingTranslation`: `app_name` di `values-in`
- Rencana: perbaiki 11 error (**P0**, S–M). Buat `lint-baseline.xml` untuk 103 warning agar hanya warning **baru** yang menggagalkan build. Set `lint { abortOnError = true; checkReleaseBuilds = true; warningsAsErrors = false; baseline = file("lint-baseline.xml") }`.

### C.2 Formatter & static analysis
- **ktlint** (via Spotless atau plugin `org.jlleitschuh.gradle.ktlint`) dengan `.editorconfig` yang **mempertahankan indentasi tab** yang dipakai project. Format sekali di satu commit terpisah agar `git blame` tetap bersih. (P1, S)
- **detekt** dengan baseline: aktifkan aturan `complexity` (fungsi panjang di `CommentViewModel`, `PostUploadWorker`), `coroutines` (`GlobalScope`, `runBlocking`), `potential-bugs`. Gunakan versi detekt yang mendukung Kotlin 2.2. (P1, S)
- Compose: aturan `compose-rules` (twitter/mrmans0n) untuk detekt. (P2)

### C.3 Coverage
- **Kover** (lebih ramah Kotlin/coroutines dibanding JaCoCo mentah) di varian `devDebug`.
- **Exclude**: `*Screen.kt`, `*Route.kt`, `ui/theme/**`, `ui/components/**` (yang tidak punya logika), `di/**`, `*_Factory`, `*_HiltModules*`, `Hilt_*`, `BuildConfig`, `data/remote/model/**` (data class), `ComposableSingletons*`.
- **Gate per paket, bukan angka global** (angka global akan didominasi kode Compose):

| Paket | Target akhir Phase 2 | Target 3 bulan pasca rilis |
|---|---|---|
| `data.remote` (interceptor, refresher, NetworkUtils), `data.session` | ≥ 85% line | ≥ 90% |
| `data.remote.upload`, `domain.post` | ≥ 80% | ≥ 85% |
| `ui.feature.**.*ViewModel` kritis (Auth, Register, Reset, Comment, BaseFeed, Settings, PostFeed) | ≥ 70% | ≥ 80% |
| Seluruh kode ter-filter | dicatat saja (**baseline terukur 17 Nov 2026: 3,2% line, 5,4% branch**) | ≥ 50%, *ratchet* (tidak boleh turun) |

### C.4 CI pipeline (GitHub Actions; repo sudah memakai PR GitHub)

| Trigger | Job | Isi |
|---|---|---|
| Setiap PR | `verify` (wajib hijau untuk merge) | `setup-java` 21 → `./gradlew :app:testDevDebugUnitTest :app:lintDevDebug detekt ktlintCheck koverVerifyDevDebug` (± 5–8 menit). Upload report test & lint sebagai artifact. |
| Push ke `master` | `release-check` | `:app:assembleProdRelease` (R8 nyata, tanpa signing atau dengan keystore dari secret) + `lintVitalProdRelease`. |
| Nightly / sebelum tag | `instrumented` | Gradle Managed Device (`pixel6Api34` ATD image) → `:app:devDebugAndroidTest` (UI P1 + E2E hermetic). |
| Tag `v*` | `bundle` | `bundleProdRelease` signed (env `KEYSTORE_*` sudah didukung `app/build.gradle.kts`), upload mapping ke Crashlytics, artifact AAB. |

Catatan: `google-services.json` per flavor sudah di-commit, jadi CI bisa build tanpa secret tambahan. Wajib pin JDK (lihat Phase 0) dan aktifkan branch protection di `master`.

### C.5 Crash monitoring & observability
- Crashlytics sudah terpasang. Pastikan **upload mapping file R8** untuk build release berjalan (default plugin Crashlytics 3.x: `mappingFileUploadEnabled` true untuk build ber-minify). Verifikasi satu crash uji dari build release internal ter-*deobfuscate* (P0, S).
- Buat alert Crashlytics: *velocity alert* + *regressed issue*; target **crash-free users ≥ 99,5%** sebelum menaikkan rollout.
- Non-fatal sudah dicatat di `PostUploadWorker.reportUploadFailure`. Tambahkan custom key versi backend / flavor bila perlu.
- Analytics event (`telemetry/AnalyticsEvents.kt`) untuk funnel: `LOGIN_FAILED`, `POST_UPLOAD_FAILED`, `SESSION_EXPIRED`. Pantau lonjakan `SESSION_EXPIRED` setelah rilis sebagai indikator regresi R1.
- Catatan: `TelemetrySettings` menonaktifkan pengiriman di build debug. Crash dari QA internal harus memakai build release.

### C.6 Strategi rilis
1. **Internal testing** (Play Console) dengan build release → smoke checklist (§D Phase 2, task 2.12) selesai.
2. **Closed testing** 3–7 hari dengan 10–50 tester.
3. **Production staged rollout**: 5% → 20% → 50% → 100%, minimal 24–48 jam per tahap. Lanjut hanya jika crash-free ≥ 99,5%, tidak ada issue baru dengan > 0,1% user, dan `POST_UPLOAD_FAILED`/`SESSION_EXPIRED` tidak melonjak.
4. **Kill switch** sudah ada: Remote Config `min_supported_version_code` (`config/RemoteConfigManager.kt`) untuk memaksa update. Untuk fitur berisiko berikutnya, pertimbangkan feature flag via Remote Config.
5. `versionCode` sekarang 3 (`versionName 1.0.2`). Pastikan naik di setiap upload; otomatisasi dari tag CI jika memungkinkan.

---

## D. Roadmap Bertahap

### D.0 Alasan pembagian phase
- **Phase 0** ada karena tanpa toolchain stabil, dependency test, dan CI, test apa pun yang ditulis tidak akan dijalankan konsisten.
- **Phase 1** (refactor minimal) dipisah dari penulisan test karena seam (interface telemetry, `DataStore` injectable, `@BaseUrl`, dispatcher, clock) adalah **prasyarat** sebagian besar test P0. Refactor ini mekanis dan berisiko rendah, jadi bisa di-review sebagai satu kelompok PR.
- **Phase 2** berisi semua **blocker rilis**: area dengan dampak bisnis tertinggi (auth, upload, parsing, process death, R8).
- **Phase 3** (UI + E2E otomatis) mahal dan lebih lambat di CI. Sebelum rilis, perannya digantikan smoke manual terstruktur, jadi boleh menyusul.
- **Phase 4** adalah pengerasan jangka panjang (screenshot, performa, a11y) yang nilainya naik setelah ada data lapangan.

### D.1 Ringkasan roadmap

| Phase | Tujuan | Durasi (1 engineer) | Blocker rilis? |
|---|---|---|---|
| 0. Fondasi | Build & test bisa jalan konsisten di semua mesin dan CI | 1–2 hari | **Ya** |
| 1. Seam testability | Refactor minimal agar kelas berisiko bisa dites di JVM | 3–4 hari | **Ya** (hanya bagian yang dibutuhkan test P0) |
| 2. Test P0 + verifikasi rilis | Menutup risiko R1–R6 dengan test otomatis + smoke release | 7–9 hari | **Ya** |
| 3. UI & E2E otomatis | Regresi UI dan flow kritis terdeteksi otomatis | 5–7 hari | Tidak (kecuali E2E-2 yang masuk Phase 2) |
| 4. Pengerasan | Screenshot, a11y, performa, coverage ratchet | berkelanjutan | Tidak |

**Perkiraan total sebelum rilis: ±11–15 hari kerja.**

### D.2 Phase 0: Fondasi

**Tujuan & scope:** environment build deterministik, dependency test tersedia, CI menjalankan unit test + lint.

| ID | Task | Dep | Prio | Usaha |
|---|---|---|---|---|
| 0.1 | Pin JVM Gradle daemon ke 21: `./gradlew updateDaemonJvm --jvm-version=21` (menghasilkan `gradle/gradle-daemon-jvm.properties`), atau upgrade wrapper ke versi Gradle yang mendukung JDK 25. Dokumentasikan di README. | – | P0 | S |
| 0.2 | Tambah dependency test ke `gradle/libs.versions.toml` (§F.1) | – | P0 | S |
| 0.3 | `android { testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } } }` | 0.2 | P0 | S |
| 0.4 | Hapus `ExampleUnitTest`; perbaiki/hapus `ExampleInstrumentedTest` (package salah) | – | P0 | S |
| 0.5 | Buat `src/test/.../testing/` berisi `MainDispatcherRule`, builder fixture (§F.4) | 0.2 | P0 | S |
| 0.6 | Perbaiki 11 lint error + buat `lint-baseline.xml` (§C.1) | – | P0 | M |
| 0.7 | Workflow GitHub Actions `verify` (unit test + lint) + branch protection `master` | 0.1, 0.6 | P0 | S |
| 0.8 | Kover + filter exclude, laporan saja (belum verify) | 0.2 | P1 | S |
| 0.9 | ktlint + detekt dengan baseline, masuk job `verify` | 0.7 | P1 | S |

- [ ] 0.1 → 0.2 → 0.3 → 0.5
- [ ] 0.4, 0.6 (paralel)
- [ ] 0.7 (setelah 0.1 & 0.6)
- [ ] 0.8, 0.9

**Definition of Done:** `./gradlew :app:testDevDebugUnitTest :app:lintDevDebug` hijau dengan JDK default mesin mana pun; CI berjalan di setiap PR dan memblokir merge saat merah; 50 test lama tetap lulus.
**Target coverage:** baseline dicatat (tanpa gate).
**Blocker rilis:** 0.1–0.7. **Boleh menyusul:** 0.8–0.9 (sebaiknya tetap minggu yang sama).

### D.3 Phase 1: Seam testability (refactor minimal)

**Tujuan & scope:** hanya refactor yang membuka test P0. Tanpa perubahan perilaku. Detail dan risiko di §E.

| ID | Task (ref §E) | Dep | Prio | Usaha |
|---|---|---|---|---|
| 1.1 | E1: interface `Analytics`, `CrashReporting`, `StringProvider` (+ implementasi Firebase/Context yang ada), binding Hilt | 0.x | P0 | M |
| 1.2 | E2: `DataStoreManager` menerima `DataStore<Preferences>` (provider di `DataStoreModule`) | 0.x | P0 | S |
| 1.3 | E3: qualifier `@BaseUrl` di-provide `NetworkModule`, dipakai `provideRetrofit` dan `TokenRefresher` | 0.x | P0 | S |
| 1.4 | E4: inject `CoroutineScope`/dispatcher ke `SessionManager`, `PresignedUploader` | 0.x | P0 | S |
| 1.5 | E5: inject `Clock` (atau `() -> Long`) ke `RegisterViewModel`, `DeviceRegistrar` | 0.x | P0 (Register) / P1 | S |
| 1.6 | E1 lanjutan: `PerfTrace` → interface `PerfTracer` yang di-inject ke `PostUploadWorker` | 1.1 | P0 | S |
| 1.7 | E7: ekstrak `MediaValidator` dari `PostFeedViewModel.validateMedia` | 0.x | P1 | M |
| 1.8 | E6: `PushTokenProvider` untuk `DeviceRegistrar` & `RemoteSessionCleaner` | 1.1 | P1 | S |
| 1.9 | E9: hapus `AuthorizationService` dari `AuthViewModel`, hapus `UserDataRepository`/`OAuth2LoginState` bila memang tidak dipakai | – | P1 | S |
| 1.11 | E11: `AppInitializer` untuk E2E Hilt (prasyarat 2.11) | 1.1 | P0 | S |
| 1.10 | E10: ganti function reference di `SavedStateHandle` (R3) dengan mekanisme aman. Kerjakan **setelah** 2.10 membuktikan crash. | 2.10 | P0 jika terbukti | M |

- [ ] 1.1 → 1.6
- [ ] 1.2, 1.3, 1.4, 1.5 (paralel, masing-masing PR kecil)
- [ ] 1.7, 1.8, 1.9
- [ ] 1.10 (bergantung 2.10)

**Definition of Done:** semua kelas di tabel A.4 bertanda P0 bisa diinstansiasi di test JVM tanpa Firebase/Context; app berjalan identik (smoke manual login → feed → upload → logout di build debug); 50 test lama lulus.
**Target coverage:** tidak relevan (refactor).
**Blocker rilis:** 1.1–1.6 (+1.10 jika R3 terbukti). **Boleh menyusul:** 1.7–1.9.

### D.4 Phase 2: Test P0 + verifikasi rilis

**Tujuan & scope:** test otomatis untuk R1–R7 dan verifikasi build release.

| ID | Task | Dep | Prio | Usaha |
|---|---|---|---|---|
| 2.1 | Unit `NetworkUtils` (B.1) | 0.3 | P0 | S |
| 2.2 | Unit `SessionManager` | 1.1, 1.2, 1.4 | P0 | S |
| 2.3 | Integration stack OkHttp: `AuthInterceptor` + `TokenRefresher` + session vs MockWebServer, termasuk 401 paralel dan verifikasi R6 | 1.3, 2.2 | P0 | L |
| 2.4 | Contract test parsing semua respons + fixture JSON dari backend | 0.2 | P0 | M |
| 2.5 | Integration `PresignedUploader` | 1.4 | P0 | S |
| 2.6 | Integration `PostUploadWorker` | 1.1, 1.6, 2.5 | P0 | L |
| 2.7 | Unit `AuthViewModel`, `RegisterViewModel`, `ResetPasswordViewModel` | 1.1, 1.2, 1.5 | P0 | M |
| 2.8 | Unit `BaseFeedViewModel` (via `FeedViewModel`) + `CommentViewModel` | 1.1, 1.2 | P0 | L |
| 2.9 | Unit `SettingsViewModel` + `PostFeedViewModel` (tanpa validasi media) | 1.1 | P0 | M |
| 2.10 | **Verifikasi R3** (process death): manual *Don't keep activities* + test `ActivityScenario.recreate()`/`StateRestorationTester` di layar feed → post detail | – | P0 | S |
| 2.11 | E2E-2 hermetic (session expiry) di emulator | 1.3, 2.3 | P0 | M |
| 2.12 | **Smoke checklist manual build release** (isi checklist konkret **belum ditulis**, termasuk: update dari v1.0.2 harus tetap login, paksa JWT expired harus refresh tanpa logout) (flavor `dev`, R8 aktif) dan `prod` internal track: E2E-1, 3, 4, 5, 6, 7, 8 + skenario R8 di B.5 + verifikasi deobfuscation Crashlytics | 0.7 | P0 | M |
| 2.13 | Aktifkan `koverVerify` per paket (§C.3) di CI | 2.1–2.9 | P0 | S |
| 2.14 | Job CI `release-check` (`assembleProdRelease`) | 0.7 | P0 | S |
| 2.15 | Unit `PostDetailViewModel`, `MainViewModel`, `SplashViewModel`, `SearchViewModel` | 1.1 | P1 | M |
| 2.16 | Integration repository impl + DataStore key snapshot + kompatibilitas WorkManager | 1.2 | P1 | M |
| 2.17 | Unit `UploadManager`, `DeviceRegistrar`, `DeviceSyncWorker`, `RemoteSessionCleaner` | 1.5, 1.8 | P1 | M |

- [ ] 2.1, 2.4, 2.5, 2.10, 2.14 (tanpa dependensi Phase 1 yang berat, bisa mulai paling awal)
- [ ] 2.2 → 2.3 → 2.11
- [ ] 2.5 → 2.6
- [ ] 2.7, 2.8, 2.9
- [ ] 2.12 (dekat tanggal rilis, pada build kandidat final)
- [ ] 2.13 (setelah test P0 masuk)
- [ ] 2.15, 2.16, 2.17

**Definition of Done:**
- Semua kasus P0 di B.1/B.2 punya test yang lulus di CI. Setiap bug yang ditemukan (R3, R6, R4, dst.) punya test regresi dan sudah diperbaiki, atau dicatat sebagai *known issue* yang disetujui.
- Smoke checklist 2.12 lulus 100% di build release kandidat.
- Tidak ada test *flaky* (setiap suite baru dijalankan 3× berturut-turut di CI tanpa gagal).

**Target coverage:** sesuai kolom "Target akhir Phase 2" di §C.3.
**Blocker rilis:** 2.1–2.14. **Boleh menyusul:** 2.15–2.17 (target ≤ 2 minggu pasca rilis).

### D.5 Phase 3: UI & E2E otomatis (pasca rilis)

| ID | Task | Dep | Prio | Usaha |
|---|---|---|---|---|
| 3.1 | Setup Compose UI test di Robolectric + Gradle Managed Device di CI nightly | 0.7 | P1 | M |
| 3.2 | UI test P1 (B.3): Login, Register/OTP, Reset, CommentSheet, PostFeed, ForceUpdate/Error dialog | 3.1 | P1 | L |
| 3.3 | Hilt `@TestInstallIn` + MockWebServer dispatcher di device (lanjutan 2.11) | 2.11 | P1 | S |
| 3.4 | E2E otomatis E2E-1, E2E-3, E2E-6 | 3.3 | P1 | L |
| 3.5 | `StateRestorationTester` untuk Register/Reset/PostFeed (form tidak hilang) | 3.1 | P1 | S |
| 3.6 | Unit `MediaValidator` (batas format, 5 MB GIF, 50 MB/60 detik video) | 1.7 | P1 | S |
| 3.7 | UI test P2 + ViewModel P2 (moderation, explore, profile) | 3.2 | P2 | M |

- [ ] 3.1 → 3.2 → 3.5
- [ ] 3.3 → 3.4
- [ ] 3.6, 3.7

**DoD:** nightly instrumented hijau 5 malam berturut-turut; checklist manual 2.12 berkurang minimal 50% karena sudah otomatis.
**Target coverage:** ViewModel kritis ≥ 80%, paket UI tidak di-gate.
**Blocker rilis:** tidak ada.

### D.6 Phase 4: Pengerasan berkelanjutan

| ID | Task | Prio | Usaha |
|---|---|---|---|
| 4.1 | Roborazzi screenshot test untuk komponen utama × EN/ID × font scale | P2 | M |
| 4.2 | Accessibility checks di UI test + audit TalkBack | P2 | M |
| 4.3 | Modul `:benchmark` (startup, scroll feed) + Baseline Profile | P2 | L |
| 4.4 | Suite E2E dijalankan terhadap build `release` (R8) | P2 | M |
| 4.5 | Coverage ratchet (gate global ≥ 50%, tidak boleh turun) | P2 | S |
| 4.6 | Test kelengkapan terjemahan `values` vs `values-in` | P2 | S |
| 4.7 | Aturan PR: bug fix wajib disertai test regresi (template PR) | P1 | S |

**DoD:** setiap item punya job CI sendiri atau masuk nightly; hasil benchmark dicatat per rilis.

### D.7 Ringkasan blocker production

**Wajib sebelum rilis:**
- [ ] JDK pinned, dependency test, CI `verify` + `release-check` hijau dan memblokir merge (0.1–0.7, 2.14)
- [ ] 11 lint error diperbaiki (0.6)
- [ ] Seam P0 (1.1–1.6)
- [ ] Test auth/session/refresh token termasuk 401 paralel (2.1–2.3)
- [ ] Contract test parsing (2.4)
- [ ] Test pipeline upload (2.5–2.6)
- [ ] Test ViewModel auth, feed, komentar, settings, post composer (2.7–2.9)
- [ ] R3 (process death) diverifikasi dan diperbaiki bila terbukti (2.10, 1.10)
- [ ] E2E session expiry otomatis (2.11)
- [ ] Smoke checklist build release + deobfuscation Crashlytics (2.12)
- [ ] Gate coverage per paket aktif (2.13)

**Boleh menyusul:** 0.8–0.9, 1.7–1.9, 2.15–2.17, seluruh Phase 3–4.

---

## E. Refactor yang Diperlukan agar Testable

| ID | Refactor | File | Bentuk | Risiko & mitigasi |
|---|---|---|---|---|
| E1 | Interface untuk telemetry & string | `telemetry/AppAnalytics.kt`, `CrashReporter.kt`, `PerfTrace.kt`, `i18n/AppStrings.kt`, `config/RemoteConfigManager.kt` | `interface Analytics { fun log(...); fun setUserId(...); ... }`. `AppAnalytics : Analytics`, `@Binds` di modul baru `TelemetryModule`. Konsumen (ViewModel, worker, `SessionManager`) bergantung ke interface. Untuk `RemoteConfigManager`: ekspos `interface AppConfigSource { val config: StateFlow<AppConfig>; suspend fun refresh(); suspend fun awaitFirstRefresh() }`. | **Rendah.** Perubahan mekanis tipe constructor. Risiko: lupa binding → error compile Hilt (terdeteksi saat build, bukan runtime). |
| E2 | `DataStoreManager(dataStore: DataStore<Preferences>)` | `data/cache/DataStoreManager.kt`, `di/DataStoreModule.kt` | Pindahkan `preferencesDataStore("app_pref")` ke `DataStoreModule` sebagai `@Provides @Singleton DataStore<Preferences>`. **Nama file `app_pref` wajib sama.** | **Sedang.** Salah nama file atau dua instance DataStore untuk file yang sama → crash `IllegalStateException: multiple DataStores active` atau user ter-logout. Mitigasi: satu provider singleton + test snapshot nama (2.16) + smoke update dari versi lama (install v1.0.2 → update → masih login). |
| E3 | `@BaseUrl` qualifier | `di/NetworkModule.kt`, `data/remote/TokenRefresher.kt` | `@Provides @BaseUrl fun baseUrl() = BuildConfig.BASE_URL`. `TokenRefresher` menerima `@BaseUrl baseUrl: String`. | **Rendah.** Pastikan trailing slash tetap (`.../api/v1/` + `auth/refresh`). |
| E4 | Inject scope/dispatcher | `data/session/SessionManager.kt`, `data/remote/upload/PresignedUploader.kt`, (P1) `MediaPreparationUseCase`, `ProfilePhotoUseCase` | `@ApplicationScope CoroutineScope` dan `@IoDispatcher CoroutineDispatcher` dari modul `CoroutinesModule`. | **Rendah.** `SessionManager` harus tetap memakai `SupervisorJob` agar satu kegagalan tidak membatalkan clear berikutnya. |
| E5 | Clock | `RegisterViewModel`, `DeviceRegistrar` | Inject `java.time.Clock` (`Clock.systemUTC()` di produksi). | **Rendah.** |
| E6 | `PushTokenProvider` | `DeviceRegistrar`, `RemoteSessionCleaner` | `interface PushTokenProvider { suspend fun token(): String; suspend fun delete() }`, implementasi `FirebaseMessaging`. Bisa sekalian memindahkan info device (`Build.*`, `ANDROID_ID`, versi app) ke `DeviceInfoProvider`. | **Rendah.** |
| E7 | `MediaValidator` | `ui/feature/home/post/PostFeedViewModel.kt` | Kelas yang menerima abstraksi `MediaInspector { mimeType(uri); sizeBytes(uri); durationMs(uri) }`; aturan (format, 5 MB GIF, 50 MB, 60 detik) jadi pure dan bisa dites. Satukan konstanta dengan `MediaPreparationUseCase.ALLOWED_*` (saat ini terduplikasi). | **Sedang.** Menyentuh validasi upload. Mitigasi: test 3.6 ditulis lebih dulu terhadap perilaku sekarang (characterization test). |
| E8 | Validasi respons wajib (opsional, keputusan setelah 2.4) | `util/NetworkUtils.kt` / model response | Opsi A: ganti Gson → Moshi codegen (`converter-moshi` sudah ada di dependency tetapi belum dipakai) agar field wajib yang hilang gagal saat parsing. Opsi B: tetap Gson + validasi eksplisit pada model kritis. | **Tinggi untuk A** (mengubah semua model, keep rule R8, dan JSON nav saved state). **Rekomendasi: B sebelum rilis, A pasca rilis bila diinginkan.** |
| E9 | Hapus kode mati | `AuthViewModel` (param `authService`), `di/AuthModule.kt`, `data/repository/UserDataRepository.kt`, `data/model/OAuth2LoginState.kt` | Hapus. Pertimbangkan juga dependency `appauth`/`play-services-auth` bila tidak dipakai di tempat lain (cek manifest: `RedirectUriReceiverActivity` AppAuth masih terdaftar). | **Rendah–sedang.** Pastikan Google Sign-In (Credential Manager) tidak bergantung pada AppAuth. Smoke login Google. |
| E11 | Pisahkan inisialisasi `NarrativApplication.onCreate()` | `NarrativApplication.kt` | Ekstrak ke `AppInitializer` (`ApiMessages.init`, channel notifikasi, telemetry). E2E Hilt memakai `HiltTestApplication`, jadi `onCreate` aslinya tidak jalan; worker `@HiltWorker` butuh `WorkManagerTestInitHelper` + `HiltWorkerFactory`. Matikan device sync dan Remote Config saat test. | **Rendah.** Pastikan urutan init tidak berubah. |
| E10 | Hand-off post ke detail tanpa function reference di `SavedStateHandle` | `FeedRoute.kt`, `ExploreRoute.kt`, `HomeRoot.kt:74`, `PostDetailRoute.kt:100-107` | Simpan hanya data serializable (JSON/`postId`). Untuk like, gunakan ViewModel bersama yang di-scope ke nav graph induk (`hiltViewModel(parentEntry)`) atau event hasil (`savedStateHandle["liked_change"] = postId`) yang dibaca layar sebelumnya. | **Sedang.** Menyentuh sinkronisasi like feed ↔ detail. Mitigasi: test 2.10 + test unit sinkronisasi like. |

Yang **sengaja tidak** direkomendasikan sebelum rilis: memecah modul, mengganti `ResultState`, menulis ulang `CommentViewModel`. Semuanya berisiko tinggi tanpa jaring test.

---

## F. Rekomendasi Setup Awal

### F.1 Dependency test (`gradle/libs.versions.toml`)

Versi di bawah adalah titik awal yang konsisten dengan stack sekarang. Verifikasi versi terbaru yang kompatibel saat implementasi.

```toml
[versions]
coroutinesTest = "1.9.0"      # samakan dengan kotlinx-coroutines-android 1.9.0
turbine = "1.2.0"
truth = "1.4.4"
robolectric = "4.14.1"        # bila belum mendukung API 36, pakai @Config(sdk = [35])
androidxTestCore = "1.6.1"
okhttpTest = "5.3.2"          # samakan dengan okhttp
kover = "0.9.1"

[libraries]
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutinesTest" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-core = { module = "androidx.test:core-ktx", version.ref = "androidxTestCore" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver3", version.ref = "okhttpTest" }
okhttp-mockwebserver-junit4 = { module = "com.squareup.okhttp3:mockwebserver3-junit4", version.ref = "okhttpTest" }
androidx-work-testing = { module = "androidx.work:work-testing", version.ref = "workManager" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version = "2.57.2" }

[plugins]
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```

```kotlin
// app/build.gradle.kts
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
testImplementation(libs.truth)
testImplementation(libs.robolectric)
testImplementation(libs.androidx.test.core)
testImplementation(libs.okhttp.mockwebserver)
testImplementation(libs.okhttp.mockwebserver.junit4)
testImplementation(libs.androidx.work.testing)
testImplementation(platform(libs.androidx.compose.bom))
testImplementation(libs.androidx.ui.test.junit4)      // Compose UI test di Robolectric

androidTestImplementation(libs.hilt.android.testing)
kspAndroidTest(libs.hilt.compiler)
androidTestImplementation(libs.okhttp.mockwebserver)
androidTestImplementation(libs.kotlinx.coroutines.test)
```

**MockK tidak ditambahkan di awal.** Tambahkan hanya jika muncul kebutuhan yang tidak bisa dipenuhi fake (misalnya kelas final pihak ketiga).

### F.2 Struktur folder test

```
app/src/
├── test/java/com/rizkysiregar/narrativ/
│   ├── testing/                      # helper bersama (tidak berisi test)
│   │   ├── MainDispatcherRule.kt
│   │   ├── TestDataStore.kt          # DataStore asli di TemporaryFolder
│   │   ├── MockWebServerExt.kt       # enqueueJson("fixtures/feed_page.json"), enqueueError(code, message)
│   │   ├── fixtures/Fixtures.kt      # aFeedItem(), aComment(), aLoginResponse(), aPage(...)
│   │   └── fakes/                    # Fake* (lihat F.4)
│   ├── data/remote/                  # NetworkUtilsTest, AuthInterceptorIntegrationTest, ContractTest
│   ├── data/remote/upload/           # PostUploadWorkerTest, PresignedUploaderTest (+ yang sudah ada)
│   ├── data/session/                 # SessionManagerTest, RemoteSessionCleanerTest
│   ├── data/repository/              # FeedRepositoryImplTest, DeviceRegistrarTest, ...
│   ├── domain/post/                  # UploadManagerTest, MediaValidatorTest
│   └── ui/feature/**/                # *ViewModelTest, mengikuti package produksi
├── test/resources/fixtures/          # JSON respons backend asli (dianonimkan)
└── androidTest/java/com/rizkysiregar/narrativ/
    ├── testing/                      # HiltTestRunner, TestNetworkModule (@TestInstallIn), MockWebServerDispatcher
    ├── e2e/                          # SessionExpiryE2ETest, CreatePostE2ETest, ...
    └── ui/                           # test yang butuh device nyata (notifikasi, media)
```

Tambahkan `testInstrumentationRunner = "com.rizkysiregar.narrativ.testing.HiltTestRunner"` saat E2E dimulai.

### F.3 Konvensi penamaan

- Kelas: `<KelasProduksi>Test` untuk unit, `<KelasProduksi>IntegrationTest` untuk yang memakai MockWebServer/DataStore/WorkManager asli, `<Flow>E2ETest` untuk E2E. Package sama dengan kelas produksi.
- Method: **ikuti gaya test yang sudah ada**, yaitu kalimat perilaku camelCase tanpa prefix `test`, contoh `oneDismissClearsEveryDialogWithTheSameMessage`, `captionOnlyPostKeepsItsEmptyFileListAfterReload`. Untuk kasus baru: `parallel401sTriggerASingleRefresh`, `failedSendPutsTheTextBack`.
- Struktur isi: *given / when / then* dipisah baris kosong; satu perilaku per test; data uji bahasa Indonesia/Inggris boleh (sudah campur di test lama).
- Indentasi: **tab**, sama dengan kode produksi.
- Fixture JSON: `snake_case` sesuai endpoint, mis. `fixtures/feed_page_with_video.json`, `fixtures/login_success.json`, `fixtures/error_401_session.json`.

### F.4 Helper & fake yang perlu dibuat

| Helper / Fake | Menggantikan | Catatan |
|---|---|---|
| `MainDispatcherRule` | `Dispatchers.Main` di `viewModelScope` | `StandardTestDispatcher` + `Dispatchers.setMain/resetMain`. |
| `FakeAuthRepository`, `FakeFeedRepository`, `FakePostRepository`, `FakeUserRepository`, `FakeModerationRepository` | `data/repository/*Repository` (sudah interface) | Hasil bisa di-script per panggilan (`MutableSharedFlow` atau antrean `ResultState`), mencatat argumen yang diterima. Pakai **use case asli** di atasnya. |
| `FakeAnalytics`, `FakeCrashReporting`, `FakePerfTracer` | E1 | Merekam event untuk assertion (misalnya `POST_UPLOAD_FAILED` dengan `stage`). |
| `FakeStringProvider` | `AppStrings` | Mengembalikan `"res:<id>:<args>"` agar assertion stabil tanpa resource. |
| `FakeAppConfigSource` | `RemoteConfigManager` | `MutableStateFlow<AppConfig>`. |
| `TestDataStore` | DataStore | DataStore **asli** (`PreferenceDataStoreFactory.create { tmpFile }`), bukan fake: perilakunya sederhana dan nyata. |
| `TestClock` | E5 | `Clock` yang bisa dimajukan (`advanceBy(Duration)`). |
| `FakePushTokenProvider` | E6 | Token tetap / bisa melempar exception. |
| `FakeDeviceSyncScheduler` | `DeviceSyncScheduler` | Merekam `schedule(force)`. Butuh interface kecil, atau gunakan `WorkManagerTestInitHelper` dan cek work yang di-enqueue. |
| `FakeRemoteSessionCleaner` | `RemoteSessionCleaner` | Merekam urutan panggilan untuk `SettingsViewModel` (butuh interface; masuk E6). |
| `Fixtures.kt` | – | Builder dengan default masuk akal: `aFeedItem(id = uuid(), liked = false, likeCount = 0, media = emptyList())`, `aComment(parentId = null)`, `aPage(content, last)`. |
| `MockWebServerExt.kt` | – | `enqueueJson(path, code = 200)`, `enqueueError(code, message)`, `takeRequestPath()`. |
| `TestPostUploadWorkerFactory` | `HiltWorkerFactory` | `WorkerFactory` manual untuk `TestListenableWorkerBuilder<PostUploadWorker>`. |

---

## Asumsi & Pertanyaan Terbuka

**Asumsi**
1. Satu engineer full-time. Estimasi tidak termasuk waktu memperbaiki bug yang ditemukan test.
2. Rilis production memakai flavor `prod` + build type `release` lewat Google Play.
3. Backend `dev` (`api-dev-narrativ...`) mencerminkan kontrak `prod` dan boleh dipakai untuk smoke test manual.
4. R6 adalah dugaan dari membaca kode yang sudah dibuktikan di level interceptor, belum direproduksi di device. R3 sudah direproduksi dan diperbaiki.

**Pertanyaan terbuka**
1. Apakah ada spesifikasi API (OpenAPI/Swagger) atau contoh respons resmi dari backend? Ini menentukan sumber fixture contract test (2.4) dan apakah bisa dibuat contract test dua arah.
2. Kapan target tanggal rilis production? Ini menentukan apakah 2.15–2.17 masuk sebelum rilis.
3. Apakah ada QA manual / tester internal untuk smoke checklist 2.12 dan closed testing?
4. Berapa umur JWT dan refresh token di backend? Ini menentukan skenario uji refresh di build release (perlu cara memaksa token expired di `dev`).
5. Apakah dependency AppAuth (`net.openid:appauth`) dan `RedirectUriReceiverActivity` di manifest masih dibutuhkan, atau sisa implementasi OAuth lama (E9)?
6. Apakah penyimpanan JWT/refresh token plaintext di DataStore dapat diterima secara kebijakan keamanan, atau perlu enkripsi sebelum rilis (di luar scope test, tetapi memengaruhi E2)?
7. Apakah repo GitHub ini akan memakai GitHub Actions (menit gratis/berbayar) untuk instrumented test nightly, atau lebih memilih Firebase Test Lab?
8. Apakah telemetry yang aktif secara default (opt-out di `TelemetrySettings`) sudah sesuai kebijakan privasi dan Data safety form Play Store?
