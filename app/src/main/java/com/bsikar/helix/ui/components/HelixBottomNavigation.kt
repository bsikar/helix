package com.bsikar.helix.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bsikar.helix.R
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.screens.MainTab

@Composable
fun HelixBottomNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    isPlayingVisible: Boolean,
    isPlayingActive: Boolean,
    onPlayingClick: () -> Unit,
    playingTitle: String? = null,
    theme: AppTheme
) {
    NavigationBar(
        containerColor = theme.surfaceColor,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == MainTab.Home,
            onClick = { onTabSelected(MainTab.Home) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.home)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.home),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = theme.accentColor.copy(alpha = theme.alphaOverlay),
                selectedIconColor = theme.accentColor,
                selectedTextColor = theme.accentColor,
                unselectedIconColor = theme.secondaryTextColor,
                unselectedTextColor = theme.secondaryTextColor
            )
        )

        if (isPlayingVisible) {
            NavigationBarItem(
                selected = isPlayingActive,
                onClick = onPlayingClick,
                icon = {
                    BadgedBox(
                        badge = {
                            if (isPlayingActive) {
                                Badge(
                                    containerColor = theme.accentColor,
                                    contentColor = theme.surfaceColor
                                ) {}
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = stringResource(R.string.playing),
                            tint = if (isPlayingActive) theme.accentColor else theme.secondaryTextColor
                        )
                    }
                },
                label = {
                    Text(
                        text = playingTitle ?: stringResource(R.string.playing),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = theme.accentColor.copy(alpha = theme.alphaOverlay),
                    selectedIconColor = theme.accentColor,
                    selectedTextColor = theme.accentColor,
                    unselectedIconColor = theme.secondaryTextColor,
                    unselectedTextColor = theme.secondaryTextColor
                )
            )
        }

        NavigationBarItem(
            selected = selectedTab == MainTab.Search,
            onClick = { onTabSelected(MainTab.Search) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.search),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = theme.accentColor.copy(alpha = theme.alphaOverlay),
                selectedIconColor = theme.accentColor,
                selectedTextColor = theme.accentColor,
                unselectedIconColor = theme.secondaryTextColor,
                unselectedTextColor = theme.secondaryTextColor
            )
        )

        NavigationBarItem(
            selected = selectedTab == MainTab.Library,
            onClick = { onTabSelected(MainTab.Library) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = stringResource(R.string.library)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.library),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = theme.accentColor.copy(alpha = theme.alphaOverlay),
                selectedIconColor = theme.accentColor,
                selectedTextColor = theme.accentColor,
                unselectedIconColor = theme.secondaryTextColor,
                unselectedTextColor = theme.secondaryTextColor
            )
        )
    }
}
