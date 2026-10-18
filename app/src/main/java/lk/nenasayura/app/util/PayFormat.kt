package lk.nenasayura.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Rs. 2,700.00" */
fun rs(value: Double): String = "Rs. " + String.format(Locale.US, "%,.2f", value)

/** "18 Oct 2026, 06:35 PM" (empty when the time is not known yet). */
fun formatDateTime(millis: Long): String =
    if (millis <= 0L) "" else SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))

/** Rounds money to 2 decimals so cents never drift (for example 2700.0000001). */
fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0