package com.raqeem.app.presentation.navigation

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raqeem.app.domain.model.SyncStatus
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.screens.accounts.AccountDetailScreen
import com.raqeem.app.presentation.ui.screens.accounts.AccountsScreen
import com.raqeem.app.presentation.ui.screens.analytics.AiChatScreen
import com.raqeem.app.presentation.ui.screens.analytics.AnalyticsScreen
import com.raqeem.app.presentation.ui.screens.auth.AuthScreen
import com.raqeem.app.presentation.ui.screens.budgets.BudgetsScreen
import com.raqeem.app.presentation.ui.screens.goals.GoalsScreen
import com.raqeem.app.presentation.ui.screens.home.HomeScreen
import com.raqeem.app.presentation.ui.screens.lock.UnlockScreen
import com.raqeem.app.presentation.ui.screens.settings.SettingsScreen
import com.raqeem.app.presentation.ui.screens.transaction.QuickAddBottomSheet
import com.raqeem.app.presentation.ui.screens.transaction.QuickAddContext
import com.raqeem.app.presentation.ui.screens.transaction.TransactionDetailScreen
import com.raqeem.app.presentation.ui.screens.transaction.TransactionsScreen
import com.raqeem.app.presentation.ui.theme.AppColors
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import com.raqeem.app.R
import kotlinx.coroutines.launch

@Composable
fun RaqeemNavHost(
    viewModel: AppRootViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when {
        uiState.isBootstrapping -> LoadingGate()
        uiState.authSession == null -> AuthScreen(
            message = uiState.authError,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onResetPassword = viewModel::resetPassword,
        )
        uiState.unlockState.isLocked -> UnlockScreen(
            isBiometricEnabled = uiState.unlockState.isBiometricEnabled,
            hasPin = uiState.unlockState.hasPin,
            message = uiState.authError,
            onUnlockWithBiometric = viewModel::unlockWithBiometric,
            onVerifyPin = { pin ->
                viewModel.verifyPin(pin) {}
            },
            onSignOut = viewModel::signOut,
        )
        else -> MainShell(
            syncStatus = uiState.syncStatus,
            onSyncNow = viewModel::triggerSync,
            onSignOut = viewModel::signOut,
            onSetLockOnLaunchEnabled = viewModel::setLockOnLaunch,
            onSetBiometricEnabled = viewModel::setBiometricEnabled,
            onSetPin = viewModel::setPin,
            onClearPin = viewModel::clearPin,
        )
    }
}

