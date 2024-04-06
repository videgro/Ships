package net.videgro.ships.fragments;

import static java.text.DateFormat.getDateTimeInstance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.ar.core.ArCoreApk;
import com.google.gson.Gson;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.Utils;
import net.videgro.ships.adapters.ShipsTableDataAdapter;
import net.videgro.ships.fragments.internal.FragmentUtils;
import net.videgro.ships.fragments.internal.IndicatorAnimation;
import net.videgro.ships.fragments.internal.OpenDeviceResult;
import net.videgro.ships.fragments.internal.ShipsTableManager;
import net.videgro.ships.listeners.ImagePopupListener;
import net.videgro.ships.listeners.OwnLocationReceivedListener;
import net.videgro.ships.listeners.ShipReceivedListener;
import net.videgro.ships.nmea2ship.domain.Ship;
import net.videgro.ships.services.NmeaClientService;
import net.videgro.ships.services.TrackService;
import net.videgro.ships.tools.HttpCacheTileServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.codecrafters.tableview.SortableTableView;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;
import de.codecrafters.tableview.toolkit.TableDataRowBackgroundProviders;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class ShowMapFragment extends Fragment implements OwnLocationReceivedListener, ShipReceivedListener, ImagePopupListener, TextToSpeech.OnInitListener {
    private static final String TAG = "ShowMapFragment";

    private static final DecimalFormat GPS_COORD_FORMAT = new DecimalFormat("##.00");
    private static final String FILE_MAP = "ships_map.jpg";

    private static final int IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR = 1102;
    private static final int IMAGE_POPUP_ID_IGNORE = 1109;
    private static final int IMAGE_POPUP_ID_AIS_RUNNING = 1110;

    private static final int REQ_CODE_START_RTLSDR = 1201;
    private static final int REQ_CODE_STOP_RTLSDR = 1202;
    private static final int REQ_CODE_CHECK_TTS_DATA = 1203;

    /**
     * The value of this placeholder is used literally in the webview link (ship popup) and in the string resources "url_mmsi_info".
     * Respectively indicating that this is a special URL and must be opened in a new browser and as a placeholder to be replaced by the real MMSI value.
     */
    private static final String PLACEHOLDER_MMSI = "PLACEHOLDER_MMSI";

    private static final int[] TABLE_HEADERS = {
        R.string.ships_table_col_country,
        R.string.ships_table_col_type,
        R.string.ships_table_col_mmsi,
        R.string.ships_table_col_name_callsign,
        R.string.ships_table_col_destination,
        R.string.ships_table_col_navigation_status,
        R.string.ships_table_col_updated
    };

    private final DialogRepeatToCloudClickListener dialogRepeatToCloudClickListener=new DialogRepeatToCloudClickListener();

    private WebView webView;
    private ImageView indicatorReceivingInternal;
    private ImageView indicatorReceivingExternal;
    private TextView logTextView;
    private TrackService trackService;
    private NmeaClientService nmeaClientService;
    private ServiceConnection locationServiceConnection;
    private ServiceConnection nmeaClientServiceConnection;
    private Location lastReceivedOwnLocation = null;
    private ToggleButton startStopButton;
    private File fileMap=null;
    private ShipsTableManager shipsTableManager;

    private boolean triedToReceiveFromAntenna=false;

    /* TTS */
    private TextToSpeech tts;
    private final List<Integer> nameSpoken=new ArrayList<>();

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        indicatorReceivingInternal = rootView.findViewById(R.id.indicatorReceivingInternal);
        indicatorReceivingExternal = rootView.findViewById(R.id.indicatorReceivingExternal);

        createShipsTable(rootView.findViewById(R.id.shipsTable));
        logTextView = rootView.findViewById(R.id.textView1);
        final RelativeLayout adView = rootView.findViewById(R.id.adView);

        if (isAdded()) {
            final Activity activity=getActivity();
            if (activity!=null) {
                final File filesDirectory = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                fileMap = new File(filesDirectory, FILE_MAP);
            }
        }

        if (isAdded()) {
            Utils.loadAd(getActivity(),adView,getString(R.string.adUnitId_ShowMapFragment));
        }

        setHasOptionsMenu(true);
        setupWebView(rootView);
        setupNmeaClientService();
        setupTts();

        startStopButton = (ToggleButton) rootView.findViewById(R.id.startStopAisButton);
        startStopButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startReceivingAisFromAntenna();
                } else {
                    stopReceivingAisFromAntenna();
                }
            }
        });

        // TODO: Not possible to stop. Will start automatically when valid PPM exists and hide (stop) button for now
        startStopButton.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start tiles caching server, will also load the OpenStreetMap after server has started
        ShowMapFragmentPermissionsDispatcher.setupHttpCachingTileServerWithPermissionCheck(this);

        ShowMapFragmentPermissionsDispatcher.setupLocationServiceWithPermissionCheck(this);

        startReceivingAisFromAntenna();
    }

    @Override
    public void onPause() {
        final String tag = "onPause";
        Analytics.logEvent(getActivity(),Analytics.CATEGORY_STATISTICS, "HttpCacheTileServer",HttpCacheTileServer.getInstance().getStatistics());
        super.onPause();
    }

    @Override
    public void onDestroy() {
        destroyNmeaClientService();
        destroyLocationService();
        destroyTts();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode);

        switch (requestCode) {
            case REQ_CODE_START_RTLSDR:
                final String startRtlSdrResultAsString = FragmentUtils.parseOpenCloseDeviceActivityResultAsString(data);
                Analytics.logEvent(getActivity(),Analytics.CATEGORY_RTLSDR_DEVICE, OpenDeviceResult.TAG, startRtlSdrResultAsString+" - "+Utils.retrieveAbi());
                logStatus(startRtlSdrResultAsString);

                if (resultCode != Activity.RESULT_OK) {
                    resetGuiToInitialState();
                    Utils.showPopup(IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR, getActivity(), this, getString(R.string.popup_start_device_failed_title), getString(R.string.popup_start_device_failed_message) + " " + startRtlSdrResultAsString, R.drawable.thumbs_down_circle, null);
                } else {
                    FragmentUtils.rtlSdrRunning = true;
                    final String msg = getString(R.string.popup_receiving_ais_message);
                    logStatus(msg);
                    Utils.showPopup(IMAGE_POPUP_ID_AIS_RUNNING, getActivity(), this, getString(R.string.popup_receiving_ais_title), msg, R.drawable.ic_information,null);
                    // On dismiss: Will continue onImagePopupDispose
                }
                break;
            case REQ_CODE_STOP_RTLSDR:
                logStatus(FragmentUtils.parseOpenCloseDeviceActivityResultAsString(data));
                FragmentUtils.rtlSdrRunning = false;
                break;

            case REQ_CODE_CHECK_TTS_DATA:
                if (isAdded()) {
                    if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                        tts = new TextToSpeech(getActivity(), this);
                    } //else {
//                    final Intent installTTSIntent = new Intent();
//                    installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
//                    startActivity(installTTSIntent);
//                    }
                }
                break;

            default:
                Log.e(TAG, "Unexpected request code: " + requestCode);
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    // Must be public to use PermissionsDispatcher
    public void setupHttpCachingTileServer() {
        final String tag = "setupHttpCachingTileServer - ";
        final HttpCacheTileServer httpCacheTileServer = HttpCacheTileServer.getInstance();
        httpCacheTileServer.init(getActivity(), SettingsUtils.getInstance().parseFromPreferencesMapCacheDiskUsageMax());

        if (httpCacheTileServer.startServer()){
            // Server in place, load page

            final String url="file:///android_asset/index.html";
            if (webView.getUrl()==null || webView.getUrl().isEmpty() || !webView.getUrl().equalsIgnoreCase(url)) {
                webView.loadUrl(url);
                // Flow will resume at: onPageFinished
            }
        }
    }

    private void inviteToShareToCloud(){
        // Check is sharing to cloud already
        final boolean repeatToCloud=SettingsUtils.getInstance().parseFromPreferencesRepeatToCloud();
        final boolean repeatFromInternal=SettingsUtils.getInstance().parseFromPreferencesRepeatFromInternal();

        if (!repeatToCloud || !repeatFromInternal) {
            new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.popup_share_to_cloud_message))
                    .setPositiveButton(getString(R.string.popup_share_to_cloud_yes), dialogRepeatToCloudClickListener)
                    .setNegativeButton(getString(R.string.popup_share_to_cloud_no), dialogRepeatToCloudClickListener)
                    .setTitle(getString(R.string.popup_share_to_cloud_title))
                    .show();
        }
    }

    private void showMessageRepeatToCloud(){
        if (isAdded()) {
            Utils.showPopup(IMAGE_POPUP_ID_IGNORE, getActivity(), this, getString(R.string.popup_share_to_cloud_title), getString(R.string.popup_share_to_cloud_message_result), R.drawable.ic_information, Utils.IMAGE_POPUP_AUTOMATIC_DISMISS);
        }
    }

    private void createShipsTable(final Object shipsTableObj){
        if (shipsTableObj instanceof SortableTableView<?>) {
            final SortableTableView<Ship> shipsTable = (SortableTableView<Ship>)shipsTableObj;
            final ShipsTableDataAdapter shipsTableDataAdapter=new ShipsTableDataAdapter(getActivity(), new ArrayList<>());
            shipsTable.setHeaderAdapter(new SimpleTableHeaderAdapter(getActivity(), TABLE_HEADERS));
            shipsTable.setDataAdapter(shipsTableDataAdapter);
            //shipsTable.sort(ShipsTableDataAdapter.COL_UPDATED, SortingOrder.DESCENDING);

            shipsTable.addDataClickListener((int rowIndex, Ship ship) -> {
                if (isAdded()) {
                    String msg = "<h3><a href='"+getString(R.string.url_mmsi_info)+ship.getMmsi()+"'>" + ship.getName() + " " + ship.getMmsi() + "</a></h3>";
                    msg += getString(R.string.ships_table_country) + ship.getCountryName() + "<br />";
                    msg += getString(R.string.ships_table_callsign) + ship.getCallsign() + "<br />";
                    msg += getString(R.string.ships_table_type) + ship.getShipType() + "<br />";
                    msg += getString(R.string.ships_table_destination) + ship.getDest() + "<br />";
                    msg += getString(R.string.ships_table_navigation_status) + ship.getNavStatus() + "<br />";
                    msg += getString(R.string.ships_table_speed) + ship.getSog() + " knots<br />";
                    msg += getString(R.string.ships_table_draught) + ship.getDraught() + " meters/10<br />";
                    msg += getString(R.string.ships_table_heading) + ship.getHeading() + " degrees<br />";
                    msg += getString(R.string.ships_table_course) + new DecimalFormat("#.#").format(ship.getCog() / 10) + " degrees<br />";
                    msg += "<h3>"+getString(R.string.ships_table_head_position)+"</h3>";
                    msg += " - "+getString(R.string.ships_table_latitude) + new DecimalFormat("#.###").format(ship.getLat()) + "<br />";
                    msg += " - "+getString(R.string.ships_table_longitude) + new DecimalFormat("#.###").format(ship.getLon()) + "<br />";
                    msg += "<h3>"+getString(R.string.ships_table_head_dimensions)+"</h3>";
                    msg += " - "+getString(R.string.ships_table_dim_bow) + ship.getDimBow() + " meters<br />";
                    msg += " - "+getString(R.string.ships_table_dim_port) + ship.getDimPort() + " meters<br />";
                    msg += " - "+getString(R.string.ships_table_dim_starboard) + ship.getDimStarboard() + " meters<br />";
                    msg += " - "+getString(R.string.ships_table_dim_stern) + ship.getDimStern() + " meters<br /><br />";
                    msg += getString(R.string.ships_table_updated) + getDateTimeInstance().format(ship.getTimestamp()) + " (age: " + (Calendar.getInstance().getTimeInMillis() - ship.getTimestamp()) + " ms)<br />";
                    msg += getString(R.string.ships_table_source) + ship.getSource();

                    Utils.showPopup(IMAGE_POPUP_ID_IGNORE, getActivity(),this,getString(R.string.popup_ship_info_title), msg, R.drawable.ic_information, null);
                    // On dismiss: Will continue onImagePopupDispose
                }
            });

            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_COUNTRY, (ship1, ship2) -> ship1.getCountryName().compareTo(ship2.getCountryName()));
            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_TYPE, (ship1, ship2) -> ship1.getShipType().compareTo(ship2.getShipType()));
            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_MMSI, (ship1, ship2) -> ship1.getMmsi() - (ship2.getMmsi()));
            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_NAME_CALLSIGN, (ship1, ship2) -> (ship1.getName()+ship1.getCallsign()).compareTo(ship2.getName()+ship2.getCallsign()));
            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_DESTINATION, (ship1, ship2) -> ship1.getDest().compareTo(ship2.getDest()));
            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_NAV_STATUS, (ship1, ship2) -> ship1.getNavStatus().compareTo(ship2.getNavStatus()));
            shipsTable.setColumnComparator(ShipsTableDataAdapter.COL_UPDATED, (ship1, ship2) -> (int) (ship1.getTimestamp() - ship2.getTimestamp()));

            final int colorEvenRows = ContextCompat.getColor(getActivity(), R.color.ships_table_row_even);
            final int colorOddRows = ContextCompat.getColor(getActivity(), R.color.ships_table_row_odd);
            shipsTable.setDataRowBackgroundProvider(TableDataRowBackgroundProviders.alternatingRowColors(colorEvenRows, colorOddRows));

            shipsTableManager=new ShipsTableManager(shipsTableDataAdapter,SettingsUtils.getInstance().parseFromPreferencesMaxAge());
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(final View rootView) {
        webView = (WebView) rootView.findViewById(R.id.webview);

        // Enable JavaScript on webview
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webView.addJavascriptInterface(new JavaScriptInterface(), "android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:init()");
                webView.loadUrl("javascript:setZoomToExtent(" + Boolean.toString(SettingsUtils.getInstance().parseFromPreferencesMapZoomToExtent()) + ")");
                webView.loadUrl("javascript:setPrefetchLowerZoomLevelsTiles(" + Boolean.toString(SettingsUtils.getInstance().parseFromPreferencesMapCacheLowerZoomlevels()) + ")");
                webView.loadUrl("javascript:setDisableSound(" + Boolean.toString(SettingsUtils.getInstance().parseFromPreferencesMapDisableSound()) + ")");
                webView.loadUrl("javascript:setShipScaleFactor("+SettingsUtils.getInstance().parseFromPreferencesShipScaleFactor()+")");
                webView.loadUrl("javascript:setMaxAge("+SettingsUtils.getInstance().parseFromPreferencesMaxAge()+")");
                webView.loadUrl("javascript:setOwnLocationIcon('"+SettingsUtils.getInstance().parseFromPreferencesOwnLocationIcon()+"')");
                if (lastReceivedOwnLocation != null) {
                    webView.loadUrl("javascript:setCurrentPosition(" + lastReceivedOwnLocation.getLongitude() + "," + lastReceivedOwnLocation.getLatitude() + ")");
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                boolean result = false;
                if (url != null && url.contains(PLACEHOLDER_MMSI)) {
                    final String mmsi = url.split(PLACEHOLDER_MMSI)[1];
                    final String newUrl = getString(R.string.url_mmsi_info).replace(PLACEHOLDER_MMSI, mmsi);
                    Analytics.logEvent(getActivity(),TAG, "shipinfo", mmsi);
                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(newUrl)));
                    result = true;
                }
                return result;
            }
        });
    }

    private class JavaScriptInterface {
        @JavascriptInterface
        public void showLayerVisibilityChanged(String layerName,boolean visibility) {
            // Use as case labels literally the layer names specified in assets/index.html
            switch (layerName){
                case "Ships":
                    shipsTableManager.updateEnabledSource(Ship.Source.INTERNAL,visibility);
                break;
                case "Ships - Peers":
                    shipsTableManager.updateEnabledSource(Ship.Source.EXTERNAL,visibility);
                    shipsTableManager.updateEnabledSource(Ship.Source.CLOUD,visibility);
                break;
                default:
                    // Nothing to do
                    break;
            } // END SWITCH
        }

        @JavascriptInterface
        public void log(String message) {
            Log.i(TAG,"FromJS: "+message);
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    // Must be public to use PermissionsDispatcher
    public void setupLocationService() {
        final String tag="setupLocationService - ";
        if (isAdded()) {
            final Activity activity=getActivity();
            if (activity!=null){
                locationServiceConnection = new LocationServiceConnection((OwnLocationReceivedListener) this);
                final Intent serviceIntent = new Intent(activity,TrackService.class);
                activity.startForegroundService(serviceIntent);
                activity.bindService(new Intent(activity,TrackService.class), locationServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                Log.e(TAG,tag+"Activity is null.");
            }
        }
    }

    private void setupTts() {
        if (isAdded()) {
            final Intent intent = new Intent();
            try {
                intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(intent, REQ_CODE_CHECK_TTS_DATA);
            } catch (ActivityNotFoundException e) {
                Analytics.logEvent(getActivity(), Analytics.CATEGORY_ERRORS, "TTS", "ActNotFound");
            }
        }
    }

    private void setupNmeaClientService() {
        final String tag="setupNmeaClientService - ";
        if (isAdded()) {
            final Activity activity=getActivity();
            if (activity!=null) {
                nmeaClientServiceConnection = new NmeaClientServiceConnection((ShipReceivedListener) this);
                final Intent serviceIntent = new Intent(activity, NmeaClientService.class);
                activity.startForegroundService(serviceIntent);
                activity.bindService(new Intent(activity, NmeaClientService.class), nmeaClientServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                Log.e(TAG,tag+"Activity is null.");
            }
        }
    }

    private void destroyLocationService() {
        if (trackService != null) {
            trackService.setListener(null);
        }

        final Activity activity=getActivity();
        if (locationServiceConnection != null && activity!=null) {
            activity.unbindService(locationServiceConnection);
            locationServiceConnection = null;
        }
    }

    private void destroyNmeaClientService() {
        if (nmeaClientService != null) {
            nmeaClientService.removeListener(this);
        }

        final Activity activity=getActivity();
        if (nmeaClientServiceConnection != null && activity!=null) {
            activity.unbindService(nmeaClientServiceConnection);
            nmeaClientServiceConnection = null;
        }
    }

    private void destroyTts(){
        if (tts!=null){
            tts.shutdown();
            tts=null;
        }
    }

    private void startReceivingAisFromAntenna() {
        final String tag = "startReceivingAisFromAntenna - ";

        if (!triedToReceiveFromAntenna && !FragmentUtils.rtlSdrRunning) {
            final int ppm = SettingsUtils.getInstance().parseFromPreferencesRtlSdrPpm();
            if (SettingsUtils.isValidPpm(ppm)) {
                triedToReceiveFromAntenna=true;
                final boolean startResult = FragmentUtils.startReceivingAisFromAntenna(this, REQ_CODE_START_RTLSDR, ppm);
                logStatus((startResult ? "Requested" : "Failed") + " to receive AIS from antenna (PPM: " + ppm + ").");

                // On positive result: Will continue at onActivityResult (REQ_CODE_START_RTLSDR)
            } else {
                Log.e(TAG, tag + "Invalid PPM: " + ppm);
            }
        } else {
            final String msg = getString(R.string.popup_receiving_ais_message);
            logStatus(msg);
            Utils.showPopup(IMAGE_POPUP_ID_AIS_RUNNING, getActivity(), this, getString(R.string.popup_receiving_ais_title), msg, R.drawable.ic_information,null);
            // On dismiss: Will continue onImagePopupDispose
        }
    }

    private void stopReceivingAisFromAntenna() {
        FragmentUtils.stopReceivingAisFromAntenna(this, REQ_CODE_STOP_RTLSDR);
    }

    private void resetGuiToInitialState() {
        startStopButton.setChecked(false);
    }

    private void logStatus(final String status) {
        Utils.logStatus(getActivity(), logTextView, status);
    }

    private Intent getShareIntent() {
        final Intent shareIntent = new Intent();

        if (fileMap!=null) {
            final ArrayList<Uri> uris = new ArrayList<>();
            uris.add(FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + getString(R.string.dot_provider), fileMap));

            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "\n\n" + getString(R.string.share_text) + " " + getString(R.string.app_name) + " - " + getString(R.string.app_url));
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.setType("*/*");
        }

        return shareIntent;
    }

    private boolean checkCameraAvailable() {
        boolean result = false;
        final Activity activity = getActivity();
        if (activity != null) {
            final PackageManager pm = activity.getPackageManager();
            result = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        }

        return result;
    }

    private boolean gpsAvailable() {
        boolean result = false;
        final Activity activity = getActivity();
        if (activity != null) {
            final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            result = locationManager != null && locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER);
        }
        return result;
    }

    private void maybeEnableArButton(final MenuItem menuItem) {
        boolean enable = false;

        if (isAdded()) {
            try {
                final ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(getActivity());

                if (availability.isTransient()) {
                    // Re-query at 5Hz while compatibility is checked in the background.
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            maybeEnableArButton(menuItem);
                        }
                    }, 200);
                }
                if (availability.isSupported()) {
                    if (checkCameraAvailable()) {
                        if (gpsAvailable()) {
                            Analytics.logEvent(getActivity(), Analytics.CATEGORY_AR, TAG, "OK");
                            enable = true;
                        } else {
                            Analytics.logEvent(getActivity(), Analytics.CATEGORY_AR_ERRORS, TAG, "NoGPS");
                        }
                    } else {
                        Analytics.logEvent(getActivity(), Analytics.CATEGORY_AR_ERRORS, TAG, "NoCamera");
                    }
                } else {
                    Analytics.logEvent(getActivity(), Analytics.CATEGORY_AR_ERRORS, TAG, "NotSupportedArCore");
                }
            } catch (Exception e) {
                // Just log all exceptions. Not sure what can happen in ArCoreApk
                Analytics.logEvent(getActivity(), Analytics.CATEGORY_AR_ERRORS, TAG, e.getMessage());
            }
        }

        menuItem.setVisible(enable);
        menuItem.setEnabled(enable);
    }

    private void takeScreenShotWithCheck() {
        ShowMapFragmentPermissionsDispatcher.takeScreenShotWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    // Must be public to use PermissionsDispatcher
	public void takeScreenShot() {
        final String tag="takeScreenShot - ";
		Log.i(TAG,tag);

		if (fileMap!=null) {
            final Picture picture = webView.capturePicture();
            final int width = picture.getWidth();
            final int height = picture.getHeight();

            if (width > 0 && height > 0) {
                final Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                final Canvas c = new Canvas(b);
                picture.draw(c);

                FileOutputStream fosScreenshot = null;
                try {
                    fosScreenshot = new FileOutputStream(fileMap);
                    b.compress(Bitmap.CompressFormat.JPEG, 100, fosScreenshot);
                    fosScreenshot.close();
                    Log.i(TAG, tag + "Screenshot available at: " + fileMap.toString());
                } catch (IOException e) {
                    Log.e(TAG, tag, e);
                }
            } else {
                Analytics.logEvent(getActivity(), TAG, tag, "Width (" + width + ") or height (" + height + ") of image <= 0.");
            }
        }
	}

    /**
     * See also: https://developer.android.com/guide/topics/ui/menus.html#checkable
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = true;
        final int id = item.getItemId();
        if (id == R.id.menu_share) {
            Analytics.logEvent(getActivity(),TAG, "share","");

            updateShareData();

            // Android Sharesheet
            // https://developer.android.com/training/sharing/send
            startActivity(Intent.createChooser(getShareIntent(), getString(R.string.share_action_text)));
        }
        return result;
    }

    private void updateShareData() {
        final String tag = "updateShareData - ";
        takeScreenShotWithCheck();
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.setGroupVisible(R.id.main_menu_group_show_map, true);

        // Enable AR related functionality on ARCore supported devices only.
        maybeEnableArButton(menu.findItem(R.id.action_ar));
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ShowMapFragmentPermissionsDispatcher.onRequestPermissionsResult(this,requestCode,grantResults);
    }

    /************************** LISTENER IMPLEMENTATIONS ******************/

    private class DialogRepeatToCloudClickListener implements DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final String analyticsAction="RepeatToCloud";
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    // 'Yes'-button clicked

                    // Update settings
                    SettingsUtils.getInstance().setToPreferencesRepeatFromInternal(true);
                    SettingsUtils.getInstance().setToPreferencesRepeatToCloud(true);

                    // Re-init Repeater
                    if (nmeaClientService!=null) {
                        nmeaClientService.init();
                    }

                    Analytics.logEvent(getActivity(),Analytics.CATEGORY_STATISTICS, analyticsAction,"Yes");

                    // Show message to user
                    showMessageRepeatToCloud();

                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    // 'No'-button clicked
                    Analytics.logEvent(getActivity(),Analytics.CATEGORY_STATISTICS, analyticsAction,"No");
                    break;
            }
        }
    }

	/**** START ImagePopupListener ****/

	@Override
	public void onImagePopupDispose(int id) {
		switch (id) {
		case IMAGE_POPUP_ID_OPEN_RTLSDR_ERROR:
		    // Ignore this error. User can still receive Ships from peers
		break;
		case IMAGE_POPUP_ID_AIS_RUNNING:
            // AIS is running, invite to share data to cloud
            inviteToShareToCloud();
		break;
		default:
			Log.d(TAG,"onImagePopupDispose - id: "+id);
		}
	}

	/**** END ImagePopupListener ****/


	/**** START NmeaReceivedListener ****/
	@Override
	public void onShipReceived(final Ship ship) {
		final String tag="onShipReceived - ";

        final String shipIdent="MMSI: "+ ship.getMmsi() + (ship.getName() != null  && !ship.getName().isEmpty() ? " "+ship.getName() : "")+" Country: "+ship.getCountryName();
        logStatus("Ship location received ("+shipIdent+")"+(SettingsUtils.getInstance().parseFromPreferencesLoggingVerbose() ? "\n"+ship.toString().replace(",","\n   ") : ""));

        if (tts!=null && ship.getName() != null && !ship.getName().isEmpty() && isAdded() && !SettingsUtils.getInstance().parseFromPreferencesMapDisableSound() && !nameSpoken.contains(ship.getMmsi()) && isAdded()){
            nameSpoken.add(ship.getMmsi());
            final String msg=ship.getName().toLowerCase()+" "+ship.getCountryName();
            tts.speak(msg,TextToSpeech.QUEUE_ADD, null);
        }

        if (isAdded() && getActivity()!=null){
            getActivity().runOnUiThread(() -> {
                shipsTableManager.update(ship);

                // Show indicator (animation)
                final ImageView indicator=(Ship.Source.EXTERNAL.equals(ship.getSource()) || Ship.Source.CLOUD.equals(ship.getSource())) ? indicatorReceivingExternal : indicatorReceivingInternal;
                indicator.setVisibility(View.VISIBLE);
                indicator.startAnimation(new IndicatorAnimation(false));

                webView.loadUrl("javascript:onShipReceived('" + new Gson().toJson(ship) + "')");
            });

            // Slow down a bit. Give the map time to draw the ship and tiles
            SystemClock.sleep(250);
        } else {
            Log.e(TAG,tag+"Huh?");
        }
	}

	/**** END NmeaListener ****/

	/**** START OwnLocationReceivedListener ****/

	@Override
	public void onOwnLocationReceived(final Location location) {
		logStatus("Own location received: Lon: " + GPS_COORD_FORMAT.format(location.getLongitude()) + ", Lat: " + GPS_COORD_FORMAT.format(location.getLatitude()));
		lastReceivedOwnLocation=location;

		if (getActivity()!=null){
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
                webView.loadUrl("javascript:setCurrentPosition(" + location.getLongitude() + "," + location.getLatitude() + ")");
				}
			});
		} else {
			Log.e(TAG,"Huh?");
		}
	}

	/**** END OwnLocationReceivedListener ****/

    /**** START TextToSpeech.OnInitListener ****/

    @Override
    public void onInit(int initStatus) {
        //check for successful instantiation
        if (initStatus == TextToSpeech.SUCCESS) {
            Analytics.logEvent(getActivity(),Analytics.CATEGORY_STATISTICS, "TTS","OK");
            if (webView!=null) {
                webView.loadUrl("javascript:setDisableSound(true)");
            }
//            if (tts.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE) {
//                tts.setLanguage(Locale.US);
//            }
        } else if (initStatus == TextToSpeech.ERROR) {
            Analytics.logEvent(getActivity(),Analytics.CATEGORY_STATISTICS, "TTS","FAILED");
            // Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }

    /**** END TextToSpeech.OnInitListener ****/

	/************************** PRIVATE CLASS IMPLEMENTATIONS ******************/

	private class LocationServiceConnection implements ServiceConnection {
		private final OwnLocationReceivedListener listener;

		LocationServiceConnection(OwnLocationReceivedListener listener) {
			this.listener = listener;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			if (service instanceof TrackService.ServiceBinder) {
				trackService = ((TrackService.ServiceBinder) service).getService();
				trackService.setListener(listener);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			trackService = null;
		}
	}

	private class NmeaClientServiceConnection implements ServiceConnection {
		private final String tag="NmeaClientServiceConnection - ";
		private final ShipReceivedListener listener;

		NmeaClientServiceConnection(ShipReceivedListener listener) {
			this.listener = listener;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			if (service instanceof NmeaClientService.ServiceBinder) {
				Log.d(TAG,tag+"onServiceConnected");
				nmeaClientService = ((NmeaClientService.ServiceBinder) service).getService();
				nmeaClientService.addListener(listener);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			nmeaClientService = null;
		}
	}

    /************************** PermissionsDispatcher IMPLEMENTATIONS ******************/

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.permission_externalstorage_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForExternalStorage() {
        if (isAdded()) {
            Utils.showPopup(IMAGE_POPUP_ID_IGNORE, getActivity(), this, getString(R.string.permission_externalstorage_denied_title), getString(R.string.permission_externalstorage_denied), R.drawable.thumbs_down_circle, null);
        }
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForExternalStorage() {
        if (isAdded()) {
            Utils.showPopup(IMAGE_POPUP_ID_IGNORE, getActivity(), this, getString(R.string.permission_externalstorage_denied_title), getString(R.string.permission_externalstorage_neverask), R.drawable.thumbs_down_circle, null);
        }
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showRationaleForLocation(final PermissionRequest request) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.permission_location_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void showDeniedForLocation() {
        if (isAdded()) {
            Utils.showPopup(IMAGE_POPUP_ID_IGNORE, getActivity(), this, getString(R.string.permission_location_denied_title), getString(R.string.permission_location_denied), R.drawable.thumbs_down_circle, null);
        }
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void showNeverAskForLocation() {
        if (isAdded()) {
            Utils.showPopup(IMAGE_POPUP_ID_IGNORE, getActivity(), this, getString(R.string.permission_location_denied_title), getString(R.string.permission_location_neverask), R.drawable.thumbs_down_circle, null);
        }
    }
}
