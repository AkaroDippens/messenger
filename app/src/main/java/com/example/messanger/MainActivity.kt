package com.example.messanger

import LoginScreen
import RegistrationScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.messanger.ui.theme.JetchatTheme

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetchatTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route

            if (currentRoute == BottomNavItem.UserList.route || currentRoute == BottomNavItem.FriendRequests.route
                || currentRoute == BottomNavItem.Chats.route) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "registration",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("registration") { RegistrationScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable(BottomNavItem.Chats.route) {
                FriendsListScreen(navController = navController)
            }
            composable(BottomNavItem.UserList.route) {
                UserSearchScreen(navController = navController)
            }
            composable(BottomNavItem.FriendRequests.route) {
                FriendRequestsScreen(navController = navController)
            }
            composable("chat/{friendUid}") { backStackEntry ->
                val friendUid = backStackEntry.arguments?.getString("friendUid")
                if (friendUid != null) {
                    ChatScreen(navController, friendUid)
                }
            }
        }
    }
}
