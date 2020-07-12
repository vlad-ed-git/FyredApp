package com.dev_vlad.fyredapp.ui.contacts

import android.app.Application
import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.repositories.HotSpotsRepo
import com.dev_vlad.fyredapp.repositories.MyContactsRepo
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.dev_vlad.fyredapp.room.FyredAppLocalDb
import com.dev_vlad.fyredapp.room.dao.MyContactsDao
import com.dev_vlad.fyredapp.room.entities.MyContacts
import com.dev_vlad.fyredapp.utils.AppConstants
import com.dev_vlad.fyredapp.utils.MyLog
import com.dev_vlad.fyredapp.utils.PhoneNumberValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val LOG_TAG = ContactsViewModel::class.java.simpleName
    }

    private val phoneContactsContentResolver =
        application.applicationContext.contentResolver

    private val defaultCountryCode = application.applicationContext.getSharedPreferences(
        AppConstants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE
    ).getString(AppConstants.USERS_COUNTRY_CODE_KEY, null)

    //Database access
    private val myContactsDao: MyContactsDao =
        FyredAppLocalDb.fyredAppRoomDbInstance(application.applicationContext).myContactsDao

    enum class ContactsListStatus {
        LOADING,
        LOADED_NOT_SYNCED_WITH_PHONE_BOOK,
        SYNCING_WITH_PHONE_BOOK,
        SYNCED_WITH_PHONE_BOOK
    }

    private val contactListStatus =
        MutableLiveData<ContactsListStatus>(
            ContactsListStatus.LOADING
        )
            .also {
                viewModelScope.launch {
                    MyContactsRepo.refreshContactsCache(myContactsDao)
                    it.value =
                        ContactsListStatus.LOADED_NOT_SYNCED_WITH_PHONE_BOOK
                }
            }

    fun observerContactListStatus(): LiveData<ContactsListStatus> = contactListStatus

    fun getContacts() = MyContactsRepo.myContactsCache


    fun scanPhoneContacts() {
        if (contactListStatus.value != ContactsListStatus.SYNCING_WITH_PHONE_BOOK) {
            contactListStatus.value =
                ContactsListStatus.SYNCING_WITH_PHONE_BOOK
            MyLog.d(
                LOG_TAG,
                "from fyredApp | scanPhoneContacts()"
            )
            viewModelScope.launch {
                val phoneBookContacts = withContext(Dispatchers.IO) {
                    async {
                        val myPhoneNumber = UserRepo.getMyNumber()
                        val phoneBookContactsTmp = ArrayList<MyContacts>()
                        val contactCursor = phoneContactsContentResolver.query(
                            ContactsContract.Contacts.CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                        )
                        if (contactCursor != null) {
                            val count = contactCursor.count
                            if (count > 0) {

                                while (contactCursor.moveToNext()) {
                                    //prepare val
                                    var phoneNumber: String? = null

                                    //get contact name
                                    val name =
                                        contactCursor.getString(
                                            contactCursor.getColumnIndex(
                                                ContactsContract.Contacts.DISPLAY_NAME
                                            )
                                        )

                                    val id =
                                        contactCursor.getString(
                                            contactCursor.getColumnIndex(
                                                ContactsContract.Contacts._ID
                                            )
                                        )

                                    //get phone number
                                    if (contactCursor.getInt(
                                            contactCursor.getColumnIndex(
                                                ContactsContract.Contacts.HAS_PHONE_NUMBER
                                            )
                                        ) > 0
                                    ) {
                                        val phoneCursor = phoneContactsContentResolver.query(
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                            null,
                                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                            arrayOf(id), null
                                        )

                                        //set phone number
                                        if (phoneCursor != null) {
                                            while (phoneCursor.moveToNext()) {
                                                phoneNumber = phoneCursor.getString(
                                                    phoneCursor.getColumnIndex(
                                                        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
                                                    )
                                                )
                                                if (phoneNumber == null)
                                                    phoneNumber = phoneCursor.getString(
                                                        phoneCursor.getColumnIndex(
                                                            ContactsContract.CommonDataKinds.Phone.NUMBER
                                                        )
                                                    )
                                            }
                                            phoneCursor.close()
                                        }

                                    }

                                    PhoneNumberValidator.addCountryCodeToPhoneNumber(
                                        defaultCountryCode,
                                        phoneNumber
                                    )?.let { validPhone ->
                                        if (validPhone != myPhoneNumber) {
                                            val tmpContact = MyContacts(
                                                phoneNumber = validPhone,
                                                phoneBookSavedName = name
                                            )
                                            phoneBookContactsTmp.add(tmpContact)
                                        }
                                    }
                                }
                            }

                            contactCursor.close()
                            phoneBookContactsTmp
                        } else {
                            MyLog.e(
                                LOG_TAG,
                                "from fyredApp | scanPhoneContacts() -> contacts cursor found null"
                            )
                            null
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    val foundContacts = phoneBookContacts.await()
                    if (foundContacts == null || foundContacts.size == 0) {
                        MyLog.e(
                            LOG_TAG,
                            "from fyredApp | scanPhoneContacts() foundContacts is null or empty"
                        )
                        contactListStatus.value =
                            ContactsListStatus.SYNCED_WITH_PHONE_BOOK
                    } else {
                        MyLog.d(
                            LOG_TAG,
                            "from fyredApp | scanPhoneContacts() found ${foundContacts.size}"
                        )
                        checkFoundContactsAgainstFyredAppUsers(foundContacts)
                    }
                }
            }
        }
    }

    private var phoneBookContactsToSync = 0
    private fun checkFoundContactsAgainstFyredAppUsers(foundContacts: ArrayList<MyContacts>) {
        MyContactsRepo.clearCachedContacts()
        phoneBookContactsToSync = foundContacts.size
        MyLog.d(
            LOG_TAG,
            "from fyredApp | checkFoundContactsAgainstFyredAppUsers"
        )
        for (contact in foundContacts) {
            MyContactsRepo.checkIfPhoneContactIsFyredAppUser(
                userPhoneBookContact = contact,
                resultCallback = checkIfUserExistsCallback
            )
        }
    }

    private var syncedPhoneBookContacts = 0
    private val checkIfUserExistsCallback = object : AsyncResultListener {
        override fun onAsyncOpComplete(isSuccessful: Boolean, data: Any?, errMsgId: Int?) {
            syncedPhoneBookContacts += 1
            if (phoneBookContactsToSync == syncedPhoneBookContacts) {
                MyLog.d(
                    LOG_TAG,
                    "from fyredApp | onAsyncOpComplete"
                )
                //all contacts have been checked
                viewModelScope.launch {
                    MyContactsRepo.addCachedContactsToLocalDb(myContactsDao)
                    phoneBookContactsToSync = 0
                    syncedPhoneBookContacts = 0
                    //contacts have changed
                    HotSpotsRepo.unRegisterHotSpotListener()
                    contactListStatus.value =
                        ContactsListStatus.SYNCED_WITH_PHONE_BOOK
                }

            }

        }
    }


    fun toggleContactsBlockStatus(shouldBlockContact: Boolean, contact: MyContacts) {
        if (contactListStatus.value != ContactsListStatus.SYNCING_WITH_PHONE_BOOK) {
            contactListStatus.value =
                ContactsListStatus.SYNCING_WITH_PHONE_BOOK
            MyLog.d(
                LOG_TAG,
                "from fyredApp | toggleContactsBlockStatus blockContact? $shouldBlockContact"
            )
            viewModelScope.launch {
                val updatedContact = MyContacts(
                    phoneNumber = contact.phoneNumber,
                    phoneBookSavedName = contact.phoneBookSavedName,
                    profileUrl = contact.profileUrl,
                    canViewMyMoments = !shouldBlockContact
                )
                MyContactsRepo.updateContact(updatedContact, myContactsDao)
                MyContactsRepo.refreshContactsCache(myContactsDao)
                //contacts have changed
                HotSpotsRepo.unRegisterHotSpotListener()
                contactListStatus.value =
                    ContactsListStatus.SYNCED_WITH_PHONE_BOOK
            }
        }
    }

}

