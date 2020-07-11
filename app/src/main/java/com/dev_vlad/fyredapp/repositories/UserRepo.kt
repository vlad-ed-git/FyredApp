package com.dev_vlad.fyredapp.repositories

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.bumptech.glide.RequestManager
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.models.UserFeedback
import com.dev_vlad.fyredapp.models.Users
import com.dev_vlad.fyredapp.utils.AppConstants
import com.dev_vlad.fyredapp.utils.AppConstants.USER_FEEDBACK_COLLECTION_NAME
import com.dev_vlad.fyredapp.utils.ImageProcessing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

object UserRepo {
    private val LOG_TAG = UserRepo::class.java.simpleName

    fun userIsLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    fun getUserId(): String = FirebaseAuth.getInstance().currentUser!!.uid

    fun getMyNumber(): String = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!

    private val userProfilePhotosFolder by lazy {
        FirebaseStorage.getInstance().getReference(AppConstants.PROFILE_PHOTO_FOLDER).child(
            getUserId()
        )
    }

    fun getUserProfilePhotoUri(): Uri? =
        FirebaseAuth.getInstance().currentUser!!.photoUrl

    fun signUserOut() {
        if (userIsLoggedIn()) {
            FirebaseAuth.getInstance().signOut()
        }
    }

    fun saveUserData(signedInUser: Users, callback: AsyncResultListener) {
        if (!userIsLoggedIn()) {
            Log.e(LOG_TAG, "from fyredApp | registerUserIfNotExists() user is logged out")
            callback.onAsyncOpComplete(isSuccessful = false)
            return
        }
        signedInUser.userId = getUserId()
        signedInUser.userPhotoUriStr = getUserProfilePhotoUri().toString()
        //TODO save device token?
        FirebaseFirestore.getInstance().collection(AppConstants.REGISTERED_USERS_COLLECTION_NAME)
            .document(signedInUser.userId!!)
            .set(signedInUser)
            .addOnSuccessListener {
                callback.onAsyncOpComplete(isSuccessful = true)
            }
            .addOnFailureListener {
                Log.e(
                    LOG_TAG,
                    "from fyredApp | registerUserIfNotExists() failed ${it.message}",
                    it.cause
                )
                signUserOut()
                callback.onAsyncOpComplete(isSuccessful = false)
            }

    }

    fun uploadUserProfilePic(
        newPhotoUriAsStr: String,
        callback: AsyncResultListener,
        glideRef: RequestManager
    ) {
        if (!userIsLoggedIn()) {
            Log.e(LOG_TAG, "from fyredApp |  uploadUserProfilePic() user is logged out")
            callback.onAsyncOpComplete(isSuccessful = false)
            return
        }

        Log.d(
            LOG_TAG,
            "from fyredApp | uploadUserProfilePic changing pic"
        )

        CoroutineScope(Main).launch {
            val byteArray = ImageProcessing.scaleAndResizeImageAsync(newPhotoUriAsStr, glideRef)
            if (byteArray == null) {
                Log.e(
                    LOG_TAG,
                    "from fyredApp | uploadUserProfilePic compression failed"
                )
                callback.onAsyncOpComplete(isSuccessful = false)
            } else {
                val fileRef = userProfilePhotosFolder.child("profile_photo")
                val uploadTask = fileRef.putBytes(byteArray)
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    fileRef.downloadUrl
                }.addOnSuccessListener {
                    Log.d(LOG_TAG, "from fyredApp | uploadUserProfilePic uploaded $it")
                    callback.onAsyncOpComplete(
                        isSuccessful = true,
                        data = it.toString()
                    )
                }.addOnFailureListener {
                    // Handle failures
                    Log.e(
                        LOG_TAG,
                        "from fyredApp | uploadUserProfilePic Failed ${it.message}",
                        it.cause
                    )
                    callback.onAsyncOpComplete(isSuccessful = false)
                }
            }

        }
    }

    fun updateUserAuthPhotoUri(
        newPhotoUriAsStr: String?,
        callback: AsyncResultListener
    ) {
        if (!userIsLoggedIn()) {
            Log.e(LOG_TAG, "from fyredApp | updateUserAuthPhotoUri() user is logged out")
            callback.onAsyncOpComplete(isSuccessful = false)
            return
        }

        Log.d(LOG_TAG, "from fyredApp | updateUserAuthPhotoUri")
        val user = FirebaseAuth.getInstance().currentUser!!
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(newPhotoUriAsStr?.toUri())
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update successful
                    Log.d(LOG_TAG, "from fyredApp | updateUserAuthPhotoUri successful")
                    callback.onAsyncOpComplete(isSuccessful = true)
                } else {
                    Log.d(
                        LOG_TAG,
                        "from fyredApp | updateUserAuthPhotoUri failed ${task.exception?.message}",
                        task.exception?.cause
                    )
                    callback.onAsyncOpComplete(
                        isSuccessful = false
                    )
                }
            }
    }

    fun deleteMyProfilePhotoFromStorage(callback: AsyncResultListener) {
        if (!userIsLoggedIn()) {
            Log.e(LOG_TAG, "from fyredApp |  deleteMyProfilePhotoFromStorage() user is logged out")
            callback.onAsyncOpComplete(isSuccessful = false)
            return
        }

        Log.d(
            LOG_TAG,
            "from fyredApp |  deleteMyProfilePhotoFromStorage deleting pic"
        )
        val fileRef = userProfilePhotosFolder.child("profile_photo")
        fileRef.delete()
            .addOnSuccessListener {
                Log.d(
                    LOG_TAG,
                    "from fyredApp |  deleteMyProfilePhotoFromStorage deleted profile pic"
                )
                callback.onAsyncOpComplete(isSuccessful = true)
            }
            .addOnFailureListener {
                Log.e(
                    LOG_TAG,
                    "from fyredApp |  deleteMyProfilePhotoFromStorage failed to delete ${it.message}",
                    it.cause
                )
                //TODO? Let user proceed anyway
                callback.onAsyncOpComplete(isSuccessful = true)
            }
    }

    fun uploadUserFeedback(rating: Float, feedback: String, callback: AsyncResultListener) {
        Log.d(LOG_TAG, "uploading user feedback")
        val userFeedback =
            UserFeedback(userRating = rating, userFeedback = feedback, isPositive = (rating > 3))
        FirebaseFirestore.getInstance().collection(USER_FEEDBACK_COLLECTION_NAME)
            .document(getUserId())
            .set(userFeedback)
            .addOnSuccessListener {
                callback.onAsyncOpComplete(isSuccessful = true)
            }.addOnFailureListener { exception ->
                Log.d(
                    LOG_TAG,
                    " uploadUserFeedback => ${exception.message}",
                    exception.cause
                )
                callback.onAsyncOpComplete(
                    isSuccessful = false
                )
            }
    }


}