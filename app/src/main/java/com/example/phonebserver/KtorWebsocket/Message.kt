package com.example.phonebserver.KtorWebsocket

import com.example.phonebserver.Room.SingleBackup
import kotlinx.serialization.Serializable


@Serializable
sealed class Message {
    @Serializable
    data class Scan(
        val singletonScan: String,
        val appName: String,
        val delay: String
    ) : Message()

    @Serializable
    data class RoomId(
        val id: Int,
        val command: String
    ) : Message()

    @Serializable
    data class Memory(val ram: List<String>): Message()

    @Serializable
    data class ScanResult(
        var directories: List<String> = emptyList(),
        val size: String = "",
        val time: String = "",
        val oldFileName: String = "",
        var scanDuration: String = "",
        var packageName: String = ""
    ):Message()

    @Serializable
    data class RestoreResult(
        val scanDuration: String = "",
        val start: String
    ):Message()

    @Serializable
    data class BackupList(
        val listOfBackups: List<SingleBackup> = emptyList()
    ):Message()

    @Serializable
    data class CloseSession(val reason: String) : Message()
}

@Serializable
data class MessageContainer(
    val type: String,
    val payload: String
)