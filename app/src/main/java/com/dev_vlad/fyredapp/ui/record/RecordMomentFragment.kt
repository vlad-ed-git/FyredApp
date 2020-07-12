package com.dev_vlad.fyredapp.ui.record

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentRecordMomentBinding
import com.dev_vlad.fyredapp.models.CustomLatLng
import com.dev_vlad.fyredapp.models.RecordedMoment
import com.dev_vlad.fyredapp.ui.adapters.RecordedMomentsAdapter
import com.dev_vlad.fyredapp.ui.dialogs.EditMyMomentDialog
import com.dev_vlad.fyredapp.ui.record.RecordMomentViewModel.UploadingState.*
import com.dev_vlad.fyredapp.utils.AppConstants.MAX_MOMENTS
import com.dev_vlad.fyredapp.utils.AppConstants.MAX_VIDEO_SECONDS
import com.dev_vlad.fyredapp.utils.AppConstants.PERMISSION_REQUEST_CAMERA
import com.dev_vlad.fyredapp.utils.AppConstants.PERMISSION_REQUEST_CAMERA_FOR_VIDEO
import com.dev_vlad.fyredapp.utils.AppConstants.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
import com.dev_vlad.fyredapp.utils.hasAppPermission
import com.dev_vlad.fyredapp.utils.requestAppPermissions
import com.dev_vlad.fyredapp.utils.shouldShowRationaleForAppPermission
import com.dev_vlad.fyredapp.utils.showSnackBarToUser
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class RecordMomentFragment : Fragment() {

    companion object {
        private val LOG_TAG = RecordMomentFragment::class.java.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_GALLERY_ACCESS = 26
        private const val REQUEST_CODE_VIDEO_CAPTURE = 6
        private const val REQUEST_CODE_IMG_CAPTURE = 89
    }

    private lateinit var recordViewModel: RecordMomentViewModel
    private lateinit var binding: FragmentRecordMomentBinding

    private var permissionJustDenied: Int? = null
    private lateinit var adapter: RecordedMomentsAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var onMomentClickedListener: RecordedMomentsAdapter.UserMomentClickListener
    private var editMomentDialog: EditMyMomentDialog? = null
    private var location: CustomLatLng? = null
    private var showMaxMomentsInfo = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_record_moment, container, false)
        recordViewModel = ViewModelProvider(this).get(
            RecordMomentViewModel::class.java
        )

        //setup menu
        setHasOptionsMenu(true)
        requireActivity().invalidateOptionsMenu()

        //setup adapter
        onMomentClickedListener = RecordedMomentsAdapter.UserMomentClickListener { clickedMoment ->
            onMomentClicked(clickedMoment)
        }
        adapter = RecordedMomentsAdapter(onMomentClickedListener)
        linearLayoutManager = LinearLayoutManager(
            requireContext(),
            RecyclerView.VERTICAL,
            false
        )
        binding.myMomentsRv.layoutManager = linearLayoutManager
        binding.myMomentsRv.adapter = adapter

        // Setup the listener for take photo button
        binding.takePhotoTv.setOnClickListener { takePhoto() }

        //and for photo gallery
        binding.photoGalleryTv.setOnClickListener { pickPhotoFromGallery() }

        //and for video recording
        binding.recordVideoTv.setOnClickListener { recordVideo() }
        return binding.root

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let { restoreSavedState(it) }

        arguments?.let {
            location = RecordMomentFragmentArgs.fromBundle(it).userLastKnownLocation
        }

        recordViewModel.getUploadingStatus().observe(viewLifecycleOwner,
            androidx.lifecycle.Observer {
                when (it) {

                    DONE_SUCCESSFULLY -> {
                        hideProgressBarAndEnableActions()
                        binding.recordFragmentLayout.showSnackBarToUser(
                            msgResId = R.string.moment_shared,
                            isErrorMsg = false,
                            actionMessage = R.string.ok,
                            actionToTake = {
                                findNavController(this@RecordMomentFragment).navigateUp()
                            }
                        )

                    }
                    ONGOING -> {
                        binding.emptyMomentsTv.setText(R.string.sharing_e_moment_wait)
                        disableActionsAndShowProgress()
                    }
                    FAILED -> {
                        binding.progressBar.visibility = View.GONE
                        binding.recordFragmentLayout.showSnackBarToUser(
                            msgResId = R.string.err_try_again,
                            isErrorMsg = true
                        )

                    }
                    else -> {
                        hideProgressBarAndEnableActions()
                    }
                }
            })

        recordViewModel.getLiveRecordedMoments().observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { recordedMoments ->
                val totalMomentsSoFar = recordedMoments.size
                Log.d(LOG_TAG, "from fyredApp | total moments $totalMomentsSoFar")
                if (totalMomentsSoFar > 0) {
                    binding.emptyMomentsTv.visibility = View.GONE
                    if (totalMomentsSoFar >= MAX_MOMENTS) {
                        disableActions()
                        if (showMaxMomentsInfo) {
                            showMaxMomentsInfo = false
                            binding.recordFragmentLayout.showSnackBarToUser(
                                msgResId = R.string.max_moments_reached,
                                isErrorMsg = false,
                                actionMessage = R.string.got_it,
                                actionToTake = {}
                            )
                        }
                    } else {
                        enableActions()
                    }
                } else {
                    binding.emptyMomentsTv.visibility = View.VISIBLE
                    enableActions()
                }

                if (::adapter.isInitialized) {
                    Log.d(LOG_TAG, "from fyredApp | moments list changed")
                    adapter.submitList(recordedMoments.toMutableList())
                }
            })
    }

    /************* PROGRESS INDICATION ************/
    private fun disableActions() {
        binding.takePhotoTv.isEnabled = false
        binding.recordVideoTv.isEnabled = false
        binding.photoGalleryTv.isEnabled = false
    }

    private fun enableActions() {
        binding.takePhotoTv.isEnabled = true
        binding.recordVideoTv.isEnabled = true
        binding.photoGalleryTv.isEnabled = true
    }

    private fun disableActionsAndShowProgress() {
        disableActions()
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyMomentsTv.visibility = View.VISIBLE
    }

    private fun hideProgressBarAndEnableActions() {
        binding.emptyMomentsTv.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        enableActions()
    }


    private fun recordVideo() {
        when {
            hasAppPermission(Manifest.permission.CAMERA) -> {
                Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                    takeVideoIntent.putExtra(
                        "android.intent.extra.durationLimit",
                        MAX_VIDEO_SECONDS
                    )
                    takeVideoIntent.resolveActivity(requireContext().packageManager)?.also {
                        startActivityForResult(takeVideoIntent, REQUEST_CODE_VIDEO_CAPTURE)
                    }
                }
            }
            shouldShowRationaleForAppPermission(Manifest.permission.CAMERA) -> {
                binding.recordFragmentLayout.showSnackBarToUser(
                    msgResId = R.string.camera_access_rationale,
                    isErrorMsg = false,
                    actionMessage = R.string.ok,
                    actionToTake = {
                        requestAppPermissions(
                            permissionsArr = arrayOf(Manifest.permission.CAMERA),
                            requestCode = PERMISSION_REQUEST_CAMERA_FOR_VIDEO
                        )
                    }
                )

            }
            else -> {
                requestAppPermissions(
                    permissionsArr = arrayOf(Manifest.permission.CAMERA),
                    requestCode = PERMISSION_REQUEST_CAMERA_FOR_VIDEO
                )
            }
        }
    }

    private fun pickPhotoFromGallery() {
        when {
            hasAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.access_gallery_prompt)),
                    REQUEST_CODE_GALLERY_ACCESS
                )
            }
            shouldShowRationaleForAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                binding.recordFragmentLayout.showSnackBarToUser(
                    msgResId = R.string.gallery_access_rationale,
                    isErrorMsg = false,
                    actionMessage = R.string.ok,
                    actionToTake = {
                        requestAppPermissions(
                            permissionsArr = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            requestCode = PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
                        )
                    }
                )

            }
            else -> {
                requestAppPermissions(
                    permissionsArr = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    requestCode = PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        }

    }

    private fun takePhoto() {
        when {
            hasAppPermission(Manifest.permission.CAMERA) -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    // Ensure that there's a camera activity to handle the intent
                    takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                        // Create the File where the photo should go
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            // Error occurred while creating the File
                            Log.d(
                                LOG_TAG,
                                "from fyredApp | takePhoto() ->  Error occurred while creating the File"
                            )
                            Log.d(LOG_TAG, ex.message, ex.cause)
                            binding.recordFragmentLayout.showSnackBarToUser(
                                msgResId = R.string.failed_to_set_photo_path,
                                isErrorMsg = true
                            )
                            null
                        }
                        // Continue only if the File was successfully created
                        photoFile?.also {
                            val photoURI: Uri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.dev_vlad.fyredapp.fileprovider",
                                it
                            )
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            startActivityForResult(takePictureIntent, REQUEST_CODE_IMG_CAPTURE)
                        }
                    }
                }
            }
            shouldShowRationaleForAppPermission(Manifest.permission.CAMERA) -> {
                binding.recordFragmentLayout.showSnackBarToUser(
                    msgResId = R.string.camera_access_rationale,
                    isErrorMsg = false,
                    actionToTake = {
                        requestAppPermissions(
                            permissionsArr = arrayOf(Manifest.permission.CAMERA),
                            requestCode = PERMISSION_REQUEST_CAMERA
                        )
                    }
                )

            }
            else -> {
                requestAppPermissions(
                    permissionsArr = arrayOf(Manifest.permission.CAMERA),
                    requestCode = PERMISSION_REQUEST_CAMERA
                )
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date())
        val storageDir: File? =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            recordViewModel.pathOfJustTakenPhoto = absolutePath
        }
    }

    private fun onMomentClicked(recordedMoment: RecordedMoment) {
        Log.d(LOG_TAG, "from fyredApp | moment clicked")
        editMomentDialog = EditMyMomentDialog(
            recordedMoment = recordedMoment
        )
        editMomentDialog?.let {

            it.listener = object : EditMyMomentDialog.MyMomentDialogListener {


                override fun onDeleteMomentClicked(removedMoment: RecordedMoment) {
                    recordViewModel.removeMoment(removedMoment)
                    it.dismiss()
                }

                override fun onEditMoment(oldMoment: RecordedMoment, newMoment: RecordedMoment) {
                    recordViewModel.modifyMoment(oldMoment = oldMoment, newMoment = newMoment)
                    it.dismiss()
                }
            }

            it.show(parentFragmentManager, "fyredapp.ui.dialogs.EditMyMomentDialog")

        }

    }


    /************* ON ACTIVITY RESULT*************/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {

            requestCode == REQUEST_CODE_GALLERY_ACCESS && resultCode == RESULT_OK -> {
                //is gallery access -- multi selection has been allowed
                if (data != null) {
                    when {
                        data.clipData != null -> {
                            val mClipData: ClipData? = data.clipData
                            if (mClipData != null) {
                                val selectedImages = mClipData.itemCount
                                for (index in 0 until selectedImages) {
                                    val item: ClipData.Item = mClipData.getItemAt(index)
                                    Log.d(
                                        LOG_TAG,
                                        "from fyredApp | on act result, adding 1 moment of many"
                                    )
                                    val moment = RecordedMoment(
                                        mediaUriString = item.uri.toString(),
                                        image = true
                                    )
                                    recordViewModel.addMoment(moment)
                                }
                            }
                        }

                        data.data != null -> {
                            Log.d(LOG_TAG, "from fyredApp | on act result, adding 1 gallery pic")
                            val moment = RecordedMoment(
                                mediaUriString = data.data.toString(),
                                image = true
                            )
                            recordViewModel.addMoment(moment)
                        }

                    }
                }


            }

            requestCode == REQUEST_CODE_VIDEO_CAPTURE && resultCode == RESULT_OK -> {
                //is video capture access
                if (data != null && data.data != null) {
                    Log.d(LOG_TAG, "from fyredApp | on act result, adding 1 video")
                    val moment = RecordedMoment(
                        mediaUriString = data.data.toString(),
                        image = false
                    )
                    recordViewModel.addMoment(moment)
                }
            }


            requestCode == REQUEST_CODE_IMG_CAPTURE && resultCode == RESULT_OK -> {
                //is img capture access
                if (recordViewModel.pathOfJustTakenPhoto != null) {
                    val fullImgUri = Uri.fromFile(
                        File(
                            recordViewModel.pathOfJustTakenPhoto!!
                        )
                    )
                    Log.d(LOG_TAG, "from fyredApp | on act result, adding 1 captured photo")
                    val moment = RecordedMoment(
                        mediaUriString = fullImgUri.toString(),
                        image = true
                    )
                    recordViewModel.addMoment(moment)
                } else {
                    Log.e(LOG_TAG, "from fyredApp | on act result, pathOfJustTakenPhoto is null")
                }
            }
        }
    }

    private fun sendCapturedMoments() {
        if (location != null) {
            Log.d(
                LOG_TAG,
                "from fyredApp | Sending captured moments from ${location!!.latitude}, ${location!!.longitude}"
            )
            //send user's moment
            recordViewModel.uploadUserMoment(userMomentsTakenAtLatLng = location!!)

        } else {
            Log.d(LOG_TAG, "from fyredApp | Failed to send captured moments, user location is null")

        }
    }


    /***** PERMISSIONS ****/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                // Request for camera permission.
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    //Camera Permission request was denied
                    permissionJustDenied = R.string.camera_access_denied

                }
            }
            PERMISSION_REQUEST_CAMERA_FOR_VIDEO -> {
                // Request for camera permission.
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recordVideo()
                } else {
                    //Camera Permission request was denied
                    permissionJustDenied = R.string.camera_access_denied

                }
            }
            PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // Request for storage permission
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickPhotoFromGallery()
                } else {
                    //WRITE STORAGE Permission request was denied
                    permissionJustDenied = R.string.gallery_access_denied
                }
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("emptyMomentsTvVisibility", binding.emptyMomentsTv.visibility)
        outState.putInt("progressBarVisibility", binding.progressBar.visibility)
    }

    private fun restoreSavedState(savedInstanceState: Bundle) {
        binding.emptyMomentsTv.visibility = savedInstanceState.getInt("emptyMomentsTvVisibility")
        binding.progressBar.visibility = savedInstanceState.getInt("progressBarVisibility")
    }


    override fun onResume() {
        super.onResume()
        permissionJustDenied?.let {
            binding.recordFragmentLayout.showSnackBarToUser(
                msgResId = it,
                isErrorMsg = true
            )
            permissionJustDenied = null
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        Log.d(LOG_TAG, "from fyredApp | onPrepareOptionsMenu called")
        menu.findItem(R.id.shareMoment).isVisible = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "from fyredApp | onOptionsItemSelected called")
        return when (item.itemId) {
            R.id.shareMoment -> {
                sendCapturedMoments()
                true
            }
            else -> item.onNavDestinationSelected(findNavController(this)) || super.onOptionsItemSelected(
                item
            )
        }
    }

}