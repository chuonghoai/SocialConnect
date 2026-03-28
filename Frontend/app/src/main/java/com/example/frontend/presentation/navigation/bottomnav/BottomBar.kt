package com.example.frontend.presentation.navigation.bottomnav

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
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
                icon = {
                    BottomBarIcon(
                        icon = item.icon,
                        selected = selected,
                        badgeCount = item.badgeCount
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
//                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun BottomBarIcon(icon: BottomNavIcon, selected: Boolean, badgeCount: Int) {
    val iconContent: @Composable () -> Unit = {
        when (icon) {
            is BottomNavIcon.Drawable -> {
                Icon(
                    painter = painterResource(icon.resId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
                            .clip(CircleShape)
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = CircleShape
                            ),
                        placeholder = painterResource(icon.fallbackResId),
                        error = painterResource(icon.fallbackResId)
                    )
                }
            }
        }
    }

    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        ) {
            iconContent()
        }
    } else {
        iconContent()
    }
}

private fun String?.isSameRouteAs(tabRoute: String): Boolean {
    if (this == null) return false
    val base = this.substringBefore("/").substringBefore("?")
    val tabBase = tabRoute.substringBefore("/").substringBefore("?")
    return base == tabBase
}
