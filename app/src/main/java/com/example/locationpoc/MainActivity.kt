package com.example.locationpoc

import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.locationpoc.LocationHelper.buildAlertMessageNoGps
import com.example.locationpoc.LocationHelper.setupLocationListener
import com.example.locationpoc.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import pl.tajchert.nammu.Nammu
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), LocationManagerUtils.CurrentLocationSettingListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var oldLocationManagerUtils: LocationManagerUtils
    private lateinit var newLocationManagerUtils: LocationManagerUtils
    private var oldLocation: Location? = null
    private var newLocation: Location? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        updateUptimes()
        Nammu.init(this)
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager

        binding.btnRefresh.setOnClickListener {
            if (manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                buildAlertMessageNoGps(this)
            } else {
                newLocationManagerUtils.checkLocationPermission()
                initOldLogic()
            }

        }
        initNewLogic()

    }

    override fun onResume() {
        super.onResume()

    }

    private fun initNewLogic() {
        newLocationManagerUtils = LocationManagerUtils()
        newLocationManagerUtils.isForceToGrantToLocationPermission = true
        newLocationManagerUtils.initLocationManager(this, { location ->

            binding.tvNewLatLong.text = "Lat-Long : " + location.latitude + "," + location.longitude
            val message =
                "new location is => " + "(" + location.latitude + "," + location.longitude + ")" + " and location fix, in milliseconds time =>" + location.time + " and by provider => " + location.provider
            Log.d("MainActivity", message)
            newLocation = location
            calculateDistance()
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            executor.execute {
                val address = GetAddress(location, this)
                val result = address.doInBackground()
                handler.post {
                    result?.let {
                        if (result.isNotEmpty()) {
                            val address = result[0]
                            binding.tvNewAddress.text =
                                "Address = \n" + address.getAddressLine(0) + "\nPincode : " + address.postalCode
                        }

                    }
                    executor.shutdown()
                }
            }
        }, true, getString(R.string.err_grant_permission), this, false)

    }

    private fun initOldLogic() {

        oldLocationManagerUtils = LocationManagerUtils()
        oldLocationManagerUtils.initLocationManager(this, { location ->
            binding.tvCurrentLatLong.text =
                "Lat-Long : " + location.latitude + "," + location.longitude
            val message =
                "old location is => " + "(" + location.latitude + "," + location.longitude + ")" + " and location fix, in milliseconds time =>" + location.time + " and by provider => " + location.provider
            Log.d("MainActivity", message)
            oldLocation = location
            calculateDistance()
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            executor.execute {
                val address = GetAddress(location, this)
                val result = address.doInBackground()
                handler.post {
                    result?.let {
                        if (result.isNotEmpty()) {
                            val address = result[0]
                            binding.tvCurrentAddress.text =
                                "Address = \n" + address.getAddressLine(0) + "\nPincode : " + address.postalCode
                        }
                        LocationServices.FusedLocationApi.removeLocationUpdates(
                            oldLocationManagerUtils.mLocationService?.googleApiClient!!,
                            oldLocationManagerUtils.mLocationListener!!
                        )

                    }
                    executor.shutdown()
                }
            }
        }, true, getString(R.string.err_grant_permission), null, true)
        oldLocationManagerUtils.isForceToGrantToLocationPermission = true


    }

    private fun calculateDistance() {
        if (oldLocation != null && newLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                oldLocation!!.latitude,
                oldLocation!!.longitude,
                newLocation!!.latitude,
                newLocation!!.longitude,
                results
            )
            binding.tvDistance.text = "Distance in Meter : " + results[0]
        }

    }

    private fun updateUptimes() {
        try {
            val time = SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.ENGLISH).format(
                Calendar
                    .getInstance().time
            )
            binding.tvTimestamp.text = "Time Stamp : $time"
            Handler(mainLooper).post { updateUptimes() }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onSatisfied() {
        if (newLocationManagerUtils.fineLocationGranted && newLocationManagerUtils.coarseLocationGranted) {
            initOldLogic()
        }
    }

}