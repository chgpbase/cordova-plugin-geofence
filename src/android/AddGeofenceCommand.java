package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationStatusCodes;

import java.util.List;

public class AddGeofenceCommand extends AbstractGoogleServiceCommand{
    private List<Geofence> geofencesToAdd;
    private PendingIntent pendingIntent;

    public AddGeofenceCommand(Context context, PendingIntent pendingIntent,
            List<Geofence> geofencesToAdd) {
        super(context);
        this.geofencesToAdd = geofencesToAdd;
        this.pendingIntent = pendingIntent;
    }

    @Override
    public void onAddGeofencesResult(int statusCode, String[] arg1) {
        // If adding the geofences was successful
        boolean success = false;
        if (LocationStatusCodes.SUCCESS == statusCode) {
            logger.log(Log.DEBUG, "Geofences successfully added");
            success = true;
            /*
             * Handle successful addition of geofences here. You can send out a
             * broadcast intent or update the UI. geofences into the Intent's
             * extended data.
             */
        } else {
            logger.log(Log.DEBUG, "Adding geofences failed");
            success = false;
            // If adding the geofences failed
            /*
             * Report errors here. You can log the error using Log.e() or update
             * the UI.
             */
        }
        CommandExecuted(success);
    }

    @Override
    public void ExecuteCustomCode() {
        // TODO Auto-generated method stub
        logger.log(Log.DEBUG, "Adding new geofences");
        try {
            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .addGeofences(geofencesToAdd)
                    .build();
            LocationServices.GeofencingApi
                    .addGeofences(mGoogleApiClient, geofencingRequest, pendingIntent)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {

                            if (status.isSuccess()) {
                                logger.log(Log.DEBUG, "Geofences successfully added");
                                /*
                                 * Handle successful addition of geofences here. You can send out a
                                 * broadcast intent or update the UI. geofences into the Intent's
                                 * extended data.
                                 */
                            } else {
                                logger.log(Log.DEBUG, "Adding geofences failed");
                                // If adding the geofences failed
                                /*
                                 * Report errors here. You can log the error using Log.e() or update
                                 * the UI.
                                 */
                            }
                            CommandExecuted(status.isSuccess());
                        }
                    });
        } catch (Exception exception) {
            logger.log(LOG.ERROR, "Exception while adding geofences");
            exception.printStackTrace();
            CommandExecuted(false);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

}
