package app.lovable.giant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.lovable.giant.ui.auth.LoginScreen
import app.lovable.giant.ui.auth.RegisterScreen
import app.lovable.giant.ui.chats.ChatsListScreen
import app.lovable.giant.ui.chats.DirectChatScreen
import app.lovable.giant.ui.community.CommunityScreen
import app.lovable.giant.ui.profile.ProfileScreen
import app.lovable.giant.ui.rooms.RoomDetailScreen
import app.lovable.giant.ui.rooms.RoomsListScreen
import app.lovable.giant.ui.splash.SplashScreen
import app.lovable.giant.ui.store.StoreScreen
import app.lovable.giant.ui.tasks.DailyTasksScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoomsList : Screen("rooms_list")
    object RoomDetail : Screen("room_detail/{roomId}") {
        fun createRoute(roomId: String) = "room_detail/$roomId"
    }
    object ChatsList : Screen("chats_list")
    object DirectChat : Screen("direct_chat/{otherUserId}") {
        fun createRoute(otherUserId: String) = "direct_chat/$otherUserId"
    }
    object Profile : Screen("profile")
    object Store : Screen("store")
    object DailyTasks : Screen("daily_tasks")
    object Community : Screen("community")
}

@Composable
fun GiantNavGraph(
    navController: NavHostController = rememberNavController(),
    onNavigateToHybridHome: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.RoomsList.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.RoomsList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.RoomsList.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.RoomsList.route) {
            RoomsListScreen(
                onNavigateToRoom = { roomId ->
                    navController.navigate(Screen.RoomDetail.createRoute(roomId))
                },
                onNavigateToChats = {
                    navController.navigate(Screen.ChatsList.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToStore = {
                    navController.navigate(Screen.Store.route)
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.DailyTasks.route)
                },
                onNavigateToCommunity = {
                    navController.navigate(Screen.Community.route)
                }
            )
        }

        composable(Screen.Community.route) {
            CommunityScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Store.route) {
            StoreScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DailyTasks.route) {
            DailyTasksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RoomDetail.route,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            RoomDetailScreen(
                roomId = roomId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ChatsList.route) {
            ChatsListScreen(
                onNavigateToChat = { otherUserId ->
                    navController.navigate(Screen.DirectChat.createRoute(otherUserId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.DirectChat.route,
            arguments = listOf(navArgument("otherUserId") { type = NavType.StringType })
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            DirectChatScreen(
                otherUserId = otherUserId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
