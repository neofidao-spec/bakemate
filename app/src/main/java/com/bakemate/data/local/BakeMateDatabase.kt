package com.bakemate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bakemate.data.local.dao.RecipeDao
import com.bakemate.data.local.dao.StarterLogDao
import com.bakemate.data.local.entity.Recipe
import com.bakemate.data.local.entity.StarterLog

@Database(
    entities = [Recipe::class, StarterLog::class],
    version = 1,
    exportSchema = false
)
abstract class BakeMateDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun starterLogDao(): StarterLogDao
}
