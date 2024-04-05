package net.videgro.ships.ar.utils

import android.app.Activity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import net.videgro.ships.R
import java.util.*

object AugmentedRealityLocationUtils {

    private const val RENDER_MARKER_MIN_DISTANCE = 2//meters
    private const val RENDER_MARKER_MAX_DISTANCE = 7000//meters
    const val INVALID_MARKER_SCALE_MODIFIER = -1F
    const val INITIAL_MARKER_SCALE_MODIFIER = 0.5f

    fun checkAvailability(activity: Activity): String {
        var result: String = "AR not available"
        val availability = ArCoreApk.getInstance().checkAvailability(activity)
        if (availability != null){
            when (availability) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> result = ""
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> try {
                    val installStatus = ArCoreApk.getInstance().requestInstall(activity, true)
                    if (ArCoreApk.InstallStatus.INSTALLED == installStatus) {
                        result = "";
                    }
                } catch (e: UnavailableDeviceNotCompatibleException) {
                    result = "Device not compatible - " + e.message
                } catch (e: UnavailableUserDeclinedInstallationException) {
                    result = "User declined installation - " + e.message
                }

                ArCoreApk.Availability.UNKNOWN_ERROR -> result = "Unknown error"
                ArCoreApk.Availability.UNKNOWN_CHECKING -> result = "Unknown checking"
                ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> result = "Unknown time out"
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> result =
                    "Device not capable"
            }
        }
        return result
    }

    @Throws(UnavailableException::class)
    fun setupSession(activity: Activity): Session? {
        var session: Session? = null
        try {
            session = Session(activity, EnumSet.of(Session.Feature.SHARED_CAMERA))
        } catch (e: FatalException) {
            /*
             * Will wrap the FatalException into a UnavailableException.
             *
             * Also throwing:
             * - UnavailableArcoreNotInstalledException
             * - UnavailableApkTooOldException
             * - UnavailableSdkTooOldException
             * - UnavailableDeviceNotCompatibleException
             */
            throw UnavailableException("FatalException - "+e.message);
        }

        val config = Config(session)
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        // IMPORTANT!!!  ArSceneView requires the `LATEST_CAMERA_IMAGE` non-blocking update mode.

        session.configure(config)

        return session
    }

    fun handleSessionException(activity: Activity, sessionException: UnavailableException): String {
        val message = when (sessionException) {
            is UnavailableArcoreNotInstalledException ->
                activity.resources.getString(R.string.popup_ar_error_arcore_not_installed)

            is UnavailableUserDeclinedInstallationException ->
                activity.resources.getString(R.string.popup_ar_error_arcore_not_installed)

            is UnavailableApkTooOldException ->
                activity.resources.getString(R.string.popup_ar_error_arcore_not_updated)

            is UnavailableSdkTooOldException ->
                activity.resources.getString(R.string.popup_ar_error_arcore_not_supported)

            else -> activity.resources.getString(R.string.popup_ar_error_arcore_not_supported)
        }

        return message
    }

    fun getScaleModifierBasedOnRealDistance(distance: Int): Float {
        return when (distance) {
            in Integer.MIN_VALUE..RENDER_MARKER_MIN_DISTANCE -> INVALID_MARKER_SCALE_MODIFIER
            in RENDER_MARKER_MIN_DISTANCE + 1..20 -> 0.8f
            in 21..40 -> 0.75f
            in 41..60 -> 0.7f
            in 61..80 -> 0.65f
            in 81..100 -> 0.6f
            in 101..1000 -> 0.5f
            in 1001..1500 -> 0.45f
            in 1501..2000 -> 0.4f
            in 2001..2500 -> 0.35f
            in 2501..3000 -> 0.3f
            in 3001..RENDER_MARKER_MAX_DISTANCE -> 0.25f
            in RENDER_MARKER_MAX_DISTANCE + 1..Integer.MAX_VALUE -> 0.15f
            else -> -1f
        }
    }

    fun generateRandomHeightBasedOnDistance(distance: Int): Float {
        return when (distance) {
            in 0..1000 -> (1..3).random().toFloat()
            in 1001..1500 -> (4..6).random().toFloat()
            in 1501..2000 -> (7..9).random().toFloat()
            in 2001..3000 -> (10..12).random().toFloat()
            in 3001..RENDER_MARKER_MAX_DISTANCE -> (12..13).random().toFloat()
            else -> 0f
        }
    }

    fun showDistance(distance: Int): String {
        return if (distance >= 1000)
            String.format("%.2f", (distance.toDouble() / 1000)) + " km"
        else
            "$distance m"
    }
}
