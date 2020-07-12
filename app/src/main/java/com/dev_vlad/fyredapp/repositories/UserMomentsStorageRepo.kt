package com.dev_vlad.fyredapp.repositories

import androidx.core.net.toUri
import com.bumptech.glide.RequestManager
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.models.RecordedMoment
import com.dev_vlad.fyredapp.models.UserMomentWrapper
import com.dev_vlad.fyredapp.utils.AppConstants
import com.dev_vlad.fyredapp.utils.AppConstants.MOMENTS_STORAGE_FOLDER
import com.dev_vlad.fyredapp.utils.ImageProcessing
import com.dev_vlad.fyredapp.utils.MyLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object UserMomentsStorageRepo {

    private val LOG_TAG = UserMomentsStorageRepo::class.java.simpleName
    val userMomentsFolder by lazy {
        FirebaseStorage.getInstance().getReference(MOMENTS_STORAGE_FOLDER).child(
            UserRepo.getUserId()
        )
    }


    /**************** RECURSIVE UPLOAD OF MOMENT FILES ***************/
    private var currentlyUploadingFileNo: Int = -1
    private lateinit var filesBeingUploaded: List<RecordedMoment>
    private lateinit var onUploadMomentsCompleted: AsyncResultListener
    private lateinit var momentsBeingUploaded: UserMomentWrapper
    private lateinit var glideRef: RequestManager
    private lateinit var currentlyUploadingMoment: RecordedMoment
    fun uploadRecordedMoments(
        wrappedUserNMoment: UserMomentWrapper,
        callback: AsyncResultListener,
        imgCompressHelper: RequestManager
    ) {
        momentsBeingUploaded = wrappedUserNMoment
        currentlyUploadingFileNo = 0
        filesBeingUploaded = wrappedUserNMoment.recordedMoments
        onUploadMomentsCompleted = callback
        glideRef = imgCompressHelper
        recursivelyUploadFiles()
    }

    private fun recursivelyUploadFiles() {
        currentlyUploadingMoment = filesBeingUploaded[currentlyUploadingFileNo]
        if (currentlyUploadingMoment.image) {
            val fileToUpload = currentlyUploadingMoment.mediaUriString
            CoroutineScope(Dispatchers.IO).launch {
                val byteArray = ImageProcessing.scaleAndResizeImageAsync(fileToUpload, glideRef)
                withContext(Dispatchers.Main) {
                    if (byteArray == null) {
                        MyLog.e(
                            LOG_TAG,
                            "from fyredApp | compressing image $currentlyUploadingFileNo failed"
                        )
                        //upload as a file
                        uploadMomentAsAFile(fileToUpload)
                    } else {
                        MyLog.d(LOG_TAG, "from fyredApp | compressing image worked")
                        uploadMomentAsAByteArray(byteArray)
                    }
                }
            }
        } else {
            uploadMomentAsAFile(momentFile = currentlyUploadingMoment.mediaUriString)
        }
    }

    private fun uploadNextFile() {
        currentlyUploadingFileNo += 1
        if (filesBeingUploaded.size == currentlyUploadingFileNo) {
            //all files have been uploaded
            uploadMomentsInfo()
        } else {
            recursivelyUploadFiles()
        }
    }

    private fun uploadMomentAsAFile(momentFile: String) {
        MyLog.d(LOG_TAG, "from fyredApp | uploadMomentAsAFile called")
        try {
            val fileRef = userMomentsFolder.child(getTimeStampAsStr())
            val uploadTask = fileRef.putFile(momentFile.toUri())

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                fileRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    task.result?.let {
                        currentlyUploadingMoment.mediaUriString = it.toString()
                        MyLog.e(
                            LOG_TAG,
                            "from fyredApp | uploadMomentAsAFile file $currentlyUploadingFileNo uploaded"
                        )
                    }
                    uploadNextFile()

                } else {
                    // Handle failures
                    MyLog.e(
                        LOG_TAG,
                        "from fyredApp | uploadMomentAsAFile Failed ${task.exception?.message}",
                        task.exception?.cause
                    )
                    onUploadMomentsCompleted.onAsyncOpComplete(
                        isSuccessful = false
                    )
                }
            }
        } catch (exc: Exception) {
            MyLog.e(
                LOG_TAG,
                "from fyredApp | uploadMomentAsAFile exception occurred -> ${exc.message}",
                exc.cause
            )
            onUploadMomentsCompleted.onAsyncOpComplete(
                isSuccessful = false
            )
        }

    }

    private fun uploadMomentAsAByteArray(byteArray: ByteArray) {
        MyLog.d(LOG_TAG, "from fyredApp | uploadMomentAsAByteArray called")
        try {
            val fileRef = userMomentsFolder.child(getTimeStampAsStr())
            val uploadTask = fileRef.putBytes(byteArray)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                fileRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    task.result?.let {
                        currentlyUploadingMoment.mediaUriString = it.toString()
                        MyLog.d(
                            LOG_TAG,
                            "from fyredApp | uploadMomentAsAByteArray file $currentlyUploadingFileNo uploaded"
                        )
                    }

                    uploadNextFile()


                } else {
                    // Handle failures
                    MyLog.e(
                        LOG_TAG,
                        "from fyredApp | uploadMomentAsAByteArray Failed ${task.exception?.message}",
                        task.exception?.cause
                    )
                    onUploadMomentsCompleted.onAsyncOpComplete(
                        isSuccessful = false
                    )
                }
            }
        } catch (exc: Exception) {
            MyLog.e(
                LOG_TAG,
                "from fyredApp | uploadMomentAsAByteArray exception occurred -> ${exc.message}",
                exc.cause
            )
            onUploadMomentsCompleted.onAsyncOpComplete(
                isSuccessful = false
            )
        }


    }

    private fun uploadMomentsInfo() {
        MyLog.d(LOG_TAG, "from fyredApp | uploadMomentsInfo called")
        FirebaseFirestore.getInstance()
            .collection(AppConstants.USERS_MOMENTS_COLLECTION_NAME)
            .document(momentsBeingUploaded.recordedBy.userId!!)
            .set(momentsBeingUploaded)
            .addOnSuccessListener {
                MyLog.d(LOG_TAG, "from fyredApp | moments uploaded sucessfully")
                onUploadMomentsCompleted.onAsyncOpComplete(
                    isSuccessful = true
                )
            }
            .addOnFailureListener {
                MyLog.e(
                    LOG_TAG,
                    "from fyredApp | uploading moments info failed ${it.message}",
                    it.cause
                )
                onUploadMomentsCompleted.onAsyncOpComplete(
                    isSuccessful = false
                )
            }
    }

    private fun getTimeStampAsStr(): String {
        return System.currentTimeMillis().toString()
    }
}