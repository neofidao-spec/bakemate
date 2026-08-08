package com.bakemate.di

import android.content.Context
import androidx.room.Room
import com.bakemate.data.local.BakeMateDatabase
import com.bakemate.data.local.BakeMateDatabase.Companion.MIGRATION_1_2
import com.bakemate.data.local.dao.BakeSessionDao
import com.bakemate.data.local.dao.RecipeDao
import com.bakemate.data.local.dao.StarterLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BakeMateDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BakeMateDatabase::class.java,
            "bakemate_db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideRecipeDao(db: BakeMateDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideStarterLogDao(db: BakeMateDatabase): StarterLogDao = db.starterLogDao()

    @Provides
    fun provideBakeSessionDao(db: BakeMateDatabase): BakeSessionDao = db.bakeSessionDao()
}
