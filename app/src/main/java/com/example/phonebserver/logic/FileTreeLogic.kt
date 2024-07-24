package com.example.phonebserver.logic

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.phonebserver.logic.singleTones.SingletonesUnixCommands
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader


//  M1 and Mmax
class FileTreeLogic {
    private fun executeRootCommand(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String? = bufferedReader.readLine()
            while (line != null) {
                output.add(line)
                line = bufferedReader.readLine()
            }

            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var errorLine: String? = errorReader.readLine()
            while (errorLine != null) {
                output.add(errorLine)
                errorLine = errorReader.readLine()
            }

            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            output.add(e.message ?: "Error executing command")
        }
        return output
    }
    private fun reduceMultipleSpacesToSingle(input: String): String {
        return input.replace(Regex("\\s+"), " ")
    }
    private fun extractMemoryInfo(processInfo: String): String {
        val elements = processInfo.split(" ").filter { it.isNotEmpty() }
        var result = ""
        if (elements.size >= 5) {
            val vsz = elements[3] + " KB" + " (Mmax)"
            val rss = elements[4] + " KB" + " (M1)"
            result+=rss
            result+=" / "
            result+=vsz
            return result
        } else {
            return "null"
        }
    }
    fun scanApplicationMemory(packageName: String): List<String> {
        val extraCommand1 = SingletonesUnixCommands.MemoryInfo.command + packageName
        //fileTree scan with sizes of directories
        val directories = executeRootCommand(extraCommand1)
        val result = mutableListOf<String>()
        directories.forEach {
            val element=reduceMultipleSpacesToSingle(it)
            result+=extractMemoryInfo(element)
        }
        Log.d("MyLogMemory", "$result")
        return result
    }
}