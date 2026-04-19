package com.example.JoystickAGV

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.ToggleButton
import android.widget.Toast
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
        
        // Check network permissions
        checkNetworkPermissions()

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
        rotatePicker.setOnValueChangedListener { picker, oldVal, newVal ->
            rotate = newVal
        }

        btnPlcSettings.setOnClickListener {
            val intent = Intent(this, PlcSettingsActivity::class.java)
            startActivity(intent)
        }
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
                    handler.postDelayed(this, 50) // every 50 ms
                }
            }
        })
    }

    private fun sendUdp() {
        Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
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
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_INTERNET -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Internet permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Internet permission required for UDP communication", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
