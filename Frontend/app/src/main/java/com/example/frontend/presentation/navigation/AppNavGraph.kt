package com.example.frontend.presentation.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.frontend.presentation.navigation.bottomnav.BottomBar
import com.example.frontend.presentation.navigation.bottomnav.bottomNavItems
import com.example.frontend.presentation.screen.chat.ChatScreen
import com.example.frontend.presentation.screen.conversation.ConversationScreen
import com.example.frontend.presentation.screen.forgotpassword.ForgotPasswordEmailScreen
import com.example.frontend.presentation.screen.forgotpassword.ForgotPasswordOtpScreen
import com.example.frontend.presentation.screen.forgotpassword.ForgotPasswordResetScreen
import com.example.frontend.presentation.screen.home.HomeScreen
import com.example.frontend.presentation.screen.login.LoginScreen
import com.example.frontend.presentation.screen.notification.NotificationScreen
import com.example.frontend.presentation.screen.postdetail.PostDetailScreen
import com.example.frontend.presentation.screen.profile.ProfileScreen
import com.example.frontend.presentation.screen.otherprofile.OtherProfileScreen
import com.example.frontend.presentation.screen.register.OtpVerificationScreen
import com.example.frontend.presentation.screen.register.RegisterScreen
import com.example.frontend.presentation.screen.register.RegisterViewModel
import com.example.frontend.presentation.screen.search.SearchScreen
import com.example.frontend.presentation.screen.setting.SettingScreen
import com.example.frontend.presentation.screen.video.VideoScreen
import com.example.frontend.presentation.viewmodel.AuthSessionViewModel
import com.example.frontend.presentation.viewmodel.MainViewModel
import com.example.frontend.presentation.viewmodel.SessionViewModel
import com.example.frontend.ui.component.AppNotification
import com.example.frontend.ui.component.NotificationType
import androidx.navigation.compose.navigation
import com.example.frontend.presentation.screen.create_post.CreatePostScreen
import com.example.frontend.presentation.screen.create_post.CreatePostViewModel
import com.example.frontend.presentation.screen.edit_post.EditPostScreen
import com.example.frontend.presentation.screen.friendrequest.FriendRequestsScreen
import com.example.frontend.presentation.screen.friend.MyFriendScreen
import com.example.frontend.presentation.screen.friend.OtherFriendScreen
import com.example.frontend.presentation.screen.home.HomeUiState
import com.example.frontend.presentation.screen.home.HomeViewModel
import com.example.frontend.presentation.screen.notification.NotificationBadgeViewModel
import com.example.frontend.presentation.screen.profile.EditProfileScreen
import com.example.frontend.presentation.screen.setting.ChangePasswordScreen
import com.example.frontend.presentation.screen.calls.CallScreen
import com.example.frontend.presentation.screen.calls.CallUiEvent
import com.example.frontend.presentation.screen.calls.CallViewModel
import com.example.frontend.domain.model.PostVisibility
import com.example.frontend.ui.component.toMediaItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.example.frontend.presentation.screen.admin.AdminScreen
import com.example.frontend.presentation.screen.admin.AdminUserProfileScreen
import com.example.frontend.presentation.screen.chat.ChatProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    authSessionViewModel: AuthSessionViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val notificationBadgeViewModel: NotificationBadgeViewModel = hiltViewModel()

    val currentUser by sessionViewModel.currentUser.collectAsState()
    val unreadNotificationCount by notificationBadgeViewModel.unreadCount.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")?.substringBefore("?")
    var notificationRefreshKey by remember { mutableIntStateOf(0) }

    val bottomBarRoutes = setOf(
        Routes.HOME,
        Routes.SEARCH,
        Routes.VIDEO_BASE,
        Routes.NOTIFICATION,
        Routes.PROFILE
    )
    val showBottomBar = currentBaseRoute in bottomBarRoutes

    val notifState by mainViewModel.notificationManager.notification.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            notificationBadgeViewModel.refreshUnreadCount()
            delay(15000)
        }
    }

    LaunchedEffect(currentBaseRoute) {
        if (currentBaseRoute == Routes.NOTIFICATION) {
            notificationBadgeViewModel.refreshUnreadCount()
        }
    }

    LaunchedEffect(Unit) {
        authSessionViewModel.sessionExpiredEvents.collectLatest {
            sessionViewModel.clearSession()
            mainViewModel.notificationManager.showMessage(
                message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
                type = NotificationType.ERROR
            )
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    // Lắng nghe cuộc gọi đến toàn cục
    LaunchedEffect(callViewModel) {
        callViewModel.uiEvent.collect { event ->
            if (event is CallUiEvent.IncomingCall) {
                callViewModel.startRinging()
                val encodedName = Uri.encode(event.name)
                val encodedAvatar = Uri.encode(event.avatar)
                navController.navigate(
                    "${Routes.CALL_BASE}/${event.callerId}?isVideoCall=${event.isVideo}&isIncoming=true&fullname=$encodedName&avatarUrl=$encodedAvatar"
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    BottomBar(
                        navController = navController,
                        items = bottomNavItems(
                            userAvatarUrl = currentUser?.avatarUrl,
                            notificationUnreadCount = unreadNotificationCount
                        )
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.SPLASH) {
                    val vm: StartViewModel = hiltViewModel()
                    val dest by vm.startDestination.collectAsState()

                    SplashScreen()

                    LaunchedEffect(dest) {
                        if (dest != null) {
                            if (dest == Routes.HOME || dest == Routes.ADMIN) {
                                sessionViewModel.fetchCurrentUser()
                            }

                            navController.navigate(dest!!) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                }

                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            navController.navigate(Routes.SPLASH) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onRegisterClick = { navController.navigate(Routes.REGISTER) },
                        onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD_EMAIL) }
                    )
                }

                composable(Routes.ADMIN) {
                    AdminScreen(
                        onLoggedOut = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                        onUserClick = { userId, isLocked ->
                            val encodedUserId = Uri.encode(userId)
                            navController.navigate(
                                "${Routes.ADMIN_USER_PROFILE_BASE}/$encodedUserId?locked=$isLocked"
                            )
                        }
                    )
                }

                composable(
                    route = Routes.ADMIN_USER_PROFILE,
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType },
                        navArgument("locked") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) {
                    AdminUserProfileScreen(
                        onBackClick = { navController.popBackStack() },
                        onDeleted = { navController.popBackStack() }
                    )
                }

                navigation(startDestination = Routes.REGISTER, route = "register_flow") {
                    composable(Routes.REGISTER) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry("register_flow")
                        }
                        val sharedViewModel: RegisterViewModel = hiltViewModel(parentEntry)

                        RegisterScreen(
                            viewModel = sharedViewModel,
                            onLoginClick = { navController.popBackStack() },
                            onSendOtpClick = {
                                navController.navigate(Routes.OTP_SENDED)
                            }
                        )
                    }

                    composable(Routes.OTP_SENDED) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry("register_flow")
                        }
                        val sharedViewModel: RegisterViewModel = hiltViewModel(parentEntry)

                        OtpVerificationScreen(
                            viewModel = sharedViewModel,
                            onBackClick = { navController.popBackStack() },
                            onRegisterClick = {
                                sessionViewModel.fetchCurrentUser()
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo("register_flow") { inclusive = true }
                                }
                            }
                        )
                    }
                }

                composable(Routes.FORGOT_PASSWORD_EMAIL) {
                    ForgotPasswordEmailScreen(
                        onBackClick = { navController.popBackStack() },
                        onOtpSent = { email ->
                            val encodedEmail = Uri.encode(email)
                            navController.navigate("${Routes.FORGOT_PASSWORD_OTP_BASE}?email=$encodedEmail")
                        }
                    )
                }

                composable(Routes.FORGOT_PASSWORD_OTP) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email")?.let(Uri::decode).orEmpty()

                    ForgotPasswordOtpScreen(
                        onBackClick = { navController.popBackStack() },
                        onOtpVerified = { _, otp ->
                            val encodedEmail = Uri.encode(email)
                            val encodedOtp = Uri.encode(otp)
                            navController.navigate("${Routes.FORGOT_PASSWORD_RESET_BASE}?email=$encodedEmail&otp=$encodedOtp")
                        }
                    )
                }

                composable(Routes.FORGOT_PASSWORD_RESET) {
                    ForgotPasswordResetScreen(
                        onBackClick = { navController.popBackStack() },
                        onResetDone = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onLoggedOut = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToSetting = {
                            navController.navigate(Routes.SETTING)
                        },
                        onNavigateToFriends = {
                            navController.navigate(Routes.MY_FRIENDS)
                        },
                        onEditPostClick = { postId ->
                            navController.navigate("${Routes.EDIT_POST_BASE}/$postId")
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        currentUser = currentUser,
                        onNavigateToMessages = {
                            navController.navigate(Routes.CONVERSATION_LIST)
                        },
                        onCreatePostClick = {
                            navController.navigate(Routes.CREATE_POST)
                        },
                        onEditPostClick = { postId ->
                            navController.navigate("${Routes.EDIT_POST_BASE}/$postId")
                        },
                        onPostClick = { post ->
                            navController.navigate("${Routes.POST_DETAIL_BASE}/${Uri.encode(post.id)}")
                        },
                        onVideoClick = { postId, videoUrl ->
                            val encodedPostId = Uri.encode(postId)
                            val encodedVideoUrl = Uri.encode(videoUrl)
                            navController.navigate("${Routes.VIDEO_BASE}?postId=$encodedPostId&videoUrl=$encodedVideoUrl") {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Routes.HOME) { saveState = true }
                            }
                        },
                        onAvatarClick = { clickedUserId ->
                            if (clickedUserId == currentUser?.id) {
                                navController.navigate(Routes.PROFILE) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else if (clickedUserId.isNotBlank()) {
                                val encodedUserId = Uri.encode(clickedUserId)
                                navController.navigate("${Routes.OTHER_PROFILE_BASE}/$encodedUserId")
                            }
                        }
                    )
                }

                composable(Routes.POST_DETAIL) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")?.let(Uri::decode)
                    PostDetailScreen(
                        postId = postId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.CREATE_POST,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(400)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(400)
                        )
                    }
                ) {
                    val createPostViewModel: CreatePostViewModel = hiltViewModel()
                    val currentUser by sessionViewModel.currentUser.collectAsState()

                    CreatePostScreen(
                        currentUser = currentUser,
                        viewModel = createPostViewModel,
                        onBackClick = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() }
                    )
                }

                composable(Routes.EDIT_POST) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                    val currentUser by sessionViewModel.currentUser.collectAsState()
                    val homeEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.HOME)
                    }
                    val homeViewModel: HomeViewModel = hiltViewModel(homeEntry)
                    val homeUiState by homeViewModel.uiState.collectAsState()
                    val editingPost = (homeUiState as? HomeUiState.Success)
                        ?.posts
                        ?.firstOrNull { it.id == postId }

                    EditPostScreen(
                        currentUser = currentUser,
                        initialContent = editingPost?.content.orEmpty(),
                        initialMedia = editingPost?.toMediaItems().orEmpty(),
                        initialVisibility = editingPost?.visibility ?: PostVisibility.PUBLIC,
                        onBackClick = { navController.popBackStack() },
                        onComplete = { content, visibility, keptExistingMedia, newMediaUris ->
                            homeViewModel.editPost(
                                postId = postId,
                                newContent = content,
                                newVisibility = visibility,
                                keptExistingMedia = keptExistingMedia,
                                newMediaUris = newMediaUris
                            )
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        currentUserId = currentUser?.id,
                        onUserClick = { userId ->
                            if (userId.isNotBlank()) {
                                val encodedUserId = Uri.encode(userId)
                                navController.navigate(Routes.OTHER_PROFILE.replace("{userId}", encodedUserId))
                            }
                        }
                    )
                }

                composable(
                    route = Routes.VIDEO,
                    arguments = listOf(
                        navArgument("postId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("videoUrl") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val initialPostId = backStackEntry.arguments?.getString("postId")?.let(Uri::decode)
                    val initialVideoUrl = backStackEntry.arguments?.getString("videoUrl")?.let(Uri::decode)
                    VideoScreen(
                        currentUserAvatarUrl = currentUser?.avatarUrl,
                        initialPostId = initialPostId,
                        initialVideoUrl = initialVideoUrl
                    )
                }

                composable(Routes.NOTIFICATION) {
                    NotificationScreen(
                        refreshKey = notificationRefreshKey.toLong(),
                        onItemsUpdated = { notificationBadgeViewModel.refreshUnreadCount() },
                        onNotificationClick = { notification ->
                            val target = notification.url.orEmpty()
                            when {
                                target.startsWith("/friends/requests") -> {
                                    navController.navigate(Routes.FRIEND_REQUESTS) {
                                        launchSingleTop = true
                                    }
                                }

                                target.startsWith("/posts/") -> {
                                    val postId = target.substringAfterLast("/").trim()
                                    if (postId.isNotBlank()) {
                                        navController.navigate("${Routes.POST_DETAIL_BASE}/${Uri.encode(postId)}")
                                    }
                                }

                                target.startsWith("/users/") -> {
                                    val userId = target.substringAfterLast("/").trim()
                                    if (userId.isNotBlank()) {
                                        if (userId == currentUser?.id) {
                                            navController.navigate(Routes.PROFILE) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } else {
                                            navController.navigate("${Routes.OTHER_PROFILE_BASE}/${Uri.encode(userId)}")
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                composable(Routes.FRIEND_REQUESTS) {
                    FriendRequestsScreen(
                        onBack = { navController.popBackStack() },
                        onAvatarClick = { clickedUserId ->
                            if (clickedUserId == currentUser?.id) {
                                navController.navigate(Routes.PROFILE) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else if (clickedUserId.isNotBlank()) {
                                val encodedUserId = Uri.encode(clickedUserId)
                                navController.navigate("${Routes.OTHER_PROFILE_BASE}/$encodedUserId")
                            }
                        },
                    )
                }

                composable(Routes.SETTING) {
                    SettingScreen(
                        onBackClick = { navController.popBackStack() },
                        onLoggedOut = {
                            sessionViewModel.clearSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        },
                        onEditProfileClick = {
                            navController.navigate(Routes.EDIT_PROFILE)
                        },
                        onChangePasswordClick = {
                            navController.navigate(Routes.CHANGE_PASSWORD)
                        }
                    )
                }

                composable(Routes.EDIT_PROFILE) {
                    val currentUser by sessionViewModel.currentUser.collectAsState()
                    EditProfileScreen(
                        currentUser = currentUser,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Routes.CHANGE_PASSWORD) {
                    ChangePasswordScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Routes.OTHER_PROFILE) { backStackEntry ->
                    val targetUserId =
                        backStackEntry.arguments?.getString("userId")?.let(Uri::decode).orEmpty()
                    OtherProfileScreen(
                        userId = targetUserId,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToChat = { conversationId, partnerId, conversationName, conversationAvatar ->
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar ?: "")
                            navController.navigate("${Routes.CHAT_BASE}/$conversationId?partnerId=$partnerId&name=$encodedName&avatar=$encodedAvatar")
                        },
                        onNavigateToFriends = { clickedUserId ->
                            if (clickedUserId.isNotBlank()) {
                                val encoded = Uri.encode(clickedUserId)
                                navController.navigate("${Routes.OTHER_FRIENDS_BASE}/$encoded")
                            }
                        }
                    )
                }

                composable(
                    route = Routes.OTHER_FRIENDS,
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val targetUserId = backStackEntry.arguments?.getString("userId")?.let(Uri::decode).orEmpty()
                    OtherFriendScreen(
                        onBack = { navController.popBackStack() },
                        onAvatarClick = { clickedId ->
                            if (clickedId == null) return@OtherFriendScreen
                            if (clickedId.isNotBlank()) {
                                val encoded = Uri.encode(clickedId)
                                navController.navigate("${Routes.OTHER_PROFILE_BASE}/$encoded")
                            }
                        },
                        onNavigateToChat = { conversationId, partnerId, conversationName, conversationAvatar ->
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar ?: "")
                            navController.navigate("${Routes.CHAT_BASE}/$conversationId?partnerId=$partnerId&name=$encodedName&avatar=$encodedAvatar")
                        }
                    )
                }

                composable(Routes.CONVERSATION_LIST) {
                    ConversationScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToChat = { conversationId, partnerId, conversationName, conversationAvatar ->
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar)
                            navController.navigate("${Routes.CHAT_BASE}/$conversationId?partnerId=$partnerId&name=$encodedName&avatar=$encodedAvatar")
                        }
                    )
                }

                composable(Routes.CHAT) { backStackEntry ->
                    val conversationId =
                        backStackEntry.arguments?.getString("conversationId") ?: "0"
                    val partnerId =
                        backStackEntry.arguments?.getString("partnerId") ?: ""
                    val conversationName =
                        backStackEntry.arguments?.getString("name")?.let(Uri::decode)
                            ?: "Người dùng $conversationId"
                    val conversationAvatar =
                        backStackEntry.arguments?.getString("avatar")?.let(Uri::decode)

                    ChatScreen(
                        conversationId = conversationId,
                        partnerId = partnerId,
                        conversationName = conversationName,
                        conversationAvatarUrl = conversationAvatar,
                        onBackClick = { navController.popBackStack() },
                        onVoiceCallClick = {
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar ?: "")
                            navController.navigate("${Routes.CALL_BASE}/$partnerId?isVideoCall=false&isIncoming=false&fullname=$encodedName&avatarUrl=$encodedAvatar")
                        },
                        onVideoCallClick = {
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar ?: "")
                            navController.navigate("${Routes.CALL_BASE}/$partnerId?isVideoCall=true&isIncoming=false&fullname=$encodedName&avatarUrl=$encodedAvatar")
                        },
                        onProfileClick = {
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar ?: "")
                            navController.navigate("${Routes.CHAT_PROFILE_BASE}/$partnerId?name=$encodedName&avatar=$encodedAvatar&conversationId=$conversationId")
                        }
                    )
                }

                composable(Routes.CHAT_PROFILE) { backStackEntry ->
                    val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
                    val partnerName = backStackEntry.arguments?.getString("name")?.let(Uri::decode) ?: "Người dùng"
                    val partnerAvatarUrl = backStackEntry.arguments?.getString("avatar")?.let(Uri::decode)
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""

                    val currentUser by sessionViewModel.currentUser.collectAsState()

                    ChatProfileScreen(
                        conversationId = conversationId,
                        partnerId = partnerId,
                        partnerName = partnerName,
                        partnerAvatarUrl = partnerAvatarUrl,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToProfile = { clickedUserId ->
                            if (clickedUserId == currentUser?.id) {
                                navController.navigate(Routes.PROFILE) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else if (clickedUserId.isNotBlank()) {
                                val encodedUserId = Uri.encode(clickedUserId)
                                navController.navigate("${Routes.OTHER_PROFILE_BASE}/$encodedUserId")
                            }
                        },
                        onVoiceCallClick = {
                            val encodedName = Uri.encode(partnerName)
                            val encodedAvatar = Uri.encode(partnerAvatarUrl ?: "")
                            navController.navigate("${Routes.CALL_BASE}/$partnerId?isVideoCall=false&isIncoming=false&fullname=$encodedName&avatarUrl=$encodedAvatar")
                        },
                        onVideoCallClick = {
                            val encodedName = Uri.encode(partnerName)
                            val encodedAvatar = Uri.encode(partnerAvatarUrl ?: "")
                            navController.navigate("${Routes.CALL_BASE}/$partnerId?isVideoCall=true&isIncoming=false&fullname=$encodedName&avatarUrl=$encodedAvatar")
                        }
                    )
                }

                composable(Routes.MY_FRIENDS) {
                    MyFriendScreen(
                        onBack = { navController.popBackStack() },
                        onAvatarClick = { clickedUserId ->
                            if (clickedUserId == currentUser?.id) {
                                navController.navigate(Routes.PROFILE) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else if (clickedUserId.isNotBlank()) {
                                val encodedUserId = Uri.encode(clickedUserId)
                                navController.navigate("${Routes.OTHER_PROFILE_BASE}/$encodedUserId")
                            }
                        },
                        onNavigateToChat = { conversationId, partnerId, conversationName, conversationAvatar ->
                            val encodedName = Uri.encode(conversationName)
                            val encodedAvatar = Uri.encode(conversationAvatar ?: "")
                            navController.navigate("${Routes.CHAT_BASE}/$conversationId?partnerId=$partnerId&name=$encodedName&avatar=$encodedAvatar")
                        }
                    )
                }

                composable(Routes.CALL) { backStackEntry ->
                    val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
                    val isVideoCall = backStackEntry.arguments?.getString("isVideoCall")?.toBoolean() ?: false
                    val isIncoming = backStackEntry.arguments?.getString("isIncoming")?.toBoolean() ?: false
                    val fullname = backStackEntry.arguments?.getString("fullname")?.let(Uri::decode) ?: ""
                    val avatarUrl = backStackEntry.arguments?.getString("avatarUrl")?.let(Uri::decode) ?: ""

                    val uiState by callViewModel.uiState.collectAsState()
                    var callDuration by remember { mutableIntStateOf(0) }

                    LaunchedEffect(uiState.status) {
                        if (uiState.status == "accepted") {
                            while (true) {
                                delay(1000)
                                callDuration++
                            }
                        }
                    }

                    LaunchedEffect(uiState.status) {
                        if (uiState.status == "rejected" || uiState.status == "ended") {
                            delay(1500)
                            callViewModel.clearState()
                            navController.popBackStack()
                        }
                    }

                    val formattedDuration = String.format("%02d:%02d", callDuration / 60, callDuration % 60)

                    // Khởi tạo cuộc gọi (Chỉ dành cho người GỌI)
                    LaunchedEffect(Unit) {
                        if (!isIncoming && uiState.status == "idle") {
                            callViewModel.startCall(targetUserId, isVideoCall, fullname, avatarUrl)
                        }
                    }

                    CallScreen(
                        status = uiState.status,
                        fullname = if (isIncoming && uiState.callerName.isNotEmpty()) uiState.callerName else fullname,
                        avatarUrl = if (isIncoming && uiState.callerAvatarUrl.isNotEmpty()) uiState.callerAvatarUrl else avatarUrl,
                        isVideoCall = uiState.isVideoCall,
                        isIncoming = uiState.isIncomingCall,
                        formattedDuration = formattedDuration,
                        isMicOn = uiState.isMicOn,
                        isCamOn = uiState.isCamOn,
                        isSpeakerOn = uiState.isSpeakerOn,
                        localVideoTrack = uiState.localVideoTrack,
                        remoteVideoTrack = uiState.remoteVideoTrack,
                        eglBaseContext = callViewModel.webRtcClient.eglBaseContext,
                        onSwitchCamera = { callViewModel.switchCamera() },
                        onToggleMic = { callViewModel.toggleMic() },
                        onToggleCam = { callViewModel.toggleCamera() },
                        onToggleSpeaker = { callViewModel.toggleSpeaker() },
                        onAcceptCall = { callViewModel.acceptCall(targetUserId) },
                        onRejectCall = { callViewModel.declineCall(targetUserId) },
                        onEndCall = { callViewModel.endCall(targetUserId) },
                    )
                }
            }
        }

        AppNotification(
            message = notifState.message,
            type = notifState.type,
            isVisible = notifState.isVisible,
            onDismiss = { mainViewModel.notificationManager.clear() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

