package com.dev_vlad.fyredapp.interfaces

interface AsyncResultListener {
    fun onAsyncOpComplete(isSuccessful: Boolean = false, data: Any? = null, errMsgId: Int? = null)
}