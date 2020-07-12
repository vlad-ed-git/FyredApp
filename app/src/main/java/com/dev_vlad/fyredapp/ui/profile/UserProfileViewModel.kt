package com.dev_vlad.fyredapp.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.models.Users
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.dev_vlad.fyredapp.utils.MyLog

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {


    private val glideRef: RequestManager by lazy {
        Glide.with(application.applicationContext)
    }


    enum class UserProfileStatus {
        LOADED,
        UPDATING,
        UPLOADING_PROFILE_PIC,
        DELETING_PROFILE_PIC,
        UPDATE_SUCCESSFUL,
        UPDATE_FAILED
    }

    fun isProfileUpdating(): Boolean {
        return userProfileStatus.value == UserProfileStatus.UPDATING ||
                userProfileStatus.value == UserProfileStatus.UPLOADING_PROFILE_PIC ||
                userProfileStatus.value == UserProfileStatus.DELETING_PROFILE_PIC
    }

    val oldUserProfile = Users(
        userId = UserRepo.getUserId(),
        phoneNumber = UserRepo.getMyNumber(),
        userPhotoUriStr = UserRepo.getUserProfilePhotoUri().toString()
    )

    var newPhotoUriAsStr: String? = null

    val userProfileStatus: MutableLiveData<UserProfileStatus> =
        MutableLiveData(UserProfileStatus.LOADED)


    private val callback: AsyncResultListener = object : AsyncResultListener {
        override fun onAsyncOpComplete(isSuccessful: Boolean, data: Any?, errMsgId: Int?) {
            if (isSuccessful) {
                when (userProfileStatus.value) {

                    UserProfileStatus.DELETING_PROFILE_PIC -> {
                        newPhotoUriAsStr = null
                        updateUserAuthPhotoUri()
                        userProfileStatus.value = UserProfileStatus.UPDATING
                    }
                    UserProfileStatus.UPLOADING_PROFILE_PIC -> {
                        if (data != null && data is String) {
                            newPhotoUriAsStr = data
                            updateUserAuthPhotoUri()
                            userProfileStatus.value = UserProfileStatus.UPDATING
                        } else {
                            userProfileStatus.value = UserProfileStatus.UPDATE_FAILED
                        }
                    }
                    else -> {
                        //updated auth
                        userProfileStatus.value = UserProfileStatus.UPDATE_SUCCESSFUL
                    }
                }
            } else {
                userProfileStatus.value = UserProfileStatus.UPDATE_FAILED
            }
        }

    }

    private fun updateUserAuthPhotoUri() {
        MyLog.d(LOG_TAG, "from fyredApp | updateUserAuthPhotoUri called")
        UserRepo.updateUserAuthPhotoUri(newPhotoUriAsStr, callback)
    }


    fun uploadProfilePicToStorage() {
        when {
            oldUserProfile.userPhotoUriStr == newPhotoUriAsStr -> {
                return
            }
            oldUserProfile.userPhotoUriStr != null && newPhotoUriAsStr == null -> {
                MyLog.d(LOG_TAG, "uploadProfilePicToStorage() -> delete photo")
                UserRepo.deleteMyProfilePhotoFromStorage(callback)
                userProfileStatus.value = UserProfileStatus.DELETING_PROFILE_PIC
            }
            newPhotoUriAsStr != null -> {
                MyLog.d(LOG_TAG, "uploadProfilePicToStorage() -> update  photo")
                UserRepo.uploadUserProfilePic(newPhotoUriAsStr!!, callback, glideRef)
                userProfileStatus.value = UserProfileStatus.UPLOADING_PROFILE_PIC
            }
        }
    }

    companion object {
        private val LOG_TAG = UserProfileViewModel::class.java.simpleName
    }

}