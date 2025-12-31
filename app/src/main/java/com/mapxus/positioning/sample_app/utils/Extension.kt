package com.mapxus.positioning.sample_app.utils

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapxus.map.mapxusmap.api.common.MultilingualObject
import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.UserFeedbackType
import com.mapxus.positioning.sample_app.ui.component.ErrorBackground
import com.mapxus.positioning.sample_app.ui.component.InfoBackground
import com.mapxus.positioning.sample_app.ui.component.MapxusToastData
import com.mapxus.positioning.sample_app.ui.component.SuccessBackground
import com.mapxus.positioning.sample_app.ui.component.WarningBackground
import com.mapxus.positioning.sample_app.ui.icons.Error
import com.mapxus.positioning.sample_app.ui.icons.Info
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Edison on 2023/11/20.
 * Describe:
 */

fun String.isHeightValid() = this.isNotBlank() && this != "0.0"

const val yyyyMMdd = "yyyy-MM-dd"
const val HHmmss = "HH:mm:ss"
const val Full_Date = "yyyy-MM-dd HH:mm:ss"

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun Long.bytesToMb(): Double {
    return this / (1024.0 * 1024.0)
}

fun Double.keepOne(): String = "%.1f".format(this)
fun Double.keepTwo(): String = "%.2f".format(this)
fun Double.keepThree(): String = "%.3f".format(this)
fun Double.keepFour(): String = "%.4f".format(this)
fun Long.HHmmss(): String = formatDate(HHmmss)
fun Long.yyyyMMdd(): String = formatDate(yyyyMMdd)
fun Long.formatDate(formatType: String): String =
    SimpleDateFormat(formatType, Locale.getDefault()).format(this)

fun Context.showToast(text: String) {
    Toast.makeText(
        this,
        text,
        Toast.LENGTH_SHORT
    ).show()
}

fun Context.createDialog(
    title: String = "",
    message: String = "",
    cancelable: Boolean = false,
    onPositiveButtonClick: ((DialogInterface) -> Unit)? = null
): AlertDialog {
    val dialogBuilder =
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
    onPositiveButtonClick?.let {
        dialogBuilder.setPositiveButton("Ok") { dialog: DialogInterface?, _: Int ->
            it(dialog!!)
        }
    }
    val dialog = dialogBuilder.create()
    return dialog
}

fun Context.showFinishedDialog(
    message: String,
    onPositiveButtonClick: (DialogInterface) -> Unit = {}
) {
    val dialog = createDialog(
        title = "Finish",
        message = message,
        onPositiveButtonClick = onPositiveButtonClick
    )
    dialog.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps =
        false
}

fun Context.showWarningDialog(message: String, onPositiveButtonClick: () -> Unit = {}) {
    val dialog = MaterialAlertDialogBuilder(this).setTitle(
        "Warning"
    ).setMessage(message)
        .setCancelable(false)
        .setPositiveButton("Ok") { _: DialogInterface?, _: Int ->
            onPositiveButtonClick()
        }.create()
    dialog.show()
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps =
        false
}

fun String.commonToMapxusToastData(): Pair<Color, ImageVector> {
    return when {
        this.contains("Success", true) -> SuccessBackground to Icons.Filled.CheckCircle
        else -> ErrorBackground to Icons.Filled.Error
    }
}


suspend fun SnackbarHostState.userFeedbackInfoToSnackbar(userFeedbackInfo: UserFeedbackInfo?) {
    this.showSnackbar(
        actionLabel = userFeedbackInfo?.type?.name ?: "Success",
        message = userFeedbackInfo?.message ?: "Success",
        duration = SnackbarDuration.Short
    )
}

fun String.userFeedbackInfoToMapxusToastData(): Pair<Color, ImageVector> {
    return when (this) {
        UserFeedbackType.ERROR_LEVEL1.name, UserFeedbackType.ERROR_LEVEL2.name -> WarningBackground to Icons.Filled.Error
        UserFeedbackType.ERROR_LEVEL3.name -> ErrorBackground to Icons.Filled.Error
        else -> InfoBackground to Info
    }
}

fun UserFeedbackInfo.toMapxusToastData(): MapxusToastData {
    val (color, iconRes) =
        when (this.type) {
            UserFeedbackType.ERROR_LEVEL1, UserFeedbackType.ERROR_LEVEL2 -> WarningBackground to Icons.Filled.Error
            UserFeedbackType.ERROR_LEVEL3 -> ErrorBackground to Icons.Filled.Error
            else -> InfoBackground to Info
        }
    return MapxusToastData(
        title = this.type.name,
        message = this.message,
        color = color,
        iconRes = iconRes
    )
}

fun MultilingualObject<String>.getName(): String = this.default