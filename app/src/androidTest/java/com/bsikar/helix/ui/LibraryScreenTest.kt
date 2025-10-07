package com.bsikar.helix.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bsikar.helix.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_showsLibraryHeader() {
        composeRule.onNodeWithText("Helix").assertIsDisplayed()
        composeRule.onNodeWithText("Scan library").assertIsDisplayed()
    }

    @Test
    fun homeScreen_hasSearchField() {
        composeRule.onNodeWithText("Search your library").assertIsDisplayed()
    }

    @Test
    fun tappingSettings_opensSettingsScreen() {
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
    }
}
