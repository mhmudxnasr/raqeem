package com.raqeem.app.presentation.ui.screens.transaction

import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raqeem.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionWorkspaceUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun add_edit_and_delete_transaction_from_transaction_workspace() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                try {
                    composeRule.onAllNodesWithText("+ Expense").fetchSemanticsNodes().isNotEmpty()
                } catch (_: IllegalStateException) {
                    false
                }
            }

            composeRule.onNodeWithText("+ Expense").performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Add Expense").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Choose an account").fetchSemanticsNodes().isEmpty() &&
                    composeRule.onAllNodesWithText("Choose a category").fetchSemanticsNodes().isEmpty()
            }

            composeRule.onNodeWithTag("transaction_editor_amount_input").performTextInput("12.50")
            composeRule.onNodeWithTag("transaction_editor_note_input").performTextInput("Lunch")
            try {
                composeRule.onNodeWithTag("transaction_editor_save_button").performClick()
            } catch (_: AssertionError) {
                composeRule.onNodeWithTag("transaction_editor_save_button").performScrollTo().performClick()
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                try {
                    composeRule.onNodeWithTag("transaction_editor_save_button").fetchSemanticsNode()
                    false
                } catch (_: AssertionError) {
                    true
                }
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Lunch").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Lunch").performClick()
            composeRule.onNodeWithText("Transaction").assertIsDisplayed()

            composeRule.onNodeWithContentDescription("Edit transaction").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Save Changes").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Choose an account").fetchSemanticsNodes().isEmpty() &&
                    composeRule.onAllNodesWithText("Choose a category").fetchSemanticsNodes().isEmpty()
            }

            composeRule.onNodeWithTag("transaction_editor_note_input").performTextClearance()
            composeRule.onNodeWithTag("transaction_editor_note_input").performTextInput("Coffee")
            try {
                composeRule.onNodeWithTag("transaction_editor_save_button").performClick()
            } catch (_: AssertionError) {
                composeRule.onNodeWithTag("transaction_editor_save_button").performScrollTo().performClick()
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                try {
                    composeRule.onNodeWithTag("transaction_editor_save_button").fetchSemanticsNode()
                    false
                } catch (_: AssertionError) {
                    true
                }
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Coffee").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithContentDescription("Delete transaction").performClick()
            composeRule.onAllNodes(hasClickAction().and(hasText("Delete")))[0].performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("No transactions yet").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithText("No transactions yet").assertIsDisplayed()
        }
    }
}
