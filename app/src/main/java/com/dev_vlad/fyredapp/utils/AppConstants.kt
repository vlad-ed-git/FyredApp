package com.dev_vlad.fyredapp.utils

object AppConstants {
    //permissions & request codes
    const val PERMISSION_REQUEST_READ_CONTACTS = 12
    const val PERMISSION_REQUEST_FINE_LOCATION = 11
    const val PERMISSION_REQUEST_CAMERA = 19
    const val PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 93

    //shared preferences keys
    const val DEVICE_TOKEN_KEY = "user_device_token"
    const val PREFERENCE_FILE_KEY = "fyredapp_shared_preference_file"
    const val USERS_COUNTRY_CODE_KEY = "user_country_code"


    //moments
    const val MAX_VIDEO_SECONDS = 8
    const val MAX_MOMENTS = 3
    const val MAX_HOURS_BEFORE_MOMENT_EXPIRES = 6

    //image resize and compressing
    const val IMG_COMPRESS_FACTOR = 60
    const val PREFERRED_IMG_HEIGHT = 960

    //firebase collections
    const val REGISTERED_USERS_COLLECTION_NAME = "registered_users"
    const val USERS_MOMENTS_COLLECTION_NAME = "user_moments"
    const val MOMENTS_STORAGE_FOLDER = "user_moments"
    const val PROFILE_PHOTO_FOLDER = "user_profile_photos"
    const val USER_FEEDBACK_COLLECTION_NAME = "user_feedback"

    //geofence
    const val MAX_ACCEPTED_LOCATION_ACCURACY_IN_METERS = 100
}