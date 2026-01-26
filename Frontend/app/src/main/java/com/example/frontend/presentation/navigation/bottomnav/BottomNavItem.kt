package com.example.frontend.presentation.navigation.bottomnav

import androidx.annotation.DrawableRes
import com.example.frontend.R

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
    val icon: BottomNavIcon
)

fun bottomNavItems(userAvatarUrl: String?): List<BottomNavItem> = listOf(
    BottomNavItem(
        route = "home",
        label = "Home",
        icon = BottomNavIcon.Drawable(R.drawable.icon_home)
    ),
    BottomNavItem(
        route = "film",
        label = "FIlm",
        icon = BottomNavIcon.Drawable(R.drawable.icon_film)
    ),
    BottomNavItem(
        route = "search",
        label = "Search",
        icon = BottomNavIcon.Drawable(R.drawable.icon_search)
    ),
    BottomNavItem(
        route = "notification",
        label = "Notification",
        icon = BottomNavIcon.Drawable(R.drawable.icon_notification)
    ),
    BottomNavItem(
        route = "profile",
        label = "Profile",
        icon = BottomNavIcon.Avatar(
            avatarUrl = userAvatarUrl,
            fallbackResId = R.drawable.icon_user
        )
    )
)
