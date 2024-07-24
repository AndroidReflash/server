package com.example.phonebserver.interfaces

import android.content.Context
import com.example.phonebserver.logic.MainViewModel
import com.example.phonebserver.logic.dataClasses.ComparingLists

interface MainActivityInterface {
    fun start()
    fun config()
    fun turnOnServer()
    fun turnOffServer()
}

