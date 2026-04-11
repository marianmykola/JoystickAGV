package com.example.JoystickAGV

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.graphics.Color
import android.widget.ToggleButton

class MainActivity : AppCompatActivity() {

    private lateinit var joystickLeft: JoystickView
    private lateinit var joystickRight: JoystickView
    private lateinit var txtValues: TextView
    private lateinit var btnStart: ToggleButton
    private lateinit var btnDMS: ToggleButton

    private var lx = 0
    private var ly = 0
    private var rx = 0
    private var ry = 0

    private var sending = false
    private val handler = Handler(Looper.getMainLooper())

    private val ip = "192.168.0.40"
    private val port = 3004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joystickLeft = findViewById(R.id.joystickLeft)
        joystickRight = findViewById(R.id.joystickRight)
        txtValues = findViewById(R.id.txtValues)
        btnStart = findViewById(R.id.btnStart)

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
            sending = isChecked

            if (isChecked) {
                button.setBackgroundColor(Color.GREEN)
                startSending()
            } else {
                button.setBackgroundColor(Color.GRAY)
            }
        }
    }

    private fun updateText() {
        txtValues.text = "L: $lx,$ly | R: $rx,$ry"
    }

    private fun startSending() {
        handler.post(object : Runnable {
            override fun run() {
                if (sending) {
                    sendUdp()
                    handler.postDelayed(this, 500) // every 0.5 sec
                }
            }
        })
    }

    private fun sendUdp() {
        Thread {
            try {
                val socket = DatagramSocket()
                val address = InetAddress.getByName(ip)

                val message = "L:$lx,$ly;R:$rx,$ry"
                val data = message.toByteArray()

                val packet = DatagramPacket(data, data.size, address, port)
                socket.send(packet)
                socket.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
