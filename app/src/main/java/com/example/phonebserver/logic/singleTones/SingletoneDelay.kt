package com.example.phonebserver.logic.singleTones

sealed class SingletoneDelay(val value: Long) {
    object Delay: SingletoneDelay(value = 100)
    object DelayParameterT: SingletoneDelay(value = 5000)
}