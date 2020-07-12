package com.dev_vlad.fyredapp.ui.profile

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentUserProfileBinding
import com.dev_vlad.fyredapp.ui.dialogs.UpdateProfilePhotoDialog
import com.dev_vlad.fyredapp.utils.AppConstants.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
import com.dev_vlad.fyredapp.utils.hasAppPermission
import com.dev_vlad.fyredapp.utils.requestAppPermissions
import com.dev_vlad.fyredapp.utils.shouldShowRationaleForAppPermission
import com.dev_vlad.fyredapp.utils.showSnackBarToUser

class UserProfileFragment : Fragment(), UpdateProfilePhotoDialog.UpdateProfilePhotoDialogListener {

    private val userProfileViewModel by lazy {
        ViewModelProvider(this).get(
            UserProfileViewModel::class.java
        )
    }

    private lateinit var binding: FragmentUserProfileBinding

    private var updatePhotoDialog: UpdateProfilePhotoDialog? = null
    private var permissionJustDenied = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_user_profile, container, false)
        binding.profilePicIv.setOnClickListener { onClickTakePhoto() }
        binding.updateBtn.setOnClickListener { onClickUpdateBtn() }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        restoreUserProfile()
        userProfileViewModel.userProfileStatus.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {

                    UserProfileViewModel.UserProfileStatus.UPDATING,
                    UserProfileViewModel.UserProfileStatus.UPLOADING_PROFILE_PIC,
                    UserProfileViewModel.UserProfileStatus.DELETING_PROFILE_PIC -> {
                        binding.updateBtn.visibility = View.GONE
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    UserProfileViewModel.UserProfileStatus.UPDATE_SUCCESSFUL -> {
                        binding.progressBar.visibility = View.GONE
                        binding.updateBtn.visibility = View.VISIBLE
                        binding.fragmentUserProfileContainer.showSnackBarToUser(
                            msgResId = R.string.profile_update_successful,
                            isErrorMsg = false,
                            actionMessage = R.string.ok,
                            actionToTake = {
                                findNavController().navigateUp()
                            }
                        )

                    }
                    UserProfileViewModel.UserProfileStatus.UPDATE_FAILED -> {
                        restoreUserProfile()
                        binding.progressBar.visibility = View.GONE
                        binding.updateBtn.visibility = View.VISIBLE
                        binding.fragmentUserProfileContainer.showSnackBarToUser(
                            msgResId = R.string.profile_update_failed,
                            isErrorMsg = true,
                            actionMessage = R.string.ok,
                            actionToTake = {}
                        )
                    }
                    else -> {
                    }
                }
            })

    }

    private fun restoreUserProfile() {
        binding.userPhoneTv.text = userProfileViewModel.oldUserProfile.phoneNumber
        val oldPic = userProfileViewModel.oldUserProfile.userPhotoUriStr
        Glide.with(requireContext())
            .load(oldPic)
            .placeholder(R.drawable.ic_empty_profile_pic)
            .error(R.drawable.ic_empty_profile_pic)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.profilePicIv)
    }

    private fun onClickTakePhoto() {
        if (userProfileViewModel.userProfileStatus.value != UserProfileViewModel.UserProfileStatus.UPDATING) {
            updatePhotoDialog = UpdateProfilePhotoDialog()
            updatePhotoDialog?.listener = this
            updatePhotoDialog?.show(
                parentFragmentManager,
                "com.dev_vlad.fyredapp.ui.pop_ups.UpdateProfilePhotoDialog"
            )
        }
    }

    private fun onClickUpdateBtn() {
        if (!userProfileViewModel.isProfileUpdating()) {
            userProfileViewModel.uploadProfilePicToStorage()
        }

    }

    private fun showSelectedProfilePhoto() {
        Glide.with(requireContext())
            .load(userProfileViewModel.newPhotoUriAsStr)
            .placeholder(R.drawable.ic_empty_profile_pic)
            .error(R.drawable.ic_add_profile_pic)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.profilePicIv)
    }

    override fun onRemovePhotoClicked() {
        updatePhotoDialog?.dismiss()
        binding.profilePicIv.setImageResource(R.drawable.ic_add_profile_pic)
        userProfileViewModel.newPhotoUriAsStr = null
    }

    override fun onOpenGalleryClicked() {
        updatePhotoDialog?.dismiss()
        when {
            hasAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                picPhotoFromGallery()
            }
            shouldShowRationaleForAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                binding.fragmentUserProfileContainer.showSnackBarToUser(
                    msgResId = R.string.gallery_access_rationale,
                    isErrorMsg = false,
                    actionMessage = R.string.ok,
                    actionToTake = {
                        requestAppPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
                        )
                    })
            }
            else -> {
                requestAppPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun picPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.access_gallery_prompt)),
            REQUEST_CODE_GALLERY_ACCESS
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {

            requestCode == REQUEST_CODE_GALLERY_ACCESS && resultCode == Activity.RESULT_OK -> {
                //is gallery access -- multi selection has been allowed
                if (data != null) {
                    when {
                        data.clipData != null -> {
                            val mClipData: ClipData? = data.clipData
                            if (mClipData != null) {
                                val selectedImages = mClipData.itemCount
                                for (index in 0 until selectedImages) {
                                    val item: ClipData.Item = mClipData.getItemAt(index)
                                    userProfileViewModel.newPhotoUriAsStr = item.uri.toString()
                                    showSelectedProfilePhoto()
                                }
                            }
                        }

                        data.data != null -> {
                            userProfileViewModel.newPhotoUriAsStr = data.data.toString()
                            showSelectedProfilePhoto()
                        }

                    }
                }


            }

        }
    }


    /********************* PERMISSIONS ********************/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // Request for storage permission
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                picPhotoFromGallery()
            } else {
                //WRITE STORAGE Permission request was denied
                permissionJustDenied = true
            }
        }

    }


    /****** constants ***********/
    companion object {
        private const val REQUEST_CODE_GALLERY_ACCESS = 25

    }

    /************LIFECYCLE METHODS ***********/
    override fun onResume() {
        super.onResume()
        if (permissionJustDenied) {
            binding.fragmentUserProfileContainer.showSnackBarToUser(
                msgResId = R.string.gallery_access_denied,
                isErrorMsg = true
            )
            permissionJustDenied = false
        }
    }


}