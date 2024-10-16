package com.rk.xededitor.MainActivity.handlers

import android.os.Build
import com.rk.settings.PreferencesData
import com.rk.settings.PreferencesKeys
import com.rk.xededitor.App
import com.rk.xededitor.R
import com.rk.xededitor.rkUtils
import com.rk.xededitor.rkUtils.getString

object VersionChangeHandler {
    fun handle(app: App) {
        val previousVersionCode = PreferencesData.getString(PreferencesKeys.VERSION_CODE, "")

        val pkgInfo = app.packageManager.getPackageInfo(app.packageName, 0)

        val currentVersionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()
            }

        if (previousVersionCode.isEmpty()) {
            // user maybe updating from 2.6.0
            // clear data
            app.filesDir.parentFile?.deleteRecursively()
            PreferencesData.setString(PreferencesKeys.VERSION_CODE, currentVersionCode.toString())
            return
        } else if (previousVersionCode.toLong() != currentVersionCode) {
            // user updated the app
            PreferencesData.setString(PreferencesKeys.VERSION_CODE, currentVersionCode.toString())
        }
    }
}
