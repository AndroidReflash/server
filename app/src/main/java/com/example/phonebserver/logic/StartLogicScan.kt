package com.example.phonebserver.logic

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.phonebserver.KtorWebsocket.Message
import com.example.phonebserver.logic.singleTones.SingleToneOfTime
import com.example.phonebserver.logic.singleTones.SingletonesForChecking
import com.example.phonebserver.logic.singleTones.SingletonesUnixCommands

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StartLogicScan {
    //getting package name by packageManager and name of application
    private fun getPackageNameForApp(context: Context, appName: String): String {
        val packageManager: PackageManager = context.packageManager
        val packages: List<ApplicationInfo> =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in packages) {
            val label = packageManager.getApplicationLabel(app).toString()
            if (label == appName) {
                return app.packageName
            }
        }
        return "Not Found"
    }

    //achieving root access for command forming folder's scan tree
    private fun executeRootCommand(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    SingletonesUnixCommands.SuperUser.command,
                    SingletonesUnixCommands.MinusC.command,
                    command
                )
            )
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String? = bufferedReader.readLine()
            while (line != null) {
                output.add(line)
                Log.d("WebSocket", "Scanning")
                line = bufferedReader.readLine()
            }

            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var errorLine: String? = errorReader.readLine()
            while (errorLine != null) {
                output.add(errorLine)
                Log.d("WebSocket", "ErrorLine")
                errorLine = errorReader.readLine()
            }

            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            output.add(e.message ?: "Error executing command")
        }
        return output
    }

    //forming folder's scan tree with time, size and packageName
    fun scanApplicationFiles(appName: String, context: Context): Message.ScanResult {
        val time = time()
        val scanList = SingletonesUnixCommands.ScanList.command
        val memory = SingletonesUnixCommands.MemoryForEach.command
        val packageName = getPackageNameForApp(context, appName)
        //fileTree scan
        val memoryForEach = SingletonesUnixCommands.MemoryForEachCatalog.command
        val fileTreeScan = scanList + packageName + memory
        //fileTree scan with sizes of directories
        val fileTreeScanWithSizes = scanList + packageName + memoryForEach
        val directories = executeRootCommand(fileTreeScan)
        val size = executeRootCommand(fileTreeScanWithSizes)
        return Message.ScanResult(
            //fileTree with sizes for each file in directory
            directories,
            //whole size of folders
            size[0],
            //time when scan started
            time,
            packageName = packageName,
        )
    }

    private fun time(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(SingleToneOfTime.Pattern.value)
        val formattedDateTime = currentDateTime.format(formatter)
        return formattedDateTime
    }

    fun createTarArchiveWithRoot(filesPath: String, archiveName: String): String? {
        val archiveDir = SingletonesForChecking.ArchivesDir.value
        val archivePath = File(archiveDir, archiveName).absolutePath
        try {
            // Forming tar command with root
            val command = "su -c \"tar -czf $archivePath $filesPath\""
            Log.d("ArchiveCreation", "Executing command: $command")

            // Command executing using Runtime.getRuntime().exec()
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitValue = process.waitFor()
            Log.d("ArchiveCreation", "Command executed with exit value: $exitValue")

            if (exitValue == 0) {
                // Getting the size of archive
                val archiveFile = File(archivePath)
                val archiveSize = archiveFile.length()
                Log.d("ArchiveCreation", "Created archive size: $archiveSize bytes")

                // return URI of created archive
                return archiveFile.toURI().toString()
            } else {
                Log.d("ArchiveCreation", "Failed to create archive, exit value: $exitValue")
                return null
            }
        } catch (e: Exception) {
            Log.e("ArchiveCreation", "Exception occurred: ${e.message}")
            return null
        }
    }
}