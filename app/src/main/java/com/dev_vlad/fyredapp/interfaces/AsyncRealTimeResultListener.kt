package com.dev_vlad.fyredapp.interfaces

interface AsyncRealTimeResultListener {
    fun onDataAdded(data: Any)
    fun onDataModified(data: Any)
    fun onDataRemoved(data: Any)
    fun onError()
}