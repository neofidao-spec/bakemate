package com.bakemate.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** TextField numerik dengan label. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // hanya angka, titik, dan koma desimal
            val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
            // hanya satu separator desimal
            val normalized = if (filtered.count { it == '.' || it == ',' } > 1) {
                filtered.dropLast(1)
            } else {
                filtered
            }
            onValueChange(normalized)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } }
    )
}

/** TextField teks biasa. */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines
    )
}
