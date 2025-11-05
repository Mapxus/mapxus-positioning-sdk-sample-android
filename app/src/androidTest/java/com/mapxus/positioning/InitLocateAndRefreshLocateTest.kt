package com.mapxus.positioning

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.mapxus.positioning.api.positioning.LatLng
import com.mapxus.positioning.api.positioning.MapxusFloor
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode
import com.mapxus.positioning.sample_app.page.positioning.PositioningActivityViewModel
import com.mapxus.positioning.sample_app.page.positioning.PositioningScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 *
 * https://www.notion.so/SDK-V2-218ec0be191580e99655e174687e6894?source=copy_link#289ec0be19158060829ccd2a882d2284
 *
 * UI Test ，测试给定位置定位功能 以及刷新位置
 *
 */
@RunWith(AndroidJUnit4::class)
internal class InitLocateAndRefreshLocateTest {
    /**
     * Grant permission rule
     */
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    /**
     * Compose test rule
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 给定位置
     */
    private val mapxusLocation = MapxusLocation(
        "3fa9c603f76146da9921d0c47920adff",
        "vivocity_foshan_d3fmv9",
        MapxusFloor(
            "9b7e40e7862d4b1cb174ad4661b175e1",
            0,
            "1",
            MapxusFloor.Type.FLOOR
        ),
        LatLng.fromLatLng(23.035187076239993, 113.18218107183202),
        3f,
        System.currentTimeMillis()
    )

    /**
     * View model
     */
    private val viewModel = PositioningActivityViewModel(
        ApplicationProvider.getApplicationContext()
    ).also {
        //提前设定位置
        it.customLocation = mapxusLocation
    }

    /**
     * Test_custom_location_positioning 以及刷新位置 行人模式
     *
     */
    @Test
    fun test_custom_location_positioning_and_refresh_location_PEDESTRIAN() {
        test_custom_location_positioning_and_refresh_location(UserMode.PEDESTRIAN)
    }

    /**
     * Test_custom_location_positioning 以及刷新位置 轮椅模式
     *
     */
    @Test
    fun test_custom_location_positioning_and_refresh_location_WHEELCHAIR() {
        test_custom_location_positioning_and_refresh_location(UserMode.WHEELCHAIR)
    }

    /**
     * Test_custom_location_positioning 以及刷新位置
     *
     * 超时代表测试不成功
     *
     */
    fun test_custom_location_positioning_and_refresh_location(userMode: UserMode) {
        //==========================第一部分 ，给定位置定位====================================================
        //Arrange
        //在定位界面注入composeTestRule
        viewModel.mapxusPositioningProvider
        composeTestRule.setContent {
            PositioningScreen(
                viewModel = viewModel,
            )
        }
        //Act
        //需要等待界面加载元素 ， 元素出现后才开始后续操作， 时间5s超时
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onNodeWithText("Positioning Client State -> ").isDisplayed()
        }
        if (userMode == UserMode.WHEELCHAIR) {
            if (composeTestRule.onNodeWithText("Current User Mode: ${UserMode.PEDESTRIAN}").isDisplayed()) {
                composeTestRule.onNodeWithText("Current User Mode: ${UserMode.PEDESTRIAN}").performClick()
            }
        } else {
            if (composeTestRule.onNodeWithText("Current User Mode: ${UserMode.WHEELCHAIR}").isDisplayed()) {
                composeTestRule.onNodeWithText("Current User Mode: ${UserMode.WHEELCHAIR}").performClick()
            }
        }
        //点击自定义定位触发UI变化
        composeTestRule.onNodeWithText("Customize Location").performClick()
        //等待界面出现 ‘开始自定义定位’按钮， 时间5s超时
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onNodeWithText("Start Positioning").isDisplayed()
        }
        //开始自定义定位
        composeTestRule.onNodeWithText("Start Positioning").performClick()
        //等待定位完成后的建筑搜索 ， 时间10s超时
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onNodeWithText("1 , 怡丰城（DEFAULT）").isDisplayed()
        }
        //判断条件
        composeTestRule.onNodeWithText(PositioningState.RUNNING.toString()).assertIsDisplayed()
        if (userMode == UserMode.WHEELCHAIR) {
            composeTestRule.onNodeWithText(UserMode.WHEELCHAIR.toString()).assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText(UserMode.PEDESTRIAN.toString()).assertIsDisplayed()
        }
        //==========================第二部分 ，刷新位置====================================================
        //core sdk 蓝点已启动
        assert(viewModel.mapxusPositioningProvider.isStarted())
        //记下刷新前的位置
        val lastLocation = viewModel.mapxusPositioningProvider.lastLocation
        //判定是否给定位置的建筑
        assert(lastLocation?.building == mapxusLocation.buildingId)
        //点击刷新位置
        composeTestRule.onNodeWithText("Refresh Location").performClick()
        //等待定位完成后的建筑搜索 ， 时间10s超时
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onNodeWithText("15 , 嘉邦国金中心3座").isDisplayed()
        }
        //刷新后的位置应为嘉邦3座
        val currentLocation = viewModel.mapxusPositioningProvider.lastLocation
        assert(lastLocation?.building != currentLocation?.building)
        assert(currentLocation?.building == "756f2ea12d414884b5774a40db81995a")
    }
}