package com.dev_vlad.fyredapp.ui.welcome

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.models.Users
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.getCredential

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private val LOG_TAG = LoginViewModel::class.java.simpleName
    }

    //initialize the user
    val user = Users()


    //verification
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var storedVerificationId: String? = null
    private var credentials: PhoneAuthCredential? = null
    var errMessageRes: Int? = null

    enum class LoginProcessStatus {
        SIGNED_OUT,
        VERIFICATION_CODE_SENT,
        VERIFICATION_CODE_AUTO_VERIFIED,
        VERIFICATION_FAILED,
        VERIFICATION_CODE_TIMEOUT,
        SIGNED_IN,
        SIGN_IN_FAILED
    }

    private val loginProcessStatus =
        MutableLiveData(LoginProcessStatus.SIGNED_OUT)

    fun getLoginProcessStatus(): LiveData<LoginProcessStatus> = loginProcessStatus

    val onVerificationStateChangedCallbacks = object :
        PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(LOG_TAG, "from fyredApp | onVerificationCompleted:$credential")
            credentials = credential
            loginProcessStatus.value =
                LoginProcessStatus.VERIFICATION_CODE_AUTO_VERIFIED
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.e(LOG_TAG, "from fyredApp | onVerificationFailed ${e.message}", e.cause)

            errMessageRes = when (e) {
                is FirebaseTooManyRequestsException -> {
                    R.string.too_many_requests
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    R.string.invalid_phone_number
                }
                else -> R.string.verification_failed
            }

            loginProcessStatus.value =
                LoginProcessStatus.VERIFICATION_FAILED

        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(LOG_TAG, "from fyredApp | onCodeSent: $verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token

            loginProcessStatus.value =
                LoginProcessStatus.VERIFICATION_CODE_SENT
        }

        override fun onCodeAutoRetrievalTimeOut(p0: String) {
            super.onCodeAutoRetrievalTimeOut(p0)
            if (loginProcessStatus.value == LoginProcessStatus.VERIFICATION_CODE_SENT)
                loginProcessStatus.value =
                    LoginProcessStatus.VERIFICATION_CODE_TIMEOUT
        }

    }

    fun clearPhoneAuthCredential() {
        credentials = null
    }


    fun getPhoneAuthCredential(userProvidedCode: String): AuthCredential? {
        if (credentials == null && storedVerificationId != null) {
            //auto verification was not done
            //try user provided code
            credentials =
                getCredential(storedVerificationId!!, userProvidedCode)
        }

        return credentials
    }


    /*
    *** SAVES USER DATA TO SERVER & country code to shared preferences for easy access
     */
    fun saveSignedInUserData() {
        UserRepo.saveUserData(
            signedInUser = user,
            callback = object : AsyncResultListener {
                override fun onAsyncOpComplete(isSuccessful: Boolean, data: Any?, errMsgId: Int?) {
                    if (!isSuccessful) {
                        //make sure user is signed out
                        UserRepo.signUserOut()
                        loginProcessStatus.value = LoginProcessStatus.SIGN_IN_FAILED
                    } else {
                        loginProcessStatus.value = LoginProcessStatus.SIGNED_IN
                    }
                }
            }
        )


    }

}