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
            readFromPLC()
        }

        btnWrite.setOnClickListener {
            writeToPLC()
        }
    }

    private fun readFromPLC() {
        Thread {
            try {
                val ip = etIp.text.toString()
                val db = etDbNumber.text.toString().toIntOrNull() ?: 0
                val offset = etOffset.text.toString().toIntOrNull() ?: 0

                // TODO: Implement PLC read with proper S7 library
                runOnUiThread {
                    tvResponse.text = "PLC Read Feature\n\nIP: $ip\nDB: $db\nOffset: $offset\n\nPLC library coming soon..."
                    Toast.makeText(this, "PLC feature under development", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun writeToPLC() {
        Thread {
            try {
                val ip = etIp.text.toString()
                val db = etDbNumber.text.toString().toIntOrNull() ?: 0
                val offset = etOffset.text.toString().toIntOrNull() ?: 0
                val value = etValue.text.toString().toIntOrNull() ?: 0

                // TODO: Implement PLC write with proper S7 library
                runOnUiThread {
                    tvResponse.text = "PLC Write Feature\n\nIP: $ip\nDB: $db\nOffset: $offset\nValue: $value\n\nPLC library coming soon..."
                    Toast.makeText(this, "PLC feature under development", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
