package com.dev_vlad.fyredapp.repositories

import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.models.Users
import com.dev_vlad.fyredapp.room.dao.MyContactsDao
import com.dev_vlad.fyredapp.room.entities.MyContacts
import com.dev_vlad.fyredapp.utils.AppConstants
import com.dev_vlad.fyredapp.utils.MyLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

object MyContactsRepo {

    private val LOG_TAG = MyContactsRepo::class.java.simpleName
    val myContactsCache = ArrayList<MyContacts>()

    suspend fun refreshContactsCache(myContactsDao: MyContactsDao) = withContext(IO) {
        myContactsCache.clear()
        myContactsCache.addAll(myContactsDao.getAllContacts())
        MyLog.d(
            LOG_TAG,
            " from fyredApp | refreshContactsCache | cached ${myContactsCache.size} contacts"
        )
    }

    suspend fun updateContact(contact: MyContacts, myContactsDao: MyContactsDao) = withContext(IO) {
        myContactsDao.update(contact)
        MyLog.d(
            LOG_TAG,
            " from fyredApp | updateContact updated"
        )
    }

    suspend fun getUnBlockedPhoneNumbers(myContactsDao: MyContactsDao): ArrayList<String> =
        withContext(IO) {
            val unBlockedContacts = ArrayList<String>()
            unBlockedContacts.addAll(myContactsDao.getPhoneNumbersByBlockStatus(true))
            unBlockedContacts
        }


    /*** Controlled by ViewModel
     * After scanning the phone book, the cachedContacts arraylist is cleared
     * then each contact is checked against the ones in the server (registered users)
     * and if found, added to this cache (at which point, it may not be same as local db)
     * so when done with all contacts scanning, the cached contacts are added to local db
     * */
    fun clearCachedContacts() {
        myContactsCache.clear()
    }

    fun checkIfPhoneContactIsFyredAppUser(
        userPhoneBookContact: MyContacts,
        resultCallback: AsyncResultListener
    ) {
        if (!UserRepo.userIsLoggedIn()) {
            MyLog.e(
                LOG_TAG,
                "from fyredApp |  checkIfPhoneContactIsFyredAppUser() user is logged out"
            )
            resultCallback.onAsyncOpComplete(isSuccessful = false)
            return
        }

        FirebaseFirestore.getInstance().collection(AppConstants.REGISTERED_USERS_COLLECTION_NAME)
            .whereEqualTo("phoneNumber", userPhoneBookContact.phoneNumber)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    userPhoneBookContact.profileUrl =
                        document.toObject(Users::class.java).userPhotoUriStr
                    myContactsCache.add(userPhoneBookContact)
                }
                resultCallback.onAsyncOpComplete(isSuccessful = true)
            }
            .addOnFailureListener {
                MyLog.e(
                    LOG_TAG,
                    "from fyredApp |  checkIfPhoneContactIsFyredAppUser() ${userPhoneBookContact.phoneNumber} failed ${it.message}",
                    it.cause
                )
                resultCallback.onAsyncOpComplete(isSuccessful = false)

            }
    }


    suspend fun addCachedContactsToLocalDb(myContactsDao: MyContactsDao) = withContext(IO) {
        //preserve block status and profile pic
        val existingContacts = myContactsDao.getAllContacts()
        val restoredContacts = ArrayList<MyContacts>()
        for (existingContact in existingContacts) {
            val cachedContact =
                myContactsCache.filter { it.phoneNumber == existingContact.phoneNumber }
            if (cachedContact.isNotEmpty()) {
                cachedContact[0].canViewMyMoments = existingContact.canViewMyMoments
                cachedContact[0].profileUrl = existingContact.profileUrl
            }
            restoredContacts.addAll(cachedContact)
        }
        myContactsDao.clearAndInsert(myContactsCache)
        myContactsDao.update(restoredContacts)
        myContactsCache.clear()
        myContactsCache.addAll(myContactsDao.getAllContacts())

    }

}