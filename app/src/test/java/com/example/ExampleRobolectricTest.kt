package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.UiState
import com.example.model.PrivateAccount
import com.example.model.TaviqoCatalog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GiftsAndWalletScreen
import com.example.ui.theme.TaviqoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name matches Taviqo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Taviqo", appName)
    }

    @Test
    fun `gift catalog computes exact 70_30 creator and platform split`() {
        TaviqoCatalog.gifts.forEach { gift ->
            assertEquals(gift.coinCost, gift.creatorNetCoins + gift.platformFeeCoins)
            assertEquals((gift.coinCost * 70L) / 100L, gift.creatorNetCoins)
            assertTrue(gift.coinCost > 0L)
        }
        val supernova = TaviqoCatalog.gifts.first { it.id == "taviqo_nova" }
        assertEquals(1000L, supernova.coinCost)
        assertEquals(700L, supernova.creatorNetCoins)
        assertEquals(300L, supernova.platformFeeCoins)
        assertTrue(supernova.isExclusive)
    }

    @Test
    fun `auth screen renders TAVIQO brand slogan and real Google sign in button`() {
        composeRule.setContent {
            TaviqoTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("TAVIQO").assertIsDisplayed()
        composeRule.onNodeWithText("El mundo es tu escenario.").assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `wallet screen displays real zero balance without simulated coins`() {
        composeRule.setContent {
            TaviqoTheme {
                GiftsAndWalletScreen(
                    currentUserId = "user_real_1",
                    walletState = UiState.Success(
                        PrivateAccount(
                            id = "wallet",
                            userId = "user_real_1",
                            coinBalance = 0L,
                            creatorEarningsCoins = 0L
                        )
                    ),
                    giftHistoryState = UiState.Success(emptyList()),
                    purchasesState = UiState.Success(emptyList()),
                    payoutsState = UiState.Success(emptyList()),
                    onCreateCoinOrder = { _, _ -> },
                    onRequestPayout = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tienda y Regalos TAVIQO").assertIsDisplayed()
        composeRule.onNodeWithText("Saldo de Monedas").assertIsDisplayed()
        composeRule.onNodeWithTag("request_payout_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
