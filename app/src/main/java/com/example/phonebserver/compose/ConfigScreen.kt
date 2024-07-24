package com.example.phonebserver.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.phonebserver.logic.ConstantsForUI.MainActivityValuesForUI
import com.example.phonebserver.logic.MainViewModel
import com.example.phonebserver.logic.dataClasses.Host
import com.example.phonebserver.logic.singleTones.NavigationSingleTones

@Composable
fun ConfigScreen(viewModel: MainViewModel, navController: NavController){
    val ui = MainActivityValuesForUI()
    var host by remember {
        mutableStateOf("")
    }
    var port by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = ui.typeSomething, Modifier.padding(5.dp))
            TextField(value = host, onValueChange = {host = it}, Modifier.padding(5.dp))
            Text(text = ui.serverPort, Modifier.padding(5.dp))
            TextField(value = port, onValueChange = {port = it}, Modifier.padding(5.dp))
            Button(onClick = {
                if(host != "" && port != ""){
                    viewModel.updateIp(
                        Host(
                            host,
                            port.toInt()
                        )
                    )
                    navController.navigate(NavigationSingleTones.MainScreen.route)
                }else{
                    Toast.makeText(context, "Нужно заполнить все поля", Toast.LENGTH_LONG).show()
                }
            }) {
                Text(text = "Сохранить")
            }
        }
    }
}