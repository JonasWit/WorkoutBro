package com.wtk_apps.workoutbro.theme

import androidx.compose.ui.graphics.Color

private const val INK_HEX = 0xFF0A0A0B
private const val SURFACE_1_HEX = 0xFF17171A
private const val SURFACE_2_HEX = 0xFF232327
private const val OUTLINE_1_HEX = 0xFF34343B
private const val TEXT_PRIMARY_HEX = 0xFFF3EFE4
private const val TEXT_SECONDARY_HEX = 0xFFB9AF9A
private const val ACCENT_GOLD_HEX = 0xFFD4AF37
private const val WORK_YELLOW_HEX = 0xFFF5C542
private const val WORK_ORANGE_HEX = 0xFFE8862B
private const val WORK_RED_ORANGE_HEX = 0xFFC7431F
private const val REST_GREEN_HEX = 0xFF57B37B
private const val PREPARE_BLUE_HEX = 0xFF5B8FD9

// Black-and-gold palette: near-black backgrounds with gold/yellow/orange accents.
val Ink = Color(INK_HEX) // app background (near black)
val Surface1 = Color(SURFACE_1_HEX)
val Surface2 = Color(SURFACE_2_HEX)
val Outline1 = Color(OUTLINE_1_HEX)
val TextPrimary = Color(TEXT_PRIMARY_HEX) // warm off-white
val TextSecondary = Color(TEXT_SECONDARY_HEX) // warm grey

val AccentGold = Color(ACCENT_GOLD_HEX) // primary accent / buttons

// Timer phase anchors (gold -> orange -> deep orange-red as a round elapses).
val WorkYellow = Color(WORK_YELLOW_HEX)
val WorkOrange = Color(WORK_ORANGE_HEX)
val WorkRedOrange = Color(WORK_RED_ORANGE_HEX)
val RestGreen = Color(REST_GREEN_HEX)
val PrepareBlue = Color(PREPARE_BLUE_HEX)
