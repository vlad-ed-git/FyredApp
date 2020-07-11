package com.dev_vlad.fyredapp.ui.home

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.models.CustomLatLng
import com.dev_vlad.fyredapp.repositories.HotSpotsRepo
import com.dev_vlad.fyredapp.room.FyredAppLocalDb
import com.dev_vlad.fyredapp.room.dao.MyContactsDao
import com.dev_vlad.fyredapp.utils.ImageProcessing
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val LOG_TAG = HomeViewModel::class.java.simpleName
    }


    init {
        loadMyLocationMarkerIconDescriptor(application.applicationContext)
        loadHotSpotBitmap(application.applicationContext)
    }

    //Database access
    private val myContactsDao: MyContactsDao =
        FyredAppLocalDb.fyredAppRoomDbInstance(application.applicationContext).myContactsDao


    fun getMyLiveOrNullSpot() = HotSpotsRepo.getObservableMySpot()
    fun getLiveHotSpots() = HotSpotsRepo.getObservableHotSpots()
    fun listenToMoments() {
        if (!HotSpotsRepo.isHotSpotListenerRegistered()) {
            HotSpotsRepo.listenForMomentsICareAbout(myContactsDao)
        }
    }

    /******** IMAGES ***********/
    lateinit var hotspotIcon: Bitmap
    private fun loadHotSpotBitmap(context: Context) {
        viewModelScope.launch {
            val glide: RequestManager = Glide.with(context)
            val bitmapIcon = ImageProcessing.getBitmapFromDrawable(
                glideRef = glide,
                imgRes = R.drawable.ic_hotspot
            )
            hotspotIcon = bitmapIcon
        }
    }

    lateinit var myMarkerIconDescriptor: BitmapDescriptor
    private fun loadMyLocationMarkerIconDescriptor(context: Context) {
        viewModelScope.launch {
            val glide: RequestManager = Glide.with(context)
            val bitmapIcon = ImageProcessing.getBitmapFromDrawable(
                glideRef = glide,
                imgRes = R.drawable.ic_my_location_pin
            )
            myMarkerIconDescriptor = BitmapDescriptorFactory.fromBitmap(bitmapIcon)
        }
    }


    /********** USER LOCATION ************/
    lateinit var userLastKnownLatLng: CustomLatLng
    fun getUserLastKnownAsLatLng(): LatLng? {
        return if (::userLastKnownLatLng.isInitialized)
            LatLng(userLastKnownLatLng.latitude, userLastKnownLatLng.longitude)
        else
            null
    }

    fun setUserLastKnownLatLng(location: Location) {
        Log.d(
            LOG_TAG,
            "from fyredApp | setting last known location @ ${location.latitude} , ${location.longitude}"
        )
        userLastKnownLatLng = CustomLatLng(
            latitude = location.latitude,
            longitude = location.longitude
        )
    }


    /********** permissions request status ************/
    enum class IntentsAwaitingLocation {
        NONE,
        ZOOM_ON_MY_LOCATION,
        SHARE_MOMENT
    }

    var intentAwaitingPermissions = IntentsAwaitingLocation.NONE


    /*
  ** the live hotSpots may change while the cluster manager is yet to be initialized i.e. ready
  * in that case switch the status to UPDATE_ON_READY so that once initialized it is immediately updated
  * without waiting for the next modification to happen on hotSpots
   */
    enum class ClusterManagerStatus {
        READY,
        NOT_READY,
        UPDATE_ON_READY
    }

    var clusterManagerStatus = ClusterManagerStatus.NOT_READY
}