package com.example.phonebserver.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Serializable
@Entity(tableName = "single_backup")
data class SingleBackup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val backupName: String,
    val archiveUri: String,
    val textUri: String
)
