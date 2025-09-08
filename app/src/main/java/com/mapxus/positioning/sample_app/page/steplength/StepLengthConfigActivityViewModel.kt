package com.mapxus.positioning.sample_app.page.steplength

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mapxus.positioning.api.positioning.MapxusPositioningClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Created by Edison on 2023/9/15.
 * Describe:
 */
class StepLengthConfigActivityViewModel(context: Application) :
    AndroidViewModel(context) {

    private val mapxusPositioningClient =
        MapxusPositioningClient.getInstance(context.applicationContext)

    private val _uiState = MutableStateFlow(StepLengthConfigActivityUiState())
    val uiState: StateFlow<StepLengthConfigActivityUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                stepLength = mapxusPositioningClient.stepLength
            )
        }
    }

    fun setStepLength() {
        val currentStepLengthTextField = _uiState.value.stepLengthTextFieldValue.toDoubleOrNull()
        val result =
            if (currentStepLengthTextField == null) false else mapxusPositioningClient.setStepLength(
                currentStepLengthTextField
            )

        _uiState.update {
            it.copy(
                setStepLengthResult = result
            )
        }

        if (result) {
            _uiState.update {
                it.copy(
                    stepLength = mapxusPositioningClient.stepLength
                )
            }
        }
    }

    fun updateStepLengthTextFieldValue(value: String) {
        _uiState.update {
            it.copy(
                stepLengthTextFieldValue = value
            )
        }
    }
}

data class StepLengthConfigActivityUiState(
    val stepLength: Double? = null,
    val setStepLengthResult: Boolean? = null,
    val stepLengthTextFieldValue: String = "",
)