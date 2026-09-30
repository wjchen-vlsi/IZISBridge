package com.example.izisbridge

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.serialport.SerialPort
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var etDevicePath: EditText
    private lateinit var etPort: EditText
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button

    private var isBridging = false
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var serialPort: SerialPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etDevicePath = findViewById(R.id.etDevicePath)
        etPort = findViewById(R.id.etPort)
        tvStatus = findViewById(R.id.tvStatus)
        btnToggle = findViewById(R.id.btnToggle)

        updateStatusText("Stopped")

        btnToggle.setOnClickListener {
            if (isBridging) stopBridge() else startBridge()
        }
    }

    private fun startBridge() {
        val devicePath = etDevicePath.text.toString().trim()
        val portStr = etPort.text.toString().trim()
        val port = portStr.toIntOrNull()

        if (devicePath.isEmpty() || port == null || port !in 1..65535) {
            Toast.makeText(this, "Invalid device path or port", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            SerialPort.setSuPath("/system/xbin/su")
            serialPort = SerialPort(File(devicePath), 115200)
            val serialIn = serialPort!!.inputStream
            val serialOut = serialPort!!.outputStream

            isBridging = true
            etDevicePath.isEnabled = false
            etPort.isEnabled = false
            btnToggle.text = "Stop Forwarding"
            updateStatusText("Listening on ${getDeviceIpAddress()}:$port...")

            thread(start = true) {
                try {
                    serverSocket = ServerSocket(port)
                    while (isBridging) {
                        clientSocket = serverSocket?.accept()
                        runOnUiThread { updateStatusText("Connected to Client!\n(${getDeviceIpAddress()}:$port)") }

                        val tcpIn = clientSocket!!.getInputStream()
                        val tcpOut = clientSocket!!.getOutputStream()

                        thread { routeData(serialIn, tcpOut) }
                        thread { routeData(tcpIn, serialOut) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: SecurityException) {
            updateStatusText("Error: Missing permissions for $devicePath")
            resetUI()
        } catch (e: Exception) {
            updateStatusText("Error: ${e.message}")
            resetUI()
        }
    }

    private fun routeData(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        try {
            while (isBridging) {
                val size = input.read(buffer)
                if (size > 0) {
                    output.write(buffer, 0, size)
                    output.flush()
                } else if (size == -1) {
                    break
                }
            }
        } catch (e: Exception) {
            // Stream closed
        }
    }

    private fun stopBridge() {
        isBridging = false
        try { clientSocket?.close() } catch (e: Exception) {}
        try { serverSocket?.close() } catch (e: Exception) {}
        try { serialPort?.close() } catch (e: Exception) {}

        resetUI()
        updateStatusText("Stopped")
    }

    private fun resetUI() {
        runOnUiThread {
            isBridging = false
            etDevicePath.isEnabled = true
            etPort.isEnabled = true
            btnToggle.text = "Start Forwarding"
        }
    }

    private fun updateStatusText(status: String) {
        runOnUiThread {
            tvStatus.text = "Board IP: ${getDeviceIpAddress()}\nStatus: $status"
        }
    }

    private fun getDeviceIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        if (ipAddress == 0) return "Not connected to Wi-Fi"
        return String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBridge()
    }
}