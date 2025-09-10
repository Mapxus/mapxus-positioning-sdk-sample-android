package com.mapxus.positioning.sample_app.page.positioning

import android.os.Bundle
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mapxus.positioning.sample_app.R
import com.mapxus.positioning.sample_app.ui.component.CommonTopAppBar
import com.mapxus.positioning.sample_app.ui.theme.AppTheme
import com.mapxus.positioning.sample_app.utils.logI

/**
 * 承载normal positioning and wheelchair sampling ui
 *
 */
private const val TAG = "PositioningActivity"

open class PositioningActivity : AppCompatActivity() {

    private val viewModel by viewModels<PositioningActivityViewModel> {
        viewModelFactory {
            initializer {
                PositioningActivityViewModel(
                    this@PositioningActivity.application
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        " ============================= 退出 PositioningActivity =============================".logI(
            TAG
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        " ============================= 进入 PositioningActivity =============================".logI(
            TAG
        )
        val title = "${getString(R.string.app_name)} ${com.mapxus.positioning.sample_app.BuildConfig.VERSION_NAME}"

        onBackPressedDispatcher.addCallback {
            finish()
        }

        setContent {
            AppTheme {
                MainContent(
                    title = title,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    title: String,
    viewModel: PositioningActivityViewModel
) {
    val dispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            CommonTopAppBar(
                text = title
            ) {
                dispatcher?.onBackPressed()
            }
        },
    ) { paddingValue ->
        Surface(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
        ) {
            PositioningScreen(
                viewModel = viewModel,
            )
        }

    }
}