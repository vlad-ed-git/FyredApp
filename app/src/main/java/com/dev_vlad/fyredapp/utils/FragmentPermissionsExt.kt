package com.dev_vlad.fyredapp.utils

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


fun Fragment.hasAppPermission(permission: String): Boolean {
    return (ContextCompat.checkSelfPermission(
        requireContext(),
        permission
    ) == PackageManager.PERMISSION_GRANTED)

}

fun Fragment.shouldShowRationaleForAppPermission(permission: String): Boolean =
    shouldShowRequestPermissionRationale(permission)


fun Fragment.requestAppPermissions(permissionsArr: Array<String>, requestCode: Int) {
    requestPermissions(
        permissionsArr,
        requestCode
    )


}