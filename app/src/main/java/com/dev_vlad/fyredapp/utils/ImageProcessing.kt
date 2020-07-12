package com.dev_vlad.fyredapp.utils

import android.graphics.Bitmap
import com.bumptech.glide.RequestManager
import com.dev_vlad.fyredapp.utils.AppConstants.IMG_COMPRESS_FACTOR
import com.dev_vlad.fyredapp.utils.AppConstants.PREFERRED_IMG_HEIGHT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream


object ImageProcessing {

    suspend fun getBitmapFromDrawable(
        glideRef: RequestManager,
        imgRes: Int? = null,
        imgUriString: String? = null
    ): Bitmap = withContext(Dispatchers.IO) {

        val imgSrc = imgRes ?: imgUriString
        val futureTarget = glideRef
            .asBitmap()
            .load(imgSrc)
            .submit()
        futureTarget.get()
    }


    suspend fun scaleAndResizeImageAsync(
        photoUriStr: String,
        glideRef: RequestManager?
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            MyLog.d(LOG_TAG, "from fyredApp | scaling and resizing image at uri str : $photoUriStr")
            val futureTarget = glideRef!!
                .asBitmap()
                .load(photoUriStr)
                .submit(0, PREFERRED_IMG_HEIGHT)

            val bitmap = futureTarget.get()
            glideRef.clear(futureTarget)

            MyLog.d(
                " ImageProcessing",
                "from fyredApp | new bitmap w * h ${bitmap.width} ,  ${bitmap.height}"
            )

            //compress after resizing
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                IMG_COMPRESS_FACTOR,
                byteArrayOutputStream
            )
            val byteArray = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.flush()
            byteArrayOutputStream.close()
            byteArray

        } catch (exc: Exception) {
            MyLog.d(LOG_TAG, "from fyredApp | Failed to compress image ${exc.message}", exc.cause)
            null
        }
    }

    private val LOG_TAG = ImageProcessing::class.java.simpleName
}