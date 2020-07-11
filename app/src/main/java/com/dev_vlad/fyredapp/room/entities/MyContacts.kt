package com.dev_vlad.fyredapp.room.entities

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "my_contacts_table"
)
@Keep
data class MyContacts(
    @PrimaryKey
    @ColumnInfo(name = "phone_number")
    var phoneNumber: String,

    @ColumnInfo(name = "phone_book_name")
    val phoneBookSavedName: String,

    @ColumnInfo(name = "fyred_app_profile_url")
    var profileUrl: String? = null,

    @ColumnInfo(name = "can_view_contacts")
    var canViewMyMoments: Boolean = true
) {
    fun isSameAs(other: Any?): Boolean {
        return other is MyContacts && other.phoneNumber == phoneNumber
    }

    fun hasSameContentsAs(other: Any?): Boolean {
        return isSameAs(other)
                && (other as MyContacts).phoneBookSavedName == phoneBookSavedName
                && other.profileUrl == profileUrl
                && other.canViewMyMoments == canViewMyMoments
    }
}