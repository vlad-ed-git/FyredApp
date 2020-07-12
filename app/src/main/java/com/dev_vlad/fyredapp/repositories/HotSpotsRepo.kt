package com.dev_vlad.fyredapp.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dev_vlad.fyredapp.models.UserMomentWrapper
import com.dev_vlad.fyredapp.room.dao.MyContactsDao
import com.dev_vlad.fyredapp.room.entities.MyContacts
import com.dev_vlad.fyredapp.utils.AppConstants.MAX_HOURS_BEFORE_MOMENT_EXPIRES
import com.dev_vlad.fyredapp.utils.AppConstants.USERS_MOMENTS_COLLECTION_NAME
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

object HotSpotsRepo {

    private val LOG_TAG = HotSpotsRepo::class.java.simpleName
    private var hotSpotsListener: ListenerRegistration? = null
    private val hotSpots = ArrayList<UserMomentWrapper>()
    private val mutableLiveHotSpots = MutableLiveData<ArrayList<UserMomentWrapper>>()
    private val mutableLiveMySpot = MutableLiveData<UserMomentWrapper>()
    private lateinit var myContactsDao: MyContactsDao

    fun getObservableHotSpots(): LiveData<ArrayList<UserMomentWrapper>> = mutableLiveHotSpots
    fun getObservableMySpot(): LiveData<UserMomentWrapper> = mutableLiveMySpot

    private fun clearHotSpotsData() {
        Log.d(LOG_TAG, "from fyredApp | clearHotSpotsData()")
        hotSpots.clear()
        mutableLiveHotSpots.value = hotSpots
        mutableLiveMySpot.value = null
    }

    fun listenForMomentsICareAbout(myContactsDao: MyContactsDao) {
        if (!UserRepo.userIsLoggedIn()) {
            Log.e(LOG_TAG, "from fyredApp | listenForMomentsICareAbout() user is logged off")
            return
        }

        Log.d(LOG_TAG, "from fyredApp | listenForMomentsICareAbout() called")
        clearHotSpotsData()

        this.myContactsDao = myContactsDao
        val query = FirebaseFirestore.getInstance().collection(USERS_MOMENTS_COLLECTION_NAME)
            .whereArrayContains("sharedWith", UserRepo.getMyNumber())
        hotSpotsListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e(LOG_TAG, "from fyredApp | listenForMomentsICareAbout() - error", e)
                return@addSnapshotListener
            }

