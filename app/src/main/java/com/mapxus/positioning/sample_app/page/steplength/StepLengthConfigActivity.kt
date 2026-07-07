package com.mapxus.positioning.sample_app.page.steplength

import android.os.Bundle
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapxus.positioning.sample_app.R
import com.mapxus.positioning.sample_app.ui.component.CommonTopAppBar
import com.mapxus.positioning.sample_app.ui.component.CurrentStepLength
import com.mapxus.positioning.sample_app.ui.component.InputStepLength
import com.mapxus.positioning.sample_app.ui.theme.AppTheme
import com.mapxus.positioning.sample_app.utils.keepTwo
import com.mapxus.positioning.sample_app.utils.logI

private const val TAG: String = "StepLengthConfigActivity"

class StepLengthConfigActivity : AppCompatActivity() {

    private val viewModel: StepLengthConfigActivityViewModel by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        " ============================= Exit StepLengthConfigActivity =============================".logI(
            TAG
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        " ============================= Enter StepLengthConfigActivity =============================".logI(
            TAG
        )

        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainContent(viewModel)
            }
        }
    }
}

@Preview
@Composable
private fun MainContent(viewModel: StepLengthConfigActivityViewModel = viewModel()) {

    val uiState: StepLengthConfigActivityUiState by viewModel.uiState.collectAsState()

    val dispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            CommonTopAppBar(
                text = stringResource(R.string.step_length_config)
            ) {
                dispatcher?.onBackPressed()
            }
        },
    ) { paddingValue ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .padding(top = 100.dp)
        ) {
            InputStepLength(
                stepLengthTextFieldValue = uiState.stepLengthTextFieldValue,
                setStepLengthResult = uiState.setStepLengthResult,
                onValueChange = { value ->
                    viewModel.updateStepLengthTextFieldValue(
                        value
                    )
                },
                onDone = {
                    viewModel.setStepLength()
                }
            )

            CurrentStepLength(uiState.stepLength?.keepTwo() ?: "")
        }
    }
}