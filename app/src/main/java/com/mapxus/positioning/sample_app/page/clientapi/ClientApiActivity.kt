package com.mapxus.positioning.sample_app.page.clientapi

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapxus.common.ui.lib.utils.DeviceUtils
import com.mapxus.positioning.api.positioning.MapxusPositioningClient
import com.mapxus.positioning.sample_app.BuildConfig
import com.mapxus.positioning.sample_app.R
import com.mapxus.positioning.sample_app.page.positioning.PositioningActivity
import com.mapxus.positioning.sample_app.page.readiness.CheckReadinessScreen
import com.mapxus.positioning.sample_app.page.steplength.StepLengthConfigActivity
import com.mapxus.positioning.sample_app.page.uploadlog.UploadLogActivity
import com.mapxus.positioning.sample_app.ui.component.CommonTopAppBar
import com.mapxus.positioning.sample_app.ui.component.MapxusToast
import com.mapxus.positioning.sample_app.ui.component.MapxusToastData
import com.mapxus.positioning.sample_app.ui.component.TextButton
import com.mapxus.positioning.sample_app.ui.theme.AppTheme
import com.mapxus.positioning.sample_app.utils.logI
import com.mapxus.positioning.sample_app.utils.showToast
import com.mapxus.positioning.sample_app.utils.showWarningDialog
import com.mapxus.positioning.sample_app.utils.userFeedbackInfoToMapxusToastData
import com.mapxus.positioning.sample_app.utils.userFeedbackInfoToSnackbar
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.launch

private object Screen {
    const val MAIN = "main"
    const val READINESS_SCREEN = "readiness_screen"
}

private const val TAG = "ClientApiActivity"

class ClientApiActivity : AppCompatActivity() {
    private val mapxusPositioningClient by lazy {
        MapxusPositioningClient.getInstance(applicationContext)
    }

    private val startActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            showToast("Click again")
        }

    private val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private var isAllGranted = false

    private fun startActivityWithCheck(launch: () -> Unit) {
        if (DeviceUtils.isWifiThrottlingEnable(this)) {
            if (!isAllGranted) {
                checkPermission()
            } else {
                launch()
            }
        } else {
            showWarningDialog(
                message = "You are not in Wi-Fi throttling enabled mode , please go to open it ",
                onPositiveButtonClick = {
                    startDevelopmentActivity()
                }
            )
        }
    }

    private fun startDevelopmentActivity() {
        try {
            startActivityLauncher.launch(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivityLauncher.launch(Intent().apply {
                    component = ComponentName(
                        "com.android.settings", "com.android.settings.DevelopmentSettings"
                    )
                    action = "android.intent.action.View"
                })
            } catch (e1: Exception) {
                try {
                    startActivityLauncher.launch(Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS")) //部分手机采用这种方式跳转
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun checkPermission() {
        PermissionX.init(this).permissions(
            permissions
        ).onForwardToSettings { scope, deniedList ->
            scope.showForwardToSettingsDialog(
                deniedList,
                "You need to allow permissions in Settings manually",
                "OK",
            )
        }.request { allGranted, _, _ ->
            if (allGranted) {
                isAllGranted = true
            } else {
                checkPermission()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = getString(R.string.app_name)
        "App info : $appName ${BuildConfig.VERSION_NAME}   ,${BuildConfig.BUILD_TYPE} ".logI(
            TAG
        )
        "Device info : ${Build.DEVICE} ${Build.MODEL} ${Build.BRAND} ".logI(TAG)
        checkPermission()
        setContent {
            AppTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    topBar = {
                        CommonTopAppBar(
                            text = stringResource(R.string.app_name)
                        )
                    },
                    snackbarHost = {
                        SnackbarHost(
                            modifier = Modifier
                                .padding(bottom = 50.dp),
                            hostState = snackbarHostState,
                        ) {
                            val (color, iconRes) = it.visuals.actionLabel!!.userFeedbackInfoToMapxusToastData()
                            MapxusToast(
                                data = MapxusToastData(
                                    title = it.visuals.actionLabel!!,
                                    message = it.visuals.message,
                                    color,
                                    iconRes
                                )
                            )
                        }
                    },
                    bottomBar = {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            Column {
                                Text(text = "SDK Version ${BuildConfig.VERSION_NAME}")
                                Text(text = "App Version ${BuildConfig.VERSION_NAME}")
                            }
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxSize()
                ) { innerPadding ->
                    NavHost(navController = navController, startDestination = Screen.MAIN) {
                        composable(Screen.READINESS_SCREEN) {
                            CheckReadinessScreen(
                                modifier = Modifier.padding(innerPadding),
                                onCheckPositioningReadinessClick = {
                                    mapxusPositioningClient.checkReadiness {
                                        scope.launch {
                                            snackbarHostState.userFeedbackInfoToSnackbar(it.takeIf { it.isNotEmpty() }
                                                ?.first())
                                        }
                                    }
                                },
                            )
                        }

                        composable(Screen.MAIN) {
                            ClientAPITestScreen(
                                modifier = Modifier.padding(innerPadding),
                                onPositioningButtonClick = {
                                    startActivityWithCheck {
                                        startActivity(
                                            Intent(
                                                this@ClientApiActivity,
                                                PositioningActivity::class.java
                                            )
                                        )
                                    }
                                },
                                onCheckReadinessClick = {
                                    navController.navigate(Screen.READINESS_SCREEN) {
                                        popUpTo(Screen.MAIN)
                                    }

                                },
                                onUploadRecordButtonClick = {
                                    startActivity(
                                        Intent(
                                            this@ClientApiActivity,
                                            UploadLogActivity::class.java
                                        )
                                    )
                                },
                                onStepLengthConfigClick = {
                                    startActivity(
                                        Intent(
                                            this@ClientApiActivity,
                                            StepLengthConfigActivity::class.java
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientAPITestScreen(
    modifier: Modifier = Modifier,
    onPositioningButtonClick: () -> Unit,
    onCheckReadinessClick: () -> Unit,
    onUploadRecordButtonClick: () -> Unit,
    onStepLengthConfigClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {

        TextButton(text = stringResource(id = R.string.positioning), onClick = {
            onPositioningButtonClick()
        })

        TextButton(text = stringResource(R.string.check_readiness), onClick = {
            onCheckReadinessClick()
        })

        TextButton(text = stringResource(R.string.upload_record), onClick = {
            onUploadRecordButtonClick()
        })

        TextButton(text = stringResource(R.string.step_length_config), onClick = {
            onStepLengthConfigClick()
        })
    }
}