package com.example.frontend.presentation.navigation.bottomnav

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.frontend.presentation.navigation.Routes

@Composable
fun BottomBar(
    navController: NavController,
    items: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        items.forEach { item ->
            val selected = currentRoute.isSameRouteAs(item.route)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) return@NavigationBarItem

                    if (item.route == Routes.HOME) {
                        val returnedToHome = navController.popBackStack(Routes.HOME, false)
                        if (!returnedToHome) {
                            navController.navigate(Routes.HOME) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        return@NavigationBarItem
                    }

                    navController.navigate(item.route) {
                        popUpTo(Routes.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { BottomBarIcon(item.icon) }
//                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun BottomBarIcon(icon: BottomNavIcon) {
    when (icon) {
        is BottomNavIcon.Drawable -> {
            Icon(
                painter = painterResource(icon.resId),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        is BottomNavIcon.Avatar -> {
            val url = icon.avatarUrl
            if (url.isNullOrBlank()) {
                Icon(
                    painter = painterResource(icon.fallbackResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = "Profile avatar",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(icon.fallbackResId),
                    error = painterResource(icon.fallbackResId)
                )
            }
        }
    }
}

private fun String?.isSameRouteAs(tabRoute: String): Boolean {
    if (this == null) return false
    val base = this.substringBefore("/")
    val tabBase = tabRoute.substringBefore("/")
    return base == tabBase
}
