package com.example.locationpoc;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.places.Places;

/**
 * Created by akhilhanda on 02/05/16.
 */
public class LocationService implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static String TAG = "LocationService";
    private GoogleApiClient mGoogleApiClient;
    private LocationSettingsRequest.Builder mBuilder;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 101;

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private Context mActivity;

    public LocationService(Context activity) {
        mActivity = activity;
    }

    private LocationListener mLocationListener;
    private boolean mIsForceToEnableGPSSetting;

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                if (mActivity != null && mActivity instanceof Activity && !((Activity) mActivity).isFinishing()) {
                    connectionResult.startResolutionForResult((Activity) mActivity, REQUEST_RESOLVE_ERROR);
                }
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /**
     * connect to GoogleApiClient
     */
    public void connect(LocationListener locationListener, boolean isForceToEnableGPSSetting) {
        mLocationListener = locationListener;
        mIsForceToEnableGPSSetting = isForceToEnableGPSSetting;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addOnConnectionFailedListener(this).addConnectionCallbacks(this).build();
        }
        if (!mResolvingError && !mGoogleApiClient.isConnected()) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    /**
     * @return LocationSettingsResult object.
     * Add ResultCallback to know the current location setting of the device
     */
    public PendingResult<LocationSettingsResult> getLocationSettingsResult() {

        LocationRequest highLocationRequest = LocationRequest.create();
        highLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(highLocationRequest).setAlwaysShow(true);

        return LocationServices.SettingsApi
                .checkLocationSettings(mGoogleApiClient, mBuilder.build());
    }


    /**
     * disconnect  GoogleApiClient
     */
    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        if (mActivity != null && mActivity instanceof Activity && !((Activity) mActivity).isFinishing()) {
            GooglePlayServicesUtil.getErrorDialog(errorCode,
                    (Activity) mActivity, REQUEST_RESOLVE_ERROR).show();
        }

    }

    public void checkLocationSettings(ResultCallback<LocationSettingsResult> resultResultCallback) {

        PendingResult<LocationSettingsResult> result = getLocationSettingsResult();
        result.setResultCallback(resultResultCallback);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "suspended");
    }

    public void connect() {
        connect(null, false);
    }

}