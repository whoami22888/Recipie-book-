package com.beyondhuman.kitchen.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val sourceRecipeNumber: Int,
    val title: String,
    val description: String,
    val section: String,
    val shoppingListJson: String,
    val methodJson: String,
    val sourcePagesJson: String,
    val qualityFlagsJson: String
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY sourceRecipeNumber")
    suspend fun all(): List<RecipeEntity>

    @Query("""
        SELECT * FROM recipes
        WHERE title LIKE :query
           OR description LIKE :query
           OR section LIKE :query
           OR shoppingListJson LIKE :query
        ORDER BY sourceRecipeNumber
    """)
    suspend fun search(query: String): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeEntity>)

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int
}

@Database(entities = [RecipeEntity::class], version = 2, exportSchema = false)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun recipes(): RecipeDao
}
