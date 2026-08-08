package com.bakemate.domain.usecase

import com.bakemate.domain.model.RecipeFormula

/**
 * Validasi resep sebelum disimpan. Mengembalikan pesan error atau null jika valid.
 */
object ValidateRecipe {

    fun validate(formula: RecipeFormula): String? {
        if (formula.name.isBlank()) return "Nama resep wajib diisi"
        if (formula.ingredients.isEmpty()) return "Tambahkan minimal satu bahan"
        if (formula.flourWeight <= 0.0) return "Minimal satu bahan harus tepung (aktifkan 'Tepung')"
        val invalid = formula.ingredients.firstOrNull { it.grams <= 0.0 || it.name.isBlank() }
        if (invalid != null) return "Bahan '${invalid.name.ifBlank { "(kosong)" }}' harus punya gram lebih dari 0"
        if (formula.steps.isEmpty()) return "Tambahkan minimal satu langkah"
        return null
    }
}
