package com.mapxus.positioning.sample_app.page.calibration


import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mapxus.positioning.sample_app.R
import com.mapxus.positioning.sample_app.ui.component.ButtonGrid
import com.mapxus.positioning.sample_app.ui.component.CommonTopAppBar
import com.mapxus.positioning.sample_app.ui.component.LoadingCircle
import com.mapxus.positioning.sample_app.ui.component.MapxusToast
import com.mapxus.positioning.sample_app.ui.component.MapxusToastData
import com.mapxus.positioning.sample_app.ui.component.OutlinedTextFieldCommon
import com.mapxus.positioning.sample_app.utils.commonToMapxusToastData
import com.mapxus.positioning.sample_app.utils.logI
import kotlinx.coroutines.launch

private const val TAG = "CalibrationMainScreen"

@Composable
fun CalibrationMainScreen(
    viewModel: CalibrationActivityViewModel,
) {

    val dispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val calibrationActivityUiState by viewModel.calibrationActivityUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    LoadingCircle(calibrationActivityUiState.isShowLoading)

    HandlerFeedbackEvent(viewModel, snackbarHostState)

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            CommonTopAppBar(
                text = stringResource(R.string.upload_record)
            ) {
                dispatcher?.onBackPressed()
            }
        },
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier
                    .padding(bottom = 50.dp),
                hostState = snackbarHostState,
            ) {
                val (color, iconRes) = it.visuals.actionLabel!!.commonToMapxusToastData()
                MapxusToast(
                    data = MapxusToastData(
                        color = color,
                        iconRes = iconRes,
                        title = it.visuals.actionLabel!!,
                        message = it.visuals.message
                    )
                )
            }
        }
    ) { paddingValues ->
        val buttonList by remember(calibrationActivityUiState.isRunning) {
            derivedStateOf {
                val result: MutableList<Pair<String, () -> Unit>> = mutableListOf()
                if (calibrationActivityUiState.isRunning) {
                    result.add("abort" to {
                        viewModel.stop()
                    })
                } else {
                    result.add(
                        "start" to {
                            if (!viewModel.start()) {
                                Toast.makeText(
                                    context,
                                    "Please input height first",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    )
                    result.add("reset" to {
                        val resetResult = viewModel.reset()
                        val message = if (resetResult) {
                            "Reset Success"
                        } else {
                            "Reset Failed ，please stop calibrate first"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    })
                }
                result
            }
        }

        Column(modifier = Modifier.padding(paddingValues)) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextFieldCommon(
                    calibrationActivityUiState.calibratorName,
                    "Calibrator Name",
                    KeyboardType.Text
                ) {
                    viewModel.updateCalibratorName(it)
                }
                OutlinedTextFieldCommon(
                    calibrationActivityUiState.calibratorHeight,
                    "Calibrator Height(cm)",
                    fillMaxWidth = 1f
                ) {
                    viewModel.updateCalibratorHeight(it)
                }
            }

            ButtonGrid(
                buttonList = buttonList,
            )


            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = calibrationActivityUiState.counterDownText)
            }

        }
    }
}

@Composable
private fun HandlerFeedbackEvent(
    viewModel: CalibrationActivityViewModel,
    snackbarHostState: SnackbarHostState
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            launch {
                viewModel.feedbackEvent.collect { feedbackInfo ->
                    "receive feedback $feedbackInfo".logI(TAG)
                    snackbarHostState.showSnackbar(
                        actionLabel = feedbackInfo.type.name,
                        message = feedbackInfo.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}