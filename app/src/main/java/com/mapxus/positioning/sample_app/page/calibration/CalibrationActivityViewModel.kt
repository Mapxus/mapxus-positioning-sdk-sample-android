package com.mapxus.positioning.sample_app.page.calibration

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.UserFeedbackType
import com.mapxus.positioning.api.calibration.MapxusCalibrationClient
import com.mapxus.positioning.api.calibration.MapxusCalibrationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Edison on 2023/9/15.
 * Describe:
 */
class CalibrationActivityViewModel(context: Application) :
    AndroidViewModel(context) {
    private var mapxusCalibrationClient: MapxusCalibrationClient? = null

    private var currentJob: Job? = null

    private val _calibrationActivityUiState = MutableStateFlow(CalibrationActivityUiState())
    val calibrationActivityUiState: StateFlow<CalibrationActivityUiState> =
        _calibrationActivityUiState.asStateFlow()


    private val _feedbackEvent: MutableSharedFlow<UserFeedbackInfo> =
        MutableSharedFlow()
    val feedbackEvent = _feedbackEvent.asSharedFlow()

    private val listener = object : MapxusCalibrationListener {
        override fun onCalibrationSuccess() {
            _calibrationActivityUiState.update {
                it.copy(
                    isShowLoading = false
                )
            }
            Toast.makeText(
                getApplication<Application>().applicationContext,
                "onCalibrationSuccess",
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onCalibrationStopped() {
            _calibrationActivityUiState.update {
                it.copy(
                    isShowLoading = false
                )
            }
        }

        override fun onFeedback(userFeedbackInfo: UserFeedbackInfo) {
            viewModelScope.launch {
                if (userFeedbackInfo.type == UserFeedbackType.ERROR_LEVEL3) {
                    _calibrationActivityUiState.update {
                        it.copy(
                            isShowLoading = false
                        )
                    }
                    stop()
                }
                _feedbackEvent.emit(userFeedbackInfo)
            }
        }
    }

    init {
        mapxusCalibrationClient =
            MapxusCalibrationClient.getInstance(context.applicationContext)
        mapxusCalibrationClient!!.addCalibrationListener(listener)
    }

    fun stop() {
        mapxusCalibrationClient!!.stop()
        _calibrationActivityUiState.update {
            it.copy(
                counterDownText = "",
                isRunning = false
            )
        }

        currentJob?.cancel()
    }

    fun reset() {
        mapxusCalibrationClient!!.reset()
    }

    fun updateCalibratorName(value: String) {
        _calibrationActivityUiState.update {
            it.copy(
                calibratorName = value
            )
        }
    }

    fun updateCalibratorHeight(value: String) {
        _calibrationActivityUiState.update {
            it.copy(
                calibratorHeight = value
            )
        }
    }


    fun start(): Boolean {

        if (_calibrationActivityUiState.value.calibratorHeight.isBlank() || _calibrationActivityUiState.value.calibratorName.isBlank()) {
            return false
        }

        currentJob = viewModelScope.launch {
            var count = 3
            repeat(3) {
                _calibrationActivityUiState.update {
                    it.copy(
                        counterDownText = "ready to start calibration $count"
                    )
                }
                delay(1000)
                count -= 1
            }

            count = 20
            repeat(20) {
                _calibrationActivityUiState.update {
                    it.copy(
                        counterDownText = "Calibration will be finished in $count seconds"
                    )
                }
                delay(1000)
                count -= 1
            }

            withContext(Dispatchers.Main) {
                _calibrationActivityUiState.update {
                    it.copy(
                        counterDownText = "",
                        isRunning = false
                    )
                }
                finishAndCalibrate(
                    _calibrationActivityUiState.value.calibratorName,
                    _calibrationActivityUiState.value.calibratorHeight.toDouble()
                )
            }
        }

        _calibrationActivityUiState.update {
            it.copy(
                isRunning = true
            )
        }

        mapxusCalibrationClient!!.start()
        return true
    }

    private fun finishAndCalibrate(calibratorName: String, calibratorHeight: Double) {
        mapxusCalibrationClient!!.finishAndCalibrate(calibratorName, calibratorHeight)
        _calibrationActivityUiState.update {
            it.copy(
                isShowLoading = true
            )
        }
    }

    override fun onCleared() {
        stop()
    }
}

data class CalibrationActivityUiState(
    val calibratorName: String = "",
    val calibratorHeight: String = "",
    val counterDownText: String = "",
    val isShowLoading: Boolean = false,
    val isRunning: Boolean = false,
)