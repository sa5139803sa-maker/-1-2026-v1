package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand & Financial Ledger Palette
val EmeraldPrimaryLight = Color(0xFF0F766E)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFCCFBF1)
val EmeraldOnContainerLight = Color(0xFF064E3B)

val SlateSecondaryLight = Color(0xFF334155)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateContainerLight = Color(0xFFE2E8F0)
val SlateOnContainerLight = Color(0xFF1E293B)

val AmberTertiaryLight = Color(0xFFD97706)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFEF3C7)
val AmberOnContainerLight = Color(0xFF78350F)

val BackgroundLight = Color(0xFFF6FAF9)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFEAF2F0)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)

// Dark Theme Palette
val EmeraldPrimaryDark = Color(0xFF2DD4BF)
val EmeraldOnPrimaryDark = Color(0xFF003831)
val EmeraldContainerDark = Color(0xFF115E59)
val EmeraldOnContainerDark = Color(0xFF99F6E4)

val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateContainerDark = Color(0xFF1E293B)
val SlateOnContainerDark = Color(0xFFE2E8F0)

val AmberTertiaryDark = Color(0xFFFBBF24)
val AmberOnTertiaryDark = Color(0xFF451A03)
val AmberContainerDark = Color(0xFF78350F)
val AmberOnContainerDark = Color(0xFFFEF3C7)

val BackgroundDark = Color(0xFF0B1314)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF132022)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF1B2B2E)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF334155)
val OutlineVariantDark = Color(0xFF1E293B)

val ErrorLight = Color(0xFFDC2626)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorDark = Color(0xFFF87171)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

@Immutable
data class LedgerSemanticColors(
    val paid: Color,
    val paidContainer: Color,
    val onPaidContainer: Color,
    val partial: Color,
    val partialContainer: Color,
    val onPartialContainer: Color,
    val unpaid: Color,
    val unpaidContainer: Color,
    val onUnpaidContainer: Color,
    val chartCollected: Color,
    val chartRemaining: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

val LightLedgerColors = LedgerSemanticColors(
    paid = Color(0xFF0D9488),
    paidContainer = Color(0xFFCCFBF1),
    onPaidContainer = Color(0xFF065F46),
    partial = Color(0xFFD97706),
    partialContainer = Color(0xFFFEF3C7),
    onPartialContainer = Color(0xFF92400E),
    unpaid = Color(0xFFDC2626),
    unpaidContainer = Color(0xFFFEE2E2),
    onUnpaidContainer = Color(0xFF991B1B),
    chartCollected = Color(0xFF0D9488),
    chartRemaining = Color(0xFFF59E0B),
    heroStart = Color(0xFF0F766E),
    heroEnd = Color(0xFF064E3B),
)

val DarkLedgerColors = LedgerSemanticColors(
    paid = Color(0xFF2DD4BF),
    paidContainer = Color(0xFF134E4A),
    onPaidContainer = Color(0xFF99F6E4),
    partial = Color(0xFFFBBF24),
    partialContainer = Color(0xFF78350F),
    onPartialContainer = Color(0xFFFEF3C7),
    unpaid = Color(0xFFF87171),
    unpaidContainer = Color(0xFF7F1D1D),
    onUnpaidContainer = Color(0xFFFECACA),
    chartCollected = Color(0xFF2DD4BF),
    chartRemaining = Color(0xFFFBBF24),
    heroStart = Color(0xFF115E59),
    heroEnd = Color(0xFF042F2E),
)

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }
