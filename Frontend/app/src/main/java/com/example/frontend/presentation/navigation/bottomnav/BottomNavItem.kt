package com.example.frontend.presentation.navigation.bottomnav

import androidx.annotation.DrawableRes
import com.example.frontend.R
import com.example.frontend.presentation.navigation.Routes

sealed class BottomNavIcon {
    data class Drawable(@DrawableRes val resId: Int) : BottomNavIcon()
    data class Avatar(
        val avatarUrl: String?,
        @DrawableRes val fallbackResId: Int
    ) : BottomNavIcon()
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: BottomNavIcon,
    val badgeCount: Int = 0
)

fun bottomNavItems(userAvatarUrl: String?, notificationUnreadCount: Int = 0): List<BottomNavItem> = listOf(
    BottomNavItem(
        route = Routes.HOME,
        label = "Home",
        icon = BottomNavIcon.Drawable(R.drawable.icon_home)
    ),
    BottomNavItem(
        route = Routes.SEARCH,
        label = "Search",
        icon = BottomNavIcon.Drawable(R.drawable.icon_search)
    ),
    BottomNavItem(
        route = Routes.VIDEO,
        label = "VIDEO",
        icon = BottomNavIcon.Drawable(R.drawable.icon_film)
    ),
    BottomNavItem(
        route = Routes.NOTIFICATION,
        label = "Notification",
        icon = BottomNavIcon.Drawable(R.drawable.icon_notification),
        badgeCount = notificationUnreadCount
    ),
    BottomNavItem(
        route = Routes.PROFILE,
        label = "Profile",
        icon = BottomNavIcon.Avatar(
            avatarUrl = userAvatarUrl,
            fallbackResId = R.drawable.icon_user
        )
    )
)
