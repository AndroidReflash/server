package com.example.phonebserver.logic.singleTones

sealed class NavigationSingleTones(val route: String) {
    object MainScreen: NavigationSingleTones(route = "main_screen")
    object ConfigScreen: NavigationSingleTones(route = "config_screen")
}