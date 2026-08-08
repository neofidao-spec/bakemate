package com.bakemate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bakemate.data.local.dao.BakeSessionDao
import com.bakemate.data.local.dao.RecipeDao
import com.bakemate.data.local.dao.StarterLogDao
import com.bakemate.data.local.entity.BakeSession
import com.bakemate.data.local.entity.Recipe
import com.bakemate.data.local.entity.RecipeIngredient
import com.bakemate.data.local.entity.RecipeStep
import com.bakemate.data.local.entity.StarterLog

@Database(
    entities = [
        Recipe::class,
        RecipeIngredient::class,
        RecipeStep::class,
        StarterLog::class,
        BakeSession::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BakeMateDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun starterLogDao(): StarterLogDao
    abstract fun bakeSessionDao(): BakeSessionDao

    companion object {
        /**
         * v1 → v2: tambah tabel recipe_ingredients, recipe_steps, bake_sessions.
         * Data resep existing (4 kolom bahan tetap) dipindah ke baris ingredient.
         * Langkah existing (kolom steps teks) tidak bisa di-parse → disimpan
         * sebagai satu langkah "Instruksi lengkap" agar tidak hilang.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabel bahan dinamis
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `grams` REAL NOT NULL,
                        `isFlour` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)"
                )

                // Tabel langkah dinamis
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_steps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `minutes` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recipe_steps_recipeId` ON `recipe_steps` (`recipeId`)"
                )

                // Tabel sesi baking
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bake_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER,
                        `recipeName` TEXT NOT NULL,
                        `stagesJson` TEXT NOT NULL,
                        `stageEndsJson` TEXT NOT NULL,
                        `currentStageIndex` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `bakeMode` INTEGER NOT NULL,
                        `status` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Kolom updatedAt di recipes
                db.execSQL("ALTER TABLE `recipes` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `recipes` SET `updatedAt` = `createdAt`")

                // Migrasi data: 4 kolom bahan → baris ingredient
                db.execSQL(
                    """
                    INSERT INTO `recipe_ingredients` (`recipeId`, `name`, `grams`, `isFlour`, `sortOrder`)
                    SELECT `id`, 'Tepung', `totalFlourGrams`, 1, 0 FROM `recipes` WHERE `totalFlourGrams` > 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recipe_ingredients` (`recipeId`, `name`, `grams`, `isFlour`, `sortOrder`)
                    SELECT `id`, 'Air', `totalWaterGrams`, 0, 1 FROM `recipes` WHERE `totalWaterGrams` > 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recipe_ingredients` (`recipeId`, `name`, `grams`, `isFlour`, `sortOrder`)
                    SELECT `id`, 'Starter', `totalStarterGrams`, 0, 2 FROM `recipes` WHERE `totalStarterGrams` > 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recipe_ingredients` (`recipeId`, `name`, `grams`, `isFlour`, `sortOrder`)
                    SELECT `id`, 'Garam', `totalSaltGrams`, 0, 3 FROM `recipes` WHERE `totalSaltGrams` > 0
                    """.trimIndent()
                )

                // Migrasi data: kolom steps teks → satu langkah instruksi
                db.execSQL(
                    """
                    INSERT INTO `recipe_steps` (`recipeId`, `text`, `minutes`, `sortOrder`)
                    SELECT `id`, `steps`, 0, 0 FROM `recipes` WHERE `steps` IS NOT NULL AND `steps` != ''
                    """.trimIndent()
                )
            }
        }
    }
}
