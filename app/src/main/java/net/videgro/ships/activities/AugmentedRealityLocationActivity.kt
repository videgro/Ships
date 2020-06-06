package net.videgro.ships.activities

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.android.synthetic.main.activity_augmented_reality_location.*
import kotlinx.android.synthetic.main.augmented_reality_location.view.*
import net.videgro.ships.Analytics
import net.videgro.ships.R
import net.videgro.ships.SettingsUtils
import net.videgro.ships.Utils
import net.videgro.ships.ar.utils.AugmentedRealityLocationUtils
import net.videgro.ships.ar.utils.AugmentedRealityLocationUtils.INITIAL_MARKER_SCALE_MODIFIER
import net.videgro.ships.ar.utils.AugmentedRealityLocationUtils.INVALID_MARKER_SCALE_MODIFIER
import net.videgro.ships.ar.utils.PermissionUtils
import net.videgro.ships.fragments.internal.FragmentUtils
import net.videgro.ships.fragments.internal.OpenDeviceResult
import net.videgro.ships.listeners.ImagePopupListener
import net.videgro.ships.listeners.ShipReceivedListener
import net.videgro.ships.nmea2ship.domain.Ship
import net.videgro.ships.services.NmeaClientService
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.CompletableFuture

@RequiresApi(Build.VERSION_CODES.N)
class AugmentedRealityLocationActivity : AppCompatActivity(), ShipReceivedListener, ImagePopupListener {
    private val TAG = "ARLocationActivity"

    private val IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR = 1102
    private val IMAGE_POPUP_ID_IGNORE = 1109
    private val IMAGE_POPUP_ID_AIS_RUNNING = 1110
    private val IMAGE_POPUP_ID_START_AR_ERROR = 1111
    private val IMAGE_POPUP_ID_REQUEST_PERMISSIONS = 1112
    private val IMAGE_POPUP_ID_ACCURACY_MSG_SHOWN = 1113

    private val REQ_CODE_START_RTLSDR = 1201
    private val REQ_CODE_STOP_RTLSDR = 1202

    // Our ARCore-Location scene
    private var locationScene: LocationScene? = null

    private var arHandler = Handler(Looper.getMainLooper())
    lateinit var loadingDialog: AlertDialog

    private val resumeArElementsTask = Runnable {
        locationScene?.resume()
        arSceneView.resume()
    }

    private var shipsMap: HashMap<Int, Ship> = hashMapOf()
    private var markers: HashMap<Int, LocationMarker> = hashMapOf();
    private var areAllMarkersLoaded = false

    private var triedToReceiveFromAntenna = false
    private var nmeaClientService: NmeaClientService? = null
    private var nmeaClientServiceConnection: ServiceConnection? = null

    /* Only render markers when ship is within this distance (meters) */
    private var maxDistance=0

