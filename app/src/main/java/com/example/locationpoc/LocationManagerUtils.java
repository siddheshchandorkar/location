package com.example.locationpoc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

import pl.tajchert.nammu.Nammu;


/**
 * Created by Prashant on 10/02/17.
 */

public class LocationManagerUtils {
    public static final int REQUEST_CHECK_SETTINGS = 111;
    public static final int REQUEST_CHECK_GPS_SETTINGS = 99;
    public static String locationFineAccessPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    public static String locationCoarseAccessPermission = Manifest.permission.ACCESS_COARSE_LOCATION;
    public String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    public LocationService mLocationService;
    public boolean isForceToEnableGPSSetting;
    public int requestCode = -1;
    @RequiresApi(Build.VERSION_CODES.Q)
    public String locationBackgroundAccessPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
    @TargetApi(28)
    public String[] locationPermissionListAPI28 = new String[]{
            locationFineAccessPermission,
            locationCoarseAccessPermission};
    @TargetApi(29)
    public String[] locationPermissionListAPI29 = new String[]{
            locationFineAccessPermission,
            locationCoarseAccessPermission,
            locationBackgroundAccessPermission};
    @TargetApi(30)
    public String[] locationPermissionListAPI30 = new String[]{
            locationBackgroundAccessPermission};
    public LocationListener mLocationListener;
    public ActivityResultLauncher<String[]> locationPermissionRequest;
    public Boolean fineLocationGranted = false;
    public Boolean coarseLocationGranted = false;
    ActivityResultLauncher<IntentSenderRequest> locationLauncher;
    ActivityResultLauncher<IntentSenderRequest> mLocationStartForResult;
    private AppCompatActivity mBaseActivity;
    private String mMsgOnRefusedPermission;
    private boolean isForceToGrantToLocationPermission;
    private boolean isOldLogic = true;
    private LocationSettingListener locationSettingListener;
    private CurrentLocationSettingListener currentLocationSettingListener;

