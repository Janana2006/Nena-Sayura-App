package lk.nenasayura.app.util

import android.util.Patterns

object Validators {
    fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isValidPassword(password: String): Boolean = password.length >= 6
}