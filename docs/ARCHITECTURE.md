# BakeMate — Arsitektur & Struktur Proyek

> Kontrak struktur sebelum implementasi. Front end (UI) → backend (data).
> Semua keputusan arsitektur tercatat di sini; implementasi wajib mengikuti.

## 1. Prinsip Arsitektur

| Prinsip | Keputusan |
|---|---|
| Offline-first | Semua data lokal (Room + DataStore). Tanpa server, tanpa akun, tanpa cost. |
| Layer ketat | UI → ViewModel → UseCase → Repository → DataSource. Arah dependensi satu arah. |
| Domain murni | BakerMath & timer engine TANPA dependensi Android → unit-testable. |
| State holder | Setiap layar punya ViewModel + UiState (immutable data class) + event satu arah. |
| Persistence baking | Sesion baking tersimpan di Room → survive app di-kill / reboot (via BootReceiver). |

## 2. Struktur Package (front end → backend)

```
com.bakemate/
├── BakeMateApp.kt                 # @HiltAndroidApp
├── MainActivity.kt                # Single-activity, Compose, permission request
│
├── di/                            # Dependency Injection
│   ├── DatabaseModule.kt          # Room DB, DAOs
│   ├── RepositoryModule.kt        # Repository bindings
│   └── SchedulerModule.kt         # TimerScheduler, NotificationHelper
│
├── ui/                            # FRONTEND — Compose
│   ├── theme/
│   │   ├── Color.kt               # Design tokens warna (light + dark)
│   │   ├── Type.kt                # Tipografi (font custom + skala)
│   │   ├── Shape.kt               # Shape tokens (konsisten)
│   │   └── Theme.kt               # BakeMateTheme (light/dark/dynamic)
│   ├── navigation/
│   │   └── AppNavigation.kt       # 5 tab: Beranda, Resep, Timer, Starter, Pengaturan
│   ├── components/                # Komponen bersama
│   │   ├── AppButton.kt           # Button/GradientButton konsisten
│   │   ├── AppCard.kt             # Card dengan elevation tokens
│   │   ├── StateViews.kt          # EmptyState, LoadingState, ErrorState
│   │   ├── FormFields.kt          # LabeledTextField, NumberField, StepEditor
│   │   ├── SectionHeader.kt       # Header section konsisten
│   │   └── Feedback.kt            # Snackbar helper
│   ├── home/                      # Beranda: greeting, quick actions, session aktif
│   ├── recipe/
│   │   ├── RecipeListScreen.kt    # Daftar + search + filter favorit
│   │   ├── RecipeDetailScreen.kt  # Formula + langkah + Bake Mode (screen awake)
│   │   ├── RecipeFormScreen.kt    # Form lengkap (ingredient dinamis + step)
│   │   └── RecipeViewModel.kt
│   ├── calculator/                # Kalkulator baker (hydration, scaling)
│   ├── timer/                     # Timer multi-tahap + Bake Mode
│   ├── starter/                   # Jurnal starter (feeding, rasio, aktivitas)
│   └── settings/                  # Notifikasi, dark mode, backup, about
│
├── domain/                        # BACKEND LOGIKA (pure Kotlin, no Android)
│   ├── BakerMath.kt               # Hydration, scaling, baker's %
│   ├── timer/
│   │   └── BakeTimer.kt           # Engine timer berbasis elapsedRealtime (pure)
│   ├── model/
│   │   ├── RecipeFormula.kt       # Domain model formula (bebas Android)
│   │   └── BakeSessionModel.kt    # Domain model sesion baking
│   └── usecase/
│       ├── CalculateHydration.kt  # Use case hydration
│       ├── ScaleRecipe.kt         # Use case scaling
│       └── ValidateRecipe.kt      # Use case validasi input resep
│
├── data/                          # BACKEND DATA (Room, DataStore)
│   ├── local/
│   │   ├── entity/
│   │   │   ├── Recipe.kt          # Resep (header)
│   │   │   ├── RecipeIngredient.kt# Bahan (dinamis, banyak per resep)
│   │   │   ├── RecipeStep.kt      # Langkah (dinamis, banyak per resep)
│   │   │   ├── StarterLog.kt      # Catatan feeding
│   │   │   └── BakeSession.kt     # Sesion baking berjalan (persist!)
│   │   ├── dao/
│   │   │   ├── RecipeDao.kt       # CRUD + relation query
│   │   │   ├── StarterLogDao.kt
│   │   │   └── BakeSessionDao.kt
│   │   └── BakeMateDatabase.kt    # Room DB (version 2, migration)
│   ├── preferences/
│   │   └── AppPreferences.kt      # DataStore: dark mode, notifikasi, default rasio
│   ├── repository/
│   │   ├── RecipeRepository.kt    # Gabung entity → domain model
│   │   ├── StarterLogRepository.kt
│   │   └── BakeSessionRepository.kt
│   └── backup/
│       ├── BackupManager.kt       # Export/import JSON (FileProvider share)
│       └── BackupFormatter.kt     # Serialisasi/deserialisasi JSON
│
└── timer/                         # ANDROID PLUMBING (AlarmManager, notif)
    ├── TimerScheduler.kt          # Jadwalkan/batalkan alarm per tahap
    ├── TimerAlarmReceiver.kt      # Terima alarm → notif + update DB
    ├── TimerBootReceiver.kt       # Restore sesion aktif setelah reboot
    └── NotificationHelper.kt      # Channel + notifikasi
```

