package com.example.phonebserver.KtorWebsocket


import android.content.Context
import kotlinx.coroutines.*
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.phonebserver.logic.MainViewModel
import com.example.phonebserver.logic.dataClasses.InputValues
import com.example.phonebserver.logic.singleTones.SingletonesForActivating
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Duration




class WebSocketService(
    private val viewModel: MainViewModel
) {
    private var server: ApplicationEngine? = null
    private val newIp = "${viewModel.ip.value.host}:${viewModel.ip.value.port}"
    private var webSocketSession: WebSocketSession? = null
    private val sessionMutex = Mutex()
    private var scanJob: Job? = null
    private var shouldContinueScanning = false
    fun createServer(context: Context, startScan: () -> Unit) {

        // Server initialization
        server = embeddedServer(Jetty, host = viewModel.ip.value.host, port = viewModel.ip.value.port) {
            install(ContentNegotiation) {
                json()
            }
            install(WebSockets) {
                pingPeriod = Duration.ofMinutes(1)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket("/ws") {
                    Log.d("WebSocket", "Client connected: $this")

                    sessionMutex.withLock {
                        webSocketSession = this
                        viewModel.webSocketSession = webSocketSession
                    }

                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    val messageContainer = Json.decodeFromString<MessageContainer>(text)
                                    val message: Message = when (messageContainer.type) {
                                        "Scan" -> Json.decodeFromString<Message.Scan>(messageContainer.payload)
                                        "RoomId" -> Json.decodeFromString<Message.RoomId>(messageContainer.payload)
                                        "Memory" -> Json.decodeFromString<Message.Memory>(messageContainer.payload)
                                        "ScanResult" -> Json.decodeFromString<Message.ScanResult>(messageContainer.payload)
                                        "CloseSession" -> Json.decodeFromString<Message.CloseSession>(messageContainer.payload)
                                        else -> throw IllegalArgumentException("Unknown message type")
                                    }

                                    when (message) {
                                        is Message.Scan -> {
                                            when (message.singletonScan) {
                                                SingletonesForActivating.StartScan.value -> {
                                                    if (scanJob == null || !scanJob!!.isActive) {
                                                        shouldContinueScanning = true
                                                        scanJob = viewModel.viewModelScope.launch {
                                                            while (shouldContinueScanning) {
                                                                handleScanMessage(message, startScan)
                                                                delay(message.delay.toLong() * 1000)
                                                            }
                                                        }
                                                    }
                                                }
                                                SingletonesForActivating.StopScan.value -> {
                                                    shouldContinueScanning = false
                                                    scanJob?.cancel()
                                                    scanJob = null
                                                }
                                            }
                                        }
                                        is Message.RoomId -> {
                                            restore(message, context)
                                        }
                                        is Message.Memory -> {
                                            // Handle Memory message
                                        }
                                        is Message.ScanResult -> {
                                            viewModel.updateScanResult(message)
                                        }
                                        is Message.RestoreResult -> TODO()
                                        is Message.BackupList -> TODO()
                                        is Message.CloseSession -> TODO()
                                    }
                                }
                                is Frame.Binary -> TODO()
                                is Frame.Close -> {
                                    sessionMutex.withLock {
                                        webSocketSession = null
                                    }
                                }
                                is Frame.Ping -> {
                                    send(Frame.Pong(frame.data))
                                }
                                is Frame.Pong -> Log.d("WebSocket", "Pong received")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("WebSocket", "Error: ${e.localizedMessage}")
                    } finally {
                        sessionMutex.withLock {
                            webSocketSession = null
                        }
                    }
                }
            }
        }

        // Запуск сервера с проверкой, запущен ли он уже Launching server with already started check
        try {
            server?.start(wait = false)
            Log.d("WebSocket", "Server created at: ${viewModel.ip.value}")
        } catch (e: IOException) {
            Log.d("WebSocket", "Server already running")
        }
    }

    private suspend fun handleScanMessage(message: Message.Scan, startScan: () -> Unit) {
        val action = message.singletonScan
        if (action == SingletonesForActivating.StartScan.value) {
            val lookingForList = viewModel.backups.value.reversed()
            var oldFileName = ""
            if (lookingForList.isNotEmpty()) {
                Log.d("WebSocketName", message.appName)
                for (element in lookingForList.reversed()) {
                    if (element.backupName.contains(Regex(message.appName))) {
                        delay(1000)
                        viewModel.updateInputValue(
                            InputValues(
                                message.appName,
                                element.backupName
                            )
                        )
                        oldFileName = element.backupName
                    }
                }
            }
            if (oldFileName == "") {
                viewModel.updateInputValue(
                    InputValues(
                        message.appName,
                        ""
                    )
                )
            }
            Log.d("WebSocket", "Message ScanIf")
            Log.d("WebSocketCor", "Scanning")
            startScan()
        } else if (action == SingletonesForActivating.StopScan.value) {
            scanJob?.cancel()
            scanJob = null
        }
    }

    fun stopServer() {
        runBlocking {
            try {
                Log.d("WebSocketSession", "Attempting to stop the server")
                Log.d("WebSocketSession", "$webSocketSession")

                // Checking active sessions
                try {
                    // Closing session message out of block
                    webSocketSession?.send(Frame.Text(Json.encodeToString(Message.CloseSession("Server stopping"))))
                    delay(2000) // Ожидание, чтобы сообщение успело отправиться

                    // Closing session out of block
                    webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Server stopping"))
                    Log.d("WebSocketSession", "WebSocket session closed")
                } catch (e: Exception) {
                    Log.e("WebSocketSession", "Failed to close WebSocket session: ${e.localizedMessage}")
                } finally {
                    // WebSocketSession is null inside block
                    sessionMutex.withLock {
                        webSocketSession = null
                        viewModel.webSocketSession = null
                    }
                }

                // Server stop
                server?.stop(gracePeriodMillis = 100, timeoutMillis = 1000)
                Log.d("WebSocketSession", "Server stopped on $newIp")

                server = null
            } catch (e: Exception) {
                Log.e("WebSocketSession", "Failed to stop the server: ${e.localizedMessage}")
            }
        }
    }

    //To send different answers
    private suspend fun sendMessage(messageContainer: MessageContainer, webSocketSession: WebSocketSession) {
        sessionMutex.withLock {
            webSocketSession.send(Frame.Text(Json.encodeToString(messageContainer)))
        }
    }

    fun ram(ram: Message.Memory, webSocketSession: WebSocketSession) {
        runBlocking {
            val containerRam = MessageContainer("Memory", Json.encodeToString(ram))
            sendMessage(containerRam, webSocketSession)
        }
    }

    fun sendScanResult(webSocketSession: WebSocketSession, result: Message.ScanResult) {
        runBlocking {
            Log.d("WebSocket", "Sending scan result: $result")
            val container = MessageContainer("ScanResult", Json.encodeToString(result))
            sendMessage(container, webSocketSession)
        }
    }

    fun sendListOfBackups(webSocketSession: WebSocketSession, result: Message.BackupList) {
        runBlocking {
            Log.d("WebSocket", "Sending BackupList: $result")
            val container = MessageContainer("BackupList", Json.encodeToString(result))
            sendMessage(container, webSocketSession)
        }
    }

    //restoring packages
    private fun restore(message: Message.RoomId, context: Context){
        viewModel.loadBackups()
        val listOfBackups = viewModel.backups.value
        for (it in listOfBackups) {
            if (it.id == message.id) {
                viewModel.viewModelScope.launch {
                    viewModel.restoreRepository(
                        it.archiveUri.substringAfter("file:"),
                        it.backupName.substring(0, it.backupName.length - 19),
                        context
                    )
                }
            }
        }
    }
}