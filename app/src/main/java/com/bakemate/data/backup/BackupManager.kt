package com.bakemate.data.backup

import android.content.Context
import android.net.Uri
import com.bakemate.data.local.dao.RecipeDao
import com.bakemate.data.local.dao.StarterLogDao
import com.bakemate.data.local.entity.Recipe
import com.bakemate.data.local.entity.RecipeIngredient
import com.bakemate.data.local.entity.RecipeStep
import com.bakemate.data.local.entity.StarterLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ekspor/impor seluruh data pengguna (resep + bahan + langkah + jurnal starter)
 * ke satu file JSON. Output di cacheDir agar bisa dibagikan via FileProvider.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recipeDao: RecipeDao,
    private val starterLogDao: StarterLogDao
) {

    companion object {
        const val BACKUP_VERSION = 1
        const val FILE_NAME = "bakemate_backup.json"
    }

    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val recipes = recipeDao.getAll()
        val ingredients = recipes.flatMap { recipeDao.getIngredients(it.id) }
        val steps = recipes.flatMap { recipeDao.getSteps(it.id) }
        val starterLogs = starterLogDao.getAll()

        val root = JSONObject()
            .put("version", BACKUP_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("recipes", JSONArray().apply {
                recipes.forEach { recipe ->
                    put(recipe.toJson())
                }
            })
            .put("recipeIngredients", JSONArray().apply {
                ingredients.forEach { put(it.toJson()) }
            })
            .put("recipeSteps", JSONArray().apply {
                steps.forEach { put(it.toJson()) }
            })
            .put("starterLogs", JSONArray().apply {
                starterLogs.forEach { put(it.toJson()) }
            })

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        file.writeText(root.toString(2))
        file
    }

    suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(IllegalStateException("Tidak bisa membaca file"))
            val root = JSONObject(json)

            // Mulai transaksi: hapus semua data lama dulu
            recipeDao.deleteAll()
            starterLogDao.deleteAll()

            val recipesJson = root.optJSONArray("recipes") ?: JSONArray()
            val ingredientsJson = root.optJSONArray("recipeIngredients") ?: JSONArray()
            val stepsJson = root.optJSONArray("recipeSteps") ?: JSONArray()

            // Re-map id lama → id baru (autoGenerate)
            val idMap = mutableMapOf<Long, Long>()

            for (i in 0 until recipesJson.length()) {
                val obj = recipesJson.getJSONObject(i)
                val oldId = obj.getLong("id")
                val recipe = Recipe(
                    name = obj.getString("name"),
                    description = obj.optString("description", ""),
                    hydrationPercent = obj.optDouble("hydrationPercent", 0.0),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                val newId = recipeDao.insertFull(recipe)
                idMap[oldId] = newId
            }

            // Bahan & langkah (pakai pemetaan id)
            for (i in 0 until ingredientsJson.length()) {
                val obj = ingredientsJson.getJSONObject(i)
                val newRecipeId = idMap[obj.getLong("recipeId")] ?: continue
                recipeDao.insertIngredient(
                    RecipeIngredient(
                        recipeId = newRecipeId,
                        name = obj.getString("name"),
                        grams = obj.getDouble("grams"),
                        isFlour = obj.optBoolean("isFlour", false),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }
            for (i in 0 until stepsJson.length()) {
                val obj = stepsJson.getJSONObject(i)
                val newRecipeId = idMap[obj.getLong("recipeId")] ?: continue
                recipeDao.insertStep(
                    RecipeStep(
                        recipeId = newRecipeId,
                        text = obj.getString("text"),
                        minutes = obj.optInt("minutes", 0),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }

            // Jurnal starter
            val logsJson = root.optJSONArray("starterLogs") ?: JSONArray()
            for (i in 0 until logsJson.length()) {
                val obj = logsJson.getJSONObject(i)
                starterLogDao.insert(
                    StarterLog(
                        starterName = obj.optString("starterName", "Starter Utama"),
                        feedingTime = obj.optLong("feedingTime", obj.optLong("date", System.currentTimeMillis())),
                        ratio = obj.optString("ratio", "1:1:1"),
                        activityLevel = obj.optInt("activityLevel", 3),
                        note = obj.optString("note", ""),
                        photoPath = obj.optString("photoPath", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val totalRecipes = idMap.size
            Result.success(totalRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== JSON serialization =====
    private fun Recipe.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("description", description)
        .put("hydrationPercent", hydrationPercent)
        .put("isFavorite", isFavorite)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)

    private fun RecipeIngredient.toJson(): JSONObject = JSONObject()
        .put("recipeId", recipeId)
        .put("name", name)
        .put("grams", grams)
        .put("isFlour", isFlour)
        .put("sortOrder", sortOrder)

    private fun RecipeStep.toJson(): JSONObject = JSONObject()
        .put("recipeId", recipeId)
        .put("text", text)
        .put("minutes", minutes)
        .put("sortOrder", sortOrder)

    private fun StarterLog.toJson(): JSONObject = JSONObject()
        .put("starterName", starterName)
        .put("feedingTime", feedingTime)
        .put("ratio", ratio)
        .put("activityLevel", activityLevel)
        .put("note", note)
        .put("photoPath", photoPath)
        .put("createdAt", createdAt)
}
