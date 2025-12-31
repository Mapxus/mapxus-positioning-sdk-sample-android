package com.mapxus.positioning.sample_app.page.uploadlog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mapxus.common.ui.lib.utils.DataStatus
import com.mapxus.positioning.api.issuereport.MapxusIssueReportClient
import com.mapxus.positioning.api.issuereport.MapxusIssueReportListener
import com.mapxus.positioning.api.issuereport.Record
import com.mapxus.positioning.sample_app.utils.logI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class UploadLogViewModel(context: Application) : AndroidViewModel(context) {

    private val bugReportListener: MapxusIssueReportListener =
        object : MapxusIssueReportListener {
            override fun onRecordUploadSuccess(record: Record) {
                "onRecordUploadSuccess record $record ".logI()
                _state.update {
                    it.copy(
                        uploadResult = DataStatus.Success(Unit),
                        progress = 0f,
                        writtenSize = 0L,
                        totalSize = 0L
                    )
                }
                getLogsFile()
            }

            override fun onRecordUploadFailed(record: Record, errorMessage: String) {
                "onRecordUploadFailed record $record , errorMessage $errorMessage ".logI()
                _state.update {
                    it.copy(
                        uploadResult = DataStatus.Failed(errorMessage),
                        progress = 0f,
                        writtenSize = 0L,
                        totalSize = 0L
                    )
                }
                getLogsFile()
            }

            override fun onUploadProgressUpdate(
                record: Record,
                progress: Float,
                writtenSize: Long,
                totalSize: Long
            ) {
                "onUploadProgressUpdate record $record , progress $progress ,writtenSize $writtenSize , totalSize $totalSize".logI()
                _state.update {
                    it.copy(
                        progress = progress,
                        writtenSize = writtenSize,
                        totalSize = totalSize,
                    )
                }
            }
        }

    private val issueReportClient: MapxusIssueReportClient =
        MapxusIssueReportClient.getInstance(context).apply {
            addIssueReportListener(bugReportListener)
        }

    private val _state: MutableStateFlow<UIState> =
        MutableStateFlow(UIState())
    val state: StateFlow<UIState> = _state

    init {
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
                records = if (logs.isNotEmpty()) DataStatus.Success(logs) else DataStatus.Failed("Empty")
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

    override fun onCleared() {
        super.onCleared()
        issueReportClient.removeIssueReportListener(bugReportListener)
    }

    data class UIState(
        val records: DataStatus<List<Record>> = DataStatus.Idle,
        val uploadResult: DataStatus<Unit> = DataStatus.Idle,
        val progress: Float = 0.00f,
        val totalSize: Long = 0L,
        val writtenSize: Long = 0L,
    ) {
        val isLoading: Boolean
            get() =
                records is DataStatus.Loading ||
                        uploadResult is DataStatus.Loading
    }
}