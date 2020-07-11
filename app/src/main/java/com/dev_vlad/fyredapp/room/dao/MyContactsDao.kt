package com.dev_vlad.fyredapp.room.dao

import androidx.annotation.Keep
import androidx.room.*
import com.dev_vlad.fyredapp.room.entities.MyContacts

@Dao
@Keep
interface MyContactsDao {

    @Query("SELECT * FROM my_contacts_table ORDER BY phone_book_name ASC")
    suspend fun getAllContacts(): List<MyContacts>

    @Query("SELECT phone_number FROM my_contacts_table WHERE can_view_contacts =:canViewMoments ORDER BY phone_book_name ASC")
    suspend fun getPhoneNumbersByBlockStatus(canViewMoments: Boolean = true): List<String>

    @Query("SELECT * FROM my_contacts_table WHERE phone_number =:recordersPhoneNumber ORDER BY phone_book_name ASC LIMIT 1")
    suspend fun getContactByPhoneNumber(recordersPhoneNumber: String): MyContacts?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(userContacts: List<MyContacts>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(userContact: MyContacts)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(userContact: List<MyContacts>)

    @Transaction
    suspend fun clearAndInsert(usersContacts: List<MyContacts>) {
        clearContacts()
        if (usersContacts.isNotEmpty())
            insertAll(usersContacts)
    }


    @Query("DELETE FROM my_contacts_table")
    suspend fun clearContacts()


}