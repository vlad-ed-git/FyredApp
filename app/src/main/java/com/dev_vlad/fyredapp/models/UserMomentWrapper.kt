package com.dev_vlad.fyredapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*
import kotlin.collections.ArrayList

@IgnoreExtraProperties
@Keep
data class UserMomentWrapper(
    val recordedBy: Users,
    val sharedWith: List<String>,
    val recordedAt: CustomLatLng,
    val recordedMoments: List<RecordedMoment> = ArrayList(),
    var likedBy: List<String> = ArrayList(),
    var likesCount: Int = 0,
    @ServerTimestamp
    var sharedOnDate: Date? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Users::class.java.classLoader)!!,
        parcel.createStringArrayList()!!,
        parcel.readParcelable(CustomLatLng::class.java.classLoader)!!,
        parcel.createTypedArrayList(RecordedMoment)!!,
        parcel.createStringArrayList()!!,
        parcel.readInt(),
        parcel.readSerializable() as Date?
    )

    //required empty constructor
    constructor() : this(
        Users(),
        ArrayList<String>(),
        CustomLatLng()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(recordedBy, flags)
        parcel.writeStringList(sharedWith)
        parcel.writeParcelable(recordedAt, flags)
        parcel.writeTypedList(recordedMoments)
        parcel.writeStringList(likedBy)
        parcel.writeInt(likesCount)
        parcel.writeSerializable(sharedOnDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserMomentWrapper> {
        override fun createFromParcel(parcel: Parcel): UserMomentWrapper {
            return UserMomentWrapper(parcel)
        }

        override fun newArray(size: Int): Array<UserMomentWrapper?> {
            return arrayOfNulls(size)
        }
    }

    fun isSameAs(other: Any?): Boolean {
        return other is UserMomentWrapper && other.recordedBy.userId == recordedBy.userId
    }

    fun hasSameContentsAs(other: Any?): Boolean {
        return isSameAs(other)
                && (other as UserMomentWrapper).sharedOnDate == sharedOnDate
                && other.recordedAt == recordedAt
                && other.likesCount == likesCount
                && other.recordedBy.userPhotoUriStr == recordedBy.userPhotoUriStr
    }
}