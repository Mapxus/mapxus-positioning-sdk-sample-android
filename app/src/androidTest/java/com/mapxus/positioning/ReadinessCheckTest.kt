package com.mapxus.positioning

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.mapxus.positioning.api.positioning.MapxusPositioningClient
import com.mapxus.positioning.sample_app.page.clientapi.MainContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 *
 * https://www.notion.so/SDK-V2-218ec0be191580e99655e174687e6894?source=copy_link#289ec0be19158018a7c0cd20e54b6992
 *
 * UI Test ，测试Check Readiness功能
 *
 */
@RunWith(AndroidJUnit4::class)
internal class ReadinessCheckTest {
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Compose test rule
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mapxusPositioningClient = MapxusPositioningClient.getInstance(context)

    /**
     * 测试Check Readiness功能
     *
     * 超时代表测试不成功
     *
     */
    @Test
    fun test_check_readiness() {
        //Arrange
        //在定位界面注入composeTestRule
        composeTestRule.setContent {
            MainContent(mapxusPositioningClient) { }
        }
        //Act
        composeTestRule.onNodeWithText("Check Readiness").performClick()
        composeTestRule.onNodeWithText("Check Positioning Readiness").performClick()

        //等待check readiness finish ， 时间5s超时
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Success").onFirst().isDisplayed()
        }
    }
}