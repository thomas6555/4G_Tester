package com.example.otgsignallogger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var usbPort: UsbSerialPort? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var isRecording = false

    private val requestTask = object : Runnable {
        override fun run() {
            if (isRecording) {
                sendSignalRequest()
                handler.postDelayed(this, 5000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initCsvFile()
        initUsbConnection()
    }

    private fun initUsbConnection() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        
        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "未找到 USB 裝置", Toast.LENGTH_SHORT).show()
            return
        }

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device) ?: return

        usbPort = driver.ports[0].apply {
            open(connection)
            setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        }
    }

    private fun sendSignalRequest() {
        val port = usbPort ?: return
        try {
            val command = "GET_RSSI\n".toByteArray(Charsets.UTF_8)
            port.write(command, 1000)

            val buffer = ByteArray(64)
            val len = port.read(buffer, 1000)
            
            if (len > 0) {
                val signalStrength = String(buffer, 0, len, Charsets.UTF_8).trim()
                fetchGpsAndSave(signalStrength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchGpsAndSave(signalStrength: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0
            writeToCsv(timestamp, lat, lng, signalStrength)
        }
    }

    private fun initCsvFile() {
        val file = File(getExternalFilesDir(null), "signal_log.csv")
        if (!file.exists()) {
            FileWriter(file, true).use { writer ->
                writer.append("Timestamp,Latitude,Longitude,SignalStrength\n")
            }
        }
    }

    private fun writeToCsv(timestamp: String, lat: Double, lng: Double, signal: String) {
        val file = File(getExternalFilesDir(null), "signal_log.csv")
        try {
            FileWriter(file, true).use { writer ->
                writer.append("$timestamp,$lat,$lng,$signal\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        usbPort?.close()
    }
}
