package com.dev_vlad.fyredapp.models

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Keep
data class UserFeedback(
    val userRating: Float,
    val userFeedback: String,
    val isPositive: Boolean
)