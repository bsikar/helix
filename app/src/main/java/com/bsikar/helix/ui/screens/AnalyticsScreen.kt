package com.bsikar.helix.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsikar.helix.R
import com.bsikar.helix.data.model.*
import com.bsikar.helix.viewmodels.AnalyticsViewModel

/**
 * Analytics dashboard screen showing reading statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBookClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val globalStats by viewModel.globalStats.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // Tab bar for different analytics views
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp
        ) {
            AnalyticsTab.values().forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(tab.title) },
                    icon = { Icon(tab.icon, contentDescription = tab.title) }
                )
            }
        }

        // Content based on selected tab
        when (AnalyticsTab.values()[selectedTab]) {
            AnalyticsTab.OVERVIEW -> OverviewTab(
                globalStats = globalStats,
                onBookClick = onBookClick
            )
            AnalyticsTab.BOOKS -> BooksAnalyticsTab(
                viewModel = viewModel,
                onBookClick = onBookClick
            )
            AnalyticsTab.SESSIONS -> SessionsTab(
                sessions = recentSessions
            )
            AnalyticsTab.GOALS -> GoalsTab(
                globalStats = globalStats,
                viewModel = viewModel
            )
        }
    }
}

/**
 * Overview tab showing general statistics
 */
@Composable
private fun OverviewTab(
    globalStats: GlobalReadingStats?,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Reading Overview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        globalStats?.let { stats ->
            item {
                GlobalStatsCard(stats = stats)
            }

            item {
                ReadingStreakCard(
                    currentStreak = stats.currentStreak,
                    longestStreak = stats.longestStreak
                )
            }

            item {
                MonthlyProgressCard(
                    booksStarted = stats.booksStartedThisMonth,
                    booksCompleted = stats.booksCompletedThisMonth,
                    averagePerMonth = stats.averageBooksPerMonth
                )
            }

            if (stats.favoriteGenres.isNotEmpty()) {
                item {
                    FavoriteGenresCard(genres = stats.favoriteGenres)
                }
            }
        } ?: item {
            // Loading state
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Books analytics tab
 */
@Composable
private fun BooksAnalyticsTab(
    viewModel: AnalyticsViewModel,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookAnalytics by viewModel.bookAnalytics.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Book Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(bookAnalytics) { analytics ->
            BookAnalyticsCard(
                analytics = analytics,
                onClick = { onBookClick(analytics.bookId) }
            )
        }

        if (bookAnalytics.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "No Reading Data",
                    subtitle = "Start reading to see analytics"
                )
            }
        }
    }
}

/**
 * Sessions tab showing recent reading sessions
 */
@Composable
private fun SessionsTab(
    sessions: List<ReadingSession>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(sessions) { session ->
            ReadingSessionCard(session = session)
        }

        if (sessions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.History,
                    title = "No Recent Sessions",
                    subtitle = "Your reading sessions will appear here"
                )
            }
        }
    }
}

/**
 * Goals tab for reading goals and progress
 */
@Composable
private fun GoalsTab(
    globalStats: GlobalReadingStats?,
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Reading Goals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        globalStats?.readingGoalProgress?.let { goal ->
            item {
                ReadingGoalCard(goal = goal)
            }
        } ?: item {
            CreateGoalCard(
                onCreateGoal = { goalType, target, deadline ->
                    viewModel.createReadingGoal(goalType, target, deadline)
                }
            )
        }

        item {
            ReadingInsightsCard(globalStats = globalStats)
        }
    }
}

// Individual card components

@Composable
private fun GlobalStatsCard(
    stats: GlobalReadingStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Reading Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.totalBooksRead}",
                    label = "Books Started",
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                StatItem(
                    value = "${stats.totalBooksCompleted}",
                    label = "Books Finished",
                    icon = Icons.Default.CheckCircle
                )
                StatItem(
                    value = stats.getFormattedTotalTime(),
                    label = "Total Time",
                    icon = Icons.Default.Schedule
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.averageReadingSpeed.toInt()} WPM",
                    label = "Reading Speed",
                    icon = Icons.Default.Speed
                )
                StatItem(
                    value = "${(stats.completionRate * 100).toInt()}%",
                    label = "Completion Rate",
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
                StatItem(
                    value = "${stats.totalSessions}",
                    label = "Sessions",
                    icon = Icons.Default.Timeline
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReadingStreakCard(
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = "Reading Streak",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reading Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$currentStreak days",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                    Text(
                        text = "Current Streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$longestStreak days",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Best Streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BookAnalyticsCard(
    analytics: ReadingAnalytics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = analytics.bookTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = analytics.getFormattedTotalTime(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Reading Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${analytics.averageReadingSpeed.toInt()} WPM",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = analytics.getReadingPaceDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { analytics.completionPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Text(
                text = "${kotlin.math.round(analytics.completionPercentage * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ReadingSessionCard(
    session: ReadingSession,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatSessionDate(session.startTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (session.isActive) {
                    Badge {
                        Text("Active")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${session.durationMinutes.toInt()} min",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${session.wordsPerMinute.toInt()} WPM",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Reading Speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions and enums

private enum class AnalyticsTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    BOOKS("Books", Icons.AutoMirrored.Filled.MenuBook),
    SESSIONS("Sessions", Icons.Default.History),
    GOALS("Goals", Icons.Default.Flag)
}

private fun formatSessionDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

// Placeholder components for remaining cards

@Composable
private fun MonthlyProgressCard(
    booksStarted: Int,
    booksCompleted: Int,
    averagePerMonth: Double,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$booksStarted books started, $booksCompleted completed",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Average: ${String.format("%.1f", averagePerMonth)} books/month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FavoriteGenresCard(
    genres: List<String>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Favorite Genres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = genres.take(3).joinToString(", "),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReadingGoalCard(
    goal: ReadingGoalProgress,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reading Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${goal.currentValue} / ${goal.targetValue} ${goal.goalType.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { goal.progressPercentage / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreateGoalCard(
    onCreateGoal: (ReadingGoalType, Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Set a Reading Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your progress with personalized goals",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { 
                    // TODO: Open goal creation dialog
                    onCreateGoal(ReadingGoalType.BOOKS_PER_MONTH, 4, System.currentTimeMillis())
                }
            ) {
                Text("Create Goal")
            }
        }
    }
}

@Composable
private fun ReadingInsightsCard(
    globalStats: GlobalReadingStats?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reading Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            globalStats?.let { stats ->
                Text(
                    text = "You're reading ${String.format("%.1f", stats.averageBooksPerMonth)} books per month on average.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (stats.averageReadingSpeed > 0) {
                    Text(
                        text = "Your reading speed of ${stats.averageReadingSpeed.toInt()} WPM is ${
                            when {
                                stats.averageReadingSpeed >= 250 -> "above average"
                                stats.averageReadingSpeed >= 200 -> "average"
                                else -> "below average"
                            }
                        }.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}