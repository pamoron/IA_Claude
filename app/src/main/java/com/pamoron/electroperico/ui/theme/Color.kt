package com.pamoron.electroperico.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta base: verde eléctrico para la marca y ámbar/rojo para las valoraciones.
// Los colores nunca son el único medio de comunicar una valoración: siempre van
// acompañados de icono y texto.

val GreenPrimary = Color(0xFF00E5FF)
val GreenOnPrimary = Color(0xFF001014)
val GreenPrimaryContainer = Color(0xFF003E47)
val GreenOnPrimaryContainer = Color(0xFF8FF5FF)

val GreenPrimaryDark = GreenPrimary
val GreenOnPrimaryDark = GreenOnPrimary
val GreenPrimaryContainerDark = GreenPrimaryContainer
val GreenOnPrimaryContainerDark = GreenOnPrimaryContainer

val BlueSecondary = Color(0xFF4A6363)
val BlueOnSecondary = Color(0xFFFFFFFF)
val BlueSecondaryContainer = Color(0xFFCCE8E7)
val BlueOnSecondaryContainer = Color(0xFF051F1F)

val BlueSecondaryDark = Color(0xFFB0CCCB)
val BlueOnSecondaryDark = Color(0xFF1B3534)
val BlueSecondaryContainerDark = Color(0xFF324B4B)
val BlueOnSecondaryContainerDark = Color(0xFFCCE8E7)

// Acento eléctrico de Stitch. Se reserva para llamadas a la acción y nunca se
// usa como único indicador de estado.
val ElectricAccent = Color(0xFFFFD740)
val OnElectricAccent = Color(0xFF241A00)
val ElectricAccentContainer = Color(0xFF3A3000)
val OnElectricAccentContainer = Color(0xFFFFE082)
val ElectricAccentDark = Color(0xFFFFDA6A)
val OnElectricAccentDark = Color(0xFF3A2E00)
val ElectricAccentContainerDark = Color(0xFF554500)
val OnElectricAccentContainerDark = Color(0xFFFFE9A9)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundLight = Color(0xFF030706)
val OnBackgroundLight = Color(0xFFE5F5F3)
val SurfaceVariantLight = Color(0xFF0A1111)
val OnSurfaceVariantLight = Color(0xFFB5C9C6)
val OutlineLight = Color(0xFF31504D)

val BackgroundDark = BackgroundLight
val OnBackgroundDark = OnBackgroundLight
val SurfaceVariantDark = SurfaceVariantLight
val OnSurfaceVariantDark = OnSurfaceVariantLight
val OutlineDark = OutlineLight

// --- Colores de valoración --------------------------------------------------
// Se usan como fondo de contenedor de las etiquetas de valoración.

val RatingGoodContainerLight = Color(0xFFC7F0C2)
val RatingGoodContentLight = Color(0xFF0C3A0A)
val RatingNeutralContainerLight = Color(0xFFDDE3EA)
val RatingNeutralContentLight = Color(0xFF1A1C1E)
val RatingWarnContainerLight = Color(0xFFFFE1A9)
val RatingWarnContentLight = Color(0xFF3B2A00)
val RatingBadContainerLight = Color(0xFFFFDAD6)
val RatingBadContentLight = Color(0xFF410002)

val RatingGoodContainerDark = Color(0xFF1F4A1C)
val RatingGoodContentDark = Color(0xFFC7F0C2)
val RatingNeutralContainerDark = Color(0xFF3B4046)
val RatingNeutralContentDark = Color(0xFFE2E2E6)
val RatingWarnContainerDark = Color(0xFF553F00)
val RatingWarnContentDark = Color(0xFFFFE1A9)
val RatingBadContainerDark = Color(0xFF93000A)
val RatingBadContentDark = Color(0xFFFFDAD6)
