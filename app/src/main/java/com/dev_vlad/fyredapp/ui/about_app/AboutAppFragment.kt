package com.dev_vlad.fyredapp.ui.about_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentAboutAppBinding


class AboutAppFragment : Fragment() {


    private lateinit var binding: FragmentAboutAppBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_about_app, container, false)
        binding.privacyPolicy.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://fyredapp.com/privacy_policy.html")
                )
            )
        }
        binding.termsNConditions.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://fyredapp.com/terms_of_service.html")
                )
            )
        }
        return binding.root
    }

}