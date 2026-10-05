package lk.nenasayura.app.ui.theme

import androidx.compose.ui.graphics.Color

/** A soft icon colour and the pale circle that sits behind the icon. */
class IconPalette(val tint: Color, val container: Color)

/**
 * Subtle icon colours that sit next to the blue/teal Nena Sayura theme.
 * Only icons and their small background circles use these.
 */
object NenaIconColors {
    val Papers = IconPalette(Color(0xFF2F5FC4), Color(0xFFDCE6FB))         // blue
    val MarkingSchemes = IconPalette(Color(0xFF7A4FB5), Color(0xFFECE2F8)) // violet
    val Quizzes = IconPalette(Color(0xFF4F46B8), Color(0xFFE3E1FA))        // indigo
    val GuideBooks = IconPalette(Color(0xFFB07A00), Color(0xFFFFF0CC))     // soft gold
    val ResourceBooks = IconPalette(Color(0xFF00897B), Color(0xFFD2F0EB))  // teal
    val Timetable = IconPalette(Color(0xFF0277BD), Color(0xFFD9EEF9))      // sky blue
    val Reminders = IconPalette(Color(0xFFB8660A), Color(0xFFFFE8D1))      // warm amber
    val Teacher = IconPalette(Color(0xFFB07A00), Color(0xFFFFF0CC))        // soft gold
    val Progress = IconPalette(Color(0xFF00796B), Color(0xFFCDEDE7))       // teal
    val Reviews = IconPalette(Color(0xFFE0A000), Color(0xFFFFF3CC))        // star gold
}