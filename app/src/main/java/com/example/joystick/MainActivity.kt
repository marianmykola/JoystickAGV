package com.example.joystick

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
import android.widget.RadioGroup

class MainActivity : AppCompatActivity() {

    private lateinit var joystickLeft: JoystickView
    private lateinit var joystickRight: JoystickView
    private lateinit var txtValues: TextView
    private lateinit var btnStart: ToggleButton
    private lateinit var btnDMS: ToggleButton
    private lateinit var btnEstop: ToggleButton
    private lateinit var rotateSwitch: RadioGroup

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

    private val ip = "192.168.0.40"
    private val port = 3004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joystickLeft = findViewById(R.id.joystickLeft)
        joystickRight = findViewById(R.id.joystickRight)
        txtValues = findViewById(R.id.txtValues)
        btnStart = findViewById(R.id.btnStart)
        btnDMS = findViewById(R.id.btnDMS)
        btnEstop = findViewById(R.id.btnEstop)
        rotateSwitch = findViewById(R.id.rotateSwitch)

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

        rotateSwitch.setOnCheckedChangeListener { group, checkedId ->
            rotate = when(checkedId) {
                R.id.rotate1 -> 1
                R.id.rotate2 -> 2
                R.id.rotate3 -> 3
                R.id.rotate4 -> 4
                R.id.rotate5 -> 5
                R.id.rotate6 -> 6
                else -> 1
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
                    handler.postDelayed(this, 50) // every 50 ms
                }
            }
        })
    }

    private fun sendUdp() {
        Thread {
            try {
                val socket = DatagramSocket()
                val address = InetAddress.getByName(ip)

                val version = "01"
                val packetStr = String.format("%02X", packetNumber)
                val rot = String.format("%02d", rotate)
                val status = if (estopChecked) "00" else if (dmsChecked) "03" else "01"
                val lxHex = String.format("%04X", lx.toShort())
                val lyHex = String.format("%04X", ly.toShort())
                val rxHex = String.format("%04X", rx.toShort())
                val ryHex = String.format("%04X", ry.toShort())

                val message = version + packetStr + rot + status + lxHex + lyHex + rxHex + ryHex
                val data = message.toByteArray()

                val packet = DatagramPacket(data, data.size, address, port)
                socket.send(packet)
                socket.close()

                packetNumber = (packetNumber + 1) % 256

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
