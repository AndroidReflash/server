package com.example.phonebserver.compose

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.phonebserver.KtorWebsocket.Message
import com.example.phonebserver.KtorWebsocket.WebSocketService
import com.example.phonebserver.logic.ConstantsForUI.MainActivityValuesForUI
import com.example.phonebserver.logic.FileTreeLogic
import com.example.phonebserver.logic.MainActivityLogic
import com.example.phonebserver.logic.MainViewModel
import com.example.phonebserver.logic.dataClasses.InputValues
import com.example.phonebserver.logic.singleTones.NavigationSingleTones
import com.example.phonebserver.logic.singleTones.SingletoneDelay
import io.ktor.server.engine.ApplicationEngine
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val ui = MainActivityValuesForUI()
    val webSocketService = WebSocketService(viewModel)
    val scanResult = viewModel.scanResult.collectAsState()
    val main = MainActivityLogic(context, viewModel, navController)
    var pack by remember {
        mutableStateOf("")
    }
    val list1 = listOf("")
    var memoryList by remember {
        mutableStateOf(list1)
    }
    pack = scanResult.value.packageName
    viewModel.loadBackups()
    Permissions(context)
    if (pack != "") {
        LaunchedEffect(pack) {
            viewModel.viewModelScope.coroutineContext.cancelChildren()
            viewModel.viewModelScope.launch {
                while (true) {
                    if (viewModel.webSocketSession == null) {
                        Log.d("WebSocketSession", "WebSocket session is null, stopping the loop")
                        break
                    }

                    val memory = FileTreeLogic()
                    memoryList = memory.scanApplicationMemory(
                        scanResult.value.packageName
                    )
                    webSocketService.ram(Message.Memory(memoryList), viewModel.webSocketSession as WebSocketSession)
                    delay(SingletoneDelay.Delay.value)
                    if (scanResult.value.packageName != pack) {
                        break
                    }
                }
            }
            pack = scanResult.value.packageName
        }
    }
    Column(modifier = Modifier.fillMaxSize()
        , horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(5.dp),
            Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                main.config()
            }) {
                Text(text = ui.config)
            }

            Button(onClick = {
                viewModel.viewModelScope.launch {
                    main.turnOnServer()
                }
            }) {
                Text(text = ui.turnOn)
            }
            Button(onClick = {
                main.turnOffServer()
            }) {
                Text(text = ui.turnOff)
            }
        }
    }
}