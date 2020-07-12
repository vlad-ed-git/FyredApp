package com.dev_vlad.fyredapp.ui.hotspots

import android.content.Intent
import android.net.Uri.parse
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentHotSpotBinding
import com.dev_vlad.fyredapp.media.CustomVideoPlayer
import com.dev_vlad.fyredapp.models.RecordedMoment
import com.dev_vlad.fyredapp.models.UserMomentWrapper
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.dev_vlad.fyredapp.utils.showSnackBarToUser
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.tabs.TabLayoutMediator

class HotSpotFragment : Fragment() {

    private lateinit var viewModel: HotSpotViewModel
    private lateinit var binding: FragmentHotSpotBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_hot_spot, container, false)
        viewModel = ViewModelProvider(this).get(
            HotSpotViewModel::class.java
        )

        return binding.root
    }

    private var recorderAndMoment: UserMomentWrapper? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            recorderAndMoment = HotSpotFragmentArgs.fromBundle(it).hotspot
        }
        recorderAndMoment?.let { recorderNMoment ->
            //initialize views
            if (recorderNMoment.recordedBy.userId == UserRepo.getUserId()) {
                binding.contactNameTv.text = getString(R.string.my_name)
                binding.goToLocationTv.text = getString(R.string.directions_for_me_txt)
            } else {
                binding.contactNameTv.text = recorderNMoment.recordedBy.phoneBookName
                binding.goToLocationTv.text = getString(R.string.directions_txt)
            }

            //prepare directions intent
            //res : https://developers.google.com/maps/documentation/urls/android-intents#kotlin
            val location = recorderNMoment.recordedAt
            val directionsUri =
                "google.navigation:q=" + location.latitude + "," + location.longitude
            val gmmIntentUri =
                parse(directionsUri)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            binding.goToLocationTv.setOnClickListener {
                if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    binding.container.showSnackBarToUser(
                        msgResId = R.string.no_google_maps_handler_app,
                        actionMessage = R.string.ok
                    )
                }

            }


            Glide.with(requireContext())
                .load(recorderNMoment.recordedBy.userPhotoUriStr)
                .placeholder(R.drawable.ic_profile_pic_pin_placeholder)
                .error(R.drawable.ic_empty_profile_pic)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profilePic)

            setupViewPager(recorderNMoment.recordedMoments)

        }

    }

    private fun setupViewPager(recordedMoments: List<RecordedMoment>) {
        binding.momentsViewPager.adapter = MomentsAdapter(this, recordedMoments)
        TabLayoutMediator(binding.tabDots, binding.momentsViewPager) { _, _ -> }.attach()

    }

}

class MomentsAdapter(fragment: Fragment, private val moments: List<RecordedMoment>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = moments.size

    override fun createFragment(position: Int): Fragment {
        val fragment = MomentFragment()
        fragment.arguments = Bundle().apply {
            putParcelable(MOMENT_KEY, moments[position])
        }
        return fragment
    }
}


private const val MOMENT_KEY = "moment"

class MomentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_moment, container, false)
    }

    private var videoPlayer: CustomVideoPlayer? = null
    private lateinit var moment: RecordedMoment
    private lateinit var captionTv: TextView
    private lateinit var playerView: PlayerView
    private lateinit var imgView: ImageView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(MOMENT_KEY) }?.apply {
            moment = getParcelable<RecordedMoment>(MOMENT_KEY)!!
            captionTv = view.findViewById<TextView>(R.id.caption_tv)
            playerView = view.findViewById<PlayerView>(R.id.moment_vid_pv)
            imgView = view.findViewById<ImageView>(R.id.moment_photo_iv)
        }
    }

    private fun resume() {
        //display caption
        val caption = moment.caption
        if (!caption.isBlank() && caption.isNotEmpty()) {
            captionTv.text = caption
        } else {
            captionTv.visibility = View.GONE
        }


        if (moment.image) {
            //show image
            playerView.visibility = View.GONE
            imgView.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(moment.mediaUriString)
                .placeholder(R.drawable.ic_img_loading_placeholder)
                .into(imgView)
        } else {
            //play video
            imgView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            videoPlayer =
                CustomVideoPlayer(momentVideoPv = playerView, videoUriStr = moment.mediaUriString)
        }
    }

    override fun onResume() {
        super.onResume()
        resume()
    }

    override fun onPause() {
        super.onPause()
        videoPlayer?.releasePlayer()
    }

}