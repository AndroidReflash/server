package com.example.phonebserver.logic

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.phonebserver.logic.singleTones.SingletonesUnixCommands
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class RecoveryClass {

    fun extractArchive(archivePath: String, context: Context, appName: String): Boolean {
        val packageName = getPackageNameForApp(context, appName)
        killProcessByPackage(packageName)
        val commandExtract = SingletonesUnixCommands.ExtractFiles.command + archivePath
        launchApp(context, packageName)
        return executeCommand(commandExtract)
    }

    private fun executeCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    SingletonesUnixCommands.SuperUser.command,
                    SingletonesUnixCommands.MinusC.command,
                    command
                )
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Logging of command for each extracted file
                Log.d("Config", line.toString())
            }
            val exitCode = process.waitFor()
            Log.d("Config","Command executed with exit code: $exitCode")
            exitCode == 0 // return true if everything is ok
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
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
    private fun killProcessByPackage(packageName: String){
        try {
            // Используем команду pkill для завершения процесса по имени пакета
            val process = Runtime.getRuntime().exec("su -c pkill -f $packageName")
            process.waitFor() // Ждем завершения команды
            val clear = Runtime.getRuntime().exec("pm clear $packageName")
            clear.waitFor()
            val forceStop = Runtime.getRuntime().exec("am force-stop $packageName")
            forceStop.waitFor()
            Log.d("Restoration","Process with package name '$packageName' terminated successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to terminate process with package name '$packageName'.")
        }
    }
    private fun launchApp(context: Context, packageName: String) {
        val pm: PackageManager = context.packageManager
        val launchIntent: Intent? = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            Log.d("Restoration","Application with package name '$packageName' launched successfully.")
        } else {
            Log.d("Restoration","Failed to launch application with package name '$packageName'.")
        }
    }
}