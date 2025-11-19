package com.mapxus.positioning.sample_app.page.calibration

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mapxus.positioning.sample_app.ui.theme.AppTheme
import com.mapxus.positioning.sample_app.utils.logI

private const val TAG: String = "CalibrationActivity"

class CalibrationActivity : AppCompatActivity() {

    private val viewModel: CalibrationActivityViewModel by viewModels {
        viewModelFactory {
            initializer {
                CalibrationActivityViewModel(
                    this@CalibrationActivity.application,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        " ============================= 退出CalibrationActivity =============================".logI(
            TAG
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        " ============================= 进入CalibrationActivity =============================".logI(
            TAG
        )

        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                CalibrationMainScreen(viewModel)
            }
        }
    }


    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }
}