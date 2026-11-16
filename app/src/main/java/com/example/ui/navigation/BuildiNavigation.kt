package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.repository.BuildRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.ProfileAccountDialog
import com.example.ui.detail.ProjectDetailScreen
import com.example.ui.detail.ProjectDetailViewModel
import com.example.ui.projects.ProjectsScreen
import com.example.ui.projects.ProjectsViewModel
import com.example.ui.tools.BuilderToolsScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_PROJECTS = "projects"
const val ROUTE_TOOLS = "tools"
const val ROUTE_PROJECT_DETAIL = "project/{projectId}"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    val uid = user.uid
    key(uid) {
      BuildiAuthenticatedShell(
        currentUserId = uid,
        displayName = user.displayName,
        email = user.email,
      )
    }
  }
}

@Composable
private fun BuildiAuthenticatedShell(
  currentUserId: String,
  displayName: String?,
  email: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val isTopLevelRoute =
    currentDestination?.route == ROUTE_PROJECTS || currentDestination?.route == ROUTE_TOOLS

  var showAccountDialog by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        if (isTopLevelRoute && !useNavRail) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            val isProjectsSelected =
              currentDestination?.hierarchy?.any { it.route == ROUTE_PROJECTS } == true
            val isToolsSelected =
              currentDestination?.hierarchy?.any { it.route == ROUTE_TOOLS } == true

            NavigationBarItem(
              selected = isProjectsSelected,
              onClick = {
                navController.navigate(ROUTE_PROJECTS) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector =
                    if (isProjectsSelected) Icons.Filled.Architecture else Icons.Outlined.Architecture,
                  contentDescription = stringResource(R.string.nav_projects),
                )
              },
              label = { Text(stringResource(R.string.nav_projects)) },
              modifier = Modifier.testTag("nav_projects"),
            )

            NavigationBarItem(
              selected = isToolsSelected,
              onClick = {
                navController.navigate(ROUTE_TOOLS) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector =
                    if (isToolsSelected) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                  contentDescription = stringResource(R.string.nav_tools),
                )
              },
              label = { Text(stringResource(R.string.nav_tools)) },
              modifier = Modifier.testTag("nav_tools"),
            )
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isTopLevelRoute && useNavRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            val isProjectsSelected =
              currentDestination?.hierarchy?.any { it.route == ROUTE_PROJECTS } == true
            val isToolsSelected =
              currentDestination?.hierarchy?.any { it.route == ROUTE_TOOLS } == true

            NavigationRailItem(
              selected = isProjectsSelected,
              onClick = {
                navController.navigate(ROUTE_PROJECTS) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector =
                    if (isProjectsSelected) Icons.Filled.Architecture else Icons.Outlined.Architecture,
                  contentDescription = stringResource(R.string.nav_projects),
                )
              },
              label = { Text(stringResource(R.string.nav_projects)) },
            )
            NavigationRailItem(
              selected = isToolsSelected,
              onClick = {
                navController.navigate(ROUTE_TOOLS) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector =
                    if (isToolsSelected) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                  contentDescription = stringResource(R.string.nav_tools),
                )
              },
              label = { Text(stringResource(R.string.nav_tools)) },
            )
          }
        }

        NavHost(
          navController = navController,
          startDestination = ROUTE_PROJECTS,
          modifier = Modifier.weight(1f),
        ) {
          composable(ROUTE_PROJECTS) {
            val projectsViewModel: ProjectsViewModel =
              viewModel(
                key = "projects_$currentUserId",
                factory =
                  viewModelFactory {
                    initializer {
                      val app = checkNotNull(this[APPLICATION_KEY])
                      val databaseId = app.getString(R.string.firestore_database_id)
                      val db = FirebaseFirestore.getInstance(databaseId)
                      ProjectsViewModel(BuildRepository(db), currentUserId)
                    }
                  },
              )
            ProjectsScreen(
              viewModel = projectsViewModel,
              onOpenProject = { projectId -> navController.navigate("project/$projectId") },
              onOpenAccountDialog = { showAccountDialog = true },
            )
          }

          composable(ROUTE_TOOLS) {
            BuilderToolsScreen(onOpenAccountDialog = { showAccountDialog = true })
          }

          composable(
            route = ROUTE_PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
          ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val detailViewModel: ProjectDetailViewModel =
              viewModel(
                key = "detail_${currentUserId}_$projectId",
                factory =
                  viewModelFactory {
                    initializer {
                      val app = checkNotNull(this[APPLICATION_KEY])
                      val databaseId = app.getString(R.string.firestore_database_id)
                      val db = FirebaseFirestore.getInstance(databaseId)
                      ProjectDetailViewModel(BuildRepository(db), currentUserId, projectId)
                    }
                  },
              )
            ProjectDetailScreen(
              viewModel = detailViewModel,
              onNavigateBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }

  if (showAccountDialog) {
    ProfileAccountDialog(
      displayName = displayName,
      email = email,
      onDismiss = { showAccountDialog = false },
      onSignOut = {
        showAccountDialog = false
        signOut(credentialManager = credentialManager, scope = scope)
      },
    )
  }
}