    public static boolean isGPSEnabled(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public LocationSettingListener getLocationSettingListener() {
        return locationSettingListener;
    }

    public void setLocationSettingListener(LocationSettingListener locationSettingListener) {
        this.locationSettingListener = locationSettingListener;
    }

    /***
     * method  use to init location manager class (this class)
     *
     * @param baseActivity
     * @param locationListener for location callback perpose
     * @param isForceToEnableGPSSetting boolean to force to enable GPSSetting navigatedFrom  setting
     * @param msgOnRefusedPermission    msg if location permission refused(for marshmallow and above device only)
     */
    public void initLocationManager(AppCompatActivity baseActivity,
                                    LocationListener locationListener,
                                    boolean isForceToEnableGPSSetting,
                                    String msgOnRefusedPermission, CurrentLocationSettingListener currentLocationSettingListener, boolean oldLogic) {
        this.mBaseActivity = baseActivity;
        this.mLocationListener = locationListener;
        this.isForceToEnableGPSSetting = isForceToEnableGPSSetting;
        this.mMsgOnRefusedPermission = msgOnRefusedPermission;
        this.currentLocationSettingListener = currentLocationSettingListener;
        mLocationService = new LocationService(baseActivity);
        mLocationService.connect(locationListener, isForceToEnableGPSSetting);
        isOldLogic = oldLogic;
        getLauncherResult();
        checkLocationPermission();
    }

    public void initLocationManager(AppCompatActivity baseActivity,
                                    LocationListener locationListener,
                                    boolean isForceToEnableGPSSetting,
                                    String msgOnRefusedPermission,
                                    ActivityResultLauncher<IntentSenderRequest> mLocationStartForResult) {
        this.mBaseActivity = baseActivity;
        this.mLocationListener = locationListener;
        this.isForceToEnableGPSSetting = isForceToEnableGPSSetting;
        this.mMsgOnRefusedPermission = msgOnRefusedPermission;
        this.mLocationStartForResult = mLocationStartForResult;
        mLocationService = new LocationService(baseActivity);
        mLocationService.connect(locationListener, isForceToEnableGPSSetting);
        checkLocationPermission();
    }

    private void getLauncherResult() {

        if (!isOldLogic) {
            locationPermissionRequest =
                    mBaseActivity.registerForActivityResult(new ActivityResultContracts
                                    .RequestMultiplePermissions(), result -> {
                                fineLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                coarseLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_COARSE_LOCATION, false);
                                if (fineLocationGranted != null && fineLocationGranted) {
                                    // Precise location access granted.
                                    Log.d("ManagerUtils", "registerForActivityResult Precise location access granted.");
                                    checkLocationSettings();
                                    if (currentLocationSettingListener != null) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                currentLocationSettingListener.onSatisfied();
                                            }
                                        }, 500);
                                    }
                                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                    // Only approximate location access granted.
                                    Log.d("ManagerUtils", "registerForActivityResult Only approximate location access granted");
                                    checkLocationPermission();

                                } else {
                                    // No location access granted.
                                    Log.d("ManagerUtils", "registerForActivityResult No location access granted.");
                                    checkLocationPermission();
                                }
                            }
                    );
            locationLauncher = mBaseActivity.registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {

                            if (requestCode == LocationManagerUtils.REQUEST_CHECK_GPS_SETTINGS) {
                                checkLocationPermission();
                            }
                        }
                    });
        } else checkLocationSettings();

    }

    /**
     * Utility - Only to establish GPS client connection pre-checking that all
     * the location permission granted
     *
     * @param baseActivity
     * @param locationListener
     * @param isForceToEnableGPSSetting
     */
    public void initLocationManager(AppCompatActivity baseActivity,
                                    LocationListener locationListener,
                                    boolean isForceToEnableGPSSetting) {
        this.mBaseActivity = baseActivity;
        this.mLocationListener = locationListener;
        this.isForceToEnableGPSSetting = isForceToEnableGPSSetting;
        mLocationService = new LocationService(baseActivity);
        mLocationService.connect(locationListener, isForceToEnableGPSSetting);
    }

    public void initLocationManager(Context appContext,
                                    LocationListener locationListener,
                                    boolean isForceToEnableGPSSetting) {
        this.mLocationListener = locationListener;
        this.isForceToEnableGPSSetting = isForceToEnableGPSSetting;
        mLocationService = new LocationService(appContext);
        mLocationService.connect(locationListener, isForceToEnableGPSSetting);
    }

    public boolean isForceToGrantToLocationPermission() {
        return isForceToGrantToLocationPermission;
    }

    public void setForceToGrantToLocationPermission(boolean forceToGrantToLocationPermission) {
        isForceToGrantToLocationPermission = forceToGrantToLocationPermission;
    }


    /**
     * method use to check Location permission ( for marshmallow and above devices)
     */
    public void checkLocationPermission() {
        if (!Nammu.checkPermission(locationPermission) || !Nammu.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {

            locationPermissionRequest.launch(locationPermissionListAPI29);
//            Nammu.askForPermission(mBaseActivity, new String[]{
//                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
//            }, new PermissionCallback() {
//                @Override
//                public void permissionGranted() {
//
//
//                    checkLocationSettings();
//                }
//
//                @Override
//                public void permissionRefused() {
//                    if (mMsgOnRefusedPermission != null && !TextUtils.isEmpty(mMsgOnRefusedPermission)) {
//                       Toast.makeText(mBaseActivity, mMsgOnRefusedPermission, Toast.LENGTH_LONG).show();
//                        if (isForceToGrantToLocationPermission) {
//                            checkLocationPermission();
//                        }
//                    }
//                }
//            });
        } else {
            checkLocationSettings();
        }
    }


    public void checkOldLocationSettings() {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mBaseActivity);
        if (mLocationService != null && status == ConnectionResult.SUCCESS) {
            mLocationService.checkLocationSettings(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                    final Status status = locationSettingsResult.getStatus();

                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can
                            // initialize location
                            // requests here.
                            enableSetup();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfi
                            //
                            // ed. But could be
                            // fixed by showing the user
                            // a dialog.
                            if (isForceToEnableGPSSetting) {
                                try {
                                    status.startResolutionForResult(mBaseActivity,
                                            REQUEST_CHECK_GPS_SETTINGS);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                }

                                try {
                                    if (mLocationStartForResult != null) {
                                        requestCode = REQUEST_CHECK_GPS_SETTINGS;
                                        if (status.hasResolution()) {
                                            IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(Objects.requireNonNull(status.getResolution())).build();
                                            mLocationStartForResult.launch(intentSenderRequest);
                                        } else {
                                            status.startResolutionForResult(mBaseActivity,
                                                    REQUEST_CHECK_GPS_SETTINGS);
                                        }
                                    } else if (locationLauncher == null) {
                                        status.startResolutionForResult(mBaseActivity,
                                                REQUEST_CHECK_GPS_SETTINGS);
                                    } else {
                                        requestCode = REQUEST_CHECK_GPS_SETTINGS;
                                        if (status.hasResolution()) {
                                            IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(Objects.requireNonNull(status.getResolution())).build();
                                            locationLauncher.launch(intentSenderRequest);
                                        } else {
                                            status.startResolutionForResult(mBaseActivity,
                                                    REQUEST_CHECK_GPS_SETTINGS);
                                        }
                                    }
                                } catch (IntentSender.SendIntentException sendEx) {
                                    // Ignore the error.
                                }
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. When
                            // user has clicked never to see popup again
                            break;
                    }
                }
            });
        } else {
            GoogleApiAvailability.getInstance().showErrorDialogFragment(mBaseActivity, status, REQUEST_CHECK_GPS_SETTINGS);
        }
    }


    public void checkLocationSettings() {
        if (isOldLogic) {
            checkOldLocationSettings();
        } else {
            setupLocationSetting(mBaseActivity);
        }
    }


    public void setupLocationSetting(final Activity baseActivity) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(LocationHelper.INSTANCE.createLocationRequest());
        SettingsClient client = LocationServices.getSettingsClient(baseActivity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(baseActivity, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            LocationHelper.INSTANCE.setupLocationListener(mBaseActivity, mLocationListener);
        });

        task.addOnFailureListener(baseActivity, exception -> {
            if (exception instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.

                if (isForceToEnableGPSSetting) {
                    try {
                        // Cast to a resolvable exception.
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        resolvable.startResolutionForResult(mBaseActivity, REQUEST_CHECK_GPS_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }

                    try {
                        // Cast to a resolvable exception.
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        Status status = resolvable.getStatus();
                        if (mLocationStartForResult != null) {
                            requestCode = REQUEST_CHECK_GPS_SETTINGS;
                            if (status.hasResolution()) {
                                IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(Objects.requireNonNull(status.getResolution())).build();
                                mLocationStartForResult.launch(intentSenderRequest);
                            } else {
                                status.startResolutionForResult(mBaseActivity,
                                        REQUEST_CHECK_GPS_SETTINGS);
                            }
                        } else if (locationLauncher == null) {
                            status.startResolutionForResult(mBaseActivity,
                                    REQUEST_CHECK_GPS_SETTINGS);
                        } else {
                            requestCode = REQUEST_CHECK_GPS_SETTINGS;
                            if (status.hasResolution()) {
                                IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(Objects.requireNonNull(status.getResolution())).build();
                                locationLauncher.launch(intentSenderRequest);
                            } else {
                                status.startResolutionForResult(mBaseActivity,
                                        REQUEST_CHECK_GPS_SETTINGS);
                            }
                        }
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }

            } else if (exception instanceof ApiException) {
                final LocationManager manager = (LocationManager) mBaseActivity.getSystemService(Context.LOCATION_SERVICE);

                if (manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationHelper.INSTANCE.buildAlertMessageNoGps(mBaseActivity);
                } else {
                    LocationHelper.INSTANCE.setupLocationListener(mBaseActivity, mLocationListener);
                }
            } else {
                Log.d(LocationHelper.LOCATION_SERVICE, exception.getMessage());
            }
        });
    }


    public void enableSetup() {
        setUpLocation();
    }


    private void setUpLocation() {
        if (Nammu.checkPermission(locationPermission) && mLocationService.getGoogleApiClient().isConnected())
            if (locationSettingListener != null) {
                locationSettingListener.onSatisfied();
            } else if (mLocationListener != null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mLocationService.getGoogleApiClient(), createLocationRequest(), mLocationListener);

            }

    }

    public LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5 * 1000);
        return mLocationRequest;
    }

    public boolean isForceToEnableGPSSetting() {
        return isForceToEnableGPSSetting;
    }

    public void checkLocation() {
        if (Nammu.checkPermission(locationPermission)
                && mLocationService.getGoogleApiClient().isConnected()) {
            if (isGPSEnabled(mBaseActivity)) {
//                if (((UserListActivity) mBaseActivity).getLatestLocation() == null || ((UserListActivity) mBaseActivity).getLatestLocation().getLatitude() == 0.0 && ((UserListActivity) mBaseActivity).getLatestLocation().getLongitude() == 0.0) {
//                    LocationServices.FusedLocationApi.requestLocationUpdates(
//                            mLocationService.getGoogleApiClient(), createLocationRequest(), mLocationListener);
//                    AppUtils.showToast(mBaseActivity, mBaseActivity.getString(R.string.fetching_location), false);
//                } else {
                checkLocationSettings();
//                }

            } else {
                checkLocationPermission();
            }
        } else {
            //TODO Siddhes confirm this
//            initLocationManager(mBaseActivity, mLocationListener, true, mBaseActivity.getString(R.string.err_grant_permission));
        }
    }

    public void requestLocationUpdate() {
        if (Nammu.checkPermission(locationPermission) && Nammu.checkPermission(locationCoarseAccessPermission)
                && mLocationService.getGoogleApiClient().isConnected()
                && isGpsActive(mBaseActivity)) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mLocationService.getGoogleApiClient(), createLocationRequest(), mLocationListener);
        }
    }

    private boolean isGpsActive(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public interface LocationSettingListener {
        void onSatisfied();
    }

    public interface CurrentLocationSettingListener {
        void onSatisfied();
    }

}
