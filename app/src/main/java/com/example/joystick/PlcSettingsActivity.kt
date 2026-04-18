package com.example.JoystickAGV

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PlcSettingsActivity : AppCompatActivity() {

    private lateinit var etIp: EditText
    private lateinit var etRack: EditText
    private lateinit var etSlot: EditText
    private lateinit var etDbNumber: EditText
    private lateinit var etOffset: EditText
    private lateinit var etValue: EditText
    private lateinit var tvResponse: TextView
    private lateinit var btnRead: Button
    private lateinit var btnWrite: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plc_settings)

        etIp = findViewById(R.id.etIp)
        etRack = findViewById(R.id.etRack)
        etSlot = findViewById(R.id.etSlot)
        etDbNumber = findViewById(R.id.etDbNumber)
        etOffset = findViewById(R.id.etOffset)
        etValue = findViewById(R.id.etValue)
        tvResponse = findViewById(R.id.tvResponse)
        btnRead = findViewById(R.id.btnRead)
        btnWrite = findViewById(R.id.btnWrite)

        // Default values
        etIp.setText("192.168.0.40")
        etRack.setText("0")
        etSlot.setText("1")

        btnRead.setOnClickListener {
            val ip = etIp.text.toString()
            val rack = etRack.text.toString()
            val slot = etSlot.text.toString()
            val db = etDbNumber.text.toString()
            val offset = etOffset.text.toString()
            
            tvResponse.text = "Read from DB$db at offset $offset\nIP: $ip, Rack: $rack, Slot: $slot"
            Toast.makeText(this, "PLC Settings (configuration mode)", Toast.LENGTH_SHORT).show()
        }

        btnWrite.setOnClickListener {
            val db = etDbNumber.text.toString()
            val offset = etOffset.text.toString()
            val value = etValue.text.toString()
            
            tvResponse.text = "Write to DB$db at offset $offset value: $value"
            Toast.makeText(this, "PLC Settings (configuration mode)", Toast.LENGTH_SHORT).show()
        }
    }
}
