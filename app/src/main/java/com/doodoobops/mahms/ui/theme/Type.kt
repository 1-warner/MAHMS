package com.doodoobops.mahms.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.doodoobops.mahms.R

/** Public Sans — all UI text. */
val PublicSans = FontFamily(
    Font(R.font.public_sans_regular, FontWeight.Normal),
    Font(R.font.public_sans_medium, FontWeight.Medium),
    Font(R.font.public_sans_semibold, FontWeight.SemiBold),
    Font(R.font.public_sans_bold, FontWeight.Bold),
)

/** IBM Plex Mono — clinical data (vitals, dosages, IDs, timestamps). */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/** Material3 typography mapped to the MAHMS scale. */
val MahmsTypography = Typography(
    // Display 34 Bold, tracking -0.01em
    displaySmall = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.01).em,
    ),
    // H1 26 Bold
    headlineMedium = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 35.sp, letterSpacing = (-0.01).em,
    ),
    // H2 22 SemiBold
    headlineSmall = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 30.sp,
    ),
    // H3 18 SemiBold
    titleLarge = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    // Body 16 Regular, line-height 1.5
    bodyLarge = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    // Small 14
    bodyMedium = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp,
    ),
    // Caption 12
    bodySmall = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    // Overline 12 SemiBold uppercase tracking 0.08em
    labelMedium = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em,
    ),
    labelSmall = TextStyle(
        fontFamily = PublicSans, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp,
    ),
)

/** Mono/data style: IBM Plex Mono 14 Medium, tabular numerals. */
val DataTextStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    textGeometricTransform = TextGeometricTransform(),
)
