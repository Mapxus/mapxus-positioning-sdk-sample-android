package com.mapxus.positioning.sample_app.page.uploadlog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mapxus.common.ui.lib.utils.DataStatus
import com.mapxus.positioning.api.issuereport.MapxusIssueReportClient
import com.mapxus.positioning.api.issuereport.MapxusIssueReportListener
import com.mapxus.positioning.api.issuereport.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class UploadLogViewModel(context: Application) : AndroidViewModel(context),
    MapxusIssueReportListener {

    private val issueReportClient: MapxusIssueReportClient =
        MapxusIssueReportClient.getInstance(context)

    private val _state: MutableStateFlow<UIState> =
        MutableStateFlow(UIState())
    val state: StateFlow<UIState> = _state

    init {
        issueReportClient.addIssueReportListener(this)
        getLogsFile()
    }

    fun getLogsFile() {
        _state.update {
            it.copy(
                records = DataStatus.Loading
            )
        }
        val logs = issueReportClient.listLocalRecords()

        _state.update {
            it.copy(
                records = if (logs.isNotEmpty())
                    DataStatus.Success(logs)
                else DataStatus.Failed("Empty")
            )
        }
    }

    fun uploadLogsFile(
        recordFile: Record,
    ) {
        _state.update {
            it.copy(
                uploadResult = DataStatus.Loading
            )
        }
        issueReportClient.uploadRecord(recordFile)
    }

    fun loggingId(): String {
        return issueReportClient.loggingId()
    }

    override fun onRecordUploadSuccess(record: Record) {
        _state.update {
            it.copy(
                uploadResult = DataStatus.Success(Unit)
            )
        }
        getLogsFile()
    }

    override fun onRecordUploadFailed(
        record: Record,
        errorMessage: String
    ) {
        _state.update {
            it.copy(
                uploadResult = DataStatus.Failed(errorMessage)
            )
        }
    }

    data class UIState(
        val records: DataStatus<List<Record>> = DataStatus.Idle,
        val uploadResult: DataStatus<Unit> = DataStatus.Idle,
    ) {
        val isLoading: Boolean
            get() =
                records is DataStatus.Loading ||
                        uploadResult is DataStatus.Loading
    }
}