    /* Remove ship from list of ships to render when last update is older than this value (in milliseconds) */
    private var maxAge=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val error=AugmentedRealityLocationUtils.checkAvailability(this);
        if (error.isEmpty()) {
            setContentView(R.layout.activity_augmented_reality_location)
            setupLoadingDialog()
            setupNmeaClientService()
        } else {
            // Not possible to start AR
            Log.e(
                TAG,
                error
            )

            Analytics.logEvent(
                this,
                Analytics.CATEGORY_AR_ERRORS,
                TAG,
                error
            )

            finish();
        }
    }

    override fun onResume() {
        super.onResume()
        shipsMap.clear()
        maxDistance = SettingsUtils.getInstance().parseFromPreferencesArMaxDistance()
        maxAge = SettingsUtils.getInstance().parseFromPreferencesArMaxAge()*1000*60
        checkAndRequestPermissions()
    }

    override fun onStart() {
        super.onStart()
        informationAccuracyMessage()
    }

    override fun onPause() {
        super.onPause()

        // Count number of rendered markers at THIS moment and log it
        var numMarkers=0
        markers.values.forEach{marker -> if (marker.anchorNode!=null) numMarkers++ }
        val msg="Rendered markers on pause: "+numMarkers+" ("+shipsMap.size+")"
        Log.i(
            TAG,
            msg
        )

        Analytics.logEvent(
            this,
            Analytics.CATEGORY_AR,
            TAG,
            msg
        )

        arSceneView.session?.let {
            locationScene?.pause()
            arSceneView?.pause()
        }
    }

    override fun onDestroy() {
        destroyNmeaClientService()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i(
            TAG,
            "onActivityResult requestCode: $requestCode, resultCode: $resultCode"
        )
        when (requestCode) {
            REQ_CODE_START_RTLSDR -> {
                val startRtlSdrResultAsString = FragmentUtils.parseOpenCloseDeviceActivityResultAsString(data)
                Analytics.logEvent(
                    this,
                    Analytics.CATEGORY_RTLSDR_DEVICE,
                    OpenDeviceResult.TAG,startRtlSdrResultAsString + " - " + Utils.retrieveAbi()
                )
                logStatus(startRtlSdrResultAsString)
                if (resultCode != Activity.RESULT_OK) {
                    Utils.showPopup(
                        IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR,
                        this,
                        this,
                        getString(R.string.popup_start_device_failed_title),
                        getString(R.string.popup_start_device_failed_message) + " " + startRtlSdrResultAsString,
                        R.drawable.thumbs_down_circle,
                        null
                    )
                } else {
                    FragmentUtils.rtlSdrRunning = true
                    val msg = getString(R.string.popup_receiving_ais_message)
                    logStatus(msg)
                    Utils.showPopup(
                        IMAGE_POPUP_ID_AIS_RUNNING,
                        this,
                        this,
                        getString(R.string.popup_receiving_ais_title),
                        msg,
                        R.drawable.ic_information,
                        null
                    )
                    // On dismiss: Will continue onImagePopupDispose
                }
            }
            REQ_CODE_STOP_RTLSDR -> {
                logStatus(FragmentUtils.parseOpenCloseDeviceActivityResultAsString(data))
                FragmentUtils.rtlSdrRunning = false
            }
            else -> Log.e(
                TAG,
                "Unexpected request code: $requestCode"
            )
        }
    }

    private fun informationAccuracyMessage(){
        Utils.showPopup(
            IMAGE_POPUP_ID_ACCURACY_MSG_SHOWN,
            this,
            this,
            getString(R.string.popup_ar_accuracy_title),
            getString(R.string.popup_ar_accuracy_message),
            R.drawable.ic_information,
            30000
        )
        // On dismiss: Will continue onImagePopupDispose
    }

    private fun startReceivingAisFromAntenna() {
        val tag = "startReceivingAisFromAntenna - "
        if (!triedToReceiveFromAntenna && !FragmentUtils.rtlSdrRunning) {
            val ppm = SettingsUtils.getInstance().parseFromPreferencesRtlSdrPpm()
            if (SettingsUtils.isValidPpm(ppm)) {
                triedToReceiveFromAntenna = true
                val startResult =   FragmentUtils.startReceivingAisFromAntenna(this, REQ_CODE_START_RTLSDR, ppm)
                logStatus((if (startResult) "Requested" else "Failed") + " to receive AIS from antenna (PPM: " + ppm + ").")

                // On positive result: Will continue at onActivityResult (REQ_CODE_START_RTLSDR)
            } else {
                Log.e(TAG, tag + "Invalid PPM: " + ppm)
            }
        } else {
            val msg = getString(R.string.popup_receiving_ais_message)
            logStatus(msg)
            Utils.showPopup(
                IMAGE_POPUP_ID_AIS_RUNNING,
                this,
                this,
                getString(R.string.popup_receiving_ais_title),
                msg,
                R.drawable.ic_information,
                null
            )
            // On dismiss: Will continue onImagePopupDispose
        }
    }

    private fun logStatus(status: String) {
        //Utils.logStatus(getActivity(), logTextView, status)
    }

    private fun setupNmeaClientService() {
        val tag = "setupNmeaClientService - "

        nmeaClientServiceConnection = this.NmeaClientServiceConnection(this as ShipReceivedListener?)
        val serviceIntent = Intent(this, NmeaClientService::class.java)

        // On Android 8+ let service run in foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        this.bindService(
            Intent(this, NmeaClientService::class.java),
            nmeaClientServiceConnection!!,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun destroyNmeaClientService() {
        if (nmeaClientService != null) {
            nmeaClientService!!.removeListener(this)
        }
        val nmeaClientServiceConnectionLocal = nmeaClientServiceConnection
        if (nmeaClientServiceConnectionLocal != null) {
            this.unbindService(nmeaClientServiceConnectionLocal)
            nmeaClientServiceConnection = null
        }
    }

    private fun setupLoadingDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        val dialogHintMainView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null) as LinearLayout
        alertDialogBuilder.setView(dialogHintMainView)
        loadingDialog = alertDialogBuilder.create()
        loadingDialog.setCanceledOnTouchOutside(false)
    }

    private fun setupSession():String{
        var error:String="";
        try {
            val session = AugmentedRealityLocationUtils.setupSession(this)
            if (session != null) {
                arSceneView.setupSession(session)
            }
        } catch (e: UnavailableException) {
            error = AugmentedRealityLocationUtils.handleSessionException(this, e)
        }
        return error
    }

    private fun setupLocationScene(){
        locationScene = LocationScene(this, arSceneView)
        locationScene!!.setMinimalRefreshing(true)
        locationScene!!.setOffsetOverlapping(true)
//            locationScene!!.setRemoveOverlapping(true)
        locationScene!!.anchorRefreshInterval = 2000
    }

    private fun createSession() {
        var error:String="";

        if (arSceneView != null) {
            if (arSceneView.session == null) {
                error=setupSession()
            }

            if (error.isEmpty()) {
                if (locationScene == null) {
                    setupLocationScene()
                }

                try {
                    resumeArElementsTask.run()
                } catch (e: CameraNotAvailableException) {
                    error=getString(R.string.popup_camera_open_error_message);
                }
            }
        } else {
            error=getString(R.string.popup_ar_error_arsceneview_not_set);
        }

        if (!error.isEmpty()){
            Analytics.logEvent(
                this,
                Analytics.CATEGORY_AR_ERRORS,
                TAG,
                error
            )
            Utils.showPopup(
                IMAGE_POPUP_ID_START_AR_ERROR,
                this,
                this,
                getString(R.string.popup_ar_error_title),
                error,
                R.drawable.thumbs_down_circle, null
            )
            // On dismiss: Will continue onImagePopupDispose
        }
    }

    private fun render() {
        setupAndRenderMarkers()
        updateMarkers()
    }

    private fun setupAndRenderMarkers() {
        shipsMap.forEach { key, ship ->
            val completableFutureViewRenderable = ViewRenderable.builder()
                .setView(this, R.layout.augmented_reality_location)
                .build()
            CompletableFuture.anyOf(completableFutureViewRenderable)
                .handle<Any> { _, throwable ->
                    //here we know the renderable was built or not
                    if (throwable != null) {
                        // handle renderable load fail
                        return@handle null
                    }
                    try {
                        val oldMarker = markers.get(ship.mmsi);

                        val marker = LocationMarker(
                            ship.lon,
                            ship.lat,
                            setNode(ship, completableFutureViewRenderable)
                        )
                        marker.setOnlyRenderWhenWithin(maxDistance)

                        markers.put(ship.mmsi, marker);
                        arHandler.postDelayed({

                            // Remember old height, to prevent jumping markers
                            marker.height=if (oldMarker != null) oldMarker.height else 0f;

                            // First add marker, before eventually removing the old one to prevent blinking markers
                            attachMarkerToScene(
                                marker,
                                completableFutureViewRenderable.get().view
                            )

                            if (oldMarker != null) {
                                removeMarkerFromScene(oldMarker,completableFutureViewRenderable.get().view)
                            }

                            if (shipsMap.values.indexOf(ship) == shipsMap.size - 1) {
                                areAllMarkersLoaded = true
                            }
                        }, 200)

                    } catch (e: Exception) {
                        Log.e(TAG, e.toString());
                    }
                    null
                }
        }
    }

    private fun updateMarkers() {
        arSceneView.scene.addOnUpdateListener()
        {
            if (!areAllMarkersLoaded) {
                return@addOnUpdateListener
            }

            locationScene?.mLocationMarkers?.forEach { locationMarker ->
                if (locationMarker.height==0f) {
                    // There is no elevation information of vessels available, just generate a random height based on distance
                    locationMarker.height =
                        AugmentedRealityLocationUtils.generateRandomHeightBasedOnDistance(
                            locationMarker?.anchorNode?.distance ?: 0
                        )
                }
            }

            val frame = arSceneView!!.arFrame ?: return@addOnUpdateListener
            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@addOnUpdateListener
            }
            locationScene!!.processFrame(frame)
        }
    }

    private fun removeMarkerFromScene(
        locationMarker: LocationMarker,
        layoutRendarable: View
    ) {
        resumeArElementsTask.run {
            locationMarker.anchorNode?.isEnabled = false
            locationScene?.mLocationMarkers?.remove(locationMarker)

            arHandler.post {
                locationScene?.refreshAnchors()
                layoutRendarable.pinContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun attachMarkerToScene(
        locationMarker: LocationMarker,
        layoutRendarable: View
    ) {
        resumeArElementsTask.run {
            locationMarker.scalingMode = LocationMarker.ScalingMode.FIXED_SIZE_ON_SCREEN
            locationMarker.scaleModifier = INITIAL_MARKER_SCALE_MODIFIER

            locationScene?.mLocationMarkers?.add(locationMarker)
            locationMarker.anchorNode?.isEnabled = true

            arHandler.post {
                locationScene?.refreshAnchors()
                layoutRendarable.pinContainer.visibility = View.VISIBLE
            }
        }
        locationMarker.setRenderEvent { locationNode ->
            layoutRendarable.distance.text = AugmentedRealityLocationUtils.showDistance(locationNode.distance)
            resumeArElementsTask.run {
                computeNewScaleModifierBasedOnDistance(locationMarker, locationNode.distance)
            }
        }
    }

    private fun computeNewScaleModifierBasedOnDistance(
        locationMarker: LocationMarker,
        distance: Int
    ) {
        val scaleModifier = AugmentedRealityLocationUtils.getScaleModifierBasedOnRealDistance(distance)
        return if (scaleModifier == INVALID_MARKER_SCALE_MODIFIER) {
            detachMarker(locationMarker)
        } else {
            locationMarker.scaleModifier = scaleModifier
        }
    }

    private fun detachMarker(locationMarker: LocationMarker) {
        locationMarker.anchorNode?.anchor?.detach()
        locationMarker.anchorNode?.isEnabled = false
        locationMarker.anchorNode = null
    }

    private fun setNode(
        ship: Ship,
        completableFuture: CompletableFuture<ViewRenderable>
    ): Node {
        val node = Node()
        node.renderable = completableFuture.get()

        val nodeLayout = completableFuture.get().view
        val name = nodeLayout.name
        val markerLayoutContainer = nodeLayout.pinContainer
        name.text = ship.name + " (" + ship.mmsi + ")"
        markerLayoutContainer.visibility = View.GONE
        nodeLayout.setOnTouchListener { _, _ ->
            val title = ship.name + " (" + ship.mmsi + ")";

            var msg = getString(R.string.ships_table_country) + ship.countryName + "<br />"
            msg += getString(R.string.ships_table_callsign) + ship.callsign + "<br />"
            msg += getString(R.string.ships_table_type) + ship.shipType + "<br />"
            msg += getString(R.string.ships_table_destination) + ship.dest + "<br />"
            msg += getString(R.string.ships_table_navigation_status) + ship.navStatus + "<br />"
            msg += getString(R.string.ships_table_speed) + ship.sog + " knots<br />"
            msg += getString(R.string.ships_table_draught) + ship.draught + " meters/10<br />"
            msg += getString(R.string.ships_table_heading) + ship.heading + " degrees<br />"
            msg += getString(R.string.ships_table_course) + DecimalFormat("#.#").format(ship.cog / 10.toLong()) + " degrees<br />"
            msg += "<h3>" + getString(R.string.ships_table_head_position) + "</h3>"
            msg += " - " + getString(R.string.ships_table_latitude) + DecimalFormat("#.###").format(ship.lat) + "<br />"
            msg += " - " + getString(R.string.ships_table_longitude) + DecimalFormat("#.###").format(ship.lon) + "<br />"
            msg += "<h3>" + getString(R.string.ships_table_head_dimensions) + "</h3>"
            msg += " - " + getString(R.string.ships_table_dim_bow) + ship.dimBow + " meters<br />"
            msg += " - " + getString(R.string.ships_table_dim_port) + ship.dimPort + " meters<br />"
            msg += " - " + getString(R.string.ships_table_dim_starboard) + ship.dimStarboard + " meters<br />"
            msg += " - " + getString(R.string.ships_table_dim_stern) + ship.dimStern + " meters<br /><br />"
            msg += getString(R.string.ships_table_updated) + DateFormat.getDateTimeInstance().format(ship.timestamp) + " (age: " + (Calendar.getInstance().timeInMillis - ship.timestamp) + " ms)<br />"
            msg += getString(R.string.ships_table_source) + ship.source;

            Utils.showPopup(
                IMAGE_POPUP_ID_IGNORE,
                this,
                this,
                title,
                msg,
                R.drawable.ic_information,
                null
            )
            // On dismiss: Will continue onImagePopupDispose

            false
        }

        Glide.with(this)
            .load("file:///android_asset/images/flags/" + ship.countryFlag + ".png")
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(nodeLayout.arMarkerCountry)

        return node
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionUtils.hasLocationAndCameraPermissions(this)) {
            PermissionUtils.requestCameraAndLocationPermissions(this)
        } else {
            createSession()
        }
    }

    /**
     * Remove old ships from map
     */
    private fun cleanUpShipsMap() {
        val now=Calendar.getInstance().timeInMillis;
        val cleanedShipsMap: HashMap<Int, Ship> = hashMapOf()
        shipsMap.forEach { key, ship ->
            if ((now - ship.timestamp) < maxAge) {
                cleanedShipsMap.put(ship.mmsi,ship)
            }
        }
        shipsMap=cleanedShipsMap;
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        if (!PermissionUtils.hasLocationAndCameraPermissions(this)) {

            Utils.showPopup(
                IMAGE_POPUP_ID_REQUEST_PERMISSIONS,
                this,
                this,
                getString(R.string.popup_camera_and_location_permission_request_title),
                getString(R.string.popup_camera_and_location_permission_request_message),
                R.drawable.thumbs_down_circle,
                null
            )
            // On dismiss: Will continue onImagePopupDispose
        }
    }

    /**** START ImagePopupListener  */
    override fun onImagePopupDispose(id: Int) {
        when (id) {
            IMAGE_POPUP_ID_ACCURACY_MSG_SHOWN -> {
                startReceivingAisFromAntenna()
            }
            IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR -> {
                // Ignore this error. User can still receive Ships from peers
            }
            IMAGE_POPUP_ID_AIS_RUNNING -> {
                // AIS is running, invite to share data to cloud
            }
            IMAGE_POPUP_ID_START_AR_ERROR -> {
                // Not possible to start AR, exit activity
                finish();
            }
            IMAGE_POPUP_ID_REQUEST_PERMISSIONS -> {
                if (!PermissionUtils.shouldShowRequestPermissionRationale(this)) {
                    // Permission denied with checking "Do not ask again".
                    PermissionUtils.launchPermissionSettings(this)
                }
                finish()
            }

            else -> Log.d(TAG, "onImagePopupDispose - id: $id")
        }
    }
    /**** END ImagePopupListener ****/

    /**** START NmeaReceivedListener ****/
    override fun onShipReceived(ship: Ship?) {
        if (ship != null) {
            shipsMap.put(ship.mmsi, ship)
        }

        cleanUpShipsMap()

        runOnUiThread(Runnable {
            arNumberOfShipsInView.setText(getString(R.string.ar_number_ships,shipsMap.size))
            areAllMarkersLoaded = false
            //     locationScene!!.clearMarkers()
            render()
        })
    }
    /**** END NmeaListener ****/

    inner class NmeaClientServiceConnection internal constructor(private val listener: ShipReceivedListener?) :
        ServiceConnection {
        private val tag = "NmeaCltServiceConn - "
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            if (service is NmeaClientService.ServiceBinder) {
                Log.d(tag, "onServiceConnected")
                var localNmeaClientService = nmeaClientService;
                localNmeaClientService = service.service
                localNmeaClientService.addListener(listener)
                nmeaClientService = localNmeaClientService
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            nmeaClientService = null
        }
    }
}