package com.example.phonebserver.logic.singleTones

sealed class SingleToneOfTime(val value: String) {
    object Pattern: SingleToneOfTime(value = "HH-mm-ss_dd-MM-yyyy")
}