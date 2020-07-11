package com.dev_vlad.fyredapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Keep
data class CustomLatLng(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )

    //required empty constructor
    constructor() : this(0.toDouble(), 0.toDouble())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomLatLng> {
        override fun createFromParcel(parcel: Parcel): CustomLatLng {
            return CustomLatLng(parcel)
        }

        override fun newArray(size: Int): Array<CustomLatLng?> {
            return arrayOfNulls(size)
        }
    }
}