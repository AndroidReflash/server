package com.example.phonebserver.logic.singleTones

sealed class SingletonesForChecking(val value: String) {
    object ScanNew: SingletonesForChecking(value = "NewFileObjectScanPhoneBServer")
    object ScanChanged: SingletonesForChecking(value = "ChangedFileObjectScanPhoneBServer")
    object ArchivesDir: SingletonesForChecking(value = "/data/data/com.example.phonebserver/files")
}