package com.example.locationpoc

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class GetAddress(private val mLocation: Location?, private val mContext: Context) {

    private val maxResults = 1
    val GEOCODE_API_URI = "https://maps.googleapis.com/maps/api/geocode/json?latlng="

    fun doInBackground(): List<Address>? {
        val geocoder: Geocoder
        return try {
            val locale = Locale("en")
            geocoder = Geocoder(mContext, locale)
            /*
              @param maxResults max number of addresses to return. Smaller numbers (1 to 5) are recommended
             */
            val result = geocoder.getFromLocation(
                mLocation!!.latitude, mLocation.longitude, maxResults
            )
            result ?: makeCallGetAddressFromLatLong()
        } catch (e: IOException) {
            makeCallGetAddressFromLatLong()
        } catch (e: Exception) {
            makeCallGetAddressFromLatLong()
        }
    }

    private fun makeCallGetAddressFromLatLong(): List<Address>? {
        if (mLocation != null && mContext != null) {
            val apiRequest = "${GEOCODE_API_URI}${mLocation.latitude},${mLocation.longitude}&language=en" //+ "&ka&sensor=false"
            return try {
                val jsonObject = JSONObject(request(apiRequest).toString())
                val result = jsonObject.getJSONArray("results")
                val formattedAddress =
                    result.getJSONObject(0).getString("formatted_address")
                val location = Location("") //provider name is unnecessary
                location.latitude = mLocation.latitude
                location.longitude = mLocation.longitude
                val locale = Locale("en")
                val address = Address(locale)
                address.latitude = mLocation.latitude
                address.longitude = mLocation.longitude
                address.setAddressLine(0, formattedAddress)
                val addressResult: MutableList<Address> = ArrayList()
                addressResult.add(address)
                addressResult
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun request(urlString: String): StringBuffer {
        val response = StringBuffer("")
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "")
        connection.requestMethod = "GET"
        connection.doInput = true
        connection.connect()
        val inputStream = connection.inputStream
        val rd = BufferedReader(InputStreamReader(inputStream))
        var line: String? = ""
        while (rd.readLine().also { line = it } != null) {
            response.append(line)
        }
        return response
    }
}