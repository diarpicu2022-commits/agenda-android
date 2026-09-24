package com.dpinta.agenda.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "place")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val googlePlaceId: String?,
)

@Dao
interface PlaceDao {
    @Query("SELECT * FROM place ORDER BY name")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Insert
    suspend fun insert(place: PlaceEntity): Long
}

@Database(entities = [PlaceEntity::class], version = 1, exportSchema = true)
abstract class AgendaDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}
