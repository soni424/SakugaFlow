package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.OfflineScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.PopularDetailScreen
import com.example.ui.screens.TagPostsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakugaApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: SakugaViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isTopLevelRoute = currentRoute == "home" || currentRoute == "explore" || currentRoute == "saved" || currentRoute == "offline" || currentRoute == "settings"

    // Collect theme setting dynamically from viewModel
    val themeState by viewModel.themeMode.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = when (themeState) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    com.example.ui.theme.MyApplicationTheme(darkTheme = useDarkTheme) {
        Scaffold(
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            Text(
                                "SakugaFlow",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { /* Home redirect */ }) {
                                Icon(
                                    imageVector = Icons.Default.MotionPhotosOn,
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                navController.navigate("offline") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Downloads"
                                )
                            }
                            IconButton(onClick = {
                                navController.navigate("settings") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Settings"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
                            label = { Text("Feed") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "explore",
                            onClick = {
                                navController.navigate("explore") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                            label = { Text("Explore") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "saved",
                            onClick = {
                                navController.navigate("saved") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Saved") },
                            label = { Text("Saved") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "offline",
                            onClick = {
                                navController.navigate("offline") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.DownloadDone, contentDescription = "Offline") },
                            label = { Text("Offline") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                navController.navigate("settings") {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSaved = { navController.navigate("saved") },
                        onPostClick = { post -> navController.navigate("detail/${post.id}") }
                    )
                }
                composable("saved") {
                    SavedScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPostClick = { post -> navController.navigate("detail/${post.id}") }
                    )
                }
                composable("offline") {
                    OfflineScreen(
                        viewModel = viewModel,
                        onPostClick = { post -> navController.navigate("detail/${post.id}") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPostClick = { post -> navController.navigate("detail/${post.id}") }
                    )
                }
                composable("explore") {
                    ExploreScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { 
                            navController.navigate("home")
                        },
                        onPostClick = { post -> navController.navigate("detail/${post.id}") },
                        onNavigateToTimeframe = { timeframe -> navController.navigate("popular_detail/$timeframe") },
                        onNavigateToTag = { tag -> navController.navigate("tag_posts/$tag") }
                    )
                }
                composable("popular_detail/{timeframe}") { backStackEntry ->
                    val timeframe = backStackEntry.arguments?.getString("timeframe") ?: "day"
                    PopularDetailScreen(
                        viewModel = viewModel,
                        timeframe = timeframe,
                        onBackClick = { navController.popBackStack() },
                        onPostClick = { post -> navController.navigate("detail/${post.id}") }
                    )
                }
                composable("tag_posts/{tag}") { backStackEntry ->
                    val tag = backStackEntry.arguments?.getString("tag") ?: ""
                    TagPostsScreen(
                        viewModel = viewModel,
                        tag = tag,
                        onBackClick = { navController.popBackStack() },
                        onPostClick = { post -> navController.navigate("detail/${post.id}") }
                    )
                }
                composable("detail/{postId}") { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull()
                    if (postId != null) {
                        DetailScreen(
                            postId = postId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onPostClick = { id -> navController.navigate("detail/$id") }
                        )
                    }
                }
            }
        }
    }
}
