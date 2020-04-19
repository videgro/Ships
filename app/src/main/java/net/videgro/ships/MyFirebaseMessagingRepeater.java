package net.videgro.ships;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.videgro.ships.services.MyFirebaseMessagingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class MyFirebaseMessagingRepeater {
    private static final String TAG = "FBMessagingRepeater";

    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";

    public static final String JSON_DATA_FIELD="nmeas";
    public static final String PREFIX_AIVDM="!AIVDM,";

    // Every 30 seconds send/clear buffer
    private static final int SEND_BUFFER_INTERVAL=1000*30;
    public static final String NMEA_SEP="|";

    private final Context context;
    private final CopyOnWriteArrayList<String> buffer = new CopyOnWriteArrayList<>();
    private final RequestQueue requestQueue;

    private Timer timer;

    MyFirebaseMessagingRepeater(final Context context){
        this.context=context;
        requestQueue = Volley.newRequestQueue(context);
    }

    public void start(){
        startTimer();
    }

    public void stop(){
       if (timer!=null) {
           timer.cancel();
       }
    }

    private void startTimer(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!buffer.isEmpty()){
                    sendBuffer(buffer);
                    buffer.clear();
                }
            }
        }, SEND_BUFFER_INTERVAL, SEND_BUFFER_INTERVAL);
    }

    public void broadcast(final String nmea){
        buffer.add(nmea.substring(PREFIX_AIVDM.length()));
    }

    private void sendBuffer(final CopyOnWriteArrayList<String> nmeas){
        final StringBuilder payload=new StringBuilder();
        for (final String nmea:nmeas){
            payload.append(nmea).append(NMEA_SEP);
        }

        /*
         * {
         *   "to": "/topics/nmea",
         *   "data": {
         *    "nmeas": "<nmea>|<nmea>|<nmea>|",
         *   }
         *  }
         *
         * Curl test:
         * curl -X POST --header "Authorization: key=<server key>"     --Header "Content-Type: application/json"     https://fcm.googleapis.com/fcm/send     -d "{\"to\":\"/topics/nmea\",\"data\":{\"nmeas\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}}"
         */
        final JSONObject notification = new JSONObject();
        final JSONObject notifcationBody = new JSONObject();
        final JSONObject fcmOptions = new JSONObject();

        try {
            notifcationBody.put(JSON_DATA_FIELD, payload);
            fcmOptions.put("analyticsLabel","NMEA_LABEL");
            notification.put("to", "/topics/"+MyFirebaseMessagingService.MESSAGING_TOPIC);
            notification.put("data", notifcationBody);
            notification.put("fcm_options", fcmOptions);
        } catch (JSONException e) {
            Log.e("TAG",e.getMessage()!=null ? e.getMessage() : "NULL");
        }

        sendNotification(notification);
    }

    private void sendNotification(final JSONObject message) {
        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, FCM_API, message, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error!=null && error.getMessage()!=null ? error.getMessage() : "NULL");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                final Map<String, String> params = new HashMap<>();
                params.put("Authorization", "key="+context.getString(R.string.firebaseMessagingKey));
                params.put("Content-Type", "application/json");

                return params;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);
    }
}
