package com.example.locationpoc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener

object LocationHelper {
    const val LOCATION_SERVICE = "LocationService"
    private const val FASTEST_INTERVAL = 5000L
    private const val INTERVAL = 10000L
    private const val LOCATION_SEARCH_TIME_LIMIT = 30 * 60 * 1000L
    private const val MAX_OLD_LOCATION = 60 * 60 * 1000L
//    private const val MAX_OLD_LOCATION = 15 * 60 * 1000L


//    fun createLocationRequest(): LocationRequest {
//        val mLocationRequest = LocationRequest.create()
//        mLocationRequest.interval = INTERVAL
//        mLocationRequest.fastestInterval = FASTEST_INTERVAL
//        mLocationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
//        return mLocationRequest
//    }

    fun createLocationRequest(): LocationRequest {
       return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL).apply {
//            setMinUpdateDistanceMeters(500f)
           setMaxUpdateDelayMillis(2000)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
    }

    public fun buildCurrentLocationRequest(): CurrentLocationRequest {
        return CurrentLocationRequest.Builder()
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setDurationMillis(LOCATION_SEARCH_TIME_LIMIT)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_OLD_LOCATION)
            .build()
    }

    public fun buildLastLocationRequest(): LastLocationRequest {
        return LastLocationRequest.Builder()
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setMaxUpdateAgeMillis(MAX_OLD_LOCATION)

            .build()
    }

    fun setupLocationListener(mBaseActivity: Activity, mLocationListener: LocationListener?) {
        if (ActivityCompat.checkSelfPermission(
                mBaseActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                mBaseActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        LocationServices.getFusedLocationProviderClient(mBaseActivity).getCurrentLocation(
            buildCurrentLocationRequest(), object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken =
                    CancellationTokenSource().token

                override fun isCancellationRequested(): Boolean = false
            }).addOnSuccessListener { location: Location? ->
            if (location == null){
                Log.e(LOCATION_SERVICE, "Cannot get Current location.")
            }
            else {
                mLocationListener?.onLocationChanged(location)
            }
        }
    }

    fun buildAlertMessageNoGps(mBaseActivity: Context) {
        val builder = AlertDialog.Builder(mBaseActivity)
        builder.setTitle("Your GPS seems to be disabled, do you want to enable it?")
        builder.setPositiveButton("Yes"
        ) { dialog, which ->
            dialog.dismiss()
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            mBaseActivity.startActivity(callGPSSettingIntent)
        }
        builder.setNegativeButton(
           "No"
        ) { dialog, which ->
            dialog.dismiss()
        }
        builder.setCancelable(false)


        val alertDialog = builder.create()
        alertDialog.show()
    }
}
