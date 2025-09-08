package com.mapxus.positioning.sample_app.ui.component

import android.app.Activity
import android.app.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mapxus.positioning.sample_app.utils.createDialog

@Composable
fun LoadingDialog(
    isShow: Boolean,
) {
    val context = LocalContext.current
    val loadingDialog: Dialog = remember {
        context.createDialog(
            title = "Initializing...",
            message = "Please stand still and wait.",
            cancelable = true
        )
    }

    LaunchedEffect(isShow) {
        if (!isShow) {
            loadingDialog.dismiss()
        } else {
            if (!(context as Activity).isFinishing) {
                loadingDialog.show()
            }
        }
    }
}