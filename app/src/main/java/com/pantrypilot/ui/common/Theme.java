package com.pantrypilot.ui.common;

import androidx.compose.material3.ColorScheme;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.Typography;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.graphics.Color;

/**
 * PantryPilot Material You theme.
 * <p>
 * Primary:    Forest Green #1B4332
 * Secondary:  Amber        #F59E0B
 * Background: Off-white    #FAFAF7
 * <p>
 * Typography: Lora (serif) for display/headings, M3 defaults for body.
 */
public class Theme {

    // ── Colors ────────────────────────────────────────────────────────────────
    static final Color ForestGreen = new Color(0xFF1B4332);
    static final Color ForestGreenLight = new Color(0xFF52796F);
    static final Color Amber = new Color(0xFFF59E0B);
    static final Color AmberDark = new Color(0xFFB45309);
    static final Color OffWhite = new Color(0xFFFAFAF7);
    static final Color SurfaceGreen = new Color(0xFFE8F5E9);
    static final Color OnForestGreen = new Color(0xFFFFFFFF);
    static final Color ErrorRed = new Color(0xFFB00020);

    static final ColorScheme LightColorScheme = new ColorScheme(
            /* primary */              ForestGreen,
            /* onPrimary */            OnForestGreen,
            /* primaryContainer */     SurfaceGreen,
            /* onPrimaryContainer */   ForestGreen,
            /* secondary */            Amber,
            /* onSecondary */          new Color(0xFF000000),
            /* secondaryContainer */   new Color(0xFFFEF3C7),
            /* onSecondaryContainer */ AmberDark,
            /* tertiary */             new Color(0xFF6D4C41),
            /* onTertiary */           new Color(0xFFFFFFFF),
            /* tertiaryContainer */    new Color(0xFFEFEBE9),
            /* onTertiaryContainer */  new Color(0xFF3E2723),
            /* error */                ErrorRed,
            /* onError */              new Color(0xFFFFFFFF),
            /* errorContainer */       new Color(0xFFFFDAD4),
            /* onErrorContainer */     new Color(0xFF410002),
            /* background */           OffWhite,
            /* onBackground */         new Color(0xFF1C1B1F),
            /* surface */              OffWhite,
            /* onSurface */            new Color(0xFF1C1B1F),
            /* surfaceVariant */       new Color(0xFFDCE4DD),
            /* onSurfaceVariant */     new Color(0xFF404943),
            /* outline */              new Color(0xFF707973),
            /* outlineVariant */       new Color(0xFFC0C8C1),
            /* scrim */                new Color(0xFF000000),
            /* inverseSurface */       new Color(0xFF313030),
            /* inverseOnSurface */     new Color(0xFFF4F0EF),
            /* inversePrimary */       ForestGreenLight,
            /* surfaceDim */           new Color(0xFFDDD9D8),
            /* surfaceBright */        OffWhite,
            /* surfaceContainerLowest */  new Color(0xFFFFFFFF),
            /* surfaceContainerLow */     new Color(0xFFF7F3F2),
            /* surfaceContainer */        new Color(0xFFF1EDEC),
            /* surfaceContainerHigh */    new Color(0xFFEBE7E6),
            /* surfaceContainerHighest */ new Color(0xFFE6E2E1)
    );

    // ── Typography with Lora for headings ─────────────────────────────────────
    // NOTE: Add Lora font files to res/font/ and define FontFamily in Kotlin
    // interop layer or use Google Fonts composable. Shown here as reference.
    //
    // val LoraFontFamily = FontFamily(
    //     Font(R.font.lora_regular, FontWeight.Normal),
    //     Font(R.font.lora_semibold, FontWeight.SemiBold),
    //     Font(R.font.lora_bold, FontWeight.Bold)
    // )
    //
    // val PantryTypography = Typography(
    //     displayLarge  = TextStyle(fontFamily = LoraFontFamily, fontSize = 57.sp),
    //     displayMedium = TextStyle(fontFamily = LoraFontFamily, fontSize = 45.sp),
    //     displaySmall  = TextStyle(fontFamily = LoraFontFamily, fontSize = 36.sp),
    //     headlineLarge = TextStyle(fontFamily = LoraFontFamily, fontSize = 32.sp),
    //     headlineMedium= TextStyle(fontFamily = LoraFontFamily, fontSize = 28.sp),
    //     headlineSmall = TextStyle(fontFamily = LoraFontFamily, fontSize = 24.sp),
    //     // Body + label use M3 defaults (Roboto/system)
    // )

    @Composable
    public static void PantryPilotTheme(boolean darkTheme, Runnable content) {
        MaterialTheme(
                /* colorScheme = */ LightColorScheme,
                /* typography  = */ Typography.Default,
                /* content     = */ content
        );
    }
}
