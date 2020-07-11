package com.dev_vlad.fyredapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

@IgnoreExtraProperties
@Keep
/*
** using uri instead of string for uri types causes object cycle serializing errors with fireBase
 */
data class Users(
    var userId: String? = null,
    var userPhotoUriStr: String? = null,
    var phoneNumber: String? = null,
    var phoneBookName: String? = null,
    var signInCountryCode: String? = null,
    var signInCountry: String? = null,
    var deviceToken: String? = null,
    @ServerTimestamp
    var signedUpOnDate: Date? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readSerializable() as Date?
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userId)
        parcel.writeString(userPhotoUriStr)
        parcel.writeString(phoneNumber)
        parcel.writeString(phoneBookName)
        parcel.writeString(signInCountryCode)
        parcel.writeString(signInCountry)
        parcel.writeString(deviceToken)
        parcel.writeSerializable(signedUpOnDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Users> {
        override fun createFromParcel(parcel: Parcel): Users {
            return Users(parcel)
        }

        override fun newArray(size: Int): Array<Users?> {
            return arrayOfNulls(size)
        }
    }
}