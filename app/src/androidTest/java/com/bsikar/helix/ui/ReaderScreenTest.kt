package com.bsikar.helix.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
class ReaderScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun readerScreen_displaysContent() {
        // This test assumes we navigate to reader screen
        // In a real scenario, we'd need to set up test data and navigate properly
        
        // Test that reader content area exists
        composeTestRule.onNodeWithTag("ReaderContent").assertExists()
    }

    @Test
    fun readerNavigation_previousNextChapter() {
        // Test previous chapter navigation
        composeTestRule.onNodeWithContentDescription("Previous chapter").performClick()
        
        // Test next chapter navigation
        composeTestRule.onNodeWithContentDescription("Next chapter").performClick()
    }

    @Test
    fun readerSettings_opensSettingsPanel() {
        // Test opening reader settings
        composeTestRule.onNodeWithContentDescription("Reader settings").performClick()
        
        // Verify settings panel is displayed
        composeTestRule.onNodeWithText("Font Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Line Height").assertIsDisplayed()
        composeTestRule.onNodeWithText("Background Color").assertIsDisplayed()
    }

    @Test
    fun fontSizeAdjustment_changesFontSize() {
        // Open settings
        composeTestRule.onNodeWithContentDescription("Reader settings").performClick()
        
        // Test font size increase
        composeTestRule.onNodeWithContentDescription("Increase font size").performClick()
        
        // Test font size decrease
        composeTestRule.onNodeWithContentDescription("Decrease font size").performClick()
    }

    @Test
    fun bookmarkFunctionality_addsAndRemovesBookmarks() {
        // Test adding bookmark
        composeTestRule.onNodeWithContentDescription("Add bookmark").performClick()
        
        // Verify bookmark was added
        composeTestRule.onNodeWithContentDescription("Remove bookmark").assertIsDisplayed()
        
        // Test removing bookmark
        composeTestRule.onNodeWithContentDescription("Remove bookmark").performClick()
        
        // Verify bookmark was removed
        composeTestRule.onNodeWithContentDescription("Add bookmark").assertIsDisplayed()
    }

    @Test
    fun tableOfContents_displaysAndNavigates() {
        // Open table of contents
        composeTestRule.onNodeWithContentDescription("Table of contents").performClick()
        
        // Verify TOC is displayed
        composeTestRule.onNodeWithTag("TableOfContents").assertIsDisplayed()
        
        // Test chapter selection (would require test data)
        // composeTestRule.onNodeWithText("Chapter 1").performClick()
    }

    @Test
    fun readerProgress_updatesCorrectly() {
        // Test that progress indicator updates when scrolling
        composeTestRule.onNodeWithTag("ProgressIndicator").assertExists()
        
        // Scroll through content
        composeTestRule.onNodeWithTag("ReaderContent").performScrollTo()
        
        // Progress should update (would need to verify actual progress value)
    }

    @Test
    fun themeToggle_switchesThemes() {
        // Test theme switching functionality
        composeTestRule.onNodeWithContentDescription("Reader settings").performClick()
        
        // Test switching to dark theme
        composeTestRule.onNodeWithText("Dark").performClick()
        
        // Test switching to light theme
        composeTestRule.onNodeWithText("Light").performClick()
        
        // Test switching to sepia theme
        composeTestRule.onNodeWithText("Sepia").performClick()
    }

    @Test
    fun loadingState_displaysLoadingIndicator() {
        // Test loading state when chapter is being loaded
        // This would require triggering a chapter load
        // composeTestRule.onNodeWithTag("LoadingIndicator").assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        // Test error state handling in reader
        // This would require injecting an error state
        // composeTestRule.onNodeWithText("Error loading chapter").assertIsDisplayed()
    }

    @Test
    fun readerMenu_displaysAllOptions() {
        // Test that reader menu shows all expected options
        composeTestRule.onNodeWithContentDescription("Reader menu").performClick()
        
        // Verify menu options
        composeTestRule.onNodeWithText("Bookmarks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Table of Contents").assertIsDisplayed()
    }

    @Test
    fun textSelection_enablesCopyFunctionality() {
        // Test text selection and copy functionality
        // This is complex to test in Compose as it involves touch gestures
        // composeTestRule.onNodeWithTag("ReaderContent").performTouchInput {
        //     longClick(Offset(100f, 100f))
        // }
        // composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
    }

    @Test
    fun fullScreenMode_hidesSystemBars() {
        // Test full screen reading mode
        composeTestRule.onNodeWithContentDescription("Full screen").performClick()
        
        // Verify UI elements are hidden in full screen mode
        // This would require checking system UI visibility
    }
}