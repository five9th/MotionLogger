package com.five9th.motionlogger.presentation.ui

import androidx.core.widget.doAfterTextChanged
import com.five9th.motionlogger.R
import com.google.android.material.textfield.TextInputLayout

class KeywordValidator(private val til: TextInputLayout) {

    private val et = til.editText ?: throw RuntimeException("EditText is null.")

    init {
        et.doAfterTextChanged { text ->
            text?.let {
                validate(it.toString())
            }
        }
    }

    private fun validate(input: String) {
        til.error = if (isValid(input)) null
        else til.context.getString(R.string.err_forbiden_characters)
    }

    fun getCurrentWordOrNull(): String? {
        val keyword = et.text.toString()

        return if (isValid(keyword)) keyword else null
    }

    companion object {
        private val ALLOWED_REGEX =
            Regex("^[\\p{L}\\p{N}_.,]*$")

        private const val MAX_LENGTH = 32

        private fun isValid(input: String): Boolean =
            input.length <= MAX_LENGTH &&
                    ALLOWED_REGEX.matches(input)
    }
}