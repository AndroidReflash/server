package com.example.phonebserver.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SingleBackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: SingleBackup)

    @Query("SELECT * FROM single_backup")
    fun getAllSingleBackups(): Flow<List<SingleBackup>>
}