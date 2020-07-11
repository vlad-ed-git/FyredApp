package com.dev_vlad.fyredapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Keep
data class RecordedMoment(
    var mediaUriString: String,
    val isImage: Boolean = true,
    var caption: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString().toString()
    )

    //required empty constructor
    constructor() : this("")

    fun isSameAs(other: Any?): Boolean {
        return other is RecordedMoment && other.mediaUriString == mediaUriString
    }

    fun hasSameContentsAs(other: Any?): Boolean {
        return isSameAs(other) && (other as RecordedMoment).caption == caption
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mediaUriString)
        parcel.writeByte(if (isImage) 1 else 0)
        parcel.writeString(caption)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecordedMoment> {
        override fun createFromParcel(parcel: Parcel): RecordedMoment {
            return RecordedMoment(parcel)
        }

        override fun newArray(size: Int): Array<RecordedMoment?> {
            return arrayOfNulls(size)
        }
    }

}