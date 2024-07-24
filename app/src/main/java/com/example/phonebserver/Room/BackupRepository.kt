package com.example.phonebserver.Room

import kotlinx.coroutines.flow.Flow

class BackupRepository(private val dao: SingleBackupDao) {

    suspend fun insertBackup(backup: SingleBackup) {
        dao.insertBackup(backup)
    }

    fun getSingleBackupsByBackupName(): Flow<List<SingleBackup>> {
        return dao.getAllSingleBackups()
    }
}