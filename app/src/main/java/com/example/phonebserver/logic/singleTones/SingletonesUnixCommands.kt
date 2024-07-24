package com.example.phonebserver.logic.singleTones

sealed class SingletonesUnixCommands(val command: String) {
    //getting list of files. It is needed add packageName right after
    object ScanList: SingletonesUnixCommands(command = "find /data/data/")
    //getting size of files. It is needed add packageName right after
    object MemoryForEach: SingletonesUnixCommands(command = " -exec du -h {} +")
    //getting size of files and catalogs. It is needed add packageName right after.
    //used for getting overall memory package uses
    object MemoryForEachCatalog: SingletonesUnixCommands(command = " -exec du -sh {} +")
    //used for getting use of RAM certain package. Package should be added right after this command
    object MemoryInfo: SingletonesUnixCommands(command = "ps -A | grep ")
    //path of extracting archive need to be added
    object ExtractFiles: SingletonesUnixCommands(command = "tar -xvf ")
    //achieves super user for command
    object SuperUser: SingletonesUnixCommands(command = "su")
    //needed for super user as well
    object MinusC: SingletonesUnixCommands(command = "-c")
}