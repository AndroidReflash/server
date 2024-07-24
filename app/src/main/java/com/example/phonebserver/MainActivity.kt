package com.example.phonebserver

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.example.phonebserver.Room.BackupViewModelFactory
import com.example.phonebserver.compose.Navigation
import com.example.phonebserver.logic.MainViewModel
import com.example.phonebserver.ui.theme.PhoneBserverTheme


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by lazy {
        val factory = BackupViewModelFactory(application)
        ViewModelProvider(this, factory).get(MainViewModel::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneBserverTheme {
                Navigation(viewModel = viewModel)
            }
        }
    }
}




