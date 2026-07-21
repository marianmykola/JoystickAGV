package com.example.JoystickAGV

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var joystickLeft: JoystickView
    private lateinit var joystickRight: JoystickView
    private lateinit var txtValues: TextView
    private lateinit var btnStart: ToggleButton
    private lateinit var btnDMS: ToggleButton
    private lateinit var btnEstop: ToggleButton
    private lateinit var btnPlcSettings: Button
    private lateinit var rotatePicker: NumberPicker

    private var lx = 0
    private var ly = 0
    private var rx = 0
    private var ry = 0

    private var sending = false
    private val handler = Handler(Looper.getMainLooper())

    private var rotate = 1
    private var packetNumber = 0
    private var estopChecked = false
    private var dmsChecked = false

    // Переменная для хранения активной Wi-Fi сети
    private var wifiNetwork: Network? = null
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    companion object {
        private const val PERMISSION_REQUEST_INTERNET = 1
        private const val DEFAULT_IP = "192.168.0.40"
        private const val DEFAULT_PORT = 3004
    }

    private var ip = DEFAULT_IP
    private var port = DEFAULT_PORT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        checkNetworkPermissions()
        bindToWifiNetwork() // Запрашиваем привязку к Wi-Fi

        joystickLeft = findViewById(R.id.joystickLeft)
        joystickRight = findViewById(R.id.joystickRight)
        txtValues = findViewById(R.id.txtValues)
        btnStart = findViewById(R.id.btnStart)
        btnDMS = findViewById(R.id.btnDMS)
        btnEstop = findViewById(R.id.btnEstop)
        btnPlcSettings = findViewById(R.id.btnPlcSettings)
        rotatePicker = findViewById(R.id.rotatePicker)

        joystickLeft.setOnMoveListener { x, y ->
            lx = x
            ly = y
            updateText()
        }

        joystickRight.setOnMoveListener { x, y ->
            rx = x
            ry = y
            updateText()
        }

        btnStart.setOnCheckedChangeListener { button, isChecked ->
            sending = isChecked
            if (isChecked) {
                button.setBackgroundColor(Color.GREEN)
                startSending()
            } else {
                button.setBackgroundColor(Color.GRAY)
            }
        }

        btnDMS.setOnCheckedChangeListener { button, isChecked ->
            dmsChecked = isChecked
            if (isChecked) {
                button.setBackgroundColor(Color.GREEN)
            } else {
                button.setBackgroundColor(Color.GRAY)
            }
        }

        btnEstop.setOnCheckedChangeListener { button, isChecked ->
            estopChecked = isChecked
            if (isChecked) {
                button.setBackgroundColor(Color.RED)
            } else {
                button.setBackgroundColor(Color.GRAY)
            }
        }

        rotatePicker.minValue = 1
        rotatePicker.maxValue = 6
        rotatePicker.value = 1
        rotatePicker.setOnValueChangedListener { _, _, newVal ->
            rotate = newVal
        }

        btnPlcSettings.setOnClickListener {
            val intent = Intent(this, PlcSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Принудительно запрашиваем у системы связь через Wi-Fi интерфейс,
     * даже если на нем нет доступа к интернету.
     */
    private fun bindToWifiNetwork() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Позволяет подключаться к Wi-Fi без интернета
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { // changed to fun instead setProcessDefaultNetwork
                wifiNetwork = network
                // Привязываем все сетевые запросы приложения к Wi-Fi интерфейсу
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.bindProcessToNetwork(network)
                } else {
                    ConnectivityManager.setProcessDefaultNetwork(network)
                }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Привязано к Wi-Fi сети", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onLost(network: Network) {
                if (wifiNetwork == network) {
                    wifiNetwork = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        connectivityManager.bindProcessToNetwork(null)
                    } else {
                        ConnectivityManager.setProcessDefaultNetwork(null)
                    }
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun updateText() {
        val lxPercent = (lx / 10.0)
        val lyPercent = (ly / 10.0)
        val rxPercent = (rx / 10.0)
        val ryPercent = (ry / 10.0)
        txtValues.text = "L: ${"%.1f".format(lxPercent)}%, ${"%.1f".format(lyPercent)}% | R: ${"%.1f".format(rxPercent)}%, ${"%.1f".format(ryPercent)}%"
    }

    private fun startSending() {
        handler.post(object : Runnable {
            override fun run() {
                if (sending) {
                    sendUdp()
                    handler.postDelayed(this, 50)
                }
            }
        })
    }

    private fun sendUdp() {
        if (!sending) return
            Thread {
            var socket: DatagramSocket? = null
        try {
        val currentWifi = wifiNetwork
        // ✅ Исправлено Build.VERSION_CODES.23 -> Build.VERSION_CODES.M
        socket = if (currentWifi != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            DatagramSocket().apply {
                currentWifi.bindSocket(this)
            }
        } else {
            DatagramSocket()
        }

        val address = InetAddress.getByName(ip)

        val buffer = ByteBuffer.allocate(12)
        buffer.put(1.toByte())  // version
        buffer.put(packetNumber.toByte())  // packet number
        buffer.put(rotate.toByte())  // rotate
        val statusByte = when {
            estopChecked -> 0.toByte()
            dmsChecked -> 3.toByte()
            else -> 1.toByte()
        }
        buffer.put(statusByte)  // status
        buffer.putShort(lx.toShort())  // lx
        buffer.putShort(ly.toShort())  // ly
        buffer.putShort(rx.toShort())  // rx
        buffer.putShort(ry.toShort())  // ry

        val data = buffer.array()
        val packet = DatagramPacket(data, data.size, address, port)
        socket.send(packet)

        packetNumber = (packetNumber + 1) % 256

    } catch (e: Exception) {
        runOnUiThread {
            if (sending) {
                Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            e.printStackTrace()
           } finally {
            socket?.close()
        }
    }.start()
        
       
    }

    private fun checkNetworkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.INTERNET),
                PERMISSION_REQUEST_INTERNET
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Отписываем коллбэк при закрытии активности
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}