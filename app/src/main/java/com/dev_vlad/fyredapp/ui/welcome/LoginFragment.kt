package com.dev_vlad.fyredapp.ui.welcome

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentLoginBinding
import com.dev_vlad.fyredapp.ui.welcome.LoginViewModel.LoginProcessStatus.*
import com.dev_vlad.fyredapp.utils.AppConstants
import com.dev_vlad.fyredapp.utils.AppConstants.USERS_COUNTRY_CODE_KEY
import com.dev_vlad.fyredapp.utils.PhoneNumberValidator.getOnlyCodesList
import com.dev_vlad.fyredapp.utils.PhoneNumberValidator.getOnlyCountriesList
import com.dev_vlad.fyredapp.utils.PhoneNumberValidator.getPhoneNumberIfValid
import com.dev_vlad.fyredapp.utils.showSnackBarToUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    companion object {
        private val LOG_TAG = LoginFragment::class.java.simpleName
    }

    private val loginViewModel: LoginViewModel by lazy {
        ViewModelProvider(this).get(
            LoginViewModel::class.java
        )
    }
    private lateinit var binding: FragmentLoginBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        setupCountrySpinner()
        binding.getCodeBtn.setOnClickListener {
            onGetVerificationCodeClicked()
        }

        binding.signInBtn.setOnClickListener {
            signInUser()
        }

        binding.termsNPolicyTv.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_aboutAppFragment)
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreSavedInstanceState(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loginViewModel.getLoginProcessStatus().observe(
            viewLifecycleOwner,
            Observer { loginStatus ->
                when (loginStatus) {
                    VERIFICATION_CODE_SENT -> {
                        hideProgressBars()
                        binding.getCodeBtn.setText(R.string.verification_code_sent_hint)
                        binding.fragmentLoginContainer.showSnackBarToUser(
                            msgResId = R.string.verification_code_sent_long_msg,
                            isErrorMsg = false
                        )
                    }
                    VERIFICATION_CODE_AUTO_VERIFIED -> {
                        //auto sign in
                        signInUser()
                    }
                    VERIFICATION_FAILED -> {
                        hideProgressBars()
                        binding.getCodeBtn.setText(R.string.resend_verification_code)
                        val errMsgRes =
                            loginViewModel.errMessageRes ?: R.string.invalid_phone_number
                        binding.fragmentLoginContainer.showSnackBarToUser(
                            msgResId = errMsgRes,
                            isErrorMsg = true
                        )

                    }
                    VERIFICATION_CODE_TIMEOUT -> {
                        //allow user to request verification code again but keep the sign in button in whatever state it is
                        hideProgressBars(changeSignInBtnState = false)
                        binding.getCodeBtn.setText(R.string.resend_verification_code)
                    }
                    SIGNED_IN -> {
                        hideProgressBars()
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                    SIGN_IN_FAILED -> {
                        hideProgressBars()
                        binding.fragmentLoginContainer.showSnackBarToUser(
                            msgResId = R.string.sign_in_failed,
                            isErrorMsg = true
                        )
                    }

                    else -> {
                    }
                }
            }
        )
    }


    private fun setupCountrySpinner() {
        val countryCodesAndNames =
            resources.getStringArray(R.array.country_codes_n_names)
        val countryNames: ArrayList<String> =
            getOnlyCountriesList(countryCodesAndNames)
        val countryCodes = getOnlyCodesList(countryCodesAndNames)

        //prepare adapter
        val countryAdapter = ArrayAdapter(
            requireContext(),
            R.layout.custom_spinner_item, countryNames as List<String>
        )

        countryAdapter.setDropDownViewResource(R.layout.custom__spinner_dropdown_item)

        binding.countrySpinner.adapter = countryAdapter

        binding.countrySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                //set the user's location
                loginViewModel.user.signInCountry = countryNames[position]

                //display country code
                val countryCode = countryCodes[position]
                binding.userCountryCodeEt.setText(countryCode)
                loginViewModel.user.signInCountryCode = countryCode

                binding.userPhoneEt.requestFocus()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }


    /******** PHONE NUMBER VERIFICATION ********/
    private fun onGetVerificationCodeClicked() {
        showProgressBar()
        val enteredPhone = binding.userPhoneEt.text.toString()
        val validatedPhone = getPhoneNumberIfValid(
            loginViewModel.user.signInCountryCode,
            enteredPhone
        )
        Log.d(
            LOG_TAG,
            "from fyredApp | onGetVerificationCodeClicked() -> enteredPhone $enteredPhone validatedPhone $validatedPhone"
        )
        if (validatedPhone != null) {
            loginViewModel.user.phoneNumber = validatedPhone
            if (loginViewModel.resendToken == null) {
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    validatedPhone, // Phone number to verify
                    60, // Timeout duration
                    TimeUnit.SECONDS, // Unit of timeout
                    requireActivity(), // Activity (for callback binding)
                    loginViewModel.onVerificationStateChangedCallbacks
                )
            } else {
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    validatedPhone, // Phone number to verify
                    60, // Timeout duration
                    TimeUnit.SECONDS, // Unit of timeout
                    requireActivity(), // Activity (for callback binding)
                    loginViewModel.onVerificationStateChangedCallbacks,
                    loginViewModel.resendToken //resend code
                )
            }
        } else {
            hideProgressBars()
            binding.fragmentLoginContainer.showSnackBarToUser(
                msgResId = R.string.invalid_phone_number,
                isErrorMsg = true
            )
        }

    }

    private fun showProgressBar(isVerifyingNotLoginIn: Boolean = true) {
        binding.signInBtn.isEnabled = false
        binding.getCodeBtn.isEnabled = false
        if (isVerifyingNotLoginIn) {
            binding.getCodeBtn.visibility = View.INVISIBLE
            binding.getCodeProgress.visibility = View.VISIBLE
        } else {
            binding.signInBtn.visibility = View.INVISIBLE
            binding.signInProgressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBars(changeSignInBtnState: Boolean = true) {
        binding.getCodeProgress.visibility = View.GONE
        binding.signInProgressBar.visibility = View.GONE
        binding.getCodeBtn.visibility = View.VISIBLE
        binding.getCodeBtn.isEnabled = true
        if (changeSignInBtnState) {
            //using if for readability
            binding.signInBtn.isEnabled = true
            binding.signInBtn.visibility = View.VISIBLE
        }
    }


    /****** SIGN IN *****/
    private fun signInUser() {
        if (loginViewModel.user.phoneNumber == null) {
            binding.fragmentLoginContainer.showSnackBarToUser(
                msgResId = R.string.verify_phone_number,
                isErrorMsg = true
            )
            return
        }
        try {
            showProgressBar(isVerifyingNotLoginIn = false)
            val userProvidedCode = binding.verificationCodeEt.text.toString()
            val phoneAuthCredential = loginViewModel.getPhoneAuthCredential(userProvidedCode)
            FirebaseAuth.getInstance()
                .signInWithCredential(phoneAuthCredential!!)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Log.d(LOG_TAG, "from fyredApp | signInUser -> signed in")

                        requireContext().getSharedPreferences(
                            AppConstants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE
                        ).edit().putString(
                            USERS_COUNTRY_CODE_KEY,
                            loginViewModel.user.signInCountryCode
                        )
                            .apply()

                        loginViewModel.saveSignedInUserData()

                    } else {
                        Log.e(
                            LOG_TAG,
                            "from fyredApp | signInWithCredential Completed: ${task.exception!!.message}",
                            task.exception!!.cause
                        )
                        binding.fragmentLoginContainer.showSnackBarToUser(
                            msgResId = R.string.invalid_verification_code_at_sign_in,
                            isErrorMsg = true
                        )
                        loginViewModel.clearPhoneAuthCredential()
                    }
                }
        } catch (exc: Exception) {
            Log.d(LOG_TAG, "from fyredApp | signInUser : ${exc.message}", exc.cause)
            hideProgressBars()
            loginViewModel.clearPhoneAuthCredential()
            binding.fragmentLoginContainer.showSnackBarToUser(
                msgResId = R.string.sign_in_failed,
                isErrorMsg = true
            )

        }

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("progressBarVisibility", binding.signInProgressBar.visibility)
        outState.putInt("getCodeProgressVisibility", binding.getCodeProgress.visibility)
        outState.putInt("getCodeBtnVisibility", binding.getCodeBtn.visibility)
        outState.putBoolean("getCodeBtnState", binding.getCodeBtn.isEnabled)
        outState.putBoolean("signInBtnState", binding.signInBtn.isEnabled)
        outState.putInt("signInBtnVisibility", binding.signInBtn.visibility)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            binding.signInProgressBar.visibility = it.getInt("progressBarVisibility")
            binding.getCodeProgress.visibility =
                it.getInt("getCodeProgressVisibility")
            binding.getCodeBtn.visibility = it.getInt("getCodeBtnVisibility")
            binding.getCodeBtn.isEnabled = it.getBoolean("getCodeBtnState")
            binding.signInBtn.isEnabled = it.getBoolean("signInBtnState")
            binding.signInBtn.visibility = it.getInt("signInBtnVisibility")
        }
    }

}