package com.dev_vlad.fyredapp.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.models.CustomLatLng
import com.dev_vlad.fyredapp.models.RecordedMoment
import com.dev_vlad.fyredapp.models.UserMomentWrapper
import com.dev_vlad.fyredapp.models.Users
import com.dev_vlad.fyredapp.repositories.MyContactsRepo
import com.dev_vlad.fyredapp.repositories.UserMomentsStorageRepo
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.dev_vlad.fyredapp.room.FyredAppLocalDb
import com.dev_vlad.fyredapp.room.dao.MyContactsDao
import com.dev_vlad.fyredapp.utils.MyLog
import kotlinx.coroutines.launch

class RecordMomentViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val LOG_TAG = RecordMomentViewModel::class.java.simpleName
    }

    var pathOfJustTakenPhoto: String? = null
    private val glideRef: RequestManager = Glide.with(application.applicationContext)

    //Database access
    private val myContactsDao: MyContactsDao =
        FyredAppLocalDb.fyredAppRoomDbInstance(application.applicationContext).myContactsDao


    private val recordedMoments = ArrayList<RecordedMoment>()
    private val liveRecordedMoments = MutableLiveData<List<RecordedMoment>>()


    fun getLiveRecordedMoments(): LiveData<List<RecordedMoment>> {
        if (liveRecordedMoments.value == null)
            updateLiveMoments()
        return liveRecordedMoments
    }

    private fun updateLiveMoments() {
        val updatedRecordedMoments = ArrayList<RecordedMoment>()
        updatedRecordedMoments.addAll(recordedMoments)
        liveRecordedMoments.value = updatedRecordedMoments
    }

    enum class UploadingState {
        NOT_STARTED,
        DONE_SUCCESSFULLY,
        ONGOING,
        FAILED
    }

    private val uploadingState = MutableLiveData<UploadingState>(UploadingState.NOT_STARTED)


    fun addMoment(newMoment: RecordedMoment) {
        if (recordedMoments.contains(newMoment))
            return //no duplicates allowed
        MyLog.d(LOG_TAG, "from fyredApp | added moment")
        recordedMoments.add(newMoment)
        updateLiveMoments()
    }


    fun removeMoment(oldMoment: RecordedMoment) {
        var removeAt = -1
        for ((index, moment) in recordedMoments.withIndex()) {
            if (moment.isSameAs(oldMoment))
                removeAt = index

        }
        if (removeAt != -1) {
            MyLog.d(LOG_TAG, "from fyredApp | removed moment")
            recordedMoments.removeAt(removeAt)
            updateLiveMoments()
        }
    }

    fun modifyMoment(newMoment: RecordedMoment, oldMoment: RecordedMoment) {
        var modifyAt = -1
        for ((index, moment) in recordedMoments.withIndex()) {
            if (moment.isSameAs(oldMoment))
                modifyAt = index

        }
        if (modifyAt != -1) {
            MyLog.d(LOG_TAG, "from fyredApp | modified moment")
            recordedMoments[modifyAt] = newMoment
            updateLiveMoments()
        }
    }


    /****** UPLOADING ********/
    fun getUploadingStatus(): LiveData<UploadingState> = uploadingState
    fun uploadUserMoment(userMomentsTakenAtLatLng: CustomLatLng) {
        if (recordedMoments.size < 1) return //there are no moments to share

        viewModelScope.launch {

            uploadingState.value = UploadingState.ONGOING

            val contactsToShareWith = MyContactsRepo.getUnBlockedPhoneNumbers(myContactsDao)
            //include one self
            contactsToShareWith.add(UserRepo.getMyNumber())

            val thisUser = Users(
                userId = UserRepo.getUserId(),
                userPhotoUriStr = UserRepo.getUserProfilePhotoUri().toString(),
                phoneNumber = UserRepo.getMyNumber()
            )

            //wrap moment with user info
            val userMomentWrapper = UserMomentWrapper(
                recordedBy = thisUser,
                sharedWith = contactsToShareWith,
                recordedAt = userMomentsTakenAtLatLng,
                recordedMoments = recordedMoments
            )

            UserMomentsStorageRepo.uploadRecordedMoments(
                imgCompressHelper = glideRef,
                wrappedUserNMoment = userMomentWrapper,
                callback = object : AsyncResultListener {
                    override fun onAsyncOpComplete(
                        isSuccessful: Boolean,
                        data: Any?,
                        errMsgId: Int?
                    ) {
                        if (isSuccessful) {
                            uploadingState.value = UploadingState.DONE_SUCCESSFULLY
                        } else
                            uploadingState.value = UploadingState.FAILED
                    }

                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveRecordedMoments.value = null
    }
}