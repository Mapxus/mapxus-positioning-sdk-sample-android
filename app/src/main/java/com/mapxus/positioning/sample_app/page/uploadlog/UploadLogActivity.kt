package com.mapxus.positioning.sample_app.page.uploadlog

import android.os.Bundle
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapxus.common.ui.lib.utils.DataStatus
import com.mapxus.positioning.api.issuereport.Record
import com.mapxus.positioning.sample_app.R
import com.mapxus.positioning.sample_app.ui.component.CommonTopAppBar
import com.mapxus.positioning.sample_app.ui.component.LoadingCircle
import com.mapxus.positioning.sample_app.ui.component.LoadingCircleWithProgressBar
import com.mapxus.positioning.sample_app.ui.component.MapxusToast
import com.mapxus.positioning.sample_app.ui.component.MapxusToastData
import com.mapxus.positioning.sample_app.ui.component.TitleText
import com.mapxus.positioning.sample_app.ui.icons.CloudUpload
import com.mapxus.positioning.sample_app.ui.theme.AppTheme
import com.mapxus.positioning.sample_app.utils.HHmmss
import com.mapxus.positioning.sample_app.utils.bytesToMb
import com.mapxus.positioning.sample_app.utils.commonToMapxusToastData
import com.mapxus.positioning.sample_app.utils.keepTwo
import com.mapxus.positioning.sample_app.utils.logD
import kotlinx.coroutines.launch

private const val TAG: String = "UploadLogActivity"

class UploadLogActivity : AppCompatActivity() {

    private val viewModel: UploadLogViewModel by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        " ============================= Exit LogsActivity =============================".logD(TAG)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        " ============================= Enter LogsActivity =============================".logD(TAG)
        setContent {
            AppTheme {
                MainContent(viewModel)
            }
        }
    }
}

@Preview
@Composable
private fun MainContent(viewModel: UploadLogViewModel = viewModel()) {
    val uiState by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    ShowUiTips(uiState.uploadResult, snackbarHostState)

    if (uiState.isLoading) {
        if (uiState.totalSize == 0L) {
            LoadingCircle()
        } else {
            LoadingCircleWithProgressBar(
                true,
                uiState.progress,
                uiState.writtenSize,
                uiState.totalSize
            )
        }
    }

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
                        title = it.visuals.actionLabel!!,
                        message = it.visuals.message,
                        color,
                        iconRes
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TitleText(
                text = "Logging Id",
                modifier = Modifier.padding(top = 20.dp)
            )

            SelectionContainer {
                Text(
                    text = viewModel.loggingId(),
                    textAlign = TextAlign.Center,
                    color = Color(0x8a000000),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                )
            }

            TitleText(
                text = "Log Files List",
                modifier = Modifier.padding(top = 20.dp)
            )

            when (uiState.records) {
                is DataStatus.Failed -> {
                    val errorMessage =
                        (uiState.records as DataStatus.Failed).errorMessage
                    TitleText(
                        text = "Logs not found\n$errorMessage",
                        modifier = Modifier.padding(top = 30.dp)
                    )
                }

                is DataStatus.Success -> {
                    val data = (uiState.records as DataStatus.Success).data
                    Spacer(modifier = Modifier.height(10.dp))
                    LogsList(
                        data,
                    ) { log ->
                        viewModel.uploadLogsFile(
                            recordFile = log,
                        )
                    }
                }

                else -> {}
            }

        }
    }

}

@Composable
private fun ShowUiTips(
    dataStatus: DataStatus<Unit>,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(dataStatus) {
        scope.launch {
            when (dataStatus) {
                is DataStatus.Failed -> {
                    "Failed" to "Failed. ${dataStatus.errorMessage}"
                }

                is DataStatus.Success -> {
                    "Success" to "Success"
                }

                else -> {
                    null
                }
            }?.let {
                snackbarHostState.showSnackbar(
                    actionLabel = it.first,
                    message = it.second,
                    duration = SnackbarDuration.Short
                )
            }

        }
    }
}

@Preview
@Composable
private fun LogsList(
    logsFiles: List<Record> = emptyList(),
    onUploadButtonClicked: (Record) -> Unit = {},
) {
    LazyColumn {
        itemsIndexed(items = logsFiles, itemContent = { _, log ->
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(color = Color.LightGray),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(

                ) {
                    LogItem(
                        text = log.date.toString(),
                        textSize = 15
                    )
                    LogItem(
                        text = "start ${log.startTimestamp.HHmmss()} , end ${log.endTimestamp.HHmmss()} , size ${
                            log.fileSize.bytesToMb().keepTwo()
                        } MB",
                        textSize = 10
                    )
                }

                IconButton(
                    onClick = {
                        onUploadButtonClicked(log)
                    },
                ) {
                    Icon(
                        modifier = Modifier.padding(10.dp),
                        imageVector = CloudUpload,
                        contentDescription = null
                    )
                }
            }

        })
    }
}

@Composable
private fun LogItem(
    text: String,
    textSize: Int,
) {
    Text(
        text = text,
        textAlign = TextAlign.Start,
        fontSize = textSize.sp,
        color = Color(0xFF545454),
        modifier = Modifier.padding(start = 20.dp)
    )
}