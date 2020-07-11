package com.dev_vlad.fyredapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentHomeBinding
import com.dev_vlad.fyredapp.location.HotSpotClusterItem
import com.dev_vlad.fyredapp.location.HotSpotClusterItemIconRenderer
import com.dev_vlad.fyredapp.models.UserMomentWrapper
import com.dev_vlad.fyredapp.repositories.UserRepo
import com.dev_vlad.fyredapp.ui.adapters.HotSpotsAdapter
import com.dev_vlad.fyredapp.ui.dialogs.ShareMomentConfirmDialog
import com.dev_vlad.fyredapp.ui.dialogs.ViewHotSpotMomentsDialog
import com.dev_vlad.fyredapp.ui.home.HomeViewModel.ClusterManagerStatus.READY
import com.dev_vlad.fyredapp.ui.home.HomeViewModel.ClusterManagerStatus.UPDATE_ON_READY
import com.dev_vlad.fyredapp.ui.home.HomeViewModel.IntentsAwaitingLocation.*
import com.dev_vlad.fyredapp.utils.*
import com.dev_vlad.fyredapp.utils.AppConstants.PERMISSION_REQUEST_FINE_LOCATION
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HomeFragment : Fragment(), OnMapReadyCallback,
    ShareMomentConfirmDialog.ShareMomentDialogListener,
    ViewHotSpotMomentsDialog.ViewMomentsAtHotSpotDialogListener,
    HotSpotsAdapter.FriendsHotSpotClickListener {


    private lateinit var adapter: HotSpotsAdapter
    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private val locationManager: LocationManager by lazy {
        requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val sharedPref by lazy {
        requireContext().getSharedPreferences(
            AppConstants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE
        )
    }
    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private var googleMap: GoogleMap? = null
    private var mClusterManager: ClusterManager<HotSpotClusterItem>? = null
    private var camPosition: CameraPosition? = null
    private var locationPermissionsJustDenied = false
    private var returningFromLocationSettings = false
    private var myPositionMarker: Marker? = null
    private var mySpotIsHot = false

    //dialogs
    private val shareMomentConfirmDialog by lazy {
        ShareMomentConfirmDialog()
            .also {
                it.listener = this@HomeFragment
            }
    }


    private var viewHotSpotMomentsDialog: ViewHotSpotMomentsDialog? = null

    companion object {
        private val LOG_TAG = HomeFragment::class.java.simpleName
        private const val CAMERA_POSITION_BEARING = "cam_position_bearing"
        private const val CAMERA_POSITION_TILT = "cam_position_tilt"
        private const val CAMERA_POSITION_ZOOM = "cam_position_zoom"
        private const val CAMERA_POSITION_LAT = "cam_position_lat"
        private const val CAMERA_POSITION_LNG = "cam_position_lng"
        private const val DEFAULT_ZOOM = 15
        private const val millSecsBtnUpdates = 5000L
        private const val minMetersBtnUpdates = 5.toFloat()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        homeViewModel = ViewModelProvider(this).get(
            HomeViewModel::class.java
        )
        restoreCameraPosition()
        setupMapThenMyLocationClickListener(savedInstanceState)
        setupRecyclerView()
        binding.recordEMomentFab.setOnClickListener {
            shareMomentAtMyLocation()
        }

        //show my contacts menu and others
        setHasOptionsMenu(true)
        requireActivity().invalidateOptionsMenu()

        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Log.d(LOG_TAG, "from fyredApp | on activity created")

        homeViewModel.getLiveHotSpots().observe(
            viewLifecycleOwner,
            Observer { hotSpotList ->

                updateClusterManager()
                if (::adapter.isInitialized) {
                    Log.d(LOG_TAG, "from fyredApp | submitting  ${hotSpotList.size} hotspots")
                    adapter.submitList(hotSpotList.toMutableList())
                }

            })

        homeViewModel.getMyLiveOrNullSpot().observe(
            viewLifecycleOwner, Observer {
                mySpotIsHot = it != null
                if (mySpotIsHot) {
                    updateClusterManager()
                }

            }
        )

    }

    private fun setupMapThenMyLocationClickListener(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        try {
            Log.d(LOG_TAG, "from fyredApp | initializing the map view")
            MapsInitializer.initialize(requireContext())
            binding.mapView.getMapAsync(this)
            binding.showMyLocationTv.setOnClickListener {
                //attempt to zoom in user location if permissions allow
                if (hasAppPermission(locationPermission)) {
                    setMyLocationThenZoomIn()
                } else {
                    homeViewModel.intentAwaitingPermissions = ZOOM_ON_MY_LOCATION
                    if (shouldShowRationaleForAppPermission(locationPermission)) {
                        binding.homeParentLayout.showSnackBarToUser(
                            msgResId = R.string.location_access_rationale,
                            isErrorMsg = false,
                            actionMessage = R.string.ok,
                            actionToTake = {
                                requestAppPermissions(
                                    permissionsArr = arrayOf(locationPermission),
                                    requestCode = PERMISSION_REQUEST_FINE_LOCATION
                                )
                            }
                        )
                    } else {
                        requestAppPermissions(
                            permissionsArr = arrayOf(locationPermission),
                            requestCode = PERMISSION_REQUEST_FINE_LOCATION
                        )

                    }
                }
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.e(
                LOG_TAG,
                "from fyredApp | Google Play Services not available ${e.message}",
                e.cause
            )
            binding.homeParentLayout.showSnackBarToUser(
                msgResId = R.string.google_play_services_not_available,
                isErrorMsg = true
            )
        }
    }

    override fun onMapReady(gMap: GoogleMap?) {
        if (gMap != null) {
            Log.d(LOG_TAG, "from fyredApp | onMapReady, map is not null")
            googleMap = gMap
            googleMap?.setPadding(0, 0, 0, 104)
            setMapStyle()
            setupClusterManager()
            //attempt to zoom in on user's last known location
            zoomOnLocationOrRestoreCamPos(
                locationToZoomTo = homeViewModel.getUserLastKnownAsLatLng(),
                isMyLocation = true
            )


        } else {
            Log.e(LOG_TAG, "from fyredApp | onMapReady() google map is null")
            binding.homeParentLayout.showSnackBarToUser(
                msgResId = R.string.failed_to_load_map,
                isErrorMsg = true
            )
        }

    }

    private fun setMapStyle() {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            googleMap?.setPadding(0, 0, 0, 104)
            val success = googleMap!!.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style_json
                )
            )

            if (!success) {
                Log.e(LOG_TAG, "from fyredApp | Map Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(LOG_TAG, "from fyredApp | Can't find map style. Error: ", e)
        }
    }

    private fun setupClusterManager() {
        mClusterManager = ClusterManager(requireActivity(), googleMap)
        googleMap!!.setOnCameraIdleListener(mClusterManager)
        googleMap!!.setOnMarkerClickListener(mClusterManager)
        mClusterManager?.renderer = HotSpotClusterItemIconRenderer(
            hotSpotIcon = homeViewModel.hotspotIcon,
            context = requireContext(),
            map = googleMap!!,
            clusterManager = mClusterManager
        )
        if (homeViewModel.clusterManagerStatus == UPDATE_ON_READY)
            updateClusterManager()

    }


    private fun updateClusterManager() {
        if (mClusterManager == null) {
            homeViewModel.clusterManagerStatus = UPDATE_ON_READY
        } else {
            homeViewModel.clusterManagerStatus = READY
            mClusterManager!!.clearItems()
            val hotSpotList = homeViewModel.getLiveHotSpots().value
            if (hotSpotList != null) {
                //add friend's hot spots
                for (hotspot in hotSpotList) {
                    val hotSpotClusterItem = HotSpotClusterItem(
                        lat = hotspot.recordedAt.latitude,
                        lng = hotspot.recordedAt.longitude,
                        tag = hotspot.recordedBy.phoneNumber!!
                    )
                    mClusterManager!!.addItem(hotSpotClusterItem)
                }
            }

            //add my hot spot if not null
            val mySpot = homeViewModel.getMyLiveOrNullSpot().value
            mySpot?.let {
                val hotSpotClusterItem = HotSpotClusterItem(
                    lat = mySpot.recordedAt.latitude,
                    lng = mySpot.recordedAt.longitude,
                    tag = mySpot.recordedBy.userId!!
                )
                mClusterManager!!.addItem(hotSpotClusterItem)
            }

            mClusterManager!!.cluster()
        }
    }


    /********* GO TO LOCATION ********/
    //handle intents when showing a location
    private fun processShareMomentClickIntent() {
        //user intends to share a moment at their location
        lifecycleScope.launch {
            binding.recordEMomentFab.isEnabled = false
            delay(500)
            shareMomentConfirmDialog.show(
                parentFragmentManager,
                "fyredapp.confirm_share_moment"
            )
            binding.recordEMomentFab.isEnabled = true
        }
    }

    private fun processViewMyHotSpotIntent() {
        //user wishes to view their hotspot
        lifecycleScope.launch {
            binding.showMyLocationTv.isEnabled = false
            delay(500)
            homeViewModel.getMyLiveOrNullSpot().value?.let {
                viewHotSpotMomentsDialog = ViewHotSpotMomentsDialog(
                    userMomentWrapper = it
                )
                viewHotSpotMomentsDialog?.listener = this@HomeFragment
                viewHotSpotMomentsDialog?.show(
                    parentFragmentManager,
                    "com.dev_vlad.fyredapp.dialog_view_hotspot"
                )
            }
            binding.showMyLocationTv.isEnabled = true
        }
    }

    private fun processViewFriendsHotSpotIntent(palsHotSpot: UserMomentWrapper) {
        lifecycleScope.launch {
            delay(500)
            viewHotSpotMomentsDialog = ViewHotSpotMomentsDialog(
                userMomentWrapper = palsHotSpot
            )
            viewHotSpotMomentsDialog?.listener = this@HomeFragment
            viewHotSpotMomentsDialog?.show(
                parentFragmentManager,
                "com.dev_vlad.fyredapp.dialog_view_hotspot"
            )
        }
    }

    private fun zoomOnLocationOrRestoreCamPos(
        locationToZoomTo: LatLng? = null,
        isMyLocation: Boolean = false,
        hotSpotIfNotMyLocation: UserMomentWrapper? = null
    ) {
        googleMap?.let { map ->
            if (locationToZoomTo != null) {
                Log.d(
                    LOG_TAG,
                    "from fyredApp | zooming in on lat ${locationToZoomTo.latitude} , long ${locationToZoomTo.longitude}"
                )
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(locationToZoomTo.latitude, locationToZoomTo.longitude),
                        DEFAULT_ZOOM.toFloat()
                    ),
                    object : GoogleMap.CancelableCallback {
                        override fun onFinish() {
                            if (homeViewModel.intentAwaitingPermissions == SHARE_MOMENT) {
                                processShareMomentClickIntent()
                            } else if (isMyLocation && mySpotIsHot) {
                                processViewMyHotSpotIntent()
                            } else if (!isMyLocation && hotSpotIfNotMyLocation != null) {
                                processViewFriendsHotSpotIntent(hotSpotIfNotMyLocation)
                            }

                            homeViewModel.intentAwaitingPermissions = NONE // reset
                        }

                        override fun onCancel() {
                            homeViewModel.intentAwaitingPermissions = NONE // reset
                        }

                    }
                )
            } else if (camPosition != null) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition))
            }
        }
    }


    /***************** USER LOCATION DISPLAY ************/
    @SuppressLint("MissingPermission")
    private fun setMyLocationThenZoomIn() {
        //TODO? might this return an outdated location
        //gps
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
            if (foundMoreAccurateLastKnownLocation(it)) {
                Log.d(
                    LOG_TAG,
                    "from fyredApp | last known location from gps accuracy = ${it.accuracy}"
                )
            }
        }

        //then network provider
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
            if (foundMoreAccurateLastKnownLocation(it)) {
                Log.d(
                    LOG_TAG,
                    "from fyredApp | last known location from network provider accuracy = ${it.accuracy}"
                )
            }

        }


        //setMyPosition marker
        val latLng = homeViewModel.getUserLastKnownAsLatLng()
        if (latLng != null) {
            //setup my marker
            myPositionMarker?.remove()
            myPositionMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
            )
            myPositionMarker!!.setIcon(homeViewModel.myMarkerIconDescriptor)
            zoomOnLocationOrRestoreCamPos(locationToZoomTo = latLng, isMyLocation = true)
        } else {
            //last known location is not yet set
            //clear pending user intents
            homeViewModel.intentAwaitingPermissions = NONE
            Log.d(
                LOG_TAG, "from fyredApp | setAndZoomInOnMyLocation() -> lastKnownLocation is null"
            )
            binding.homeParentLayout.showSnackBarToUser(
                msgResId = R.string.null_last_known_location,
                isErrorMsg = false
            )
        }
    }


    /************* hotspots *******************/
    private fun setupRecyclerView() {
        //setup recycler view
        binding.whatshotRv.layoutManager = LinearLayoutManager(
            requireContext(),
            RecyclerView.HORIZONTAL,
            false
        )
        adapter = HotSpotsAdapter(this)
        binding.whatshotRv.adapter = adapter
    }

    override fun onFriendsHotSpotClicked(
        clickedMoment: UserMomentWrapper
    ) {
        Log.d(
            LOG_TAG,
            "from fyredApp | friend's pin clicked - zoom to location ${clickedMoment.recordedAt.latitude}, ${clickedMoment.recordedAt.longitude}"
        )
        val hotSpotLatLng =
            LatLng(clickedMoment.recordedAt.latitude, clickedMoment.recordedAt.longitude)
        zoomOnLocationOrRestoreCamPos(
            locationToZoomTo = hotSpotLatLng,
            isMyLocation = false,
            hotSpotIfNotMyLocation = clickedMoment
        )
    }


    /************* SHARE MOMENT *********/
    private fun shareMomentAtMyLocation() {
        homeViewModel.intentAwaitingPermissions = SHARE_MOMENT
        //check for location permissions
        if (hasAppPermission(locationPermission)) {
            //location services are required
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //zoom in on my location with intent to share my moment
                setMyLocationThenZoomIn()

            } else {
                // Provider not enabled, prompt user to enable it if they have not just denied us
                if (!returningFromLocationSettings) {
                    binding.homeParentLayout.showSnackBarToUser(
                        msgResId = R.string.please_turn_on_gps,
                        isErrorMsg = false,
                        actionMessage = R.string.ok,
                        actionToTake = {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                            returningFromLocationSettings = true
                        }
                    )
                } else {
                    //reset and do nothing
                    returningFromLocationSettings = false
                    homeViewModel.intentAwaitingPermissions = NONE
                }
            }


        } else {
            requestAppPermissions(
                permissionsArr = arrayOf(locationPermission),
                requestCode = PERMISSION_REQUEST_FINE_LOCATION
            )

        }
    }

    override fun onContinueSharingClicked() {
        Log.i(LOG_TAG, "from fyredApp | continue sharing clicked")
        val action =
            HomeFragmentDirections.actionHomeFragmentToRecordMomentFragment(homeViewModel.userLastKnownLatLng)
        findNavController(this).navigate(action)
    }


    /************ GETTING LOCATION UPDATES ***/
    private fun foundMoreAccurateLastKnownLocation(location: Location): Boolean {
        Log.d(
            LOG_TAG,
            "from fyredApp | foundMoreAccurateLastKnownLocation called with location of accuracy ${location.accuracy}"
        )
        return if (location.accuracy > 0 &&
            location.accuracy < AppConstants.MAX_ACCEPTED_LOCATION_ACCURACY_IN_METERS
        ) {
            homeViewModel.setUserLastKnownLatLng(location)
            true
        } else
            false
    }

    private val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            //refresh location if accuracy is good enough
            Log.d(
                LOG_TAG, "from fyredApp | gpsLocationListener onLocationChanged"
            )
            location?.let {
                if (foundMoreAccurateLastKnownLocation(it)) {
                    stopReceivingGpsUpdates()
                }
            }
        }


        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(LOG_TAG, "from fyredApp | gpsLocationListener status changed $provider")
        }

        override fun onProviderEnabled(provider: String?) {
            Log.d(LOG_TAG, "from fyredApp | gpsLocationListener provider enabled $provider")
        }

        override fun onProviderDisabled(provider: String?) {
            Log.d(LOG_TAG, "from fyredApp | gpsLocationListener provider disabled $provider")
        }

    }

    private val networkProviderListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            //refresh location if accuracy is good enough
            location?.let {
                if (foundMoreAccurateLastKnownLocation(it)) {
                    stopReceivingNetworkProviderUpdates()
                    Log.d(
                        LOG_TAG,
                        "from fyredApp | networkProvider onLocationChanged accuracy = ${it.accuracy}"
                    )
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(LOG_TAG, "from fyredApp | networkProvider status changed $provider")
        }

        override fun onProviderEnabled(provider: String?) {
            Log.d(LOG_TAG, "from fyredApp | networkProvider provider enabled $provider")
        }

        override fun onProviderDisabled(provider: String?) {
            Log.d(LOG_TAG, "from fyredApp | networkProvider provider disabled $provider")
        }

    }

    private fun stopReceivingGpsUpdates() {
        Log.d(LOG_TAG, "from fyredApp | stopGPSReceivingLocationUpdates called")
        locationManager.removeUpdates(gpsLocationListener)
    }

    private fun stopReceivingNetworkProviderUpdates() {
        Log.d(LOG_TAG, "from fyredApp | stopNetworkProviderReceivingLocationUpdates called")
        locationManager.removeUpdates(networkProviderListener)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // gps is enabled request gps updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                millSecsBtnUpdates,
                minMetersBtnUpdates,
                gpsLocationListener
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // gps is enabled request gps updates
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                millSecsBtnUpdates,
                minMetersBtnUpdates,
                networkProviderListener
            )
        }

    }


    /************ SAVING STATE *************/
    private fun restoreCameraPosition() {
        Log.d(LOG_TAG, "from fyredApp | restoring CameraPosition")
        val lat = sharedPref.getString(CAMERA_POSITION_LAT, null)?.toDouble()
        val lng = sharedPref.getString(CAMERA_POSITION_LNG, null)?.toDouble()
        val zoom = sharedPref.getFloat(CAMERA_POSITION_ZOOM, 1F)
        val tilt = sharedPref.getFloat(CAMERA_POSITION_TILT, 1F)
        val bearing = sharedPref.getFloat(CAMERA_POSITION_BEARING, 1F)
        if (lat != null && lng != null) {
            camPosition = CameraPosition(
                LatLng(lat, lng), zoom, tilt, bearing
            )
        }
    }

    private fun saveCameraPosition() {
        Log.d(LOG_TAG, "from fyredApp | saving CameraPosition")
        googleMap?.let { map ->
            camPosition = CameraPosition(
                map.cameraPosition.target,
                map.cameraPosition.zoom,
                map.cameraPosition.tilt,
                map.cameraPosition.bearing
            )

            camPosition?.let {
                val editor = sharedPref.edit()
                editor.putString(CAMERA_POSITION_LAT, it.target.latitude.toString())
                editor.putString(CAMERA_POSITION_LNG, it.target.longitude.toString())
                editor.putFloat(CAMERA_POSITION_ZOOM, it.zoom)
                editor.putFloat(CAMERA_POSITION_TILT, it.tilt)
                editor.putFloat(CAMERA_POSITION_BEARING, it.bearing)
                editor.apply()
            }

        }
    }


    /*** NAVIGATE AWAY **/
    override fun onSeeMoreClicked(userMomentWrapper: UserMomentWrapper) {
        Log.d(LOG_TAG, "from fyredApp | onSeeMoreClicked")
        val action =
            HomeFragmentDirections.actionHomeFragmentToHotSpotFragment(userMomentWrapper)
        findNavController(this).navigate(action)
    }


    /*************** LIFE CYCLE METHODS ************/
    private fun handleIntentsAwaitingForPermissions() {
        if (locationPermissionsJustDenied) {
            locationPermissionsJustDenied = false
            binding.homeParentLayout.showSnackBarToUser(
                msgResId = R.string.location_access_denied,
                isErrorMsg = true
            )
        } else if (homeViewModel.intentAwaitingPermissions == SHARE_MOMENT && returningFromLocationSettings) {
            returningFromLocationSettings = false
            shareMomentAtMyLocation()
        } else if (returningFromLocationSettings) {
            returningFromLocationSettings = false
            //request updates
            requestLocationUpdates()
        }

    }

    override fun onResume() {
        super.onResume()
        if (!UserRepo.userIsLoggedIn()) {
            findNavController().navigate(R.id.action_homeFragment_to_welcomeFragment)
        } else {
            homeViewModel.listenToMoments()
            if (::binding.isInitialized)
                binding.mapView.onResume()
            if (hasAppPermission(locationPermission)) {
                requestLocationUpdates()
            }
            handleIntentsAwaitingForPermissions()
        }
        Log.d(LOG_TAG, "from fyredApp | onResume called")
    }

    override fun onPause() {
        super.onPause()

        if (::binding.isInitialized)
            binding.mapView.onPause()
        stopReceivingGpsUpdates()
        stopReceivingNetworkProviderUpdates()
        saveCameraPosition()
        Log.d(LOG_TAG, "from fyredApp | onPause called")
    }


    override fun onStop() {
        super.onStop()
        if (::binding.isInitialized)
            binding.mapView.onStop()
        Log.d(LOG_TAG, "from fyredApp | onStop called")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::binding.isInitialized)
            binding.mapView.onLowMemory()
        Log.d(LOG_TAG, "from fyredApp | onLowMemory called")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized)
            binding.mapView.onDestroy()
        Log.d(LOG_TAG, "from fyredApp | onDestroy called")

    }

    ////////PERMISSIONS RESULTS
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {

            //assume Location Permission request was just denied
            locationPermissionsJustDenied = true
            for (grantedResult in grantResults) {
                locationPermissionsJustDenied = (grantedResult != PackageManager.PERMISSION_GRANTED)
                if (locationPermissionsJustDenied)
                    break
            }
            //when done
            if (!locationPermissionsJustDenied) {
                Log.d(LOG_TAG, "from fyredApp | location permissions granted")
                if (homeViewModel.intentAwaitingPermissions == ZOOM_ON_MY_LOCATION
                    || homeViewModel.intentAwaitingPermissions == SHARE_MOMENT
                ) {
                    setMyLocationThenZoomIn()
                }

            } else {
                Log.d(LOG_TAG, "from fyredApp | location permissions denied")
                //reset
                homeViewModel.intentAwaitingPermissions = NONE

            }
        }
    }


    /***********MENU ************/
    override fun onPrepareOptionsMenu(menu: Menu) {
        Log.d(LOG_TAG, "from fyredApp | onPrepareOptionsMenu called")
        menu.findItem(R.id.contactsFragment).isVisible = true
        menu.findItem(R.id.userProfileFragment).isVisible = true
        menu.findItem(R.id.submitFeedbackFragment).isVisible = true
        menu.findItem(R.id.aboutAppFragment).isVisible = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "from fyredApp | onOptionsItemSelected called")
        return item.onNavDestinationSelected(findNavController(this)) || super.onOptionsItemSelected(
            item
        )
    }


}