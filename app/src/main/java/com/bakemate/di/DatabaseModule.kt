package com.bakemate.di

import android.content.Context
import androidx.room.Room
import com.bakemate.data.local.BakeMateDatabase
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
        ).build()
    }

    @Provides
    fun provideRecipeDao(db: BakeMateDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideStarterLogDao(db: BakeMateDatabase): StarterLogDao = db.starterLogDao()
}
