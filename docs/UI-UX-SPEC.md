# BakeMate — Spesifikasi UI/UX

> Kontrak desain. Setiap layar harus memenuhi spesifikasi di bawah sebelum dianggap selesai.

## 1. Design Tokens

### Warna (netral, eye-friendly — preferensi user)
| Token | Light | Dark |
|---|---|---|
| `primary` | #8A5A3B (cokelat hangat muted) | #D9A36A |
| `onPrimary` | #FFFFFF | #3A2410 |
| `primaryContainer` | #F0E3D4 | #5D3A20 |
| `onPrimaryContainer` | #3E2410 | #F8EEDD |
| `secondary` | #6E6254 (abu-cokelat) | #C0B4A0 |
| `background` | #FAF8F5 (krem netral) | #16130F |
| `surface` | #FFFFFF | #1E1A15 |
| `surfaceVariant` | #F1ECE4 | #37312A |
| `error` | #BA1A1A | #FFB4AB |
| `outline` | #857A6E | #9C9184 |

### Tipografi
- **Font**: Plus Jakarta Sans (regular, medium, semiBold, bold) via `res/font/`
- **Skala**: displayLarge 32sp, headlineMedium 24sp, titleMedium 16sp, bodyMedium 14sp, labelMedium 12sp
- **Angka besar** (timer): tabular figures (monospace untuk countdown)

### Shape
- Card: `RoundedCornerShape(16.dp)`
- Button: `RoundedCornerShape(12.dp)`
- TextField: `RoundedCornerShape(12.dp)`

### Spacing
- Grid: 4.dp base; padding layar 16.dp; antar-card 12.dp; antar-section 24.dp

## 2. Navigasi (5 tab)

| Tab | Ikon | Label | Screen |
|---|---|---|---|
| 1 | home | Beranda | HomeScreen (greeting, session aktif, quick actions, statistik) |
| 2 | recipe | Resep | RecipeListScreen (search, filter favorit, FAB tambah) |
| 3 | timer | Timer | TimerScreen (multi-tahap, edit durasi, mulai) |
| 4 | starter | Starter | StarterScreen (jurnal feeding) |
| 5 | settings | Pengaturan | SettingsScreen (notifikasi, dark mode, backup, about) |

## 3. Spesifikasi per Screen

### 3.1 Beranda (HomeScreen)
- Greeting "Halo, Baker!" + tanggal (Bahasa Indonesia)
- **Kartu "Baking Berjalan"** (jika ada sesion aktif): nama resep, tahap saat ini, countdown, tombol "Lanjutkan" → navigasi ke Timer. **Ini yang membuat app terasa hidup & user-friendly.**
- Quick actions: 4 kartu (Kalkulator, Resep Baru, Catat Feeding, Mulai Timer)
- Statistik: jumlah resep, total baking selesai (dari bake_sessions), feeding terakhir
- Empty state jika belum pernah baking: "Mulai baking pertama Anda" + CTA

### 3.2 Resep (RecipeListScreen)
- Header judul + search bar (filter by nama)
- Filter chip: "Semua" / "Favorit"
- Card resep: nama, hydration %, total berat, jumlah langkah, badge favorit
- FAB "+" → form resep
- Empty state: ilustrasi + "Belum ada resep" + tombol "Tambah Resep Pertama"
- **RecipeDetailScreen** (klik card):
  - Header: nama, deskripsi, stats (hydration, total, jumlah langkah)
  - Section "Formula": tabel bahan (gram) + bar hydration visual
  - Section "Langkah": list step dengan durasi; tiap step punya tombol "Timer" → start timer dari step ini
  - Tombol "Mulai Baking" → ke Timer dengan stage dari resep ini
  - Menu: Edit, Hapus (dialog konfirmasi), Favorit

### 3.3 Kalkulator (CalculatorScreen)
- 4 field input: Tepung, Air, Starter, Garam (gram) — pakai keyboard numerik
- Field opsional: target berat dough
- Hasil real-time: hydration % (besar, primary), total dough
- Tabel scaling (jika target diisi): Tepung/Air/Starter/Garam dalam gram
- Tips kecil: "Hydration dihitung termasuk starter (50/50)"

### 3.4 Timer (TimerScreen)
- **Header**: nama resep (jika dari resep) atau "Timer Bebas"
- **Card utama**: tahap saat ini, countdown besar (tabular figures), progress bar antar tahap
- Kontrol: Mulai/Jeda/Lanjut, Lewati, Hentikan (konfirmasi)
- Bake Mode switch: "Layar tetap menyala" + ikon
- List tahap: tiap baris = nama + durasi + status (menunggu/berjalan/selesai), edit durasi saat idle
- **Persist**: sesion tersimpan; banner "Baking sedang berjalan" muncul di Beranda

### 3.5 Starter (StarterScreen)
- Kartu teratas: "Terakhir Feeding" (nama, waktu relatif "2 jam lalu", rasio, aktivitas)
- Tombol "+ Catat Feeding" → form (nama, rasio, catatan, slider aktivitas 1-5)
- Riwayat list dengan hapus (konfirmasi)

### 3.6 Pengaturan (SettingsScreen)
- Notifikasi timer (switch) → cek izin POST_NOTIFICATIONS (Android 13+)
- Mode gelap (switch) — sinkron dengan tema
- Backup: "Ekspor Data (JSON)" → FileProvider share; "Impor Data"
- Tentang: nama app, versi, ikon app — tanpa catatan developer

## 4. Komponen UI (components/)

| Komponen | Fungsi |
|---|---|
| `EmptyState` | Ikon + judul + deskripsi + CTA button — dipakai semua layar kosong |
| `LoadingState` | CircularProgressIndicator terpusat |
| `ErrorState` | Pesan error + tombol coba lagi |
| `SectionHeader` | Judul section konsisten (label + optional action) |
| `AppCard` | Card standar (surface, elevation 1) |
| `NumberField` | TextField numerik dengan label |
| `StepEditor` | Editor langkah dinamis (tambah/hapus baris) |
| `IngredientEditor` | Editor bahan dinamis (nama + gram + isFlour toggle) |
| `AppSnackbar` | Helper snackbar sukses/error |

## 5. Feedback & Konfirmasi

- **Aksi destruktif** (hapus resep, hapus log, hentikan baking): `AlertDialog` konfirmasi
- **Aksi sukses** (simpan resep, catat feeding): Snackbar singkat
- **Error input**: pesan inline di bawah field (bukan toast)

## 6. Aksesibilitas

- Konten description di semua ikon interaktif
- Kontras warna ≥ 4.5:1 untuk teks (tokens sudah dipilih memenuhi)
- Touch target ≥ 48.dp
- Text scale: layout harus responsif (fontScale 1.3x tidak patah)

## 7. Verifikasi Visual (definition of done)

- [ ] Semua layar punya empty state dengan CTA
- [ ] Konsistensi token: tidak ada warna/hex hardcode di screen (semua via theme)
- [ ] Typography custom terpasang dan dipakai
- [ ] Bake Mode benar-benar membuat layar tetap menyala
- [ ] Timer persist: kill app → buka lagi → sesion masih ada
- [ ] Snackbar untuk aksi sukses, dialog untuk destruktif
- [ ] Dark mode: semua layar terlihat benar (kontras)
- [ ] Launcher icon: adaptive + monochrome (Android 13 themed icon)
