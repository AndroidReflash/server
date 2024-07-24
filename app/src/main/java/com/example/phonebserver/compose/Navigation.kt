package com.example.phonebserver.compose

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable

import androidx.navigation.Navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.phonebserver.logic.MainViewModel
import com.example.phonebserver.logic.singleTones.NavigationSingleTones

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Navigation(viewModel: MainViewModel){
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavigationSingleTones.MainScreen.route
    ){
        composable(route = NavigationSingleTones.MainScreen.route){
            MainScreen(viewModel = viewModel, navController)
        }
        composable(route = NavigationSingleTones.ConfigScreen.route){
            ConfigScreen(viewModel = viewModel, navController)
        }
    }
}