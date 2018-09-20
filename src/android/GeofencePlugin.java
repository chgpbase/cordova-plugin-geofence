package com.cowbell.cordova.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "GeofencePlugin";
    
    public static final String ERROR_UNKNOWN = "UNKNOWN";
    public static final String ERROR_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String ERROR_GEOFENCE_NOT_AVAILABLE = "GEOFENCE_NOT_AVAILABLE";
    public static final String ERROR_GEOFENCE_LIMIT_EXCEEDED = "GEOFENCE_LIMIT_EXCEEDED";
    
    private GeoNotificationManager geoNotificationManager;
    private Context context;
    protected static Boolean isInBackground = true;
    public static CordovaWebView webView = null;
    
   private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }
    
    private Action executedAction;
    
    private GeoNotificationBroadcastReceiver mReceiver = null;

    private class GeoNotificationBroadcastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(true);
            results.putBoolean("HANDLED", true);
            Log.d(TAG, "GeoNotificationBroadcastReceiver - Received Broadcast intent");
        }
    }

    /**
     * @param cordova
     *            The context of the main Activity.
     * @param webView
     *            The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView = webView;
        context = this.cordova.getActivity();
        Logger.setLogger(new Logger(TAG, context, false));
        geoNotificationManager = new GeoNotificationManager(context);
    }

    @Override
    protected void pluginInitialize() {
        Log.d(TAG, "GeoNotificationBroadcastReceiver - pluginInitialize");
        super.pluginInitialize();
        mReceiver = new GeoNotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter(ReceiveTransitionsIntentService.GeofenceTransitionIntent);
        Context context = this.cordova.getActivity();
        if(context != null)
        {
            context.registerReceiver(mReceiver, filter);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "GeoNotificationBroadcastReceiver - onResume");
        IntentFilter filter = new IntentFilter(ReceiveTransitionsIntentService.GeofenceTransitionIntent);
        if(context != null && mReceiver != null) {
            Context context = this.cordova.getActivity();
            context.registerReceiver(mReceiver, filter);
        }
        super.onResume(multitasking);
    }

    @Override
    public void onPause(boolean multitasking) {
        try {
            Context context = this.cordova.getActivity();
            context.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause(multitasking);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "GeoNotificationBroadcastReceiver - onDestroy");
        try {
            Context context = this.cordova.getActivity();
            context.unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "GeofencePlugin execute action: " + action + " args: "
                + args.toString());
        
        executedAction = new Action(action, args, callbackContext);
        cordova.getThreadPool().execute(new Runnable() {
	    public void run() {
        if (action.equals("addOrUpdate")) {
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
            for (int i = 0; i < args.length(); i++) {
                GeoNotification not = parseFromJSONObject(args.getJSONObject(i));
                if (not != null) {
                    geoNotifications.add(not);
                }
            }
            geoNotificationManager.addGeoNotifications(geoNotifications,
                    callbackContext);
        } else if (action.equals("remove")) {
            List<String> ids = new ArrayList<String>();
            for (int i = 0; i < args.length(); i++) {
                ids.add(args.getString(i));
            }
            geoNotificationManager.removeGeoNotifications(ids, callbackContext);
        } else if (action.equals("removeAll")) {
            geoNotificationManager.removeAllGeoNotifications(callbackContext);
        } else if (action.equals("getWatched")) {
            List<GeoNotification> geoNotifications = geoNotificationManager
                    .getWatched();
            callbackContext.success(Gson.get().toJson(geoNotifications));
        } else if (action.equals("initialize")) {
            initialize(callbackContext);
        } else if (action.equals("deviceReady")) {
            deviceReady();
            callbackContext.success();
        } else {
            return false;
        }
           }
        });
        return true;
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = null;
        geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    public static void onTransitionReceived(List<GeoNotification> notifications) {
        Log.d(TAG, "Transition Event Received!");
        String js = "setTimeout('geofence.onTransitionReceived("
                + Gson.get().toJson(notifications) + ")',0)";
        if (webView == null) {
            Log.d(TAG, "Webview is null");
        } else {
            webView.sendJavascript(js);
        }
    }

    private void deviceReady() {
        Intent intent = cordova.getActivity().getIntent();
        Intent launcherIntent = null;
        if(intent != null && intent.hasExtra("LAUNCHER_INTENT")){
            launcherIntent = intent.getParcelableExtra("LAUNCHER_INTENT");

        } else {
            launcherIntent = intent;
        }

        if(launcherIntent != null){

            String data = launcherIntent.getStringExtra("geofence.notification.data");
            String js = "setTimeout('geofence.onTransitionReceived(["
                    + data + "])',0)";

            if (data == null) {
                Log.d(TAG, "No notifications clicked.");
            } else {
                webView.sendJavascript(js);
                launcherIntent.removeExtra("geofence.notification.data");
            }
        }
    }

    private void initialize(CallbackContext callbackContext) {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(permissions)) {
            PermissionHelper.requestPermissions(this, 0, permissions);
        } else {
            callbackContext.success();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!PermissionHelper.hasPermission(this, permission)) return false;
        }

        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;

        if (executedAction != null) {
            for (int r:grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    executedAction.callbackContext.sendPluginResult(result);
                    executedAction = null;
                    return;
                }
            }
            Log.d(TAG, "Permission Granted!");
            execute(executedAction);
            executedAction = null;
        }
    }
}
