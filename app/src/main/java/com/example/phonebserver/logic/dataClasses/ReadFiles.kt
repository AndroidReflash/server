package com.example.phonebserver.logic.dataClasses

data class ReadFiles(
    val oldFileName: String = "",
    val listFiles: List<String> = emptyList()
)
