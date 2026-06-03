package com.tyron.code.ui.file

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.net.toUri

object FilePermissionChecker {
    fun check(ctx: Activity) {
        if (isA11()) {
            if (!Environment.isExternalStorageManager()) {
                MaterialAlertDialogBuilder(ctx).apply {
                    setTitle("Storage Permission required")
                    setMessage("CodeAssist-Pro use this permission to Create projects and manage them in the External Storage scope, storage/emulated/0/CodeAssistProjects folder")
                    setNegativeButton("Exit", { d, int ->
                      ctx.finishAffinity()
                    })
                    setPositiveButton("Agree",{d,int->
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        intent.setData("package:${ctx.packageName}".toUri())
                        ctx.startActivity(intent)
                    })
                }.create().show()
            }
        }
    }
    fun isA11(): Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

}