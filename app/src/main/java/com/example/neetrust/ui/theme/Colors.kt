// [KATALOG] shared.ui.theme.Colors
package com.example.neetrust.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PALET WARNA MORI (森 - Deep Forest & Twilight Lo-Fi)
 * Terinspirasi dari tema Mori (github.com/coflyn/Mori):
 * - Nuansa hutan malam yang tenang dan pekat (Deep Night & Charcoal Teal)
 * - Aksen lumut/emerald segar (Emerald & Forest Green)
 * - Teks earthy sage & cream yang sangat nyaman untuk mata saat menonton lama
 * - Token warna didefinisikan dengan nama yang jelas agar mudah diganti.
 */

// Backgrounds
val MoriBgDeep = Color(0xFF0F1416)         // Latar belakang utama aplikasi
val MoriBgCard = Color(0xFF161E22)         // Kartu & kontainer sekunder
val MoriBgSurface = Color(0xFF1C272C)      // Permukaan elevated / dialog
val MoriBgSurfaceHover = Color(0xFF243339) // State tombol / sorotan

// Accent & Primary (Forest Emerald)
val MoriEmeraldPrimary = Color(0xFF4E9F76) // Aksen utama (tombol tonton, tab aktif)
val MoriEmeraldLight = Color(0xFF70BF97)   // Aksen terang
val MoriEmeraldDark = Color(0xFF275C41)    // Aksen gelap / badge
val MoriEmeraldSubtle = Color(0xFF1E3A2E)  // Latar belakang chip terpilih

// Secondary & Warmth (Amber / Autumn Leaf)
val MoriAmberAccent = Color(0xFFE5A952)    // Rating bintang & tag status
val MoriAmberSubtle = Color(0xFF3B2E1C)
val MoriCoralAlert = Color(0xFFE06D53)     // Hapus / Error / Batal

// Borders & Dividers
val MoriBorder = Color(0xFF25343A)         // Garis batas kartu
val MoriBorderFocused = Color(0xFF4E9F76)  // Garis batas saat aktif/fokus
val MoriDivider = Color(0xFF1D282E)

// Text Colors
val MoriTextPrimary = Color(0xFFEDF3EE)    // Teks judul / teks utama
val MoriTextSecondary = Color(0xFFA1B3B8)  // Teks deskripsi / sinopsis
val MoriTextMuted = Color(0xFF6B7F85)      // Waktu rilis / label kecil
val MoriTextInverse = Color(0xFF0F1416)    // Teks di atas warna terang

// Player Overlay Colors
val MoriPlayerOverlay = Color(0xCC0A0E10)
val MoriPlayerControls = Color(0xFFEDF3EE)
