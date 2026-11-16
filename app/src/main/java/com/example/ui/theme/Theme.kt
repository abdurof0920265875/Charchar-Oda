package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val BuildiLightColorScheme =
  lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainerLight,
    onPrimaryContainer = TerracottaOnContainerLight,
    secondary = BlueprintNavy,
    onSecondary = Color.White,
    secondaryContainer = BlueprintContainerLight,
    onSecondaryContainer = BlueprintOnContainerLight,
    tertiary = SageMeadow,
    onTertiary = Color.White,
    tertiaryContainer = SageContainerLight,
    onTertiaryContainer = SageOnContainerLight,
    background = PaperBackgroundLight,
    onBackground = InkDark,
    surface = SurfaceLight,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkMutedLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = BrickErrorLight,
    onError = Color.White,
    errorContainer = BrickErrorContainerLight,
    onErrorContainer = Color(0xFF410002),
  )

private val BuildiDarkColorScheme =
  darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Color(0xFF2F1004),
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = TerracottaOnContainerDark,
    secondary = BlueprintNavyLight,
    onSecondary = Color(0xFF0E2438),
    secondaryContainer = BlueprintContainerDark,
    onSecondaryContainer = BlueprintOnContainerDark,
    tertiary = SageLight,
    onTertiary = Color(0xFF0A2E1C),
    tertiaryContainer = SageContainerDark,
    onTertiaryContainer = SageOnContainerDark,
    background = SlateBackgroundDark,
    onBackground = InkLight,
    surface = SurfaceDark,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = InkMutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = BrickErrorDark,
    onError = Color(0xFF690005),
    errorContainer = BrickErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
  )

val BuildiShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun BuildiTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> BuildiDarkColorScheme
      else -> BuildiLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = BuildiTypography,
    shapes = BuildiShapes,
    content = content,
  )
}