@Composable
private fun LoadingGate() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified
            )
            SurfaceCard {
                Text(
                    text = "Restoring your session and syncing local finance data...",
                    color = AppColors.textSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainShell(
    syncStatus: SyncStatus,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onSetLockOnLaunchEnabled: (Boolean) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "main_pager"
    val isTopLevelRoute = currentRoute == "main_pager"
    var quickAddContext by rememberSaveable { mutableStateOf(QuickAddContext.Overview) }
    var pendingGoalFundingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showQuickAddSheet by rememberSaveable { mutableStateOf(false) }
    var showSyncDetails by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { RaqeemDestination.topLevelDestinations.size })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isTopLevelRoute && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.bgBase,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (isTopLevelRoute) {
                FloatingActionButton(
                    onClick = {
                        quickAddContext = QuickAddContext.Overview
                        pendingGoalFundingId = null
                        showQuickAddSheet = true
                    },
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Quick add",
                    )
                }
            }
        },
        bottomBar = {
            if (isTopLevelRoute) {
                RaqeemBottomBar(
                    selectedIndex = pagerState.currentPage,
                    onNavigate = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppColors.bgBase),
        ) {
            NavHost(
                navController = navController,
                startDestination = "main_pager",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { androidx.compose.animation.EnterTransition.None },
                exitTransition = { androidx.compose.animation.ExitTransition.None },
            ) {
                composable("main_pager") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondBoundsPageCount = 1,
                    ) { page ->
                        key(page) {
                            when (page) {
                            0 -> HomeScreen(
                                syncStatus = syncStatus,
                                onSyncStatusClick = { showSyncDetails = true },
                                onOpenSettings = { navController.navigate(RaqeemDestination.Settings.route) },
                                onViewAllTransactions = {
                                    navController.navigate(RaqeemDestination.Transactions.route)
                                },
                                onOpenTransaction = { transactionId ->
                                    navController.navigate(RaqeemDestination.TransactionDetail.createRoute(transactionId))
                                },
                                onAddExpense = {
                                    quickAddContext = QuickAddContext.Expense
                                    pendingGoalFundingId = null
                                    showQuickAddSheet = true
                                },
                                onAddIncome = {
                                    quickAddContext = QuickAddContext.Income
                                    pendingGoalFundingId = null
                                    showQuickAddSheet = true
                                },
                            )
                            1 -> AccountsScreen(
                                onOpenSettings = { navController.navigate(RaqeemDestination.Settings.route) },
                                onOpenAccount = { accountId ->
                                    navController.navigate(RaqeemDestination.AccountDetail.createRoute(accountId))
                                },
                            )
                            2 -> AnalyticsScreen(
                                onOpenSettings = { navController.navigate(RaqeemDestination.Settings.route) },
                                onOpenChat = { month ->
                                    navController.navigate(RaqeemDestination.AnalyticsChat.createRoute(month))
                                },
                            )
                            3 -> BudgetsScreen(
                                onOpenSettings = { navController.navigate(RaqeemDestination.Settings.route) },
                            )
                            4 -> GoalsScreen(
                                onOpenSettings = { navController.navigate(RaqeemDestination.Settings.route) },
                                onAddFunds = { goalId ->
                                    quickAddContext = QuickAddContext.Transfer
                                    pendingGoalFundingId = goalId
                                    showQuickAddSheet = true
                                },
                            )
                        }
                    }
                }
                }
                composable(RaqeemDestination.Transactions.route) {
                    TransactionsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTransaction = { transactionId ->
                            navController.navigate(RaqeemDestination.TransactionDetail.createRoute(transactionId))
                        },
                    )
                }
                composable(RaqeemDestination.TransactionDetail.route) { detailEntry ->
                    val transactionId = detailEntry.arguments?.getString("transactionId").orEmpty()
                    TransactionDetailScreen(
                        transactionId = transactionId,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(RaqeemDestination.AccountDetail.route) { detailEntry ->
                    val accountId = detailEntry.arguments?.getString("accountId").orEmpty()
                    AccountDetailScreen(
                        accountId = accountId,
                        onBack = { navController.popBackStack() },
                        onOpenTransaction = { transactionId ->
                            navController.navigate(RaqeemDestination.TransactionDetail.createRoute(transactionId))
                        },
                    )
                }

                composable(RaqeemDestination.AnalyticsChat.route) { entry ->
                    AiChatScreen(
                        initialMonth = entry.arguments?.getString("month"),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(RaqeemDestination.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onSignOut = onSignOut,
                        onSetLockOnLaunchEnabled = onSetLockOnLaunchEnabled,
                        onSetBiometricEnabled = onSetBiometricEnabled,
                        onSetPin = onSetPin,
                        onClearPin = onClearPin,
                    )
                }
            }

            if (showQuickAddSheet) {
                QuickAddBottomSheet(
                    initialContext = quickAddContext,
                    initialGoalId = pendingGoalFundingId,
                    onDismiss = {
                        showQuickAddSheet = false
                        pendingGoalFundingId = null
                    },
                )
            }

            if (showSyncDetails) {
                SyncStatusDetailsSheet(
                    syncStatus = syncStatus,
                    onDismiss = { showSyncDetails = false },
                    onSyncNow = onSyncNow,
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncStatusDetailsSheet(
    syncStatus: SyncStatus,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Sync Status",
                style = MaterialTheme.typography.headlineMedium,
            )
            when (syncStatus) {
                SyncStatus.Idle -> Text("Sync is standing by. We will upload local changes the next time a sync runs.")
                SyncStatus.Offline -> Text("You are offline right now. Your changes stay on this device until connectivity returns.")
                SyncStatus.Syncing -> Text("Sync is running now and reconciling local changes with the cloud.")
                is SyncStatus.Failed -> Text(
                    if (syncStatus.failedCount > 0) {
                        "${syncStatus.failedCount} ${if (syncStatus.failedCount == 1) "change" else "changes"} still need attention. Last error: ${syncStatus.message}"
                    } else {
                        "Last sync failed: ${syncStatus.message}"
                    }
                )
                is SyncStatus.Synced -> Text(
                    buildString {
                        append("Last sync completed ")
                        append(formatRelativeSyncTime(syncStatus.lastSyncAtMillis))
                        append(".")
                        if (syncStatus.pendingCount > 0) {
                            append(" ")
                            append(syncStatus.pendingCount)
                            append(" ${if (syncStatus.pendingCount == 1) "change is" else "changes are"} still queued.")
                        } else {
                            append(" Everything is backed up.")
                        }
                        if (syncStatus.failedCount > 0) {
                            append(" ")
                            append(syncStatus.failedCount)
                            append(" ${if (syncStatus.failedCount == 1) "item" else "items"} failed and may need a retry.")
                        }
                    },
                )
            }
            Button(
                onClick = {
                    onDismiss()
                    onSyncNow()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text("Sync now")
            }
        }
    }
}


private fun formatRelativeSyncTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "recently"
    val relative = DateUtils.getRelativeTimeSpanString(
        timestampMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    return if (relative.startsWith("0 min")) {
        "just now"
    } else {
        relative
    }
}

@Composable
private fun RaqeemBottomBar(
    selectedIndex: Int,
    onNavigate: (Int) -> Unit,
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = AppColors.borderSubtle,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            },
        containerColor = AppColors.bgSurface,
        tonalElevation = 0.dp,
    ) {
        RaqeemDestination.topLevelDestinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onNavigate(index) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.purple400,
                    selectedTextColor = AppColors.purple400,
                    unselectedIconColor = AppColors.textMuted,
                    unselectedTextColor = AppColors.textMuted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
