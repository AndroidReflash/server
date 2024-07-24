package com.example.phonebserver.logic

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phonebserver.KtorWebsocket.Message
import com.example.phonebserver.Room.BackupRepository
import com.example.phonebserver.Room.SingleBackup
import com.example.phonebserver.logic.dataClasses.ComparingLists
import com.example.phonebserver.logic.dataClasses.Host
import com.example.phonebserver.logic.dataClasses.InputValues
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.time.Duration
import java.time.Instant

class MainViewModel(private val repository: BackupRepository): ViewModel() {

    private var _webSocketSession: WeakReference<WebSocketSession>? = null

    var webSocketSession: WebSocketSession?
        get() = _webSocketSession?.get()
        set(value) {
            _webSocketSession = if (value != null) WeakReference(value) else null
        }

    private val _inputValues = MutableStateFlow(InputValues())
    val inputValues: StateFlow<InputValues> = _inputValues

    fun updateInputValue(newValue: InputValues){
        _inputValues.value = newValue
    }

    private val _comparingLists = MutableStateFlow(ComparingLists())
    val comparingLists: StateFlow<ComparingLists> = _comparingLists

    fun updateComparingLists(newValue: ComparingLists){
        _comparingLists.value = newValue
    }


    private val _scanResult = MutableStateFlow(Message.ScanResult())
    val scanResult: StateFlow<Message.ScanResult> = _scanResult

    fun updateScanResult(newValue: Message.ScanResult): Message.ScanResult{
        _scanResult.value = newValue
        return newValue
    }

    private val _backups = MutableStateFlow<List<SingleBackup>>(emptyList())
    val backups: StateFlow<List<SingleBackup>> = _backups

    fun loadBackups() {
        viewModelScope.launch {
            repository.getSingleBackupsByBackupName().collect { backupList ->
                _backups.value = backupList
            }
        }
    }

    fun addBackup(backup: SingleBackup) {
        viewModelScope.launch {
            repository.insertBackup(backup)
            _backups.value += backup
        }
    }

    fun restoreRepository(path:String, appName: String, context: Context){
        val recovery = RecoveryClass()
        viewModelScope.launch(Dispatchers.IO) {
            val start = Instant.now()
            val result  = recovery.extractArchive(
                archivePath = path, context, appName
            )
            val end = Instant.now()
            val scanDuration = duration(start, end).toString()
            if(result){
                Log.d("Restoration", "Восстановление завершено")
                Log.d("Restoration", "Дата и время восттановления: $start")
                Log.d("Restoration", "Заняло времени $scanDuration мс")
                withContext(Dispatchers.Main){
                    Toast.makeText(context, "Восстановление завершено", Toast.LENGTH_LONG).show()
                    Toast.makeText(context, "Дата и время восттановления: $start", Toast.LENGTH_LONG).show()
                    Toast.makeText(context, "Заняло времени $scanDuration мс", Toast.LENGTH_LONG).show()
                }
            }else{
                Log.d("Restoration", "Ошибка")
                Toast.makeText(context, "Ошибка", Toast.LENGTH_LONG).show()
            }
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

    private val _ip = MutableStateFlow(Host())
    val ip: StateFlow<Host> = _ip

    fun updateIp(newValue: Host){
        _ip.value = newValue
    }

    override fun onCleared() {
        super.onCleared()
        webSocketSession = null
    }
}