## 3. Alur Data (end-to-end)

```
[UI Compose] → ViewModel (UiState) → UseCase → Repository → Room/DataStore
      ↑                                                          |
      └────────── Flow<List<T>> (observe) ←─────────────────────┘
```

- **Tulis**: UI → VM → UseCase → Repository → DAO → Room
- **Baca**: Room → DAO → Repository (map ke domain) → Flow → VM → UI (collectAsStateWithLifecycle)
- **Timer**: VM menghitung state via `BakeTimer` (elapsedRealtime) → `TimerScheduler` (AlarmManager) → Receiver → notif + tulis `BakeSession` ke Room → VM observe DB → restore saat app dibuka.

## 4. Skema Database (v2)

```
recipes(id PK, name, description, flourGrams, waterGrams, starterGrams,
        saltGrams, hydration, isFavorite, createdAt, updatedAt)

recipe_ingredients(id PK, recipeId FK→recipes, name, grams, isFlour,
                   sortOrder)          -- bahan dinamis

recipe_steps(id PK, recipeId FK→recipes, text, minutes, sortOrder)
                                       -- langkah + durasi (timer-ready)

starter_logs(id PK, starterName, feedingTime, ratio, note, activityLevel,
             photoPath, createdAt)

bake_sessions(id PK, recipeId FK?, stagesJson, currentStageIndex,
              stageEndTimesJson, startedAt, status[ACTIVE|PAUSED|DONE],
              bakeMode)                -- persist sesion aktif
```

Migration v1→v2: buat `recipe_ingredients` + `recipe_steps` + `bake_sessions` dari data existing (ingredient 4-tetap → baris dinamis).

## 5. Keputusan UI/UX (ringkas — detail di UI-UX-SPEC.md)

- **Navigasi 5 tab**: Beranda, Resep, Timer, Starter, Pengaturan (Resep naik jadi tab — fitur inti)
- **Bake Mode** di detail resep & timer: layar tetap menyala
- **Timer persist**: sesion tersimpan; banner "Lanjutkan Baking" di Beranda
- **Empty states** di semua layar dengan CTA jelas
- **Typography custom** + shape tokens + monochrome icon (Android 13+)
- **Feedback**: Snackbar untuk aksi sukses; dialog konfirmasi untuk hapus

## 6. Non-Goals v1 (defer)

- Cloud sync / multi-device (butuh server & akun — tawarkan sebagai fase berikutnya)
- Widget home screen
- Foto pengambilan langsung (hanya path dari galeri via photo picker)
- PWA/web version

## 7. Build & Delivery

- Build APK **hanya di GitHub Actions** (HP tidak panas): `.github/workflows/build-apk.yml` (sudah ada)
- Lokal: hanya edit + git push. Sanity check compile ringan bila perlu.
- Versi: `versionCode` bump per rilis, `versionName` semver.
