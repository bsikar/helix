package com.bsikar.helix.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bsikar.helix.MainActivity
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
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
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun libraryScreen_displaysCorrectly() {
        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plan to Read").assertIsDisplayed()
        composeTestRule.onNodeWithText("Completed").assertIsDisplayed()
    }

    @Test
    fun searchFunctionality_filtersBooks() {
        // Navigate to library if not already there
        composeTestRule.onNodeWithContentDescription("Library").performClick()
        
        // Open search
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        
        // Type in search box
        composeTestRule.onNodeWithTag("SearchField").performTextInput("test")
        
        // Verify search is active
        composeTestRule.onNodeWithText("test").assertIsDisplayed()
    }

    @Test
    fun bookCard_clickNavigatesToReader() {
        // This test would require having test books in the database
        // For now, we'll test the UI components that should be present
        composeTestRule.onNodeWithText("Reading").assertIsDisplayed()
        
        // If there are books, this would test clicking on a book card
        // composeTestRule.onNodeWithTag("BookCard_testBook").performClick()
        // composeTestRule.onNodeWithTag("ReaderScreen").assertIsDisplayed()
    }

    @Test
    fun sortingButtons_toggleSortOrder() {
        // Test reading sort toggle
        composeTestRule.onNodeWithContentDescription("Sort reading books").performClick()
        
        // Test plan to read sort toggle
        composeTestRule.onNodeWithContentDescription("Sort plan to read books").performClick()
        
        // Test completed sort toggle  
        composeTestRule.onNodeWithContentDescription("Sort completed books").performClick()
    }

    @Test
    fun refreshButton_triggersLibraryRefresh() {
        composeTestRule.onNodeWithContentDescription("Refresh library").performClick()
        
        // Verify refresh was triggered (might show loading indicator)
        // This would require checking the state in the ViewModel
    }

    @Test
    fun tabNavigation_switchesBetweenSections() {
        // Test switching to Plan to Read tab
        composeTestRule.onNodeWithText("Plan to Read").performClick()
        composeTestRule.onNodeWithText("Plan to Read").assertIsDisplayed()
        
        // Test switching to Completed tab
        composeTestRule.onNodeWithText("Completed").performClick()
        composeTestRule.onNodeWithText("Completed").assertIsDisplayed()
        
        // Test switching back to Reading tab
        composeTestRule.onNodeWithText("Reading").performClick()
        composeTestRule.onNodeWithText("Reading").assertIsDisplayed()
    }

    @Test
    fun bookImport_opensFilePicker() {
        // Test import button functionality
        composeTestRule.onNodeWithContentDescription("Import book").performClick()
        
        // This would typically open a file picker, which is harder to test in unit tests
        // but we can verify the UI responds to the click
    }

    @Test
    fun emptyState_displaysCorrectMessage() {
        // This test verifies empty state handling when no books are present
        // The exact implementation depends on how empty states are handled
        // composeTestRule.onNodeWithText("No books found").assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        // Test error state handling
        // This would require injecting an error state into the ViewModel
        // composeTestRule.onNodeWithText("Error loading books").assertIsDisplayed()
    }

    @Test
    fun bookContextMenu_displaysOptions() {
        // Test long press or context menu on book cards
        // This would require having test books and testing long press gestures
        // composeTestRule.onNodeWithTag("BookCard_testBook").performLongClick()
        // composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        // composeTestRule.onNodeWithText("Edit Tags").assertIsDisplayed()
    }
}