package com.example.phonebserver.logic.dataClasses

import com.example.phonebserver.KtorWebsocket.Message


data class ComparingLists(
    val oldDirs: List<String> = emptyList(),
    val dirs: Message.ScanResult = Message.ScanResult(),
)
