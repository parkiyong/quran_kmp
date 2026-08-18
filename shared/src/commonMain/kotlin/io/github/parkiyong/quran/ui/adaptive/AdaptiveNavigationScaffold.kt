package io.github.parkiyong.quran.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.parkiyong.quran.navigation.NavigationItem

fun NavigationItem.getIcon(): ImageVector = when (this) {
    NavigationItem.SURAH_INDEX -> Icons.Default.MenuBook
    NavigationItem.BOOKMARKS -> Icons.Default.Bookmark
    NavigationItem.AUDIO_DOWNLOADS -> Icons.Default.Headphones
    NavigationItem.SETTINGS -> Icons.Default.Settings
}

@Composable
fun AdaptiveNavigationScaffold(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowWidthSizeClass.fromWidth(maxWidth)
        val isExpanded = windowSizeClass == WindowWidthSizeClass.EXPANDED

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavigationItem.entries.forEach { item ->
                        NavigationRailItem(
                            selected = selectedItem == item,
                            onClick = { onItemSelected(item) },
                            icon = {
                                Icon(
                                    imageVector = item.getIcon(),
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    content()
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationItem.entries.forEach { item ->
                            NavigationBarItem(
                                selected = selectedItem == item,
                                onClick = { onItemSelected(item) },
                                icon = {
                                    Icon(
                                        imageVector = item.getIcon(),
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    content()
                }
            }
        }
    }
}
