package com.dev_vlad.fyredapp.ui.about_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentSubmitFeedbackBinding
import com.dev_vlad.fyredapp.interfaces.AsyncResultListener
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.dev_vlad.fyredapp.utils.showSnackBarToUser

class SubmitFeedbackFragment : Fragment() {

    private lateinit var binding: FragmentSubmitFeedbackBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_submit_feedback, container, false)
        binding.submitBtn.setOnClickListener {
            uploadFeedback()
        }
        return binding.root
    }


    private fun uploadFeedback() {
        binding.submitBtn.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        UserRepo.uploadUserFeedback(
            rating = binding.rating.rating,
            feedback = binding.feedbackEt.text.toString(),
            callback = object : AsyncResultListener {
                override fun onAsyncOpComplete(isSuccessful: Boolean, data: Any?, errMsgId: Int?) {

                    binding.progressBar.visibility = View.GONE
                    binding.submitBtn.visibility = View.VISIBLE
                    if (isSuccessful) {
                        binding.container.showSnackBarToUser(
                            msgResId = R.string.feedback_sent_successful,
                            isErrorMsg = false,
                            actionMessage = R.string.ok,
                            actionToTake = {
                                findNavController().navigateUp()
                            }
                        )
                    } else {
                        binding.container.showSnackBarToUser(
                            msgResId = R.string.feedback_sending_failed,
                            isErrorMsg = true,
                            actionMessage = R.string.ok,
                            actionToTake = {}
                        )
                    }
                }

            }
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            binding.submitBtn.visibility = it.getInt("submitBtnVisibility")
            binding.progressBar.visibility = it.getInt("progressBarVisibility")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("submitBtnVisibility", binding.submitBtn.visibility)
        outState.putInt("progressBarVisibility", binding.progressBar.visibility)
    }

}