package com.example.phonebserver.logic

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.phonebserver.KtorWebsocket.Message
import com.example.phonebserver.KtorWebsocket.WebSocketService
import com.example.phonebserver.Room.SingleBackup
import com.example.phonebserver.interfaces.MainActivityInterface
import com.example.phonebserver.logic.dataClasses.ComparingLists
import com.example.phonebserver.logic.dataClasses.InputValues
import com.example.phonebserver.logic.dataClasses.ReadFiles
import com.example.phonebserver.logic.singleTones.NavigationSingleTones
import com.example.phonebserver.logic.singleTones.SingletonesForChecking
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant

class MainActivityLogic (
    private val context: Context,
    private val viewModel: MainViewModel,
    private val navController: NavController
): MainActivityInterface {
    private val webSocketService = WebSocketService(viewModel)

    //scanning for tree of files, comparing it with old tree
    override fun start(){
        val start = Instant.now() //for checking the duration of scan
        val inputValues = viewModel.inputValues.value
        val startLogic = StartLogicScan()
        //creates a list of current files in defined package
        val dirs = startLogic.scanApplicationFiles(inputValues.appName, context)
        //creates a list of previous files in defined package
        val oldDirs = readFileFromInternalStorage(context, inputValues.oldFileName, viewModel.backups.value.reversed())
        val comparedList = processLists(oldDirs.listFiles, dirs.directories)
        val end = Instant.now()
        val scanDuration = duration(start, end).toString()
        //creates temporary fake link to backup file
        viewModel.updateScanResult(
            Message.ScanResult(
                directories = comparedList,
                time = dirs.time,
                size = dirs.size,
                scanDuration = scanDuration,
                packageName = dirs.packageName,
                oldFileName = dirs.oldFileName
            )
        )
        //two lists for saving files
        viewModel.updateComparingLists(ComparingLists(oldDirs.listFiles, dirs))
    }
    override fun config() {
        navController.navigate(NavigationSingleTones.ConfigScreen.route)
    }

    override fun turnOnServer() {
        runCommandAsRoot() // to show root dialog if there is no root access

        webSocketService.createServer(context) {
            viewModel.viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    start()
                }
                // start of a scan
                withContext(Dispatchers.IO) {
                    webSocketService.sendScanResult(
                        viewModel.webSocketSession as WebSocketSession,
                        viewModel.scanResult.value
                    )
                    webSocketService.sendListOfBackups(
                        viewModel.webSocketSession as WebSocketSession,
                        Message.BackupList(viewModel.backups.value)
                    )
                }
                //comparing two lists
                withContext(Dispatchers.IO) {
                    managingResult(viewModel.comparingLists.value)
                    webSocketService.sendListOfBackups(
                        viewModel.webSocketSession as WebSocketSession,
                        Message.BackupList(viewModel.backups.value)
                    )
                }

                // refreshing viewModel.backups.value
                viewModel.loadBackups()
                delay(1000)
                // sending list of backups
                withContext(Dispatchers.IO) {
                    webSocketService.sendListOfBackups(
                        viewModel.webSocketSession as WebSocketSession,
                        Message.BackupList(viewModel.backups.value)
                    )
                }
            }
        }
    }

    override fun turnOffServer() {
        try {
            Log.d("WebSocketSession", "Tapped")
            webSocketService.stopServer()
        }catch (e: IOException){
            Log.d("WebSocketSession", "Nah")
        }
    }
    //defining what to do with the scanned list: save it or not
    private fun managingResult(result: ComparingLists){
        val startLogic = StartLogicScan()
        val dirs = result.dirs
        val oldDirs = result.oldDirs
        val inputValues = viewModel.inputValues.value
        Log.d("NotFoundPackage", dirs.packageName)
        if(dirs.directories != oldDirs){
            val uriText = saveListToInternalStorage(
                context,
                dirs.directories,
                inputValues.appName+dirs.time
            )
            val zipFileUri = startLogic.createTarArchiveWithRoot(
                "/data/data/${dirs.packageName}",
                "${inputValues.appName}_${dirs.time}.tar.gz"
            )
            val newBackup = SingleBackup(
                backupName = inputValues.appName+dirs.time,
                archiveUri = zipFileUri.toString(),
                textUri = uriText
            )
            viewModel.addBackup(
                newBackup
            )
        }else if(dirs.directories.isNotEmpty()){
            Log.d("WorkHere", "else")
            Log.d("Backups", "${viewModel.backups.value}")
        }
    }


    private fun duration(start: Instant, end: Instant): Double{
        val timeElapsed = Duration.between(start, end)
        val substringsToRemove = listOf("PT", "S")
        val regexPattern = substringsToRemove.joinToString(separator = "|") { Regex.escape(it) }
        val outputString = timeElapsed.toString().replace(Regex(regexPattern), "")
        val timeInMills = outputString.toDouble() * 1000
        return timeInMills
    }

    //creating list of scanned files
    private fun readFileFromInternalStorage(
        context: Context,
        appName: String,
        list: List<SingleBackup>
    ): ReadFiles {
        val fileName = createFileName(list, appName)
        Log.d("Name", fileName)
        try {
            val file = File(context.filesDir, fileName)
            val readList = file.readLines()
            return ReadFiles(fileName, readList)
        }catch (e: Exception) {
            Log.e("NameError", "Name is empty or invalid")
            return ReadFiles()
        }
    }

    private fun createFileName(list: List<SingleBackup>, appName: String): String{
        Log.d("CreateName", "appName $appName")
        var fileName = ""
        for(element in list){
            if(element.backupName.contains(Regex(appName))){
                fileName = element.backupName
                Log.d("CreateName", fileName)
                return fileName
            }
        }
        return ""
    }
    //saving text file of folder's scan tree
    private fun saveListToInternalStorage(context: Context, list: List<String>, fileName: String):String {
        val file = File(context.filesDir, fileName)
        try {
            FileWriter(file).use { writer ->
                list.forEach { line ->
                    writer.write(line + System.lineSeparator())
                }
            }

            Log.d("MyLog","File saved successfully!")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("MyLog","Failed to save the file.")
        }
        return Uri.fromFile(file).toString()
    }

    /*function compares two lists: old and new, and forms new list based on these two
        * if size of file changed or it is a new file, corresponding singletonScan is added
        * list1 is list with old files list2 is list with current files*/
    private fun processLists(list1: List<String>, list2: List<String>): List<String> {
        if(list1.isNotEmpty()){
            //mark that adds to changed element (helps to make it purple later)
            val changed = SingletonesForChecking.ScanChanged.value
            //mark that adds to new element (helps to make it green later)
            val new = SingletonesForChecking.ScanNew.value
            val result = mutableListOf<String>()

            val map1 = try {
                list1.associateBy { it.substringAfter("/") }
            }catch (e: Exception){
                emptyMap()
            }
            val map2 = list2.associateBy { it.substringAfter("/") }

            for ((suffix, value2) in map2) {
                val value1 = map1[suffix]
                when {
                    // if values are the same
                    value1 == value2 -> result.add(value2)
                    // if values before "/" different and after "/" are the same
                    value1 != null && value1 != value2 -> result.add("$value2 $changed")
                    // if element does not exist list1
                    value1 == null -> result.add("$value2 $new")
                }
            }
            return result
        }else{
            val resultList = mutableListOf<String>()
            val new = SingletonesForChecking.ScanNew.value
            for (element in list2){
                resultList+=element+new
            }
            return resultList
        }
    }

    private fun runCommandAsRoot() {
        val processBuilder = ProcessBuilder("su", "-c", "ls /data")
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        val exitValue = process.waitFor()
        if (exitValue != 0) {
            throw RuntimeException("Command exited with non-zero status: $exitValue")
        }
    }
}