            for (dc in snapshots!!.documentChanges) {
                when (dc.type) {
                    DocumentChange.Type.ADDED -> {
                        Log.d(
                            LOG_TAG,
                            "from fyredApp | listenForMomentsICareAbout()  - found added"
                        )
                        val hotSpot = dc.document.toObject(UserMomentWrapper::class.java)
                        addToHotSpots(hotSpot)
                        updateContactsProfileUri(myContactsDao, hotSpot)
                    }
                    DocumentChange.Type.MODIFIED -> {
                        Log.d(LOG_TAG, "from fyredApp | listenForMomentsICareAbout()  - modified")
                        val hotSpot = dc.document.toObject(UserMomentWrapper::class.java)
                        modifyHotSpots(hotSpot)
                    }

                    DocumentChange.Type.REMOVED -> {
                        Log.d(LOG_TAG, "from fyredApp | listenForMomentsICareAbout()  - removed")
                        val hotSpot = dc.document.toObject(UserMomentWrapper::class.java)
                        removeFromHotSpots(hotSpot)
                    }
                }
            }
        }

    }

    private fun updateContactsProfileUri(
        myContactsDao: MyContactsDao,
        hotSpot: UserMomentWrapper
    ) {
        CoroutineScope(IO).launch {
            try {
                val existingContact =
                    myContactsDao.getContactByPhoneNumber(hotSpot.recordedBy.phoneNumber!!)
                if (existingContact != null) {
                    existingContact.profileUrl = hotSpot.recordedBy.userPhotoUriStr
                    myContactsDao.update(existingContact)
                }
            } catch (exc: Exception) {
                Log.d(
                    LOG_TAG,
                    "from fyredApp updateContactsProfileUri() failed ${exc.message}",
                    exc.cause
                )
            }
        }
    }

    private fun notifyLiveHotSpotsObserver() {
        Log.d(LOG_TAG, "from fyredApp |  notifyLiveHotSpotsObserver() - notified")
        val updatedHotSpots: ArrayList<UserMomentWrapper> = ArrayList()
        updatedHotSpots.addAll(hotSpots)
        mutableLiveHotSpots.value = updatedHotSpots
    }


    private fun addToHotSpots(hotspot: UserMomentWrapper) {
        if (hotspot.recordedBy.userId == UserRepo.getUserId()) {
            Log.d(LOG_TAG, "from fyredApp | my hotspot set")
            //the spot is where user is at
            mutableLiveMySpot.value = hotspot
        } else {
            Log.d(LOG_TAG, "from fyredApp | new hotspot added")
            addContactInfoNInsertHotSpot(hotspot)
        }
    }

    private fun removeFromHotSpots(hotspot: UserMomentWrapper) {

        if (hotspot.recordedBy.userId == UserRepo.getUserId()) {
            Log.d(LOG_TAG, "from fyredApp | my hotspot has been removed")
            mutableLiveMySpot.value = hotspot
        } else {
            val hotSpotToRemove = hotSpots.filter { it.isSameAs(hotspot) }
            if (hotSpotToRemove.isNotEmpty()) {
                Log.d(LOG_TAG, "from fyredApp | hotspot removed")
                hotSpots.remove(hotSpotToRemove[0])
                notifyLiveHotSpotsObserver()
            }
        }
    }

    private fun modifyHotSpots(newHotSpot: UserMomentWrapper) {

        if (newHotSpot.recordedBy.userId == UserRepo.getUserId()) {
            Log.d(LOG_TAG, "from fyredApp | my hotspot modified")
            mutableLiveMySpot.value = newHotSpot

        } else {

            val spotToModify = hotSpots.filter { it.isSameAs(newHotSpot) }
            if (spotToModify.isNotEmpty()) {
                Log.d(LOG_TAG, "from fyredApp | hotspot modified")
                hotSpots.remove(spotToModify[0])
                addContactInfoNInsertHotSpot(newHotSpot)

            }
        }
    }

    private fun addContactInfoNInsertHotSpot(hotspot: UserMomentWrapper) {
        if (!::myContactsDao.isInitialized) {
            Log.e(LOG_TAG, "addHotSpotInfo myContactsDao is not initialized")
        }

        val recordersPhoneNumber = hotspot.recordedBy.phoneNumber!!
        CoroutineScope(IO).launch {
            val contacts: MyContacts? = myContactsDao.getContactByPhoneNumber(recordersPhoneNumber)
            withContext(Main) {
                contacts?.let { unBlockedContact ->
                    if (unBlockedContact.canViewMyMoments) {
                        hotspot.recordedBy.phoneBookName = unBlockedContact.phoneBookSavedName
                        Log.d(LOG_TAG, "addHotSpotInfo added")
                        hotSpots.add(hotspot)
                        notifyLiveHotSpotsObserver()
                    }
                }

            }
        }
    }

    fun unRegisterHotSpotListener() {
        Log.d(LOG_TAG, "from fyredApp | listenToMomentsICareAbout() - listener cleared")
        hotSpotsListener?.remove()
        hotSpotsListener = null
    }

    fun isHotSpotListenerRegistered(): Boolean = hotSpotsListener != null

    fun autoDeleteOldMoments() {
        Log.d(LOG_TAG, "autoDeleteOldMoments()")
        //deletes old moments
        val calendar: Calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -(MAX_HOURS_BEFORE_MOMENT_EXPIRES))
        val date: Date = calendar.time

        val fireStoreDb = FirebaseFirestore.getInstance()
        for (hotspot in hotSpots) {

            if (hotspot.sharedOnDate!! > date) {
                return
            } else {
                Log.d(LOG_TAG, "auto deleting old moment from")


                //delete moment files
                fireStoreDb.collection(USERS_MOMENTS_COLLECTION_NAME)
                    .document(hotspot.recordedBy.userId!!)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(LOG_TAG, "Deleted old moment info successfully")
                        //delete moments
                        UserMomentsStorageRepo.userMomentsFolder.delete()
                            .addOnFailureListener {
                                Log.d(
                                    LOG_TAG,
                                    "Failed to Delete old moment files ${it.message}",
                                    it.cause
                                )
                            }

                    }
                    .addOnFailureListener {
                        Log.d(
                            LOG_TAG,
                            "Failed to Delete old moment info ${it.message}",
                            it.cause
                        )
                    }
            }

        }
    }

}